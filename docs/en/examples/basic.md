# Basic Examples

## Basic

Demonstrates two managed scenes and the `RenderMode` API. In `Scene1`, press:

- `1`: `FULL`
- `2`: `STANDARD`
- `3`: `DOMEMASTER`
- `4`: `EQUIRECTANGULAR`
- `5`: `SKYBOX`
- `+` / `-`: adjust animation speed
- `r`: reset animation speed
- Mouse wheel: change the pillar orbit radius

Use Left/Right arrows to switch between the rotating pillars and the static six-face alignment grid.

## EmptyProject

A minimal multi-file starter template with one `Scene`, one `SceneManager`, automatic Processing hooks, an intentionally empty `sceneRender()`, and an intentionally empty sketch `draw()`.

## SphereParticle

A larger scene example that uses an executor for particle simulation and keeps graphics calls on the render thread. Click or drag to add particles.

All examples preserve `sceneRender(PGraphicsOpenGL)` and never call `ziviDome.draw()` manually.

Start from `EmptyProject`, use `Basic` to learn scene switching and render modes,
then inspect `SphereParticle` for the boundary between background simulation and
render-thread drawing.
