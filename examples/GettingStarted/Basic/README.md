# Basic

**Category:** Getting Started
**Required dependency:** ControlP5 2.2.6

A two-scene introduction to the final ziviDomeLive 2.0 API. The first scene is activated with
`setScene(...)`; the second is added with `registerScene(...)`, so configuration and setup remain
owned by the facade lifecycle.

## Controls

- Left/Right: switch registered scenes;
- `1`: `RenderMode.FULL`;
- `2`: `RenderMode.STANDARD`;
- `3`: `RenderMode.DOMEMASTER`;
- `4`: `RenderMode.EQUIRECTANGULAR`;
- `5`: `RenderMode.SKYBOX`;
- `+` / `-`: change animation speed;
- `R`: reset animation speed;
- mouse wheel in Scene 1: change the pillar radius.

Animation advances only in `Scene.update()`. Spherical capture may call `sceneRender(...)` six or
more times per Processing frame, so render callbacks do not mutate simulation state.
