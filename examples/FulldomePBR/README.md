# FulldomePBR example

A new fulldome-oriented Processing sketch that demonstrates:

- 3D primitives: spheres, boxes, and custom cylinders
- PBR-inspired materials using Processing lighting controls
- A star shell and dome grid for a more immersive fulldome composition
- The library's native scene-space camera service (`OrbitCamera`) for fluid navigation

## Controls

- `+` / `=`: increase orbital animation speed
- `-`: decrease orbital animation speed
- `[` / `]`: shrink / grow the module orbit radius
- `r`: reset orbit radius and rotation speed
- `v`: reset the camera position/orientation

### Camera navigation (all views)

Navigation uses the **native ziviDomeLive scene camera service**, retrieved via
`parent.getSceneCamera()` (an `OrbitCamera`). It lives in **scene space** — it
transforms the scene graphics directly and therefore works identically across
every projection (fisheye, equirectangular, cubemap, standard). It never changes
the dome parameters (`yaw`, `pitch`, `roll`, `fov`) articulated by the
ControlManager.

The example enables built-in input with `parent.setSceneCameraInputEnabled(true)`,
so the library handles the mouse automatically:

- Drag: orbit around the scene (quaternion rotation, gimbal-lock free)
- Mouse wheel: fly toward / away from the target (smooth zoom, trackpad aware)

The scene simply calls `parent.getSceneCamera().apply(pg)` inside `sceneRender`.
All target/orientation/distance changes are smoothly interpolated (SLERP/LERP).

## Notes

This example is designed to stay inside the current `Scene` contract:
`sceneRender(PGraphicsOpenGL)` does not call `beginDraw()` / `endDraw()`.
