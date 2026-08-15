package com.victorvalentim.zividomelive.performance;

/** Policy controlling ownership and architecture-specific fallback of OpenGL timer queries. */
public enum GpuTimerPolicy {
	/** Uses timestamp pairs only, avoiding ownership of the global elapsed-query target. */
	SAFE,
	/** Prefers timestamps and permits exclusive elapsed queries on Apple Silicon benchmarks. */
	ARCHITECTURE_AWARE,
	/** Explicitly requests the exclusive elapsed-query backend on any supporting architecture. */
	TIME_ELAPSED_EXCLUSIVE;

	/**
	 * Selects a backend from runtime counter capabilities.
	 *
	 * @param architecture normalized host architecture
	 * @param timestampCounterBits counter bits reported for {@code GL_TIMESTAMP}
	 * @param elapsedCounterBits counter bits reported for {@code GL_TIME_ELAPSED}
	 * @return selected backend, or {@link GpuTimerBackend#NONE}
	 */
	public GpuTimerBackend selectBackend(
			GpuTimerArchitecture architecture,
			int timestampCounterBits,
			int elapsedCounterBits) {
		if (this != TIME_ELAPSED_EXCLUSIVE && timestampCounterBits > 0) {
			return GpuTimerBackend.TIMESTAMP_PAIR;
		}
		boolean elapsedAllowed = this == TIME_ELAPSED_EXCLUSIVE
				|| (this == ARCHITECTURE_AWARE
						&& architecture == GpuTimerArchitecture.APPLE_SILICON);
		return elapsedAllowed && elapsedCounterBits > 0
				? GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE
				: GpuTimerBackend.NONE;
	}
}
