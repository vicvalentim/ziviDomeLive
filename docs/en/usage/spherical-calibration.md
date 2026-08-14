# Spherical Calibration

The spherical controls transform the shared cubemap capture used by domemaster, equirectangular, and skybox views. They do not move the scene-space `OrbitCamera` and do not alter the independent Standard perspective camera.

## Parameters

| Parameter | Panel range | Default | Affects |
|---|---:|---:|---|
| Pitch | `-PI..PI` cyclic | `0` | Every spherical representation |
| Yaw | `-PI..PI` cyclic | `0` | Every spherical representation |
| Roll | `-PI..PI` cyclic | `0` | Every spherical representation |
| FOV | `0..360` degrees | `210` | Fisheye domemaster |
| Size% | `0..100` percent | `100` | Fisheye domemaster scaling |

Programmatic callers should stay inside the supported FOV and Size% domain. `FisheyeDomemaster` constrains Size% internally, while the facade retains the value supplied by the caller for compatibility.

## Quaternion Orientation

The 1.5 orientation source is one normalized quaternion:

- Pitch applies a shortest angular delta around local `X`.
- Yaw applies a shortest angular delta around local `Z`.
- Roll applies a shortest angular delta around local `Y`.
- Deltas are composed in the order setter or panel events arrive.
- Non-finite angle values are ignored.

The three getters preserve their corresponding control accumulators. They are not an Euler decomposition of the final attitude. Consequently, two command sequences with the same final displayed triple may describe different attitudes when their event order differs. This is what removes the Euler singularity and keeps Yaw and Roll independent at a 90-degree Pitch.

For deterministic calibration, start from identity and apply a known sequence:

```java
dome.resetOrientation();
dome.setPitch(HALF_PI);
dome.setYaw(0);
dome.setRoll(0);
```

The old `CubemapRenderer.captureCubemap(pitch, yaw, roll, ...)` overload remains available and now maintains its own incremental quaternion state. New application code should use the facade rather than owning renderers directly.

## FOV

FOV controls the angular extent of the fisheye shader. The established default is `210` degrees. Representative qualification values should include the target lens setting, `180`, `210`, and boundary behavior where relevant.

FOV does not change equirectangular or skybox geometry, so the built-in panel hides it in those dedicated modes.

## Size%

Size% scales the finished domemaster around the center of its square target. It is intended for projector/lens alignment and image-circle matching, not for scene zoom. Output-target recreation restores the configured value.

## Resolution

Resolution is independent from FOV and Size%:

- Standard preview follows the window.
- Spherical preview uses the automatic `256..1024` square policy.
- External outputs use the selected `1024`, `2048`, `3072`, or `4096` target.

Higher output resolution increases cubemap and projection cost. Qualify focus and receiver behavior at the exact production bucket.

## Reset Operations

```java
dome.resetOrientation(); // Pitch/Yaw/Roll quaternion and accumulators only.
dome.resetControls();    // Orientation plus FOV=210 and Size%=100.
```

`resetControls()` also synchronizes the built-in widgets and is available after managers reach `MANAGERS_READY`.

## Visual Qualification

Automated math tests verify normalization, event-order composition, cyclic boundary continuity, multi-turn accumulator compatibility, and axis independence at 90-degree Pitch. They do not verify GPU sampling, mirroring, seams, projector focus, or receiver color.

Use the [CalibrationTool protocol](../qualification/1.5-calibration-tool.md) for those checks and record evidence with the [2.0 release-readiness checklist](../qualification/2.0-release-readiness.md).
