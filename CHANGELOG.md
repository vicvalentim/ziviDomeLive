# Changelog

All notable changes to this project are documented in this file.

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
- OpenGL error `1282` caused by nested `beginDraw()/endDraw()` in scene rendering flow.
- Lifecycle and initialization reliability improvements (register method flow, initialization state handling).
- `LogManager` thread-safety race condition fix.
- `FulldomePBR` camera handling corrected to scene-space navigation (without mutating dome control parameters).

### Docs
- Scene/render contract clarifications in API docs and Javadoc.
- README updates for shader target and tone mapping behavior.
