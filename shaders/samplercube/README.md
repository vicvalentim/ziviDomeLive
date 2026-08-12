SamplerCube spherical shaders
=============================

These shaders are adapted from the external `ShadersSpherical.zip` reference
provided for the Processing PGL migration. They are packaged with the library
under `data/shaders/samplercube/`.

The 2.0 pipeline wires the equirectangular, fisheye, and cubemap-layout
samplerCube shaders when a native `CubemapTarget` is available, while keeping the legacy
`data/shaders/equirectangular.*` and `data/shaders/domemaster.*` passes as
fallbacks for unsupported OpenGL contexts.
