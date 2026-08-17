---
title: "Advanced Public API"
icon: material/api
status: advanced
---
# Advanced Public API

Advanced public API is **supported as callable public Java surface**, but it is not required for a first ziviDomeLive scene.

## Scene services

`SceneServices` provides lifecycle-aware services to scenes that opt into them through `Scene.configure(SceneServices)`. Simple scenes can ignore it completely.

Related public facilities include frame/time services such as `FrameClock` and `SimulationTimeline`, plus service-backed project facilities documented in the Scene Services guide.

## Camera and orientation helpers

`OrbitCamera` is an optional scene-space camera helper. `SphericalOrientation` represents spherical orientation/calibration state. Keep those concepts separate: moving the scene camera is not equivalent to rotating the spherical representation.

## Public renderer implementations

Renderer classes such as the Standard, cubemap and projection renderers are exposed for compatibility/advanced integration. Direct use makes lifecycle and graphics-target ownership the caller's concern. Prefer the `ziviDomeLive` facade unless direct renderer control is genuinely required.
