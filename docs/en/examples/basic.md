# Basic Examples

## Basic

Demonstrates two managed scenes and the `RenderMode` API. In `Scene1`, press:

- `1`: `FULL`
- `2`: `STANDARD`
- `3`: `DOMEMASTER`
- `4`: `EQUIRECTANGULAR`
- `5`: `SKYBOX`
- `+` / `-`: adjust animation speed
- `r`: reset animation speed
- Mouse wheel: change the pillar orbit radius

Use Left/Right arrows to switch between the rotating pillars and the static six-face alignment grid.

## EmptyProject

A minimal multi-file starter template with one `Scene`, one `SceneManager`, automatic Processing hooks, an intentionally empty `sceneRender()`, and an intentionally empty sketch `draw()`.

## SphereParticle

A larger scene example that runs at most one simulation task through the shared `ThreadManager`, cancels scene-owned work during disposal, and keeps graphics calls on the render thread. Click or drag to add particles.

## BenchmarkTool

A quantitative qualification sketch with deterministic `EMPTY`, `LIGHT`, `MEDIUM`, `HEAVY`, and
`SPHERICAL_STRESS` scenes. Its ControlP5 panel separates warm-up from measurement, displays an
immutable post-run snapshot, and exports schema-versioned `summary.json`, `frames.csv`, and
`environment.json`. Set `ZIVIDOME_BENCHMARK_OUTPUT` to keep repository runs under
`build/benchmark-results/`; the documented manual fallback is outside the examples directory.

The initial `RUN SUITE` covers the four dedicated render modes for the selected scene,
resolution, and outputs. Generate the offline HTML dashboard with `./gradlew benchmarkReport`;
see [Benchmark Reporting](../qualification/benchmark-reporting.md). GPU timers and transition
runs remain separate stages.

All examples preserve `sceneRender(PGraphicsOpenGL)` and never call `ziviDome.draw()` manually.

Start from `EmptyProject`, use `Basic` to learn scene switching and render modes,
then inspect `SphereParticle` for the boundary between background simulation and
render-thread drawing.
