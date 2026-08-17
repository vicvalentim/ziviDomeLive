---
title: Architecture Overview
icon: material/source-branch
---

# Architecture Overview

ziviDomeLive 2.0 organizes rendering into **two domains** that can be required independently or in the same Processing frame. This separation is the central architectural fact behind routing and reuse.

<figure markdown="span">
  ![Standard and spherical rendering domains](../img/architecture-domains.png)
  <figcaption>Standard rendering remains independent; spherical final views share one cubemap capture.</figcaption>
</figure>

<div class="grid cards" markdown>

- :material-monitor: **Standard Domain**

    `Scene` → Standard renderer → Standard final target. No spherical cubemap is required for Standard-only work.

    [Standard Domain →](standard-domain.md)

- :material-earth: **Spherical Domain**

    `Scene` → cubemap capture → `CubemapTarget` → Domemaster / Equirectangular / Skybox.

    [Spherical Domain →](spherical-domain.md)

</div>

## Capture once, project many, consume many

The cubemap is the shared spherical capture. Requested spherical projections should reuse it whenever the frame requirements permit. Preview and output consumers receive final views; they should not force duplicate scene capture merely because several consumers request the same domain.

!!! info "Architecture is not a prerequisite for creative use"
    Artists can remain at the `Scene`, `RenderMode`, `ViewType`, camera and calibration level. This section exists for contributors, developers and researchers who need the engine contracts.

## Continue through the engine

<div class="grid cards" markdown>

- **Rendering Pipeline** — requirement resolution and final-view reuse. [Open →](rendering-pipeline.md)
- **OpenGL Backend** — Processing/OpenGL boundary and resource behavior. [Open →](opengl-backend.md)
- **Lifecycle** — ownership, invalidation and shutdown. [Open →](runtime-lifecycle.md)
- **Output Backends** — NDI/Syphon/Spout internal boundaries. [Open →](output-backends.md)

</div>
