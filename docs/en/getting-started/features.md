# Key Features

## Explicit Render Modes

`RenderMode` selects the global behavior:

- `FULL`: independent preview and per-output `ViewType` routes
- `STANDARD`: perspective Standard representation
- `DOMEMASTER`: fisheye domemaster
- `EQUIRECTANGULAR`: 2:1 spherical projection
- `SKYBOX`: cubemap layout

Dedicated modes override effective routes without erasing their saved `ViewType` selections. Returning to `FULL` restores independent routing.

## Independent Rendering Domains

Standard scenes render directly through `StandardRenderer`. Spherical views share cubemap capture and projection passes. A centralized policy computes only the views required by the main preview, floating domemaster, and enabled outputs.

## Domemaster Calibration

FOV and Size% are operational calibration parameters, not visual decoration:

| Parameter | Range | Default |
|---|---:|---:|
| FOV | `0..360` degrees | `210` |
| Size | `0..100` percent | `100` |

Pitch, yaw, and roll affect every spherical mode from the same orientation.

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

The ControlP5 panel groups global status, spherical parameters, preview selection, and output controls. Output toggles own publication changes, while each enabled output exposes its own view selector.
