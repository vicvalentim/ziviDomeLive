# Advanced Examples

## CompatibilityLock

A static asymmetric scene that labels all six cubemap directions. Use it for GPU qualification of face identity, layout, mirroring, 90-degree rotations, FOV, Size%, Standard independence, and floating domemaster behavior. Press `0` to restore the canonical calibration state; accepted controls print their values to the console.

## FulldomePBR

Demonstrates retained `PShape` geometry, GLSL 4.10 metallic-roughness shading, fixed-function fallback, and the shared scene-space `OrbitCamera`. The camera transforms scene content consistently without mutating spherical pitch/yaw/roll, and the scene releases camera input when disposed.

## SolarSystem

A multi-file application showing domain models, simulation time, shaders, textures, camera control, and scene integration.

Advanced examples still follow the same ownership rule: `sceneRender(PGraphicsOpenGL)` draws into an already-open target and does not call `beginDraw()` or `endDraw()`.
