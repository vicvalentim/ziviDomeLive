# Scene Interface

Only `sceneRender(PGraphicsOpenGL)` is abstract. Every lifecycle and event method has a default implementation, preserving source compatibility for minimal scenes.

```java
class ExampleScene implements Scene {
  public void setupScene() {
    // Allocate or reset scene-owned state.
  }

  public void update() {
    // Advance state once per Processing frame.
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    // Draw only. Do not call beginDraw() or endDraw().
  }

  public void keyEvent(processing.event.KeyEvent event) {}
  public void mouseEvent(processing.event.MouseEvent event) {}
  public void controlEvent(controlP5.ControlEvent event) {}

  public void dispose() {
    // Release resources created by setupScene().
  }

  public String getName() {
    return "Example";
  }
}
```

## Ownership Rules

- The library owns every render target draw lifecycle.
- `update()` is the place for once-per-frame mutation.
- `sceneRender()` may run against Standard and multiple cubemap faces in one frame.
- `setupScene()` may run again after a scene is deactivated and later reactivated.
- `dispose()` must release resources that `setupScene()` will recreate.
- Input callbacks are forwarded automatically; the main sketch must not forward them again.
