package com.victorvalentim.zividomelive.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RollingStatisticsTest {

	@Test
	void emptySamplesProduceFiniteZeroStatistics() {
		RollingStatistics.Summary summary = RollingStatistics.summarize(
				PerformanceMetric.FRAME_TOTAL, new long[0], new int[0], 0);

		assertEquals(0.0, summary.averageMilliseconds());
		assertEquals(0.0, summary.p99Milliseconds());
		assertEquals(0.0, summary.averageFps());
		assertEquals(0.0, summary.onePercentLowFps());
	}

	@Test
	void nearestRankPercentilesAndOnePercentLowUseSlowFrames() {
		long[] sorted = new long[100];
		for (int index = 0; index < sorted.length; index++) {
			sorted[index] = (index + 1L) * 1_000_000L;
		}

		assertEquals(50_000_000L, RollingStatistics.percentile(sorted, 0.50));
		assertEquals(95_000_000L, RollingStatistics.percentile(sorted, 0.95));
		assertEquals(99_000_000L, RollingStatistics.percentile(sorted, 0.99));
		assertEquals(10.0, RollingStatistics.onePercentLowFps(sorted), 0.000001);
	}

	@Test
	void onePercentLowRoundsUpForNonHundredSampleRuns() {
		long[] sorted = {10_000_000L, 20_000_000L, 30_000_000L};
		assertEquals(1_000.0 / 30.0, RollingStatistics.onePercentLowFps(sorted), 0.000001);
	}
}
