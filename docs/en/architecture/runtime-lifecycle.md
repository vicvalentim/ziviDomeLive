# Runtime Lifecycle

The `ziviDomeLive` facade owns Processing hook registration, manager initialization, scene updates, renderer resources, input forwarding, pause/resume, and terminal disposal. Its `draw()` hook delegates per-frame ordering to the internal `RenderPipeline`, which uses the existing renderer backend without changing target ownership.

## Initialization States

| `InitState` | Meaning |
|---|---|
| `NOT_INITIALIZED` | Instance exists; `setup()` has not completed |
| `SETUP_COMPLETE` | Basic services exist; renderer managers wait for a valid post-setup OpenGL context |
| `MANAGERS_READY` | Camera, renderers, local texture backend, and controls are ready |
| `READY` | Reserved 1.x enum value for future lifecycle expansion |

Typical sequence:

```text
constructor
  -> register pre/draw/post/input/dispose hooks
setup()
  -> target frame rate, OpenGL diagnostics, hints, OutputManager, splash, bootstrap scene
first post()
  -> CameraManager, output and preview renderers, Syphon/Spout preparation, ControlManager
  -> MANAGERS_READY
```

`initializeManagers()` is public for 1.x compatibility, but ordinary sketches rely on the registered `post()` hook. Duplicate `setup()` calls are ignored.

## Processing Hooks

| Hook | Responsibility |
|---|---|
| `pre()` | Drain the scene render queue, tick its clock, handle deferred reload or call `Scene.update()`, track the camera target, advance `OrbitCamera`, then synchronize Environment orientation |
| `draw()` | Delegate frame ordering to `RenderPipeline`, then handle splash state |
| `post()` | Lazily initialize managers once after Processing setup |
| `keyEvent()` | Global shortcuts, then active-scene forwarding |
| `mouseEvent()` | Forward to the active scene, then route navigation to either the scene camera or Standard camera |
| `controlEvent()` | Internal panel handling, then active scene |
| `dispose()` / `stop()` | Terminal cleanup |

Do not call or forward these hooks manually from a sketch.

## Scene Ownership

`SceneManager` is the active-scene authority:

- the first facade-registered scene is activated, receives a fresh `SceneServices` context through `configure()`, then receives `setupScene()`;
- activating a different scene disposes the leaving scene before setting up the arriving scene;
- selecting the active scene again is a no-op;
- inactive scenes that were never activated have not entered setup/dispose ownership;
- `clearScenes()` disposes the active scene and clears registrations;
- replacing a manager preserves a transferred active instance and otherwise disposes old ownership.

`setScene()` and facade `registerScene()` are the preferred service-aware paths.
For every activation the context owns frame timing, fixed-step simulation, a
render-thread queue, bounded tasks, assets, actions, camera tracking, and
Environment configuration. A deferred reload closes the old context and creates
a new one without mutating scene ownership inside an input callback.

`StandardRenderer` instances are synchronized with the current scene after ownership changes.

## Pause and Resume

`pause()` records which outputs were publishing, stops output services, and gates update/render work. `resume()` reinitializes required managers when necessary and attempts to restore the previously enabled publications.

Backend restoration can fail independently. Query `OutputState` and `getOutputFailureReason()` rather than assuming a successful native restart.

## Terminal Disposal

`dispose()` is idempotent and terminal. It:

1. marks the facade disposed;
2. releases splash and ControlP5 resources;
3. shuts down NDI, Syphon, and Spout;
4. disposes preview and output render targets;
5. clears scene ownership;
6. disposes camera state;
7. cancels scene task groups while leaving the process-wide shared `ThreadManager` available to other library instances;
8. unregisters Processing callbacks.

After disposal, setup, scene changes, rendering, and manager initialization are ignored.

## Thread Boundaries

- Processing and OpenGL work remains on the Processing thread.
- `Scene.sceneRender()` must not create its own draw lifecycle around the provided target.
- NDI CPU conversion and sending use a dedicated worker with bounded shutdown.
- Scene background work should use `SceneServices.tasks()`; Processing/OpenGL handoff should use `renderQueue()`.
- The runtime cancels each scene task group before `Scene.dispose()` and clears asset/Environment ownership afterward.

## Error Recovery

Partial manager initialization rolls back allocated resources and returns to `SETUP_COMPLETE` instead of advancing to a ready state. A later valid initialization attempt can retry.

Syphon/Spout publication errors disable publication without immediately destroying their prepared backend. NDI initialization failure marks the backend unavailable; another explicit enable request retries.

See [Scene Services](../api/scene-services.md), [Scene Management](../usage/scene-management.md), [Event Handling](../usage/event-handling.md), and [External Integration](../usage/external-integration.md).
