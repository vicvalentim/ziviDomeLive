# Core Classes

## ziviDomeLive

Create one instance with the active `PApplet`, then call `setup()` once:

```java
ziviDomeLive dome = new ziviDomeLive(this);
dome.setup();
```

The constructor registers Processing lifecycle and input hooks immediately.
`setup()` creates output services and the startup scene; GPU renderers are
created lazily from the registered `post()` hook after Processing has a valid
OpenGL surface. `getInitState()` exposes the setup milestones
`NOT_INITIALIZED`, `SETUP_COMPLETE`, and `MANAGERS_READY`; `READY` is reserved.
Use `isInitialized()` for the common render-ready check. Pause and disposal are
separate lifecycle concerns rather than additional `InitState` values.

Key method groups:

| Group | Methods |
|---|---|
| Scene | `setScene()`, `setSceneManager()`, `getSceneManager()` |
| Render behavior | `setRenderMode()`, `getRenderMode()`, `setCurrentView()` |
| Calibration | `setFov()`, `setFishSize()`, `setPitch()`, `setYaw()`, `setRoll()`, `resetOrientation()` |
| Preview | `setShowPreview()`, `setStandardOutputAspectMode()` |
| Output | `getOutputManager()`, `resetGraphics()`, `getOutputResolution()` |
| Camera | `getSceneCamera()`, `setSceneCameraInputEnabled()` |
| Lifecycle | `pause()`, `resume()`, `dispose()` |

## SceneManager

`SceneManager` is the sole active-scene authority. It rejects null and duplicate registrations, activates the first scene automatically, and avoids reinitializing an already active scene.

```java
SceneManager scenes = new SceneManager();
scenes.registerScene(new SceneA());
scenes.registerScene(new SceneB());
scenes.nextScene();
```

Switching disposes the leaving scene and sets up the arriving scene. `clearScenes()` disposes the active scene and removes every registration.

| Operation | Behavior |
|---|---|
| `registerScene(scene)` | Adds a unique non-null scene; the first one becomes active |
| `activateScene(scene)` | Activates a registered scene by identity |
| `nextScene()` / `previousScene()` | Wraps through the registration order |
| `setCurrentSceneIndex(index)` | Selects a valid zero-based index |
| `containsScene()` / `getSceneCount()` | Inspects registration state |
| `clearScenes()` | Disposes the active scene and clears all registrations |

## OutputManager

The manager separates configured route, backend availability, native initialization, publication, and render requirements.
Internally it delegates native ownership to concrete NDI, Syphon, and Spout services;
those implementation classes are not part of the public API.

```java
OutputManager output = dome.getOutputManager();
output.setViewForOutput(
    OutputManager.OutputType.NDI,
    ViewType.EQUIRECTANGULAR);
output.toggleOutput("ndi");
```

Use `getOutputState()` and `getOutputFailureReason()` for diagnostics. Use `isNdiEnabled()`, `isSyphonEnabled()`, or `isSpoutEnabled()` only when publication state is the specific question.

| State | Meaning |
|---|---|
| `UNAVAILABLE` | Unsupported backend or failed last initialization |
| `AVAILABLE` | Backend is eligible but owns no native resources |
| `INITIALIZED` | Native resources exist; publication is disabled |
| `ENABLED` | Native resources exist and frames are published |
| `STOPPING` | NDI publication stopped while bounded cleanup completes |

`setViewForOutput()` changes a saved route. A dedicated `RenderMode` overrides
the effective route without deleting that saved value; `FULL` restores it.
Syphon and Spout receive the selected `PGraphicsOpenGL` directly. NDI performs
pixel readback on the render thread and sends through a bounded three-slot
worker pipeline.

The automatic `RenderPipeline` supplies completed targets through `FrameViews`.
`OutputManager` chooses the logical `ViewType` to publish and does not inspect
the concrete renderer that produced it. Applications normally do not need to
call the frame-aware `sendOutput(FrameViews)` overload directly.

## SphericalOrientation

`SphericalOrientation` owns the shared attitude for every spherical projection.
Its setters accept cyclic control values, calculate the shortest delta, and
compose that delta around local pitch `X`, yaw `Z`, or roll `Y` axes. The stored
quaternion is normalized after composition.

`getPitch()`, `getYaw()`, and `getRoll()` return the latest control accumulators;
they are not an Euler conversion of `getQuaternion()`. Command order is
therefore significant. `reset()` restores identity and zero accumulators.

Applications usually access this behavior through the facade's calibration
methods rather than constructing a separate orientation object.

## OrbitCamera

`OrbitCamera` is an optional scene-space transform. It is shared across all
targets so Standard and spherical views see the same scene attitude.

Configure distance limits, collapse guard, interpolation, drag sensitivity,
and wheel steps through its setters. `setTarget()`, `setDistance()`,
`setOrientation()`, `snapTo()`, and `reset()` update its desired state. Callers
normally retrieve the shared instance with `getSceneCamera()` and let the
facade forward mouse input only while `setSceneCameraInputEnabled(true)` is
active.

## Renderers

The public 1.x renderer classes remain available for compatibility:

- `StandardRenderer`
- `CubemapRenderer`
- `EquirectangularRenderer`
- `FisheyeDomemaster`
- `CubemapViewRenderer`

Applications should prefer the facade and `RenderMode`. Direct renderer ownership is advanced 1.x integration and may not transfer unchanged to 2.0.

Do not retain a renderer target across `resetGraphics()`: resolution changes
are deferred to the render loop and can replace high-resolution renderer
instances. Query the facade again after the reset is applied.
