# Contributing

## Local Checks

Use Java 17 and run:

```bash
./gradlew clean qualificationTests
./gradlew build -x test
./gradlew buildReleaseArtifacts
python3 -m mkdocs build --strict
./gradlew attachJavadocsToSite --console=plain
python3 tools/validate_documentation.py --root . --site-dir site
```

Preview the manual with `python3 -m mkdocs serve`. This deliberately avoids legacy system-level MkDocs executables that may belong to Python 2.

## Project Contracts

- Keep `ViewType` order unchanged.
- Keep English and Portuguese pages paired and update `mkdocs.yml` navigation together.
- Do not call `beginDraw()` or `endDraw()` inside a `Scene`.
- Preserve deferred output-resolution reset.
- Use `LogManager` for library logging.
- Use activation-scoped `SceneServices.tasks()` for scene background work; do not expose or create another executor.
- Keep Syphon/Spout on the `PGraphicsOpenGL` path.
- Do not reintroduce the removed `PGraphicsOpenGL[]` spherical capture path.

GPU or output changes require the [CalibrationTool](qualification/calibration-tool.md) visual protocol and target-platform hardware evidence in addition to unit tests.

`qualificationTests` is the canonical automated test run. Its summary, HTML,
and JUnit XML evidence is written under `build/reports/qualification/` and
`build/test-results/qualification/`. You can diagnose one class with
`./gradlew qualificationTests --tests '*OrbitCameraTest'`, but release
acceptance requires the complete unfiltered suite. Test sources stay in Git and
are excluded from Processing packages and sketchbook deployment.

GitHub also runs this task in the independent `Automated Qualification`
workflow for every push, pull requests targeting `main`, and manual executions.
Its job summary shows the totals and its downloadable artifact retains the
detailed evidence for 30 days.

## Change Scope

Public behavior changes require Javadocs, focused unit tests, bilingual user
documentation, and a changelog entry. Keep pure routing, orientation, sizing,
and lifecycle policy isolated from OpenGL where possible so it can be tested in
the headless qualification fork.

Do not commit generated `build/`, `site/`, or `release/` contents. Release
artifacts are produced by Gradle and published from version tags.
