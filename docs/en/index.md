---
title: Home
hide:
  - navigation
  - toc
---

<section class="zd-hero" markdown>
<div markdown>
<span class="zd-kicker">Processing 4 · Fulldome · Native Cubemap</span>

# Processing 4 rendering for fulldome and spherical output.

ziviDomeLive extends Processing sketches with scene management, Standard preview rendering, native cubemap capture, domemaster projection, equirectangular output, skybox inspection, and optional external video routing. This manual documents installation, dependencies, examples, public API, release artifacts, and hardware qualification for version 2.0.0.

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
[Capture guide](usage/visual-capture-guide.md){ .zd-button .zd-button--secondary }
[Open Javadocs](api/javadocs.md){ .zd-button .zd-button--secondary }
</div>
</div>

<div class="zd-visual" markdown>
<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Primary screenshot placeholder**

Replace later with a manual capture of `CalibrationTool` in `SKYBOX` mode, preferably 16:9.
</div>
</div>
</div>
</section>

## What changed in 2.0

<div class="zd-grid" markdown>
<div class="zd-card" markdown>
### Native cubemap capture

Spherical rendering writes to a native `GL_TEXTURE_CUBE_MAP` target. Projection passes sample that target directly through `samplerCube` shaders.
</div>

<div class="zd-card" markdown>
### Processing-facing scene contract

User sketches implement `Scene.sceneRender(PGraphicsOpenGL)`. The library owns target setup, cubemap capture, projection passes, and output routing.
</div>

<div class="zd-card" markdown>
### Publication-ready artifacts

Release builds produce the Processing ZIP, PDEX installer, metadata TXT, examples, license files, and generated Javadocs under `reference/`.
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

Use the [Visual Capture Guide](usage/visual-capture-guide.md) to replace these placeholders with final screenshots.

## Start here

1. Review the [system requirements](installation/requirements.md), [dependencies](installation/dependencies.md), and [installation steps](installation/installation-steps.md).
2. Open a contributed example, then build a first scene with the [quickstart](getting-started/quickstart.md).
3. Choose a representation with [Render Modes](usage/basic-usage.md).
4. Calibrate spherical output with the [Control Panel](usage/control-panel.md) and [Spherical Calibration](usage/spherical-calibration.md) pages.
5. Use [Generated Javadocs](api/javadocs.md) for signatures and the [API Overview](api/overview.md) for ownership rules.
6. Before publishing or deploying, complete [Processing Publication](qualification/processing-publication.md) and [2.0 Release Readiness](qualification/2.0-release-readiness.md).

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
