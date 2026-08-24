---
title: Quickstart
icon: material/rocket-launch-outline
description: Build a first ziviDomeLive Scene and test Domemaster without learning renderer internals.
---

# Quickstart

Create a first working scene, keep animation state coherent and switch to Domemaster. This path intentionally avoids Scene Services, benchmark tooling, output internals, threading and renderer architecture.

!!! info "The one contract to remember"
    `update()` advances **state/simulation once per Processing frame**. `sceneRender()` draws the current state and may run more than once during spherical capture.

## 1. Imports

```java
import com.victorvalentim.zividomelive.*;
import controlP5.*;
import processing.opengl.PGraphicsOpenGL;
```

ControlP5 is a required external Processing library and every distributed example imports it. Install it explicitly through the Contribution Manager; ziviDomeLive does not claim transitive installation through `library.properties`. Syphon and Spout remain optional platform integrations and must not be imported by sketches that do not use their APIs directly.

## 2. Create the runtime

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
  dome = new ziviDomeLive(this); // (1)!
  dome.setup();
  dome.setScene(new MainScene()); // (2)!
}
```

1. Associates the runtime with the current Processing sketch and its lifecycle hooks.
2. Makes `MainScene` the active scene.

## 4. Create a Scene

```java
class MainScene implements Scene {
  float angle;

  public void update() {
    angle += 0.01f; // (1)!
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

1. State advances here so every spherical render pass observes the same frame state.

## 5. Understand `update()`

Use `update()` for state that must advance **once per Processing frame**: animation counters, physics/simulation, timelines, mutable randomization and state transitions.

## 6. Understand `sceneRender()`

Use `sceneRender(PGraphicsOpenGL)` only to draw the current state.

!!! warning "Spherical capture can render the Scene repeatedly"
    `sceneRender()` may be called more than once during one Processing frame. Advancing animation inside it can make different spherical directions observe different states.

The library already owns `beginDraw()` and `endDraw()` for the target passed to the scene. Do not call them inside `sceneRender()`.

## 7. Change RenderMode

```java
// Try one at a time:
dome.setRenderMode(RenderMode.STANDARD);
dome.setRenderMode(RenderMode.DOMEMASTER);
dome.setRenderMode(RenderMode.EQUIRECTANGULAR);
dome.setRenderMode(RenderMode.SKYBOX);
dome.setRenderMode(RenderMode.FULL);
```

`FULL` is the default working mode for independently routed preview/output views.

## 8. Test Domemaster

```java
dome.setRenderMode(RenderMode.DOMEMASTER);
```

Then continue to [Spherical Calibration](../usage/spherical-calibration.md). FOV, Size% and Pitch/Yaw/Roll are calibration controls; they are not substitutes for moving the Scene camera.

<div class="zd-actions" markdown>
[Render modes](../usage/basic-usage.md){ .md-button .md-button--primary }
[Learning examples](../examples/basic.md){ .md-button }
</div>
