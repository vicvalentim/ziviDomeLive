package com.victorvalentim.zividomelive.performance;

/** GPU timer mechanism effectively selected for a profiling session. */
public enum GpuTimerBackend {
	/** No GPU timer is active; CPU profiling remains available. */
	NONE,
	/** Two asynchronous {@code GL_TIMESTAMP} queries delimit the pipeline. */
	TIMESTAMP_PAIR,
	/** One exclusive asynchronous {@code GL_TIME_ELAPSED} query delimits the pipeline. */
	TIME_ELAPSED_EXCLUSIVE
}
