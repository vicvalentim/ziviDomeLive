# Changelog

All notable project changes are recorded here. Version 2.0 follows the final public API freeze; earlier sections preserve the historical contract of their release and must not be read as current 2.0 API documentation.

## [2.0.0] - Unreleased

Version 2.0.0 is a deliberate major-version reset. It preserves the Processing-oriented `Scene` extension model while replacing the exposed 1.x implementation surface with a small, lifecycle-safe, typed API. Internally, spherical rendering moves to a native GPU cubemap with sibling final projections.

The Java package remains `com.victorvalentim.zividomelive`. The public facade is `ziviDomeLive`.

### Public API freeze

The final surface is classified and protected as follows:

- **Stable:** `ziviDomeLive`, `StandardOutputAspectMode`, `Scene`, `SceneManager`, `RenderMode`, `ViewType`, `LogMode`;
- **Advanced Stable:** activation services, typed output control, quaternion/orientation helpers and `OrbitCamera`;
- **Experimental:** performance snapshots, metrics, capability reports and GPU timing policy/backend/architecture;
- **Processing callbacks:** public facade hooks invoked by Processing or ControlP5;
- **Internal:** render graph, OpenGL targets/adapters, UI, runtime queues/executors and output producers.

An exact reflection-based snapshot freezes public types, methods, constructors, enum order, field mutability and negative engine-leak constraints.

### Breaking changes

- Renamed the facade from lowercase `zividomelive` to `ziviDomeLive`.
- Made `ViewType` a top-level enum with the frozen order `STANDARD`, `DOMEMASTER`, `EQUIRECTANGULAR`, `SKYBOX`.
- Kept `RenderMode` at `FULL`, `STANDARD`, `DOMEMASTER`, `EQUIRECTANGULAR`, `SKYBOX`.
- Removed all deprecated 1.x compatibility commands instead of carrying unsafe aliases into the new major version.
- Removed direct facade renderer getters/setters and direct render commands.
- Removed public concrete renderers, cubemap targets, GL adapters, camera/cubemap engine types, final-frame containers, UI/support managers and performance-monitor implementations.
- Removed public/global thread-manager and executor access.
- Removed `Scene.controlEvent(ControlEvent)`; ControlP5 callbacks now terminate at the facade/internal UI boundary.
- Removed raw render queues, arbitrary dispose hooks, service construction/closure and scene/runtime escape hatches from `SceneServices`.
- Removed string-based output toggles, generic `setView`, public output producers and direct frame publication.
- Removed the pre-2.0 quaternion matrix alias in favor of the final explicit matrix API.

### Added — Scene lifecycle and services

- Added optional `Scene.configure(SceneServices)` before every activation `setupScene()`.
- Preserved `Scene.sceneRender(PGraphicsOpenGL)` as the only required abstract method.
- Made first registration, explicit activation, next/previous, index selection, reload, manager replacement, clear and facade disposal follow the same activation order.
- Added fresh activation-scoped `SceneServices` for every activation/reload of a scene instance.
- Added `FrameClock` using monotonic `double` time with bounded delta.
- Added bounded fixed-step `SimulationTimeline` with scene-controlled rate, position, step, maximum substeps, pause and dropped-unit telemetry.
- Added bounded keyed `SceneTaskGroup.submitIfIdle(...)` with callback-based results/errors and no public `Future`/executor.
- Added activation-aware `SceneAssets` for Processing images, shaders and retained shapes.
- Added `SceneActionMap` for named key/mouse actions while retaining raw key/mouse Scene callbacks.
- Added `SceneCameraService` for scene-space orbit input and target tracking.
- Added `SceneEnvironmentService` for activation-owned image, visibility, intensity and yaw overrides with conditional restoration.
- Added protocol-agnostic `ScenePorts`, `SceneInputPort` and `SceneOutputPort` SPI with bounded external-input delivery and backpressure telemetry.
- Added deferred `SceneServices.requestReload()` at a safe frame boundary.

### Changed — Scene and frame semantics

- Made `pre()` the authoritative per-frame/render-thread boundary.
- Guaranteed `Scene.update()` once per Processing frame before any render pass.
- Kept `sceneRender()` draw-only even when spherical capture invokes it for several faces.
- Kept target `beginDraw()`/`endDraw()` ownership in the library.
- Bound task results, render-queue work, ports, actions, camera input and environment state to the exact activation that created them.
- Made scene registration/activation consistently identity-based so `equals()` cannot collide with activation-service ownership.
- Made reload a complete stop-work → dispose → release → fresh-services → configure → setup cycle.

### Added — Rendering architecture

- Added native `GL_TEXTURE_CUBE_MAP` ownership behind the internal OpenGL boundary.
- Added one reusable Processing command target feeding six native cubemap faces.
- Added GLSL 4.10 `samplerCube` shaders under `data/shaders/samplercube/`.
- Added direct sibling Domemaster, Equirectangular and Skybox projections from the shared cubemap.
- Added internal requirement resolution so Standard and spherical domains run only when requested.
- Added environment composition shared by Standard and spherical representations.
- Added deferred renderer/FBO recreation for `resetGraphics(int)` at a safe draw boundary.

### Changed — Rendering behavior

- Kept Standard rendering independent from spherical capture.
- Reused one spherical cubemap capture for all spherical projections/consumers required in a frame.
- Removed the equirectangular intermediate from the active Domemaster path.
- Preserved cubemap face orientation and previous skybox cross-layout behavior.
- Preserved environment infinity: scene-camera orientation rotates it, while target translation and orbit distance do not.
- Kept `RenderMode.FULL` as the independent preview/output routing mode.
- Made dedicated modes temporary effective-view overrides that do not erase saved routes.
- Preserved `ViewType` declaration order because ControlP5 dropdown routing is index-based.

### Added — Camera and math

- Added immutable public `Quaternion` composition, normalization, slerp and matrix publication.
- Added public `SphericalOrientation` with cyclic control accumulators and normalized quaternion attitude.
- Expanded `OrbitCamera` with Processing-friendly `PVector` overloads, atomic `goTo`, immediate setters, snap, axis rotation and explicit input-state reset.
- Added numerical regression coverage for immediate drag/wheel manipulation, synchronized current/goal state, smooth programmatic movement and stale-anchor cleanup.

### Changed — Camera and input

- Restored immediate direct manipulation based on the SolarSystem 1.5 controller behavior while preserving smooth programmatic motion.
- Preserved the established navigation coefficients: 0.01 radians/pixel, yaw on Y, pitch on X, 80 standard wheel units and 0.001 trackpad setting.
- Routed each gesture to exactly one navigation camera.
- Made a visible ControlP5 control under the pointer own its gesture.
- Cleared drag anchors on release, owner change, scene switch/reload, pause and terminal disposal.
- Fixed ControlP5 2.2.6 key-code indexing through a guarded internal bridge.
- Routed the ControlP5 callback through the public facade to avoid illegal access to an internal UI class.

### Added — Typed outputs

- Made `OutputManager` a consumer-facing interface returned by the facade.
- Added frozen `OutputType` order `NDI`, `SPOUT`, `SYPHON`.
- Added output states `UNAVAILABLE`, `AVAILABLE`, `INITIALIZED`, `ENABLED`, `STOPPING`.
- Added typed `setOutputEnabled`, `isOutputEnabled`, `toggleOutput(OutputType)`, `setViewForOutput` and per-backend view conveniences.
- Added state/failure diagnostics and NDI captured/sent/dropped/failed counters.
- Added platform-local texture availability/name/view reporting without exposing a producer handle.

### Changed — Output runtime

- Made every output opt-in/disabled after setup.
- Kept Syphon and Spout on platform-local GPU-native `PGraphicsOpenGL` publication.
- Kept NDI as the explicit GPU-to-CPU/network boundary.
- Used three bounded NDI frame slots, latest-frame-wins backpressure and a dedicated non-OpenGL sender worker.
- Made normal NDI disable asynchronous (`STOPPING`) without joining a native send from the render thread.
- Kept only terminal shutdown eligible for a bounded worker wait.
- Updated the bundled Devolay dependency to `2.2.0-vic.2`; the proprietary NDI Runtime remains separate.

### Performance and allocation work

- Removed recurring frame-path allocation from quaternion matrix publication, orbit-camera state reads, environment orientation publication, cubemap request routing, NDI conversion/publication and ControlP5 synchronization.
- Reused internal physics and rendering buffers where ownership is stable.
- Migrated SolarSystem orbital inputs, elapsed time, anomaly/solver/perturbation intermediates and compensated time accumulation to `double`, converting only when publishing `PVector` state.
- Scaled SolarSystem fixed steps for very slow rates while retaining the normal maximum of `1/120` simulated day.
- Removed artificial SolarSystem locks made unnecessary by coherent publication at the frame boundary.
- Reworked SphereParticle snapshots around primitive buffers and bounded activation tasks to reduce GC jitter and eliminate unbounded submission.
- Preserved non-blocking OpenGL-thread behavior for external I/O.

### Examples

- Migrated `EmptyProject` and `Basic` to the facade-owned 2.0 lifecycle.
- Migrated `SphereParticle` to bounded activation work and frame-boundary publication.
- Added/updated `InfiniteBackground` for translation-invariant environment behavior.
- Updated `FulldomePBR` to retained shapes, packaged GLSL and the shared scene-space camera.
- Reworked `SolarSystem` as the reference consumer for clock, timeline, tasks, assets, actions, reload, target tracking and environment ownership.
- Kept `CalibrationTool` as the two-scene visual orientation/projection instrument.
- Kept `BenchmarkTool` as the graphical CPU/GPU qualification surface rather than a beginner example.
- Removed per-frame diagnostic console chatter from normal example operation; debug logging remains explicit.

### Documentation and research-software readiness

- Rebuilt the README as a Processing library homepage with statement of need, audience, installation, dependencies, examples, keywords, update date, qualification status, citation and API levels.
- Reclassified API documentation into Stable, Advanced Stable, Experimental, Processing Callback and Internal levels.
- Preserved 1.x names only in explicit migration/history material.
- Added detailed bilingual release notes, lifecycle/service/output documentation and research-software/JOSS-readiness evidence mapping without claiming JOSS submission or acceptance.
- Added Material for MkDocs Mermaid diagrams, tags, CI social cards, bilingual navigation and semantic page statuses.
- Replaced raster diagram placeholders with theme-aware Mermaid source.
- Replaced the hero placeholder with a traceable SVG/PNG editorial illustration.
- Kept real example/calibration/benchmark screenshots gated on installed-package qualification rather than publishing mock evidence.
- Updated documentation validation for Processing homepage/package requirements, API levels, research evidence, release-note parity, placeholder removal and advanced MkDocs configuration.

### Source organization

- Reorganized internal production sources under physical `_internal/` categories for output, performance, render camera/core/GL/modes, runtime, scene, support and UI.
- Mirrored that internal taxonomy in tests.
- Preserved package declarations where package-private collaboration requires them; the physical reorganization does not create a new callable namespace.
- Added tests that prevent uncategorized internal sources and public implementation leakage from returning.

### Fixed

- Bound/drained activation render work only at the authoritative frame boundary.
- Prevented stale task/port/action/environment work from one activation reaching another.
- Preserved configure-before-setup and dispose-before-service-release across every activation path.
- Prevented duplicate camera routing and UI-owned gestures from navigating the scene.
- Prevented disabled outputs from publishing automatically.
- Prevented NDI sender backpressure from blocking the OpenGL thread.
- Preserved environment state owned by a later activation during old-activation cleanup.
- Removed deprecated or nonexistent method names from current documentation/examples.
- Eliminated the ControlP5/Numberbox key-event illegal-access path by using the public facade callback.

### Removed — Runtime and resources

- Removed the six-`PGraphicsOpenGL` spherical fallback.
- Removed the old six-texture spherical shader passes.
- Removed the cubemap → equirectangular → domemaster dependency chain.
- Removed parallel/legacy renderer scaffolding and obsolete source directories after the internal taxonomy migration.
- Removed direct protocol dependencies from scene integration; optional control protocols use the small ports SPI.

### Validation

- Public API compatibility tests enforce final types, exact methods/constructors, enums, absence of mutable public fields and negative engine leaks.
- Lifecycle suites cover first activation, switching, reload, fresh services, order, idempotence, cancellation/isolation and state restoration.
- Numerical suites cover quaternion/orbit-camera/timeline/clock behavior.
- Output suites cover typed routes, states, bounded NDI shutdown and producer encapsulation.
- Render-policy/math tests cover Standard/spherical requirement separation and projection state without requiring OpenGL when pure state is sufficient.
- Release validation covers Java 17 build, Javadocs, Processing metadata/package shape, source/examples/reference/licenses, shader resources and byte-identical ZIP/PDEX.
- Documentation validation covers EN/PT parity, links, metadata, API vocabulary, Processing homepage requirements, research-software readiness and strict MkDocs.
- GPU image quality, projector/lens behavior and NDI/Syphon/Spout interoperability remain manual target-hardware gates.

### Release gates still open

- `v2.0.0` remains untagged until `maintainer/release-evidence.md` contains no unresolved applicable gate.
- Exact `tested.platform` and `tested.processingVersion` metadata remains blank until installed-package and physical qualification justify it.
- The external software DOI record must be verified before tagging.
- Final example/tool screenshots must be captured from the installed qualified package if they are to serve as release evidence.

### Not included in 2.0.0

- Spherical Mirror and any `SPHERICAL_MIRROR` enum value.
- HDR environment loading/render targets, IBL, irradiance, BRDF LUT or ambient-occlusion engine features.
- PBO/fence-based NDI readback.
- Headset, stereoscopic VR or generic XR runtime claims.
- Core MIDI/OSC/device dependencies; only the protocol-agnostic ports SPI is included.

## [1.5.0] - 2026-08-11

Version 1.5.0 is the final consolidation release for the 1.x renderer. It keeps the
lowercase `zividomelive` facade and the established Standard/spherical split while
making rendering intent, scene ownership, output lifecycle, calibration, testing,
and release evidence explicit.

### Added
- Public `RenderMode` API with `FULL`, `STANDARD`, `DOMEMASTER`, `EQUIRECTANGULAR`, and `SKYBOX`; `FULL` is the compatibility default.
- Public `SphericalOrientation` state and `resetOrientation()` support for cyclic, unit-quaternion pitch/yaw/roll composition.
- Public output lifecycle states and diagnostics through `OutputState`, `getOutputState()`, and `getOutputFailureReason()`.
- NDI failed-frame telemetry alongside the existing captured, sent, and dropped counters.
- Two-scene `CalibrationTool`: a GLSL 4.10 cube-face focus/color target and four original, resolution-selected Paul Bourke v14 equirectangular patterns mapped to a complete 360-degree sphere.
- Compatibility and regression coverage for the public API, preview sizing, Standard aspect policy, scene contract, output routing, spherical orientation, render requirements, lifecycle, packaging metadata, and example integrity.
- Dedicated `qualificationTests` Gradle evidence task and independent GitHub workflow, plus a documented GPU/native-output qualification protocol.

### Changed
- Centralized render-requirement resolution so preview, floating domemaster, and enabled outputs request only the passes they need.
- Made dedicated `RenderMode` values override effective preview/output representation without erasing the independent `ViewType` routes restored by `FULL`.
- Consolidated active-scene ownership in `SceneManager`, including deterministic setup, switching, disposal, pause/resume, and terminal shutdown behavior.
- Made the ControlP5 panel capability-aware: dedicated modes hide inapplicable projection and routing selectors, while `FULL` exposes independent preview and enabled-output routes.
- Made pitch, yaw, and roll panel sliders cyclic and replaced per-frame Euler reconstruction with shortest-delta local-axis composition on a normalized quaternion. Getter/setter values remain source-compatible control accumulators.
- Hardened NDI around three bounded frame slots, latest-frame-wins backpressure, a dedicated non-OpenGL worker, packed RGBA progressive frames, and bounded shutdown.
- Documented NDI as an experimental, unofficial, video-only output and added Windows, macOS, and Linux installation guidance for the runtime-separated Devolay dependency.
- Made Syphon and Spout initialization, publication, resize, failure reporting, shutdown, and explicit retry states observable without moving them off the GPU-native `PGraphicsOpenGL` path.
- Kept Standard preview dimensions tied to the Processing window, spherical preview resolution automatic, and output resolution independently deferred to the draw loop.
- Pinned Processing-side dependency downloads to immutable assets with SHA-256 verification.
- Hardened release packaging with project and third-party licenses, citation metadata, tag/version validation, least-privilege publication, strict bilingual documentation builds, and exclusion of tests and local compile-only helper JARs from installable artifacts.
- Restored the intended teaching roles of `Basic`, `EmptyProject`, and `SphereParticle`, refined `FulldomePBR`, preserved `SolarSystem`, and documented the Processing runtime dependency imports required by contributed-library sketches.

### Fixed
- Prevented redundant target-frame-rate requests and CalibrationTool playback profiles from restarting Processing's JOGL animator.
- Preserved domemaster Size% when render targets are recreated after an output-resolution change.
- Removed duplicate rendering and duplicate event forwarding from examples and documentation.
- Removed nested `beginDraw()` / `endDraw()` ownership from scene examples.
- Prevented ControlP5 output toggles from being processed twice.
- Prevented partial renderer initialization from advancing the lifecycle incorrectly.
- Restored enabled outputs after pause/resume and made repeated disposal idempotent.
- Replaced hard-coded NDI `150/1` interlaced metadata with the configured target frame rate and progressive frames.
- Added explicit recovery after local-output or NDI initialization failures.

### Compatibility
- The public `zividomelive` facade and `ViewType` order remain unchanged.
- A sketch that never calls `setRenderMode()` continues in `RenderMode.FULL`.
- Configured preview and per-output `ViewType` values survive temporary dedicated modes.
- Pitch/yaw/roll method signatures and returned accumulator values remain compatible; orientation is now composed incrementally in event order rather than reconstructed as an Euler triple.
- Legacy render convenience methods remain available as deprecated compatibility shims.
- The current 1.x cubemap-to-equirectangular-to-domemaster backend remains internal and is not promoted to a permanent API contract.
- No experimental 2.0 renderer, native cube-map backend, PBO, or OpenGL fence pipeline is included.

### Release Evidence
- Java 17 build, Javadocs, metadata checks, Processing ZIP/PDEX/TXT generation, package-content verification, and byte-identical ZIP/PDEX checks are automated.
- MkDocs builds English and Portuguese documentation in strict mode.
- Automated tests remain intentionally headless; GPU image quality and NDI/Syphon/Spout interoperability still require the documented CalibrationTool protocol on target hardware.

## [1.4.0] - 2026-08-02

### Added
- Native scene-space `OrbitCamera` service in the library API (`getSceneCamera()`, `setSceneCameraInputEnabled()`), including per-frame update and mouse event forwarding integration.
- New `FulldomePBR` example scene with PBR pipeline, custom shaders, and retained-mode `PShape`/VBO primitives.
- New `OrbitCamera` collapse guard (`setCollapseGuard(float)`) to prevent crossing/collapsing around distance zero.

### Changed
- Upgraded Processing core dependency to `4.5.6` and centralized dependency versions in Gradle.
- Standard view camera navigation moved to quaternion-based behavior with improved zoom controls and reset helpers.
- Standard preview rendering now uses a configurable sky background instead of transparent black.
- Release/build script quality refinements and cleanup.
- `FulldomePBR` example shader stack migrated to GLSL `#version 410` to match the OpenGL 4.1 core context.
- `FulldomePBR` visual style updated with a vibrant HSB-driven palette and richer lighting defaults.

### Fixed
- Dynamic near/far clipping in standard view to avoid object disappearance at large camera distances.
- Reduced frequency of OpenGL error `1282` by removing invalid nested `beginDraw()/endDraw()` calls in the NDI capture path. Note: the error is endemic to certain hardware/driver combinations and may still occur in some configurations.
- Lifecycle and initialization reliability improvements (register method flow, initialization state handling).
- `LogManager` thread-safety race condition fix.
- `FulldomePBR` camera handling corrected to scene-space navigation (without mutating dome control parameters).

### Docs
- Scene/render contract clarifications in API docs and Javadoc.
- README updates for shader target and tone mapping behavior.
