import com.victorvalentim.zividomelive.*;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;

// Main instances
zividomelive ziviDome;  // Instance of the zividomelive library
Scene currentScene;     // Current scene implementing the Scene interface

void settings() {
  size(1280, 720, P3D);  // Set the window size and P3D mode
}

void setup() {
  // Initialize the zividomelive library
  ziviDome = new zividomelive(this);
  ziviDome.setup();  // Initial setup of the library

  // Initialize the scene and set it in the library
  currentScene = new Scene1(ziviDome);
  ziviDome.setScene(currentScene);
}

void draw() {
  // Call the draw method of the library for additional rendering
  ziviDome.draw();
}

// Forward key events to the library
void keyPressed() {
  ziviDome.keyPressed();
  // Optionally call keyPressed of the current scene
  if (currentScene != null) {
    currentScene.keyPressed(key);
  }
}

// Forward mouse events to the library
void mouseEvent(processing.event.MouseEvent event) {
  ziviDome.mouseEvent(event);
  // Optionally call mouseEvent of the current scene
  if (currentScene != null) {
    currentScene.mouseEvent(event);
  }
}

// Forward control events to the library
void controlEvent(controlP5.ControlEvent theEvent) {
  ziviDome.controlEvent(theEvent);
}

// Implementation of Scene1 that uses the Scene interface
class Scene1 implements Scene {
  zividomelive parent;

  Scene1(zividomelive parent) {
    this.parent = parent;
  }


  public void setupScene() {
    // Specific scene setup, if necessary
  }

 
  public void sceneRender(PGraphics pg) {
    // Scene rendering logic
  }

 
  public void keyPressed(char key) {
    // Key response logic
  }

 
  public void mouseEvent(MouseEvent event) {
    // Mouse event logic
  }
}
