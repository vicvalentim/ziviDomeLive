# Advanced Learning Examples

These examples show larger creative-project patterns while preserving the same Scene and render-thread contracts introduced by the foundation examples.

!!! note "Visual evidence policy"
    Final example captures belong to installed-package qualification. Until that evidence exists, this page describes the executable sketches without publishing a mock screenshot.

## InfiniteBackground

Qualifies the shared LDR Environment background with real and synthetic equirectangular sources. It exercises visibility, intensity, longitude offset, far-depth occlusion, and Standard/spherical orientation without scene-owned sky geometry.

## FulldomePBR

Demonstrates retained `PShape` geometry, GLSL 4.10 metallic-roughness shading, fixed-function fallback, and the shared scene-space `OrbitCamera`. The camera transforms scene content consistently without mutating spherical pitch/yaw/roll, and the scene releases camera input when disposed.

## SolarSystem

A multi-file application and reference consumer for `SceneServices`. It uses the library-owned frame clock, bounded fixed-step timeline, typed assets, named actions, deferred reload, cleanup, `OrbitCamera` target tracking, and scene-scoped Environment. The camera supports reset, drag, wheel zoom, and quaternion orientation; the same orientation drives the infinite background without a scene-owned sky sphere. Julian Date and orbital propagation remain application-domain adapters. Press `n` to toggle UTC clock diagnostics without printing every frame by default.

Advanced learning examples still follow the same ownership rule: `sceneRender(PGraphicsOpenGL)` draws into an already-open target and does not call `beginDraw()` or `endDraw()`.

<div class="zd-actions" markdown>
[Foundation Examples](basic.md){ .md-button }
[CalibrationTool](../qualification/calibration-tool.md){ .md-button }
[BenchmarkTool](../qualification/benchmark-guide.md){ .md-button }
</div>
