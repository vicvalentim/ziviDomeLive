package com.victorvalentim.zividomelive.performance;

import java.util.List;

/**
 * Read-only report of retained performance samples.
 *
 * <p>This is Experimental API. Snapshots are created by the runtime and obtained through
 * {@code ziviDomeLive.getPerformanceSnapshot()}.</p>
 *
 * @since 2.0.0
 */
public interface PerformanceSnapshot {

	PerformanceMode getRequestedMode();

	PerformanceMode getEffectiveMode();

	long getTotalFrames();

	int getStoredFrames();

	long getOverwrittenFrames();

	MetricStatistics getStatistics(PerformanceMetric metric);

	MetricStatistics getGpuStatistics(PerformanceMetric metric);

	boolean hasGpuTimings();

	long getDurationNanos(PerformanceMetric metric, int frameIndex);

	int getCalls(PerformanceMetric metric, int frameIndex);

	long getGpuDurationNanos(PerformanceMetric metric, int frameIndex);

	int getGpuCalls(PerformanceMetric metric, int frameIndex);

	long getInvariantViolations();

	long getCubemapCaptureViolations();

	long getUnexpectedPassViolations();

	List<String> getDiagnostics();

	GpuTimerPolicy getGpuTimerPolicy();

	GpuTimerBackend getGpuTimerBackend();

	GpuTimerArchitecture getGpuTimerArchitecture();

	/** Read-only aggregate values for one metric over retained frames. */
	interface MetricStatistics {

		int getSampledFrames();

		long getTotalCalls();

		double getAverageCallsPerFrame();

		double getAverageMilliseconds();

		double getP50Milliseconds();

		double getP95Milliseconds();

		double getP99Milliseconds();

		double getMaximumMilliseconds();

		double getAverageFps();

		double getOnePercentLowFps();

		long getFramesOver16Point67Milliseconds();

		long getFramesOver33Point33Milliseconds();

		long getFramesOver50Milliseconds();
	}
}
