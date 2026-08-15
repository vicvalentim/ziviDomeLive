package com.victorvalentim.zividomelive.performance;

import java.util.List;

/**
 * Immutable copy of collected performance samples.
 *
 * <p>Snapshot creation and percentile aggregation allocate memory by design and
 * should happen outside a measured interval. Runtime collection itself uses
 * preallocated primitive buffers.</p>
 *
 * @since 2.0.0
 */
public final class PerformanceSnapshot {
	private final PerformanceMode requestedMode;
	private final PerformanceMode effectiveMode;
	private final long totalFrames;
	private final int storedFrames;
	private final long overwrittenFrames;
	private final long[][] durationsNanos;
	private final int[][] calls;
	private final MetricStatistics[] statistics;
	private final long invariantViolations;
	private final long cubemapCaptureViolations;
	private final long unexpectedPassViolations;
	private final List<String> diagnostics;

	/**
	 * Creates a snapshot from runtime-owned sample arrays.
	 *
	 * <p>This constructor is public only to bridge the internal recorder package;
	 * applications should obtain snapshots from the ziviDomeLive facade.</p>
	 *
	 * @param requestedMode mode requested by the application
	 * @param effectiveMode mode actually used by the recorder
	 * @param totalFrames completed frames since the latest reset
	 * @param storedFrames completed frames retained in the ring buffer
	 * @param overwrittenFrames completed frames overwritten by the ring buffer
	 * @param durationsNanos chronological metric durations indexed by metric ordinal
	 * @param calls chronological metric call counts indexed by metric ordinal
	 * @param invariantViolations total detected render-graph invariant violations
	 * @param cubemapCaptureViolations detected cubemap-capture invariant violations
	 * @param unexpectedPassViolations detected unexpected render-pass violations
	 * @param diagnostics immutable human-readable collection diagnostics
	 */
	public PerformanceSnapshot(
			PerformanceMode requestedMode,
			PerformanceMode effectiveMode,
			long totalFrames,
			int storedFrames,
			long overwrittenFrames,
			long[][] durationsNanos,
			int[][] calls,
			long invariantViolations,
			long cubemapCaptureViolations,
			long unexpectedPassViolations,
			List<String> diagnostics) {
		this.requestedMode = requestedMode;
		this.effectiveMode = effectiveMode;
		this.totalFrames = totalFrames;
		this.storedFrames = storedFrames;
		this.overwrittenFrames = overwrittenFrames;
		this.durationsNanos = deepCopy(durationsNanos);
		this.calls = deepCopy(calls);
		this.invariantViolations = invariantViolations;
		this.cubemapCaptureViolations = cubemapCaptureViolations;
		this.unexpectedPassViolations = unexpectedPassViolations;
		this.diagnostics = List.copyOf(diagnostics);
		this.statistics = createStatistics();
	}

	/** @return profiling mode requested by the application */
	public PerformanceMode getRequestedMode() {
		return requestedMode;
	}

	/** @return mode actually used to collect the retained samples */
	public PerformanceMode getEffectiveMode() {
		return effectiveMode;
	}

	/** @return every completed frame since the latest reset, including overwritten samples */
	public long getTotalFrames() {
		return totalFrames;
	}

	/** @return samples retained by the configured ring-buffer capacity */
	public int getStoredFrames() {
		return storedFrames;
	}

	/** @return number of completed samples displaced by the ring buffer */
	public long getOverwrittenFrames() {
		return overwrittenFrames;
	}

	/**
	 * Returns aggregate values for one metric.
	 *
	 * @param metric metric to aggregate
	 * @return immutable aggregate statistics
	 */
	public MetricStatistics getStatistics(PerformanceMetric metric) {
		if (metric == null) {
			throw new IllegalArgumentException("Performance metric cannot be null.");
		}
		return statistics[metric.ordinal()];
	}

	/**
	 * Returns one chronological raw duration sample without exposing mutable storage.
	 *
	 * @param metric sampled metric
	 * @param frameIndex chronological retained-frame index
	 * @return duration in nanoseconds
	 */
	public long getDurationNanos(PerformanceMetric metric, int frameIndex) {
		checkMetric(metric);
		checkFrameIndex(frameIndex);
		return durationsNanos[metric.ordinal()][frameIndex];
	}

	/**
	 * Returns one chronological per-frame call count.
	 *
	 * @param metric sampled metric
	 * @param frameIndex chronological retained-frame index
	 * @return calls recorded for that frame
	 */
	public int getCalls(PerformanceMetric metric, int frameIndex) {
		checkMetric(metric);
		checkFrameIndex(frameIndex);
		return calls[metric.ordinal()][frameIndex];
	}

	/** @return all detected render-graph invariant violations */
	public long getInvariantViolations() {
		return invariantViolations;
	}

	/** @return detected cubemap-capture invariant violations */
	public long getCubemapCaptureViolations() {
		return cubemapCaptureViolations;
	}

	/** @return detected unexpected render-pass violations */
	public long getUnexpectedPassViolations() {
		return unexpectedPassViolations;
	}

	/** @return immutable human-readable collection diagnostics */
	public List<String> getDiagnostics() {
		return diagnostics;
	}

	private void checkFrameIndex(int frameIndex) {
		if (frameIndex < 0 || frameIndex >= storedFrames) {
			throw new IndexOutOfBoundsException("Frame index out of range: " + frameIndex);
		}
	}

	private static void checkMetric(PerformanceMetric metric) {
		if (metric == null) {
			throw new IllegalArgumentException("Performance metric cannot be null.");
		}
	}

	private MetricStatistics[] createStatistics() {
		PerformanceMetric[] metrics = PerformanceMetric.values();
		MetricStatistics[] result = new MetricStatistics[metrics.length];
		for (PerformanceMetric metric : metrics) {
			result[metric.ordinal()] = aggregate(
					metric,
					durationsNanos[metric.ordinal()],
					calls[metric.ordinal()],
					storedFrames);
		}
		return result;
	}

	private static MetricStatistics aggregate(
			PerformanceMetric metric,
			long[] durations,
			int[] callCounts,
			int frameCount) {
		RollingStatistics.Summary summary = RollingStatistics.summarize(
				metric, durations, callCounts, frameCount);
		return new MetricStatistics(
				summary.sampledFrames(),
				summary.totalCalls(),
				summary.averageCallsPerFrame(),
				summary.averageMilliseconds(),
				summary.p50Milliseconds(),
				summary.p95Milliseconds(),
				summary.p99Milliseconds(),
				summary.maximumMilliseconds(),
				summary.averageFps(),
				summary.onePercentLowFps(),
				summary.framesOver16Point67Milliseconds(),
				summary.framesOver33Point33Milliseconds(),
				summary.framesOver50Milliseconds());
	}

	private static long[][] deepCopy(long[][] source) {
		long[][] copy = new long[source.length][];
		for (int index = 0; index < source.length; index++) {
			copy[index] = source[index].clone();
		}
		return copy;
	}

	private static int[][] deepCopy(int[][] source) {
		int[][] copy = new int[source.length][];
		for (int index = 0; index < source.length; index++) {
			copy[index] = source[index].clone();
		}
		return copy;
	}

	/** Aggregated values for one metric over retained frames. */
	public static final class MetricStatistics {
		private final int sampledFrames;
		private final long totalCalls;
		private final double averageCallsPerFrame;
		private final double averageMilliseconds;
		private final double p50Milliseconds;
		private final double p95Milliseconds;
		private final double p99Milliseconds;
		private final double maximumMilliseconds;
		private final double averageFps;
		private final double onePercentLowFps;
		private final long framesOver16Point67Milliseconds;
		private final long framesOver33Point33Milliseconds;
		private final long framesOver50Milliseconds;

		private MetricStatistics(
				int sampledFrames,
				long totalCalls,
				double averageCallsPerFrame,
				double averageMilliseconds,
				double p50Milliseconds,
				double p95Milliseconds,
				double p99Milliseconds,
				double maximumMilliseconds,
				double averageFps,
				double onePercentLowFps,
				long framesOver16Point67Milliseconds,
				long framesOver33Point33Milliseconds,
				long framesOver50Milliseconds) {
			this.sampledFrames = sampledFrames;
			this.totalCalls = totalCalls;
			this.averageCallsPerFrame = averageCallsPerFrame;
			this.averageMilliseconds = averageMilliseconds;
			this.p50Milliseconds = p50Milliseconds;
			this.p95Milliseconds = p95Milliseconds;
			this.p99Milliseconds = p99Milliseconds;
			this.maximumMilliseconds = maximumMilliseconds;
			this.averageFps = averageFps;
			this.onePercentLowFps = onePercentLowFps;
			this.framesOver16Point67Milliseconds = framesOver16Point67Milliseconds;
			this.framesOver33Point33Milliseconds = framesOver33Point33Milliseconds;
			this.framesOver50Milliseconds = framesOver50Milliseconds;
		}

		/** @return retained frames used by this aggregate */
		public int getSampledFrames() { return sampledFrames; }
		/** @return calls recorded across retained frames */
		public long getTotalCalls() { return totalCalls; }
		/** @return arithmetic mean calls per retained frame */
		public double getAverageCallsPerFrame() { return averageCallsPerFrame; }
		/** @return arithmetic mean duration in milliseconds */
		public double getAverageMilliseconds() { return averageMilliseconds; }
		/** @return nearest-rank median duration in milliseconds */
		public double getP50Milliseconds() { return p50Milliseconds; }
		/** @return nearest-rank 95th-percentile duration in milliseconds */
		public double getP95Milliseconds() { return p95Milliseconds; }
		/** @return nearest-rank 99th-percentile duration in milliseconds */
		public double getP99Milliseconds() { return p99Milliseconds; }
		/** @return maximum retained duration in milliseconds */
		public double getMaximumMilliseconds() { return maximumMilliseconds; }
		/** @return frames per second derived from the average duration */
		public double getAverageFps() { return averageFps; }
		/** @return FPS derived from the slowest one percent of retained samples */
		public double getOnePercentLowFps() { return onePercentLowFps; }
		/** @return samples strictly over 16.67 milliseconds */
		public long getFramesOver16Point67Milliseconds() { return framesOver16Point67Milliseconds; }
		/** @return samples strictly over 33.33 milliseconds */
		public long getFramesOver33Point33Milliseconds() { return framesOver33Point33Milliseconds; }
		/** @return samples strictly over 50 milliseconds */
		public long getFramesOver50Milliseconds() { return framesOver50Milliseconds; }
	}
}
