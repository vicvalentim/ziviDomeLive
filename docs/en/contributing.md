# Contributing

## Local Checks

Use Java 17 and run:

```bash
./gradlew clean test build
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

GPU or output changes require the CompatibilityLock visual protocol and target-platform hardware evidence in addition to unit tests.
