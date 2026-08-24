# SphereParticle

**Category:** Advanced
**Required dependency:** ControlP5 2.2.6

Demonstrates activation-scoped background work with `SceneServices.tasks()`. Particle simulation
runs on the shared worker pool; immutable results return through a callback at the owning scene's
frame boundary. Old-activation work cannot publish after disposal.

## Interaction

- press or drag the mouse to add particles;
- press any key to observe the routed scene callback.

The example never calls Processing/OpenGL APIs from a worker. Rendering reads one published
snapshot in `sceneRender(...)`, while lifetime and rotation advance in `update()`.
