import com.victorvalentim.zividomelive.*;
import processing.opengl.PGraphicsOpenGL;

zividomelive ziviDome;
ReferenceScene referenceScene;

void settings() {
  size(1280, 720, P3D);
  pixelDensity(1);
}

void setup() {
  ziviDome = new zividomelive(this);
  ziviDome.setup();

  referenceScene = new ReferenceScene(ziviDome);
  ziviDome.setScene(referenceScene);

  ziviDome.setCurrentView(zividomelive.ViewType.FISHEYE_DOMEMASTER);
  ziviDome.setShowPreview(true);
}

void draw() {
  // ziviDomeLive registers its own draw hook. This sketch intentionally does
  // not call ziviDome.draw(), so it can catch duplicate-render regressions.
}

void keyPressed() {
  if (key == '1') ziviDome.setCurrentView(zividomelive.ViewType.FISHEYE_DOMEMASTER);
  if (key == '2') ziviDome.setCurrentView(zividomelive.ViewType.EQUIRECTANGULAR);
  if (key == '3') ziviDome.setCurrentView(zividomelive.ViewType.CUBEMAP);
  if (key == '4') ziviDome.setCurrentView(zividomelive.ViewType.STANDARD);

  if (key == '[') ziviDome.setFishSize(max(0, ziviDome.getFishSize() - 10));
  if (key == ']') ziviDome.setFishSize(min(100, ziviDome.getFishSize() + 10));

  if (key == '-') ziviDome.setFov(max(0, ziviDome.getFov() - 10));
  if (key == '=') ziviDome.setFov(min(360, ziviDome.getFov() + 10));

  if (key == 'p') ziviDome.setPitch(ziviDome.getPitch() + HALF_PI / 2);
  if (key == 'y') ziviDome.setYaw(ziviDome.getYaw() + HALF_PI / 2);
  if (key == 'r') ziviDome.setRoll(ziviDome.getRoll() + HALF_PI / 2);

  if (key == 'f') ziviDome.setShowPreview(!ziviDome.isShowPreview());
}
