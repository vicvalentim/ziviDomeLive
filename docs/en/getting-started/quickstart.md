# Quickstart

## Create the Sketch

```java
import com.victorvalentim.zividomelive.*;
import processing.opengl.PGraphicsOpenGL;

zividomelive ziviDome;

void settings() {
  size(1280, 720, P3D);
  pixelDensity(1);
}

void setup() {
  ziviDome = new zividomelive(this);
  ziviDome.setup();
  ziviDome.setScene(new MainScene());
}

void draw() {
  // ziviDomeLive renders automatically through its Processing draw hook.
}
```

Call `setup()` once after construction. Do not call `ziviDome.draw()` from the sketch: the constructor already registered the library's draw hook.

## Implement a Scene

```java
class MainScene implements Scene {
  float angle;

  public void setupScene() {
    angle = 0;
  }

  public void update() {
    angle += 0.01f;
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(8, 12, 24);
    pg.lights();
    pg.rotateY(angle);
    pg.box(180);
    // The library owns beginDraw() and endDraw().
  }

  public String getName() {
    return "Main";
  }
}
```

`sceneRender()` is invoked for every target needed by the current frame. Keep state changes in `update()` so animation advances once per frame rather than once per cubemap face.

## Select a Render Mode

`FULL` is the default. A sketch that never calls `setRenderMode()` keeps the 1.4 routing behavior.

```java
ziviDome.setRenderMode(RenderMode.FULL);
ziviDome.setRenderMode(RenderMode.STANDARD);
ziviDome.setRenderMode(RenderMode.DOMEMASTER);
ziviDome.setRenderMode(RenderMode.EQUIRECTANGULAR);
ziviDome.setRenderMode(RenderMode.SKYBOX);
```

Use `setCurrentView()` for the preview route in `FULL` mode:

```java
ziviDome.setCurrentView(zividomelive.ViewType.FISHEYE_DOMEMASTER);
```

## Receive Events

The library registers Processing keyboard and mouse hooks and forwards events once to the active scene. The internal ControlP5 listener forwards panel events through the same contract.

```java
public void keyEvent(processing.event.KeyEvent event) {
  if (event.getAction() == processing.event.KeyEvent.PRESS) {
    println(event.getKey());
  }
}

public void mouseEvent(processing.event.MouseEvent event) {
  // Handle scene input.
}

public void controlEvent(controlP5.ControlEvent event) {
  // Handle relevant built-in panel events.
}
```

Do not forward these events again from the main sketch.

## Add More Scenes

```java
SceneManager scenes = new SceneManager();
scenes.registerScene(new IntroScene());
scenes.registerScene(new MainScene());
ziviDome.setSceneManager(scenes);
```

The first registration activates that scene. Left and Right arrow keys switch scenes through the library's global shortcuts.

## Route an Output

Outputs are disabled by default:

```java
OutputManager outputs = ziviDome.getOutputManager();
outputs.setNdiView(zividomelive.ViewType.EQUIRECTANGULAR);
outputs.toggleOutput("ndi");
```

Check state and diagnostics without confusing backend readiness with render demand:

```java
println(outputs.getOutputState(OutputManager.OutputType.NDI));
println(outputs.getOutputFailureReason(OutputManager.OutputType.NDI));
```

Native output interoperability still requires platform hardware qualification.
