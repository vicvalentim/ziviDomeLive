# Changelog

All notable changes to this project are documented in this file.

## [1.5.0] - 2026-08-10

### Added
- Public `RenderMode` API with `FULL`, `STANDARD`, `DOMEMASTER`, `EQUIRECTANGULAR`, and `SKYBOX`; `FULL` remains the compatibility default.
- Compatibility-lock tests for the public API, preview sizing, Standard aspect policy, scene contract, output routing, spherical orientation, and render requirements.
- Static asymmetric GPU qualification scene and a documented visual/hardware qualification protocol.
- Public output lifecycle states and diagnostics through `OutputState`, `getOutputState()`, and `getOutputFailureReason()`.
- NDI failed-frame telemetry alongside the existing captured, sent, and dropped counters.

### Changed
- Centralized render-requirement resolution so preview, floating domemaster, and enabled outputs request only the passes they need.
- Consolidated active-scene ownership in `SceneManager`, including deterministic setup, switching, disposal, pause/resume, and terminal shutdown behavior.
- Organized ControlP5 controls into global, spherical, view, and output scopes while preserving widget order and callback ownership.
- Hardened NDI around three bounded frame slots, latest-frame-wins backpressure, a dedicated non-OpenGL worker, packed RGBA progressive frames, and bounded shutdown.
- Made Syphon and Spout initialization, publication, resize, failure reporting, shutdown, and explicit retry states observable without moving them off the GPU-native `PGraphicsOpenGL` path.
- Pinned Processing-side dependency downloads to immutable assets with SHA-256 verification.
- Replaced `CompatibilityLock` with the two-scene `CalibrationTool`, combining a GLSL 4.10 cube-face focus/color chart with Paul Bourke's unmodified 8192 x 4096 equirectangular test pattern on a complete sphere; also refined `FulldomePBR`, expanded the example catalog, and documented Processing runtime dependency imports.

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
- Legacy render convenience methods remain available as deprecated compatibility shims.
- The current 1.x cubemap-to-equirectangular-to-domemaster backend remains internal and is not promoted to a permanent API contract.
- No experimental 2.0 renderer, native cube-map backend, PBO, or OpenGL fence pipeline is included.

### Docs
- Reworked the README, bilingual documentation, examples, Javadocs, release metadata, platform matrix, and qualification checklist for the 1.5.0 release.

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
