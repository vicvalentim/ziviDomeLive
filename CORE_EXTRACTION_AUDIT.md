# ziviDomeLive Core extraction audit

## Baseline

- Branch: `architecture/zividomelive-core`
- Golden commit: `0d2f03af8ff2dd4d077a50656018e61de08d653c`
- Local tracking ref: `origin/architecture/zividomelive-core` at the same commit
- Java: Eclipse Temurin 17.0.18
- Gradle: 8.5
- Baseline tests: 387 passed, 0 failed, 0 skipped
- Baseline tasks: `clean test`, `build`, `clean qualificationTests`, and
  `buildReleaseArtifacts` (including `verifyProcessingPackage`) all passed.

The golden implementation remains in place. Core is added beside it and the Processing API does
not delegate to Core on this architecture branch.

## Classification key

- **A — PURE CORE:** existing semantics are host-neutral.
- **B — SPLIT CORE + PROCESSING ADAPTER:** state/math belongs in Core; Processing integration stays
  in the current library.
- **C — PROCESSING ONLY:** the type is a Processing-facing contract or resource owner.
- **D — RENDERER/BACKEND:** graphics implementation, not Core state.
- **E — OUTPUT INTEGRATION:** host/output transport implementation.
- **F — TOOLING:** build, test, benchmark, documentation, or packaging support.
- **G — DEFERRED:** concept is intentionally not introduced in Core 0.1.

## Candidate audit

| Current class | Current package | Processing dependency | Other dependency | Qualified behavior | Core semantic owner? | Target Core class | Processing adapter responsibility | Compatibility risk | Golden tests | Decision |
|---|---|---:|---:|---|---|---|---|---|---|---|
| `Scene` | root | `PGraphicsOpenGL`, input events | none | configure/setup/update/render/dispose; update once/render many | No type extraction | — | Artist scene/render callback contract | High: frozen required render method | `SceneContractTest`, `PublicApiCompatibilityTest`, lifecycle tests | **C** |
| `SceneServices` | root | `PApplet` and Processing services | none | one context per activation; fresh reload; deterministic stop/close | Semantics only | No `CoreScene` or generic service container in 0.1 | Construct and order Processing activation services | High: exact frozen accessor set | `ZividomeliveLifecycleTest`, `PublicApiCompatibilityTest` | **B/G** |
| `FrameClock` | root | Javadocs only | `java.base` | monotonic tick, first-zero delta, clamp, elapsed, frame index, reset, backward-clock guard | Yes | `core.time.FrameClock` | Tick once at host frame boundary | Low | `FrameClockTest` | **A** |
| `SimulationTimeline` | root | none | `java.base` | bounded fixed step, rate, pause, position, accumulator, drop telemetry | Yes | `core.time.SimulationTimeline` | Call from scene update, never render | Low | `SimulationTimelineTest`, SolarSystem tests/examples | **A** |
| `Quaternion` | `render` | `PVector`, `PMatrix3D` conveniences | `java.base` | float axis-angle, multiply order, normalization, shortest-path SLERP | Math only | `core.math.Quaternion` | Preserve current vector and matrix conveniences | Medium: float/order exactness | `QuaternionTest` | **B** |
| `SphericalOrientation` | `render` | `PConstants` only | none | local X pitch/local Z yaw/local Y roll, shortest cyclic delta, event-order composition | Yes | `core.projection.SphericalOrientation` | Facade controls and shader matrix conversion | Medium: cyclic constants/order | `SphericalOrientationTest`, `SphericalProjectionContractTest` | **A** |
| `OrbitCamera` | `render.camera` | `PVector`, `PMatrix3D`, graphics and mouse events | none | signed distance, guard, current/goal pose, world-space rotation, interpolation | State/controller only | `core.camera.OrbitCamera`, `core.math.Vec3` | Mouse binding and graphics transform application | High: -Z camera convention and multiplication order | `OrbitCameraTest`, `SceneCameraServiceTest` | **B** |
| `SceneCameraService` | root | graphics, vectors, facade mouse routing | none | activation ownership, target tracking, axis-angle helper, view-light pose | Camera state is Core; lighting/application is not | Uses future adapter around Core camera | Apply matrix, route Processing events, view-light setup, restore input state | High: public surface and input ownership | `SceneCameraServiceTest`, lifecycle and camera tests | **B** |
| `SceneActionMap` | root | key/mouse events | `java.base` | named registry plus host bindings | Registry only | `core.action.ActionMap` | Key code, character, mouse event bindings and dispatch | Medium: replacement/unregister/close | `SceneActionMapTest`, lifecycle tests | **B** |
| `RenderThreadQueue` | root `_internal/runtime` | terminology only | concurrent collections | bind/rebind, execute-or-enqueue, finite snapshot drain, closed guard | Yes | `core.task.FrameThreadQueue` | Bind it at the Processing `pre()` boundary | Low | `RenderThreadQueueTest`, lifecycle rebind test | **A** |
| `SharedTaskExecutor` | root `_internal/runtime` | none | Java concurrency | process-wide daemon workers, all reported CPUs, bounded queue 256 | Yes, behind task API | `core.task.CoreTaskExecutor` (package-private) | None; host may inject an executor for tests/integration | Medium: worker ownership/backpressure | `SceneTaskGroupTest` | **A** |
| `SceneTaskGroup` | root | terminology/logging only | Java concurrency | keyed bounded work, no public Future, frame callbacks, cancellation and stale suppression | Yes | `core.task.TaskGroup` | Own/close it with activation and drain frame queue | Medium: races and cancellation | `SceneTaskGroupTest`, lifecycle tests | **A** |
| `SceneInputPort` | root | terminology only | `java.base` | generic producer adapter lifecycle | Yes | `core.ports.InputPort` | Protocol-specific MIDI/OSC/device adapter | Low | `ScenePortsTest` | **A** |
| `SceneOutputPort` | root | terminology only | `java.base` | non-blocking generic output/backpressure contract | Yes | `core.ports.OutputPort` | Protocol/transport implementation | Low | `ScenePortsTest` | **A** |
| `ScenePorts` | root | terminology/logging only | Java collections | identity registration, bounded input, drop-oldest, frame dispatch, pause, reverse close | Yes | `core.ports.Ports` | Bind/drain at frame boundary; provide optional adapters | Medium: threading/order | `ScenePortsTest`, lifecycle switch/pause tests | **A** |
| `EnvironmentState` | root `_internal/render/core` | borrowed `PImage` source | none | visible, non-negative intensity, yaw, independent source and camera orientations | State only | `core.environment.EnvironmentState` | Own borrowed image and renderer-facing camera orientation publication | Medium: keep source separate | `EnvironmentStateTest`, spherical render contracts | **B** |
| `SceneEnvironmentService` | root | `PImage`, facade | none | activation-scoped overrides restored only while still owned | Generic ownership algorithm is reusable | `core.lifecycle.ScopedValue` plus environment state | Capture/restore the Processing image and facade state | Medium: compare-and-restore | lifecycle environment tests | **B** |
| `SceneAssets` | root | `PApplet`, images, shapes, shaders | none | render-thread creation and typed caches | No graphics abstraction | — | All asset loading/GPU resource handling | High if generalized artificially | `SceneAssetsTest`, lifecycle tests | **C** |
| `SceneResourceCache<T>` | root `_internal/scene` | none | Java collections, logging | borrowed/owned values, replacement disposal, prefix invalidation, reverse clear, idempotent close | Yes | `core.lifecycle.ResourceCache<T>` | Processing chooses resource ownership and disposer | Low | `SceneResourceCacheTest` | **A** |
| `SceneManager` | root | `Scene` lifecycle | none | identity membership, activation selection, complete reload/switch lifecycle | Semantics documented; no scene abstraction now | — | Remains authoritative Processing scene manager | High: frozen identity/lifecycle behavior | `SceneManagerTest`, lifecycle tests | **B/G** |
| `ViewType` | root | none | none | four final view projections; explicit mapping rather than ordinal persistence | Yes | `core.projection.ProjectionType` | Explicit `ViewType` mapping in future 2.1 adapter | Medium: enum order must not leak | routing/UI/API tests | **A/B** |
| `RenderMode` | root | none | none | `FULL` policy plus dedicated runtime overrides | Only dedicated projection names | No Core `RenderMode`; map dedicated values to `ProjectionType` | Preserve `FULL` policy and public enum order | High if copied blindly | `RenderModeTest`, routing/API tests | **B** |
| Facade calibration state | root `ziviDomeLive` | facade/renderers/UI | none | FOV 0..360, size 0..100, non-finite ignored; deferred output resolution | Only domemaster calibration | `core.projection.DomemasterSettings` | Log ignored values, update renderer/UI, defer allocation | Medium | `ZividomeliveLifecycleTest`, fisheye tests | **B** |
| Facade activation/frame ordering | root `ziviDomeLive` | Processing hooks | outputs/renderers | queue/input/clock/update/target/camera order; coalesced reload | Primitives only | Existing Core clock/queue/tasks/ports and `ScopedValue` | Retain authoritative hook and lifecycle ordering | High | lifecycle, scene manager, render pipeline tests | **B/G** |
| `CubemapRenderer`, projection renderers, GL adapters/shaders | `_internal/render` and `data/shaders` | Processing/JOGL | GPU | cubemap capture and projection rendering | No | — | Entire rendering pipeline | Critical/out of scope | renderer and shader tests | **D** |
| `OutputManagerImpl`, NDI, Syphon, Spout backends | `_internal/output` | Processing/texture sharing | Devolay/native SDKs | routed opt-in output and bounded shutdown | No | — | Entire transport lifecycle | Critical/out of scope | output tests | **E** |
| ControlP5 UI and Processing input bridge | `_internal/ui` | Processing/ControlP5 | ControlP5 | stable explicit UI mapping and input ownership | No | — | Entire UI lifecycle | Critical/out of scope | UI and compatibility tests | **C** |
| Build, benchmark, docs and packaging helpers | root tooling | varies | Gradle/MkDocs | release and qualification | No runtime ownership | Core gets a self-contained module build only | Root tooling continues to package Processing | Medium | qualification/package tests | **F** |
| Lighting, materials, post-FX, physics, render graph/backend/targets/textures/shaders/meshes, standalone window/engine | nonexistent/planned | n/a | n/a | no golden implementation | No | — | Future milestones | Avoid premature API | none | **G** |

## Boundary decisions

1. Core Java sources use only Java 17 `java.base` APIs. JUnit exists only in the Core test
   configuration.
2. Core does not contain a scene/render abstraction. Update-once/render-many is an ownership rule
   for hosts and for Core state advancement, not a new `CoreScene` API.
3. Processing mathematical conveniences remain in the golden types. Core Quaternion has no
   `PVector` or `PMatrix3D` overloads.
4. Camera input and graphical application remain host adapters. Core camera exposes current and
   goal state but never handles Processing events or graphics targets.
5. Environment image ownership remains entirely in Processing. Core stores only visual scalar and
   orientation state.
6. `RenderMode.FULL` is not a projection and is not copied. The four `ViewType` values map
   explicitly to Core `ProjectionType` values in equivalence tests.
7. No generic DI container, event bus, graphics abstraction, or backend preparation is introduced.
8. The existing 2.0 classes remain the golden reference; production delegation is deferred to the
   future 2.1 integration branch.
