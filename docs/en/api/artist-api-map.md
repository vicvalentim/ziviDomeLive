---
title: Artist API Map
icon: material/api
status: stable
---

# Artist API Map

This map identifies the public surface intended for ordinary Processing sketches. Use it to decide **where to start**; use generated Javadocs for exact signatures, overloads and lifecycle notes.

<div class="grid cards" markdown>

- :material-application-braces-outline: **`ziviDomeLive`**

    Main runtime facade associated with the Processing sketch.

- :material-palette-outline: **`Scene`**

    Defines state update and drawing behavior through `update()` and `sceneRender()`.

- :material-view-dashboard-outline: **`RenderMode` + `ViewType`**

    Separate current working mode from per-destination representation.

- :material-export: **`OutputManager`**

    Routes final views to optional external outputs and exposes state/failure information.

</div>

## Runtime and scenes

| Type | Role |
|---|---|
| `ziviDomeLive` | Main runtime facade used by a Processing sketch. |
| `Scene` | Scene lifecycle, update and drawing contract. |
| `SceneManager` | Registration and scene switching. |

The normal application path is `ziviDomeLive` → `Scene`, with `SceneManager` used when a sketch contains more than one scene.

## Common facade controls

<div class="grid cards" markdown>

- **Scene** — `setScene(...)`, `registerScene(...)`, `setSceneManager(...)`, `getSceneManager()`
- **Render mode** — `setRenderMode(...)`, `getRenderMode()`, `setCurrentView(...)`
- **Spherical calibration** — `setFov(...)`, `setFishSize(...)`, `setPitch(...)`, `setYaw(...)`, `setRoll(...)`, `resetOrientation()`
- **Preview/output** — `setShowPreview(...)`, `setStandardOutputAspectMode(...)`, `getOutputManager()`, `resetGraphics(int)`, `getOutputResolution()`
- **Scene camera** — `getSceneCamera()`, `setSceneCameraInputEnabled(...)`
- **Lifecycle** — `pause()`, `resume()`, `dispose()`

</div>

!!! info "Java public ≠ artist-facing"
    `SceneServices`, timing/assets/task services, public renderer implementations, performance snapshots, `FrameViews` and low-level OpenGL bridge/target types are documented separately as Advanced, Experimental or Engine-facing API.

<div class="zd-actions" markdown>
[API Overview](overview.md){ .md-button }
[Generated Javadocs](javadocs.md){ .md-button .md-button--primary }
</div>
