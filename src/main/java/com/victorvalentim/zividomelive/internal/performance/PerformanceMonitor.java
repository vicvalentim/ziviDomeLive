package com.victorvalentim.zividomelive.internal.performance;

import com.victorvalentim.zividomelive.performance.PerformanceMetric;
import com.victorvalentim.zividomelive.performance.PerformanceMode;
import com.victorvalentim.zividomelive.performance.PerformanceSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Internal cross-package recorder for the experimental performance API.
 * Applications should use the facade rather than depending on this class.
 */
public final class PerformanceMonitor {
	public static final int DEFAULT_CAPACITY = 4096;
	public static final int MINIMUM_CAPACITY = 2;
	public static final int MAXIMUM_CAPACITY = 100_000;

	private static final int METRIC_COUNT = PerformanceMetric.values().length;
	private static final long[][] EMPTY_DURATIONS = new long[METRIC_COUNT][0];
	private static final int[][] EMPTY_CALLS = new int[METRIC_COUNT][0];
	private static final ThreadLocal<PerformanceMonitor> CURRENT = new ThreadLocal<>();

	private volatile PerformanceMode requestedMode = PerformanceMode.OFF;
	private volatile PerformanceMode effectiveMode = PerformanceMode.OFF;
	private volatile boolean active;
	private volatile long sessionId;
	private int capacity;
	private long[][] durationsNanos = EMPTY_DURATIONS;
	private int[][] calls = EMPTY_CALLS;
	private long[][] gpuDurationsNanos = EMPTY_DURATIONS;
	private int[][] gpuCalls = EMPTY_CALLS;
	private long[] currentDurations;
	private int[] currentCalls;
	private AtomicLongArray concurrentDurations;
	private AtomicIntegerArray concurrentCalls;
	private long frameStartedNanos;
	private boolean frameOpen;
	private long totalFrames;
	private int storedFrames;
	private long invariantViolations;
	private long cubemapCaptureViolations;
	private long unexpectedPassViolations;
	private long droppedGpuSamples;
	private String gpuDiagnostic;
	private boolean expectationsSet;
	private int expectedStandard;
	private int expectedCubemap;
	private int expectedDomemaster;
	private int expectedEquirectangular;
	private int expectedSkybox;
	private int expectedPreviewCopies;

	public boolean isEnabled() {
		return active;
	}

	public PerformanceMode getRequestedMode() {
		return requestedMode;
	}

	public PerformanceMode getEffectiveMode() {
		return effectiveMode;
	}

	public synchronized void enable(PerformanceMode mode, int sampleCapacity) {
		if (mode == null) {
			throw new IllegalArgumentException("Performance mode cannot be null.");
		}
		if (mode == PerformanceMode.OFF) {
			active = false;
			advanceSession();
			requestedMode = PerformanceMode.OFF;
			effectiveMode = PerformanceMode.OFF;
			frameOpen = false;
			expectationsSet = false;
			return;
		}
		validateCapacity(sampleCapacity);
		active = false;
		requestedMode = mode;
		capacity = sampleCapacity;
		durationsNanos = new long[METRIC_COUNT][sampleCapacity];
		calls = new int[METRIC_COUNT][sampleCapacity];
		gpuDurationsNanos = mode == PerformanceMode.CPU_GPU
				? new long[METRIC_COUNT][sampleCapacity]
				: EMPTY_DURATIONS;
		gpuCalls = mode == PerformanceMode.CPU_GPU
				? new int[METRIC_COUNT][sampleCapacity]
				: EMPTY_CALLS;
		currentDurations = new long[METRIC_COUNT];
		currentCalls = new int[METRIC_COUNT];
		concurrentDurations = new AtomicLongArray(METRIC_COUNT);
		concurrentCalls = new AtomicIntegerArray(METRIC_COUNT);
		resetState();
		advanceSession();
		effectiveMode = PerformanceMode.CPU;
		active = true;
	}

	public synchronized void disable() {
		active = false;
		advanceSession();
		frameOpen = false;
		expectationsSet = false;
	}

	public synchronized void reset() {
		if (currentDurations == null) {
			return;
		}
		for (long[] metricDurations : durationsNanos) {
			Arrays.fill(metricDurations, 0L);
		}
		for (int[] metricCalls : calls) {
			Arrays.fill(metricCalls, 0);
		}
		for (long[] metricDurations : gpuDurationsNanos) {
			Arrays.fill(metricDurations, 0L);
		}
		for (int[] metricCalls : gpuCalls) {
			Arrays.fill(metricCalls, 0);
		}
		resetState();
		advanceSession();
	}

	private void resetState() {
		if (currentDurations != null) Arrays.fill(currentDurations, 0L);
		if (currentCalls != null) Arrays.fill(currentCalls, 0);
		if (concurrentDurations != null) {
			for (int index = 0; index < METRIC_COUNT; index++) {
				concurrentDurations.set(index, 0L);
				concurrentCalls.set(index, 0);
			}
		}
		frameStartedNanos = 0L;
		frameOpen = false;
		totalFrames = 0L;
		storedFrames = 0;
		invariantViolations = 0L;
		cubemapCaptureViolations = 0L;
		unexpectedPassViolations = 0L;
		droppedGpuSamples = 0L;
		gpuDiagnostic = null;
		expectationsSet = false;
	}

	/** Returns the absolute index of the open frame, or {@code -1} outside a frame. */
	synchronized long getCurrentFrameId() {
		return active && frameOpen ? totalFrames : -1L;
	}

	/** Marks GPU collection active only when initialization belongs to this session. */
	synchronized void activateGpu(long expectedSessionId) {
		if (requestedMode == PerformanceMode.CPU_GPU && isSessionActive(expectedSessionId)) {
			effectiveMode = PerformanceMode.CPU_GPU;
			gpuDiagnostic = null;
		}
	}

	/** Falls back truthfully after a capability or runtime failure. */
	synchronized void fallbackGpu(long expectedSessionId, String diagnostic) {
		if (requestedMode == PerformanceMode.CPU_GPU && isSessionActive(expectedSessionId)) {
			effectiveMode = PerformanceMode.CPU;
			gpuDiagnostic = diagnostic;
		}
	}

	/** Records one delayed GPU result against its original committed frame. */
	synchronized boolean recordGpuDuration(
			PerformanceMetric metric,
			long frameId,
			long durationNanos,
			long expectedSessionId) {
		if (metric == null
				|| !isSessionActive(expectedSessionId)
				|| effectiveMode != PerformanceMode.CPU_GPU
				|| durationNanos <= 0L
				|| frameId < Math.max(0L, totalFrames - storedFrames)
				|| frameId >= totalFrames) {
			return false;
		}
		int destination = (int) (frameId % capacity);
		int metricIndex = metric.ordinal();
		gpuDurationsNanos[metricIndex][destination] += Math.max(0L, durationNanos);
		gpuCalls[metricIndex][destination]++;
		return true;
	}

	synchronized void countDroppedGpuSample(long expectedSessionId) {
		if (isSessionActive(expectedSessionId)) {
			droppedGpuSamples++;
		}
	}

	public void beginFrame() {
		if (!isEnabled()) {
			return;
		}
		beginFrame(System.nanoTime());
	}

	/** Deterministic boundary used by unit tests and the runtime. */
	public synchronized void beginFrame(long nowNanos) {
		if (!isEnabled()) {
			return;
		}
		if (frameOpen) {
			drainConcurrentMetrics();
			currentDurations[PerformanceMetric.FRAME_TOTAL.ordinal()] =
					Math.max(0L, nowNanos - frameStartedNanos);
			currentCalls[PerformanceMetric.FRAME_TOTAL.ordinal()] = 1;
			commitCurrentFrame();
		}
		Arrays.fill(currentDurations, 0L);
		Arrays.fill(currentCalls, 0);
		frameStartedNanos = nowNanos;
		frameOpen = true;
		expectationsSet = false;
	}

	public long start() {
		return isEnabled() ? System.nanoTime() : 0L;
	}

	public void record(PerformanceMetric metric, long startedNanos) {
		if (!isEnabled() || !frameOpen || metric == null || startedNanos == 0L) {
			return;
		}
		recordDuration(metric, Math.max(0L, System.nanoTime() - startedNanos));
	}

	public void recordDuration(PerformanceMetric metric, long durationNanos) {
		if (!isEnabled() || !frameOpen || metric == null) {
			return;
		}
		int index = metric.ordinal();
		currentDurations[index] += Math.max(0L, durationNanos);
		currentCalls[index]++;
	}

	public void count(PerformanceMetric metric) {
		if (!isEnabled() || !frameOpen || metric == null) {
			return;
		}
		currentCalls[metric.ordinal()]++;
	}

	/** Records worker-thread work without touching the render-thread frame arrays. */
	public synchronized void recordConcurrent(PerformanceMetric metric, long durationNanos) {
		recordConcurrent(metric, durationNanos, getActiveSessionId());
	}

	/** Records worker work only when it belongs to the current profiling session. */
	public synchronized void recordConcurrent(
			PerformanceMetric metric,
			long durationNanos,
			long expectedSessionId) {
		AtomicLongArray durations = concurrentDurations;
		AtomicIntegerArray metricCalls = concurrentCalls;
		if (!isSessionActive(expectedSessionId)
				|| metric == null
				|| durations == null
				|| metricCalls == null) {
			return;
		}
		int index = metric.ordinal();
		durations.addAndGet(index, Math.max(0L, durationNanos));
		metricCalls.incrementAndGet(index);
	}

	/** Returns a token that worker tasks can use to reject stale measurements. */
	public long getActiveSessionId() {
		return active ? sessionId : 0L;
	}

	/** Reports whether a worker token still belongs to the active measurement. */
	public boolean isSessionActive(long expectedSessionId) {
		return active && expectedSessionId != 0L && sessionId == expectedSessionId;
	}

	public void setExpectedPassCounts(
			int standard,
			int cubemap,
			int domemaster,
			int equirectangular,
			int skybox,
			int previewCopies) {
		if (!isEnabled() || !frameOpen) {
			return;
		}
		expectedStandard = standard;
		expectedCubemap = cubemap;
		expectedDomemaster = domemaster;
		expectedEquirectangular = equirectangular;
		expectedSkybox = skybox;
		expectedPreviewCopies = previewCopies;
		expectationsSet = true;
	}

	public synchronized PerformanceSnapshot snapshot() {
		int retained = storedFrames;
		long[][] chronologicalDurations = new long[METRIC_COUNT][retained];
		int[][] chronologicalCalls = new int[METRIC_COUNT][retained];
		long[][] chronologicalGpuDurations = new long[METRIC_COUNT][retained];
		int[][] chronologicalGpuCalls = new int[METRIC_COUNT][retained];
		int oldest = retained > 0 && retained == capacity
				? (int) (totalFrames % capacity)
				: 0;
		for (int metric = 0; metric < METRIC_COUNT; metric++) {
			for (int frame = 0; frame < retained; frame++) {
				int source = (oldest + frame) % capacity;
				chronologicalDurations[metric][frame] = durationsNanos[metric][source];
				chronologicalCalls[metric][frame] = calls[metric][source];
				if (gpuDurationsNanos[metric].length > 0) {
					chronologicalGpuDurations[metric][frame] = gpuDurationsNanos[metric][source];
					chronologicalGpuCalls[metric][frame] = gpuCalls[metric][source];
				}
			}
		}

		List<String> diagnostics = new ArrayList<>(5);
		if (requestedMode == PerformanceMode.CPU_GPU && effectiveMode == PerformanceMode.CPU) {
			diagnostics.add(gpuDiagnostic == null
					? "CPU_GPU requested; GPU timer queries are unavailable, so CPU profiling is active."
					: gpuDiagnostic);
		}
		if (effectiveMode == PerformanceMode.CPU_GPU) {
			diagnostics.add("GPU elapsed time covers RENDER_PIPELINE only; all other timings are CPU wall time.");
		}
		if (droppedGpuSamples > 0L) {
			diagnostics.add("Asynchronous GPU samples dropped without blocking: " + droppedGpuSamples + ".");
		}
		if (cubemapCaptureViolations > 0L) {
			diagnostics.add("Cubemap capture invariant violations: " + cubemapCaptureViolations + ".");
		}
		if (unexpectedPassViolations > 0L) {
			diagnostics.add("Render-pass invariant violations: " + unexpectedPassViolations + ".");
		}

		return new PerformanceSnapshot(
				requestedMode,
				effectiveMode,
				totalFrames,
				retained,
				Math.max(0L, totalFrames - retained),
				chronologicalDurations,
				chronologicalCalls,
				invariantViolations,
				cubemapCaptureViolations,
				unexpectedPassViolations,
				diagnostics,
				chronologicalGpuDurations,
				chronologicalGpuCalls);
	}

	private void drainConcurrentMetrics() {
		if (concurrentDurations == null) {
			return;
		}
		for (int index = 0; index < METRIC_COUNT; index++) {
			currentDurations[index] += concurrentDurations.getAndSet(index, 0L);
			currentCalls[index] += concurrentCalls.getAndSet(index, 0);
		}
	}

	private void commitCurrentFrame() {
		validateInvariants();
		int destination = (int) (totalFrames % capacity);
		for (int metric = 0; metric < METRIC_COUNT; metric++) {
			durationsNanos[metric][destination] = currentDurations[metric];
			calls[metric][destination] = currentCalls[metric];
			if (gpuDurationsNanos[metric].length > 0) {
				gpuDurationsNanos[metric][destination] = 0L;
				gpuCalls[metric][destination] = 0;
			}
		}
		totalFrames++;
		if (storedFrames < capacity) storedFrames++;
	}

	private void validateInvariants() {
		int cubemapCalls = currentCalls[PerformanceMetric.CUBEMAP_TOTAL.ordinal()];
		if (cubemapCalls > 1 || (expectationsSet && cubemapCalls != expectedCubemap)) {
			cubemapCaptureViolations++;
			invariantViolations++;
		}
		if (!expectationsSet) {
			return;
		}
		boolean unexpected =
				currentCalls[PerformanceMetric.STANDARD_RENDER.ordinal()] != expectedStandard
				|| currentCalls[PerformanceMetric.DOMEMASTER.ordinal()] != expectedDomemaster
				|| currentCalls[PerformanceMetric.EQUIRECTANGULAR.ordinal()] != expectedEquirectangular
				|| currentCalls[PerformanceMetric.SKYBOX.ordinal()] != expectedSkybox
				|| currentCalls[PerformanceMetric.PREVIEW_COPY.ordinal()] != expectedPreviewCopies;
		if (unexpected) {
			unexpectedPassViolations++;
			invariantViolations++;
		}
	}

	private static void validateCapacity(int sampleCapacity) {
		if (sampleCapacity < MINIMUM_CAPACITY || sampleCapacity > MAXIMUM_CAPACITY) {
			throw new IllegalArgumentException(
					"Sample capacity must be between " + MINIMUM_CAPACITY + " and " + MAXIMUM_CAPACITY + ".");
		}
	}

	private void advanceSession() {
		sessionId = sessionId == Long.MAX_VALUE ? 1L : sessionId + 1L;
	}

	public static PerformanceMonitor current() {
		return CURRENT.get();
	}

	/** Installs a monitor for nested render/output code and returns the previous value. */
	public static PerformanceMonitor attach(PerformanceMonitor monitor) {
		PerformanceMonitor previous = CURRENT.get();
		CURRENT.set(monitor);
		return previous;
	}

	public static void restore(PerformanceMonitor monitor) {
		if (monitor == null) {
			CURRENT.remove();
		} else {
			CURRENT.set(monitor);
		}
	}
}
