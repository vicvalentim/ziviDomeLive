class InfiniteBackgroundScene implements Scene {
  private final ziviDomeLive dome;
  private PImage realEnvironment;
  private PImage calibrationEnvironment;
  private float t;
  private boolean calibrationSource = true;

  InfiniteBackgroundScene(
      ziviDomeLive dome,
      PImage realEnvironment,
      PImage calibrationEnvironment) {
    this.dome = dome;
    this.realEnvironment = realEnvironment;
    this.calibrationEnvironment = calibrationEnvironment;
  }

  public void setupScene() {
    String environmentPath = sketchPath("../SolarSystem/data/textures/8k_stars_milky_way.jpg");
    realEnvironment = loadImage(environmentPath);
    calibrationEnvironment = createCalibrationEnvironment(2048, 1024);
      if (realEnvironment == null) {
        println("[InfiniteBackground] Could not load environment: " + environmentPath);
      }
      dome.setEquirectangularBackground(calibrationEnvironment);
      dome.setEnvironmentBackgroundIntensity(1.0);
      
      println("[InfiniteBackground] Synthetic calibration Environment active.");
      println("[InfiniteBackground] Scene ready.");

      println("[InfiniteBackground] E source, V visible, [/] Environment yaw, -/+ intensity.");
      println("[InfiniteBackground] P/Y/R spherical +90 degrees, C resets spherical orientation.");
      println("[InfiniteBackground] Standard: drag rotates; mouse wheel changes orbit distance only.");
      println("[InfiniteBackground] Calibration: -Z seam magenta, -X cyan, +Z yellow, +X red, +Y green, -Y violet.");
  }

  public void update() {
    t += 0.01;
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
    int count = 10;
    float radius = 520;
    for (int i = 0; i < count; i++) {
      float a = TWO_PI * i / count + t;
      float x = cos(a) * radius;
      float y = sin(a) * radius;
      float z = sin(a * 2.0) * 180;

      pg.pushMatrix();
        pg.translate(x, y, z);
        pg.rotateX(t + i * 0.2);
        pg.rotateY(t * 1.4 + i * 0.1);
        pg.noStroke();
        pg.fill(80 + i * 15, 170, 255 - i * 12);
        pg.box(85);
      pg.popMatrix();
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
        dome.setEnvironmentBackgroundVisible(!dome.isEnvironmentBackgroundVisible());
        println("[InfiniteBackground] visible=" + dome.isEnvironmentBackgroundVisible());
        break;
      case '[':
        dome.setEnvironmentBackgroundYawOffset(dome.getEnvironmentBackgroundYawOffset() - 0.1);
        break;
      case ']':
        dome.setEnvironmentBackgroundYawOffset(dome.getEnvironmentBackgroundYawOffset() + 0.1);
        break;
      case '-':
        dome.setEnvironmentBackgroundIntensity(max(0, dome.getEnvironmentBackgroundIntensity() - 0.1));
        break;
      case '+':
      case '=':
        dome.setEnvironmentBackgroundIntensity(dome.getEnvironmentBackgroundIntensity() + 0.1);
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
      case '1':
        dome.setRenderMode(RenderMode.STANDARD);
        println("[InfiniteBackground] mode=STANDARD");
        break;
      case '2':
        dome.setRenderMode(RenderMode.DOMEMASTER);
        println("[InfiniteBackground] mode=DOMEMASTER");
        break;
      case '3':
        dome.setRenderMode(RenderMode.EQUIRECTANGULAR);
        println("[InfiniteBackground] mode=EQUIRECTANGULAR");
        break;
      case '4':
        dome.setRenderMode(RenderMode.SKYBOX);
        println("[InfiniteBackground] mode=SKYBOX");
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
    dome.setEquirectangularBackground(
      calibrationSource ? calibrationEnvironment : realEnvironment);
    println("[InfiniteBackground] source=" + (calibrationSource ? "calibration" : "real"));
  }

  public void mouseEvent(processing.event.MouseEvent event) {
  }

  public void controlEvent(controlP5.ControlEvent event) {
  }

  public String getName() {
    return "Infinite Background";
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
