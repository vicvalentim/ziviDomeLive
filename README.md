# ziviDomeLive 2.0.0

ziviDomeLive is a Processing 4 library for real-time fulldome, spherical and immersive visual workflows. It lets a Processing sketch render the same scene as Standard perspective, Domemaster, Equirectangular or Skybox representations and route those views to preview and optional live outputs.

## What you can create

- fulldome and planetarium visuals;
- spherical and 360° visual workflows;
- real-time immersive installations;
- creative-coding sketches with multiple scenes;
- live outputs through NDI and platform-local Syphon/Spout where available and qualified.

![ziviDomeLive overview](docs/img/hero-overview.png)

## Requirements

- Processing 4 with the `P3D` renderer;
- `pixelDensity(1)` for deterministic render-target dimensions;
- the dependencies distributed/required by the Processing package;
- an OpenGL-capable system compatible with the packaged projection shaders.

Platform support and platform qualification are different. Consult the technical documentation and release evidence before treating any external-output backend as tested on a specific system.

## Quick example

```java
import com.victorvalentim.zividomelive.*;
import processing.opengl.PGraphicsOpenGL;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;

ziviDomeLive dome;

void settings() {
  size(1280, 720, P3D);
  pixelDensity(1);
}

void setup() {
  dome = new ziviDomeLive(this);
  dome.setup();
  dome.setScene(new MainScene());
  dome.setRenderMode(RenderMode.DOMEMASTER);
}

class MainScene implements Scene {
  public void update() {
    // Update mutable state once per Processing frame.
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(8, 12, 24);
    pg.lights();
    pg.box(180);
  }
}
```

The library owns `beginDraw()` and `endDraw()` for the `PGraphicsOpenGL` target passed to `sceneRender()`.

## Representations

- `STANDARD` — perspective rendering;
- `DOMEMASTER` — fisheye fulldome representation;
- `EQUIRECTANGULAR` — 2:1 spherical representation;
- `SKYBOX` — cubemap layout.

`RenderMode.FULL` keeps preview and external-output `ViewType` routes independent. Dedicated modes temporarily override the effective representation without erasing those stored routes.

## Installation

Use the Processing Contribution Manager when the package is published there. For a manual installation, use the release package (`ziviDomeLive.zip` or `.pdex`) rather than the repository source archive and place the resulting `ziviDomeLive` folder in the Processing sketchbook `libraries` directory.

## Examples

Learning examples:

1. `EmptyProject`
2. `Basic`
3. `SphereParticle`
4. `InfiniteBackground`
5. `FulldomePBR`
6. `SolarSystem`

Qualification tools:

- `CalibrationTool`
- `BenchmarkTool`

## Documentation

The MkDocs site is the official technical documentation for the current software version. Generated Javadocs are the signature-level API reference. A future GitBook may provide a separate didactic/editorial publication; it is not required to install, use or publish the Processing library.

## Citation

Software citation metadata is provided in `CITATION.cff`. The repository currently records the software DOI `10.5281/zenodo.15671506`; release maintainers must verify the registered record before tagging or changing it.

## License

GPL-2.0-only. See `LICENSE` and `THIRD_PARTY.md` for project and bundled-component notices.
