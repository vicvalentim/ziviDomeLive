# Key Features

## Explicit Render Modes

`RenderMode` selects the global behavior:

- `FULL`: independent preview and per-output `ViewType` routes
- `STANDARD`: perspective Standard representation
- `DOMEMASTER`: fisheye domemaster
- `EQUIRECTANGULAR`: 2:1 spherical projection
- `SKYBOX`: true equi-angular cubemap (EAC) cross layout

Dedicated modes override effective routes without erasing their saved `ViewType` selections. Returning to `FULL` restores independent routing.

## Independent Rendering Domains

Standard scenes render directly through `StandardRenderer`. Spherical views share cubemap capture and projection passes. A centralized policy computes only the views required by the main preview, floating domemaster, and enabled outputs.

## Domemaster Calibration

FOV and Size% are operational calibration parameters, not visual decoration:

| Parameter | Range | Default |
|---|---:|---:|
| FOV | `0..360` degrees | `210` |
| Size | `0..100` percent | `100` |

Pitch, yaw, and roll affect every spherical mode from the same unit quaternion. Their `-PI..PI` panel sliders wrap continuously under mouse-wheel input; each change is composed as a local-axis quaternion delta instead of rebuilding the attitude from Euler angles.

## Scene Lifecycle

`SceneManager` owns the active scene. It runs `setupScene()` on activation, `update()` before each rendered frame, and `dispose()` when a scene leaves active ownership or the manager is cleared.

## Preview and Output Separation

The Standard preview follows the Processing window dimensions. Spherical preview targets use an automatic square size between 256 and 1024 pixels. External outputs use an independent global resolution selected from 1K, 2K, 3K, or 4K presets.

## External Outputs

- Syphon on macOS and Spout on Windows receive GPU-native `PGraphicsOpenGL` targets.
- NDI copies completed Processing pixels to a three-slot bounded pipeline and sends from a dedicated worker.
- Preview selection, publication state, backend lifecycle, and render requirements remain independent.
- State and failure diagnostics are available through `OutputManager`.

## Built-in Control Panel

The ControlP5 panel groups global status, spherical parameters, preview selection, and output controls. Its available controls follow the active rendering capability:

| Mode | Orientation | FOV / Size | Floating domemaster | View selectors |
|---|---|---|---|---|
| `FULL` | Shown | Shown | Shown | Preview and enabled outputs |
| `STANDARD` | When floating domemaster is enabled | When floating domemaster is enabled | Shown | Hidden |
| `DOMEMASTER` | Shown | Shown | Hidden | Hidden |
| `EQUIRECTANGULAR` / `SKYBOX` | Shown | Hidden | Hidden | Hidden |

Output resolution and publication toggles remain visible in every mode. Per-output view selectors appear only in `FULL`, where routes are independently configurable.

Continue with [Render Modes](../usage/basic-usage.md), the
[Control Panel](../usage/control-panel.md), and
[Spherical Calibration](../usage/spherical-calibration.md) for operational
details.
