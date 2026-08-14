# Event Handling

The `ziviDomeLive` constructor registers `keyEvent` and `mouseEvent` Processing hooks. The built-in ControlP5 panel registers one listener that forwards relevant events to the active scene.

Implement callbacks on the scene:

```java
public void keyEvent(processing.event.KeyEvent event) {}
public void mouseEvent(processing.event.MouseEvent event) {}
public void controlEvent(controlP5.ControlEvent event) {}
```

Do not add main-sketch forwarding such as `ziviDome.keyEvent(event)`. That delivers the same event twice.

Service-aware scenes can map stable action names instead of branching in the raw
callback:

```java
services.actions().bindKeyPressed("reload", 'R', services::requestReload);
services.actions().register("reset-camera", () -> services.camera().orbit().reset());
services.actions().trigger("reset-camera");
```

The runtime dispatches action bindings before the raw scene callback. The raw
callback still runs for compatibility, so avoid performing the same operation in
both paths. Bindings are cleared automatically when the scene leaves ownership.

## Global Shortcuts

- `h`: toggle the built-in panel
- `m`: cycle the configured preview view
- Left/Right arrows: previous/next scene

Global shortcuts run before the event reaches the scene.

## Camera Input

When `setSceneCameraInputEnabled(true)` or `services.camera().setInputEnabled(true)` is active, navigation gestures reach the scene-space `OrbitCamera` instead of the independent Standard perspective camera. This prevents one drag or wheel event from moving two cameras at once.

All registered callbacks are removed during terminal disposal.
