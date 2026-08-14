# Event Handling

The `ziviDomeLive` constructor registers `keyEvent` and `mouseEvent` Processing hooks. The built-in ControlP5 panel registers one listener that forwards relevant events to the active scene.

Implement callbacks on the scene:

```java
public void keyEvent(processing.event.KeyEvent event) {}
public void mouseEvent(processing.event.MouseEvent event) {}
public void controlEvent(controlP5.ControlEvent event) {}
```

Do not add main-sketch forwarding such as `ziviDome.keyEvent(event)`. That delivers the same event twice.

## Global Shortcuts

- `h`: toggle the built-in panel
- `m`: cycle the configured preview view
- Left/Right arrows: previous/next scene

Global shortcuts run before the event reaches the scene.

## Camera Input

When `setSceneCameraInputEnabled(true)` is active, navigation gestures reach the scene-space `OrbitCamera` instead of the independent Standard perspective camera. This prevents one drag or wheel event from moving two cameras at once.

All registered callbacks are removed during terminal disposal.
