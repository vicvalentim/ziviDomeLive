# FulldomePBR example

**Category:** Advanced
**Required dependency:** ControlP5 2.2.6

A fulldome-oriented Processing sketch that demonstrates:

- Full **PBR lighting via GLSL shaders** (metallic-roughness, Cook-Torrance GGX)
- **Retained-mode primitives** (`PShape` / VBO) built from `PVector` geometry for
  efficient rendering (sphere, box, cylinder are uploaded once and reused)
- A star shell and dome grid for a more immersive fulldome composition
- The library's native scene-space camera service (`OrbitCamera`) for fluid navigation

## PBR pipeline

- `data/pbr.vert` / `data/pbr.frag` implement a metallic-roughness workflow with
  GGX distribution, Smith geometry and Schlick-Fresnel, plus hemispheric IBL
  (sky/ground) and ACES filmic tone mapping with gamma correction. The shaders
  target **GLSL `#version 410`** (OpenGL 4.1 core), matching the library's OpenGL
  context.
- Lighting is evaluated in **eye space**. World-space lights are transformed with a
  `uViewMatrix` uniform (the scene's camera/view matrix, captured each frame), which
  keeps lighting consistent across every projection and all six cubemap faces.
- Four analytic lights are used (two directional + two point). Each object sets its
  material via uniforms: `uAlbedo`, `uMetallic`, `uRoughness`, `uEmissive`.
- If the shader fails to load, the sketch automatically falls back to Processing's
  fixed-function lighting so it always runs.

## Controls

- `+` / `=`: increase orbital animation speed
- `-`: decrease orbital animation speed
- `[` / `]`: shrink / grow the module orbit radius
- `p`: toggle PBR shader on/off (fixed-function fallback)
- `r`: reset orbit radius and rotation speed
- `v`: reset the camera position/orientation

### Camera navigation (all views)

Navigation uses the **native ziviDomeLive scene camera service**, received as
`services.camera()` during `configure(...)`. Its `orbit()` camera lives in **scene space** — it
transforms the scene graphics directly and therefore works identically across
every projection (fisheye, equirectangular, cubemap, standard). It never changes
the dome parameters (`yaw`, `pitch`, `roll`, `fov`) articulated by the
ControlManager.

The example enables built-in input with `camera.setInputEnabled(true)`,
so the library handles the mouse automatically:

- Drag: orbit around the scene (quaternion rotation, gimbal-lock free)
- Mouse wheel: fly toward / away from the target (smooth zoom, trackpad aware)

The scene simply calls `camera.apply(pg)` inside `sceneRender`.
All target/orientation/distance changes are smoothly interpolated (SLERP/LERP).
The runtime restores the previous input owner when the activation ends; `dispose()` only drops the
scene's references.

## Notes

This example is designed to stay inside the current `Scene` contract:
`sceneRender(PGraphicsOpenGL)` does not call `beginDraw()` / `endDraw()`.
