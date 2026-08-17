---
title: "Performance Internals"
icon: material/source-branch
---
# Performance Internals


Performance diagnostics must distinguish CPU observation from GPU execution.

## CPU measurements

CPU wall time can identify expensive CPU-side sections and blocking behavior, but does not by itself measure asynchronous GPU execution.

## GPU measurements

Only report GPU elapsed metrics that the current implementation actually records. Do not label a CPU interval as “GPU time” and do not promise per-stage GPU timer-query coverage when only coarse measurement exists.

## Allocation/readback boundaries

Keep large render targets resident on the GPU for normal rendering/preview/native sharing. CPU readback is a distinct cost boundary required by transports such as the current NDI path.

## Qualification

Use `BenchmarkTool` and recorded environment metadata for repeatable comparisons. A benchmark result is evidence for the exact tested configuration, not a universal platform guarantee.
