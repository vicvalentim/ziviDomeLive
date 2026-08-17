# Benchmark Guide

The **BenchmarkTool** measures real ziviDomeLive performance inside Processing. It helps you find
out whether an application maintains its target frame rate, where the pipeline spends time, and
whether a code change improved or reduced performance.

You do not need OpenGL knowledge to get started. Run a quick test first; once it works, continue
with the full qualification suite.


<div class="zd-image-placeholder" markdown>
**IMAGE PLACEHOLDER — BenchmarkTool interface**
Final capture: BenchmarkTool running with the ziviDomeLive panel and benchmark controls visible.
Suggested final asset: `docs/img/benchmark-tool-interface.png`
</div>

!!! note "A graphical session is required"
    The benchmark opens a Processing window and uses the machine's real GPU. It is not a headless
    unit test and requires a working graphical OpenGL session.

## Get Started in 5 Minutes

Open a terminal at the repository root.

### 1. Check Processing

```bash
./gradlew benchmarkDoctor
```

A healthy result looks like:

```text
Processing CLI: /Applications/Processing.app/Contents/MacOS/Processing (Processing cli)
```

Discovery supports both the old `processing-java` command and the modern `Processing cli`
launcher. It searches `PATH` and common macOS, Windows, and Linux installation locations.

### 2. Run a Smoke Test

```bash
./gradlew benchmarkSuite \
  -PbenchmarkSuite=MODES \
  -PbenchmarkScene=EMPTY \
  -PbenchmarkResolution=1024 \
  -PbenchmarkPreview=false \
  -PbenchmarkGpu=true \
  -PbenchmarkGpuTimerPolicy=AUTO \
  -PbenchmarkWarmupFrames=8 \
  -PbenchmarkMeasurementFrames=16
```

Processing tests all four render modes, exports the results, closes the sketch, and generates the
report. The intervals are deliberately short: this test proves orchestration works, but it does not
prove performance stability.

Expected final output resembles:

```text
Benchmark report: .../build/reports/benchmark/index.html
(4 valid run(s), 0 notice(s), comparison=false)
BUILD SUCCESSFUL
```

### 3. Open the Report

```bash
./gradlew benchmarkOpen
```

If desktop browsing is unavailable, open `build/reports/benchmark/index.html` manually.

## Full Qualification

After the smoke test passes, use qualification-length intervals:

```bash
./gradlew benchmarkSuite \
  -PbenchmarkSuite=ALL \
  -PbenchmarkScene=MEDIUM \
  -PbenchmarkResolution=2048 \
  -PbenchmarkPreview=false \
  -PbenchmarkGpu=true \
  -PbenchmarkGpuTimerPolicy=AUTO \
  -PbenchmarkWarmupFrames=600 \
  -PbenchmarkMeasurementFrames=1800 \
  -PbenchmarkTransitionBaselineFrames=120 \
  -PbenchmarkTransitionPostFrames=240
```

This takes longer because it combines modes, resolutions, and transitions. While it runs:

- do not resize or close the window;
- do not change controls;
- avoid heavy background applications;
- keep laptops connected to power;
- wait for Processing to exit on its own.

Preserve the evidence when it finishes:

```bash
./gradlew benchmarkArchive
```

The ZIP is written to `build/benchmark-archives/`. Because `./gradlew clean` deletes `build`, copy
the archive elsewhere when you need long-term storage.

## Available Suites

| Suite | What it measures | When to use it |
| --- | --- | --- |
| `MODES` | Standard, Domemaster, Equirectangular, and Skybox at the selected resolution | First test and quick comparison |
| `MATRIX` | Standard and spherical modes at 1024, 2048, 3072, and 4096 | Resolution scaling |
| `TRANSITIONS` | Resolution, mode, preview, scene, and NDI changes | Stalls and recovery time |
| `ALL` | Full matrix followed by transitions | Final qualification; automation default |

An unavailable output is never treated as having zero cost. The scenario receives `UNSUPPORTED`
status and the suite manifest records the reason.

## Test Scenes

The scenes are synthetic and deterministic: the same configuration repeats the same workload.

| Scene | Workload | Recommended use |
| --- | ---: | --- |
| `EMPTY` | No geometry | Library baseline cost |
| `LIGHT` | 24 boxes | Light projects |
| `MEDIUM` | 180 boxes | General comparison; automation default |
| `HEAVY` | 720 boxes | Geometry stress |
| `SPHERICAL_STRESS` | 640 boxes around the camera | Exercise every cubemap face |

## Render Modes

| Mode | What it represents |
| --- | --- |
| `STANDARD` | Conventional rectangular view |
| `DOMEMASTER` | Fisheye projection for fulldome |
| `EQUIRECTANGULAR` | 360° panoramic projection |
| `SKYBOX` | Cubemap face inspection |

Without an external output, Standard resolution is tied to the Processing window. In spherical
modes, it controls the preview cubemap. With NDI, Syphon, or Spout enabled, resolution represents
the high-resolution output base. Check `resolutionDomain` in `summary.json` to identify the measured
domain.

## Interactive Interface

Open the visual tool with:

```bash
./gradlew runBenchmark
```

The regular ziviDomeLive panel appears on the left and BenchmarkTool on the right. Gradle updates
the sketchbook library before opening Processing.

### Configuration

- **Render Mode:** Standard, Domemaster, Equirectangular, or Skybox.
- **Resolution:** 1024, 2048, 3072, or 4096.
- **Scene:** synthetic workload used by the test.
- **Floating Preview:** includes or excludes the floating preview.
- **GPU timer:** requests asynchronous GPU timing in addition to CPU timing.
- **NDI / Syphon / Spout:** includes an output available on the current system.
- **Warm-up frames:** frames discarded before measurement.
- **Measurement frames:** frames retained in the result.

### Buttons and Shortcuts

| Control | Action |
| --- | --- |
| **START** | Runs the configured warm-up and starts measurement |
| **WARM UP** | Runs diagnostic warm-up only; it creates no exportable result |
| **STOP** or `X` | Stops; retains frames if measurement already started |
| **RESET** | Clears the current result |
| **EXPORT** or `E` | Writes the last completed measurement |
| **RUN SUITE** | Runs `MODES` with the selected scene and resolution |
| `H` | Shows or hides the regular ziviDomeLive panel |

Do not change either panel during warm-up or measurement. Stop the run, change the configuration,
and start a new test.

## Automation Options

| Gradle property | Default | Purpose |
| --- | --- | --- |
| `benchmarkSuite` | `ALL` | `MODES`, `MATRIX`, `TRANSITIONS`, or `ALL` |
| `benchmarkScene` | `MEDIUM` | Scene used by the suite |
| `benchmarkResolution` | `2048` | Selected resolution |
| `benchmarkPreview` | `false` | Floating preview state |
| `benchmarkGpu` | `false` | Requests GPU timing |
| `benchmarkGpuTimerPolicy` | `AUTO` | GPU timer selection policy |
| `benchmarkWarmupFrames` | `600` | Frames discarded before each measurement |
| `benchmarkMeasurementFrames` | `1800` | Frames retained in steady-state scenarios |
| `benchmarkTransitionBaselineFrames` | `120` | Interval before a transition |
| `benchmarkTransitionPostFrames` | `240` | Interval after a transition |
| `processingExecutable` | automatic | Custom Processing installation path |

Example for a custom installation:

```bash
./gradlew benchmarkSuite \
  -PprocessingExecutable=/path/to/processing-java \
  -PbenchmarkSuite=MODES
```

You can also set `PROCESSING_EXECUTABLE` in the environment.

## GPU Timing and Apple Silicon

For most machines, use:

```text
-PbenchmarkGpu=true -PbenchmarkGpuTimerPolicy=AUTO
```

The `AUTO` policy observes the architecture and OpenGL capabilities:

- it prefers timestamp pairs when they are reliable;
- on Apple Silicon, it selects `TIME_ELAPSED_EXCLUSIVE` when required;
- if no safe timer exists, it continues with CPU timing and records a diagnostic.

The `profiling` object in `summary.json` shows what actually happened:

```json
"profiling": {
  "requestedMode": "CPU_GPU",
  "effectiveMode": "CPU_GPU",
  "gpuTimerPolicy": "ARCHITECTURE_AWARE",
  "gpuTimerBackend": "TIME_ELAPSED_EXCLUSIVE",
  "gpuTimerArchitecture": "APPLE_SILICON",
  "gpuSamples": 1800
}
```

Treat `effectiveMode` as the source of truth. If it says `CPU`, inspect `diagnostics`. Do not compare
a CPU-only run with a CPU+GPU run because GPU profiling adds boundaries to the rendering pipeline.

Advanced policies: `SAFE` prohibits exclusive elapsed timing; `TIMESTAMP` requires timestamps;
`ARCHITECTURE_AWARE` explicitly selects by architecture; `ELAPSED` and
`TIME_ELAPSED_EXCLUSIVE` are intended for controlled diagnostics.

## Reading the Metrics

| Metric | Interpretation |
| --- | --- |
| Average FPS | Higher is better |
| Average frame time | Lower is better |
| P50 | Half the frames were this fast or faster |
| P95 | 95% of frames stayed below this value; useful stability indicator |
| P99 | Highlights rare stalls |
| Maximum | Worst observed frame |
| 1% low FPS | Performance of the slowest range; higher is better |
| Frames above 16.67 ms | Frames that missed the 60 FPS budget |
| Frames above 33.33 ms | Frames that missed the 30 FPS budget |
| Frames above 50 ms | Noticeable stalls |

Start with P95, P99, 1% low, and frames above 16.67 ms. A good average can hide occasional stalls.

The **Pipeline Breakdown** separates Standard, cubemap, projection, Skybox, preview, NDI, and GPU
costs. `calls/f` means calls per frame. Invariant violations should remain at zero.

### Transitions

- `normalP95Ms`: P95 of the stable interval before the change;
- `transitionMaxMs`: worst frame after the change;
- `recoveryFrames`: frames needed to return to the normal P95;
- `-1`: no recovery inside the observed interval.

These are descriptive measurements; the tool applies no arbitrary pass threshold.

## Results and Reports

Each run creates:

```text
build/benchmark-results/
├── <date>-<mode>-<resolution>-<scene>/
│   ├── summary.json
│   ├── frames.csv
│   └── environment.json
└── suite-<date>.json
```

- `summary.json`: configuration, aggregates, profiling, and diagnostics;
- `frames.csv`: one row for each retained frame;
- `environment.json`: system, architecture, Java, Processing, and OpenGL;
- `suite-*.json`: `SUPPORTED`, `UNSUPPORTED`, and `FAILED` states.

The offline report is written to `build/reports/benchmark/` and contains `index.html`, `data.json`,
and `summary.md`.

### Compare Baseline and Candidate

```bash
./gradlew benchmarkReport \
  -PbenchmarkBaseline=<old-run-directory> \
  -PbenchmarkCandidate=<new-run-directory>
```

Only compare runs with the same machine, scene, mode, resolution, outputs, GPU timer, and frame
count. Thermal conditions and background applications matter as well.

## Useful Tasks

```bash
./gradlew benchmarkDoctor   # checks Processing
./gradlew runBenchmark      # opens the interface
./gradlew benchmarkSuite    # runs automation
./gradlew benchmarkReport   # regenerates the report
./gradlew benchmarkOpen     # generates and opens the report
./gradlew benchmarkArchive  # archives data and report
./gradlew benchmarkClean    # deletes benchmark data and report
```

## Common Problems

### Processing CLI Not Found

Run `./gradlew benchmarkDoctor`. On macOS, keep `Processing.app` in `/Applications`. For a custom
installation, use `-PprocessingExecutable=<path>` or `PROCESSING_EXECUTABLE`.

### Zero Valid Runs

Check whether `build/benchmark-results` contains complete runs. A measurement that never started,
or a previous `clean`, leaves no valid data. Run the smoke test again.

### GPU Timing Fell Back to CPU

Check `effectiveMode`, `gpuTimerBackend`, and `diagnostics` in `summary.json`. Fallback is expected
when the driver exposes no safe path. Keep `AUTO` selected on Apple Silicon.

### NDI, Syphon, or Spout Is Unavailable

Syphon is macOS-specific; Spout is Windows-specific; NDI depends on native libraries. A missing
output produces `UNSUPPORTED`, never an artificial zero-cost measurement.

### The Report Disappeared After `clean`

Run the suite again or restore a `benchmarkArchive`. Preserve important evidence outside `build`.

## Limitations

- Pipeline times, except for the explicit GPU metric, are CPU observations.
- NDI measures local capture and send, not network latency or receiver presentation.
- Zero GPU samples means no result was available, not that the GPU had no cost.
- The visible ControlP5 interface is part of the example workload.
- Final aggregation and disk writes happen after the measured window.
- Different machines or configurations do not form a reliable direct comparison.

## Advanced Reference

- [Automated benchmarks](automated-benchmarks.md): detailed plans, properties, and transitions.
- [Benchmark reporting](benchmark-reporting.md): schemas, validation, and comparison.
- [Performance profiling](../api/performance-profiling.md): public instrumentation API.
