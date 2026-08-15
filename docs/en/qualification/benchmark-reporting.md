# Benchmark Reporting

The development-only report tool validates and aggregates BenchmarkTool schema-v1 runs. It uses
only the JDK, writes a static offline dashboard, and is isolated from the Processing library JAR
and release package.

## Capture And Report

Configure BenchmarkTool to export into the repository before starting the Processing sketch:

```bash
export ZIVIDOME_BENCHMARK_OUTPUT="$PWD/build/benchmark-results"
```

After one or more runs have been exported, generate the report:

```bash
./gradlew benchmarkReport
```

The task reads immediate run directories from `build/benchmark-results/`, validates
`summary.json`, `environment.json`, and every `frames.csv` row, then writes:

- `build/reports/benchmark/index.html`: self-contained HTML/CSS/SVG dashboard;
- `build/reports/benchmark/data.json`: report schema v1 for automation;
- `build/reports/benchmark/summary.md`: compact auditable summary.

Invalid, unsupported, symlinked, or incomplete runs are excluded and identified in the report.
An absent or empty results directory produces a valid empty report rather than reusing stale data.
Transition runs include their initial and target endpoints, normal P95, transition maximum, and
recovery-frame count in both the selected-run detail and test matrix.

## Baseline And Candidate

Pass run directory names, or paths whose final component is a run directory name:

```bash
./gradlew benchmarkReport \
  -PbenchmarkBaseline=<baseline-run> \
  -PbenchmarkCandidate=<candidate-run>
```

The comparison shows both values, absolute delta, percentage delta, expected direction, and the
directional result. Lower is better for frame time (average, P50, P95, P99, and maximum); higher is
better for average FPS and 1% low FPS. A result is labelled improvement, regression, or unchanged
strictly from that direction. The tool deliberately applies no arbitrary acceptance threshold.

Compare like-for-like scenarios and environments. CPU-observed OpenGL timing can include driver
waits, and external-output measurements do not prove receiver presentation or network latency.

## Supporting Tasks

```bash
./gradlew benchmarkOpen
./gradlew benchmarkArchive
./gradlew benchmarkClean
```

`benchmarkOpen` regenerates and opens `index.html` when desktop browsing is available, otherwise it
prints the absolute path. `benchmarkArchive` regenerates the report and creates a timestamped ZIP in
`build/benchmark-archives/` containing both inputs and outputs. `benchmarkClean` explicitly removes
captured runs and generated reports; use it only after archiving any evidence that must be retained.
