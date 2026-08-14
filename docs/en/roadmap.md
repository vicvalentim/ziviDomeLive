# Roadmap

## Scene Services Runtime (post-2.0)

The 2.0 release remains focused on the qualified native cubemap, projection,
Environment, scene lifecycle, camera, and output contracts. A later release may
turn infrastructure proven by larger examples into lifecycle-aware API services:

- `SceneServices` or `SceneContext` as one stable access point;
- `FrameClock`, `SimulationTimeline`, and bounded fixed-step advancement;
- deferred scene reload and scene-scoped cleanup;
- a render-thread queue for Processing/OpenGL resource creation;
- scene task groups backed by the shared `ThreadManager`;
- typed image, shader, and primitive caches with explicit ownership;
- action-based input mapping and optional `OrbitCamera` target tracking.

Astronomy-specific models, Julian Date conversion, Kepler propagation, and orbital
rendering remain outside the core. They may become an optional domain module only
after more than one maintained consumer establishes a stable contract.

These services are intentionally not part of 2.0.0 and do not block its release.
