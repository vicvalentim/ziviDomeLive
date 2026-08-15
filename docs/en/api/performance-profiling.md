# Performance Profiling

The experimental performance API provides low-overhead CPU instrumentation for development and qualification tools. It is disabled by default and does not replace external GPU profilers.

## Enable And Read

```java
dome.enablePerformanceProfiling(PerformanceMode.CPU, 4096);

// Run warm-up, then reset before the measured interval.
dome.resetPerformanceStatistics();

PerformanceSnapshot snapshot = dome.getPerformanceSnapshot();
PerformanceSnapshot.MetricStatistics frame =
    snapshot.getStatistics(PerformanceMetric.FRAME_TOTAL);

println(frame.getAverageMilliseconds());
println(frame.getP95Milliseconds());
println(frame.getOnePercentLowFps());
```

Snapshot creation copies and sorts retained samples. Request snapshots only outside the interval being measured. The first `pre()` boundary establishes a baseline; the first completed `FRAME_TOTAL` sample appears at the next boundary.

## Modes And Overhead

- `OFF`: no `System.nanoTime()`, sample writes, or profiler atomics on the render path.
- `CPU`: CPU-observed wall time from `System.nanoTime()`.
- `CPU_GPU`: reserved for asynchronous, capability-gated OpenGL timer queries. Version 2.0 currently reports CPU as the effective mode and adds a diagnostic instead of synchronizing the GPU.

The OFF path performs only predictable inactive-monitor checks at instrumented boundaries. Sample arrays and worker accumulators are allocated only when profiling is enabled.

## Interpretation

`FRAME_TOTAL` is the interval between consecutive Processing `pre()` boundaries. It is the primary source for average FPS, P50, P95, P99, maximum frame time, 1% low, and the 16.67/33.33/50 ms threshold counts.

OpenGL-related CPU durations measure submission plus any driver wait observed by the caller. They are not isolated GPU elapsed times. Likewise, NDI send duration is the native sender-call duration, not end-to-end network latency; Syphon and Spout durations do not measure receiver presentation.

Each metric also reports total calls and average calls per retained frame. This makes pass-count invariants observable independently from timing. The monitor records violations when cubemap capture exceeds or differs from the required count, or when Standard/projection/preview-copy calls differ from the current requirement closure.

## Storage

Samples use a preallocated primitive ring buffer. When its capacity is exceeded, the oldest frames are overwritten and `getOverwrittenFrames()` reports the count. Raw samples remain available chronologically through `getDurationNanos()` and `getCalls()` for later JSON/CSV export.

Calling `disablePerformanceProfiling()` stops collection without discarding completed samples. Calling `resetPerformanceStatistics()` clears timings and invariant counters.
