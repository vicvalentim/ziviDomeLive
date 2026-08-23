package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.performance.PerformanceMetric;
import com.victorvalentim.zividomelive.performance.PerformanceMode;
import processing.core.PApplet;

/**
 * Render-thread coordinator for optional pipeline-wide GPU measurement.
 *
 * <p>The measurement session discovers and qualifies timer-query backends before normal results
 * are accepted. When qualified timer queries are unavailable under the automatic policy, it may
 * fall back to asynchronous fence completion latency. Timer-query results and fence-completion
 * results deliberately use different backend identifiers so reports cannot silently treat them as
 * the same physical quantity.</p>
 */
final class GpuPerformanceTimer {
    private static final int QUERY_POOL_SIZE = 8;

    private final PApplet parent;
    private final PerformanceMonitor monitor;
    private final ProcessingGlAdapter.GpuTimerResultConsumer resultConsumer;
    private ProcessingGpuMeasurementSession measurementSession;
    private long measurementSessionId;
    private long failedSessionId;
    private boolean intervalActive;
    private boolean closePending;

    public GpuPerformanceTimer(PApplet parent, PerformanceMonitor monitor) {
        if (parent == null || monitor == null) {
            throw new IllegalArgumentException("GPU performance timer dependencies cannot be null.");
        }
        this.parent = parent;
        this.monitor = monitor;
        this.resultConsumer = this::acceptResult;
    }

    /** Begins the pipeline-wide interval when CPU_GPU is requested and a backend is available. */
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
            if (measurementSession == null
                    || measurementSessionId != activeSessionId
                    || closePending) {
                closeCurrentSession();
                measurementSession = new ProcessingGpuMeasurementSession(
                        parent,
                        QUERY_POOL_SIZE,
                        monitor.getGpuTimerPolicy());
                measurementSessionId = activeSessionId;
                closePending = false;
            }

            intervalActive = measurementSession.begin(
                    frameId,
                    activeSessionId,
                    resultConsumer);

            if (measurementSession.isReady()) {
                monitor.activateGpu(
                        activeSessionId,
                        measurementSession.getBackend(),
                        measurementSession.getArchitecture(),
                        measurementSession.getDiagnostic());
                if (!intervalActive) {
                    monitor.countDroppedGpuSample(activeSessionId);
                }
            }
            return intervalActive;
        } catch (RuntimeException | LinkageError error) {
            fallback(activeSessionId, error);
            return false;
        }
    }

    /** Ends the active interval. Failure truthfully degrades the current profiling session. */
    public void end() {
        if (!intervalActive || measurementSession == null) {
            return;
        }
        long activeSessionId = measurementSessionId;
        try {
            measurementSession.end();
            if (measurementSession.isReady()) {
                monitor.activateGpu(
                        activeSessionId,
                        measurementSession.getBackend(),
                        measurementSession.getArchitecture(),
                        measurementSession.getDiagnostic());
            }
        } catch (RuntimeException | LinkageError error) {
            fallback(activeSessionId, error);
        } finally {
            intervalActive = false;
        }
    }

    /**
     * Polls ready tail samples once, then releases native resources. Must only be requested from
     * the Processing render thread; other callers defer cleanup to the next render-thread use.
     */
    public void stop(boolean onRenderThread) {
        if (!onRenderThread) {
            closePending = measurementSession != null;
            return;
        }
        if (measurementSession == null) {
            return;
        }
        try {
            measurementSession.collectAvailable(resultConsumer);
        } catch (RuntimeException | LinkageError error) {
            fallback(measurementSessionId, error);
            return;
        }
        int pendingResults = measurementSession.pendingResultCount();
        for (int index = 0; index < pendingResults; index++) {
            monitor.countDroppedGpuSample(measurementSessionId);
        }
        closeCurrentSession();
    }

    /** Releases native measurement resources during the Processing disposal hook. */
    public void dispose(boolean onRenderThread) {
        if (onRenderThread) {
            closeCurrentSession();
        } else {
            closePending = measurementSession != null;
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
                "CPU_GPU requested; GPU measurement failed ("
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
        ProcessingGpuMeasurementSession current = measurementSession;
        measurementSession = null;
        measurementSessionId = 0L;
        intervalActive = false;
        closePending = false;
        if (current != null) {
            try {
                current.close();
            } catch (RuntimeException | LinkageError ignored) {
                // The GL context may already be gone. The bounded native resources are abandoned.
            }
        }
    }
}
