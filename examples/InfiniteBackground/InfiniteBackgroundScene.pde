class InfiniteBackgroundScene implements Scene {
  private final ziviDomeLive dome;
  private final PImage realEnvironment;
  private final PImage calibrationEnvironment;
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
    println("[InfiniteBackground] Scene ready.");
    println("[InfiniteBackground] 1 Standard, 2 Domemaster, 3 Equirectangular, 4 Skybox.");
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
      case '1':
        dome.setRenderMode(RenderMode.STANDARD);
        break;
      case '2':
        dome.setRenderMode(RenderMode.DOMEMASTER);
        break;
      case '3':
        dome.setRenderMode(RenderMode.EQUIRECTANGULAR);
        break;
      case '4':
        dome.setRenderMode(RenderMode.SKYBOX);
        break;
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
}
