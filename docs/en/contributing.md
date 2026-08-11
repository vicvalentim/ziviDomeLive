# Contributing

## Local Checks

Use Java 17 and run:

```bash
./gradlew clean qualificationTests
./gradlew build -x test
./gradlew buildReleaseArtifacts
mkdocs build --strict
```

## Project Contracts

- Keep `ViewType` order unchanged.
- Do not call `beginDraw()` or `endDraw()` inside a `Scene`.
- Preserve deferred output-resolution reset.
- Use `LogManager` for library logging.
- Use `ThreadManager` for shared background tasks.
- Keep Syphon/Spout on the `PGraphicsOpenGL` path.
- Do not add experimental 2.0 render backends to the 1.x line.

GPU or output changes require the CalibrationTool visual protocol and target-platform hardware evidence in addition to unit tests.

`qualificationTests` is the canonical automated test run. Its summary, HTML,
and JUnit XML evidence is written under `build/reports/qualification/` and
`build/test-results/qualification/`. You can diagnose one class with
`./gradlew qualificationTests --tests '*CameraManagerTest'`, but release
acceptance requires the complete unfiltered suite. Test sources stay in Git and
are excluded from Processing packages and sketchbook deployment.

GitHub also runs this task in the independent `Automated Qualification`
workflow for every push, pull requests targeting `main`, and manual executions.
Its job summary shows the totals and its downloadable artifact retains the
detailed evidence for 30 days.
