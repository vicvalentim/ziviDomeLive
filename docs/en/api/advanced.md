---
title: Advanced Stable API
icon: material/layers-triple-outline
status: advanced
tags:
  - API
---

# Advanced Stable API

Advanced Stable is supported public surface with stricter lifecycle or conceptual prerequisites. It is not an invitation to bypass the facade.

## Activation services

| Group | Types | Main guarantee |
|---|---|---|
| Time | `FrameClock`, `SimulationTimeline` | `double` time, bounded deltas and fixed-step catch-up telemetry |
| Work | `SceneTaskGroup` | Bounded keyed background work on a shared executor |
| Assets | `SceneAssets` | Render-thread Processing asset creation and activation cleanup |
| Input | `SceneActionMap` | Named bindings compatible with raw Scene callbacks |
| Camera | `SceneCameraService` | One scene-space orbit camera, target tracking and opt-in view light rig |
| Environment | `SceneEnvironmentService` | Activation-owned overrides with safe restoration |
| Integration | `ScenePorts`, `SceneInputPort`, `SceneOutputPort` | Bounded protocol-agnostic adapter SPI |

All are reached through `SceneServices`; none of the concrete service objects has a public constructor or scene-facing `close()`.

## Output control

`OutputManager` is a public interface returned by the facade. Its typed `OutputType` and `OutputState` surface controls intent and diagnostics, while producer operations remain internal. See [Outputs API](outputs.md).

## Math and navigation

- `Quaternion` is immutable and exposes normalized composition, spherical interpolation and matrix publication;
- `SphericalOrientation` keeps cyclic pitch/yaw/roll control values and one normalized attitude;
- `OrbitCamera` performs immediate direct manipulation and optionally smooth programmatic movement.

`SceneCameraService.applyWithViewLighting(...)` applies the orbit transform and a camera-positioned
spotlight aimed at the current target. It is explicit and replaces current fixed-function lights;
custom shader lighting remains scene-owned.

Scene-space camera motion and dome calibration are independent: changing orbit target/distance does not redefine pitch/yaw/roll or domemaster FOV.

## Compatibility promise

Advanced Stable types are included in the exact 2.0 API snapshot. Their lifecycle restrictions, enum members and absence of mutable public fields are tested. New engine implementation types will not be added to this level merely because a caller requests raw access.
