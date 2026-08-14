import com.victorvalentim.zividomelive.*;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;

// Main instances
ziviDomeLive ziviDome;      // Instance of the ziviDomeLive library
SceneManager sceneManager;  // SceneManager to manage multiple scenes

void settings() {
  pixelDensity(1);  // Library default policy
  size(1280, 720, P3D);  // Set the window size and P3D mode
}

void setup() {
  // Uncomment while diagnosing a sketch:
  // ziviDomeLive.enableDebugLogging();

  // Initialize the ziviDomeLive library
  ziviDome = new ziviDomeLive(this);
  ziviDome.setup();  // Initial setup of the library
  ziviDome.setRenderMode(RenderMode.FULL);  // Compatibility default; outputs may route independently

  // Create and configure the SceneManager
  sceneManager = new SceneManager();
  sceneManager.registerScene(new Scene1(ziviDome)); // Register Scene1
  sceneManager.registerScene(new Scene2(ziviDome)); // Register Scene2

  // Link the SceneManager to the library
  ziviDome.setSceneManager(sceneManager);
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}
