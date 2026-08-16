# ziviDomeLive

**Create real-time fulldome, spherical and immersive visuals in Processing.**

ziviDomeLive lets one Processing scene be presented as Standard perspective, Domemaster, Equirectangular or Skybox output. You can work interactively in the Processing window, calibrate spherical output for a dome, manage multiple scenes and optionally publish selected views through NDI, Syphon or Spout.

![Artist-first overview](../img/hero-overview.png)

## Start here

If this is your first project, follow the [Quickstart](getting-started/quickstart.md). A basic scene only needs the library instance, `setup()`, `update()` when state changes over time, and `sceneRender()` for drawing.

## Choose a representation

![Render modes overview](../img/render-modes-overview.png)

| Representation | Typical use |
|---|---|
| Standard | Processing-window perspective view and conventional visual output |
| Domemaster | Fisheye image for fulldome projection |
| Equirectangular | 2:1 spherical/360° workflows |
| Skybox | Cubemap layout and inspection workflows |

`RenderMode.FULL` keeps preview and enabled outputs independently routable through `ViewType`. Dedicated modes are temporary working modes and do not erase stored routes.

## Calibrate for the dome

Use Pitch/Yaw/Roll to orient the shared spherical domain, FOV to define the Domemaster angular field, and Size% to fit the circular Domemaster image to the projector/lens system. Size% is a physical output calibration control, not scene zoom.

## Learn from examples

Use the six learning examples in increasing complexity, then move to the qualification tools when you need installation validation or performance evidence.

## API and developer material

The [API Reference](api/overview.md) documents callable contracts. The Developer Guide explains the Standard/Spherical rendering architecture, OpenGL backend, lifecycle, threading and output internals. Those internals are not prerequisites for creative use.

### Under the hood

Version 2.0 uses a native cubemap as the shared source for spherical projections. This is an implementation detail: artist-facing contracts are the final representations, routing, orientation and calibration behavior.
