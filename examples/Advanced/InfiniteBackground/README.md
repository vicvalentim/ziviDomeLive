# InfiniteBackground

**Category:** Advanced
**Required dependency:** ControlP5 2.2.6

Shows the activation-owned `SceneEnvironmentService` and verifies environment infinity: camera
rotation and spherical orientation affect the panorama, while scene translation and orbit distance
do not introduce parallax.

## Controls

- `E`: switch between the generated calibration map and the optional panorama;
- `V`: toggle environment visibility;
- `[` / `]`: change environment yaw;
- `-` / `+`: change environment intensity;
- `P`, `Y`, `R`: add 90 degrees of spherical pitch, yaw, or roll;
- `C`: reset spherical orientation.

The generated calibration source always works. If the optional SolarSystem panorama is absent, the
example remains on the calibration source and reports that condition without failing.
