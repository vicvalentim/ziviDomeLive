package com.victorvalentim.zividomelive.internal.performance;

import com.victorvalentim.zividomelive.performance.PerformanceMetric;
import com.victorvalentim.zividomelive.performance.PerformanceMode;
import com.victorvalentim.zividomelive.render.gl.ProcessingGlAdapter;
import processing.core.PApplet;

/**
 * Render-thread coordinator for the optional pipeline-wide GPU elapsed query.
 *
 * <p>The native query pool is lazy, bounded, and owned by {@link ProcessingGlAdapter}.
 * This coordinator only associates delayed results with monitor sessions and frames.</p>
 */
public final class GpuPerformanceTimer {
	private static final int QUERY_POOL_SIZE = 8;

	private final PApplet parent;
	private final PerformanceMonitor monitor;
	private final ProcessingGlAdapter glAdapter;
	private final ProcessingGlAdapter.GpuTimerResultConsumer resultConsumer;
	private ProcessingGlAdapter.GpuTimerQuerySession querySession;
	private long querySessionId;
	private long failedSessionId;
	private boolean queryActive;
	private boolean closePending;

	public GpuPerformanceTimer(PApplet parent, PerformanceMonitor monitor) {
		if (parent == null || monitor == null) {
			throw new IllegalArgumentException("GPU performance timer dependencies cannot be null.");
		}
		this.parent = parent;
		this.monitor = monitor;
		this.glAdapter = ProcessingGlAdapter.getDefault();
		this.resultConsumer = this::acceptResult;
	}

	/** Begins the single pipeline-wide interval when CPU_GPU is active and supported. */
	public boolean begin() {
		long activeSessionId = monitor.getActiveSessionId();
		long frameId = monitor.getCurrentFrameId();
		if (monitor.getRequestedMode() != PerformanceMode.CPU_GPU
				|| activeSessionId == 0L
				|| frameId < 0L) {
			closeIfPending();
			return false;
		}
		if (failedSessionId == activeSessionId) {
			return false;
		}

		try {
			if (querySession == null || querySessionId != activeSessionId || closePending) {
				closeCurrentSession();
				querySession = glAdapter.createGpuTimerQuerySession(
						parent,
						QUERY_POOL_SIZE,
						monitor.getGpuTimerPolicy());
				querySessionId = activeSessionId;
				closePending = false;
			}
			queryActive = querySession.begin(frameId, activeSessionId, resultConsumer);
			monitor.activateGpu(
					activeSessionId,
					querySession.getBackend(),
					querySession.getArchitecture());
			if (!queryActive) {
				monitor.countDroppedGpuSample(activeSessionId);
			}
			return queryActive;
		} catch (RuntimeException | LinkageError error) {
			fallback(activeSessionId, error);
			return false;
		}
	}

	/** Ends the active interval. Failure degrades the current profiling session to CPU. */
	public void end() {
		if (!queryActive || querySession == null) {
			return;
		}
		long activeSessionId = querySessionId;
		try {
			querySession.end();
		} catch (RuntimeException | LinkageError error) {
			fallback(activeSessionId, error);
		} finally {
			queryActive = false;
		}
	}

	/**
	 * Polls ready tail samples once, then releases the pool. Must only be requested from the
	 * Processing render thread; other callers defer cleanup to the next render-thread use.
	 */
	public void stop(boolean onRenderThread) {
		if (!onRenderThread) {
			closePending = querySession != null;
			return;
		}
		if (querySession == null) {
			return;
		}
		try {
			querySession.collectAvailable(resultConsumer);
		} catch (RuntimeException | LinkageError error) {
			fallback(querySessionId, error);
			return;
		}
		int pendingResults = querySession.pendingResultCount();
		for (int index = 0; index < pendingResults; index++) {
			monitor.countDroppedGpuSample(querySessionId);
		}
		closeCurrentSession();
	}

	/** Releases native query resources during the Processing disposal hook. */
	public void dispose(boolean onRenderThread) {
		if (onRenderThread) {
			closeCurrentSession();
		} else {
			closePending = querySession != null;
		}
	}

	/** Performs cleanup deferred by a non-render-thread control call. */
	public void maintain() {
		closeIfPending();
	}

	private void acceptResult(long frameId, long profilingSessionId, long elapsedNanos) {
		if (!monitor.recordGpuDuration(
				PerformanceMetric.RENDER_PIPELINE,
				frameId,
				elapsedNanos,
				profilingSessionId)) {
			monitor.countDroppedGpuSample(profilingSessionId);
		}
	}

	private void fallback(long activeSessionId, Throwable error) {
		failedSessionId = activeSessionId;
		String detail = error.getMessage();
		monitor.fallbackGpu(
				activeSessionId,
				"CPU_GPU requested; asynchronous GPU timer queries failed ("
						+ error.getClass().getSimpleName()
						+ (detail == null || detail.isBlank() ? "" : ": " + detail)
						+ "), so CPU profiling is active.");
		closeCurrentSession();
	}

	private void closeIfPending() {
		if (closePending) {
			closeCurrentSession();
		}
	}

	private void closeCurrentSession() {
		ProcessingGlAdapter.GpuTimerQuerySession current = querySession;
		querySession = null;
		querySessionId = 0L;
		queryActive = false;
		closePending = false;
		if (current != null) {
			try {
				current.close();
			} catch (RuntimeException | LinkageError ignored) {
				// The GL context may already be gone. Session.close() abandons its bounded IDs.
			}
		}
	}
}
