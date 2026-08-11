# CompatibilityLock

This example is a static alignment and color-calibration chart for repeatable GPU, projection, and output checks. It renders the same target on all six cubemap directions without animation or lighting.

## Test Target

- Face index, axis, and direction labels for `+X`, `-X`, `+Y`, `-Y`, `+Z`, and `-Z`
- 12 x 12 grid, two safe-area frames, concentric circles, and a center crosshair
- `UP`, `R`, and corner labels to reveal rotation or mirroring
- GLSL 4.10 RGB, CMY, white, and black reference bars
- Nine-step grayscale ramp from 0 to 255
- A unique positive/negative accent for each axis

The color and grayscale fields come from `data/calibration-colors.frag`. If GLSL compilation or application fails, the console reports it once and the scene draws the same fields with Processing primitives. Lighting is disabled in both paths.

## Controls

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
- `0`: restore FOV 210, Size% 100, zero rotation, fisheye view, and floating preview

Each accepted control prints the current calibration state to the Processing console. A useful capture record includes Processing version, OS, CPU architecture, GPU, driver, resolution, output receiver, and observed OpenGL errors. Reference images must be captured from a qualified target stack; the sketch does not invent a golden image.
