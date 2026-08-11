class CalibrationScene implements Scene {
  private static final float FACE_DISTANCE = 900f;
  private static final float TARGET_SIZE = 1740f;
  private static final int GRID_DIVISIONS = 12;

  private final zividomelive dome;
  private PShader colorReferenceShader;
  private boolean shaderFailureReported;

  CalibrationScene(zividomelive dome) {
    this.dome = dome;
  }

  @Override
  public void setupScene() {
    try {
      colorReferenceShader = dome.getPApplet().loadShader(
          "calibration-colors.frag",
          "calibration-colors.vert");
      println("[CalibrationTest] GLSL color reference loaded.");
    } catch (RuntimeException error) {
      colorReferenceShader = null;
      reportShaderFailure(error);
    }
  }

  @Override
  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(0);
    pg.noLights();
    pg.colorMode(RGB, 255);
    pg.blendMode(BLEND);

    drawFace(pg, 0, "+X", "RIGHT", pg.color(235, 55, 55),
        FACE_DISTANCE, 0, 0, 0, HALF_PI, 0);
    drawFace(pg, 1, "-X", "LEFT", pg.color(125, 20, 20),
        -FACE_DISTANCE, 0, 0, 0, -HALF_PI, 0);
    drawFace(pg, 2, "+Y", "DOWN", pg.color(45, 210, 80),
        0, FACE_DISTANCE, 0, -HALF_PI, 0, 0);
    drawFace(pg, 3, "-Y", "UP", pg.color(20, 115, 45),
        0, -FACE_DISTANCE, 0, HALF_PI, 0, 0);
    drawFace(pg, 4, "+Z", "FRONT", pg.color(60, 125, 245),
        0, 0, FACE_DISTANCE, 0, 0, 0);
    drawFace(pg, 5, "-Z", "BACK", pg.color(35, 45, 145),
        0, 0, -FACE_DISTANCE, 0, PI, 0);
  }

  public void keyEvent(KeyEvent event) {
    if (event.getAction() != KeyEvent.PRESS) {
      return;
    }

    char pressed = Character.toLowerCase(event.getKey());
    switch (pressed) {
      case '1': dome.setCurrentView(zividomelive.ViewType.FISHEYE_DOMEMASTER); break;
      case '2': dome.setCurrentView(zividomelive.ViewType.EQUIRECTANGULAR); break;
      case '3': dome.setCurrentView(zividomelive.ViewType.CUBEMAP); break;
      case '4': dome.setCurrentView(zividomelive.ViewType.STANDARD); break;
      case '[': dome.setFishSize(max(0f, dome.getFishSize() - 10f)); break;
      case ']': dome.setFishSize(min(100f, dome.getFishSize() + 10f)); break;
      case '-': dome.setFov(max(0f, dome.getFov() - 10f)); break;
      case '=':
      case '+': dome.setFov(min(360f, dome.getFov() + 10f)); break;
      case 'p': dome.setPitch(dome.getPitch() + HALF_PI); break;
      case 'y': dome.setYaw(dome.getYaw() + HALF_PI); break;
      case 'r': dome.setRoll(dome.getRoll() + HALF_PI); break;
      case 'f': dome.setShowPreview(!dome.isShowPreview()); break;
      case '0': resetCalibrationState(); break;
      default: return;
    }

    printCalibrationState();
  }

  private void drawFace(
      PGraphicsOpenGL pg,
      int index,
      String axis,
      String direction,
      int accent,
      float x,
      float y,
      float z,
      float rotateX,
      float rotateY,
      float rotateZ) {
    pg.pushMatrix();
    pg.translate(x, y, z);
    pg.rotateX(rotateX);
    pg.rotateY(rotateY);
    pg.rotateZ(rotateZ);
    drawCalibrationTarget(pg, index, axis, direction, accent);
    pg.popMatrix();
  }

  private void drawCalibrationTarget(
      PGraphicsOpenGL pg,
      int index,
      String axis,
      String direction,
      int accent) {
    float half = TARGET_SIZE / 2f;

    pg.pushStyle();
    pg.rectMode(CENTER);
    pg.ellipseMode(CENTER);
    pg.textAlign(CENTER, CENTER);

    if (!drawShaderColorPattern(pg)) {
      drawFallbackColorPattern(pg, half);
    }

    // All overlays sit slightly toward the camera to avoid coplanar flicker.
    pg.translate(0, 0, -2f);
    drawAlignmentGrid(pg, half, accent);
    drawOrientationCues(pg, half, accent);
    drawColorBarLabels(pg, half);
    drawGrayRampLabels(pg, half);

    pg.noStroke();
    pg.fill(accent);
    pg.rect(0, -half + 62f, 760f, 102f);
    pg.fill(255);
    pg.textSize(48f);
    pg.text("FACE " + index + "   " + axis + "   " + direction, 0, -half + 57f);

    pg.popStyle();
  }

  private boolean drawShaderColorPattern(PGraphicsOpenGL pg) {
    if (colorReferenceShader == null) {
      return false;
    }

    try {
      colorReferenceShader.set("targetSize", TARGET_SIZE);
      pg.shader(colorReferenceShader);
      pg.noStroke();
      pg.fill(255);
      pg.rect(0, 0, TARGET_SIZE, TARGET_SIZE);
      pg.resetShader();
      return true;
    } catch (RuntimeException error) {
      pg.resetShader();
      colorReferenceShader = null;
      reportShaderFailure(error);
      return false;
    }
  }

  private void drawFallbackColorPattern(PGraphicsOpenGL pg, float half) {
    pg.noStroke();
    pg.fill(18);
    pg.rect(0, 0, TARGET_SIZE, TARGET_SIZE);

    int[] colors = {
        pg.color(255, 0, 0),
        pg.color(0, 255, 0),
        pg.color(0, 0, 255),
        pg.color(0, 255, 255),
        pg.color(255, 0, 255),
        pg.color(255, 255, 0),
        pg.color(255),
        pg.color(0)
    };
    float colorWidth = 132f;
    float colorY = half - 280f;
    for (int i = 0; i < colors.length; i++) {
      float x = (i - (colors.length - 1) / 2f) * colorWidth;
      pg.fill(colors[i]);
      pg.rect(x, colorY, colorWidth, 108f);
    }

    int graySteps = 9;
    float grayWidth = 118f;
    float grayY = half - 145f;
    for (int i = 0; i < graySteps; i++) {
      int level = round(map(i, 0, graySteps - 1, 0, 255));
      float x = (i - (graySteps - 1) / 2f) * grayWidth;
      pg.fill(level);
      pg.rect(x, grayY, grayWidth, 72f);
    }
  }

  private void drawAlignmentGrid(PGraphicsOpenGL pg, float half, int accent) {
    float step = TARGET_SIZE / GRID_DIVISIONS;

    for (int i = -GRID_DIVISIONS / 2; i <= GRID_DIVISIONS / 2; i++) {
      float position = i * step;
      boolean major = i == 0 || abs(i) == GRID_DIVISIONS / 4;
      pg.stroke(major ? 125 : 58);
      pg.strokeWeight(major ? 3f : 1f);
      pg.line(position, -half, position, half);
      pg.line(-half, position, half, position);
    }

    pg.noFill();
    pg.stroke(accent);
    pg.strokeWeight(10f);
    pg.rect(0, 0, TARGET_SIZE - 18f, TARGET_SIZE - 18f);

    pg.stroke(220);
    pg.strokeWeight(4f);
    pg.rect(0, -70f, TARGET_SIZE * 0.78f, TARGET_SIZE * 0.62f);
    pg.rect(0, -70f, TARGET_SIZE * 0.56f, TARGET_SIZE * 0.42f);
    pg.ellipse(0, -70f, TARGET_SIZE * 0.52f, TARGET_SIZE * 0.52f);
    pg.ellipse(0, -70f, TARGET_SIZE * 0.24f, TARGET_SIZE * 0.24f);

    pg.stroke(accent);
    pg.strokeWeight(7f);
    pg.line(-150f, -70f, 150f, -70f);
    pg.line(0, -220f, 0, 80f);
    pg.noFill();
    pg.ellipse(0, -70f, 42f, 42f);
  }

  private void drawOrientationCues(PGraphicsOpenGL pg, float half, int accent) {
    pg.noStroke();
    pg.fill(accent);
    pg.triangle(-34f, -half + 245f, 34f, -half + 245f, 0, -half + 170f);
    pg.textSize(30f);
    pg.fill(255);
    pg.text("UP", 0, -half + 278f);

    float markerX = half - 108f;
    pg.fill(accent);
    pg.rect(markerX, -70f, 116f, 116f);
    pg.fill(255);
    pg.textSize(52f);
    pg.text("R", markerX, -76f);

    pg.textSize(28f);
    pg.textAlign(LEFT, CENTER);
    pg.fill(255);
    pg.text("TL", -half + 34f, -half + 36f);
    pg.textAlign(RIGHT, CENTER);
    pg.text("TR", half - 34f, -half + 36f);
    pg.textAlign(LEFT, CENTER);
    pg.text("BL", -half + 34f, half - 36f);
    pg.textAlign(RIGHT, CENTER);
    pg.text("BR", half - 34f, half - 36f);
    pg.textAlign(CENTER, CENTER);

    pg.stroke(255);
    pg.strokeWeight(6f);
    float tick = 62f;
    pg.line(-tick, -half + 8f, tick, -half + 8f);
    pg.line(-tick, half - 8f, tick, half - 8f);
    pg.line(-half + 8f, -tick, -half + 8f, tick);
    pg.line(half - 8f, -tick, half - 8f, tick);
  }

  private void drawColorBarLabels(PGraphicsOpenGL pg, float half) {
    String[] labels = { "R", "G", "B", "C", "M", "Y", "W", "K" };
    float barWidth = 132f;
    float y = half - 280f;

    pg.textSize(25f);
    for (int i = 0; i < labels.length; i++) {
      float x = (i - (labels.length - 1) / 2f) * barWidth;
      pg.fill(i == 1 || i == 3 || i == 5 || i == 6 ? 0 : 255);
      pg.text(labels[i], x, y);
    }
  }

  private void drawGrayRampLabels(PGraphicsOpenGL pg, float half) {
    int steps = 9;
    float width = 118f;
    float y = half - 145f;

    pg.textSize(20f);
    for (int i = 0; i < steps; i++) {
      int level = round(map(i, 0, steps - 1, 0, 255));
      float x = (i - (steps - 1) / 2f) * width;
      pg.fill(level > 135 ? 0 : 255);
      pg.text(str(level), x, y);
    }
  }

  private void reportShaderFailure(RuntimeException error) {
    if (!shaderFailureReported) {
      println(
          "[CalibrationTest] GLSL color reference unavailable; "
          + "using the Processing fallback: " + error.getMessage());
      shaderFailureReported = true;
    }
  }

  private void printCalibrationState() {
    println(
        "[CalibrationTest] view=" + dome.getCurrentView()
        + " fov=" + nf(dome.getFov(), 0, 1)
        + " size=" + nf(dome.getFishSize(), 0, 1)
        + " pitch=" + nf(degrees(dome.getPitch()), 0, 1)
        + " yaw=" + nf(degrees(dome.getYaw()), 0, 1)
        + " roll=" + nf(degrees(dome.getRoll()), 0, 1)
        + " floatingPreview=" + dome.isShowPreview());
  }

  @Override
  public String getName() {
    return "Alignment and Color Calibration";
  }
}
