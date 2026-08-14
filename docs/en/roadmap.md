# Roadmap

## Scene Services Runtime (delivered in 2.0)

Infrastructure proven by the maintained `SolarSystem` example is now available
through lifecycle-aware API services:

- `SceneServices` as the stable activation-scoped access point;
- `FrameClock`, `SimulationTimeline`, and bounded fixed-step advancement;
- deferred scene reload and last-in/first-out cleanup;
- a render-thread queue for Processing/OpenGL handoff;
- bounded, keyed scene task groups backed by the shared `ThreadManager`;
- typed image, shader, and shape caches with explicit ownership;
- action-based input mapping, `OrbitCamera` target tracking, and scene-scoped
  Environment configuration.

The next API work is adoption-driven: refine these contracts only from multiple
maintained consumers, preserve source compatibility, and keep GPU ownership
explicit. Candidate additions include reusable diagnostics/telemetry and optional
asset preloading policies, but neither is a release commitment.

Astronomy-specific models, Julian Date conversion, Kepler propagation, and orbital
rendering remain outside the core. They may become an optional domain module only
after more than one maintained consumer establishes a stable contract.
