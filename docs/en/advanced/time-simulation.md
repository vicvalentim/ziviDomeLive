---
title: "Time and Simulation"
icon: material/layers-triple-outline
---
# Time and Simulation

Use explicit time/simulation services only when a project needs deterministic or lifecycle-aware timing beyond simple state updated in `Scene.update()`. Keep all state that must advance once per Processing frame outside `sceneRender()`. Consult generated Javadocs for the exact `FrameClock`/`SimulationTimeline` methods in 2.0.
