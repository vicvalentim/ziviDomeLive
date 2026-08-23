package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.performance.PerformanceMetric;

import java.util.Arrays;

/** Allocation-on-snapshot statistics over primitive runtime samples. */
final class RollingStatistics {
	private static final double NANOS_PER_MILLISECOND = 1_000_000.0;

	private RollingStatistics() {
	}

	static Summary summarize(
			PerformanceMetric metric,
			long[] durations,
			int[] callCounts,
			int frameCount) {
		long totalNanos = 0L;
		long maximumNanos = 0L;
		long totalCalls = 0L;
		int activeFrames = 0;
		long[] activeDurations = new long[frameCount];
		long over16 = 0L;
		long over33 = 0L;
		long over50 = 0L;

		for (int index = 0; index < frameCount; index++) {
			long duration = Math.max(0L, durations[index]);
			int count = Math.max(0, callCounts[index]);
			totalCalls += count;
			if (count == 0 && duration == 0L) {
				continue;
			}
			activeDurations[activeFrames++] = duration;
			totalNanos += duration;
			maximumNanos = Math.max(maximumNanos, duration);
			if (metric == PerformanceMetric.FRAME_TOTAL) {
				if (duration > 16_670_000L) over16++;
				if (duration > 33_330_000L) over33++;
				if (duration > 50_000_000L) over50++;
			}
		}

		long[] sorted = Arrays.copyOf(activeDurations, activeFrames);
		Arrays.sort(sorted);
		double averageNanos = activeFrames == 0 ? 0.0 : totalNanos / (double) activeFrames;
		double averageFps = metric == PerformanceMetric.FRAME_TOTAL && averageNanos > 0.0
				? 1_000_000_000.0 / averageNanos
				: 0.0;

		return new Summary(
				activeFrames,
				totalCalls,
				frameCount == 0 ? 0.0 : totalCalls / (double) frameCount,
				averageNanos / NANOS_PER_MILLISECOND,
				percentile(sorted, 0.50) / NANOS_PER_MILLISECOND,
				percentile(sorted, 0.95) / NANOS_PER_MILLISECOND,
				percentile(sorted, 0.99) / NANOS_PER_MILLISECOND,
				maximumNanos / NANOS_PER_MILLISECOND,
				averageFps,
				metric == PerformanceMetric.FRAME_TOTAL ? onePercentLowFps(sorted) : 0.0,
				over16,
				over33,
				over50);
	}

	static long percentile(long[] sorted, double quantile) {
		if (sorted.length == 0) return 0L;
		int rank = (int) Math.ceil(quantile * sorted.length);
		return sorted[Math.max(0, Math.min(sorted.length - 1, rank - 1))];
	}

	static double onePercentLowFps(long[] sorted) {
		if (sorted.length == 0) return 0.0;
		int sampleCount = Math.max(1, (int) Math.ceil(sorted.length * 0.01));
		double total = 0.0;
		for (int index = sorted.length - sampleCount; index < sorted.length; index++) {
			total += sorted[index];
		}
		double averageWorstNanos = total / sampleCount;
		return averageWorstNanos > 0.0 ? 1_000_000_000.0 / averageWorstNanos : 0.0;
	}

	record Summary(
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
	}
}
