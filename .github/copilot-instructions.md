# GitHub Copilot Instructions for ziviDomeLive

Read `AGENTS.md` first. Source code is authoritative when this document and implementation diverge.

## Project State

ziviDomeLive 1.5.0 is the final consolidation of the Processing 4 / Java 17 architecture. The public lowercase facade `zividomelive` and compatibility-sensitive `ViewType` order remain unchanged. Do not add experimental 2.0 renderer infrastructure to the 1.x line.

## Rendering Domains

Standard and spherical rendering are independent:

```text
STANDARD
Scene -> StandardRenderer -> Standard target

SPHERICAL
Scene -> CubemapRenderer -> EquirectangularRenderer -> FisheyeDomemaster
                         \-> CubemapViewRenderer
```

The spherical chain is an internal 1.x topology, not a permanent public contract. Preserve visual orientation, face content/layout, FOV, Size%, and pitch/yaw/roll behavior without creating APIs that require future versions to keep `PGraphicsOpenGL[]` or the same pass chain.

`RenderRequirementsPolicy` computes the minimum passes required by:

- the main preview;
- the optional floating domemaster preview;
- every enabled external output.

The Processing window always composites preview-resolution targets. High-resolution output targets remain offscreen.

## Public Render Behavior

`RenderMode.FULL` is the default and preserves independent preview/output `ViewType` routes. Dedicated modes temporarily override the effective representation:

```text
STANDARD       -> ViewType.STANDARD
DOMEMASTER     -> ViewType.FISHEYE_DOMEMASTER
EQUIRECTANGULAR-> ViewType.EQUIRECTANGULAR
SKYBOX         -> ViewType.CUBEMAP
```

The floating domemaster may add a spherical requirement while the global mode is Standard.

## Scene Contract

- `SceneManager` is the active-scene authority.
- The first registered scene is activated once.
- Switching disposes the leaving scene and sets up the arriving scene.
- `Scene.update()` runs once before the frame render.
- `Scene.sceneRender(PGraphicsOpenGL)` receives a target whose draw lifecycle is already open.
- A scene must never call `beginDraw()` or `endDraw()`.
- Keyboard, mouse, and ControlP5 events are forwarded automatically and must not be forwarded again by the sketch.

## Resolution and Calibration

- Standard preview uses current window dimensions.
- Spherical preview uses `min(1024, max(256, min(windowWidth, windowHeight)))`.
- Output resolution is independent and uses presets 1024, 2048, 3072, and 4096.
- Output reallocation is deferred to the draw loop.
- FOV is `0..360`, default `210`.
- Domemaster Size% is `0..100`, default `100`, and must survive target recreation.
- Pitch, yaw, and roll are shared spherical-domain parameters.

## External Outputs

Availability, native initialization, publication, and render requirement are separate states.

Syphon and Spout remain on the GPU-native `PGraphicsOpenGL` path. Do not add CPU readback, worker threads, or intermediate graphics targets to these backends.

NDI is the GPU-to-CPU boundary. Preserve:

- three reusable frame slots;
- bounded free/ready queues;
- latest-frame-wins backpressure;
- one dedicated sender worker;
- no OpenGL calls on that worker;
- captured, sent, dropped, and failed counters;
- packed RGBA, positive `width * 4` stride, source row order, progressive frames;
- configured target frame-rate metadata;
- bounded shutdown with deferred native cleanup if a send remains blocked;
- explicit retry after initialization or worker failure.

The NDI worker is the intentional exception to the general `ThreadManager` rule because it owns a bounded native sender lifecycle. Other background work uses `ThreadManager`.

## Lifecycle

The facade registers Processing hooks in its constructor. The sketch calls `setup()` once but does not call `ziviDome.draw()`.

`pause()` records active publications and shuts outputs down. `resume()` attempts to restore them. `dispose()` is terminal and idempotently releases outputs, controls, scenes, renderers, splash resources, callbacks, and shared threads.

## OpenGL Error 1282

Version 1.4 removed a nested NDI `beginDraw()` / `endDraw()` path that amplified `GL_INVALID_OPERATION`. The broader error remains endemic to some Processing/JOGL, framebuffer, GPU, and driver combinations and is not considered solved.

Do not reintroduce nested scene draw ownership, texture-bound `glReadPixels`, PBOs, or low-level OpenGL work on the NDI worker. Keep the known issue documented and require target-hardware qualification.

## Conventions

- Never reorder `ViewType`, `InitState`, `RenderMode`, `OutputType`, or `OutputState` values.
- Use `LogManager.getLogger()` for library logging.
- Use `ThreadManager` for shared background tasks.
- Keep shader paths under `data/shaders/`; Gradle packages `shaders/` there.
- Keep changes scoped to the current 1.x architecture.
- Do not add native cube-map backends, `samplerCube`, PBO, OpenGL fences, HDR/PBR architecture, SphericalMirror, or placeholder `future`/`v2` packages.

## Validation

```bash
./gradlew clean qualificationTests
./gradlew build -x test
./gradlew buildReleaseArtifacts
mkdocs build --strict
```

Automated tests do not prove GPU visual parity or NDI/Syphon/Spout interoperability. Use `examples/CalibrationTool/` and `docs/qualification/1.5-release-readiness.md` on qualified hardware.
