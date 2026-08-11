import com.victorvalentim.zividomelive.*;
import com.victorvalentim.zividomelive.render.*;
import com.victorvalentim.zividomelive.render.camera.*;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;

// Main instances
zividomelive ziviDome;
SceneManager sceneManager;

void settings() {
  pixelDensity(1);
  size(1280, 720, P3D);
}

void setup() {
  surface.setTitle("ziviDomeLive - Fulldome PBR Example");

  // Optional: enable verbose library logs for debugging.
  //zividomelive.enableDebugLogging();

  // Initialize the library and its render pipeline.
  ziviDome = new zividomelive(this);
  ziviDome.setup();

  // Register the example scene.
  sceneManager = new SceneManager();
  sceneManager.registerScene(new Scene1(ziviDome));
  ziviDome.setSceneManager(sceneManager);
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}
