# Changelog

All notable changes to this project are documented in this file.

## [2.0.0] - 2026-08-12

Version 2.0.0 is the native-cubemap major release. It keeps the Processing
scene contract and the Standard/spherical rendering split, but replaces the
old six-Processing-target spherical backend with a GPU-resident
`GL_TEXTURE_CUBE_MAP` pipeline sampled directly by the final projection
shaders.

### Breaking Changes
- Renamed the public facade class from `zividomelive` to `ziviDomeLive`.
  The Java package remains `com.victorvalentim.zividomelive`.
- Extracted `ViewType` from the facade into the top-level public API:
  `com.victorvalentim.zividomelive.ViewType`.
- Replaced the 1.x view names with the final 2.0 names and order:
  `STANDARD`, `DOMEMASTER`, `EQUIRECTANGULAR`, `SKYBOX`.
- Advanced integrations that consumed internal `PGraphicsOpenGL[]` cubemap
  faces must migrate to the native cubemap path.

### Added
- Native `CubemapTarget` ownership for `GL_TEXTURE_CUBE_MAP` texture storage,
  cubemap face attachments, framebuffer binding, conservative texture
  filtering, and seamless-cubemap capability handling.
- `ProcessingGlAdapter` and `ProcessingGlCapabilities` as the narrow PGL seam
  for graphics allocation, texture checks, framebuffer operations, cubemap
  binding, NDI readback, disposal, and OpenGL capability discovery.
- `CubemapFace` as the canonical table for the six native cubemap faces and
  their orientation contract.
- `RenderPipeline` to own per-frame orchestration while preserving the facade's
  public lifecycle and Processing hook behavior.
- `FrameViews` as the minimal final-frame boundary used by output publishers.
- Separate concrete output backend services for NDI, Syphon, and Spout without
  introducing a backend factory layer.
- GLSL 4.10 `samplerCube` shader set under `data/shaders/samplercube/` for
  cubemap layout, equirectangular projection, fisheye/domemaster projection,
  and skybox inspection.
- LDR equirectangular environment background service that paints a `PImage`
  as an infinite far-depth background after scene rendering.
- Publication-focused documentation, including Processing Contribution Manager
  metadata guidance, release packaging checks, generated Javadocs links, and
  bilingual MkDocs navigation.

### Changed
- Replaced the six independent Processing cubemap face targets with native
  cubemap face capture through a reusable framebuffer.
- Kept `Scene.sceneRender(PGraphicsOpenGL)` as the Processing-facing scene
  contract. The library continues to own `beginDraw()` and `endDraw()`.
- Moved sky/star-field background ownership for spherical modes into the
  render pipeline through `setEquirectangularBackground(PImage)`.
- Kept `STANDARD` rendering independent from spherical cubemap capture.
- Rendered equirectangular output directly from the native cubemap with a
  `samplerCube` shader.
- Rendered domemaster/fisheye output directly from the native cubemap,
  removing the equirectangular intermediate from that path.
- Rendered the skybox/cubemap layout directly from the native cubemap while
  preserving the original `CubemapView` cross-matrix orientation.
- Updated render requirements so domemaster, equirectangular, and skybox are
  sibling projections fed by the same cubemap source instead of a lateral
  projection chain.
- Kept `RenderMode.FULL` as the default operational mode and preserved
  independent preview and output routes.
- Kept dedicated `RenderMode` values as temporary effective-view overrides
  that do not erase stored `ViewType` routing.
- Kept Syphon and Spout on the GPU-native `PGraphicsOpenGL` publication path.
- Kept NDI as the explicit GPU-to-CPU/network boundary with Processing-thread
  pixel capture, three bounded frame slots, latest-frame-wins backpressure, a
  dedicated sender worker, progressive RGBA metadata, and bounded shutdown.
- Reduced native cubemap logging noise while preserving allocation, failure,
  capability, and bounded GL-error diagnostics.
- Reworked the README and MkDocs site into a Processing-library manual with
  installation, dependencies, API, examples, architecture, qualification, and
  publication pages.
- Updated Processing metadata to point to the stable public documentation URL
  and to describe platform-specific output dependencies.

### Fixed
- Corrected native equirectangular orientation against the `sampleCube`
  reference path.
- Preserved skybox face positions and rotations from the original
  `CubemapView` layout when switching to native `samplerCube` sampling.
- Avoided the black-screen regression by returning to the single reusable
  `PGraphicsOpenGL` command target feeding native cubemap faces.
- Prevented the removed spherical fallback from masking native cubemap capture
  failures during validation.
- Kept the known Processing/OpenGL `1282` teardown diagnostic documented as a
  non-fatal runtime note unless paired with visible rendering failure.

### Removed
- Removed the `PGraphicsOpenGL[]` spherical fallback path from the runtime
  renderer.
- Removed the old six-texture Processing spherical shader passes from packaged
  resources.
- Removed the cubemap-to-equirectangular-to-domemaster dependency chain from
  the active spherical pipeline.
- Removed permanent legacy/parallel renderer scaffolding from the 2.0 runtime.

### Validation
- Added or updated tests for facade rename, top-level `ViewType`, enum order,
  `FrameViews`, render requirements, cubemap face mappings, GL capability
  handling, samplerCube shader resources, metadata, packaging, documentation
  publication, and output routing boundaries.
- Automated gates cover Java compilation, unit tests, Javadocs,
  Processing-library metadata, release ZIP/PDEX/TXT generation, packaged
  shader resources, legal files, and byte-identical ZIP/PDEX output.
- MkDocs builds English and Portuguese documentation in strict mode and the
  GitHub Pages workflow publishes generated Javadocs for both localized site
  trees.
- GPU image quality, projector/lens behavior, and NDI/Syphon/Spout receiver
  interoperability remain manual qualification tasks on target hardware.

### Not Included in 2.0.0
- Spherical Mirror is not included and no `SPHERICAL_MIRROR` enum value is
  exposed yet.
- HDR environment loading, HDR render targets, IBL/PBR engine features, ambient occlusion, direct NDI
  RGBA readback, PBO/fence-based NDI transfer, and performance telemetry remain
  future work.
- NDI still uses the safe Processing-thread `loadPixels()` readback boundary;
  no OpenGL calls are made by the NDI sender worker.

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
