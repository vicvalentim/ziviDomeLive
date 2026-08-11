import com.victorvalentim.zividomelive.*;
// Processing adds contributed libraries to the runtime classpath through imports.
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;
import processing.core.PImage;
import processing.event.KeyEvent;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;

ziviDomeLive ziviDome;
SceneManager sceneManager;

void settings() {
  size(1280, 720, P3D);
  pixelDensity(1);
}

void setup() {
  surface.setTitle("ziviDomeLive - Calibration Tool");

  ziviDome = new ziviDomeLive(this);
  ziviDome.setup();
  ziviDome.setRenderMode(RenderMode.FULL);

  sceneManager = new SceneManager();
  sceneManager.registerScene(new CubeCalibrationScene(ziviDome));
  sceneManager.registerScene(new BourkeSphereScene(ziviDome));
  ziviDome.setSceneManager(sceneManager);

  resetCalibrationState();
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}

boolean handleCalibrationKey(KeyEvent event) {
  if (event.getAction() != KeyEvent.PRESS) {
    return false;
  }

  char pressed = Character.toLowerCase(event.getKey());
  switch (pressed) {
    case '1': ziviDome.setCurrentView(ziviDomeLive.ViewType.FISHEYE_DOMEMASTER); break;
    case '2': ziviDome.setCurrentView(ziviDomeLive.ViewType.EQUIRECTANGULAR); break;
    case '3': ziviDome.setCurrentView(ziviDomeLive.ViewType.CUBEMAP); break;
    case '4': ziviDome.setCurrentView(ziviDomeLive.ViewType.STANDARD); break;
    case '[': ziviDome.setFishSize(max(0f, ziviDome.getFishSize() - 10f)); break;
    case ']': ziviDome.setFishSize(min(100f, ziviDome.getFishSize() + 10f)); break;
    case '-': ziviDome.setFov(max(0f, ziviDome.getFov() - 10f)); break;
    case '=':
    case '+': ziviDome.setFov(min(360f, ziviDome.getFov() + 10f)); break;
    case 'p': ziviDome.setPitch(ziviDome.getPitch() + HALF_PI); break;
    case 'y': ziviDome.setYaw(ziviDome.getYaw() + HALF_PI); break;
    case 'r': ziviDome.setRoll(ziviDome.getRoll() + HALF_PI); break;
    case 'f': ziviDome.setShowPreview(!ziviDome.isShowPreview()); break;
    case '0': resetCalibrationState(); break;
    default: return false;
  }

  printCalibrationState();
  return true;
}

void resetCalibrationState() {
  ziviDome.setCurrentView(ziviDomeLive.ViewType.FISHEYE_DOMEMASTER);
  ziviDome.setFishSize(100f);
  ziviDome.setFov(210f);
  ziviDome.setPitch(0f);
  ziviDome.setYaw(0f);
  ziviDome.setRoll(0f);
  ziviDome.setShowPreview(true);
}

void printCalibrationState() {
  Scene activeScene = sceneManager != null ? sceneManager.getCurrentScene() : null;
  println(
      "[CalibrationTool] scene=" + (activeScene != null ? activeScene.getName() : "none")
      + " view=" + ziviDome.getCurrentView()
      + " fov=" + nf(ziviDome.getFov(), 0, 1)
      + " size=" + nf(ziviDome.getFishSize(), 0, 1)
      + " pitch=" + nf(degrees(ziviDome.getPitch()), 0, 1)
      + " yaw=" + nf(degrees(ziviDome.getYaw()), 0, 1)
      + " roll=" + nf(degrees(ziviDome.getRoll()), 0, 1)
      + " floatingPreview=" + ziviDome.isShowPreview()
      + " outputEnabled=" + ziviDome.isEnableOutput()
      + " outputResolution=" + ziviDome.getOutputResolution());
}
