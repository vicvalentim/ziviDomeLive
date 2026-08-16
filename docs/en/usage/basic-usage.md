# Render Modes and View Types

ziviDomeLive separates **how the application is currently working** from **which representation a destination receives**.

![RenderMode and ViewType](../../img/render-modes-overview.png)

## RenderMode: how do I want to work now?

`RenderMode` defines the effective global working mode:

- `FULL`
- `STANDARD`
- `DOMEMASTER`
- `EQUIRECTANGULAR`
- `SKYBOX`

`FULL` is the default. It preserves the independent preview and output routes configured through `ViewType`.

Dedicated modes temporarily override the effective representation. They do **not** erase the stored routes that reappear when you return to `FULL`.

## ViewType: which representation goes to this destination?

`ViewType` identifies the final representation requested by preview or an external output:

- `STANDARD`
- `DOMEMASTER`
- `EQUIRECTANGULAR`
- `SKYBOX`

This distinction matters most in `FULL`: a Standard preview can coexist with a Domemaster NDI output, for example, without turning those destinations into the same route.

## What RenderMode does not mean

A render mode is not a second runtime class and does not replace the `ziviDomeLive` instance. The public model remains one runtime with multiple working modes.
