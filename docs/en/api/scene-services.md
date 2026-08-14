# Scene Services

`SceneServices` is a lifecycle-aware context created for each activation of a
`Scene`. It removes common infrastructure from large sketches while keeping
astronomy, physics, game rules, and rendering decisions in application code.

Receive the context through the optional `configure()` callback and register the
scene through the facade:

```java
class ExampleScene implements Scene {
  private SceneServices services;

  public void configure(SceneServices services) {
    this.services = services;
  }

  public void setupScene() {
    services.timeline().setFixedStep(1.0 / 120.0);
    services.timeline().setMaxSubSteps(8);
    services.actions().bindKeyPressed(
        "reload", 'R', services::requestReload);
    services.camera().setInputEnabled(true);
  }

  public void update() {
    services.timeline().advance(
        services.frameClock().getDeltaSeconds(),
        this::simulate);
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    services.camera().apply(pg);
    // Draw only; the library owns beginDraw()/endDraw().
  }

  private void simulate(double step) {
    // Advance domain state by one fixed simulation step.
  }
}

dome.setScene(new ExampleScene());
```

`setScene()` and the facade-level `registerScene()` are the preferred
service-aware registration paths. A detached `SceneManager` can activate its
first scene before it is attached to the facade, so code that needs services in
its first `setupScene()` should register through the facade.

## Available Services

| Accessor | Responsibility |
|---|---|
| `frameClock()` | Monotonic frame delta, elapsed time, frame index, and configurable hitch clamping |
| `timeline()` | Pausable, rate-controlled fixed-step simulation with bounded catch-up telemetry |
| `renderQueue()` | Cross-thread handoff to the Processing/OpenGL thread at the next frame boundary |
| `tasks()` | Keyed, bounded scene tasks using the library's shared worker pool |
| `assets()` | Render-thread image, shader, and retained-shape caches |
| `actions()` | Named keyboard/mouse actions while raw `Scene` callbacks remain available |
| `camera()` | Shared `OrbitCamera`, input ownership, and optional dynamic target tracking |
| `environment()` | Scene-scoped LDR Environment configuration that restores replaced state |
| `onDispose()` | Additional last-in/first-out cleanup for application-owned resources |

`parent()`, `applet()`, and `scene()` expose the owning facade, Processing
applet, and scene identity when direct access is necessary.

## Frame And Reload Ordering

For each active frame the facade drains the render queue, ticks `FrameClock`,
calls `Scene.update()`, updates a tracked camera target, advances the shared
camera, synchronizes its quaternion with Environment orientation, and then
renders. `requestReload()` does not mutate the scene from an input callback. It
defers a complete dispose/setup cycle to the next frame boundary and supplies a
fresh `SceneServices` context.

The fixed-step timeline executes at most `maxSubSteps` callbacks per frame. If a
hitch produces more complete steps, the excess is reported through
`getDroppedUnits()` instead of creating an unbounded catch-up spiral.

## Thread And Resource Rules

Processing and OpenGL objects must be created or mutated on the render thread.
Background work may use `tasks()`, then enqueue the minimal Processing/OpenGL
handoff with `renderQueue()`. Keyed `submitIfIdle()` is useful for continuous
work because it prevents a frame-by-frame backlog. Processing can execute sketch
setup and JOGL animation on different threads; the runtime therefore establishes
the queue's authoritative affinity at each `pre()` frame boundary.

`SceneAssets` treats ordinary Processing images, shaders, and shapes as borrowed:
closing the context clears references but does not dispose Processing-managed
objects. Use `SceneResourceCache.getOrCreateOwned()` only for resources whose
explicit disposer belongs to the scene. Owned resources are disposed in reverse
insertion order.

Before `Scene.dispose()` the runtime stops action dispatch, cancels scene tasks,
rejects queued render work, disables scene-camera input, and clears target
tracking. After `dispose()` it runs custom cleanup, restores the Environment state
that activation replaced, and closes asset caches. Closing one scene never shuts down the library-wide
`ThreadManager`.

The `SolarSystem` example is the maintained reference for all of these services.
Julian Date conversion, orbital propagation, celestial-body models, and astronomy
rendering remain example-domain code rather than core API.
