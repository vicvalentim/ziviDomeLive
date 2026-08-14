# Control Panel

The built-in ControlP5 panel is an operational surface for preview, calibration, and output publication. Press `h` to show or hide it. The panel is created after renderer initialization and is hidden while the startup splash is active.

## Control Groups

| Group | Controls | Scope |
|---|---|---|
| Global | FPS label | Runtime status |
| Spherical | Pitch, Yaw, Roll, FOV, Size%, Reset | Shared spherical orientation and domemaster calibration |
| View | Floating domemaster, View Mode | Processing-window preview |
| Outputs | Resolution, NDI, Syphon/Spout, per-output View | Offscreen output targets and publication |

Syphon controls exist only on macOS. Spout controls exist only on Windows. NDI is shown on all platforms, but native availability is reported separately by `OutputState`.

## Visibility by RenderMode

The panel exposes only controls that can affect the active rendering capability:

| Mode | Pitch / Yaw / Roll | FOV / Size | Floating domemaster | Main View Mode | Output View selectors |
|---|---|---|---|---|---|
| `FULL` | Shown | Shown | Shown | Shown | Shown for enabled outputs |
| `STANDARD` | Shown only when floating domemaster is enabled | Shown only when floating domemaster is enabled | Shown | Hidden | Hidden |
| `DOMEMASTER` | Shown | Shown | Hidden | Hidden | Hidden |
| `EQUIRECTANGULAR` | Shown | Hidden | Hidden | Hidden | Hidden |
| `SKYBOX` | Shown | Hidden | Hidden | Hidden | Hidden |

Output resolution and publication toggles remain visible in every mode. A hidden selector does not erase its configured value. Returning to `FULL` restores independent preview and output routing.

## Cyclic Orientation Sliders

Pitch, Yaw, and Roll sliders use the ControlP5 flexible-handle style and a `-PI..PI` display range. Mouse-wheel movement wraps at either boundary:

```text
... 3.10, 3.14, -3.10, -3.04 ...
```

The wrap does not jump the rendered attitude. The library computes the shortest delta and composes it into the shared unit quaternion. FOV and Size% remain bounded sliders.

Each orientation row also includes an editable number box. Numeric entry and direct facade calls retain the value supplied by the caller; the final attitude is not converted back to Euler angles for display.

## View and Output Controls

- **Preview Domemaster** enables the floating fisheye thumbnail.
- **View Mode** changes the configured main preview route in `FULL`.
- **Output Resolution** schedules a deferred output-target reallocation.
- **Enable NDI/Syphon/Spout** changes publication state.
- **NDI/Syphon/Spout View** changes only that output route in `FULL`.

Publication toggles own backend state changes. Scene `controlEvent()` receives the resulting ControlP5 event once; it must not toggle the same backend again unless the scene intentionally wants a second state transition.

## Keyboard Shortcuts

| Key | Action |
|---|---|
| `h` | Show/hide the panel |
| `m` | Cycle the configured preview `ViewType` |
| Left / Right | Previous/next scene |

The `m` shortcut updates the stored preview route even while a dedicated mode forces another effective view. The stored selection becomes visible again in `FULL`.

## Programmatic Control

Facade setters affect rendering immediately. The panel updates its own paired slider/number-box values for panel-originated changes and for `resetControls()`. For an application-managed UI, treat the facade as the authoritative state and query its getters rather than reading ControlP5 widgets.

Use:

```java
dome.resetOrientation(); // Quaternion attitude only.
dome.resetControls();    // Orientation, FOV, Size%, and panel values.
```

`resetControls()` requires the control manager to be initialized. In ordinary sketches this happens automatically through the registered `post()` hook after `setup()`.

See [Spherical Calibration](spherical-calibration.md) for axis and parameter semantics.
