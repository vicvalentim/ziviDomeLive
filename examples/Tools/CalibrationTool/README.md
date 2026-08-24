# CalibrationTool

**Category:** Tools
**Required dependency:** ControlP5 2.2.6

`CalibrationTool` is a two-scene instrument for repeatable alignment, focus, color, projection, and output checks. It starts on **Paul Bourke Environment Background** in `DOMEMASTER` with FOV `210`, Size `100%`, and global pitch/yaw/roll at `0°`. A fixed `90°` X-axis alignment is applied directly to the Environment image lookup, without rotating scene geometry or the dome controls. Use Left/Right arrows to switch scenes.

## Cube Focus and Color

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

## Initial Scene: Paul Bourke Environment Background

One of four original, unmodified Paul Bourke v14 equirectangular test patterns is supplied to the activation-owned `SceneEnvironmentService`. The library composes it at far depth in Standard and spherical views, so camera target translation and orbit distance cannot introduce parallax. Its fixed X-axis alignment and longitude yaw are source-image transforms; shared dome pitch, yaw, roll, FOV, and Size% remain independent projection controls.

When an external output is enabled, the source follows its 1024, 2048, 3072, or 4096 resolution bucket. With outputs disabled, the nearest bucket is selected from the window's smaller dimension. Only the selected PNG is held in memory, and a runtime resolution change reloads the matching source automatically.

| Render bucket | Original source |
|---|---|
| 1024 (1k) | `data/img/spherical2400.png` (2400 x 1200) |
| 2048 (2k) | `data/img/spherical4096.png` (4096 x 2048) |
| 3072 (3k) | `data/img/spherical4800.png` (4800 x 2400) |
| 4096 (4k) | `data/img/spherical8192.png` (8192 x 4096) |

- `Space`: toggle one revolution per 60 seconds
- `T`: switch the rotation profile between 30 fps / 1800 frames and 60 fps / 3600 frames per revolution
- `,` / `.`: rotate backward / forward by one degree and pause
- `C`: restore the source orientation and pause
- `V`: toggle Environment visibility
- `D` / `B`: decrease / increase Environment intensity by 0.1

Paul Bourke recommends a slow 360-degree rotation to reveal aliasing and playback discontinuity. Environment longitude is quantized against elapsed time: 1800 source positions at 30 fps or 3600 at 60 fps, always completing in 60 seconds. The profile does not change Processing's global frame rate or restart its JOGL animator. It is useful for live display diagnosis, but, as the author notes, it is not equivalent to testing a movie encoded with rotating source frames. The original images use gamma 1.0 and a linear color profile.

See [THIRD_PARTY.md](THIRD_PARTY.md) for authorship, source, integrity hash, and redistribution conditions.

## Shared Controls

- Left/Right arrows: switch scenes
- `1`: fisheye domemaster
- `2`: equirectangular
- `3`: true equi-angular cubemap (EAC) cross
- `4`: Standard
- `[` / `]`: decrease / increase domemaster Size% by 10
- `-` / `+`: decrease / increase FOV by 10 degrees
- `P`: add 90 degrees of pitch
- `Y`: add 90 degrees of yaw
- `R`: add 90 degrees of roll
- `F`: toggle the floating domemaster preview
- `0`: restore the startup projection state; in the Paul Bourke scene it also resets Environment rotation, visibility, intensity, and playback profile

Each accepted projection control prints the active scene and calibration state to the Processing console.
