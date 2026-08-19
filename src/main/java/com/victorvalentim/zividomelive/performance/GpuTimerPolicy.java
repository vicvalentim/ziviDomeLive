package com.victorvalentim.zividomelive.performance;

/** Policy controlling OpenGL GPU measurement backend selection. */
public enum GpuTimerPolicy {
	/** Uses timestamp pairs only, avoiding ownership of the global elapsed-query target. */
	SAFE,

	/**
	 * Legacy public name retained for compatibility.
	 *
	 * <p>Selection is capability-driven: timestamp pairs are preferred, elapsed queries are the
	 * timer fallback, and the runtime measurement layer may use fence completion when both timer
	 * paths are unavailable or fail qualification. {@link GpuTimerArchitecture} remains metadata
	 * and is not treated as proof that a backend is reliable.</p>
	 */
	ARCHITECTURE_AWARE,

	/** Explicitly requests the exclusive elapsed-query backend for controlled diagnostics. */
	TIME_ELAPSED_EXCLUSIVE;

	/**
	 * Selects the first timer-query candidate from runtime counter capabilities.
	 *
	 * <p>This method selects a candidate only. Runtime qualification is responsible for deciding
	 * whether the candidate is trustworthy on the active driver/context.</p>
	 *
	 * @param architecture normalized host architecture retained for API compatibility and metadata
	 * @param timestampCounterBits counter bits reported for {@code GL_TIMESTAMP}
	 * @param elapsedCounterBits counter bits reported for {@code GL_TIME_ELAPSED}
	 * @return selected timer candidate, or {@link GpuTimerBackend#NONE}
	 */
	public GpuTimerBackend selectBackend(
			GpuTimerArchitecture architecture,
			int timestampCounterBits,
			int elapsedCounterBits) {
		if (this == TIME_ELAPSED_EXCLUSIVE) {
			return elapsedCounterBits > 0
					? GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE
					: GpuTimerBackend.NONE;
		}
		if (timestampCounterBits > 0) {
			return GpuTimerBackend.TIMESTAMP_PAIR;
		}
		if (this == ARCHITECTURE_AWARE && elapsedCounterBits > 0) {
			return GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE;
		}
		return GpuTimerBackend.NONE;
	}

	/** @return whether a failed timestamp candidate may fall back to an elapsed query */
	public boolean allowsElapsedFallback() {
		return this == ARCHITECTURE_AWARE;
	}

	/** @return whether failed/unavailable timer queries may fall back to fence completion */
	public boolean allowsFenceFallback() {
		return this == ARCHITECTURE_AWARE;
	}
}
