---
title: "Performance Profiling"
icon: material/api
status: experimental
---
# Performance Profiling

!!! warning "Advanced / qualification"
    Performance instrumentation is not required to create or render a normal ziviDomeLive scene. Use it when profiling, benchmarking or qualifying a release/hardware configuration.

## CPU wall time is not GPU elapsed time

CPU wall-time measurements describe time observed by the CPU around a stage. OpenGL work can be queued/asynchronous, so that value must not be described as the GPU execution time of the same stage.

GPU elapsed measurements are only valid where the current implementation actually exposes a GPU timing result. Do not infer per-stage GPU granularity that the API does not provide.

## BenchmarkTool

`BenchmarkTool` is a qualification tool. Use it to produce repeatable evidence for a specific software/hardware configuration; do not present it as a prerequisite for ordinary sketches.

## Reporting rule

A useful report identifies at least:

- ziviDomeLive version/commit under test;
- Processing/Java environment actually used;
- output resolution and active render/output routes;
- benchmark mode/measurement type;
- whether a metric is CPU or GPU derived;
- hardware/OS when the result is used as platform qualification evidence.

Never convert “supported by the code path” into “tested on this platform” without recorded qualification.
