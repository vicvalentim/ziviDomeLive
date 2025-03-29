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

  // Link the SceneManager to the library
  ziviDome.setSceneManager(sceneManager);

  // Set the initial scene in the zividomelive library
  ziviDome.setScene(sceneManager.getCurrentScene());
}
void draw() {
  // Call the draw method of the library for additional rendering
  ziviDome.draw();
}
