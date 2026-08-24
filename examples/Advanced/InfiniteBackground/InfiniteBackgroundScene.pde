class InfiniteBackgroundScene implements Scene {
  private final int BOX_COUNT = 10;
  private final float BOX_ORBIT_RADIUS = 520f;
  private final float[] boxX = new float[BOX_COUNT];
  private final float[] boxY = new float[BOX_COUNT];
  private final float[] boxZ = new float[BOX_COUNT];
  private final float[] boxRotationX = new float[BOX_COUNT];
  private final float[] boxRotationY = new float[BOX_COUNT];
  private final ziviDomeLive dome;
  private SceneServices services;
  private SceneEnvironmentService environment;
  private PApplet applet;
  private PImage realEnvironment;
  private PImage calibrationEnvironment;
  private float t;
  private boolean calibrationSource = true;

  InfiniteBackgroundScene(ziviDomeLive dome) {
    this.dome = dome;
  }

  public void configure(SceneServices services) {
    this.services = services;
    this.environment = services.environment();
    this.applet = services.applet();
  }

  public void setupScene() {
    String environmentPath = applet.sketchPath(
      "../SolarSystem/data/textures/8k_stars_milky_way.jpg");
    realEnvironment = services.assets().loadImage(environmentPath);
    calibrationEnvironment = createCalibrationEnvironment(2048, 1024);
      updateBoxTransforms();
      if (realEnvironment == null) {
        println("[InfiniteBackground] Could not load environment: " + environmentPath);
      }
      environment.setEquirectangular(calibrationEnvironment);
      environment.setIntensity(1.0f);

      println("[InfiniteBackground] Synthetic calibration Environment active.");
      println("[InfiniteBackground] Scene ready.");

      println("[InfiniteBackground] E source, V visible, [/] Environment yaw, -/+ intensity.");
      println("[InfiniteBackground] P/Y/R spherical +90 degrees, C resets spherical orientation.");
      println("[InfiniteBackground] Standard: drag rotates; mouse wheel changes orbit distance only.");
      println("[InfiniteBackground] Calibration: -Z seam magenta, -X cyan, +Z yellow, +X red, +Y green, -Y violet.");
  }

  public void update() {
    t += 0.01;
    updateBoxTransforms();
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    // Intentionally clear the scene. The library-owned environment pass is drawn after this
    // during cubemap capture, so this sketch verifies that scene background() calls do not
    // erase the infinite equirectangular background.
    pg.background(0, 0, 0, 0);

    pg.pushMatrix();
      drawReferenceRig(pg);
      drawOrbitingBoxes(pg);
    pg.popMatrix();
  }

  private void drawReferenceRig(PGraphicsOpenGL pg) {
    pg.noLights();
    pg.strokeWeight(3);

    pg.stroke(255, 80, 80);
    pg.line(0, 0, 0, 800, 0, 0);
    pg.stroke(140, 20, 20);
    pg.line(0, 0, 0, -800, 0, 0);

    pg.stroke(80, 255, 80);
    pg.line(0, 0, 0, 0, 800, 0);
    pg.stroke(20, 130, 20);
    pg.line(0, 0, 0, 0, -800, 0);

    pg.stroke(80, 150, 255);
    pg.line(0, 0, 0, 0, 0, 800);
    pg.stroke(30, 60, 150);
    pg.line(0, 0, 0, 0, 0, -800);

    pg.strokeWeight(1);
    pg.stroke(255, 255, 255, 80);
    int grid = 800;
    int step = 100;
    for (int i = -grid; i <= grid; i += step) {
      pg.line(i, -grid, -250, i, grid, -250);
      pg.line(-grid, i, -250, grid, i, -250);
    }
  }

  private void drawOrbitingBoxes(PGraphicsOpenGL pg) {
    pg.lights();
    for (int i = 0; i < BOX_COUNT; i++) {
      pg.pushMatrix();
        pg.translate(boxX[i], boxY[i], boxZ[i]);
        pg.rotateX(boxRotationX[i]);
        pg.rotateY(boxRotationY[i]);
        pg.noStroke();
        pg.fill(80 + i * 15, 170, 255 - i * 12);
        pg.box(85);
      pg.popMatrix();
    }
  }

  private void updateBoxTransforms() {
    for (int i = 0; i < BOX_COUNT; i++) {
      float angle = TWO_PI * i / BOX_COUNT + t;
      boxX[i] = cos(angle) * BOX_ORBIT_RADIUS;
      boxY[i] = sin(angle) * BOX_ORBIT_RADIUS;
      boxZ[i] = sin(angle * 2f) * 180f;
      boxRotationX[i] = t + i * 0.2f;
      boxRotationY[i] = t * 1.4f + i * 0.1f;
    }
  }

  public void keyEvent(processing.event.KeyEvent event) {
    if (event.getAction() != processing.event.KeyEvent.PRESS) {
      return;
    }

    switch (event.getKey()) {
      case 'e':
      case 'E':
        toggleEnvironmentSource();
        break;
      case 'v':
      case 'V':
        environment.setVisible(!environment.isVisible());
        println("[InfiniteBackground] visible=" + environment.isVisible());
        break;
      case '[':
        environment.setYawOffset(environment.getYawOffset() - 0.1f);
        break;
      case ']':
        environment.setYawOffset(environment.getYawOffset() + 0.1f);
        break;
      case '-':
        environment.setIntensity(max(0f, environment.getIntensity() - 0.1f));
        break;
      case '+':
      case '=':
        environment.setIntensity(environment.getIntensity() + 0.1f);
        break;
      case 'p':
      case 'P':
        dome.setPitch(dome.getPitch() + HALF_PI);
        break;
      case 'y':
      case 'Y':
        dome.setYaw(dome.getYaw() + HALF_PI);
        break;
      case 'r':
      case 'R':
        dome.setRoll(dome.getRoll() + HALF_PI);
        break;
      case 'c':
      case 'C':
        dome.setPitch(0);
        dome.setYaw(0);
        dome.setRoll(0);
        break;
    }
  }

  private void toggleEnvironmentSource() {
    if (calibrationSource && realEnvironment == null) {
      println("[InfiniteBackground] Real panorama is unavailable; keeping calibration source.");
      return;
    }
    calibrationSource = !calibrationSource;
    environment.setEquirectangular(
      calibrationSource ? calibrationEnvironment : realEnvironment);
    println("[InfiniteBackground] source=" + (calibrationSource ? "calibration" : "real"));
  }

  public void mouseEvent(processing.event.MouseEvent event) {
  }

  public String getName() {
    return "Infinite Background";
  }

  public void dispose() {
    realEnvironment = null;
    calibrationEnvironment = null;
    environment = null;
    applet = null;
    services = null;
  }

  // Generated once during setup: this is a small diagnostic source, never a frame-loop readback.
  PImage createCalibrationEnvironment(int width, int height) {
    PImage image = createImage(width, height, ARGB);
    image.loadPixels();
    for (int y = 0; y < height; y++) {
      float v = y / float(height - 1);
      float latitudeShade = 0.45 + 0.55 * sin(v * PI);
      for (int x = 0; x < width; x++) {
        float u = x / float(width);
        int sector = floor(u * 8.0) % 8;
        int baseR = int((35 + sector * 17) * latitudeShade);
        int baseG = int((45 + ((sector + 3) % 8) * 14) * latitudeShade);
        int baseB = int((60 + ((sector + 5) % 8) * 16) * latitudeShade);
        int pixelColor = color(baseR, baseG, baseB);

        boolean longitudeGrid = x % (width / 24) < 2;
        boolean latitudeGrid = y % (height / 12) < 2;
        if (longitudeGrid || latitudeGrid) {
          pixelColor = color(220, 220, 220);
        }

        float markerWidth = 0.006;
        if (circularDistance(u, 0.00) < markerWidth) pixelColor = color(255, 0, 255); // -Z Back / seam
        if (circularDistance(u, 0.25) < markerWidth) pixelColor = color(0, 255, 255); // -X Left
        if (circularDistance(u, 0.50) < markerWidth) pixelColor = color(255, 220, 0); // +Z Front
        if (circularDistance(u, 0.75) < markerWidth) pixelColor = color(255, 50, 50); // +X Right
        if (v < 0.012) pixelColor = color(50, 255, 80);                              // +Y Top
        if (v > 0.988) pixelColor = color(120, 60, 255);                             // -Y Bottom

        image.pixels[y * width + x] = pixelColor;
      }
    }
    image.updatePixels();
    return image;
  }

  float circularDistance(float a, float b) {
    float direct = abs(a - b);
    return min(direct, 1.0 - direct);
  }
}
