# PortLoopback

**Category:** Getting Started
**Required dependency:** ControlP5 2.2.6

A transport-free demonstration of the `ScenePorts` SPI. A tiny input adapter publishes integer
messages into the activation's bounded queue; the scene receives them at a later Processing frame
boundary. A managed, non-blocking output adapter reports the applied value. The current message
level drives a 3D ring of eight signal towers, a central hub, and an animated data pulse. The
sketch initializes spherical pitch and yaw at `PI`, with roll at zero, so the first domemaster
frame follows the library's negative-Z (`Z-`) forward convention.

## Controls

- `+` / `=`: enqueue a larger level;
- `-`: enqueue a smaller level.

Replace `ManualIntegerInput` or `ConsoleOutput` with an optional MIDI, OSC, or device adapter while
keeping the scene message types unchanged. After connection, `ScenePorts` owns adapter closure, so
the scene only drops its references in `dispose()`.
