---
title: ziviDomeLive
icon: material/home-outline
description: Create real-time fulldome, spherical and immersive visual workflows in Processing.
---

<div class="zd-hero" markdown>
<div markdown>

<div class="zd-hero__eyebrow">Processing library · ziviDomeLive 2.0</div>

# Create for the dome, the sphere and the live image

Build real-time **fulldome, spherical and immersive visual workflows** from one Processing scene, then choose how each preview or output should be represented.

<div class="zd-actions" markdown>
[Start creating](getting-started/quickstart.md){ .md-button .md-button--primary }
[Explore the API](api/artist-api-map.md){ .md-button }
</div>

</div>
<div class="zd-hero__image">
<div class="zd-splash-stage" data-zd-splash>
<canvas class="zd-splash-canvas" data-zd-splash-canvas width="566" height="480" role="img" aria-label="Animated ziviDomeLive splash sphere with orbiting wireframe cubes">Animated ziviDomeLive splash sphere</canvas>
</div>
</div>
</div>

## What can I create?

<div class="grid cards" markdown>

- :material-monitor: **Standard**

    Conventional perspective rendering for the Processing window and standard visual outputs.

- :material-panorama-fisheye: **Domemaster**

    Circular fisheye representation for fulldome projection and dome calibration.

- :material-earth: **Equirectangular**

    2:1 spherical representation for 360° image workflows.

- :material-cube-outline: **Skybox**

    Cubemap-layout representation for inspection and compatible spherical workflows.

</div>

!!! tip "Start with one Scene"
    A basic project needs the `ziviDomeLive` runtime and a `Scene`. Put state/simulation in `update()` and drawing in `sceneRender()`.

## Release & compatibility

ziviDomeLive is a Processing 4 library for real-time Standard, fulldome and spherical visual workflows, including Domemaster, Equirectangular and Skybox representations, scene management, spherical calibration and optional NDI, Syphon and Spout output routing.

| Item | Current information |
|---|---|
| Release | **2.0.0** |
| Last update | **24 August 2026** |
| Processing compatibility | Processing 4, revision `1285+` |
| Automated test version | **Processing 4.5.6** |
| Physically qualified systems | macOS Apple Silicon `arm64`; macOS Intel `x86_64`; Windows `x86_64`; Linux `x86_64` |
| Categories | `3D`, `Video & Vision` |
| Keywords | `fulldome`, `projection`, `immersive`, `spherical`, `360`, `visual`, `real-time`, `NDI`, `Syphon`, `Spout` |
| Required Processing dependency | ControlP5 `2.2.6` for the distributed examples and control panel |
| Optional output dependencies | NDI Runtime; Syphon for Processing `4.0` on macOS; Spout for Processing `2.0.8.0` on Windows |

**Distributed examples and tools:** `EmptyProject`, `Basic`, `NamedActions`, `PortLoopback`, `SphereParticle`, `InfiniteBackground`, `FulldomePBR`, `SolarSystem`, `CalibrationTool`, and `BenchmarkTool`.

[Download ziviDomeLive.zip](https://github.com/vicvalentim/ziviDomeLive/releases/latest/download/ziviDomeLive.zip){ .md-button .md-button--primary }
[Installation details](installation/installation-steps.md){ .md-button }
[Dependencies](installation/dependencies.md){ .md-button }

The release qualification record covers the four desktop configurations above, all ten distributed examples and tools, Standard and spherical rendering, calibration, Processing package installation and platform-applicable native outputs. Qualify the exact GPU, driver, projector, lens and receiver chain before production use.

## Choose your path

<div class="grid cards" markdown>

- :material-rocket-launch-outline: **New to ziviDomeLive**

    Install the library, run the Quickstart and continue through the eight learning examples.

    [Open the Quickstart →](getting-started/quickstart.md)

- :material-palette-outline: **Building an artwork or installation**

    Learn RenderMode, Preview × Output, spherical calibration, camera/navigation and external outputs.

    [Open the Creative Guide →](usage/basic-usage.md)

- :material-api: **Programming against the library**

    Use the Artist API Map first, then generated Javadocs for exact signatures.

    [Open the API Map →](api/artist-api-map.md)

- :material-source-branch: **Contributing or researching the engine**

    Study the Standard/Spherical domains, OpenGL backend, lifecycle, threading and output boundaries.

    [Open the architecture →](architecture/overview.md)

</div>

## Calibration belongs to the output, not to scene zoom

Pitch/Yaw/Roll orient the shared spherical domain. Domemaster additionally uses FOV and Size% to fit the representation to a physical projection system.

[Open Spherical Calibration](usage/spherical-calibration.md){ .md-button }

??? abstract "Under the hood"
    Version 2.0 captures the spherical domain through a native cubemap and derives Domemaster, Equirectangular and Skybox from that shared representation. This implementation detail is documented for developers; artists can remain at the `Scene`, `RenderMode`, `ViewType` and calibration level.
