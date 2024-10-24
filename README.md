# ziviDomeLive Library

**ziviDomeLive** is a Processing library designed to facilitate the creation of immersive visual experiences for fulldome projections, VR environments, and interactive installations. The library provides a flexible framework to manage scenes, handle 3D rendering, and integrate external controllers or projection technologies like **Syphon** and **Spout**. Whether you're working on planetarium installations, live audiovisual performances, or interactive media, **ziviDomeLive** offers a robust framework for real-time rendering and scene management.

## Features

- **Multiple Projection Modes**: Supports fisheye domemaster, equirectangular, cubemap, and more for fulldome projection.
- **Scene Management**: Implement and switch between scenes using the Scene interface.
- **Real-time Rendering**: Optimized for live performances and real-time applications.
- **External Integration**: Supports **Syphon** (macOS) and **Spout** (Windows) for sharing frames with other applications.
- **Interactive UI**: Integrates with **ControlP5** to create user interfaces within your Processing sketches.

## Installation

1. Download the **ziviDomeLive** library from the [Processing Contribution Manager] or clone the repository from GitHub.
2. Place the `ziviDomeLive` folder in your Processing `libraries` directory: `Documents/Processing/libraries/`.
3. Ensure that all required dependencies (**ControlP5**, **Syphon**, **Spout**) are installed in your Processing environment.

## Import Libraries

```java
import com.victorvalentim.zividomelive.*;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;
```

## Usage

To use **ziviDomeLive** in your Processing sketch, import the library at the beginning of your code:

```java
zividomelive ziviDome;  // Instance of the ziviDomeLive library
Scene currentScene;     // The current scene implementing the Scene interface

void settings() {
  size(1280, 720, P3D);  // Set window size and 3D rendering mode
}

void setup() {
  ziviDome = new zividomelive(this);
  ziviDome.setup();  // Initialize the library

  // Initialize a scene and set it in the library
  currentScene = new Scene1(ziviDome);
  ziviDome.setScene(currentScene);
}

void draw() {
  ziviDome.draw();  // Handle rendering
}
```
## Implementing Scenes

A Scene is an interface that allows you to define different visual environments. Below is an example of how to implement a custom scene:

```java
class Scene1 implements Scene {
  zividomelive parent;

  Scene1(zividomelive parent) {
    this.parent = parent;
  }

  @Override
  public void setupScene() {
    println("Scene1 setup complete.");
  }

  @Override
  public void sceneRender(PGraphics pg) {
    pg.background(0, 0, 80);  // Set the background color
    pg.pushMatrix();
    // Custom 3D rendering logic
    pg.popMatrix();
  }

  @Override
  public void keyPressed(char key) {
    println("Key pressed in Scene1: " + key);
  }
}
```
## Event Handling

**ziviDomeLive** allows you to forward Processing events, such as key presses and mouse events, to your scene or the library itself:

```java
void keyPressed() {
  ziviDome.keyPressed();
  if (currentScene != null) {
    currentScene.keyPressed(key);
  }
}

void mouseEvent(processing.event.MouseEvent event) {
  ziviDome.mouseEvent(event);
}

void controlEvent(controlP5.ControlEvent theEvent) {
  ziviDome.controlEvent(theEvent);
}
```
## Contributing Development

We welcome contributions to the development of **ziviDomeLive**! Whether it's bug fixes, new features, or improvements to documentation, your help is greatly appreciated.

### Steps to Contribute:

1. **Fork the Repository**:

    - Go to the [ziviDomeLive GitHub repository](https://github.com/vicvalentim/zividomelive) and click the **Fork** button to create your own copy of the repository.


2. **Clone Your Fork**:

    - Clone the forked repository to your local machine by running:
      ```bash
      git clone https://github.com/vicvalentim/zividomelive.git
      ```

3. **Create a New Branch**:

    - Create a branch to work on your changes:
      ```bash
      git checkout -b your-branch-name
      ```

4. **Make Changes**:

    - Implement your changes in the codebase.
    - Test your changes locally to ensure they work as expected.


5. **Push Your Changes**:
    - After committing your changes, push them to your forked repository:
      ```bash
      git push origin your-branch-name
      ```

6. **Create a Pull Request**:
    - Go to the original repository and open a pull request (PR) from your fork. Provide a clear explanation of your changes and why they should be merged.

Thank you for contributing to **ziviDomeLive**!

## Author

**Developed by Victor Valentim.**

For more information, visit [Victor Valentim's GitHub page](https://github.com/vicvalentim).



