import com.victorvalentim.zividomelive.*;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;

ziviDomeLive ziviDome;
SceneManager sceneManager;

void settings() {
  pixelDensity(1);
  size(1280, 720, P3D);
}

void setup() {
  ziviDome = new ziviDomeLive(this);
  ziviDome.setup();
  ziviDome.setRenderMode(RenderMode.EQUIRECTANGULAR);

  String environmentPath = sketchPath("../SolarSystem/data/textures/8k_stars_milky_way.jpg");
  PImage environment = loadImage(environmentPath);
  if (environment == null) {
    println("[InfiniteBackground] Could not load environment: " + environmentPath);
  } else {
    ziviDome.setEquirectangularBackground(environment);
    ziviDome.setEnvironmentBackgroundIntensity(1.0);
    println("[InfiniteBackground] Environment loaded: " + environmentPath);
  }

  sceneManager = new SceneManager();
  sceneManager.registerScene(new InfiniteBackgroundScene(ziviDome));
  ziviDome.setSceneManager(sceneManager);
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}
