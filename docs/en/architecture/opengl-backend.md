---
title: "OpenGL Backend"
icon: material/source-branch
---
# OpenGL Backend


The 2.0 spherical renderer uses an OpenGL cubemap target and samplerCube-based projection shaders.

## Boundary with Processing

An internal Processing/OpenGL adapter concentrates graphics allocation, framebuffer operations, cubemap binding, readback and capability discovery. An internal cubemap target owns texture/FBO/depth resources. These implementation types are described here for maintenance and are not public API.

## State and ownership

Low-level OpenGL work must respect Processing's active graphics context and restore/contain state according to the implementation contract. Raw GL identifiers should remain implementation details unless a public Javadoc explicitly exposes them.

## No roadmap leakage

PBO readback, HDR/IBL and other future optimizations/features are not current 2.0 capabilities merely because they are plausible OpenGL techniques. Document them only in Roadmap until implemented and qualified.
