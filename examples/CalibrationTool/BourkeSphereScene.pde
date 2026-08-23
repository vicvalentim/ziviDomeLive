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
  private final float[] latitudeSines = new float[LATITUDE_SEGMENTS + 1];
  private final float[] latitudeCosines = new float[LATITUDE_SEGMENTS + 1];
  private final float[] latitudeTextureV = new float[LATITUDE_SEGMENTS + 1];
  private final float[] longitudeSines = new float[LONGITUDE_SEGMENTS + 1];
  private final float[] longitudeCosines = new float[LONGITUDE_SEGMENTS + 1];
  private final float[] longitudeTextureU = new float[LONGITUDE_SEGMENTS + 1];

  private final ziviDomeLive dome;
  private SceneServices services;
  private PApplet applet;
  private PImage pattern;
  private int loadedPatternIndex = -1;
  private float patternRotation;
  private float rotationOrigin;
  private boolean rotating;
  private int rotationFrame;
  private int playbackFrameRate = DEFAULT_PLAYBACK_FPS;
  private double rotationStartSeconds;

  BourkeSphereScene(ziviDomeLive dome) {
    this.dome = dome;
  }

  @Override
  public void configure(SceneServices services) {
    this.services = services;
    this.applet = services.applet();
  }

  @Override
  public void setupScene() {
    buildSphereLookupTables();
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
      double elapsedSeconds = Math.max(
        0.0, services.frameClock().getElapsedSeconds() - rotationStartSeconds);
      int nextFrame = (int) (
        (elapsedSeconds * playbackFrameRate) % frameCount);
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
      pg.beginShape(TRIANGLE_STRIP);
      pg.texture(pattern);
      for (int longitude = 0; longitude <= LONGITUDE_SEGMENTS; longitude++) {
        sphereVertex(pg, latitude, longitude);
        sphereVertex(pg, latitude + 1, longitude);
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
    boolean outputEnabled = dome.getOutputManager().isActive();
    int referenceResolution = outputEnabled
      ? dome.getOutputResolution()
      : max(1, min(applet.width, applet.height));
    int patternIndex = closestPatternIndex(referenceResolution);
    if (pattern != null && patternIndex == loadedPatternIndex) {
      return;
    }

    PImage candidate = services.assets().loadImage(IMAGE_FILES[patternIndex]);
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
      : "window " + applet.width + "x" + applet.height;
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
    rotationStartSeconds = services.frameClock().getElapsedSeconds();
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

  private void buildSphereLookupTables() {
    for (int latitude = 0; latitude <= LATITUDE_SEGMENTS; latitude++) {
      float v = latitude / (float) LATITUDE_SEGMENTS;
      float angle = HALF_PI - PI * v;
      latitudeSines[latitude] = sin(angle);
      latitudeCosines[latitude] = cos(angle);
      latitudeTextureV[latitude] = v;
    }
    for (int longitude = 0; longitude <= LONGITUDE_SEGMENTS; longitude++) {
      float u = longitude / (float) LONGITUDE_SEGMENTS;
      float angle = -PI + TWO_PI * u;
      longitudeSines[longitude] = sin(angle);
      longitudeCosines[longitude] = cos(angle);
      longitudeTextureU[longitude] = u;
    }
  }

  private void sphereVertex(PGraphicsOpenGL pg, int latitude, int longitude) {
    float equatorialRadius = latitudeCosines[latitude];
    float x = SPHERE_CENTER_X
      + SPHERE_RADIUS * equatorialRadius * longitudeCosines[longitude];
    float y = SPHERE_CENTER_Y
      + SPHERE_RADIUS * equatorialRadius * longitudeSines[longitude];
    float z = SPHERE_CENTER_Z + SPHERE_RADIUS * latitudeSines[latitude];
    pg.vertex(
      x,
      y,
      z,
      longitudeTextureU[longitude],
      latitudeTextureV[latitude]);
  }

  private float wrapAngle(float angle) {
    float wrapped = angle % TWO_PI;
    return wrapped < 0f ? wrapped + TWO_PI : wrapped;
  }

  @Override
  public void dispose() {
    pattern = null;
    applet = null;
    services = null;
  }

  @Override
  public String getName() {
    return "Paul Bourke 360 Degree Sphere";
  }
}
