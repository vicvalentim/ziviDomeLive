# FulldomePBR example

A new fulldome-oriented Processing sketch that demonstrates:

- 3D primitives: spheres, boxes, and custom cylinders
- PBR-inspired materials using Processing lighting controls
- A star shell and dome grid for a more immersive fulldome composition
- Interaction via the library's standard controls, plus scene-level orbit radius controls

## Controls

- `+` / `=`: increase orbital animation speed
- `-`: decrease orbital animation speed
- `r`: reset orbit radius and rotation speed
- Mouse wheel: change module orbit radius

## Notes

This example is designed to stay inside the current `Scene` contract:
`sceneRender(PGraphicsOpenGL)` does not call `beginDraw()` / `endDraw()`.
