package com.victorvalentim.zividomelive.performance;

import java.util.List;

/**
 * Read-only report of retained performance samples.
 *
 * <p>This is Experimental API. Snapshots are created by the runtime and obtained through
 * {@link com.victorvalentim.zividomelive.ziviDomeLive#getPerformanceSnapshot()}.</p>

 * <p>A snapshot is immutable. Creating one allocates and sorts retained sample copies, so obtain
 * it outside the interval being measured.</p>
 *
 * <p><strong>API stability:</strong> Experimental.</p>
 *
 * @since 2.0.0
 */
public interface PerformanceSnapshot {

	/** @return profiling mode requested when this collection session was enabled */
	PerformanceMode getRequestedMode();

	/** @return mode effectively used after runtime capability fallback */
	PerformanceMode getEffectiveMode();

	/** @return completed frames since the latest statistics reset, including overwritten frames */
	long getTotalFrames();

	/** @return chronological frames retained by the configured ring-buffer capacity */
	int getStoredFrames();

	/** @return completed frames displaced from the bounded ring buffer */
	long getOverwrittenFrames();

	/**
	 * Aggregates CPU-observed values for one metric over retained frames.
	 *
	 * @param metric non-null metric
	 * @return immutable aggregate statistics
	 * @throws IllegalArgumentException when {@code metric} is null
	 */
	MetricStatistics getStatistics(PerformanceMetric metric);

	/**
	 * Aggregates GPU values for one metric over retained frames.
	 * Metrics without GPU instrumentation return an empty aggregate, never CPU values.
	 *
	 * @param metric non-null metric
	 * @return immutable GPU aggregate statistics
	 * @throws IllegalArgumentException when {@code metric} is null
	 */
	MetricStatistics getGpuStatistics(PerformanceMetric metric);

	/** @return whether at least one retained render-pipeline GPU sample is available */
	boolean hasGpuTimings();

	/**
	 * @param metric non-null sampled metric
	 * @param frameIndex zero-based chronological retained-frame index
	 * @return CPU-observed duration in nanoseconds; zero means no recorded duration
	 * @throws IllegalArgumentException when {@code metric} is null
	 * @throws IndexOutOfBoundsException when {@code frameIndex} is not retained
	 */
	long getDurationNanos(PerformanceMetric metric, int frameIndex);

	/**
	 * @param metric non-null sampled metric
	 * @param frameIndex zero-based chronological retained-frame index
	 * @return calls recorded for that metric and frame
	 * @throws IllegalArgumentException when {@code metric} is null
	 * @throws IndexOutOfBoundsException when {@code frameIndex} is not retained
	 */
	int getCalls(PerformanceMetric metric, int frameIndex);

	/**
	 * @param metric non-null sampled metric
	 * @param frameIndex zero-based chronological retained-frame index
	 * @return GPU duration in nanoseconds; zero may mean unavailable rather than zero GPU cost
	 * @throws IllegalArgumentException when {@code metric} is null
	 * @throws IndexOutOfBoundsException when {@code frameIndex} is not retained
	 */
	long getGpuDurationNanos(PerformanceMetric metric, int frameIndex);

	/**
	 * @param metric non-null sampled metric
	 * @param frameIndex zero-based chronological retained-frame index
	 * @return GPU results recorded for the frame; zero means unavailable
	 * @throws IllegalArgumentException when {@code metric} is null
	 * @throws IndexOutOfBoundsException when {@code frameIndex} is not retained
	 */
	int getGpuCalls(PerformanceMetric metric, int frameIndex);

	/** @return total render-graph invariant violations detected in the collection session */
	long getInvariantViolations();

	/** @return cubemap-capture invariant violations detected in the collection session */
	long getCubemapCaptureViolations();

	/** @return unexpected render-pass violations detected in the collection session */
	long getUnexpectedPassViolations();

	/** @return immutable human-readable collection diagnostics */
	List<String> getDiagnostics();

	/** @return GPU timer ownership and fallback policy requested for the session */
	GpuTimerPolicy getGpuTimerPolicy();

	/** @return GPU measurement backend effectively used, or {@link GpuTimerBackend#NONE} */
	GpuTimerBackend getGpuTimerBackend();

	/** @return normalized host/renderer architecture recorded during backend selection */
	GpuTimerArchitecture getGpuTimerArchitecture();

	/** Read-only aggregate values for one metric over retained frames. */
	interface MetricStatistics {

		/** @return retained frames containing at least one call or non-zero duration */
		int getSampledFrames();

		/** @return calls recorded across all retained frames */
		long getTotalCalls();

		/** @return arithmetic mean call count across all retained frames */
		double getAverageCallsPerFrame();

		/** @return arithmetic mean duration across sampled frames, in milliseconds */
		double getAverageMilliseconds();

		/** @return nearest-rank median duration, in milliseconds */
		double getP50Milliseconds();

		/** @return nearest-rank 95th-percentile duration, in milliseconds */
		double getP95Milliseconds();

		/** @return nearest-rank 99th-percentile duration, in milliseconds */
		double getP99Milliseconds();

		/** @return maximum retained duration, in milliseconds */
		double getMaximumMilliseconds();

		/**
		 * @return frames per second derived from average duration for
		 *         {@link PerformanceMetric#FRAME_TOTAL}, otherwise zero
		 */
		double getAverageFps();

		/**
		 * @return FPS derived from the slowest one percent of retained
		 *         {@link PerformanceMetric#FRAME_TOTAL} samples, otherwise zero
		 */
		double getOnePercentLowFps();

		/** @return frame-total samples strictly over 16.67 milliseconds */
		long getFramesOver16Point67Milliseconds();

		/** @return frame-total samples strictly over 33.33 milliseconds */
		long getFramesOver33Point33Milliseconds();

		/** @return frame-total samples strictly over 50 milliseconds */
		long getFramesOver50Milliseconds();
	}
}
