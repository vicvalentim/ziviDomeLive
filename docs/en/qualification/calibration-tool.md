---
title: CalibrationTool
icon: material/axis-arrow
status: qualification
---
# CalibrationTool

`CalibrationTool` is the **current visual qualification instrument** for ziviDomeLive. It is not a learning example and it is not a substitute for the historical 1.5 protocol. Use it to inspect spherical orientation, projection mapping, focus, color, calibration controls and output behavior on the actual target system.

<div class="zd-image-placeholder" markdown>
**IMAGE PLACEHOLDER — CalibrationTool current release**  
Final capture: compose Scene 1 and Scene 2 from the installed `CalibrationTool`, with the active ViewType/calibration state visible.  
Suggested final asset: `docs/img/calibration-tool-overview.png`
</div>

## Scene 1 — Cube Focus and Color

Six GLSL 4.10 targets form a closed cube around the observer. Each face uses explicit local `0..1` coordinates so grids, geometric references, focus features, color ramps and annotations form one continuous spherical calibration surface.

The scene contains:

- 24 × 24 grid with quarter divisions and face boundaries;
- safe-area rectangles, concentric circles, radial spokes and center crosshairs;
- 1, 2, 4 and 8 pixel line pairs;
- 1, 2, 3 and 4 pixel points, starbursts and a deterministic star field;
- RGB/CMY/white/black references;
- continuous grayscale, discrete levels and near-black/near-white clipping patches;
- face index, axis, direction, grid coordinates, `UP` and `R` orientation markers.

Pixel-sized features are exact only when a cubemap face is sampled one to one. Their degradation through another projection, resolution, codec or receiver is part of what the tool is intended to reveal.

## Scene 2 — Paul Bourke 360 Degree Sphere

The second scene maps one of four original, unmodified Paul Bourke v14 equirectangular test patterns to the inside of a complete sphere centered at `(0, 0, 0)`. The sphere has diameter 1800 units, north pole `+Z`, south pole `-Z`, and an equator on `Z=0`.

The source follows the active output-resolution bucket when an external output is enabled. With outputs disabled, the nearest bucket is selected from the Processing window.

| Render bucket | Source used by the example |
|---|---|
| 1024 (1k) | `spherical2400.png` (2400 × 1200) |
| 2048 (2k) | `spherical4096.png` (4096 × 2048) |
| 3072 (3k) | `spherical4800.png` (4800 × 2400) |
| 4096 (4k) | `spherical8192.png` (8192 × 4096) |

Scene 2 also provides a slow, time-quantized rotation for observing aliasing and playback discontinuity:

- `Space`: toggle one revolution per 60 seconds;
- `T`: switch between 30 fps / 1800 positions and 60 fps / 3600 positions;
- `,` / `.`: step backward / forward by one degree and pause;
- `C`: restore source orientation and pause.

The rotation profile does not change Processing's global frame rate.

## Shared controls

Use Left/Right arrows to switch scenes.

| Control | Effect |
|---|---|
| `1` | Domemaster ViewType |
| `2` | Equirectangular ViewType |
| `3` | Skybox ViewType |
| `4` | Standard ViewType |
| `[` / `]` | Decrease / increase Domemaster Size% by 10 |
| `-` / `+` | Decrease / increase FOV by 10° |
| `P` | Add 90° pitch |
| `Y` | Add 90° yaw |
| `R` | Add 90° roll |
| `F` | Toggle floating Domemaster preview |
| `0` | Restore canonical projection state |

The example starts in `RenderMode.FULL`. View selection therefore exercises the same independent Preview/Output routing model documented for normal projects.

## Recommended qualification sequence

1. Start from the canonical reset state (`0`).
2. Inspect Scene 1 in Domemaster, Equirectangular, Skybox and Standard.
3. Apply `P`, `Y` and `R` independently and verify continuous orientation across the spherical projections.
4. In Domemaster, vary FOV and Size% and confirm that they solve angular coverage and physical image fitting rather than Scene camera movement.
5. Toggle the floating preview and confirm it does not redefine external-output resolution.
6. Switch to Scene 2 and inspect poles, equator, longitude continuity and the slow rotation profile.
7. Repeat on every GPU/platform/output configuration that will be claimed as tested for the release.
8. Record screenshots and receiver evidence from the **actual installed package or qualified checkout**.

!!! warning "Visual inspection is hardware evidence"
    Source compatibility is not proof that a GPU, driver, projector, lens or receiver chain is qualified. Record the exact environment used for every release claim.

## Current protocol vs. historical protocol

This page is the current CalibrationTool protocol. The preserved [1.5 Calibration Tool and Compatibility Baseline](1.5-calibration-tool.md) documents the historical 1.5 qualification state and should remain unchanged except for factual errata explicitly identified as historical corrections.

<div class="zd-actions" markdown>
[Spherical Calibration](../usage/spherical-calibration.md){ .md-button .md-button--primary }
[Release Readiness](2.0-release-readiness.md){ .md-button }
</div>
