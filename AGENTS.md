# AGENTS Guide for ziviDomeLive 2.0

## Scope, authority, and current state
- This repository is a Processing 4 / Java 17 library for fulldome, spherical, and immersive rendering. The public facade is `com.victorvalentim.zividomelive.ziviDomeLive`.
- Treat sources of truth in this order: `src/main/java` -> `src/test/java` -> executable examples -> `build.gradle.kts` and workflows -> README/docs.
- The 2.0 public API freeze is implemented and guarded by compatibility tests. Documentation is maintained against that frozen contract, but must never override source or executable tests when drift is found.
- This file is canonical. `.github/copilot-instructions.md` is a rendering/release supplement and must not contradict it.
- Do not describe a planned refactor as implemented until source and tests prove it, and do not restore pre-2.0 compatibility surface solely because historical documentation names it.
- Preserve unrelated user changes in a dirty worktree. Use `apply_patch` for edits and keep changes scoped.

## Source taxonomy and visibility
- Artist-facing API remains in `src/main/java/com/victorvalentim/zividomelive`, with the deliberate public subpackages `manager`, `performance`, `render`, and `render/camera`.
- Engine implementations are physically grouped by responsibility under `_internal/{output,performance,render,runtime,scene,support,ui}`. They intentionally retain the root `com.victorvalentim.zividomelive` package declaration so package-private collaborators can work across those physical categories.
- Every top-level type under `_internal` must remain package-private. Do not import an `_internal` source from sketches, expose it in a public signature, or turn the physical directory name into a Java package without a deliberate architecture change.
- `InternalSourceLayoutTest` protects the directory taxonomy and `PublicApiCompatibilityTest` protects the exported type/member snapshot. Update either test only for an intentional 2.0 contract change.

## Processing-facing programming model
- Keep artist code close to ordinary Processing: configure in `setupScene()`, mutate once in `update()`, draw in `sceneRender(...)`, and receive `keyEvent(...)`/`mouseEvent(...)` when direct callbacks are simplest.
- `Scene` is the extension contract. Its only required abstract method is `sceneRender(PGraphicsOpenGL)`; lifecycle, input, disposal, and naming methods remain defaults.
- `Scene.configure(SceneServices)` is optional and must run before the matching `setupScene()` for every activation.
- A `Scene` instance may be activated repeatedly. `dispose()` ends one activation; it does not necessarily mean the Java object will never be used again.
- Mutable simulation state advances in `Scene.update()`, once per Processing frame. Spherical capture may invoke `sceneRender(...)` multiple times in that frame, so rendering must not advance physics, timelines, counters, or mutable randomness shared by cubemap faces.
- The library owns the supplied target's `beginDraw()`/`endDraw()`. Scenes must not call them and must not retain the callback target as scene-owned graphics state.

## Facade and scene lifecycle authority
- `ziviDomeLive` owns the authoritative `SceneManager`, Processing hooks, activation services, renderer synchronization, input routing, outputs, and terminal disposal.
- The facade attaches a `SceneManager.LifecycleListener` so services are prepared before setup, prepared for shutdown before scene disposal, and released afterward.
- The activation order is contractual:
  1. create activation-scoped services;
  2. call `Scene.configure(services)`;
  3. call `Scene.setupScene()`;
  4. run frame/input callbacks;
  5. stop accepting activation work;
  6. call `Scene.dispose()`;
  7. release activation resources.
- Reload performs a complete dispose/setup cycle and supplies fresh `SceneServices` to the same scene instance.
- Every path must honor the same order: first registration, `setScene`, `registerScene`, next/previous, index selection, reload, manager replacement, clear, and facade disposal.
- Services are currently stored in an `IdentityHashMap`. Scene registration, lookup, activation, and release must use a consistent instance-identity policy; do not mix identity ownership with `equals()`-based management.
- Prefer facade-owned registration. A detached/replacement `SceneManager` must be attached to the facade lifecycle before it performs the first setup.

## Per-frame runtime order
- Processing hooks are registered by the facade constructor. Initialization is split between `setup()` and lazy renderer initialization in `post()`.
- `pre()` is the authoritative frame and render-thread boundary:
  1. begin performance tracking and synchronize the active scene;
  2. bind/drain the activation render queue and bounded external-input ports;
  3. tick `FrameClock`;
  4. consume a pending reload or call `Scene.update()`;
  5. refresh a tracked scene-camera target;
  6. advance camera interpolation once;
  7. publish camera orientation to the environment state.
- `draw()`/`RenderPipeline` render the already-updated state, produce requested projections, publish enabled outputs, draw preview, and finally draw the ControlP5 panel.
- Until managers are ready, drawing is gated and clears safely instead of running a partial pipeline.

## Current SceneServices contract
- `SceneServices` belongs to exactly one activation. The facade creates, advances, and closes it; scenes receive it through `configure`.
- Processing-facing entry points currently are:
  - `applet()`: owning `PApplet`;
  - `frameClock()`: monotonic per-frame timing;
  - `timeline()`: bounded fixed-step simulation;
  - `tasks()`: activation-scoped background work on the shared executor;
  - `assets()`: Processing images, shaders, and retained shapes;
  - `actions()`: named key/mouse actions while retaining raw Scene callbacks;
  - `camera()`: scene-space orbit camera, input, and target tracking;
  - `environment()`: activation-owned environment overrides;
  - `ports()`: bounded activation-owned bindings for optional external message adapters;
  - `requestReload()`: deferred reload at a safe frame boundary.
- `parent()`, `scene()`, `renderQueue()`, `onDispose()`, `isClosed()`, and `close()` are not artist-facing. Keep runtime ownership internal rather than restoring these escape hatches.
- Lifecycle controls on child services—construction, clock ticking, queue draining, input dispatch, raw caches, and closure—are internal. `SimulationTimeline` controls remain scene-facing because simulation position and policy belong to the scene.
- Keep the hierarchy shallow: `Scene -> SceneServices -> focused concrete service`. Do not add a dependency-injection framework, global internal service locator, deep interface hierarchy, or duplicate aliases.

## Service ownership rules
- Runtime-owned: service construction/closure, frame ticking, queue binding/draining, input dispatch, deactivation cancellation, camera update, environment restoration, cache shutdown, and reload execution.
- Scene-controlled: simulation rate/position, task submission, asset requests, action bindings, camera pose/configuration, optional mouse enablement, target tracking, and activation environment values.
- A scene never closes a runtime-supplied service. Only the input/output port provider SPI exposes `AutoCloseable`, so adapter authors can implement lifecycle while `ScenePorts` retains activation ownership.
- `SceneTaskGroup` submits callback-based work to the package-private, process-wide `SharedTaskExecutor`; never create an executor per scene and never expose the executor or a `Future` to artist code. The removed public `ThreadManager` is not part of 2.0.
- Facade disposal closes each activation's `SceneTaskGroup` and cancels its work, but does not shut down the process-wide daemon executor. The bounded NDI sender worker is an intentional output-specific exception with its own native lifecycle.
- Background work must not call Processing/OpenGL APIs. Render-thread publication belongs to the activation queue, and old-activation work must not reach a new activation of the same scene.
- `SceneAssets` creates Processing/GPU-facing assets on the bound render thread. Borrowed resources drop references on disposal; owned native/GPU resources need deterministic disposers.
- `SceneEnvironmentService` restores only values it touched and only when facade state still matches the value it applied, so a later owner is not overwritten.

## Mouse and scene-camera regression contract
- The SolarSystem controller from v1.5.0 is the behavioral reference for direct orbit navigation, not delayed generic smoothing.
- Direct manipulation is immediate; programmatic movement may be smooth:
  - drag synchronizes current and goal orientation;
  - wheel synchronizes current and goal distance;
  - `setTarget`, `goTo`, and tracked targets may interpolate.
- SolarSystem coefficients are contractual unless deliberately redesigned: `0.01` radians/pixel, Y-axis yaw, X-axis pitch, `80` standard wheel units, and the existing `0.001` trackpad setting.
- Route a gesture to exactly one navigation camera. Do not restore historical double-routing between scene and Standard cameras.
- Named mouse actions and raw `Scene.mouseEvent` callbacks remain compatible; built-in navigation is routed afterward.
- A visible ControlP5 control under the pointer owns its gesture; UI interaction must not orbit or zoom the scene camera.
- Clear drag anchors on release, input-owner changes, scene switch/reload, pause, and terminal disposal. Service-owned input restores the state it replaced.
- `OrbitCamera` works in scene space inside `sceneRender(...)`; dome yaw/pitch/roll/FOV remain separate projection controls.
- Preserve numerical `OrbitCameraTest` coverage for immediate drag/wheel, synchronized goals, smooth programmatic motion, and stale-anchor cleanup.

## Time and simulation contract
- `FrameClock` and `SimulationTimeline` use `double`; do not introduce service-layer casts to `float`.
- `SimulationTimeline` is a bounded fixed-step accumulator. It limits catch-up work and records dropped units after stalls.
- Step policy belongs to each scene. Do not globally force a rate-dependent step because simulations have different stability/performance requirements.
- SolarSystem is a regression example for very slow rates:
  - its physics step scales down with time rate to avoid hold-and-jump translation;
  - the normal maximum step remains `1/120` simulated day;
  - JSON orbital parameters, time, anomaly, Kepler/Newton calculations, and perturbation intermediates use `double`;
  - elapsed orbital time uses compensated summation;
- each computed state is published into Processing's float-based `PVector`; do not move float conversion earlier into time, anomaly, solver, or perturbation calculations;
  - reusable physics buffers avoid per-step GC jitter.
- Keep SolarSystem stable at `1x`, `0.1x`, `0.01x`, and lower rates. Do not trade slow-motion smoothness for global timeline complexity.

## Rendering and graphics invariants
- The projection pipeline remains:
  - `_internal/render/core/CubemapRenderer.java`: six native cubemap faces;
  - `_internal/render/modes/EquirectangularRenderer.java`: cubemap to equirectangular;
  - `_internal/render/modes/FisheyeDomemaster.java`: equirectangular to domemaster;
  - `_internal/render/modes/StandardRenderer.java` and `CubemapViewRenderer.java`: optional viewers.
- `ViewType` and `RenderMode` are top-level public enums. Keep `ViewType` declaration order stable because ControlP5 dropdowns map by index.
- Resolution changes are deferred: setters record a pending reset; renderer/FBO allocation happens at a safe draw boundary.
- Shader resources are packaged under `data/shaders`; keep Gradle copying and runtime paths aligned.
- Preserve environment infinity: camera rotation affects the environment, while target translation and orbit distance do not.
- Prefer pure math/state extraction for renderer tests and avoid OpenGL contexts when behavior can be tested independently.

## Input, controls, outputs, and integrations
- Built-in keys remain: `h` toggles the panel, `m` cycles `ViewType`, and Left/Right switch scenes when ControlP5 text input is inactive.
- The facade automatically routes Processing key/mouse callbacks to named actions and the active Scene's raw callbacks. Sketches must not forward them again.
- `ControlManager` owns ControlP5 widgets. `ziviDomeLive.controlEvent(ControlEvent)` is a public Processing/ControlP5 callback adapter for the built-in panel, not a Scene callback or artist command; do not restore `Scene.controlEvent(...)`.
- Guard ControlP5 2.2.6 key-code indexing through `ControlP5KeyEventBridge`, and never register package-private UI implementation objects as Processing callback targets.
- `OutputManager` coordinates NDI, Syphon, and Spout with independent view routing. Outputs start opt-in/disabled after setup; do not reintroduce automatic publication.
- Syphon is macOS-gated, Spout is Windows-gated, and NDI initializes when enabled and supported. Linux has reduced local texture-sharing support.
- Keep bounded non-blocking output workers and explicit shutdown. Never perform external I/O on the OpenGL thread.
- MIDI/OSC/device control connects through the protocol-agnostic `ScenePorts` SPI, not through `OutputManager` or a core protocol dependency. Real adapters remain optional; external-thread input is bounded and delivered at a frame boundary to the correct activation.

## Public API governance for 2.0
- Optimize for a didactic Processing API: few concepts, concrete names, direct calls, safe defaults, and teachable examples.
- Stability is explicit: core facade/Scene/render selection types are Stable; opt-in services, outputs, quaternion/orbit utilities, and ports are Advanced Stable; performance and GPU diagnostics are Experimental. Keep Javadocs and compatibility tests aligned with these tiers.
- Protect `Scene` before simplifying services. Do not add required Scene methods or move runtime cleanup into scenes.
- Prefer the smallest visibility/lifecycle fix over a broad rewrite. Avoid event buses, generic dependency containers, interface explosions, and unrelated renderer/output changes.
- Before removing/internalizing a public method, characterize current use, add lifecycle tests, and update `PublicApiCompatibilityTest` deliberately. Do not keep unsafe duplicate aliases indefinitely.
- Public surface and runtime ownership must eventually agree; documentation alone is not sufficient protection.
- Keep protocol adapters outside the core; add only a small registration/binding SPI when integrations are explicitly in scope.

## Surgical-change discipline
- Every implementation task must name the contract being changed and the contracts that must remain untouched before editing code.
- Change the fewest production files and public symbols that can completely solve the verified problem. A nearby imperfection is not automatically in scope.
- Do not combine lifecycle sanitation with opportunistic renames, formatting sweeps, package moves, renderer cleanup, output changes, example rewrites, or documentation expansion.
- Cross a subsystem boundary only when evidence shows the fix cannot be correct inside the original boundary. State that reason in the task summary and add a regression test at the boundary.
- Prefer characterization test -> minimal production patch -> focused test -> full relevant validation. Do not pre-build abstractions for hypothetical consumers.
- Preserve existing behavior by default. Any intentional behavior or source-compatibility change must be isolated, named, tested, and justified as part of the 2.0 contract.
- Examples should change only when they exercise the affected public contract or contain the bug itself. Never use example migration as permission to redesign unrelated example code.
- Review the final diff for contamination: unrelated imports, visibility changes, renamed concepts, generated files, broad comments, and incidental formatting should be removed.
- If a clean solution would require a materially broader redesign, stop at the safe boundary, document the follow-up, and do not silently expand scope.

## Testing and validation
- Main CI: `./gradlew build`; focused tests: `./gradlew test`; preferred full validation after API/lifecycle changes: `./gradlew clean test build`.
- Release qualification: `./gradlew clean qualificationTests`; release packaging: `./gradlew buildReleaseArtifacts`.
- Important suites cover SceneManager/lifecycle, public API compatibility, camera/quaternion math, timeline/clock, output lifecycle, and render-state logic.
- Any Services change must cover configure-before-setup, first activation, switch, reload, fresh services, disposal order, old-task isolation, state restoration, and idempotence.
- Compile every affected Processing example against the just-built library artifact, not an unrelated sketchbook installation. SolarSystem remains the minimum numerical/lifecycle regression sketch:
  `processing-java --sketch=examples/SolarSystem --output=/tmp/zividomelive-solarsystem-build --force --build`.
- Documentation qualification builds the bilingual MkDocs export, runs `./gradlew attachJavadocsToSite` to place one canonical Java reference at `site/reference`, and validates the exported routes with `python3 tools/validate_documentation.py --root . --site-dir site`. Portuguese pages must link back to that canonical tree rather than duplicate Javadocs below `site/pt`.
- Run `git diff --check` and inspect the final public surface. Build success alone does not prove lifecycle or artist-facing API quality.

## Build, packaging, docs, and release
- `compileJava` depends on `downloadDependencies`; its script fills `src/main/libs` when needed.
- Release artifacts: `./gradlew buildReleaseArtifacts`; sketchbook deployment: `./gradlew deployToProcessingSketchbook`.
- Docs use MkDocs Material, Mermaid diagrams, and bilingual suffix files. Keep API/reference pages, executable examples, release notes, and the packaged artifact mutually consistent.
- Use `LogManager.getLogger()`; debug logs are also written under `/tmp/zividomelive/logs` on non-Windows.
- The 2.0 project-authored licensing authority is Apache-2.0. Historical releases and third-party material keep their original terms. Preserve `LICENSE`, citation metadata, bilingual license pages, `THIRD_PARTY.md`, `examples/SolarSystem/THIRD_PARTY.md`, and `examples/SolarSystem/ASSET_PROVENANCE.json` in sync; never collapse NASA/JPL scientific provenance, Solar System Scope/INOVE CC BY 4.0 textures, or ESO/S. Brunier CC BY 4.0 media into the project Apache license.
