---
title: Scene Services
icon: material/layers-triple-outline
status: advanced
tags:
  - API
  - Lifecycle
  - Concurrency
---

# Scene Services

`SceneServices` is an **Advanced Stable**, activation-scoped capability set. It is created, advanced and closed by `ziviDomeLive`, then supplied before each `setupScene()`.

```java
class ServiceScene implements Scene {
  SceneServices services;

  public void configure(SceneServices services) {
    this.services = services;
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    services.camera().apply(pg);
    // draw
  }
}
```

## Service map

| Accessor | Focus | Scene controls | Runtime owns |
|---|---|---|---|
| `applet()` | Processing host | Read/use ordinary applet facilities on the correct thread | Applet lifetime |
| `frameClock()` | Frame time | Maximum accepted delta | Tick and monotonic frame index |
| `timeline()` | Simulation | Rate, position, fixed step, catch-up policy, pause | None of the scene's step policy |
| `tasks()` | Background CPU/I/O | Bounded keyed submission, result/error callbacks | Shared executor, cancellation, frame-boundary publication |
| `assets()` | Images/shaders/shapes | Requests and retained shapes | Render-thread creation and cache shutdown |
| `actions()` | Named input | Bind, trigger, unregister | Dispatch order and activation cleanup |
| `camera()` | Scene-space navigation and opt-in view lighting | Pose, input, tracking, light-rig call | Once-per-frame update and stale-anchor reset |
| `environment()` | Background overrides | Image, visibility, intensity, longitude yaw, fixed source orientation | Conditional restoration on deactivation |
| `ports()` | Optional adapters | Connect bounded input/output ports | Drain limit, telemetry and closure |
| `requestReload()` | Lifecycle | Ask for reload | Execute at a safe frame boundary |

## Ownership restrictions

Scenes cannot construct or close runtime-supplied service objects. `parent`, `scene`, raw render queues, arbitrary dispose hooks and service `close()` methods are intentionally absent.

Only `SceneInputPort` and `SceneOutputPort` extend `AutoCloseable`; that is an adapter-provider SPI. `ScenePorts` still retains activation ownership and closes connected adapters.

## Background task contract

`SceneTaskGroup.submitIfIdle(key, ...)` is bounded and callback-based. It does not return `Future`, expose the executor or accept unbounded work.

```java
services.tasks().submitIfIdle(
    "mesh",
    () -> buildCpuOnlyMeshData(),
    data -> publishOnFrameBoundary(data),
    error -> report(error));
```

- the callable/runnable must not call Processing/OpenGL;
- result and error consumers run only for the submitting activation;
- stale work from a disposed activation cannot publish into a later activation of the same `Scene` instance;
- `getInFlightCount()` and `getMaxInFlight()` expose bounded-state telemetry.

## Ports contract

`connectInput(port, consumer)` accepts external-thread data into a bounded activation queue. The runtime delivers a bounded amount at a frame boundary. `getPendingInputCount()` and `getDroppedInputCount()` make backpressure observable.

`connectOutput(port)` retains the provider and its non-blocking `offer(value)` contract. Real MIDI, OSC or device adapters remain optional and outside the core library.

## Environment restoration

The environment service restores only values it changed and only while facade state still matches the value it applied. A later owner is never overwritten by stale cleanup.

## Camera-synchronized view lighting

`camera().applyWithViewLighting(pg)` is an explicit alternative to `camera().apply(pg)`. It applies
the current orbit transform, clears the target's lights, and installs a neutral ambient light plus
a warm spotlight located at the scene-camera position and aimed at its current look-at target.
Additional lights may be added afterward.

The call only reads the already-updated camera pose. It does not advance interpolation, so every
cubemap face in the same Processing frame observes the same position and direction. Lighting is
never enabled automatically; use ordinary `apply(pg)` when a scene owns a different lighting
model or shader pipeline.

Ordinary scenes can stay on the root wildcard import: `setDistanceLimits`, `setCollapseGuard`,
`setLerpFactor`, `setDragSensitivity`, and `snapToAxisAngle` forward the common orbit operations.
The advanced `orbit()` accessor remains available when code intentionally works with
`render.camera.OrbitCamera` or `render.Quaternion` directly.

## Reload

`requestReload()` defers the request. The next safe frame boundary performs stop-work → `dispose()` → service release → fresh services → `configure()` → `setupScene()`.
