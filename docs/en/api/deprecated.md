---
title: Removed 1.x API
icon: material/history
status: internal
tags:
  - Migration
  - History
---

# Removed 1.x API

The final 2.0 surface contains **no deprecated compatibility API**. This page preserves migration history without presenting removed symbols as callable.

| 1.x concept | 2.0 migration |
|---|---|
| lowercase facade `zividomelive` | Rename declarations/construction to `ziviDomeLive`; package spelling is unchanged |
| nested/legacy view values | Import top-level `ViewType`; use `STANDARD`, `DOMEMASTER`, `EQUIRECTANGULAR`, `SKYBOX` |
| direct `renderStandard`, `renderFisheyeDomemaster`, `renderEquirectangular`, `renderCubemap` | Select `RenderMode` and destination `ViewType`; rendering runs from registered hooks |
| renderer getters/setters and direct renderer classes | Use facade configuration; concrete render graph is internal |
| generic/string output toggles and `setView` | Use typed `OutputType`, `setOutputEnabled`, `setViewForOutput` |
| public `sendOutput`/frame containers | Use `OutputManager` consumer controls; publication is internal |
| public `ThreadManager`/executor access | Use `SceneServices.tasks().submitIfIdle(...)` |
| Scene `controlEvent(ControlEvent)` | Use facade-owned panel, `SceneActionMap`, or raw Processing key/mouse callbacks |
| scene/runtime cleanup hooks and service `close()` | Release scene-owned resources in `dispose()`; runtime closes activation services |
| raw render queue | Return CPU results through task callbacks at the activation frame boundary |
| public GL/cubemap adapters and targets | No replacement; these are implementation details |

## Migration discipline

1. Make state mutation explicit in `update()`.
2. Keep `sceneRender()` draw-only and remove `beginDraw()`/`endDraw()`.
3. Register scenes through the facade.
4. Move background work to bounded activation tasks.
5. Replace backend strings and generic view routing with typed output methods.
6. Delete dependencies on renderers, `FrameViews`, GL adapters and cubemap targets.

Historical 1.4/1.5 API snapshots under `docs/qualification/` remain evidence of what existed; they are not 2.0 reference material.
