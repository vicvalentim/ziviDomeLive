# ziviDomeLive 1.5.0

![ziviDomeLive splash](../assets/images/splash.jpg){ width="520" }

ziviDomeLive is a Processing 4 library for real-time fulldome, monoscopic VR, and immersive installation graphics. It combines scene lifecycle management, independent Standard and spherical rendering, domemaster calibration, and optional NDI, Syphon, or Spout output routing.

Version 1.5.0 consolidates the mature 1.x architecture. It preserves the public `zividomelive` facade and legacy `ViewType` order while adding `RenderMode`, centralized render requirements, predictable lifecycle ownership, quaternion spherical controls, and observable output states.

## Start Here

1. Review the [system requirements](installation/requirements.md) and [dependencies](installation/dependencies.md).
2. Install the packaged library using the [installation guide](installation/installation-steps.md).
3. Build a first scene with the [quickstart](getting-started/quickstart.md).
4. Choose between independent routing and dedicated rendering in [Render Modes](usage/basic-usage.md).
5. Learn the [control panel](usage/control-panel.md) and [spherical calibration](usage/spherical-calibration.md).
6. Review the [1.5.0 release notes](release-notes/1.5.0.md) before upgrading an existing sketch.

## Stable 1.5 Contracts

- `Scene.sceneRender(PGraphicsOpenGL)` receives an open render target; the library owns `beginDraw()` and `endDraw()`.
- `RenderMode.FULL` is the default and preserves independent preview/output routes.
- Standard rendering is independent from spherical cubemap capture.
- Spherical pitch, yaw, and roll compose shortest deltas into one normalized quaternion; their facade values remain control accumulators.
- Domemaster FOV is `0..360` with default `210`.
- Domemaster Size% is `0..100` with default `100`.
- Output-resolution presets are `1024`, `2048`, `3072`, and `4096`.
- External output publication is disabled by default.

## Rendering Domains

```text
STANDARD
Scene -> StandardRenderer -> Standard target

SPHERICAL
Scene -> native GL_TEXTURE_CUBE_MAP -> equirectangular
                                    \-> domemaster
                                    \-> cubemap layout
```

The spherical topology above is an internal implementation detail. The stable behavior is the rendered spherical output, not the exact allocation strategy behind it.

## Qualification

The Java suite validates API, state, lifecycle, routing, math, metadata, and release contracts without requiring a GPU. Start with the [rendering architecture](architecture/rendering-pipeline.md), then use the [CalibrationTool protocol](qualification/1.5-calibration-tool.md) and [release-readiness checklist](qualification/1.5-release-readiness.md) on qualified hardware. No golden image is manufactured by the repository.

See the [known issues](known-issues.md) before deploying to production.
