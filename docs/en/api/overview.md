---
title: "API Overview"
icon: material/api
---
# API Overview

The public Java surface is deliberately documented by **audience and stability role**, not only by the `public` modifier.

## Artist-facing stable

Start here for normal Processing projects:

- `ziviDomeLive`
- `Scene`
- `SceneManager`
- `RenderMode`
- `ViewType`
- `OutputManager`

These types define the normal scene, rendering, calibration, preview and output workflow.

## Advanced public

Public facilities intended for projects that need more lifecycle, time, task, camera or direct renderer control include `SceneServices`, `FrameClock`, `SimulationTimeline`, `OrbitCamera`, `SphericalOrientation` and public renderer implementations. They remain callable public API, but are not prerequisites for a simple scene.

## Experimental public

Performance instrumentation is experimental/qualification-oriented. Treat the generated Javadocs and the Performance Profiling page as the contract for the exact metrics currently implemented. CPU wall time and GPU elapsed time are different measurements.

## Engine-facing public

Some public types exist because renderer/output components need a callable boundary. Examples include `FrameViews`, `CubemapTarget` and `ProcessingGlAdapter`. They are documented for contributors and advanced integration work; they are not part of the artist learning path.

## Deprecated compatibility surface

Deprecated methods remain documented while they exist so existing sketches can migrate. Do not use deprecated convenience methods in new examples when a current route is available.

## Internal architecture is not public API

Types such as the package-private rendering policy/pipeline internals can be documented in the Developer Guide without being presented as callable API.

For exact signatures, always use the generated Javadocs. If prose and Javadocs disagree, implementation/Javadocs win and the prose must be corrected.
