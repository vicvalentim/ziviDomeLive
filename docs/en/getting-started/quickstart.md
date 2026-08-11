
# Quickstart Guide for ziviDomeLive

Congratulations on installing **ziviDomeLive**! If you haven’t completed the installation yet, please refer to the [Installation Steps](../installation/installation-steps.md) to set everything up. Once installed, you’re ready to start creating immersive visuals directly in Processing.

---

## Step 1: Setting Up Your Sketch

To begin, open Processing and create a new sketch. Setting up **ziviDomeLive** is simple and allows you to quickly explore its core functionalities.

First, import **ziviDomeLive** and any essential dependencies at the start of your sketch. This ensures that all library functionalities are accessible and ready for use.

```java
import com.victorvalentim.zividomelive.*;
import processing.opengl.PGraphicsOpenGL;
```

Next, initialize **ziviDomeLive** by creating an instance of the library. This instance will be the foundation of your immersive environment, facilitating the management and rendering of scenes.

```java
zividomelive ziviDome; // Declares a ziviDome variable of type zividomelive. This variable will be used to instantiate and control the ziviDomeLive library.
Scene currentScene; // Declares a currentScene variable of type Scene. This variable will store the current scene being rendered and interacting with ziviDomeLive.
```

In the `settings()` and `setup()` functions, define the screen dimensions and the 3D rendering mode. Then, call the **ziviDomeLive** setup function to initialize it properly. With these steps, the environment is ready, and **ziviDomeLive** is prepared to manage your visuals.

```java
void settings() {
size(1280, 720, P3D); // Sets the window size and enables 3D rendering mode (P3D)
}

void setup() {
ziviDome = new zividomelive(this); // Creates a new instance of ziviDomeLive, passing the reference of the current sketch

    ziviDome.setup(); // Configures ziviDomeLive, initializing its variables and preparing it for rendering

    currentScene = new Scene1(ziviDome); // Creates the scene associated with ziviDomeLive

    ziviDome.setScene(currentScene); // Sets currentScene as the active scene within ziviDomeLive
}
```

Completing this step, the environment is ready, and **ziviDomeLive** is prepared to manage your visuals.

---

## Step 2: Letting the Library Render

The `zividomelive` constructor registers its own Processing `draw` hook. Do not call `ziviDome.draw()` from the sketch because that renders the pipeline twice per frame. The sketch can keep an empty `draw()` function for its own future logic:

```java
void draw() {
    // ziviDomeLive renders automatically.
}
```
___

## Step 3: Enabling Basic Interaction Controls

The library also registers Processing keyboard and mouse hooks and forwards each event to the active scene. Its built-in ControlP5 panel forwards control events through the same `Scene` contract. Implement only the callbacks your scene needs; do not forward them again from the main sketch.

The available scene callbacks are:

1. **Keyboard Input**:
   `keyEvent()` receives Processing key events after the library handles its global shortcuts.

2. **Mouse Events**:
   `mouseEvent()` receives clicks, movement, dragging, and wheel events.

3. **Control Events**:
   `controlEvent()` receives events from the library's built-in ControlP5 panel.

```java
public void keyEvent(processing.event.KeyEvent event) {
    if (event.getAction() == processing.event.KeyEvent.PRESS) {
        println("Key pressed: " + event.getKey());
    }
}

public void mouseEvent(processing.event.MouseEvent event) {
    // Handle scene mouse input.
}

public void controlEvent(controlP5.ControlEvent event) {
    // Handle built-in panel events relevant to this scene.
}
```
___

## Step 4: Creating a Basic Scene Class

The core of **ziviDomeLive** revolves around scenes, which allow you to organize different visual components and easily switch between them.

To begin, create a basic scene class by implementing the **Scene** interface. Define the initial scene setup, including background colors, shapes, or 3D objects you want to display. In the main content of the scene, use the `sceneRender()` function to define what should be drawn in each frame.

```java
class Scene1 implements Scene {
    zividomelive parent;

    Scene1(zividomelive parent) {
        this.parent = parent;
    }

    @Override
    public void setupScene() {
        // Optional one-time scene setup.
    }

    @Override
    public void sceneRender(PGraphicsOpenGL pg) {
        pg.background(0);
        pg.box(200);
        // The library owns beginDraw() and endDraw().
    }
}
```

After defining the scene class, set it as the active scene in **ziviDomeLive** by assigning it in the `setup()` function. This allows **ziviDomeLive** to manage rendering and any interaction events, like key presses, directly in your scene.

---

## Step 5: Running and Interacting with the Sketch

After setting up and assigning your scene, you’re ready to run the sketch. Simply click the Run button in Processing and watch **ziviDomeLive** bring your scene to life.

With the sketch running, you can interact using keyboard inputs or other Processing events. Since **ziviDomeLive** supports interactive functionality, you can easily add controls, experiment with dynamic visuals, or adjust parameters in real-time.

___

## General Summary

These 5 steps form the essential foundation for using the **ziviDomeLive** library in Processing, enabling immersive visualization and interface control. With this setup, ziviDomeLive is ready to manage scenes and interactions, offering complete support for immersive visual experiences.

___

## What’s Next?

Now that you’ve set up a basic scene, feel free to explore additional features. Try adding new scenes, integrating with external tools like **Syphon** or **Spout** for real-time sharing, or setting up custom user interfaces with **ControlP5**. **ziviDomeLive** provides a flexible framework for experimenting and creating dynamic visual experiences that respond to your interaction.
