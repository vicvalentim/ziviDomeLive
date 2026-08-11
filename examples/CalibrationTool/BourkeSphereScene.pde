class BourkeSphereScene implements Scene {
  private static final String IMAGE_FILE = "spherical8192.png";
  private static final float SPHERE_CENTER_X = 0f;
  private static final float SPHERE_CENTER_Y = 0f;
  private static final float SPHERE_CENTER_Z = 0f;
  private static final float SPHERE_DIAMETER = 1800f;
  private static final float SPHERE_RADIUS = SPHERE_DIAMETER * 0.5f;
  private static final int LATITUDE_SEGMENTS = 90;
  private static final int LONGITUDE_SEGMENTS = 180;
  private static final float ROTATION_PERIOD_SECONDS = 60f;

  private final zividomelive dome;
  private PImage pattern;
  private float patternRotation;
  private boolean rotating;
  private int lastUpdateMillis;

  BourkeSphereScene(zividomelive dome) {
    this.dome = dome;
  }

  @Override
  public void setupScene() {
    pattern = dome.getPApplet().loadImage(IMAGE_FILE);
    if (pattern == null || pattern.width != 8192 || pattern.height != 4096) {
      throw new IllegalStateException(
          "Paul Bourke spherical test pattern must be the original 8192 x 4096 PNG.");
    }
    lastUpdateMillis = dome.getPApplet().millis();
    println(
        "[CalibrationTool] Paul Bourke v14 spherical pattern loaded: 8192 x 4096; "
        + "center=(0, 0, 0); diameter=" + nf(SPHERE_DIAMETER, 0, 0) + ".");
  }

  @Override
  public void update() {
    int now = dome.getPApplet().millis();
    float deltaSeconds = max(0, now - lastUpdateMillis) / 1000f;
    lastUpdateMillis = now;

    if (rotating) {
      patternRotation = (patternRotation
          + TWO_PI * deltaSeconds / ROTATION_PERIOD_SECONDS) % TWO_PI;
    }
  }

  @Override
  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(0);
    pg.noLights();
    pg.noStroke();
    pg.textureMode(NORMAL);
    pg.textureWrap(CLAMP);
    pg.hint(DISABLE_TEXTURE_MIPMAPS);
    pg.textureSampling(POINT);
    pg.pushMatrix();
    pg.rotateX(patternRotation);

    for (int latitude = 0; latitude < LATITUDE_SEGMENTS; latitude++) {
      float v0 = latitude / (float) LATITUDE_SEGMENTS;
      float v1 = (latitude + 1f) / LATITUDE_SEGMENTS;
      float latitude0 = HALF_PI - PI * v0;
      float latitude1 = HALF_PI - PI * v1;

      pg.beginShape(TRIANGLE_STRIP);
      pg.texture(pattern);
      for (int longitude = 0; longitude <= LONGITUDE_SEGMENTS; longitude++) {
        float u = longitude / (float) LONGITUDE_SEGMENTS;
        float sphericalLongitude = -PI + TWO_PI * u;
        sphereVertex(pg, latitude0, sphericalLongitude, u, v0);
        sphereVertex(pg, latitude1, sphericalLongitude, u, v1);
      }
      pg.endShape();
    }

    pg.popMatrix();
    pg.textureSampling(5);
    pg.hint(ENABLE_TEXTURE_MIPMAPS);
  }

  @Override
  public void keyEvent(KeyEvent event) {
    if (event.getAction() != KeyEvent.PRESS) {
      return;
    }

    char pressed = Character.toLowerCase(event.getKey());
    switch (pressed) {
      case ' ':
        rotating = !rotating;
        println("[CalibrationTool] Bourke rotation: " + (rotating ? "60 s/revolution" : "paused"));
        return;
      case ',':
        patternRotation = wrapAngle(patternRotation - radians(1f));
        rotating = false;
        break;
      case '.':
        patternRotation = wrapAngle(patternRotation + radians(1f));
        rotating = false;
        break;
      case 'c':
        patternRotation = 0f;
        rotating = false;
        break;
      case '0':
        patternRotation = 0f;
        rotating = false;
        handleCalibrationKey(event);
        break;
      default:
        handleCalibrationKey(event);
        return;
    }

    println(
        "[CalibrationTool] Bourke rotation="
        + nf(degrees(patternRotation), 0, 1) + " degrees");
  }

  private void sphereVertex(
      PGraphicsOpenGL pg,
      float latitude,
      float longitude,
      float u,
      float v) {
    float equatorialRadius = cos(latitude);
    float x = SPHERE_CENTER_X + SPHERE_RADIUS * sin(latitude);
    float y = SPHERE_CENTER_Y + SPHERE_RADIUS * equatorialRadius * sin(longitude);
    float z = SPHERE_CENTER_Z + SPHERE_RADIUS * equatorialRadius * cos(longitude);
    pg.vertex(x, y, z, u, v);
  }

  private float wrapAngle(float angle) {
    float wrapped = angle % TWO_PI;
    return wrapped < 0f ? wrapped + TWO_PI : wrapped;
  }

  @Override
  public String getName() {
    return "Paul Bourke 360 Degree Sphere";
  }
}
