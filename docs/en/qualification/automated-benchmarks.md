# Automated Benchmarks

`BenchmarkTool` can execute deterministic suites through a real Processing 4 Java Mode process.
This is GPU/hardware integration, not a headless unit test: the machine must provide a working
graphical OpenGL session and the native backends requested by a scenario.

## Processing CLI

Configure the Processing executable either as a Gradle property or environment variable:

```bash
./gradlew benchmarkSuite \
  -PprocessingExecutable=/path/to/processing-java

export PROCESSING_EXECUTABLE=/path/to/processing-java
./gradlew benchmarkSuite
```

When neither value is set, Gradle searches for `processing-java` on `PATH`. A missing or
non-executable CLI stops immediately with a configuration message. No application location is
hardcoded. `runBenchmark` uses the same discovery and opens the interactive sketch:

```bash
./gradlew runBenchmark -PprocessingExecutable=/path/to/processing-java
```

Both tasks first run `deployBenchmarkLibrary`, which updates only the benchmark library JAR,
runtime dependency, and metadata in the detected Processing sketchbook. Unlike the full release
deployment, this task does not invoke `clean`, so captured baseline runs remain available.

## Suites

```bash
./gradlew benchmarkSuite \
  -PprocessingExecutable=/path/to/processing-java \
  -PbenchmarkSuite=ALL
```

Available plans are:

- `MODES`: Standard, Domemaster, Equirectangular, and Skybox at the selected resolution;
- `MATRIX`: one rectangular Standard run plus Domemaster, Equirectangular, and Skybox at 1024,
  2048, 3072, and 4096;
- `TRANSITIONS`: 2048→4096, Standard→Domemaster, Preview off→on, Light→Heavy, and NDI off→on;
- `ALL`: matrix followed by transitions; this is the default for `benchmarkSuite`.

Standard is intentionally not repeated for cubemap resolutions when external output is disabled:
its active domain is the rectangular Processing window. Native-output scenarios are never treated
as zero-cost when unavailable; they are skipped as `UNSUPPORTED` in the suite manifest.

The runner exports every completed run, writes `suite-<timestamp>.json`, exits Processing, and then
generates `build/reports/benchmark/index.html`.

## Duration And Scenario Properties

Defaults represent qualification intervals, while shorter values are useful only for smoke tests:

```bash
./gradlew benchmarkSuite \
  -PprocessingExecutable=/path/to/processing-java \
  -PbenchmarkSuite=MODES \
  -PbenchmarkScene=MEDIUM \
  -PbenchmarkResolution=2048 \
  -PbenchmarkPreview=false \
  -PbenchmarkGpu=false \
  -PbenchmarkWarmupFrames=600 \
  -PbenchmarkMeasurementFrames=1800 \
  -PbenchmarkTransitionBaselineFrames=120 \
  -PbenchmarkTransitionPostFrames=240
```

Warm-up is discarded. A transition then records an initial steady interval, applies exactly one
change on the Processing/render thread, and retains the configured post interval. `normalP95Ms` is
calculated from the initial interval; `transitionMaxMs` is the largest post-change sample;
`recoveryFrames` is the first post-change offset at or below normal P95, or `-1` when recovery is
not observed. These are descriptive measurements with no hidden pass/fail threshold.

The CLI sets the output directory and current Git revision for the sketch. Use like-for-like
machines and configurations for comparisons. A successful smoke run with very short intervals
proves orchestration, not performance stability.

Set `-PbenchmarkGpu=true` to request the bounded asynchronous pipeline timer. The exported
`profiling.effectiveMode` is authoritative: unsupported desktop GL/JOGL contexts fall back to CPU.
GPU mode adds two Processing GL flush boundaries per measured frame, so it must not be mixed with
CPU-only runs in a baseline comparison.
