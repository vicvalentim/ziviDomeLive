Scene[] createBenchmarkScenes(ziviDomeLive parent) {
  return new Scene[] {
      new SyntheticBenchmarkScene(parent, "EMPTY", 0, false),
      new SyntheticBenchmarkScene(parent, "LIGHT", 24, false),
      new SyntheticBenchmarkScene(parent, "MEDIUM", 180, false),
      new SyntheticBenchmarkScene(parent, "HEAVY", 720, false),
      new SyntheticBenchmarkScene(parent, "SPHERICAL_STRESS", 640, true)
  };
}

/** Deterministic synthetic geometry with all mutable buffers allocated before measurement. */
class SyntheticBenchmarkScene implements Scene {
  final ziviDomeLive parent;
  final String sceneName;
  final int objectCount;
  final boolean spherical;
  final float[] positionsX;
  final float[] positionsY;
  final float[] positionsZ;
  final float[] scales;
  final float[] phases;
  final int[] colors;
  PShape unitBox;
  float animationPhase;

  SyntheticBenchmarkScene(
      ziviDomeLive parent,
      String sceneName,
      int objectCount,
      boolean spherical) {
    this.parent = parent;
    this.sceneName = sceneName;
    this.objectCount = objectCount;
    this.spherical = spherical;
    positionsX = new float[objectCount];
    positionsY = new float[objectCount];
    positionsZ = new float[objectCount];
    scales = new float[objectCount];
    phases = new float[objectCount];
    colors = new int[objectCount];
    initializeTransforms();
  }

  void initializeTransforms() {
    if (spherical) {
      final float goldenAngle = PI * (3.0f - sqrt(5.0f));
      for (int index = 0; index < objectCount; index++) {
        float normalized = (index + 0.5f) / max(1.0f, objectCount);
        float y = 1.0f - 2.0f * normalized;
        float ringRadius = sqrt(max(0.0f, 1.0f - y * y));
        float angle = goldenAngle * index;
        float radius = 850.0f + (index % 9) * 28.0f;
        positionsX[index] = cos(angle) * ringRadius * radius;
        positionsY[index] = y * radius;
        positionsZ[index] = sin(angle) * ringRadius * radius;
        initializeStyle(index, 28.0f, 5.0f);
      }
      return;
    }

    int grid = max(1, (int)ceil(pow(max(1, objectCount), 1.0f / 3.0f)));
    float spacing = objectCount >= 500 ? 95.0f : 135.0f;
    float center = (grid - 1) * 0.5f;
    for (int index = 0; index < objectCount; index++) {
      int x = index % grid;
      int y = (index / grid) % grid;
      int z = index / (grid * grid);
      positionsX[index] = (x - center) * spacing;
      positionsY[index] = (y - center) * spacing;
      positionsZ[index] = (z - center) * spacing;
      initializeStyle(index, 34.0f, 7.0f);
    }
  }

  void initializeStyle(int index, float baseScale, float scaleStep) {
    scales[index] = baseScale + (index % 7) * scaleStep;
    phases[index] = index * 0.17320508f;
    int red = 64 + (index * 53) % 192;
    int green = 64 + (index * 97) % 192;
    int blue = 64 + (index * 193) % 192;
    colors[index] = color(red, green, blue);
  }

  public void setupScene() {
    if (objectCount > 0 && unitBox == null) {
      unitBox = createShape(BOX, 1.0f);
      unitBox.disableStyle();
    }
    animationPhase = 0.0f;
  }

  public void update() {
    animationPhase += 0.0025f;
    if (animationPhase > TWO_PI) animationPhase -= TWO_PI;
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    if (objectCount == 0 || unitBox == null) return;

    pg.pushStyle();
    pg.noStroke();
    for (int index = 0; index < objectCount; index++) {
      pg.pushMatrix();
      pg.translate(positionsX[index], positionsY[index], positionsZ[index]);
      float angle = animationPhase + phases[index];
      pg.rotateX(angle * 0.71f);
      pg.rotateY(angle);
      pg.rotateZ(angle * 0.37f);
      pg.scale(scales[index]);
      pg.fill(colors[index]);
      pg.shape(unitBox);
      pg.popMatrix();
    }
    pg.popStyle();
  }

  public String getName() {
    return sceneName;
  }

  public void dispose() {
    // PShape and primitive buffers are retained for deterministic scene reactivation.
  }
}
