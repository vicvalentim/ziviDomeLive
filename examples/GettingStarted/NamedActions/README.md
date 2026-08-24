# NamedActions

**Category:** Getting Started
**Required dependency:** ControlP5 2.2.6

A focused introduction to activation-owned `SceneActionMap` bindings that are not demonstrated by
the other examples: key-code actions, mouse actions, named registration, and programmatic
`trigger(...)`. The actions move a lit sphere across a 3D floor surrounded by animated markers.
The sketch initializes the spherical domemaster orientation with pitch and yaw at `PI`, and roll at
zero, so its first rendered frame follows the library's negative-Z (`Z-`) forward convention.

## Controls

- `I`, `J`, `K`, `L`: move the sphere across the 3D X/Z plane through key-code bindings;
- mouse press or drag: move it through named mouse bindings;
- `C`: cycle the palette;
- `0`: trigger the separately registered `target.center` action.

Bindings execute on the Processing frame thread and are removed automatically when the scene
activation ends. The scene does not forward Processing events manually or close the action map.

The scene also demonstrates the explicit `SceneCameraService.applyWithViewLighting(...)` call. It
applies the scene camera and replaces the current lights with an ambient/spotlight rig located at
the camera and aimed at its current target. Nothing is synchronized until the scene calls it.
