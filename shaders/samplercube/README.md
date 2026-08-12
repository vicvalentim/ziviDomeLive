SamplerCube spherical shaders
=============================

These shaders are adapted from the external `ShadersSpherical.zip` reference
provided for the Processing PGL migration. They are packaged with the library
under `data/shaders/samplercube/`.

They are intentionally not wired into the runtime yet. The active 1.x pipeline
still uses `data/shaders/equirectangular.*` and `data/shaders/domemaster.*`
with `PGraphicsOpenGL[]` cubemap faces. These samplerCube shaders are reserved
for the native `CubemapTarget` projection PRs.
