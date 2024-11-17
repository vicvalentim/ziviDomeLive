import com.victorvalentim.zividomelive.*;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;

// Main instances
zividomelive ziviDome;      // Instance of the zividomelive library
SceneManager sceneManager;  // SceneManager to manage multiple scenes

void settings() {
  size(1280, 720, P3D);  // Set the window size and P3D mode
}

void setup() {
  // Initialize the zividomelive library
  ziviDome = new zividomelive(this);
  ziviDome.setup();  // Initial setup of the library

  // Create and configure the SceneManager
  sceneManager = new SceneManager();
  sceneManager.registerScene(new Scene1(ziviDome)); // Register Scene1
  sceneManager.registerScene(new Scene2(ziviDome)); // Register Scene2

  // Link the SceneManager to the library
  ziviDome.setSceneManager(sceneManager);

  // Set the initial scene in the zividomelive library
  ziviDome.setScene(sceneManager.getCurrentScene());
}

void draw() {
  // Call the draw method of the library
  ziviDome.draw();
}

// Scene1 implementation
class Scene1 implements Scene {
  private zividomelive parent;
  private float rotationSpeed = 0.01f; // Speed of rotation for the animation
  private float radius = 700; // Distance of the pillars from the center
  private float time = 0; // Tracks elapsed time for animation
  private int numPillars = 8; // Number of pillars in the scene
  private int[] colors = {
      0xFFFF0000, // Red
      0xFF00FF00, // Green
      0xFF0000FF, // Blue
      0xFFFFFF00, // Yellow
      0xFFFF00FF, // Magenta
      0xFF00FFFF, // Cyan
      0xFFFFFFFF, // White
      0xFFFF8000  // Orange
  };

  Scene1(zividomelive parent) {
      this.parent = parent;
  }

  public void setupScene() {
      println("Scene1 setup completed.");
  }

  public void update() {
      time += rotationSpeed; // Increment time for animation
  }

  public void sceneRender(PGraphicsOpenGL pg) {
      pg.pushMatrix();
      pg.background(0, 0, 80, 0); // Clear the background
      float angleStep = TWO_PI / numPillars;

      for (int i = 0; i < numPillars; i++) {
          float angle = angleStep * i + time; // Add rotation animation
          float x = sin(angle) * radius;
          float y = cos(angle) * radius;
          pg.pushMatrix();
          pg.translate(x, y, 0);
          pg.rotateX(time); // Add rotation on the X axis
          pg.fill(colors[i % colors.length]);
          pg.box(200); // Render a box for each pillar
          pg.popMatrix();
      }

      pg.popMatrix();
  }

  public void keyEvent(processing.event.KeyEvent event) {
      if (event.getAction() == processing.event.KeyEvent.PRESS) { // Only handle key press events
          char key = event.getKey();
          println("Key pressed in Scene1: " + key);

          switch (key) {
              case '+':
                  rotationSpeed += 0.01f; // Increase speed
                  break;
              case '-':
                  rotationSpeed = max(0.01f, rotationSpeed - 0.01f); // Decrease speed
                  break;
              case 'r':
                  rotationSpeed = 0.02f; // Reset to default speed
                  break;
              default:
                  println("Unhandled key in Scene1.");
          }
      }
  }

  public void mouseEvent(MouseEvent event) {
      if (event.getAction() == MouseEvent.WHEEL) {
          float e = event.getCount();
          radius = max(100, radius + e * 10); // Zoom in or out by changing the radius
      }
  }

  public void controlEvent(controlP5.ControlEvent theEvent) {
      println("Control event in Scene1: " + theEvent.getName());
  }

  public String getName() {
      return "Scene1";
  }
}

// Scene2 implementation (Example for additional scene)
class Scene2 implements Scene {
  private zividomelive parent;

  Scene2(zividomelive parent) {
      this.parent = parent;
  }

  public void setupScene() {
      println("Scene2 setup completed.");
  }

  public void update() {
      // Optional update logic
  }

  public void sceneRender(PGraphicsOpenGL pg) {
      pg.background(50, 50, 150); // Set a different background
      pg.fill(255);
      pg.textSize(36);
      pg.textAlign(CENTER, CENTER);
      pg.text("Welcome to Scene2", pg.width / 2f, pg.height / 2f);
  }

  public void keyEvent(processing.event.KeyEvent event) {
      println("Key pressed in Scene2.");
  }

  public void mouseEvent(MouseEvent event) {
      println("Mouse event in Scene2.");
  }

  public void controlEvent(controlP5.ControlEvent theEvent) {
      println("Control event in Scene2: " + theEvent.getName());
  }

  public String getName() {
      return "Scene2";
  }
}
