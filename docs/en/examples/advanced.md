# Advanced Examples

## CalibrationTool

A two-scene qualification tool. Scene 1 maps six GLSL 4.10 targets onto explicit face-local `0..1` coordinates of a closed cube, so its 24 x 24 grid, geometric references, focus lines, points, stars, gradients, swatches, grayscale, clipping levels, and annotations follow the spherical pitch/yaw/roll transforms as one surface. Scene 2 maps Paul Bourke's original unmodified 8192 x 4096 v14 equirectangular pattern onto a complete 1800-unit sphere centered at `(0, 0, 0)`, whose north pole is `+Z`; `Space` toggles the recommended 60-second rotation, `,`/`.` steps one degree, and `C` resets it. Use Left/Right arrows to switch scenes.

## FulldomePBR

Demonstrates retained `PShape` geometry, GLSL 4.10 metallic-roughness shading, fixed-function fallback, and the shared scene-space `OrbitCamera`. The camera transforms scene content consistently without mutating spherical pitch/yaw/roll, and the scene releases camera input when disposed.

## SolarSystem

A multi-file application showing domain models, simulation time, shaders, textures, camera control, and scene integration.

Advanced examples still follow the same ownership rule: `sceneRender(PGraphicsOpenGL)` draws into an already-open target and does not call `beginDraw()` or `endDraw()`.
