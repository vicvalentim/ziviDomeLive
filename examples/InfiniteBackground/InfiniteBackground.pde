import com.victorvalentim.zividomelive.*;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;

ziviDomeLive ziviDome;
SceneManager sceneManager;
PImage realEnvironment;
PImage calibrationEnvironment;

void settings() {
  pixelDensity(1);
  fullScreen(P3D, 1);
  //size(1280, 720, P3D);
}

void setup() {
  ziviDome = new ziviDomeLive(this);
  ziviDome.setup();
  ziviDome.setRenderMode(RenderMode.FULL);
  sceneManager = new SceneManager();
  sceneManager.registerScene(new InfiniteBackgroundScene(
    ziviDome, realEnvironment, calibrationEnvironment));
  ziviDome.setSceneManager(sceneManager);
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}
