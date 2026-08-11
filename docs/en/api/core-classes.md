# Core Classes

## zividomelive

Create one instance with the active `PApplet`, then call `setup()` once:

```java
zividomelive dome = new zividomelive(this);
dome.setup();
```

Key method groups:

| Group | Methods |
|---|---|
| Scene | `setScene()`, `setSceneManager()`, `getSceneManager()` |
| Render behavior | `setRenderMode()`, `getRenderMode()`, `setCurrentView()` |
| Calibration | `setFov()`, `setFishSize()`, `setPitch()`, `setYaw()`, `setRoll()` |
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

## OutputManager

The manager separates configured route, backend availability, native initialization, publication, and render requirements.

```java
OutputManager output = dome.getOutputManager();
output.setViewForOutput(
    OutputManager.OutputType.NDI,
    zividomelive.ViewType.EQUIRECTANGULAR);
output.toggleOutput("ndi");
```

Use `getOutputState()` and `getOutputFailureReason()` for diagnostics. Use `isNdiEnabled()`, `isSyphonEnabled()`, or `isSpoutEnabled()` only when publication state is the specific question.

## Renderers

The public 1.x renderer classes remain available for compatibility:

- `StandardRenderer`
- `CubemapRenderer`
- `EquirectangularRenderer`
- `FisheyeDomemaster`
- `CubemapViewRenderer`

Applications should prefer the facade and `RenderMode`. Direct renderer ownership is advanced 1.x integration and may not transfer unchanged to 2.0.
