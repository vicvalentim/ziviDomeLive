import com.victorvalentim.zividomelive.*;
import controlP5.*;
import codeanticode.syphon.*;

// Main instances
ziviDomeLive ziviDome;      // Instance of the ziviDomeLive library
SceneManager sceneManager;  // SceneManager to manage multiple scenes

void settings() {
  pixelDensity(1);  // Library default policy
  size(1200, 800, P3D);  // Set the window size and P3D mode
}

void setup() {
  // Initialize the ziviDomeLive library
  ziviDome = new ziviDomeLive(this);
  ziviDome.setup();  // Initial setup of the library

  // Create and configure the SceneManager
  sceneManager = new SceneManager();
  sceneManager.registerScene(new Scene1(ziviDome, this)); 
  
  // Link the SceneManager to the library
  ziviDome.setSceneManager(sceneManager);
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}
