class CubeCalibrationScene implements Scene {
  private final float FACE_DISTANCE = 900f;
  private final float TARGET_SIZE = 1800f;
  private final float ANNOTATION_BIAS = 2f;
  private final int ANNOTATION_TEXTURE_SIZE = 1024;
  private final int GRID_DIVISIONS = 24;

  private final ziviDomeLive dome;
  private PShader calibrationShader;
  private PGraphicsOpenGL[] annotationMaps;
  private boolean shaderFailureReported;

  CubeCalibrationScene(ziviDomeLive dome) {
    this.dome = dome;
  }

  @Override
  public void setupScene() {
    try {
      calibrationShader = dome.getPApplet().loadShader(
          "cube-calibration.frag",
          "cube-calibration.vert");
      createAnnotationMaps();
      println(
          "[CalibrationTool] Cube calibration shader loaded (GLSL 4.10); "
          + "annotations mapped at " + ANNOTATION_TEXTURE_SIZE + " px per face.");
    } catch (RuntimeException error) {
      calibrationShader = null;
      disposeAnnotationMaps();
      reportShaderFailure(error);
    }
  }

  @Override
  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(0);
    pg.noLights();
    pg.colorMode(RGB, 255);
    pg.blendMode(BLEND);

    drawFace(pg, 0, "+X", "RIGHT", faceAccent(0),
        FACE_DISTANCE, 0, 0, 0, HALF_PI, 0);
    drawFace(pg, 1, "-X", "LEFT", faceAccent(1),
        -FACE_DISTANCE, 0, 0, 0, -HALF_PI, 0);
    drawFace(pg, 2, "+Y", "DOWN", faceAccent(2),
        0, FACE_DISTANCE, 0, -HALF_PI, 0, 0);
    drawFace(pg, 3, "-Y", "UP", faceAccent(3),
        0, -FACE_DISTANCE, 0, HALF_PI, 0, 0);
    drawFace(pg, 4, "+Z", "FRONT", faceAccent(4),
        0, 0, FACE_DISTANCE, 0, 0, 0);
    drawFace(pg, 5, "-Z", "BACK", faceAccent(5),
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

    if (!drawMappedFacePattern(pg, index, accent)) {
      drawFallbackPattern(pg, half, accent);
      pg.translate(0, 0, -ANNOTATION_BIAS);
      drawFaceAnnotations(pg, index, axis, direction, accent, half);
    }
    pg.popStyle();
  }

  private boolean drawMappedFacePattern(PGraphicsOpenGL pg, int index, int accent) {
    if (calibrationShader == null || annotationMaps == null) {
      return false;
    }

    try {
      calibrationShader.set("faceResolution", (float) pg.width, (float) pg.height);
      calibrationShader.set("gridDivisions", (float) GRID_DIVISIONS);
      calibrationShader.set("faceIndex", index);
      calibrationShader.set("annotationMap", annotationMaps[index]);
      calibrationShader.set(
          "accentColor",
          pg.red(accent) / 255f,
          pg.green(accent) / 255f,
          pg.blue(accent) / 255f);

      pg.shader(calibrationShader);
      pg.noStroke();
      pg.fill(255);
      pg.textureMode(NORMAL);
      float half = TARGET_SIZE / 2f;
      pg.beginShape(QUADS);
      pg.texture(annotationMaps[index]);
      pg.vertex(-half, -half, 0f, 0f, 0f);
      pg.vertex(half, -half, 0f, 1f, 0f);
      pg.vertex(half, half, 0f, 1f, 1f);
      pg.vertex(-half, half, 0f, 0f, 1f);
      pg.endShape(CLOSE);
      pg.resetShader();
      return true;
    } catch (RuntimeException error) {
      pg.resetShader();
      calibrationShader = null;
      reportShaderFailure(error);
      return false;
    }
  }

  private void createAnnotationMaps() {
    disposeAnnotationMaps();
    annotationMaps = new PGraphicsOpenGL[6];
    for (int index = 0; index < annotationMaps.length; index++) {
      PGraphicsOpenGL annotation = (PGraphicsOpenGL) dome.getPApplet().createGraphics(
          ANNOTATION_TEXTURE_SIZE,
          ANNOTATION_TEXTURE_SIZE,
          P3D);
      annotationMaps[index] = annotation;
      annotation.beginDraw();
      annotation.clear();
      annotation.noLights();
      annotation.colorMode(RGB, 255);
      annotation.blendMode(BLEND);
      annotation.pushMatrix();
      annotation.translate(ANNOTATION_TEXTURE_SIZE / 2f, ANNOTATION_TEXTURE_SIZE / 2f);
      annotation.scale(ANNOTATION_TEXTURE_SIZE / TARGET_SIZE);
      annotation.rectMode(CENTER);
      annotation.textAlign(CENTER, CENTER);
      drawFaceAnnotations(
          annotation,
          index,
          faceAxis(index),
          faceDirection(index),
          faceAccent(index),
          TARGET_SIZE / 2f);
      annotation.popMatrix();
      annotation.endDraw();
    }
  }

  private int faceAccent(int index) {
    switch (index) {
      case 0: return dome.getPApplet().color(235, 55, 55);
      case 1: return dome.getPApplet().color(125, 20, 20);
      case 2: return dome.getPApplet().color(45, 210, 80);
      case 3: return dome.getPApplet().color(20, 115, 45);
      case 4: return dome.getPApplet().color(60, 125, 245);
      default: return dome.getPApplet().color(35, 45, 145);
    }
  }

  private String faceAxis(int index) {
    String[] axes = {"+X", "-X", "+Y", "-Y", "+Z", "-Z"};
    return axes[index];
  }

  private String faceDirection(int index) {
    String[] directions = {"RIGHT", "LEFT", "DOWN", "UP", "FRONT", "BACK"};
    return directions[index];
  }

  private void disposeAnnotationMaps() {
    if (annotationMaps == null) {
      return;
    }
    for (PGraphicsOpenGL annotation : annotationMaps) {
      if (annotation != null) {
        annotation.dispose();
      }
    }
    annotationMaps = null;
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
  public void dispose() {
    disposeAnnotationMaps();
  }

  @Override
  public String getName() {
    return "Cube Focus and Color";
  }
}
