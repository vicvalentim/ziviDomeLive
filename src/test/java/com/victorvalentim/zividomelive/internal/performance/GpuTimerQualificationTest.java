package com.victorvalentim.zividomelive.internal.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GpuTimerQualificationTest {

	@Test
	void acceptsTimerInsideSynchronizedEnvelope() {
		assertTrue(GpuTimerQualification.isPlausible(9_500_000L, 10_000_000L));
	}

	@Test
	void rejectsFourTimesElapsedResult() {
		assertFalse(GpuTimerQualification.isPlausible(40_000_000L, 10_000_000L));
	}

	@Test
	void rejectsZeroOrNegativeResults() {
		assertFalse(GpuTimerQualification.isPlausible(0L, 10_000_000L));
		assertFalse(GpuTimerQualification.isPlausible(1_000_000L, 0L));
	}

	@Test
	void absoluteToleranceProtectsShortIntervals() {
		assertTrue(GpuTimerQualification.isPlausible(1_500_000L, 1_000_000L));
		assertFalse(GpuTimerQualification.isPlausible(2_100_000L, 1_000_000L));
	}

	@Test
	void computesMedianWithoutMutatingInput() {
		double[] values = {1.02, 0.98, 1.00};
		assertEquals(1.00, GpuTimerQualification.median(values, 3), 0.000001);
		assertEquals(1.02, values[0], 0.0);
	}
}
