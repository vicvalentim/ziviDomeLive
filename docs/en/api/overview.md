# API Overview

## Primary Types

| Type | Responsibility |
|---|---|
| `zividomelive` | Processing integration, lifecycle, rendering, calibration, and service access |
| `RenderMode` | Global rendering behavior |
| `Scene` | User drawing and event contract |
| `SceneManager` | Scene registration, active ownership, switching, and disposal |
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

`zividomelive.ViewType` remains available for preview and per-output routes. Its order is compatibility-sensitive and must not be changed.

| `ViewType` index | Value | Representation |
|---:|---|---|
| 0 | `FISHEYE_DOMEMASTER` | Circular fulldome projection |
| 1 | `EQUIRECTANGULAR` | 2:1 spherical projection |
| 2 | `CUBEMAP` | Six-face inspection layout |
| 3 | `STANDARD` | Perspective scene rendering |

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

- The lowercase public facade name `zividomelive` remains unchanged.
- `RenderMode.FULL` preserves the legacy routing model.
- `renderFisheyeDomemaster()`, `renderEquirectangular()`, `renderCubemap()`, and `renderStandard()` remain deprecated compatibility shims.
- Renderer getters remain public for 1.x compatibility, but renderer topology is not a permanent backend contract.

Generated Javadocs in the release package are the signature-level reference.
Use [Core Classes](core-classes.md) for ownership and state semantics,
[Operational Helpers](helper-functions.md) for runtime controls, and the
[Scene Interface](scene-interface.md) for the drawing contract.
