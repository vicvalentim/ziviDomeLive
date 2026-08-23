# Learning Examples — Foundations

These examples form the first learning path for the library. Qualification tools are documented separately because their purpose is to test a release or hardware configuration rather than teach the basic project model.

!!! note "Visual evidence policy"
    Screenshots used as release evidence must come from the installed, qualified package. This learning page uses source behavior as its authority and does not substitute an editorial mockup for a real capture.

## EmptyProject

A minimal multi-file starter template with one `Scene`, one `SceneManager`, automatic Processing hooks, an intentionally empty `sceneRender()`, and an intentionally empty sketch `draw()`.

Start here when creating a project from scratch.

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

## SphereParticle

A larger scene example that runs bounded simulation work through `SceneServices.tasks()`, isolates results to the current activation, and keeps graphics calls on the render thread. Click or drag to add particles.

All learning examples preserve `sceneRender(PGraphicsOpenGL)` and never call `ziviDome.draw()` manually.

<div class="zd-actions" markdown>
[Advanced Learning Examples](advanced.md){ .md-button .md-button--primary }
[BenchmarkTool](../qualification/benchmark-guide.md){ .md-button }
</div>
