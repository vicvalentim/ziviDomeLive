---
title: "Engine-facing Public API"
icon: material/api
status: engine
---
# Engine-facing Public API

Some classes are public because the rendering engine and output integrations need explicit boundaries. They are not prerequisites for ordinary sketches.

Confirmed examples include:

- `FrameViews` — frame-level final views exchanged between the rendering pipeline and output consumers;
- `CubemapTarget` — OpenGL cubemap/FBO/depth target used by the spherical domain;
- `ProcessingGlAdapter` — boundary that concentrates Processing/OpenGL interaction.

Other low-level public GL/cubemap support types should be treated the same way when their Javadocs identify an engine role.

## Stability rule

Do not infer artist-facing stability from Java visibility. Engine-facing public types may expose lifecycle, graphics-context or target-ownership constraints that are inappropriate for the User Guide.

Package-private pipeline/policy classes remain internal even though the Developer Guide may document their architecture.
