---
title: "Lifecycle and Resource Ownership"
icon: material/source-branch
---
# Lifecycle and Resource Ownership


Lifecycle is part of correctness because Processing/OpenGL resources belong to a graphics context and scenes/outputs can be activated, resized or disposed.

## Scene lifecycle

A scene may receive services, run setup, update/render, and later receive `dispose()` on switch, clear, replacement or facade release. A later activation may set up again.

## Graphics targets

Output-target recreation is requested through `resetGraphics(int)` and applied at the appropriate render/draw boundary. Renderer/target implementation remains internal, so sketches cannot retain invalidated render resources.

## Ownership rule

The library owns the active draw frame of the `PGraphicsOpenGL` passed to `sceneRender()`. Scene code must not call `beginDraw()`/`endDraw()` there and should not retain the target as scene-owned state.

## Shutdown

Outputs, background workers and native resources must reach deterministic shutdown before process/release completion. Backend-specific details are covered in Output Backends.
