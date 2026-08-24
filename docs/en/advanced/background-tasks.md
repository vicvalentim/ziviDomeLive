---
title: "Background Tasks"
icon: material/layers-triple-outline
---
# Background Tasks

Background work is useful for CPU/network/file tasks that should not block rendering. It must not assume ownership of the Processing OpenGL context. Schedule graphics-context work through the current render-thread mechanism exposed by Scene Services, and release/cancel scene-owned work during `dispose()` as required by the current API.
