# GitHub Copilot Instructions for ziviDomeLive

Read `AGENTS.md` first. It is the canonical lifecycle, Services, Processing-facing API,
and regression contract. This file supplements it with native-cubemap, output, and release
qualification details. Source and tests are authoritative if either instruction file drifts.

## Project State

ziviDomeLive 2.0.0 is the native cubemap consolidation of the Processing 4 / Java 17
architecture. The public facade is `ziviDomeLive`; `ViewType` and `RenderMode` are top-level
public enums. The Processing-facing Scene contract remains intentionally small while spherical
rendering uses native `GL_TEXTURE_CUBE_MAP` capture and `samplerCube` projection shaders.

Documentation is behind the implementation. Do not infer the 2.0 public contract from docs
without checking source, lifecycle tests, compatibility tests, and executable examples.

## Rendering Domains

Standard and spherical rendering are independent:

```text
STANDARD
Scene -> StandardRenderer -> Standard target

SPHERICAL
Scene -> CubemapRenderer -> native GL_TEXTURE_CUBE_MAP -> EquirectangularRenderer
                                                       \-> FisheyeDomemaster
                                                       \-> CubemapViewRenderer
```

The spherical chain is an internal topology, not a permanent public contract. Preserve visual orientation, face content/layout, FOV, Size%, and pitch/yaw/roll behavior without reintroducing independent `PGraphicsOpenGL[]` face targets or six-texture projection fallbacks.

`RenderRequirementsPolicy` computes the minimum passes required by:

- the main preview;
- the optional floating domemaster preview;
- every enabled external output.

The Processing window always composites preview-resolution targets. High-resolution output targets remain offscreen.

## Public Render Behavior

`RenderMode.FULL` is the default and preserves independent preview/output `ViewType` routes. Dedicated modes temporarily override the effective representation:

```text
STANDARD       -> ViewType.STANDARD
DOMEMASTER     -> ViewType.DOMEMASTER
EQUIRECTANGULAR-> ViewType.EQUIRECTANGULAR
SKYBOX         -> ViewType.SKYBOX
```

The floating domemaster may add a spherical requirement while the global mode is Standard.

## Scene Contract

- `ziviDomeLive` owns and attaches the authoritative `SceneManager` lifecycle.
- `Scene.sceneRender(PGraphicsOpenGL)` is the only required abstract Scene method.
- For every activation, `Scene.configure(SceneServices)` runs before `setupScene()`.
- Switching disposes the leaving activation and sets up the arriving activation with fresh services.
- Reload is a full dispose/configure/setup cycle, even when the Scene Java object is reused.
- `Scene.update()` runs once before rendering; spherical capture may render the same state multiple times.
- `Scene.sceneRender(PGraphicsOpenGL)` receives a target whose draw lifecycle is already open.
- A scene must never call `beginDraw()` or `endDraw()`.
- Keyboard, mouse, and ControlP5 events are forwarded automatically and must not be forwarded again by the sketch.
- Scene activation ownership uses instance identity; do not mix `equals()` registration with identity-scoped services.

## Services and Processing Input

- `SceneServices` belongs to one activation and is retained only until its matching `dispose()`.
- Artist-facing entry points are `applet`, `frameClock`, `timeline`, `tasks`, `assets`,
  `actions`, `camera`, `environment`, and deferred `requestReload`.
- The runtime owns service closure, ticking, queue drain, dispatch, cancellation, and restoration.
  Current public `close`, raw queue/cache, `parent`, and lifecycle methods are transition
  liabilities, not patterns to teach or expand.
- Keep service usage direct and Processing-like. Do not introduce a DI framework, generic event
  bus, global internal service locator, or an interface hierarchy for every service.
- Direct camera manipulation is immediate; tracked/programmatic camera goals may be smooth.
  Route each gesture to one camera, let visible ControlP5 widgets capture their gestures, and
  clear drag anchors on owner/lifecycle transitions.
- Preserve SolarSystem slow-time behavior: rate-adaptive physics step, double-precision orbital
  math, compensated elapsed time, and reusable physics buffers. Do not move that scene-specific
  step policy into `SimulationTimeline` globally.

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
All outputs start opt-in/disabled after setup; platform availability must not automatically begin publication.

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

`pre()` binds the current Processing/OpenGL thread, begins activation services, ticks the frame
clock, updates the Scene once, refreshes tracked camera state, and advances camera smoothing.

`pause()` records active publications and shuts outputs down. `resume()` attempts to restore
them. `dispose()` is terminal and idempotently releases owned outputs, controls, scene
activations, renderers, splash resources, and callbacks. It must not shut down the process-wide
shared `ThreadManager` merely because one facade instance is disposed.

## OpenGL Error 1282

Version 1.4 removed a nested NDI `beginDraw()` / `endDraw()` path that amplified `GL_INVALID_OPERATION`. The broader error remains endemic to some Processing/JOGL, framebuffer, GPU, and driver combinations and is not considered solved.

Do not reintroduce nested scene draw ownership, texture-bound `glReadPixels`, PBOs, or low-level OpenGL work on the NDI worker. Keep the known issue documented and require target-hardware qualification.

## Conventions

- Never reorder `ViewType`, `InitState`, `RenderMode`, `OutputType`, or `OutputState` values.
- Protect the optional/default-method Scene contract before changing Services.
- Prefer minimal visibility/ownership corrections over broad public-API rewrites.
- Make surgical patches: identify the affected contract, touch the minimum files/symbols, and
  leave adjacent subsystems unchanged unless a regression test proves the boundary must move.
- Do not mix Services work with opportunistic renames, package moves, render/output cleanup,
  formatting sweeps, example modernization, or documentation expansion.
- When broader work is genuinely required, isolate and justify it explicitly instead of
  allowing scope to grow implicitly inside an otherwise small fix.
- Use `LogManager.getLogger()` for library logging.
- Use `ThreadManager` for shared background tasks.
- Keep shader paths under `data/shaders/`; Gradle packages `shaders/` there.
- Keep changes scoped to the current 2.0 native cubemap architecture.
- Do not reintroduce `PGraphicsOpenGL[]` spherical capture, six-texture projection fallbacks, or placeholder `future`/`v2` packages.

## Validation

```bash
./gradlew clean test build
./gradlew clean qualificationTests
./gradlew build -x test
./gradlew buildReleaseArtifacts
mkdocs build --strict
processing-java --sketch=examples/SolarSystem \
  --output=/tmp/zividomelive-solarsystem-build --force --build
```

Run only the validation tiers relevant to the change, but do not omit `clean test build` for
public API/lifecycle work. Automated tests do not prove GPU visual parity or NDI/Syphon/Spout
interoperability. Use `examples/CalibrationTool/` and
`docs/qualification/2.0-release-readiness.md` on qualified hardware.
