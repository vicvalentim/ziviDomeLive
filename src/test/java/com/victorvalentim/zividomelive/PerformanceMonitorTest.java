package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.performance.PerformanceMetric;
import com.victorvalentim.zividomelive.performance.PerformanceMode;
import com.victorvalentim.zividomelive.performance.PerformanceSnapshot;
import com.victorvalentim.zividomelive.performance.GpuTimerArchitecture;
import com.victorvalentim.zividomelive.performance.GpuTimerBackend;
import com.victorvalentim.zividomelive.performance.GpuTimerPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceMonitorTest {

	@Test
	void offModeDoesNotCollectFramesOrReadTheClock() {
		PerformanceMonitor monitor = new PerformanceMonitor();

		monitor.beginFrame(1L);
		monitor.beginFrame(10_000_001L);

		assertFalse(monitor.isEnabled());
		assertEquals(0L, monitor.start());
		assertEquals(0L, monitor.snapshot().getTotalFrames());
	}

	@Test
	void frameStatisticsUseNanosecondsAndNearestRankPercentiles() {
		PerformanceMonitor monitor = new PerformanceMonitor();
		monitor.enable(PerformanceMode.CPU, 8);

		monitor.beginFrame(1L);
		monitor.beginFrame(10_000_001L);
		monitor.beginFrame(30_000_001L);
		monitor.beginFrame(70_000_001L);
		monitor.beginFrame(170_000_001L);

		PerformanceSnapshot snapshot = monitor.snapshot();
		PerformanceSnapshot.MetricStatistics frame =
				snapshot.getStatistics(PerformanceMetric.FRAME_TOTAL);

		assertEquals(4, snapshot.getStoredFrames());
		assertEquals(42.5, frame.getAverageMilliseconds(), 0.000001);
		assertEquals(20.0, frame.getP50Milliseconds(), 0.000001);
		assertEquals(100.0, frame.getP95Milliseconds(), 0.000001);
		assertEquals(100.0, frame.getP99Milliseconds(), 0.000001);
		assertEquals(100.0, frame.getMaximumMilliseconds(), 0.000001);
		assertEquals(1_000.0 / 42.5, frame.getAverageFps(), 0.000001);
		assertEquals(10.0, frame.getOnePercentLowFps(), 0.000001);
		assertEquals(3L, frame.getFramesOver16Point67Milliseconds());
		assertEquals(2L, frame.getFramesOver33Point33Milliseconds());
		assertEquals(1L, frame.getFramesOver50Milliseconds());
	}

	@Test
	void ringBufferRetainsChronologicalNewestSamples() {
		PerformanceMonitor monitor = new PerformanceMonitor();
		monitor.enable(PerformanceMode.CPU, 2);

		monitor.beginFrame(1L);
		monitor.beginFrame(10_000_001L);
		monitor.beginFrame(30_000_001L);
		monitor.beginFrame(60_000_001L);

		PerformanceSnapshot snapshot = monitor.snapshot();
		assertEquals(3L, snapshot.getTotalFrames());
		assertEquals(2, snapshot.getStoredFrames());
		assertEquals(1L, snapshot.getOverwrittenFrames());
		assertEquals(20_000_000L, snapshot.getDurationNanos(PerformanceMetric.FRAME_TOTAL, 0));
		assertEquals(30_000_000L, snapshot.getDurationNanos(PerformanceMetric.FRAME_TOTAL, 1));
	}

	@Test
	void passCountsAndInvariantViolationsAreRetained() {
		PerformanceMonitor monitor = new PerformanceMonitor();
		monitor.enable(PerformanceMode.CPU, 4);
		monitor.beginFrame(1L);
		monitor.setExpectedPassCounts(1, 0, 0, 0, 0, 0);
		monitor.recordDuration(PerformanceMetric.STANDARD_RENDER, 2_000_000L);
		monitor.recordDuration(PerformanceMetric.STANDARD_RENDER, 3_000_000L);
		monitor.count(PerformanceMetric.CUBEMAP_TOTAL);
		monitor.count(PerformanceMetric.CUBEMAP_TOTAL);
		monitor.beginFrame(10_000_001L);

		PerformanceSnapshot snapshot = monitor.snapshot();
		PerformanceSnapshot.MetricStatistics standard =
				snapshot.getStatistics(PerformanceMetric.STANDARD_RENDER);
		assertEquals(2L, standard.getTotalCalls());
		assertEquals(2.0, standard.getAverageCallsPerFrame(), 0.0);
		assertEquals(5.0, standard.getAverageMilliseconds(), 0.000001);
		assertEquals(2L, snapshot.getInvariantViolations());
		assertEquals(1L, snapshot.getCubemapCaptureViolations());
		assertEquals(1L, snapshot.getUnexpectedPassViolations());
		assertEquals(2, snapshot.getDiagnostics().size());
	}

	@Test
	void matchingPassCountsDoNotProduceInvariantViolations() {
		PerformanceMonitor monitor = new PerformanceMonitor();
		monitor.enable(PerformanceMode.CPU, 4);
		monitor.beginFrame(1L);
		monitor.setExpectedPassCounts(1, 1, 1, 0, 0, 1);
		monitor.count(PerformanceMetric.STANDARD_RENDER);
		monitor.count(PerformanceMetric.CUBEMAP_TOTAL);
		monitor.count(PerformanceMetric.DOMEMASTER);
		monitor.count(PerformanceMetric.PREVIEW_COPY);
		monitor.beginFrame(10L);

		PerformanceSnapshot snapshot = monitor.snapshot();
		assertEquals(0L, snapshot.getInvariantViolations());
		assertTrue(snapshot.getDiagnostics().isEmpty());
	}

	@Test
	void workerMetricsAreDrainedAtTheNextFrameBoundary() {
		PerformanceMonitor monitor = new PerformanceMonitor();
		monitor.enable(PerformanceMode.CPU, 4);
		monitor.beginFrame(1L);
		monitor.recordConcurrent(PerformanceMetric.NDI_CONVERSION, 2_000_000L);
		monitor.recordConcurrent(PerformanceMetric.NDI_SEND, 3_000_000L);
		monitor.beginFrame(10L);

		PerformanceSnapshot snapshot = monitor.snapshot();
		assertEquals(2_000_000L,
				snapshot.getDurationNanos(PerformanceMetric.NDI_CONVERSION, 0));
		assertEquals(1, snapshot.getCalls(PerformanceMetric.NDI_CONVERSION, 0));
		assertEquals(3_000_000L,
				snapshot.getDurationNanos(PerformanceMetric.NDI_SEND, 0));
		assertEquals(1, snapshot.getCalls(PerformanceMetric.NDI_SEND, 0));
	}

	@Test
	void resetRejectsWorkerSamplesFromThePreviousSession() {
		PerformanceMonitor monitor = new PerformanceMonitor();
		monitor.enable(PerformanceMode.CPU, 4);
		long warmupSession = monitor.getActiveSessionId();
		monitor.reset();
		monitor.beginFrame(1L);
		monitor.recordConcurrent(
				PerformanceMetric.NDI_SEND,
				3_000_000L,
				warmupSession);
		monitor.beginFrame(10L);

		PerformanceSnapshot snapshot = monitor.snapshot();
		assertEquals(0L, snapshot.getDurationNanos(PerformanceMetric.NDI_SEND, 0));
		assertEquals(0, snapshot.getCalls(PerformanceMetric.NDI_SEND, 0));
	}

	@Test
	void cpuGpuRequestFallsBackTruthfullyAndCapacityIsValidated() {
		PerformanceMonitor monitor = new PerformanceMonitor();

		monitor.enable(PerformanceMode.CPU_GPU, 4);
		PerformanceSnapshot snapshot = monitor.snapshot();

		assertEquals(PerformanceMode.CPU_GPU, snapshot.getRequestedMode());
		assertEquals(PerformanceMode.CPU, snapshot.getEffectiveMode());
		assertTrue(snapshot.getDiagnostics().stream()
				.anyMatch(message -> message.contains("qualified GPU measurement backend")));
		assertThrows(IllegalArgumentException.class,
				() -> monitor.enable(PerformanceMode.CPU, 1));
		assertThrows(IllegalArgumentException.class,
				() -> monitor.enable(PerformanceMode.CPU, PerformanceMonitor.MAXIMUM_CAPACITY + 1));
	}

	@Test
	void delayedGpuResultIsAlignedWithItsOriginalCommittedFrame() {
		PerformanceMonitor monitor = new PerformanceMonitor();
		monitor.enable(PerformanceMode.CPU_GPU, 4, GpuTimerPolicy.ARCHITECTURE_AWARE);
		long sessionId = monitor.getActiveSessionId();

		monitor.beginFrame(1L);
		assertEquals(0L, monitor.getCurrentFrameId());
		monitor.activateGpu(
				sessionId,
				GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE,
				GpuTimerArchitecture.APPLE_SILICON);
		monitor.beginFrame(10_000_001L);
		assertTrue(monitor.recordGpuDuration(
				PerformanceMetric.RENDER_PIPELINE,
				0L,
				4_000_000L,
				sessionId));

		PerformanceSnapshot snapshot = monitor.snapshot();
		assertEquals(PerformanceMode.CPU_GPU, snapshot.getEffectiveMode());
		assertEquals(GpuTimerPolicy.ARCHITECTURE_AWARE, snapshot.getGpuTimerPolicy());
		assertEquals(GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE, snapshot.getGpuTimerBackend());
		assertEquals(GpuTimerArchitecture.APPLE_SILICON, snapshot.getGpuTimerArchitecture());
		assertTrue(snapshot.hasGpuTimings());
		assertEquals(4_000_000L,
				snapshot.getGpuDurationNanos(PerformanceMetric.RENDER_PIPELINE, 0));
		assertEquals(1, snapshot.getGpuCalls(PerformanceMetric.RENDER_PIPELINE, 0));
		assertEquals(4.0,
				snapshot.getGpuStatistics(PerformanceMetric.RENDER_PIPELINE)
						.getAverageMilliseconds(),
				0.000001);
		assertEquals(0L,
				snapshot.getGpuStatistics(PerformanceMetric.CUBEMAP_TOTAL).getTotalCalls());
	}

	@Test
	void lateGpuResultNeverOverwritesAReusedRingSlot() {
		PerformanceMonitor monitor = new PerformanceMonitor();
		monitor.enable(PerformanceMode.CPU_GPU, 2);
		long sessionId = monitor.getActiveSessionId();
		monitor.activateGpu(sessionId);

		monitor.beginFrame(1L);
		monitor.beginFrame(2L);
		monitor.beginFrame(3L);
		monitor.beginFrame(4L);

		assertFalse(monitor.recordGpuDuration(
				PerformanceMetric.RENDER_PIPELINE,
				0L,
				5_000_000L,
				sessionId));
		monitor.countDroppedGpuSample(sessionId);

		PerformanceSnapshot snapshot = monitor.snapshot();
		assertFalse(snapshot.hasGpuTimings());
		assertTrue(snapshot.getDiagnostics().stream()
				.anyMatch(message -> message.contains("GPU samples dropped")));
	}

	@Test
	void resetClearsThePreviouslySelectedGpuBackendUntilTheNewSessionBegins() {
		PerformanceMonitor monitor = new PerformanceMonitor();
		monitor.enable(PerformanceMode.CPU_GPU, 4, GpuTimerPolicy.ARCHITECTURE_AWARE);
		monitor.activateGpu(
				monitor.getActiveSessionId(),
				GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE,
				GpuTimerArchitecture.APPLE_SILICON);

		monitor.reset();
		PerformanceSnapshot snapshot = monitor.snapshot();

		assertEquals(PerformanceMode.CPU, snapshot.getEffectiveMode());
		assertEquals(GpuTimerBackend.NONE, snapshot.getGpuTimerBackend());
		assertEquals(GpuTimerArchitecture.OTHER, snapshot.getGpuTimerArchitecture());
	}

	@Test
	void disablePreservesCompletedSamplesAndResetClearsThem() {
		PerformanceMonitor monitor = new PerformanceMonitor();
		monitor.enable(PerformanceMode.CPU, 4);
		monitor.beginFrame(1L);
		monitor.beginFrame(10L);
		monitor.disable();

		assertEquals(1L, monitor.snapshot().getTotalFrames());
		monitor.reset();
		assertEquals(0L, monitor.snapshot().getTotalFrames());
	}
}
