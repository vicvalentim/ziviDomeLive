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

  // Initialize the first scene and set it in the library
  currentScene = new Scene1(ziviDome);
  ziviDome.setScene(currentScene);
}

void draw() {
  // Call the draw method of the library for additional rendering
  ziviDome.draw();
}

// Forward key events to the library and switch scenes
void keyPressed() {
  ziviDome.keyPressed();
  if (currentScene != null) {
    currentScene.keyPressed(key);
  }
  switch (key) {
    case 'i':
      currentScene = new Scene1(ziviDome);
      break;
    case 'o':
      currentScene = new Scene2(ziviDome);
      break;
  }
  ziviDome.setCurrentScene(currentScene);
}

// Forward mouse events to the library
void mouseEvent(processing.event.MouseEvent event) {
  ziviDome.mouseEvent(event);
  if (currentScene != null) {
    currentScene.mouseEvent(event.getX(), event.getY(), event.getButton());
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
    pg.ambientLight(128, 128, 128); // Add ambient light
    pg.pushMatrix();
    pg.background(0, 0, 80, 0);
    pg.ambientLight(128, 128, 128); // Add ambient light
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

 
  public void mouseEvent(int mouseX, int mouseY, int button) {
    // Implement mouse event logic, if necessary
    println("Mouse event in Scene1: " + mouseX + ", " + mouseY + ", button: " + button);
  }
}

// Forward control events to the library
void controlEvent(controlP5.ControlEvent theEvent) {
  ziviDome.controlEvent(theEvent);
}

// Implementation of Scene2 that uses the Scene interface
class Scene2 implements Scene {
  zividomelive parent;

  Scene2(zividomelive parent) {
    this.parent = parent;
  }

 
  public void setupScene() {
    // Specific scene settings, if necessary
  }

 
  public void sceneRender(PGraphics pg) {
    pg.pushMatrix();
    pg.background(25, 25, 112);
    pg.pushMatrix();
    pg.fill(255);
    drawLabeledBox(pg, 200); // Draw the cube with labels and mesh
    pg.popMatrix();
    pg.popMatrix();
  }

 
  public void keyPressed(char key) {
    // Implement key response logic, if necessary
  }

 
  public void mouseEvent(int mouseX, int mouseY, int button) {
    // Implement mouse event logic, if necessary
    println("Mouse event in Scene2: " + mouseX + ", " + mouseY + ", button: " + button);
  }

  void drawLabeledBox(PGraphics pg, float size) {
    pg.pushMatrix();
    // Front (+Z)
    pg.pushMatrix();
    pg.translate(0, 0, size / 2);
    drawFaceWithMesh(pg, size, "+Z Front", pg.color(255, 0, 0)); // Red (Primary) // Purple // Vibrant blue
    pg.popMatrix();
    // Back (-Z)
    pg.pushMatrix();
    pg.translate(0, 0, -size / 2);
    pg.rotateY(PI); // Rotate so the text faces outward
    drawFaceWithMesh(pg, size, "-Z Back", pg.color(0, 255, 0)); // Green (Secondary) // Orange // Vibrant green
    pg.popMatrix();
    // Right (+X)
    pg.pushMatrix();
    pg.translate(size / 2, 0, 0);
    pg.rotateY(-HALF_PI); // Rotate so the text faces outward
    drawFaceWithMesh(pg, size, "+X Right", pg.color(0, 0, 255)); // Blue (Primary) // Teal // Vibrant red
    pg.popMatrix();
    // Left (-X)
    pg.pushMatrix();
    pg.translate(-size / 2, 0, 0);
    pg.rotateY(HALF_PI); // Rotate so the text faces outward
    drawFaceWithMesh(pg, size, "-X Left", pg.color(255, 255, 0)); // Yellow (Secondary) // Olive // Vibrant yellow
    pg.popMatrix();
    // Top (+Y)
    pg.pushMatrix();
    pg.translate(0, -size / 2, 0);
    pg.rotateX(-HALF_PI); // Rotate so the text faces outward
    drawFaceWithMesh(pg, size, "+Y Top", pg.color(255, 0, 255)); // Magenta (Secondary) // Indigo // Vibrant magenta
    pg.popMatrix();
    // Bottom (-Y)
    pg.pushMatrix();
    pg.translate(0, size / 2, 0);
    pg.rotateX(HALF_PI); // Rotate so the text faces outward
    drawFaceWithMesh(pg, size, "-Y Bottom", pg.color(0, 255, 255)); // Cyan (Secondary) // Deep Pink // Vibrant cyan
    pg.popMatrix();
    pg.popMatrix();
  }

  void drawFaceWithMesh(PGraphics pg, float size, String label, int faceColor) {
    pg.fill(faceColor);
    pg.beginShape();
    pg.vertex(-size / 2, -size / 2, 0);
    pg.vertex(size / 2, -size / 2, 0);
    pg.vertex(size / 2, size / 2, 0);
    pg.vertex(-size / 2, size / 2, 0);
    pg.endShape(CLOSE);
    pg.stroke(0);
    pg.strokeWeight(1);
    float step = size / 10.0;
    for (float i = -size / 2; i <= size / 2; i += step) {
      pg.line(i, -size / 2, 0, i, size / 2, 0); // Vertical lines
      pg.line(-size / 2, i, 0, size / 2, i, 0); // Horizontal lines
    }
    pg.fill(0);
    pg.textAlign(CENTER, CENTER);
    pg.textSize(30); // Increase font size
    pg.text(label, 0, 0, 0);
  }
}