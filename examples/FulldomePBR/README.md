# FulldomePBR example

A new fulldome-oriented Processing sketch that demonstrates:

- 3D primitives: spheres, boxes, and custom cylinders
- PBR-inspired materials using Processing lighting controls
- A star shell and dome grid for a more immersive fulldome composition
- Interaction via the library's standard controls, plus scene-level orbit radius controls

## Controls

- `+` / `=`: increase orbital animation speed
- `-`: decrease orbital animation speed
- `[` / `]`: shrink / grow the module orbit radius
- `r`: reset orbit radius and rotation speed
- `v`: reset the dome camera (yaw, pitch, roll, FOV) via the library API

### Camera navigation (dome views)

The scene drives the ziviDomeLive dome camera through the public API
(`setYaw`, `setPitch`, `setRoll`, `setFov`):

- Left-drag: look around (horizontal = yaw, vertical = pitch)
- Right-drag: roll the horizon
- Mouse wheel: zoom via field of view

In `STANDARD` view the library's own `MouseControlledCamera` handles navigation,
so the scene defers to it and the mouse wheel adjusts the module orbit radius instead.

## Notes

This example is designed to stay inside the current `Scene` contract:
`sceneRender(PGraphicsOpenGL)` does not call `beginDraw()` / `endDraw()`.
