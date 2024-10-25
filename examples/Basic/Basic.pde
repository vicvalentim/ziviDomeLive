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
    println("Scene1 setup completed.");
  }

  public void sceneRender(PGraphics pg) {
    pg.pushMatrix();
    pg.background(0, 0, 80, 0);
    float radius = 700; // Distance from the center
    int numPillars = 8;
    float angleStep = TWO_PI / numPillars;
    int[] colors = {#FF0000, #00FF00, #0000FF, #FFFF00, #FF00FF, #00FFFF, #FFFFFF, #FF8000};
    float time = millis() / 2000.0; // Time in seconds for animation
    for (int i = 0; i < numPillars; i++) {
      float angle = angleStep * i + time; // Add rotation animation
      float x = sin(angle) * radius;
      float y = cos(angle) * radius;
      pg.pushMatrix();
      pg.translate(x, y, 0);
      pg.rotateX(time); // Add rotation on the X axis
      pg.fill(colors[i]);
      pg.box(200); // Adjust parameters as needed to change the size
      pg.popMatrix();
    }
    pg.popMatrix();
  }

  public void keyPressed(char key) {
    // Implement key response logic, if necessary
    println("Key pressed in Scene1: " + key);
  }

  public void mouseEvent(MouseEvent event) {
    // Implement mouse event logic, if necessary
    println("Mouse event in Scene1: " + event.getX() + ", " + event.getY() + ", button: " + event.getButton());
  }
}