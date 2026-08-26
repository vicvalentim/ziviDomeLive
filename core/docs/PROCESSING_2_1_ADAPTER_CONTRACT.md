# Future Processing 2.1 adapter contract

## Scope

This is a compatibility design for a future ziviDomeLive 2.1 implementation. It is not an
implementation plan for the current hardening branch. Processing 2.0 remains the golden runtime,
and its facade, `Scene`, `SceneServices`, renderers, inputs, outputs, assets, UI, and public API do
not consume Core here.

## Type and ownership mapping

| Processing 2.0 surface | Core 0.1 semantic owner | Future Processing adapter responsibility |
|---|---|---|
| `FrameClock` | `core.time.FrameClock` | Keep ticking internal to the facade exactly once in `pre()`; do not expose ticking/reset ownership to scenes |
| `SimulationTimeline` | `core.time.SimulationTimeline` | Delegate artist-controlled rate, position, step and pause policy without float conversion |
| `render.Quaternion` | `core.math.Quaternion` | Convert four float components; retain `PVector`/`PMatrix3D` conveniences host-side; preserve multiplication order |
| `PVector` | `core.math.Vec3` | Copy values at the boundary; never retain a mutable `PVector` as Core state |
| `render.SphericalOrientation` | `core.projection.SphericalOrientation` | Preserve event-order local-axis controls and convert quaternion for renderer matrices |
| `render.camera.OrbitCamera` | `core.camera.OrbitCamera` | Convert targets/quaternions, route Processing `MouseEvent`, apply Processing matrices, and advance once per frame |
| `MouseEvent` | no Core type | Keep drag anchor, UI ownership, 0.01 radians/pixel, 80 wheel units and 0.001 trackpad policy in the Processing adapter |
| `SceneActionMap` named registry | `core.action.ActionMap` | Retain Processing character/key-code/mouse binding maps and raw callback ordering host-side |
| `RenderThreadQueue` | `core.task.FrameThreadQueue` | Bind during Processing `pre()` and drain before activation input/clock/update |
| `SceneTaskGroup` / `SharedTaskExecutor` | `core.task.TaskGroup` plus package-private Core executor | Own one group per activation, return callbacks through the frame queue, and never expose a `Future` |
| `SceneInputPort` / `SceneOutputPort` / `ScenePorts` | `core.ports.InputPort`, `OutputPort`, `Ports` | Adapt protocol implementations and bind pause/drain/close to activation lifecycle |
| `SceneResourceCache<T>` | `core.lifecycle.ResourceCache<T>` | Select borrowed versus owned resource policy and supply Processing/GPU disposers |
| `SceneEnvironmentService` scalar/orientation state | `core.environment.EnvironmentState` and `core.lifecycle.ScopedValue` | Create per-property scopes and use bitwise quaternion/float equality for ownership-safe restoration |
| `SceneEnvironmentService` `PImage` source | none | Keep borrowed image reference, texture resolution and identity comparison entirely host-side |
| renderer scene-camera environment orientation | none | Continue publishing only rotational camera state host/render-side; never add it to Core merely for symmetry |
| `ViewType` | `core.projection.ProjectionType` | Use an explicit exhaustive mapping; never depend on enum ordinal |
| facade FOV/fish-size state | `core.projection.DomemasterSettings` | Delegate scalar policy while retaining UI/renderer synchronization and deferred allocation |
| `RenderMode.FULL` | none | Keep as Processing runtime/output policy; it is not a projection |
| `SceneServices` lifecycle state | `core.lifecycle.ActivationState` primitives only | Preserve configure/setup/frame/stop/dispose/release authority in the facade; do not replace `SceneServices` with a Core container |

## Required lifecycle ordering

The adapter must preserve the frozen activation sequence:

1. create Core and Processing activation-owned resources;
2. call `Scene.configure(SceneServices)`;
3. call `Scene.setupScene()`;
4. at each `pre()`, bind/drain queues and ports, tick time, update once, update camera once;
5. stop admission and suppress old callbacks;
6. call `Scene.dispose()`;
7. close adapters, restore scoped values, and release activation resources.

Reload creates fresh Core activation objects even when the Java `Scene` instance is reused.
Rendering reads the already-updated state and must never advance Core simulation or camera state per
cubemap face.

## Processing 2.0 tolerance versus Core rejection

The future adapter must make an explicit compatibility choice before calling Core for each case:

| Processing API edge case | Processing 2.0 | Core 0.1 | Adapter obligation |
|---|---|---|---|
| Orbit constructor NaN/infinite distance | Accepted | Rejects | Preserve old public behavior outside Core or explicitly version/document a 2.1 behavior change |
| `setDistance` / immediate distance with infinity | Clamp behavior inherited from Processing | Rejects | Normalize at Processing boundary if source compatibility requires it |
| distance NaN | May propagate into camera state | Rejects | Do not silently pass to Core; decide whether to retain old state or emulate propagation host-side |
| zoom non-finite/overflow | Not prevalidated | Rejects non-finite amount | Sanitize or retain 2.0 path host-side |
| inverted or non-finite distance limits | Stored without validation | Rejects transactionally | Validate/translate before delegation; Core range must remain ordered and finite |
| collapse guard NaN/infinity | Accepted and may poison/overspecify state | Rejects | Keep compatibility handling in Processing adapter |
| lerp NaN/infinity | NaN may persist; infinities clamp | Rejects | Apply legacy Processing clamp/retention policy before delegation |
| target `PVector` with non-finite components | Accepted | `Vec3` rejects | Validate conversion; do not construct invalid Core state |
| null target vector | `IllegalArgumentException` | `NullPointerException` at Core value boundary | Translate exception when frozen public behavior requires exact compatibility |
| `goTo`/`snapTo` with invalid orientation | May partially write target before failing | Rejects atomically without mutation | Prefer prevalidation and atomic Core behavior; emulate partial mutation only if compatibility tests prove it required |

The default recommendation for 2.1 is to keep Core strict and place any unavoidable legacy
tolerance in the Processing facade/adapter. That prevents a second host from inheriting
Processing-specific invalid-state behavior.

## Explicit exclusions

The adapter must not move any of the following into Core: `PApplet`, `PGraphicsOpenGL`, `PImage`,
`PShape`, `PShader`, ControlP5, JOGL/PGL, shaders, cubemap/projection rendering, NDI, Syphon, Spout,
output routing, preview policy, scene callbacks, view lighting, or renderer allocation.

No 2.1 integration, version change, API freeze, remote publication, engine, LWJGL, lighting,
materials, post-processing, or physics is authorized by this document.
