# Scene Management

## Register Scenes

```java
dome.setScene(new OpeningScene());
dome.registerScene(new PerformanceScene());
SceneManager manager = dome.getSceneManager();
```

The first scene becomes active during registration. Additional scenes remain registered but inactive until selected. Facade registration guarantees that `configure(SceneServices)` runs before the first `setupScene()`.

## Switch Scenes

```java
manager.nextScene();
manager.previousScene();
manager.setCurrentSceneIndex(1);
manager.activateScene(sceneInstance);
```

Changing the active scene follows this order:

1. Stop input, tasks, queued render work, and camera tracking for the leaving context.
2. Call `dispose()` on the leaving scene, then close its remaining services.
3. Create a fresh context and call `configure()` then `setupScene()` on the arriving scene.
4. Synchronize Standard renderers with the new active scene.

Selecting the already active scene is a no-op.

## Replace the Manager

`dome.setSceneManager(newManager)` transfers facade authority to the new manager. The old active scene is disposed unless the same scene instance is being transferred to the new manager. Because a detached manager activates its first registration immediately, prefer facade registration for scenes that require services during their first setup.

## Release Resources

Call `dome.dispose()` only for terminal shutdown. It releases outputs, controls, scenes, renderers, callbacks, and splash resources. Scene task groups are cancelled; the process-wide shared thread pool remains available to other library instances. Repeated calls are safe.
