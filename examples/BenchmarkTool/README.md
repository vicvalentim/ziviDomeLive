# BenchmarkTool

`BenchmarkTool` is the quantitative performance example for ziviDomeLive 2.0. It is a normal
Processing sketch: it uses only the public scene, output, and experimental performance APIs.
It does not generate HTML and it never writes files during the measured interval.

## Scenarios

The sketch provides five deterministic synthetic scenes:

- `EMPTY`: baseline scene callback with no geometry.
- `LIGHT`: 24 reusable boxes.
- `MEDIUM`: 180 reusable boxes.
- `HEAVY`: 720 reusable boxes.
- `SPHERICAL_STRESS`: 640 boxes distributed by a deterministic Fibonacci sphere so every
  cubemap face receives useful load.

All transforms, colors, and primitive arrays are prepared before measurement. Geometry uses a
reusable `PShape`; no per-object Java allocation occurs in `sceneRender()`.

## Workflow

1. Select `RenderMode`, output resolution, scene, Preview, supported outputs, and optionally `GPU timer`.
2. Keep the default 600 warm-up / 1800 measurement frames or choose another bounded interval.
3. Press `START`. Warm-up samples are reset before measurement begins.
4. Inspect the immutable snapshot after the run.
5. Press `EXPORT` to write structured results.

Do not manipulate either ControlP5 panel during warm-up or measurement; configuration changes are
not part of a steady-state scenario. Use `STOP`/`X`, then start a new run instead.

`WARM UP` performs only the configured warm-up. `STOP` keeps completed measurement frames but
discards warm-up-only work. `RESET` clears the run. The interactive `RUN SUITE` executes Standard,
Domemaster, Equirectangular, and Skybox at the selected scene and resolution. Automated Gradle
suites additionally provide the full spherical resolution matrix and transition measurements.
Keyboard shortcuts: `X` stops and `E` exports.

The library's regular control panel remains available on the left; press `h` when you want to
hide it. The BenchmarkTool panel is on the right.

## Output Location

When running from the repository, let Gradle discover the legacy or modern Processing launcher:

```bash
./gradlew benchmarkDoctor
./gradlew runBenchmark
```

Gradle searches `PATH` and standard application locations for the current operating system. Use
`-PprocessingExecutable=<path>` or `PROCESSING_EXECUTABLE` only for a custom installation.

When launching the sketch directly instead, set the output root explicitly. Processing 4.4.3 and
newer use the application launcher with the `cli` subcommand:

```bash
export ZIVIDOME_BENCHMARK_OUTPUT="$PWD/build/benchmark-results"
export ZIVIDOME_BENCHMARK_REVISION="$(git rev-parse HEAD)"
/Applications/Processing.app/Contents/MacOS/Processing cli \
  --sketch="$PWD/examples/BenchmarkTool" \
  --output="$PWD/build/processing-benchmark" \
  --force --run
```

For older Processing installations, replace that launcher with `processing-java` and omit `cli`.

The current library must first be installed in the Processing sketchbook, for example with
`./gradlew deployToProcessingSketchbook`. When the output variable/property is absent, the sketch
uses `~/ziviDomeLive-benchmark-results` and shows that absolute path in the interface. It never
falls back to a path inside `examples/`.

Each export creates:

```text
<output-root>/
└── <timestamp>-<mode>-<resolution>-<scene>/
    ├── summary.json
    ├── frames.csv
    └── environment.json
```

Run files use schema version `2` (suite manifests remain version `1`). The report reader remains
compatible with historical schema-v1 runs. `frames.csv` contains one row per retained completed frame:

```text
frame,totalMs,sceneMs,standardMs,cubemapMs,projectionMs,previewMs,outputMs,ndiMs,standardCalls,cubemapCalls,domemasterCalls,equirectangularCalls,skyboxCalls,gpuPipelineMs,gpuPipelineCalls
```

Automated suites also write a root-level `suite-<timestamp>.json` manifest. It records every
scenario as `SUPPORTED`, `UNSUPPORTED`, or `FAILED`, including scenarios skipped because a native
output is unavailable. Transition summaries add the initial/target endpoints, normal P95,
transition maximum, and recovery frames. Recovery is the first post-transition sample at or below
the baseline P95; `-1` means the measured post interval did not recover.

The repository document `docs/en/qualification/automated-benchmarks.md` describes Gradle tasks,
CLI configuration, suite composition, and hardware requirements.

GPU timer backend selection is architecture-aware by default. `TIMESTAMP_PAIR` is preferred when
the active context exposes useful timestamp counter bits. On Apple Silicon, when timestamps report
zero bits but elapsed queries are available, the controlled BenchmarkTool scenes use
`TIME_ELAPSED_EXCLUSIVE`. Override the policy with
`ZIVIDOME_BENCHMARK_GPU_TIMER_POLICY` or `-PbenchmarkGpuTimerPolicy=` using `AUTO`, `SAFE`,
`TIMESTAMP`, `ARCHITECTURE_AWARE`, `ELAPSED`, or `TIME_ELAPSED_EXCLUSIVE`.

## Interpretation And Limitations

- Pipeline timings are CPU-observed wall time. When `GPU timer` is enabled and supported,
  `gpuPipeline` and the final CSV columns contain asynchronous elapsed time for
  `RENDER_PIPELINE` only. A zero `gpuPipelineCalls` value means no result, not zero GPU cost.
- GPU timing uses a bounded query pool, never `glFinish()`, and falls back to CPU with a diagnostic.
  The two Processing GL boundaries flush queued commands and therefore add profiling overhead;
  compare CPU-only runs with CPU-only runs and GPU runs with GPU runs.
- `TIME_ELAPSED_EXCLUSIVE` is restricted to controlled benchmark scenes because OpenGL permits
  only one active elapsed timer query. Do not use that policy when scene code owns timer queries.
- NDI send time covers the native sender call, not receiver/network latency. Worker metrics that
  complete after the final frame boundary may not be attributed to the final snapshot; cumulative
  captured deltas begin exactly at measurement, while asynchronous sent/dropped/failed deltas may
  cross the warm-up/final boundary and are exported as boundary-observed telemetry.
- A requested unavailable output aborts the run as `UNSUPPORTED`; absence is never reported as
  zero-cost output.
- Output resolution affects high-resolution targets only while an external output is enabled.
  Without an output, `resolutionDomain` is `PREVIEW_WINDOW` for Standard or `PREVIEW_CUBEMAP` for
  spherical modes; with an output it is `OUTPUT_BASE`.
- OpenGL vendor, renderer, and version use the library's existing Processing GL adapter. GLSL is
  recorded as `unknown` because that value is not exposed by its public capability snapshot.
- ControlP5 and the visible diagnostic UI are part of this consumer workload. Snapshot aggregation,
  graph updates, and file writes happen only after measurement.
