# CalibrationTool

`CalibrationTool` is a two-scene instrument for repeatable alignment, focus, color, projection, and output checks. Use Left/Right arrows to switch scenes.

## Scene 1: Cube Focus and Color

Six GLSL 4.10 targets form a closed cube around the observer. Every quad has an explicit local `0..1` mapping. A transparent 1024-pixel annotation layer is composited into the same fragment pass, while resolution-sensitive lines, points, grids, and gradients remain procedural at the active cubemap-face resolution. Pitch, yaw, and roll are applied afterward by the spherical capture matrix, so the complete mapped cube follows continuous controls and 90-degree steps without screen-space drift.

- 24 x 24 grid with quarter divisions and a two-pixel face boundary
- Safe-area rectangles, concentric circles, radial spokes, and center crosshairs
- Vertical and horizontal 1, 2, 4, and 8 pixel line pairs
- Exact 1, 2, 3, and 4 pixel points, starbursts, and a deterministic star field
- Continuous black-primary-white RGB and CMY ramps
- Pure RGB/CMY/white/black bars
- Continuous grayscale, nine discrete levels, and near-black/near-white clipping patches
- Face index, axis, direction, grid coordinates, `UP`, and `R` orientation markers

Pixel-sized features are exact when a cubemap face is sampled one to one. Their degradation through another projection, resolution, codec, or receiver is the behavior being measured.

## Scene 2: Paul Bourke 360 Degree Sphere

One of four original, unmodified Paul Bourke v14 equirectangular test patterns is mapped onto the inside of a complete sphere centered at `(0, 0, 0)`. Its diameter is 1800 units, matching the 900-unit cube-face distance in Scene 1. The north pole is `(0, 0, 900)`, the south pole is `(0, 0, -900)`, and the equator lies on `Z=0`, giving 360 degrees of longitude and 180 degrees of latitude. The 2-degree mesh follows the source grid exactly. Point sampling and disabled mipmaps avoid adding an extra filtering stage.

When an external output is enabled, the source follows its 1024, 2048, 3072, or 4096 resolution bucket. With outputs disabled, the nearest bucket is selected from the window's smaller dimension. Only the selected PNG is held in memory, and a runtime resolution change reloads the matching source automatically.

| Render bucket | Original source |
|---|---|
| 1024 (1k) | `data/img/spherical2400.png` (2400 x 1200) |
| 2048 (2k) | `data/img/spherical4096.png` (4096 x 2048) |
| 3072 (3k) | `data/img/spherical4800.png` (4800 x 2400) |
| 4096 (4k) | `data/img/spherical8192.png` (8192 x 4096) |

- `Space`: toggle one revolution per 60 seconds
- `T`: switch the playback target between 30 fps / 1800 frames and 60 fps / 3600 frames per revolution
- `,` / `.`: rotate backward / forward by one degree and pause
- `C`: restore the source orientation and pause

Paul Bourke recommends a slow 360-degree rotation to reveal aliasing and playback discontinuity. Rotation advances by one deterministic frame step around the `+Z`/`-Z` polar axis: 1800 steps at 30 fps or 3600 at 60 fps. It is useful for live display diagnosis, but, as the author notes, it is not equivalent to testing a movie encoded with rotating source frames. The original images use gamma 1.0 and a linear color profile.

See [THIRD_PARTY.md](THIRD_PARTY.md) for authorship, source, integrity hash, and redistribution conditions.

## Shared Controls

- Left/Right arrows: switch scenes
- `1`: fisheye domemaster
- `2`: equirectangular
- `3`: cubemap layout
- `4`: Standard
- `[` / `]`: decrease / increase domemaster Size% by 10
- `-` / `+`: decrease / increase FOV by 10 degrees
- `P`: add 90 degrees of pitch
- `Y`: add 90 degrees of yaw
- `R`: add 90 degrees of roll
- `F`: toggle the floating domemaster preview
- `0`: restore the canonical projection state; in Scene 2 it also resets texture rotation

Each accepted projection control prints the active scene and calibration state to the Processing console.
