# Performance Profiling

The experimental performance API provides low-overhead CPU instrumentation plus an optional coarse GPU elapsed measurement for development and qualification tools. It is disabled by default and does not replace external GPU profilers.

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

To request GPU elapsed time, use `PerformanceMode.CPU_GPU`, then inspect the effective mode and
the separate GPU channel:

```java
PerformanceSnapshot snapshot = dome.getPerformanceSnapshot();
if (snapshot.getEffectiveMode() == PerformanceMode.CPU_GPU && snapshot.hasGpuTimings()) {
  PerformanceSnapshot.MetricStatistics gpu =
      snapshot.getGpuStatistics(PerformanceMetric.RENDER_PIPELINE);
  println(gpu.getAverageMilliseconds());
}
```

Snapshot creation copies and sorts retained samples. Request snapshots only outside the interval being measured. The first `pre()` boundary establishes a baseline; the first completed `FRAME_TOTAL` sample appears at the next boundary.

## Modes And Overhead

- `OFF`: no `System.nanoTime()`, sample writes, or profiler atomics on the render path.
- `CPU`: CPU-observed wall time from `System.nanoTime()`.
- `CPU_GPU`: CPU instrumentation plus one asynchronous, capability-gated GPU timestamp interval around `RENDER_PIPELINE`. Unsupported or failed contexts report `CPU` as the effective mode and add a diagnostic.

The OFF path performs only predictable inactive-monitor checks at instrumented boundaries. Sample arrays and worker accumulators are allocated only when profiling is enabled. GPU query objects are allocated lazily only after `CPU_GPU` is requested on the Processing render thread. The active desktop context must report non-zero `GL_TIMESTAMP` counter bits; drivers that expose elapsed queries but no timestamps fall back to CPU. An eight-slot pair pool reads only results advertised by `GL_QUERY_RESULT_AVAILABLE`; saturation, late results, disable, or context loss discard samples instead of waiting. No `glFinish()` is used.

## Interpretation

`FRAME_TOTAL` is the interval between consecutive Processing `pre()` boundaries. It is the primary source for average FPS, P50, P95, P99, maximum frame time, 1% low, and the 16.67/33.33/50 ms threshold counts.

OpenGL-related CPU durations measure submission plus any driver wait observed by the caller. The separate GPU value measures only commands between the complete pipeline timestamp boundaries; it excludes `Scene.update()`, frame pacing, CPU work outside that interval, receiver presentation, and network latency. It is intentionally not split into per-pass intervals because extra `beginPGL()` boundaries would flush and perturb the workload. Timestamp pairs avoid occupying the global `GL_TIME_ELAPSED` target, so scene-owned elapsed queries are not nested inside a library query. NDI send duration remains the native sender-call duration; Syphon and Spout durations do not measure receiver presentation.

Each metric also reports total calls and average calls per retained frame. This makes pass-count invariants observable independently from timing. The monitor records violations when cubemap capture exceeds or differs from the required count, or when Standard/projection/preview-copy calls differ from the current requirement closure.

## Storage

Samples use a preallocated primitive ring buffer. When its capacity is exceeded, the oldest frames are overwritten and `getOverwrittenFrames()` reports the count. Raw CPU samples remain available through `getDurationNanos()` / `getCalls()`; aligned GPU results use `getGpuDurationNanos()` / `getGpuCalls()`. A GPU call count of zero means that frame has no completed query result, not zero GPU cost.

Calling `disablePerformanceProfiling()` stops collection without discarding completed samples. Calling `resetPerformanceStatistics()` clears timings and invariant counters.
