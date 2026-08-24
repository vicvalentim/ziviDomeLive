# PortLoopback

**Category:** Getting Started
**Required dependency:** ControlP5 2.2.6

A transport-free demonstration of the `ScenePorts` SPI. A tiny input adapter publishes integer
messages into the activation's bounded queue; the scene receives them at a later Processing frame
boundary. A managed, non-blocking output adapter reports the applied value. The current message
level drives a 3D ring of eight signal towers, a central hub, and an animated data pulse. The scene
camera starts at a negative orbit distance, placing the hub at the Domemaster optical center
without modifying spherical Pitch/Yaw/Roll calibration.

## Controls

- `+` / `=`: enqueue a larger level;
- `-`: enqueue a smaller level.
- mouse drag: orbit the scene camera;
- mouse wheel or trackpad: zoom while remaining on the front hemisphere;
- `R`: restore the centered camera pose.

Replace `ManualIntegerInput` or `ConsoleOutput` with an optional MIDI, OSC, or device adapter while
keeping the scene message types unchanged. After connection, `ScenePorts` owns adapter closure, so
the scene only drops its references in `dispose()`.
