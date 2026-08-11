import com.victorvalentim.zividomelive.*;
// Processing adds contributed libraries to the runtime classpath through imports.
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;
import processing.event.KeyEvent;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;

zividomelive ziviDome;

void settings() {
  size(1280, 720, P3D);
  pixelDensity(1);
}

void setup() {
  surface.setTitle("ziviDomeLive - Calibration Test");

  ziviDome = new zividomelive(this);
  ziviDome.setup();
  ziviDome.setRenderMode(RenderMode.FULL);
  ziviDome.setScene(new CalibrationScene(ziviDome));

  resetCalibrationState();
}

void draw() {
  // ziviDomeLive registers its own draw hook. This sketch intentionally does
  // not call ziviDome.draw(), so it can catch duplicate-render regressions.
}

void resetCalibrationState() {
  ziviDome.setCurrentView(zividomelive.ViewType.FISHEYE_DOMEMASTER);
  ziviDome.setFishSize(100f);
  ziviDome.setFov(210f);
  ziviDome.setPitch(0f);
  ziviDome.setYaw(0f);
  ziviDome.setRoll(0f);
  ziviDome.setShowPreview(true);
}
