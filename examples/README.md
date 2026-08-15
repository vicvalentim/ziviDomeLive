# ziviDomeLive Examples

Open each sketch from Processing after installing ziviDomeLive and its declared dependencies. Every example uses `P3D`, `pixelDensity(1)`, automatic Processing hooks, and the current `Scene.sceneRender(PGraphicsOpenGL)` contract.

| Example | Purpose | Primary interaction |
|---|---|---|
| `BenchmarkTool` | Deterministic performance qualification and JSON/CSV export | ControlP5 panel, `X`, `E` |
| `EmptyProject` | Empty starter template for a first scene | None |
| `Basic` | Multiple scenes, all `RenderMode` values, and an alignment grid | `1..5`, `+`/`-`, mouse wheel, Left/Right arrows |
| `SphereParticle` | Particle simulation using bounded scene tasks | Click/drag |
| `CalibrationTool` | Cube-face focus/color tests and a 360-degree Paul Bourke spherical reference | Left/Right, `1..4`, brackets, `+`/`-`, `P`, `Y`, `R`, `F`, `Space`, `,`/`.`, `C`, `0` |
| `FulldomePBR` | Retained geometry, PBR shader fallback, and scene-space camera | Drag, wheel, `P`, `V`, brackets, `+`/`-`, `R` |
| `InfiniteBackground` | Qualified LDR Environment background in Standard and spherical views | `1..4`, `E`, `B`, `I`/`K`, `Y`/`U`, `0` |
| `SolarSystem` | Full Scene Services reference with fixed-step simulation, assets, actions, camera tracking, and Environment | Drag, wheel, `1..9`, `w`/`s`/`t`, `+`/`-`, `o`, `p`, `l`, `n`, `R` |

## Example Contract

- The sketch calls `ziviDome.setup()` once and never calls `ziviDome.draw()`.
- A scene receives an already-open render target and never calls `beginDraw()` or `endDraw()`.
- Examples demonstrate both direct scene registration and `SceneManager`, according to their teaching purpose.
- Service-aware examples receive `SceneServices` through `configure()` and register through the facade before their first `setupScene()`.
- Entry tabs retain ControlP5, Syphon, and Spout imports because Processing uses them to assemble the contributed-library runtime classpath.
- External outputs remain disabled until explicitly enabled through the control panel or API.

`CalibrationTool` is a qualification instrument, not a generated golden reference. Use independently captured baseline evidence on the target GPU and native-output stack. Its Paul Bourke image remains subject to the bundled third-party notice.

`BenchmarkTool` is the quantitative counterpart: it creates machine-readable evidence only.
See its [workflow and schema notes](BenchmarkTool/README.md) before comparing runs.
