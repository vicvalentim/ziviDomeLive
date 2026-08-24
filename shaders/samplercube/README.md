SamplerCube spherical shaders
=============================

These shaders are adapted from the external `ShadersSpherical.zip` reference
provided for the Processing PGL migration. They are packaged with the library
under `data/shaders/samplercube/`.

The spherical pipeline wires the equirectangular, fisheye, and cubemap-layout
samplerCube shaders directly to the native `CubemapTarget`. The previous
six-texture Processing shader passes were removed with the independent
face-target array path.
