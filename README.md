# ziviDomeLive Library

**ziviDomeLive** is a Processing library designed to facilitate the creation of immersive visual experiences for fulldome projections, monoscopic VR environments, and interactive installations. The library provides a flexible framework to manage scenes, handle 3D rendering, and integrate external controllers or projection technologies like **Syphon** and **Spout**. Whether you're working on planetarium installations, live audiovisual performances, or interactive media, **ziviDomeLive** offers a robust framework for real-time rendering and scene management.

## Features

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

## Known Issues

**Disclaimer for Apple Silicon Users**:
- **Important**: For users on macOS with Apple Silicon (M family), it is recommended to use the Intel version of Processing to ensure full functionality of **Syphon**. The native ARM version of Processing does not currently support Syphon, which may limit the capabilities of the **ziviDomeLive** library.

**Disclaimer for Linux Users**:
- Due to the absence of a native library for **NDI** in Processing, Linux users will not have access to external integration features, such as those provided by **Syphon** or **Spout** on macOS and Windows.

**OpenGL Error 1282**:
- Some users may encounter the following OpenGL error in the Processing console:
   ```
   OpenGL error 1282 at bot endDraw(): invalid operation
   ```
This error is related to specific OpenGL calls within Processing, but it does not impact the functionality of the **ziviDomeLive** library. Your visuals and performance should remain unaffected, and you can safely ignore this warning.

## Installation

1. Download the **ziviDomeLive** library from the [Processing Contribution Manager] or clone the repository from GitHub.
2. Place the `ziviDomeLive` folder in your Processing `libraries` directory: `Documents/Processing/libraries/`.
3. Ensure that all required dependencies (**ControlP5**, **Syphon**, **Spout**) are installed in your Processing environment.

## Dependencies

The **ziviDomeLive** library requires a few additional libraries to extend its capabilities and provide seamless integration with external tools. Below are the key dependencies you need to install in your Processing environment:

- **[ControlP5](https://github.com/sojamo/controlp5)**:
    - This library is used for creating graphical user interfaces (GUI) within your Processing sketches. It allows you to create buttons, sliders, knobs, and other interactive elements that can be used to control the behavior of your visual scenes.
    - You can install it directly from the Processing Contribution Manager.

- **[Syphon](https://github.com/Syphon/Processing)** (for macOS):
    - Syphon is a technology that enables you to share frames from your Processing sketch with other Syphon-compatible applications on macOS. It’s ideal for live visual performances where you want to send your visuals to other software for further manipulation or projection.
    - Ensure that the **Syphon for Processing** library is installed in your Processing environment.

- **[Spout](https://github.com/leadedge/SpoutProcessing)** (for Windows):
    - Similar to Syphon but designed for Windows, Spout allows you to share frames from your Processing sketch with other Spout-compatible applications. This is useful for integrating your visuals into larger multimedia projects or sending them to other software for real-time manipulation or display.
    - You will need to install the **Spout for Processing** library to enable this feature.

These dependencies must be correctly installed in your Processing environment to ensure that **ziviDomeLive** operates as intended. If any of these libraries are missing, some features like external rendering or user interface controls may not function correctly.

Make sure you have the latest versions of these libraries and place them in your Processing `libraries` folder (`Documents/Processing/libraries/`) to avoid compatibility issues.

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

**Developed by [Victor Valentim](https://victorvalentim.com).**

**Affiliations**:
- CECULT - Universidade Federal do Recôncavo da Bahia
- PPGARTES - Universidade Federal de Minas Gerais

## License

**ziviDomeLive** is distributed under the GPL-2.0 license.

Copyright (c) 2024 Victor Valentim

For more information, visit [Victor Valentim's GitHub page](https://github.com/vicvalentim).
