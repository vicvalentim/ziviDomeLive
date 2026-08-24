---
title: Internal Boundary
icon: material/shield-lock-outline
status: internal
tags:
  - Architecture
  - Internal
---

# Internal Boundary

The former “engine-facing public API” category was eliminated during the final 2.0 freeze. Renderer/output implementation can be documented for maintainers without being callable by sketches.

## Physical source taxonomy

| Folder | Responsibility |
|---|---|
| `_internal/render/core` | Frame requirement resolution, environment composition and pipeline orchestration |
| `_internal/render/camera` | Cubemap face/orientation and internal camera routing |
| `_internal/render/gl` | Processing/JOGL adapter, cubemap/FBO resources and GPU measurement seam |
| `_internal/render/modes` | Standard, domemaster, equirectangular and skybox implementations |
| `_internal/output` | Final-frame containers, output manager implementation and backend producers |
| `_internal/performance` | Mutable monitoring, statistics and GPU timers |
| `_internal/runtime` | Render-thread queue and shared executor |
| `_internal/scene` | Default scene and resource-cache implementation |
| `_internal/ui` | ControlP5 panel, input bridge, layout and splash |
| `_internal/support` | Logging and library metadata implementation |

The physical folders improve maintainability. Package declarations intentionally retain the collaboration boundary needed by package-private implementation classes.

## Historical names

`FrameViews`, `CubemapTarget`, `ProcessingGlAdapter`, concrete renderers, `ThreadManager`, `ControlManager` and similar names may appear in architecture/history. They are not public 2.0 API and must never appear in current sketch code.

## Promotion rule

An internal type can become public only through an explicit future API proposal with audience, ownership, lifecycle, Javadocs, tests, examples and compatibility policy. Convenience alone is insufficient.
