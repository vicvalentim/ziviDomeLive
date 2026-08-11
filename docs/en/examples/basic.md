# Basic Examples

## Basic

Demonstrates two managed scenes and the `RenderMode` API. In `Scene1`, press:

- `1`: `FULL`
- `2`: `STANDARD`
- `3`: `DOMEMASTER`
- `4`: `EQUIRECTANGULAR`
- `5`: `SKYBOX`
- `+` / `-`: adjust animation speed

## EmptyProject

A minimal multi-file template with one `Scene`, one `SceneManager`, automatic Processing hooks, and an intentionally empty sketch `draw()`.

## SphereParticle

A larger scene example that uses a bounded executor for simulation work and keeps all graphics calls on the render thread. It demonstrates why state mutation belongs outside per-target draw ownership.

All examples preserve `sceneRender(PGraphicsOpenGL)` and never call `ziviDome.draw()` manually.
