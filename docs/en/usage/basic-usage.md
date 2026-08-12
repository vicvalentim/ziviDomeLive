# Render Modes

`RenderMode` controls the effective representation used by the Processing window and every enabled external output. It does not select an output backend and it does not replace the `ViewType` routing API.

## FULL Compatibility Mode

`FULL` is the default. Existing sketches that never call `setRenderMode()` keep independent preview and output routes:

```java
dome.setRenderMode(RenderMode.FULL);
dome.setCurrentView(ViewType.STANDARD);

OutputManager outputs = dome.getOutputManager();
outputs.setNdiView(ViewType.EQUIRECTANGULAR);
outputs.setSyphonView(ViewType.DOMEMASTER);
outputs.setSpoutView(ViewType.SKYBOX);
```

Only enabled outputs request frames. Merely configuring a route or preparing Syphon/Spout does not activate publication or add a render requirement.

## Dedicated Modes

Dedicated modes force one effective representation for the main preview and all enabled outputs:

| `RenderMode` | Effective `ViewType` | Main pipeline |
|---|---|---|
| `STANDARD` | `STANDARD` | Direct perspective Standard renderer |
| `DOMEMASTER` | `DOMEMASTER` | Cubemap, fisheye samplerCube |
| `EQUIRECTANGULAR` | `EQUIRECTANGULAR` | Cubemap, equirectangular |
| `SKYBOX` | `SKYBOX` | Cubemap, skybox layout |

```java
dome.setRenderMode(RenderMode.DOMEMASTER);
```

The configured preview and per-output `ViewType` values are retained while a dedicated mode is active. Returning to `FULL` restores those independent routes:

```java
dome.setRenderMode(RenderMode.FULL);
```

## Floating Domemaster

The floating fisheye thumbnail is an auxiliary preview service:

```java
dome.setRenderMode(RenderMode.STANDARD);
dome.setShowPreview(true);
```

This combination intentionally renders the Standard path plus the spherical passes needed by the thumbnail. In other dedicated modes the service can still be enabled programmatically, but the built-in panel hides its redundant toggle.

## Render Requirements

The library computes a dependency closure for each frame:

```text
Standard                 -> Standard only
Cubemap layout           -> cubemap capture + layout
Equirectangular          -> cubemap capture + equirectangular
Fisheye domemaster       -> cubemap capture + fisheye samplerCube
```

When multiple enabled outputs request different views in `FULL`, their requirements are merged. At most one master cubemap is captured for the frame. See [Rendering Pipeline](../architecture/rendering-pipeline.md) for the complete frame order.

## Resolution Domains

Standard preview uses the current Processing window dimensions. Spherical preview targets use:

```text
min(1024, max(256, min(windowWidth, windowHeight)))
```

External output targets use the independent output resolution:

```java
dome.resetGraphics(2048);
```

The supported panel presets are `1024`, `2048`, `3072`, and `4096`. Reallocation is deferred to the draw loop, affects output targets only, and preserves domemaster Size%.

## Related Guides

- [Control Panel](control-panel.md)
- [Spherical Calibration](spherical-calibration.md)
- [External Integration](external-integration.md)
- [Runtime Lifecycle](../architecture/runtime-lifecycle.md)
