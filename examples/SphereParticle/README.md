# SphereParticle

This example separates simulation from rendering so every cubemap face and output target receives the same particle state.

- Click or drag to add particle bursts.
- Press `Space` to add a burst at the origin.
- Press `C` to clear the field.
- Press `R` to restore the initial field.

The simulation is bounded to 240 particles and advances once in `Scene.update()`. `sceneRender()` performs graphics work only; it never starts background jobs or mutates the particle collection.
