# Advanced Examples

## CalibrationTool

A two-scene qualification tool. Scene 1 maps six GLSL 4.10 targets onto explicit face-local `0..1` coordinates of a closed cube, so its 24 x 24 grid, geometric references, focus lines, points, stars, gradients, swatches, grayscale, clipping levels, and annotations follow the spherical pitch/yaw/roll transforms as one surface. Scene 2 selects one of four original unmodified Paul Bourke v14 equirectangular patterns for the active 1k/2k/3k/4k output bucket, or the bucket nearest the window when outputs are disabled, and maps it onto a complete 1800-unit sphere centered at `(0, 0, 0)`, whose north pole is `+Z`. `Space` toggles the recommended 60-second rotation; `T` selects a time-quantized 30 fps/1800-frame or 60 fps/3600-frame rotation profile without changing Processing's global frame rate; `,`/`.` steps one degree; and `C` resets it. Use Left/Right arrows to switch scenes.

Follow the [Calibration Tool Protocol](../qualification/1.5-calibration-tool.md)
when using this example as release evidence.

## FulldomePBR

Demonstrates retained `PShape` geometry, GLSL 4.10 metallic-roughness shading, fixed-function fallback, and the shared scene-space `OrbitCamera`. The camera transforms scene content consistently without mutating spherical pitch/yaw/roll, and the scene releases camera input when disposed.

## InfiniteBackground

Qualifies the shared LDR Environment background with real and synthetic equirectangular sources. It exercises visibility, intensity, longitude offset, far-depth occlusion, and Standard/spherical orientation without scene-owned sky geometry.

## SolarSystem

A multi-file application and reference consumer for `SceneServices`. It uses the library-owned frame clock, bounded fixed-step timeline, typed assets, named actions, deferred reload, cleanup, `OrbitCamera` target tracking, and scene-scoped Environment. The camera supports reset, drag, wheel zoom, and quaternion orientation; the same orientation drives the infinite background without a scene-owned sky sphere. Julian Date and orbital propagation remain application-domain adapters. Press `n` to toggle UTC clock diagnostics without printing every frame by default.

Advanced examples still follow the same ownership rule: `sceneRender(PGraphicsOpenGL)` draws into an already-open target and does not call `beginDraw()` or `endDraw()`.
