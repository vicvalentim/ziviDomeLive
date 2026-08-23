---
title: Core Classes
icon: material/cube-outline
status: stable
tags:
  - API
---

# Core Classes

## ziviDomeLive

Create the facade with the active `PApplet`, call `setup()` once and register scenes through it. The constructor registers Processing hooks; renderer creation remains lazy until a valid OpenGL surface exists.

```java
ziviDomeLive dome = new ziviDomeLive(this);
dome.setup();
dome.setScene(new MainScene());
```

| Concern | Public controls |
|---|---|
| Scenes | `setScene`, `setCurrentScene`, `registerScene`, `setSceneManager`, `getSceneManager` |
| Representation | `setRenderMode`, `getRenderMode`, `setCurrentView`, `getCurrentView` |
| Calibration | `setFov`, `setFishSize`, `setPitch`, `setYaw`, `setRoll`, `resetOrientation`, `resetControls` |
| Preview | `setShowPreview`, `setStandardOutputAspectMode` |
| Resolution/output | `resetGraphics`, `getOutputResolution`, `getOutputManager` |
| Camera | `getSceneCamera`, `setSceneCameraInputEnabled` |
| Environment | `setEquirectangularBackground`, visibility/intensity/yaw controls, `clearEnvironmentBackground` |
| Logging | `setLogMode`, `enableDebugLogging`, `enableReleaseLogging` |
| Experimental profiling | performance enable/disable/snapshot/capability controls |

`isInitialized()` is the artist-facing readiness query. Initialization internals and renderer getters are not public 2.0 API.

## Scene

`Scene` protects the Processing programming model. Only `sceneRender(PGraphicsOpenGL)` is abstract; every lifecycle/input method is a default. See the [complete Scene contract](scene-interface.md).

## SceneManager

`SceneManager` is the active-scene authority. Registration and activation use **object identity**, not `equals()`.

| Method | Contract |
|---|---|
| `registerScene(scene)` | Register one unique instance; first registration activates it |
| `activateScene(scene)` | Activate a registered instance |
| `nextScene()` / `previousScene()` | Wrap through registration order |
| `setCurrentSceneIndex(index)` | Select a valid zero-based index |
| `reloadCurrentScene()` | Full disposal/reactivation with fresh services |
| `clearScenes()` | Dispose active activation and remove all registrations |

Prefer facade `setScene`/`registerScene`. A replacement manager is attached to facade lifecycle before first setup so `configure()` always precedes `setupScene()`.

## RenderMode and ViewType

`RenderMode` is runtime policy; `ViewType` is a final representation. `FULL` preserves independent routes. The exact enum orders are frozen and verified.

## LogMode

`DEBUG` permits diagnostic console/file logging; `RELEASE` suppresses debug chatter. Example sketches ship without enabling debug logging so ordinary console output stays quiet.

## Stable does not expose the render graph

No public `CubemapRenderer`, `CubemapTarget`, `FrameViews`, `ProcessingGlAdapter`, ControlP5 manager or worker/executor exists in 2.0. This removal is intentional and prevents callers from assuming graphics-context and ownership responsibilities.
