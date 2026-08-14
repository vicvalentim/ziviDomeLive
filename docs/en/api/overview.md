# API Overview

## Primary Types

| Type | Responsibility |
|---|---|
| `ziviDomeLive` | Processing integration, lifecycle, rendering, calibration, and service access |
| `RenderMode` | Global rendering behavior |
| `ViewType` | Preview and independent output route selection in `FULL` mode |
| `FrameViews` | Completed final targets exposed by logical view without renderer coupling |
| `Scene` | User drawing and event contract |
| `SceneManager` | Scene registration, active ownership, switching, and disposal |
| `SceneServices` | Activation-scoped time, tasks, assets, actions, camera, Environment, and cleanup |
| `FrameClock` / `SimulationTimeline` | Clamped frame time and bounded fixed-step simulation |
| `OutputManager` | NDI, Syphon, and Spout routing and lifecycle |
| `OrbitCamera` | Optional scene-space camera shared by all rendered targets |
| `SphericalOrientation` | Cyclic pitch/yaw/roll accumulation on a unit quaternion |

## Public Enums

```java
RenderMode.FULL
RenderMode.STANDARD
RenderMode.DOMEMASTER
RenderMode.EQUIRECTANGULAR
RenderMode.SKYBOX
```

`ViewType` is a top-level public enum for preview and per-output routes. Its 2.0 order is part of the public contract.

| `ViewType` index | Value | Representation |
|---:|---|---|
| 0 | `STANDARD` | Perspective scene rendering |
| 1 | `DOMEMASTER` | Circular fulldome projection |
| 2 | `EQUIRECTANGULAR` | 2:1 spherical projection |
| 3 | `SKYBOX` | Six-face inspection layout |

`StandardOutputAspectMode` selects `AUTO`, `ASPECT_16_9`, `ASPECT_16_10`,
`ASPECT_4_3`, or `ASPECT_1_1` for the high-resolution Standard output. It does
not resize the Processing preview window.

`OutputManager.OutputState` distinguishes:

- `UNAVAILABLE`: unsupported or last initialization failed
- `AVAILABLE`: eligible for initialization, no native resources
- `INITIALIZED`: native resources exist, publication disabled
- `ENABLED`: publication enabled
- `STOPPING`: NDI publication stopped while bounded cleanup completes

## Compatibility Notes

- The public facade is `ziviDomeLive`; the lowercase 1.x class is not retained in 2.0.
- `ViewType` is top-level in 2.0; the nested 1.x enum and its old constant names are not retained.
- `RenderMode.FULL` preserves the compatibility routing model.
- `renderFisheyeDomemaster()`, `renderEquirectangular()`, `renderCubemap()`, and `renderStandard()` remain deprecated compatibility shims.
- Renderer getters remain public for compatibility, but renderer topology is not a permanent backend contract.

Generated Javadocs in the release package and on GitHub Pages are the signature-level reference.
Use [Generated Javadocs](javadocs.md) for direct API signatures,
[Core Classes](core-classes.md) for ownership and state semantics,
[Operational Helpers](helper-functions.md) for runtime controls, and the
[Scene Interface](scene-interface.md) for the drawing contract. The
[Scene Services](scene-services.md) guide covers reusable application infrastructure.
