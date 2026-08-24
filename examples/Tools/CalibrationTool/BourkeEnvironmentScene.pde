class BourkeEnvironmentScene implements Scene {
  private final int[] RESOLUTION_BUCKETS = {1024, 2048, 3072, 4096};
  private final String[] IMAGE_FILES = {
    "img/spherical2400.png",
    "img/spherical4096.png",
    "img/spherical4800.png",
    "img/spherical8192.png"
  };
  private final int[] IMAGE_WIDTHS = {2400, 4096, 4800, 8192};
  private final int[] IMAGE_HEIGHTS = {1200, 2048, 2400, 4096};
  private final float ROTATION_PERIOD_SECONDS = 60f;
  private final float SOURCE_PITCH = HALF_PI;
  private final int DEFAULT_PLAYBACK_FPS = 60;

  private final ziviDomeLive dome;
  private SceneServices services;
  private SceneEnvironmentService environment;
  private PApplet applet;
  private PImage pattern;
  private int loadedPatternIndex = -1;
  private float patternRotation;
  private float rotationOrigin;
  private boolean rotating;
  private int rotationFrame;
  private int playbackFrameRate = DEFAULT_PLAYBACK_FPS;
  private double rotationStartSeconds;

  BourkeEnvironmentScene(ziviDomeLive dome) {
    this.dome = dome;
  }

  public void configure(SceneServices services) {
    this.services = services;
    this.environment = services.environment();
    this.applet = services.applet();
  }

  public void setupScene() {
    loadPatternForCurrentResolution();
    environment.setVisible(true);
    environment.setIntensity(1f);
    alignPatternToDome();
    environment.setYawOffset(patternRotation);
    println(
      "[CalibrationTool] Paul Bourke pattern uses the library Environment pass; "
      + "translation remains infinite and source alignment stays image-owned.");
    printEnvironmentState();
  }

  public void update() {
    loadPatternForCurrentResolution();

    if (rotating) {
      int frameCount = framesPerRevolution();
      double elapsedSeconds = Math.max(
        0.0, services.frameClock().getElapsedSeconds() - rotationStartSeconds);
      int nextFrame = (int) ((elapsedSeconds * playbackFrameRate) % frameCount);
      if (nextFrame != rotationFrame) {
        rotationFrame = nextFrame;
        setPatternRotation(
          rotationOrigin + TWO_PI * rotationFrame / (float) frameCount);
      }
    }
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    // The library-owned Environment pass is composed after sceneRender at far depth.
    pg.background(0, 0, 0, 0);
  }

  public void keyEvent(KeyEvent event) {
    if (event.getAction() != KeyEvent.PRESS) {
      return;
    }

    char pressed = Character.toLowerCase(event.getKey());
    switch (pressed) {
      case ' ':
        if (rotating) {
          pauseRotation();
          println("[CalibrationTool] Bourke Environment rotation: paused");
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
        setPatternRotation(patternRotation - radians(1f));
        break;
      case '.':
        pauseRotation();
        setPatternRotation(patternRotation + radians(1f));
        break;
      case 'c':
        pauseRotation();
        setPatternRotation(0f);
        break;
      case 'v':
        environment.setVisible(!environment.isVisible());
        break;
      case 'd':
        environment.setIntensity(max(0f, environment.getIntensity() - 0.1f));
        break;
      case 'b':
        environment.setIntensity(environment.getIntensity() + 0.1f);
        break;
      case '0':
        pauseRotation();
        playbackFrameRate = DEFAULT_PLAYBACK_FPS;
        environment.setVisible(true);
        environment.setIntensity(1f);
        alignPatternToDome();
        setPatternRotation(0f);
        handleCalibrationKey(event);
        break;
      default:
        handleCalibrationKey(event);
        return;
    }

    printEnvironmentState();
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
    environment.setEquirectangular(pattern);
    String source = outputEnabled
      ? "output " + referenceResolution + "px"
      : "window " + applet.width + "x" + applet.height;
    println(
      "[CalibrationTool] Paul Bourke v14 Environment: "
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

  private void setPatternRotation(float angle) {
    patternRotation = wrapAngle(angle);
    environment.setYawOffset(patternRotation);
  }

  private void alignPatternToDome() {
    // This rotates only the borrowed Environment image lookup, never dome/scene geometry.
    environment.setOrientationAxisAngle(1f, 0f, 0f, SOURCE_PITCH);
  }

  private void printRotationProtocol() {
    println(
      "[CalibrationTool] Bourke Environment rotation: "
      + (rotating ? "running, " : "paused, ")
      + playbackFrameRate + " fps profile, " + framesPerRevolution()
      + " positions/revolution, 60 s/revolution.");
  }

  private void printEnvironmentState() {
    println(
      "[CalibrationTool] Bourke Environment yaw="
      + nf(degrees(patternRotation), 0, 1) + " degrees"
      + " sourcePitch=" + nf(degrees(SOURCE_PITCH), 0, 1) + " degrees"
      + " visible=" + environment.isVisible()
      + " intensity=" + nf(environment.getIntensity(), 0, 1));
  }

  private float wrapAngle(float angle) {
    float wrapped = angle % TWO_PI;
    return wrapped < 0f ? wrapped + TWO_PI : wrapped;
  }

  public void dispose() {
    // SceneEnvironmentService restores the state replaced by this activation.
    pattern = null;
    environment = null;
    applet = null;
    services = null;
  }

  public String getName() {
    return "Paul Bourke Environment Background";
  }
}
