class BourkeSphereScene implements Scene {
  private final int[] RESOLUTION_BUCKETS = {1024, 2048, 3072, 4096};
  private final String[] IMAGE_FILES = {
    "img/spherical2400.png",
    "img/spherical4096.png",
    "img/spherical4800.png",
    "img/spherical8192.png"
  };
  private final int[] IMAGE_WIDTHS = {2400, 4096, 4800, 8192};
  private final int[] IMAGE_HEIGHTS = {1200, 2048, 2400, 4096};
  private final float SPHERE_CENTER_X = 0f;
  private final float SPHERE_CENTER_Y = 0f;
  private final float SPHERE_CENTER_Z = 0f;
  private final float SPHERE_DIAMETER = 1800f;
  private final float SPHERE_RADIUS = SPHERE_DIAMETER * 0.5f;
  private final int LATITUDE_SEGMENTS = 90;
  private final int LONGITUDE_SEGMENTS = 180;
  private final float ROTATION_PERIOD_SECONDS = 60f;
  private final int DEFAULT_PLAYBACK_FPS = 60;

  private final ziviDomeLive dome;
  private PImage pattern;
  private int loadedPatternIndex = -1;
  private float patternRotation;
  private float rotationOrigin;
  private boolean rotating;
  private int rotationFrame;
  private int playbackFrameRate = DEFAULT_PLAYBACK_FPS;
  private long rotationStartMillis;

  BourkeSphereScene(ziviDomeLive dome) {
    this.dome = dome;
  }

  @Override
  public void setupScene() {
    loadPatternForCurrentResolution();
    println(
      "[CalibrationTool] Bourke sphere center=(0, 0, 0); diameter="
      + nf(SPHERE_DIAMETER, 0, 0) + ".");
  }

  @Override
  public void update() {
    loadPatternForCurrentResolution();

    if (rotating) {
      int frameCount = framesPerRevolution();
      long elapsedMillis = Math.max(
        0L, (long) dome.getPApplet().millis() - rotationStartMillis);
      int nextFrame = (int) (
        (elapsedMillis * playbackFrameRate / 1000L) % frameCount);
      if (nextFrame != rotationFrame) {
        rotationFrame = nextFrame;
        patternRotation = wrapAngle(
          rotationOrigin + TWO_PI * rotationFrame / (float) frameCount);
      }
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
    pg.rotateZ(patternRotation);

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
        if (rotating) {
          pauseRotation();
          println("[CalibrationTool] Bourke rotation: paused");
        } else {
          beginRotationSequence();
          printRotationProtocol();
        }
        return;
      case 't':
        playbackFrameRate = playbackFrameRate == 30 ? 60 : 30;
        if (rotating) {
          beginRotationSequence();
        }
        printRotationProtocol();
        return;
      case ',':
        pauseRotation();
        patternRotation = wrapAngle(patternRotation - radians(1f));
        break;
      case '.':
        pauseRotation();
        patternRotation = wrapAngle(patternRotation + radians(1f));
        break;
      case 'c':
        pauseRotation();
        patternRotation = 0f;
        break;
      case '0':
        pauseRotation();
        patternRotation = 0f;
        playbackFrameRate = DEFAULT_PLAYBACK_FPS;
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

  private void loadPatternForCurrentResolution() {
    boolean outputEnabled = dome.isEnableOutput();
    int referenceResolution = outputEnabled
      ? dome.getOutputResolution()
      : max(1, min(dome.getPApplet().width, dome.getPApplet().height));
    int patternIndex = closestPatternIndex(referenceResolution);
    if (pattern != null && patternIndex == loadedPatternIndex) {
      return;
    }

    PImage candidate = dome.getPApplet().loadImage(IMAGE_FILES[patternIndex]);
    if (candidate == null
      || candidate.width != IMAGE_WIDTHS[patternIndex]
      || candidate.height != IMAGE_HEIGHTS[patternIndex]) {
      throw new IllegalStateException(
        "Paul Bourke spherical test pattern must be the original "
        + IMAGE_WIDTHS[patternIndex] + " x " + IMAGE_HEIGHTS[patternIndex]
        + " PNG: " + IMAGE_FILES[patternIndex]);
    }

    pattern = candidate;
    loadedPatternIndex = patternIndex;
    String source = outputEnabled
      ? "output " + referenceResolution + "px"
      : "window " + dome.getPApplet().width + "x" + dome.getPApplet().height;
    println(
      "[CalibrationTool] Paul Bourke v14 pattern: "
      + IMAGE_WIDTHS[patternIndex] + "x" + IMAGE_HEIGHTS[patternIndex]
      + " for " + source + " (" + RESOLUTION_BUCKETS[patternIndex] + " bucket).");
  }

  private int closestPatternIndex(int referenceResolution) {
    int closestIndex = 0;
    int closestDistance = abs(referenceResolution - RESOLUTION_BUCKETS[0]);
    for (int index = 1; index < RESOLUTION_BUCKETS.length; index++) {
      int distance = abs(referenceResolution - RESOLUTION_BUCKETS[index]);
      if (distance < closestDistance) {
        closestIndex = index;
        closestDistance = distance;
      }
    }
    return closestIndex;
  }

  private int framesPerRevolution() {
    return max(1, round(ROTATION_PERIOD_SECONDS * playbackFrameRate));
  }

  private void beginRotationSequence() {
    rotating = true;
    rotationOrigin = patternRotation;
    rotationFrame = 0;
    rotationStartMillis = dome.getPApplet().millis();
  }

  private void pauseRotation() {
    rotating = false;
    rotationFrame = 0;
  }

  private void printRotationProtocol() {
    println(
      "[CalibrationTool] Bourke rotation: "
      + (rotating ? "running, " : "paused, ")
      + playbackFrameRate + " fps profile, " + framesPerRevolution()
      + " frames/revolution, 60 s/revolution.");
  }

  private void sphereVertex(
    PGraphicsOpenGL pg,
    float latitude,
    float longitude,
    float u,
    float v) {
    float equatorialRadius = cos(latitude);
    float x = SPHERE_CENTER_X + SPHERE_RADIUS * equatorialRadius * cos(longitude);
    float y = SPHERE_CENTER_Y + SPHERE_RADIUS * equatorialRadius * sin(longitude);
    float z = SPHERE_CENTER_Z + SPHERE_RADIUS * sin(latitude);
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
