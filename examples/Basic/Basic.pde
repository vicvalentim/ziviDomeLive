import com.victorvalentim.zividomelive.*;
// Processing adds contributed libraries to the runtime classpath through imports.
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;

zividomelive ziviDome;
SceneManager sceneManager;

void settings() {
  size(1280, 720, P3D);
  pixelDensity(1);
}

void setup() {
  surface.setTitle("ziviDomeLive - Basic");
  // Uncomment while diagnosing a sketch: zividomelive.enableDebugLogging();

  ziviDome = new zividomelive(this);
  ziviDome.setup();
  ziviDome.setRenderMode(RenderMode.FULL);

  sceneManager = new SceneManager();
  sceneManager.registerScene(new Scene1(ziviDome));
  sceneManager.registerScene(new Scene2(ziviDome));
  ziviDome.setSceneManager(sceneManager);
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}
