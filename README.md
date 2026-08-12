[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.15671506.svg)](https://doi.org/10.5281/zenodo.15671506)

# ziviDomeLive 2.0.0

ziviDomeLive is a Processing 4 library for fulldome, monoscopic VR, and immersive installation sketches. It provides scene lifecycle management, independent Standard and spherical rendering, native cubemap capture, domemaster calibration, equirectangular and skybox views, and optional NDI, Syphon, or Spout output routing.

The public manual is available at [vicvalentim.github.io/ziviDomeLive](https://vicvalentim.github.io/ziviDomeLive/). Version 2.0.0 promotes the spherical renderer to a native OpenGL cubemap pipeline. Scenes keep the Processing-facing `sceneRender(PGraphicsOpenGL)` contract, while spherical capture writes directly into a `GL_TEXTURE_CUBE_MAP` and every spherical projection samples it through `samplerCube` shaders. See the [2.0.0 release notes](https://vicvalentim.github.io/ziviDomeLive/release-notes/2.0.0/) for migration and validation notes.

## Requirements

- Processing 4 with the `P3D` renderer
- Java 17 for source builds
- `pixelDensity(1)` for stable cross-display behavior
- ControlP5
- Syphon for Processing on macOS
- Spout for Processing on Windows
- An OpenGL 4.1-capable GPU and driver for the packaged GLSL 4.10 projection shaders

The build targets Processing core `4.5.6` and Devolay `2.2.0-vic.1`. Source builds download pinned ControlP5, Syphon, and Spout JARs with SHA-256 verification when they are missing.

## Rendering Model

The library keeps Standard and spherical rendering as separate domains:

```text
Scene -> StandardRenderer -> Standard target

Scene -> native GL_TEXTURE_CUBE_MAP -> equirectangular
                                    \-> fisheye domemaster
                                    \-> cubemap skybox layout
```

The spherical topology is an internal implementation detail. The stable contracts are the rendered content, cubemap orientation and layout, spherical pitch/yaw/roll behavior, domemaster FOV, and Size% calibration. The current implementation does not allocate six independent Processing face targets.

### RenderMode

`RenderMode.FULL` is the default and preserves the independent preview and output routes configured through `ViewType`. Dedicated modes temporarily override the effective representation without erasing those configured routes.

```java
ziviDome.setRenderMode(RenderMode.FULL);
ziviDome.setRenderMode(RenderMode.STANDARD);
ziviDome.setRenderMode(RenderMode.DOMEMASTER);
ziviDome.setRenderMode(RenderMode.EQUIRECTANGULAR);
ziviDome.setRenderMode(RenderMode.SKYBOX);
```

| Mode | Effective representation |
|---|---|
| `FULL` | Independent preview and output `ViewType` routes |
| `STANDARD` | Perspective Standard renderer |
| `DOMEMASTER` | Fisheye domemaster |
| `EQUIRECTANGULAR` | 2:1 spherical projection |
| `SKYBOX` | Cubemap layout |

The floating domemaster preview is an auxiliary service. It can request the spherical pipeline while the main mode is `STANDARD`.

The built-in panel follows the active mode:

| Mode | Pitch / Yaw / Roll | FOV / Size | Floating domemaster | View selectors |
|---|---|---|---|---|
| `FULL` | Shown | Shown | Shown | Preview and enabled outputs |
| `STANDARD` | With floating domemaster | With floating domemaster | Shown | Hidden |
| `DOMEMASTER` | Shown | Shown | Hidden | Hidden |
| `EQUIRECTANGULAR` / `SKYBOX` | Shown | Hidden | Hidden | Hidden |

Output resolution and publication toggles remain available in every mode. Pitch, yaw, and roll use cyclic `-PI..PI` sliders whose mouse-wheel motion wraps continuously. Their shortest angular deltas are composed directly into one normalized quaternion in event order, so spherical orientation never depends on an Euler reconstruction and does not acquire a gimbal-lock singularity. The values returned by the facade remain control accumulators, not Euler angles extracted from the final attitude.

## Installation

Install the published package through Processing's Contribution Manager when available. For manual installation, use the packaged release artifact rather than the repository source archive:

1. Download `ziviDomeLive.zip` or `ziviDomeLive.pdex` from the matching release.
2. Extract the `ziviDomeLive` folder into the Processing sketchbook `libraries` directory.
3. Install ControlP5 and the platform-local sharing library required by the sketch.
4. Restart Processing and open an example from **File > Examples > Contributed Libraries > ziviDomeLive**.

Cloning the source repository is intended for development. Use `./gradlew buildReleaseArtifacts` to produce the installable package layout, including `library.properties`, examples, generated reference documentation, and release metadata.

## Quickstart

```java
import com.victorvalentim.zividomelive.*;
import com.victorvalentim.zividomelive.manager.OutputManager;
import processing.opengl.PGraphicsOpenGL;
// Processing contributed-library runtime dependencies:
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;

ziviDomeLive ziviDome;

void settings() {
  size(1280, 720, P3D);
  pixelDensity(1);
}

void setup() {
  ziviDome = new ziviDomeLive(this);
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
    // The library owns beginDraw() and endDraw().
  }
}
```

The constructor registers Processing lifecycle and input hooks automatically. Call `setup()` once, but do not call `ziviDome.draw()` or forward keyboard, mouse, or ControlP5 events from the sketch.

## Scene Contract

`SceneManager` is the authority for the active scene. The first registered scene is activated automatically; scene changes dispose the leaving scene and call `setupScene()` on the arriving scene.

```java
SceneManager scenes = new SceneManager();
scenes.registerScene(new IntroScene());
scenes.registerScene(new LiveScene());
ziviDome.setSceneManager(scenes);
```

A scene may implement:

- `setupScene()` when it becomes active
- `update()` once before rendering each frame
- `sceneRender(PGraphicsOpenGL pg)` for drawing only
- `keyEvent()`, `mouseEvent()`, and `controlEvent()` for forwarded input
- `dispose()` to release scene-owned resources
- `getName()` for diagnostics

Never call `beginDraw()` or `endDraw()` inside `sceneRender()`.

## Environment Background

Use a library-owned equirectangular background when a sky, star field, or LDR environment map should appear in spherical render modes without drawing a giant sphere inside the scene:

```java
PImage stars = loadImage("textures/8k_stars_milky_way.jpg");
ziviDome.setEquirectangularBackground(stars);
ziviDome.setEnvironmentBackgroundIntensity(1.0f);
ziviDome.setEnvironmentBackgroundYawOffset(0.0f);
```

The current environment pass is LDR (`PImage`) and is rendered as an infinite far-depth background after `sceneRender()`. This survives scene-owned `background()` calls while keeping foreground geometry in front. Domemaster, equirectangular, and skybox projections then sample the same cubemap as usual. HDR textures, IBL prefiltering, and AO remain future rendering stages.

## Spherical Calibration

- FOV range: `0..360`, default `210`
- Domemaster Size% range: `0..100`, default `100`
- Pitch, yaw, and roll are shared by every spherical representation
- Output resolution presets: `1024`, `2048`, `3072`, `4096`

The ranges above are the supported panel/calibration domain. Programmatic callers should keep FOV and Size% inside those ranges. Use `resetOrientation()` for only the quaternion attitude, or `resetControls()` after manager initialization to restore orientation, FOV, and Size% together.

The Standard preview follows the Processing window. Spherical previews use an automatic square resolution:

```text
min(1024, max(256, min(windowWidth, windowHeight)))
```

Changing output resolution is deferred to the draw loop and does not redefine preview resolution.

## External Outputs

All external outputs are opt-in. Syphon and Spout remain GPU-native and receive `PGraphicsOpenGL` directly. NDI is the GPU-to-CPU boundary and uses three frame slots, bounded queues, a dedicated sender worker, and latest-frame-wins backpressure.

```java
OutputManager outputs = ziviDome.getOutputManager();
outputs.setNdiView(ViewType.EQUIRECTANGULAR);
outputs.toggleOutput("ndi");

println(outputs.getOutputState(OutputManager.OutputType.NDI));
println(outputs.getOutputFailureReason(OutputManager.OutputType.NDI));
println(outputs.getNdiCapturedFrames());
println(outputs.getNdiSentFrames());
println(outputs.getNdiDroppedFrames());
println(outputs.getNdiFailedFrames());
```

Backend availability, native initialization, publication, and render requirements are distinct. `OutputManager.OutputState` reports `UNAVAILABLE`, `AVAILABLE`, `INITIALIZED`, `ENABLED`, or `STOPPING`. An explicit toggle retries a backend whose previous initialization failed.

### Platform Matrix

| Capability | macOS | Windows | Linux |
|---|---|---|---|
| Core rendering and previews | Supported | Supported | Supported |
| NDI | Requires compatible Devolay natives and receiver qualification | Requires compatible Devolay natives and receiver qualification | Reduced/unqualified native support |
| Syphon | Supported platform; Intel/Rosetta may be required on Apple Silicon | Not available | Not available |
| Spout | Not available | Supported platform | Not available |

Automated tests do not replace GPU, receiver, or native-sharing qualification. See [the 2.0 release-readiness protocol](https://vicvalentim.github.io/ziviDomeLive/qualification/2.0-release-readiness/).

## Built-in Controls

- `h`: show or hide the ControlP5 panel
- `m`: cycle the configured preview `ViewType`
- Left/Right arrows: switch scenes

The panel groups global status, spherical calibration, view selection, and output controls. Per-output routing is independently editable in `FULL`; dedicated modes preserve those stored routes while hiding selectors that cannot affect the active representation.

## Logging

Release logging is the default. Enable diagnostics before creating the instance:

```java
ziviDomeLive.enableDebugLogging();
// Equivalent: ziviDomeLive.setLogMode(LogManager.Mode.DEBUG);
```

Return to release mode with `ziviDomeLive.enableReleaseLogging()`.

## Examples

See the [examples catalog](examples/README.md) for controls and lifecycle conventions.

- `Basic`: scene switching and `RenderMode` keys `1..5`
- `EmptyProject`: intentionally empty one-scene starter template
- `CalibrationTool`: two-scene GLSL focus/color chart and 360-degree spherical reference
- `FulldomePBR`: retained geometry, PBR shaders, and scene-space orbit camera
- `SolarSystem`: larger multi-file application
- `SphereParticle`: threaded scene simulation

## Build and Verification

```bash
./gradlew buildReleaseArtifacts
./gradlew qualificationTests
mkdocs build --strict
```

Release output is written to `release/ziviDomeLive.zip`, `release/ziviDomeLive.pdex`, and `release/ziviDomeLive.txt`. The package includes project and bundled-dependency notices, while test sources and local compile-only helper JARs are excluded from both the Processing package and sketchbook deployment. Automated qualification results are written under `build/reports/qualification/` and `build/test-results/qualification/`. GPU and native-output checks remain manual and must use real hardware; no golden images are fabricated by the automated suite.

Before submitting a release to the Processing Contribution Manager, review the [Processing Publication checklist](https://vicvalentim.github.io/ziviDomeLive/qualification/processing-publication/) and confirm that the ZIP, PDEX, TXT metadata file, examples, `reference/index.html`, source repository, and license notices are all present.

## Known Issues

- Some Processing/JOGL hardware and driver combinations still emit OpenGL error `1282` (`GL_INVALID_OPERATION`). It is usually non-fatal but remains under investigation.
- Native Syphon on Apple Silicon may require the Intel Processing build under Rosetta 2.
- Linux has reduced external-output support.

## License and Citation

ziviDomeLive is distributed under the [GPL-2.0-only license](LICENSE). Bundled components and calibration assets are documented in [THIRD_PARTY.md](THIRD_PARTY.md). Citation metadata is provided in [CITATION.cff](CITATION.cff), with DOI [10.5281/zenodo.15671506](https://doi.org/10.5281/zenodo.15671506).

Copyright (c) 2024 Victor Valentim.
