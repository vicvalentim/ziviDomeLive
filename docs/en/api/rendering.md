---
title: "Rendering API"
icon: material/api
status: stable
---
# Rendering API

## RenderMode

`RenderMode` answers: **How do I want the runtime to work now?**

- `FULL`
- `STANDARD`
- `DOMEMASTER`
- `EQUIRECTANGULAR`
- `SKYBOX`

`FULL` is the multi-route working mode. Dedicated modes temporarily force the effective representation and do not erase the stored preview/output selections used by `FULL`.

## ViewType

`ViewType` answers: **Which final representation should this destination receive?**

- `STANDARD`
- `DOMEMASTER`
- `EQUIRECTANGULAR`
- `SKYBOX`

Do not document `ViewType` as another runtime mode: it is a routing representation.

## Resolution and calibration

The established facade uses `resetGraphics(int)` to request output-target recreation. Documentation and examples must keep this implemented API name.

Spherical calibration uses the public controls for Pitch/Yaw/Roll, while Domemaster additionally uses FOV and Size%. Camera motion and spherical orientation are different operations.

## Direct renderer access

Renderer implementation classes are public for compatibility/advanced use but are not the recommended starting point for ordinary sketches. Target ownership and invalidation rules matter when using them directly; consult their Javadocs and the Developer Guide.
