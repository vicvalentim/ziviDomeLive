# Basic Examples

## Basic

Demonstrates two managed scenes and the `RenderMode` API. In `Scene1`, press:

- `1`: `FULL`
- `2`: `STANDARD`
- `3`: `DOMEMASTER`
- `4`: `EQUIRECTANGULAR`
- `5`: `SKYBOX`
- `+` / `-`: adjust animation speed
- Mouse wheel: change the pillar orbit radius

Use Left/Right arrows to switch between the orbiting pillars and the labeled orientation cube. Press `R` in either scene to restore its animation defaults.

## EmptyProject

A minimal multi-file template with one scene registered through `setScene`, automatic Processing hooks, a visible reference cube, and an intentionally empty sketch `draw()`. Press `R` to reset the cube rotation.

## SphereParticle

A bounded particle-field example that advances simulation once in `update()` and performs graphics work only in `sceneRender()`. Click or drag to add bursts, press `Space` for a central burst, `C` to clear, and `R` to restore the initial field.

All examples preserve `sceneRender(PGraphicsOpenGL)` and never call `ziviDome.draw()` manually.
