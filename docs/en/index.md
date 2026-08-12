---
title: Home
hide:
  - navigation
  - toc
---

<section class="zd-hero" markdown>
<div markdown>
<span class="zd-kicker">Processing 4 · Fulldome · Native Cubemap</span>

# Real-time spherical rendering for artists who need control.

ziviDomeLive is a Processing 4 library for fulldome, monoscopic VR, immersive installations, and GPU-native spherical output. Version 2.0.0 keeps the familiar `Scene.sceneRender(PGraphicsOpenGL)` contract while consolidating native `GL_TEXTURE_CUBE_MAP` capture and direct `samplerCube` projection shaders.

<div class="zd-badges" markdown>
<span class="zd-badge">2.0.0</span>
<span class="zd-badge">P3D</span>
<span class="zd-badge">Domemaster</span>
<span class="zd-badge">Equirectangular</span>
<span class="zd-badge">Skybox</span>
</div>

<div class="zd-actions" markdown>
[Start with Quickstart](getting-started/quickstart.md){ .zd-button }
[See render modes](usage/basic-usage.md){ .zd-button .zd-button--secondary }
[Plan screenshots](usage/visual-capture-guide.md){ .zd-button .zd-button--secondary }
[Open Javadocs](api/javadocs.md){ .zd-button .zd-button--secondary }
</div>
</div>

<div class="zd-visual" markdown>
<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Hero capture placeholder**

Replace later with a manual capture of `CalibrationTool` in `SKYBOX` mode, preferably 16:9.
</div>
</div>
</div>
</section>

## What changed in 2.0

<div class="zd-grid" markdown>
<div class="zd-card" markdown>
### Native spherical capture

Spherical rendering now writes into one native cubemap target instead of keeping the old `PGraphicsOpenGL[]` face-array fallback alive.
</div>

<div class="zd-card" markdown>
### Direct projection shaders

Equirectangular, domemaster, and skybox projections sample the cubemap through `samplerCube`, reducing pipeline indirection and preserving face orientation.
</div>

<div class="zd-card" markdown>
### Stable Processing contract

Scenes still render through `Scene.sceneRender(PGraphicsOpenGL)`, so existing sketches can migrate without adopting a custom context object.
</div>
</div>

## Rendering at a glance

<div class="zd-pipeline" markdown>
<div class="zd-step" markdown>
**Scene**

<span>Processing drawing commands in the active `Scene`.</span>
</div>
<div class="zd-step" markdown>
**Native Cubemap**

<span>Six orientations captured into `GL_TEXTURE_CUBE_MAP`.</span>
</div>
<div class="zd-step" markdown>
**samplerCube**

<span>Projection shaders sample the cubemap directly.</span>
</div>
<div class="zd-step" markdown>
**Output**

<span>Preview, domemaster, equirectangular, skybox, NDI, Syphon, or Spout.</span>
</div>
</div>

## Manual capture slots

<div class="zd-gallery" markdown>
<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Standard preview**

Suggested file: `docs/assets/images/screenshots/standard-preview.png`
</div>
</div>

<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Domemaster**

Suggested file: `docs/assets/images/screenshots/domemaster-output.png`
</div>
</div>

<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Equirectangular**

Suggested file: `docs/assets/images/screenshots/equirectangular-output.png`
</div>
</div>

<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Skybox layout**

Suggested file: `docs/assets/images/screenshots/skybox-layout.png`
</div>
</div>
</div>

Use the [Visual Capture Guide](usage/visual-capture-guide.md) when you are ready to replace these placeholders with final screenshots.

## Start here

1. Review the [system requirements](installation/requirements.md) and [dependencies](installation/dependencies.md).
2. Install the packaged library using the [installation guide](installation/installation-steps.md).
3. Build a first scene with the [quickstart](getting-started/quickstart.md).
4. Choose between independent routing and dedicated rendering in [Render Modes](usage/basic-usage.md).
5. Learn the [control panel](usage/control-panel.md) and [spherical calibration](usage/spherical-calibration.md).
6. Review the [2.0.0 release notes](release-notes/2.0.0.md) before upgrading an existing sketch.

## Stable 2.0 contracts

- `Scene.sceneRender(PGraphicsOpenGL)` receives an open render target; the library owns `beginDraw()` and `endDraw()`.
- `RenderMode.FULL` is the default and preserves independent preview/output routes.
- Standard rendering is independent from spherical cubemap capture.
- Spherical capture writes into a native `GL_TEXTURE_CUBE_MAP`; equirectangular, domemaster, and skybox projections sample it directly.
- Spherical pitch, yaw, and roll compose shortest deltas into one normalized quaternion; their facade values remain control accumulators.
- Domemaster FOV is `0..360` with default `210`.
- Domemaster Size% is `0..100` with default `100`.
- Output-resolution presets are `1024`, `2048`, `3072`, and `4096`.
- External output publication is disabled by default.

## Qualification

The Java suite validates API, state, lifecycle, routing, math, metadata, and release contracts without requiring a GPU. Start with the [rendering architecture](architecture/rendering-pipeline.md), then use the [CalibrationTool protocol](qualification/1.5-calibration-tool.md) and [2.0 release-readiness checklist](qualification/2.0-release-readiness.md) on qualified hardware. No golden image is manufactured by the repository.

See the [known issues](known-issues.md) before deploying to production.
