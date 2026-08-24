# EmptyProject

**Category:** Getting Started
**Required dependency:** ControlP5 2.2.6

The smallest ziviDomeLive 2.0 sketch. It creates the facade, calls `setup()`, and activates one
`Scene` through the facade-owned `setScene(...)` lifecycle.

## What it demonstrates

- `P3D` with `pixelDensity(1)`;
- scene registration through `ziviDomeLive`, not a detached manager;
- an empty Processing `draw()` because the library owns its registered render hook;
- draw-only `sceneRender(PGraphicsOpenGL)` and direct key/mouse callbacks.

Use this folder as the starting point for a new sketch. Add mutable state in `update()` and keep
graphics calls in `sceneRender(...)`. Do not call `beginDraw()`, `endDraw()`, or `ziviDome.draw()`.
