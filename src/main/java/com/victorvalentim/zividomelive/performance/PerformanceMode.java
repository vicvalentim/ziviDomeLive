package com.victorvalentim.zividomelive.performance;

/**
 * Selects the amount of runtime performance instrumentation.
 *
 * <p>The performance API is experimental in ziviDomeLive 2.0. CPU/GPU mode uses
 * capability-gated asynchronous OpenGL timer queries for the complete render pipeline.
 * {@link GpuTimerPolicy} controls architecture-specific fallback and query ownership.</p>
 *
 * <p><strong>API stability:</strong> Experimental.</p>
 *
 * @since 2.0.0
 */
public enum PerformanceMode {
	/** No timing or sample collection. */
	OFF,
	/** CPU wall-clock instrumentation based on {@link System#nanoTime()}. */
	CPU,
	/** CPU instrumentation plus optional GPU timers when supported. */
	CPU_GPU
}
