<div align="center">

<img src="docs/assets/png/logo.png" alt="ziviDomeLive logo" width="180">

# ziviDomeLive

**Open-source Processing library for real-time immersive audiovisual creation, fulldome projection, spherical rendering, and creative coding.**

[![Latest Release](https://img.shields.io/github/v/release/vicvalentim/ziviDomeLive?display_name=tag&label=version)](https://github.com/vicvalentim/ziviDomeLive/releases/latest)
[![Processing](https://img.shields.io/badge/Processing-tested%204.5.6-006699)](https://processing.org/)
[![Java](https://img.shields.io/badge/Java-17-007396)](https://github.com/vicvalentim/ziviDomeLive)
[![Documentation](https://img.shields.io/badge/docs-MkDocs-526CFE)](https://vicvalentim.github.io/ziviDomeLive/)
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.15671506.svg)](https://doi.org/10.5281/zenodo.15671506)
[![License](https://img.shields.io/badge/license-GPL--2.0--only-blue.svg)](LICENSE)
[![Sponsor](https://img.shields.io/badge/Sponsor-GitHub%20Sponsors-EA4AAA?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/vicvalentim)

[Documentation](https://vicvalentim.github.io/ziviDomeLive/) ·
[Latest release](https://github.com/vicvalentim/ziviDomeLive/releases/latest) ·
[Examples](examples/) ·
[Contributing](https://vicvalentim.github.io/ziviDomeLive/en/contributing/) ·
[DOI](https://doi.org/10.5281/zenodo.15671506) ·
[GitHub Sponsors](https://github.com/sponsors/vicvalentim)

</div>

---

## Overview

**ziviDomeLive** is a Processing 4 library designed to facilitate the creation of immersive visual and audiovisual experiences for fulldome projection, live performance, interactive installations, and other spherical-display workflows.

The library provides a flexible framework for scene management, real-time 3D rendering, projection conversion and calibration, interactive control, and external video routing. It supports independent Standard and spherical rendering domains, fisheye domemaster, equirectangular and cubemap representations, dynamic output resolution, and optional NDI, Syphon, or Spout publication.

ziviDomeLive is intended for artists, creative coders, researchers, educators, students, developers, planetarium practitioners, VJs, and other users working with real-time immersive media.

>[!IMPORTANT]
> **Current stable release: 1.5.0 — August 11, 2026.**
>
> Version 1.5.0 is the final consolidation of the 1.x architecture. It preserves the established public facade while adding explicit `RenderMode` control, stronger lifecycle and routing contracts, quaternion-based spherical orientation, output diagnostics, qualification tooling, and expanded documentation. Experimental 2.0 rendering work is not part of the 1.5.0 public contract.

**Multiple Projection Modes**:
- **ziviDomeLive** supports a wide range of projection modes including fisheye domemaster, equirectangular, cubemap, and more. These projection modes are ideal for fulldome displays, virtual reality setups, and immersive environments, allowing you to create visuals that wrap around the viewer or adapt to spherical displays.

**Resolution Switching for Domemaster**:
- The library includes a mode that allows you to switch between 1k, 2k, 3k, and 4k resolutions for domemaster projection. This flexibility ensures that your visuals look sharp and detailed, regardless of the scale of your dome or display system. You can optimize performance based on the hardware capabilities and the specific requirements of your project.

**Scene Management**:
- Easily manage and switch between different scenes using the **Scene** interface. This feature allows for modular visual compositions where you can define multiple scenes and toggle between them dynamically. Each scene can have its own setup, rendering logic, and user interactions, making it versatile for both interactive installations and performances.

**Real-time Rendering**:
- **ziviDomeLive** is optimized for live visual performances and real-time applications. It handles frame-by-frame rendering, ensuring smooth performance even with complex 3D scenes and shader effects. This makes it perfect for VJs, live coding performances, and interactive art installations.

**External Integration**:
- Seamlessly integrate with other applications using **Syphon** (for macOS) or **Spout** (for Windows). With these integrations, you can share rendered frames from your Processing sketches to other software in real-time. This is particularly useful for multimedia performances, where your visuals can be further processed or projected using different tools.

**Interactive UI**:
- The library integrates with **ControlP5**, a Processing library for creating graphical user interfaces (GUIs). This allows you to build interactive controls directly into your Processing sketches, such as sliders, buttons, and toggle switches, which can be used to manipulate various parameters of your visuals in real-time.

**Cross-Platform Compatibility**:
- **ziviDomeLive** works across multiple operating systems, including macOS, Windows, and Linux, making it highly versatile and accessible to a wide range of users. This ensures that your visual creations can be deployed on various platforms without compatibility issues.

**Customizable Rendering Pipelines**:
- Define and customize rendering pipelines to meet the needs of your project. Whether you are rendering for fulldome projection or interactive experiences, the library allows you to adjust the rendering resolution, projection mode, and other parameters to optimize performance and visual quality.

## Research and Artistic Context

ziviDomeLive is developed as open-source research software and as a technical-artistic research artifact at the intersection of **creative coding, immersive media, fulldome, real-time audiovisual systems, artistic research, and education**.

The project treats the immersive rendering environment as a programmable space for artistic experimentation. Its development combines software engineering, computational art, projection systems, live audiovisual practice, and research-creation workflows.

The project is developed by **[Victor Valentim](https://victorvalentim.com/)**.

**Affiliations documented by the project:**

- CECULT/UFRB — Federal University of Reconcavo da Bahia
- PPGARTES/UFMG — Federal University of Minas Gerais
- ORCID: [0000-0002-0282-7947](https://orcid.org/0000-0002-0282-7947)

For academic and research use, see [Citation](#citation).

## Features

### Multiple Rendering and Projection Modes

ziviDomeLive supports multiple real-time representations through the public `RenderMode` API:

| Mode | Representation |
| --- | --- |
| `FULL` | Independent preview and output routes through `ViewType` |
| `STANDARD` | Perspective Standard renderer |
| `DOMEMASTER` | Fisheye domemaster |
| `EQUIRECTANGULAR` | 2:1 spherical projection |
| `SKYBOX` | Cubemap layout |

`RenderMode.FULL` is the default compatibility mode.

### Fulldome and Spherical Rendering

The spherical domain supports:

- fisheye domemaster rendering;
- equirectangular output;
- cubemap / skybox output;
- shared pitch, yaw, and roll orientation controls;
- fisheye field-of-view calibration;
- domemaster Size% calibration;
- independent preview and output resolution policies.

### Resolution Switching

Output resolution presets are available at:

- `1024 × 1024`
- `2048 × 2048`
- `3072 × 3072`
- `4096 × 4096`

The Standard preview follows the Processing window, while spherical preview resolution is calculated automatically and remains independent from output resolution.

### Scene Management

The `Scene` interface and `SceneManager` support modular real-time compositions with:

- scene setup;
- per-frame updates;
- `PGraphicsOpenGL` rendering;
- deterministic scene switching;
- keyboard, mouse, and ControlP5 event forwarding;
- scene disposal and resource cleanup.

### Real-Time Rendering

ziviDomeLive is designed for live visual performance and interactive applications. The library manages rendering lifecycle and frame routing while preserving a Processing-oriented scene workflow.

Typical use cases include:

- fulldome performances;
- planetarium installations;
- immersive audiovisual works;
- VJ and live-coding environments;
- interactive media installations;
- monoscopic VR and spherical visualization;
- research and teaching in art and technology.

### External Video Integration

Optional output backends include:

| Backend | Platform | Path |
| --- | --- | --- |
| Syphon | macOS | GPU-native texture sharing |
| Spout | Windows | GPU-native texture sharing |
| NDI | macOS / Windows / experimental Linux | GPU-to-CPU network video boundary |

NDI support uses the bundled Devolay Java/JNI dependency but requires a separately installed NDI Runtime. NDI in ziviDomeLive 1.5.0 is an experimental, unofficial, video-only integration.

### Interactive UI

The built-in ControlP5 panel exposes runtime controls for:

- render mode;
- preview and output routing;
- pitch, yaw, and roll;
- domemaster FOV;
- domemaster Size%;
- output resolution;
- output publication state.

### Cross-Platform Architecture

Core Standard and spherical rendering are supported on:

- macOS
- Windows
- Linux

External-sharing capabilities remain platform-specific. See the [system requirements](https://vicvalentim.github.io/ziviDomeLive/en/installation/requirements/) and [known issues](https://vicvalentim.github.io/ziviDomeLive/en/known-issues/) before production deployment.

## Known Issues

>[!IMPORTANT]
>**Disclaimer for Apple Silicon Users**:
>>For users on macOS with Apple Silicon processors (M series), it is recommended to use the Intel version of Processing (run via Rosetta 2) to ensure full **Syphon** functionality. The native ARM version of Processing currently lacks Syphon support, which may limit the real-time video-sharing capabilities of the **ziviDomeLive** library.
>
>**Disclaimer for Linux Users**:
>>Due to the absence of a native library for **NDI** in Processing, Linux users will not have access to external integration features, such as those provided by **Syphon** or **Spout** on macOS and Windows.
>

>[!WARNING]
>**OpenGL Error 1282**:
> Some users may encounter the following OpenGL error in the Processing console:
>   ```
>   OpenGL error 1282 at bot endDraw(): invalid operation
>   ```
>This error is related to specific OpenGL calls within Processing, but it does not impact the functionality of the **ziviDomeLive** library. Your visuals and performance should remain unaffected, and you can safely ignore this warning.
>

### Qualification and Diagnostics

Version 1.5.0 includes explicit release and runtime diagnostics:

- automated API, state, lifecycle, routing, math, metadata, and packaging tests;
- `qualificationTests` Gradle task;
- CalibrationTool visual qualification workflow;
- observable external-output lifecycle states;
- NDI captured, sent, dropped, and failed-frame telemetry;
- documented GPU and native-output qualification procedures.

## Rendering Model

ziviDomeLive 1.5.0 keeps Standard and spherical rendering as separate domains:

```text
STANDARD
Scene -> StandardRenderer -> Standard target

SPHERICAL
Scene -> six cubemap faces -> equirectangular -> fisheye domemaster
                          \-> cubemap / skybox layout
```

The 1.x spherical topology is an internal implementation detail. Stable public contracts concern rendered content, projection behavior, calibration, scene lifecycle, routing, and the public API rather than a permanent backend implementation.

## Requirements

| Requirement | Current 1.5.0 target |
| --- | --- |
| Processing | Processing 4 |
| Tested Processing version | 4.5.6 |
| Renderer | `P3D` |
| Java for source builds | Java 17 |
| Pixel density | `pixelDensity(1)` recommended |
| GPU | OpenGL 4.1-capable GPU/driver |
| Packaged shaders | GLSL 4.10 |
| Platforms tested | macOS, Windows, Linux |

A dedicated GPU is recommended for 3K/4K spherical rendering and shader-heavy scenes.

## Dependencies

### Processing libraries

| Dependency | Purpose | Platform |
| --- | --- | --- |
| ControlP5 `2.2.6` | Built-in control panel | All |
| Syphon for Processing `4.0` | GPU texture sharing | macOS |
| Spout for Processing `2.0.8.0` | GPU texture sharing | Windows |

Install Processing dependencies through the Contribution Manager where available.

### Bundled Java dependency

ziviDomeLive 1.5.0 includes Devolay `2.2.0-vic.1` for experimental NDI output. The proprietary NDI Runtime is **not bundled** and must be installed separately.

See the [NDI Runtime documentation](https://vicvalentim.github.io/ziviDomeLive/en/installation/ndi/) before enabling NDI.

## Installation

### Processing Contribution Manager

When ziviDomeLive is available through Processing's Contribution Manager:

1. Open Processing.
2. Go to **Sketch → Import Library… → Manage Libraries…**
3. Search for **ziviDomeLive**.
4. Install the library.
5. Open an example from **File → Examples → Contributed Libraries → ziviDomeLive**.

### Manual installation

Download the latest release artifact:

- [ziviDomeLive.zip](https://github.com/vicvalentim/ziviDomeLive/releases/latest/download/ziviDomeLive.zip)
- [ziviDomeLive.pdex](https://github.com/vicvalentim/ziviDomeLive/releases/latest/download/ziviDomeLive.pdex)
- [ziviDomeLive.txt](https://github.com/vicvalentim/ziviDomeLive/releases/latest/download/ziviDomeLive.txt)

For manual ZIP installation:

1. Extract the `ziviDomeLive` folder.
2. Move it to the Processing sketchbook `libraries` directory.
3. Install the required Processing dependencies.
4. Restart Processing.

Cloning this repository is intended for development rather than normal installation.

## Quickstart

```java
import com.victorvalentim.zividomelive.*;

// Processing contributed-library runtime dependencies:
import controlP5.*;
import codeanticode.syphon.*; // Apple
import spout.*; // Windows

zividomelive ziviDome;

void settings() {
  size(1280, 720, P3D);
  pixelDensity(1);
}

void setup() {
  ziviDome = new zividomelive(this);
  ziviDome.setup();
  ziviDome.setScene(new Scene1());
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}

class Scene1 implements Scene {
  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(8, 12, 24);
    pg.lights();
    pg.box(180);
  }
}
```

The library owns the Processing rendering lifecycle around the scene target. Do not call `beginDraw()` or `endDraw()` inside `sceneRender()`, and do not manually forward lifecycle or input events already registered by ziviDomeLive.

## Scene Contract

A scene may implement:

- `setupScene()` when it becomes active;
- `update()` once before rendering each frame;
- `sceneRender(PGraphicsOpenGL pg)` for drawing;
- `keyEvent()`, `mouseEvent()`, and `controlEvent()` for input;
- `dispose()` to release scene-owned resources;
- `getName()` for diagnostics.

`SceneManager` is the authority for the active scene.

## Spherical Calibration

The supported 1.5.0 calibration domain is:

| Parameter | Range / values |
| --- | --- |
| Pitch | cyclic `-PI .. PI` |
| Yaw | cyclic `-PI .. PI` |
| Roll | cyclic `-PI .. PI` |
| Domemaster FOV | `0 .. 360`, default `210` |
| Domemaster Size% | `0 .. 100`, default `100` |
| Output resolution | `1024`, `2048`, `3072`, `4096` |

Pitch, yaw, and roll are shared across spherical representations and compose incrementally into a normalized quaternion.

## External Outputs

External outputs are opt-in.

```java
import com.victorvalentim.zividomelive.manager.OutputManager;

OutputManager outputs = ziviDome.getOutputManager();

outputs.setNdiView(zividomelive.ViewType.EQUIRECTANGULAR);
outputs.toggleOutput("ndi");

println(outputs.getOutputState(OutputManager.OutputType.NDI));
println(outputs.getOutputFailureReason(OutputManager.OutputType.NDI));
```

Syphon and Spout remain GPU-native. NDI is the explicit GPU-to-CPU boundary and uses bounded buffering and latest-frame-wins backpressure.

## Built-in Controls

Default controls include:

- `h` — show or hide the ControlP5 panel;
- `m` — cycle the configured legacy preview `ViewType`;
- Left / Right arrows — switch scenes.

The panel adapts to the active `RenderMode`.

## Examples

The repository includes examples ranging from minimal templates to larger immersive scenes:

| Example | Purpose |
| --- | --- |
| `Basic` | Scene switching and `RenderMode` keys `1..5` |
| `EmptyProject` | Minimal one-scene starter template |
| `CalibrationTool` | GLSL focus/color target and 360-degree spherical calibration |
| `FulldomePBR` | Retained geometry, PBR shaders, and scene-space orbit camera |
| `SolarSystem` | Larger multi-file application |
| `SphereParticle` | Threaded scene simulation |

Browse the [examples directory](examples/) or the [examples documentation](https://vicvalentim.github.io/ziviDomeLive/en/examples/basic/).

## Documentation

Full documentation is available at:

**https://vicvalentim.github.io/ziviDomeLive/**

The documentation includes:

- installation and dependencies;
- quickstart;
- render modes;
- control panel;
- spherical calibration;
- scene management;
- external integration;
- rendering architecture;
- API reference;
- examples;
- qualification procedures;
- release notes;
- contributing guidance;
- known issues;
- author and license information.

Processing library releases also include the generated reference documentation required by the Processing library distribution structure.

## Processing Contribution Library Metadata

ziviDomeLive follows the Processing contributed-library packaging model.

| Metadata | Value |
| --- | --- |
| Name | `ziviDomeLive` |
| Current version | `1.5.0` |
| Processing version tested | `4.5.6` |
| Categories | `3D`, `Video & Vision` |
| Platforms | macOS, Windows, Linux |
| License | GPL-2.0-only |
| Last stable release | August 11, 2026 |
| DOI | `10.5281/zenodo.15671506` |

**Keywords:** fulldome, projection, immersive media, creative coding, real-time graphics, NDI, Syphon, Spout, Processing, OpenGL, computational art, planetarium, live video.

The release workflow produces the three matching Processing distribution artifacts:

```text
ziviDomeLive.zip
ziviDomeLive.txt
ziviDomeLive.pdex
```

The ZIP distribution contains the compiled library, examples, source, metadata, notices, and reference material required by the Processing library packaging workflow.

## Build and Verification

For development:

```bash
./gradlew clean qualificationTests
./gradlew build -x test
./gradlew buildReleaseArtifacts
mkdocs build --strict
```

Release artifacts are written to:

```text
release/ziviDomeLive.zip
release/ziviDomeLive.pdex
release/ziviDomeLive.txt
```

GPU rendering and native output interoperability must also be qualified manually on target hardware.

## Known Issues

- Some Processing/JOGL hardware and driver combinations may emit OpenGL error `1282` (`GL_INVALID_OPERATION`). The issue is generally non-fatal but remains under investigation.
- Native Syphon on Apple Silicon may require the Intel build of Processing under Rosetta 2.
- Linux has reduced external-output support relative to macOS and Windows.
- NDI output is experimental and requires qualification of the exact runtime, network, sender, receiver, operating system, and frame format.

See the complete [Known Issues](https://vicvalentim.github.io/ziviDomeLive/en/known-issues/) documentation.

## Contributing

Contributions are welcome in the form of bug reports, documentation improvements, tests, examples, and code.

Before submitting code:

```bash
./gradlew clean qualificationTests
./gradlew build -x test
./gradlew buildReleaseArtifacts
mkdocs build --strict
```

Please preserve the documented 1.x public contracts and update tests, Javadocs, bilingual documentation, and the changelog when public behavior changes.

Read the complete [Contributing Guide](https://vicvalentim.github.io/ziviDomeLive/en/contributing/) before opening a pull request.

## Citation

If ziviDomeLive contributes to academic research, artistic research, teaching, software studies, publications, artworks, or technical reports, please cite the software.

**DOI:** [10.5281/zenodo.15671506](https://doi.org/10.5281/zenodo.15671506)

Citation metadata is maintained in [`CITATION.cff`](CITATION.cff), allowing GitHub and compatible reference managers to expose structured software citation information.

## Author

**Victor Valentim**  
Researcher, artist-programmer, professor, musician, and developer.

- Website: [victorvalentim.com](https://victorvalentim.com/)
- GitHub: [@vicvalentim](https://github.com/vicvalentim)
- ORCID: [0000-0002-0282-7947](https://orcid.org/0000-0002-0282-7947)

**Affiliations documented by the project:**

- CECULT/UFRB — Federal University of Reconcavo da Bahia (2024 - Present)
- PPGARTES/UFMG — Federal University of Minas Gerais (2024 - 2025)

## Support

ziviDomeLive is developed and maintained as open-source software for creative coding, immersive media, artistic research, and education.

If the project is useful to your work, you can support its continued development through **[GitHub Sponsors](https://github.com/sponsors/vicvalentim)**.

Sponsorship contributes to development time, cross-platform testing, documentation, examples, dependency maintenance, qualification, and the long-term sustainability of the project.

The library, documentation, releases, and public development remain openly available regardless of sponsorship.

## License

ziviDomeLive is distributed under the **[GPL-2.0-only](LICENSE)** license.

Bundled components, third-party software, and calibration assets are documented in [`THIRD_PARTY.md`](THIRD_PARTY.md).

Copyright (c) 2024-2026 Victor Valentim.
