# Scene Management

## Register Scenes

```java
SceneManager manager = new SceneManager();
manager.registerScene(new OpeningScene());
manager.registerScene(new PerformanceScene());
dome.setSceneManager(manager);
```

The first scene becomes active during registration. Additional scenes remain registered but inactive until selected.

## Switch Scenes

```java
manager.nextScene();
manager.previousScene();
manager.setCurrentSceneIndex(1);
manager.activateScene(sceneInstance);
```

Changing the active scene follows this order:

1. Update active ownership in `SceneManager`.
2. Call `dispose()` on the leaving scene.
3. Call `setupScene()` on the arriving scene.
4. Synchronize Standard renderers with the new active scene.

Selecting the already active scene is a no-op.

## Replace the Manager

`dome.setSceneManager(newManager)` transfers facade authority to the new manager. The old active scene is disposed unless the same scene instance is being transferred to the new manager.

## Release Resources

Call `dome.dispose()` only for terminal shutdown. It releases outputs, controls, scenes, renderers, callbacks, splash resources, and the shared thread manager. Repeated calls are safe.
