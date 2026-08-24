# Benchmark report tool

This JDK-only development tool discovers BenchmarkTool schema-v1 runs and generates a fully
offline report. Gradle compiles it in a dedicated source set, so its classes and generated files
do not enter the Processing library JAR.

Use `./gradlew benchmarkReport`. Results are read from `build/benchmark-results/` and the report
is written to `build/reports/benchmark/`.
