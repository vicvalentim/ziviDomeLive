# Learning Examples — Foundations

These examples form the first learning path for the library. Qualification tools are documented separately because their purpose is to test a release or hardware configuration rather than teach the basic project model.

!!! note "Visual evidence policy"
    Screenshots used as release evidence must come from the installed, qualified package. This learning page uses source behavior as its authority and does not substitute an editorial mockup for a real capture.

## EmptyProject

A minimal multi-file starter template with one facade-owned `Scene`, automatic Processing hooks, an intentionally empty `sceneRender()`, and an intentionally empty sketch `draw()`.

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

## NamedActions

Demonstrates activation-owned key-code and mouse actions, named registration and programmatic
triggering. The actions move a lit sphere through a 3D composition. Its explicit
`applyWithViewLighting(...)` call attaches an ambient/spotlight rig to the scene camera and aims it
at the current target. The initial negative orbit distance places the composition in the
Domemaster front hemisphere without changing global Pitch/Yaw/Roll calibration. Drag to orbit,
use the mouse wheel or trackpad to zoom, and press `R` to restore the centered camera; clicking
moves the sphere without competing with drag navigation.

## PortLoopback

Demonstrates the bounded `ScenePorts` adapter SPI without adding a transport dependency. Integer
messages drive a 3D signal ring and a non-blocking output adapter reports the applied level. Its
camera uses the same Domemaster-centered negative-distance convention: drag to orbit, use the
mouse wheel or trackpad to zoom, and press `R` to restore the initial view.

All learning examples preserve `sceneRender(PGraphicsOpenGL)` and never call `ziviDome.draw()` manually.

<div class="zd-actions" markdown>
[Advanced Learning Examples](advanced.md){ .md-button .md-button--primary }
[BenchmarkTool](../qualification/benchmark-guide.md){ .md-button }
</div>
