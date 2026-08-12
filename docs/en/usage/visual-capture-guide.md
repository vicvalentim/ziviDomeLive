# Visual Capture Guide

This page is a staging area for the screenshots that will make the documentation feel concrete. The placeholders below are intentionally styled like final slots: capture the images manually, save them with the suggested names, then replace each placeholder block with a normal Markdown image.

!!! tip "Recommended capture rhythm"
    Capture the same sketch in `STANDARD`, `DOMEMASTER`, `EQUIRECTANGULAR`, and `SKYBOX` so readers can compare modes without wondering whether the scene changed.

## Capture checklist

<div class="zd-checklist" markdown>
<div class="zd-check" markdown>
**1. Use CalibrationTool**

<span>Start from a scene with visible face orientation, depth, and horizon cues.</span>
</div>
<div class="zd-check" markdown>
**2. Wait for splash**

<span>Let the splash screen finish before taking production screenshots.</span>
</div>
<div class="zd-check" markdown>
**3. Ignore GL 1282 noise**

<span>The known Processing teardown warning is not a screenshot blocker.</span>
</div>
<div class="zd-check" markdown>
**4. Keep naming stable**

<span>Use the filenames below so future docs edits stay mechanical.</span>
</div>
</div>

## Primary screenshots

<div class="zd-gallery" markdown>
<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Hero / SKYBOX**

Save as `docs/assets/images/screenshots/hero-skybox.png`. Use this on the home page.
</div>
</div>

<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Standard preview**

Save as `docs/assets/images/screenshots/standard-preview.png`.
</div>
</div>

<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Domemaster output**

Save as `docs/assets/images/screenshots/domemaster-output.png`.
</div>
</div>

<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Equirectangular output**

Save as `docs/assets/images/screenshots/equirectangular-output.png`.
</div>
</div>

<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Skybox face layout**

Save as `docs/assets/images/screenshots/skybox-layout.png`.
</div>
</div>

<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Control panel**

Save as `docs/assets/images/screenshots/control-panel.png`.
</div>
</div>
</div>

## Replacement pattern

When a capture exists, replace the placeholder with:

```markdown
![Domemaster output](../../assets/images/screenshots/domemaster-output.png){ .img-center }
```

Keep screenshots under `docs/assets/images/screenshots/` and prefer PNG for UI or calibration images where crisp text and hard edges matter.

## Suggested framing

| Capture | Suggested ratio | What it should prove |
|---|---:|---|
| Hero / SKYBOX | 16:9 | Face layout, horizon continuity, overall identity |
| Standard preview | 16:9 | Normal Processing view still works independently |
| Domemaster | 1:1 | Fisheye shape, FOV, Size%, dome framing |
| Equirectangular | 2:1 | Seam quality and horizontal continuity |
| Control panel | 16:9 or cropped | Mode routing, calibration controls, output state |

After replacing the slots, run:

```bash
.venv-docs/bin/mkdocs build --strict
```

The GitHub Pages workflow also attaches generated Javadocs to `/reference/`, so API links can point there once the site is deployed.
