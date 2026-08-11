# CompatibilityLock

Use this static asymmetric scene to qualify GPU behavior against an independently captured 1.4 reference. The sketch does not generate or imply a golden image.

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

Each accepted control prints the current calibration state to the Processing console. Record the Processing version, OS, CPU architecture, GPU, driver, resolution, output receiver, and any OpenGL errors with every capture set.
