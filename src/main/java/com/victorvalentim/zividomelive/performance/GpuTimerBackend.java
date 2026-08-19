package com.victorvalentim.zividomelive.performance;

/** GPU measurement mechanism effectively selected for a profiling session. */
public enum GpuTimerBackend {
	/** No GPU measurement is active; CPU profiling remains available. */
	NONE,
	/** Two asynchronous {@code GL_TIMESTAMP} queries delimit GPU execution. */
	TIMESTAMP_PAIR,
	/** One exclusive asynchronous {@code GL_TIME_ELAPSED} query delimits GPU execution. */
	TIME_ELAPSED_EXCLUSIVE,
	/**
	 * An asynchronous GL fence reports CPU-observed completion latency.
	 *
	 * <p>This backend is deliberately distinct from timer-query backends: it does not expose a
	 * GPU clock duration. It measures the latency from the CPU-side beginning of the profiled
	 * interval until a later non-blocking poll observes the end fence as complete.</p>
	 */
	FENCE_COMPLETION
}
