class CubeCalibrationScene implements Scene {
  private static final float FACE_DISTANCE = 900f;
  private static final float TARGET_SIZE = 1800f;
  private static final int GRID_DIVISIONS = 24;

  private final zividomelive dome;
  private PShader calibrationShader;
  private boolean shaderFailureReported;

  CubeCalibrationScene(zividomelive dome) {
    this.dome = dome;
  }

  @Override
  public void setupScene() {
    try {
      calibrationShader = dome.getPApplet().loadShader(
          "cube-calibration.frag",
          "cube-calibration.vert");
      println("[CalibrationTool] Cube calibration shader loaded (GLSL 4.10).");
    } catch (RuntimeException error) {
      calibrationShader = null;
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

  @Override
  public void keyEvent(KeyEvent event) {
    handleCalibrationKey(event);
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
    pg.textAlign(CENTER, CENTER);

    if (!drawShaderPattern(pg, index, accent)) {
      drawFallbackPattern(pg, half, accent);
    }

    // Annotations sit toward the observer and remain attached to the face.
    pg.translate(0, 0, -2f);
    drawFaceAnnotations(pg, index, axis, direction, accent, half);
    pg.popStyle();
  }

  private boolean drawShaderPattern(PGraphicsOpenGL pg, int index, int accent) {
    if (calibrationShader == null) {
      return false;
    }

    try {
      calibrationShader.set("targetSize", TARGET_SIZE);
      calibrationShader.set("faceResolution", (float) pg.width, (float) pg.height);
      calibrationShader.set("gridDivisions", (float) GRID_DIVISIONS);
      calibrationShader.set("faceIndex", index);
      calibrationShader.set(
          "accentColor",
          pg.red(accent) / 255f,
          pg.green(accent) / 255f,
          pg.blue(accent) / 255f);

      pg.shader(calibrationShader);
      pg.noStroke();
      pg.fill(255);
      pg.rect(0, 0, TARGET_SIZE, TARGET_SIZE);
      pg.resetShader();
      return true;
    } catch (RuntimeException error) {
      pg.resetShader();
      calibrationShader = null;
      reportShaderFailure(error);
      return false;
    }
  }

  private void drawFallbackPattern(PGraphicsOpenGL pg, float half, int accent) {
    pg.noStroke();
    pg.fill(18);
    pg.rect(0, 0, TARGET_SIZE, TARGET_SIZE);

    float step = TARGET_SIZE / GRID_DIVISIONS;
    for (int i = -GRID_DIVISIONS / 2; i <= GRID_DIVISIONS / 2; i++) {
      float position = i * step;
      boolean major = i % 6 == 0;
      pg.stroke(major ? accent : pg.color(58));
      pg.strokeWeight(major ? 3f : 1f);
      pg.line(position, -half, position, half);
      pg.line(-half, position, half, position);
    }

    int[] colors = {
        pg.color(255, 0, 0), pg.color(0, 255, 0),
        pg.color(0, 0, 255), pg.color(0, 255, 255),
        pg.color(255, 0, 255), pg.color(255, 255, 0),
        pg.color(255), pg.color(0)
    };
    pg.noStroke();
    float width = 132f;
    for (int i = 0; i < colors.length; i++) {
      pg.fill(colors[i]);
      pg.rect((i - 3.5f) * width, half - 380f, width, 92f);
    }
  }

  private void drawFaceAnnotations(
      PGraphicsOpenGL pg,
      int index,
      String axis,
      String direction,
      int accent,
      float half) {
    pg.noStroke();
    pg.fill(accent);
    pg.rect(0, -half + 48f, 650f, 78f);
    pg.fill(255);
    pg.textSize(38f);
    pg.text("FACE " + index + "   " + axis + "   " + direction, 0, -half + 44f);

    pg.textSize(21f);
    pg.text("FOCUS LINES   1  2  4  8 PX", -525f, -half + 132f);
    pg.text("STARS   1  2  3  4 PX", 565f, -half + 132f);
    pg.text("LINEAR RGB", -500f, half - 565f);
    pg.text("LINEAR CMY", 500f, half - 565f);
    pg.text("R  G  B  C  M  Y  W  K", 0, half - 438f);
    pg.text("0       32       64       96      128      159      191      223      255",
        0, half - 238f);

    pg.fill(accent);
    pg.triangle(-32f, -half + 205f, 32f, -half + 205f, 0, -half + 148f);
    pg.fill(255);
    pg.textSize(22f);
    pg.text("UP", 0, -half + 235f);

    pg.fill(accent);
    pg.rect(half - 88f, -40f, 90f, 90f);
    pg.fill(255);
    pg.textSize(42f);
    pg.text("R", half - 88f, -45f);

    pg.textSize(18f);
    for (int i = 0; i <= 4; i++) {
      float position = map(i, 0, 4, -half + 32f, half - 32f);
      String value = str(i * 6);
      if (i != 2) {
        pg.text(value, position, -half + 96f);
      }
      pg.text(value, -half + 24f, position);
    }
  }

  private void reportShaderFailure(RuntimeException error) {
    if (!shaderFailureReported) {
      println(
          "[CalibrationTool] GLSL cube pattern unavailable; using fallback: "
          + error.getMessage());
      shaderFailureReported = true;
    }
  }

  @Override
  public String getName() {
    return "Cube Focus and Color";
  }
}
