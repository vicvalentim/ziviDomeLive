# Quickstart

This path intentionally avoids Scene Services, benchmark tooling, output internals, threading and renderer architecture.

## 1. Imports

```java
import com.victorvalentim.zividomelive.*;
import processing.opengl.PGraphicsOpenGL;

// Processing contributed-library runtime dependencies:
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;
```

The Syphon/Spout imports are package-runtime dependencies used by the contributed-library distribution. You do not need to configure or learn those output systems to create a basic scene.

## 2. Create ziviDomeLive

```java
ziviDomeLive dome;

void settings() {
  size(1280, 720, P3D);
  pixelDensity(1);
}
```

## 3. Setup

```java
void setup() {
  dome = new ziviDomeLive(this);
  dome.setup();
  dome.setScene(new MainScene());
}
```

The constructor registers the Processing hooks used by the library. Do not manually forward draw/input hooks unless a documented API explicitly asks you to.

## 4. Create a Scene

```java
class MainScene implements Scene {
  float angle;

  public void update() {
    angle += 0.01f;
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(8, 12, 24);
    pg.lights();
    pg.translate(pg.width * 0.5f, pg.height * 0.5f);
    pg.rotateY(angle);
    pg.box(180);
  }
}
```

## 5. `update()` = state

Use `update()` for state that must advance **once per Processing frame**:

- animation counters;
- physics/simulation;
- timelines;
- mutable randomization;
- state transitions.

## 6. `sceneRender()` = drawing

Use `sceneRender(PGraphicsOpenGL)` only to draw the current state.

!!! important
    Spherical capture may call `sceneRender()` more than once during one Processing frame. If animation/state is advanced inside `sceneRender()`, the six spherical directions can observe different states.

The library already owns `beginDraw()` and `endDraw()` for the target passed to the scene. Do not call them inside `sceneRender()`.

## 7. Change RenderMode

```java
dome.setRenderMode(RenderMode.STANDARD);
dome.setRenderMode(RenderMode.DOMEMASTER);
dome.setRenderMode(RenderMode.EQUIRECTANGULAR);
dome.setRenderMode(RenderMode.SKYBOX);
dome.setRenderMode(RenderMode.FULL);
```

`FULL` is the default working mode for independently routed preview/output views.

## 8. Test Domemaster

Start with:

```java
dome.setRenderMode(RenderMode.DOMEMASTER);
```

Then open the Spherical Calibration page before using projector/lens output. FOV, Size% and Pitch/Yaw/Roll are calibration controls; they are not substitutes for moving the scene camera.
