# ziviDomeLive Examples

Open each sketch from Processing after installing ziviDomeLive and its declared dependencies. Every example uses `P3D`, `pixelDensity(1)`, automatic Processing hooks, and the current `Scene.sceneRender(PGraphicsOpenGL)` contract.

| Example | Purpose | Primary interaction |
|---|---|---|
| `EmptyProject` | Empty starter template for a first scene | None |
| `Basic` | Multiple scenes, all `RenderMode` values, and an alignment grid | `1..5`, `+`/`-`, mouse wheel, Left/Right arrows |
| `SphereParticle` | Threaded particle simulation | Click/drag |
| `CompatibilityLock` | Static face, alignment, color, and grayscale calibration | `1..4`, brackets, `+`/`-`, `P`, `Y`, `R`, `F`, `0` |
| `FulldomePBR` | Retained geometry, PBR shader fallback, and scene-space camera | Drag, wheel, `P`, `V`, brackets, `+`/`-`, `R` |
| `SolarSystem` | Large multi-file application | See the sketch's own source and configuration |

## Example Contract

- The sketch calls `ziviDome.setup()` once and never calls `ziviDome.draw()`.
- A scene receives an already-open render target and never calls `beginDraw()` or `endDraw()`.
- Examples demonstrate both direct scene registration and `SceneManager`, according to their teaching purpose.
- Entry tabs retain ControlP5, Syphon, and Spout imports because Processing uses them to assemble the contributed-library runtime classpath.
- External outputs remain disabled until explicitly enabled through the control panel or API.

`CompatibilityLock` is a qualification instrument, not a generated golden reference. Use independently captured baseline evidence on the target GPU and native-output stack.
