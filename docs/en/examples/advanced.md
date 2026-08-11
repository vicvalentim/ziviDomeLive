# Advanced Examples

## CalibrationTool

A two-scene qualification tool. Scene 1 closes six face-local GLSL 4.10 targets around the observer with a 24 x 24 grid, geometric alignment references, 1/2/4/8-pixel focus lines, 1/2/3/4-pixel points and stars, continuous RGB/CMY gradients, solid swatches, grayscale, and clipping levels. Scene 2 maps Paul Bourke's original unmodified 8192 x 4096 v14 equirectangular pattern onto a complete 1800-unit sphere centered at `(0, 0, 0)`, whose north pole is `+Z`; `Space` toggles the recommended 60-second rotation, `,`/`.` steps one degree, and `C` resets it. Use Left/Right arrows to switch scenes.

## FulldomePBR

Demonstrates retained `PShape` geometry, GLSL 4.10 metallic-roughness shading, fixed-function fallback, and the shared scene-space `OrbitCamera`. The camera transforms scene content consistently without mutating spherical pitch/yaw/roll, and the scene releases camera input when disposed.

## SolarSystem

A multi-file application showing domain models, simulation time, shaders, textures, camera control, and scene integration.

Advanced examples still follow the same ownership rule: `sceneRender(PGraphicsOpenGL)` draws into an already-open target and does not call `beginDraw()` or `endDraw()`.
