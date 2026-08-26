# ziviDomeLive Core

ziviDomeLive Core is the platform-independent Java 17 foundation shared by future ziviDomeLive
hosts. It contains qualified time, mathematics, camera, projection, action, task, port,
environment, lifecycle, and resource-ownership semantics.

The current **ziviDomeLive for Processing** project remains the artist-facing Processing 4 host
and golden behavioral reference. A separate **ziviDomeLive Engine** host is planned; it does not
exist in this repository yet.

Core does not render, open windows, own GPU resources, or depend on Processing, JOGL, LWJGL,
ControlP5, NDI, Syphon, or Spout. Production code has no external dependencies.

## Spatial and frame conventions

- `+Z` is the front of the dome and `-Z` is the rear.
- The default scene camera is on the `-Z` side looking toward `+Z`.
- Orbit distance is signed. Negative values are valid and must not be normalized away.
- Spherical pitch/yaw/roll are projection controls independent of scene-camera orientation.
- Hosts update mutable state once per frame and may render that state many times. Rendering a
  cubemap face must never tick a clock, advance a timeline, interpolate a camera, drain input, or
  execute a simulation step.

## Packages

- `time`: monotonic frame timing and bounded fixed-step simulation.
- `math`: immutable float quaternion and small immutable 3D vector values.
- `camera`: host-neutral signed-distance orbit-camera state and interpolation.
- `projection`: spherical control orientation and projection/calibration state.
- `action`: synchronous named actions.
- `task`: frame-thread delivery and activation-owned bounded background work.
- `ports`: generic bounded input and non-blocking output adapter lifecycle.
- `environment`: visual environment scalars and source orientation; no image/texture handle.
- `lifecycle`: scoped restoration and generic borrowed/owned resource caching.

## Thread and lifecycle ownership

`FrameThreadQueue` is bound by a host at its authoritative frame boundary. `TaskGroup` workers run
off that thread and publish callbacks back through the queue. `Ports` accepts producer data from
arbitrary threads but invokes handlers only when the bound frame thread drains it.

One host activation owns one set of queues, tasks, ports, actions, and scoped overrides. Pause
drops queued external input. Disposal stops admission before domain disposal, suppresses stale
callbacks, closes adapters in reverse registration order, and then releases remaining resources.

## Build and publication

From the containing build:

```text
./gradlew :core:clean :core:check :core:javadoc :core:publishCorePublicationToCoreTestRepository
```

Artifacts are `zividomelive-core-0.1.0-SNAPSHOT.jar`, sources JAR, Javadoc JAR, and a Maven POM.
The local publication repository is written below `core/build/` and never publishes remotely.

Core 0.1 is a development API extracted from the frozen 2.0 behavior. Its public types are usable,
but the compatibility tier is not yet 1.0 Stable.
