import com.victorvalentim.zividomelive.*;
import controlP5.*;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;

ziviDomeLive ziviDome;

void settings() {
  size(1280, 720, P3D);
  pixelDensity(1);
}

void setup() {
  ziviDome = new ziviDomeLive(this);
  ziviDome.setup();
  ziviDome.setScene(new NamedActionsScene());
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}

class NamedActionsScene implements Scene {
  private SceneServices services;
  private SceneActionMap actions;
  private SceneCameraService camera;
  private float targetX;
  private float targetZ;
  private float rotation;
  private int paletteIndex;
  private int actionCount;
  private final int[] palette = {
    0xFF4FC3F7,
    0xFFFFB74D,
    0xFF81C784,
    0xFFBA68C8
  };

  public void configure(SceneServices services) {
    this.services = services;
    this.actions = services.actions();
    this.camera = services.camera();
  }

  public void setupScene() {
    camera.setDistanceLimits(-2400f, -280f);
    camera.setCollapseGuard(240f);
    camera.setDragSensitivity(0.01f);
    camera.setLerpFactor(0.18f);
    camera.orbit().setWheelSteps(-80f, -0.001f);
    resetCamera();
    camera.setInputEnabled(true);
    actions.bindKeyPressed("color.next", 'c', this::nextColor);
    actions.bindKeyCodePressed(
      "target.left", java.awt.event.KeyEvent.VK_J, () -> moveBy(-40f, 0f));
    actions.bindKeyCodePressed(
      "target.right", java.awt.event.KeyEvent.VK_L, () -> moveBy(40f, 0f));
    actions.bindKeyCodePressed(
      "target.forward", java.awt.event.KeyEvent.VK_I, () -> moveBy(0f, -40f));
    actions.bindKeyCodePressed(
      "target.back", java.awt.event.KeyEvent.VK_K, () -> moveBy(0f, 40f));
    actions.bindMouse("target.click", MouseEvent.CLICK, this::moveToPointer);
    actions.register("target.center", this::centerTarget);
    actions.bindKeyPressed("target.center.key", '0', () -> actions.trigger("target.center"));
    actions.bindKeyPressed("camera.reset", 'r', this::resetCamera);
  }

  public void update() {
    rotation += 0.012f;
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(12, 18, 32);
    pg.pushMatrix();
    try {
      // Explicit opt-in: applies the camera and a spotlight that follows it toward the target.
      camera.applyWithViewLighting(pg);
      drawGround(pg);
      drawOrbitingMarkers(pg);
      drawTarget(pg);
    } finally {
      pg.popMatrix();
    }
  }

  public String getName() {
    return "Named Actions";
  }

  public void dispose() {
    // Bindings are activation-owned and cleared by the runtime.
    actions = null;
    camera = null;
    services = null;
  }

  private void nextColor() {
    paletteIndex = (paletteIndex + 1) % palette.length;
    actionCount++;
    services.applet().println("[NamedActions] actions=" + actionCount);
  }

  private void moveBy(float dx, float dz) {
    targetX = constrain(targetX + dx, -360f, 360f);
    targetZ = constrain(targetZ + dz, -360f, 360f);
    actionCount++;
  }

  private void moveToPointer(MouseEvent event) {
    PApplet applet = services.applet();
    targetX = map(event.getX(), 0f, applet.width, -360f, 360f);
    targetZ = map(event.getY(), 0f, applet.height, -360f, 360f);
    actionCount++;
  }

  private void centerTarget() {
    targetX = 0f;
    targetZ = 0f;
    actionCount++;
  }

  private void resetCamera() {
    camera.snapToAxisAngle(0f, 0f, 0f, 1f, 0f, 0f, -0.32f, -1100f);
  }

  private void drawGround(PGraphicsOpenGL pg) {
    pg.stroke(86, 118, 150, 120);
    pg.strokeWeight(2f);
    for (int coordinate = -500; coordinate <= 500; coordinate += 100) {
      pg.line(coordinate, 170f, -500f, coordinate, 170f, 500f);
      pg.line(-500f, 170f, coordinate, 500f, 170f, coordinate);
    }
  }

  private void drawOrbitingMarkers(PGraphicsOpenGL pg) {
    pg.noStroke();
    for (int index = 0; index < 8; index++) {
      float angle = TWO_PI * index / 8f + rotation;
      pg.pushMatrix();
      try {
        pg.translate(cos(angle) * 330f, 75f, sin(angle) * 330f);
        pg.rotateX(rotation + index * 0.17f);
        pg.rotateY(rotation * 1.4f + index * 0.11f);
        pg.fill(45, 95 + index * 15, 175 + index * 8);
        pg.specular(180f);
        pg.shininess(24f);
        pg.box(64f, 110f, 64f);
      } finally {
        pg.popMatrix();
      }
    }
  }

  private void drawTarget(PGraphicsOpenGL pg) {
    pg.pushMatrix();
    try {
      pg.translate(targetX, 40f + sin(rotation * 2f) * 18f, targetZ);
      pg.rotateY(rotation * 2f);
      pg.noStroke();
      pg.fill(palette[paletteIndex]);
      pg.specular(255f);
      pg.shininess(48f);
      pg.sphereDetail(24);
      pg.sphere(76f);

      pg.fill(255, 255, 255, 170);
      for (int axis = 0; axis < 3; axis++) {
        pg.pushMatrix();
        try {
          pg.rotateY(TWO_PI * axis / 3f);
          pg.translate(118f, 0f, 0f);
          pg.box(24f);
        } finally {
          pg.popMatrix();
        }
      }
    } finally {
      pg.popMatrix();
    }
  }
}
