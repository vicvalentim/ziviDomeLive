---
title: "Spherical Domain"
icon: material/source-branch
---
# Spherical Domain


The Spherical Domain captures the scene into a native cubemap target and derives the supported spherical representations from that capture.

## Capture

The cubemap contains the six directions required to represent the spherical scene. `sceneRender()` can therefore be invoked multiple times during a single Processing frame while the cubemap is captured.

This is why mutable state must advance in `update()` when all faces must observe one coherent state.

## Projection siblings

Domemaster, Equirectangular and Skybox are sibling final projections derived from the cubemap. They should not be documented as a serial conversion chain when the implementation can project them directly from the shared cubemap.

Pitch/Yaw/Roll belongs to the shared spherical orientation. Domemaster additionally applies its FOV and Size% calibration controls.
