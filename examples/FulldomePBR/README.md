# FulldomePBR example

A new fulldome-oriented Processing sketch that demonstrates:

- 3D primitives: spheres, boxes, and custom cylinders
- PBR-inspired materials using Processing lighting controls
- A star shell and dome grid for a more immersive fulldome composition
- Fluid quaternion-based camera navigation through space (same approach as the SolarSystem example)

## Controls

- `+` / `=`: increase orbital animation speed
- `-`: decrease orbital animation speed
- `[` / `]`: shrink / grow the module orbit radius
- `r`: reset orbit radius and rotation speed
- `v`: reset the camera position/orientation

### Camera navigation (all views)

The scene owns a quaternion orbit camera that lives in **scene space** — it
transforms the scene graphics directly and therefore works identically across
every projection (fisheye, equirectangular, cubemap, standard). It never changes
the dome parameters (`yaw`, `pitch`, `roll`, `fov`) that are articulated by the
library's ControlManager.

- Drag: orbit around the scene (quaternion rotation, gimbal-lock free)
- Mouse wheel: fly toward / away from the target (smooth zoom, trackpad aware)

All target/orientation/distance changes are smoothly interpolated (SLERP/LERP)
for fluid motion.

## Notes

This example is designed to stay inside the current `Scene` contract:
`sceneRender(PGraphicsOpenGL)` does not call `beginDraw()` / `endDraw()`.
