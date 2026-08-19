package com.victorvalentim.zividomelive.internal.performance;

import java.util.Arrays;

/** Pure validation policy for synchronized OpenGL timer-query qualification probes. */
public final class GpuTimerQualification {
	/** Number of successful synchronized probes required before accepting a timer backend. */
	public static final int REQUIRED_PROBES = 3;
	/** Absolute allowance for scheduling and Java-side boundary overhead. */
	public static final long MIN_ABSOLUTE_TOLERANCE_NANOS = 1_000_000L;
	/** Relative allowance above the synchronized CPU envelope. */
	public static final double RELATIVE_TOLERANCE = 0.15;

	private GpuTimerQualification() {
	}

	/**
	 * Returns whether a GPU timer result is physically plausible inside a synchronized CPU envelope.
	 *
	 * <p>The envelope begins after previous GPU work has completed and ends only after the queried
	 * workload has completed. A timer result therefore cannot legitimately exceed that envelope by
	 * more than the documented tolerance.</p>
	 */
	public static boolean isPlausible(long gpuNanos, long cpuEnvelopeNanos) {
		return gpuNanos > 0L
				&& cpuEnvelopeNanos > 0L
				&& gpuNanos <= upperBound(cpuEnvelopeNanos);
	}

	/** Returns the accepted upper bound for a synchronized CPU envelope. */
	public static long upperBound(long cpuEnvelopeNanos) {
		if (cpuEnvelopeNanos <= 0L) {
			return 0L;
		}
		long relative = Math.round(cpuEnvelopeNanos * RELATIVE_TOLERANCE);
		long tolerance = Math.max(MIN_ABSOLUTE_TOLERANCE_NANOS, relative);
		if (Long.MAX_VALUE - cpuEnvelopeNanos < tolerance) {
			return Long.MAX_VALUE;
		}
		return cpuEnvelopeNanos + tolerance;
	}

	/** Returns {@code gpuNanos / cpuEnvelopeNanos}, or NaN when the envelope is invalid. */
	public static double ratio(long gpuNanos, long cpuEnvelopeNanos) {
		return cpuEnvelopeNanos > 0L
				? (double) gpuNanos / (double) cpuEnvelopeNanos
				: Double.NaN;
	}

	/** Returns the median of the first {@code count} finite values. */
	public static double median(double[] values, int count) {
		if (values == null || count <= 0 || count > values.length) {
			return Double.NaN;
		}
		double[] copy = new double[count];
		int finite = 0;
		for (int index = 0; index < count; index++) {
			if (Double.isFinite(values[index])) {
				copy[finite++] = values[index];
			}
		}
		if (finite == 0) {
			return Double.NaN;
		}
		copy = Arrays.copyOf(copy, finite);
		Arrays.sort(copy);
		int middle = finite / 2;
		return (finite & 1) == 1
				? copy[middle]
				: (copy[middle - 1] + copy[middle]) * 0.5;
	}
}
