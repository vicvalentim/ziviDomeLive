import java.util.concurrent.locks.ReentrantReadWriteLock;

class Scene1 implements Scene {
  private zividomelive parent;
  private PApplet pApplet;
  private ArrayList<Planet> planets;
  private Sun sun;

  private SimParams simParams;
  private TextureManager textureManager;
  private ShaderManager shaderManager;
  private ShapeManager shapeManager;
  private ConfigLoader configLoader;
  private PhysicsEngine physicsEngine;
  private Renderer renderer;

  private float timeScale = 1.0f;
  private boolean showOrbits = true;
  private boolean showMoonOrbits = true;
  private boolean showLabels = false;
  private int selectedPlanet = -1;

  private int prevMouseX, prevMouseY;
  private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

  // DeltaTime
  private long lastUpdateTime = System.nanoTime();

  Scene1(zividomelive parent, PApplet pApplet) {
    this.parent = parent;
    this.pApplet = pApplet;

    simParams = new SimParams();
    textureManager = new TextureManager(pApplet);
    shapeManager = new ShapeManager(pApplet);
    shaderManager = new ShaderManager(pApplet);

    loadAllShaders();

    configLoader = new ConfigLoader(pApplet, textureManager, simParams);
    sun = configLoader.loadSun();
    planets = configLoader.loadConfiguration();

    physicsEngine = new PhysicsEngine(pApplet, planets, sun);
    renderer = new Renderer(pApplet, planets, configLoader.getSkySphere(), shapeManager, shaderManager, simParams);
    renderer.setSun(sun);

    configureCamera();
  }

  private void loadAllShaders() {
    pApplet.println("[Scene1] Shaders carregados com sucesso.");
  }

  private void configureCamera() {
    float neptuneDrawPos = NEPTUNE_DIST * PIXELS_PER_AU + SUN_VISUAL_RADIUS;
    renderer.setCameraDistance(neptuneDrawPos * 1.2f);
  }

  private float calculateDeltaTimeInSeconds() {
    long currentTime = System.nanoTime();
    float dt = (currentTime - lastUpdateTime) / 1_000_000_000.0f;
    lastUpdateTime = currentTime;
    return dt;
  }

  public void update() {
    rwLock.writeLock().lock();
    try {
      float dt = calculateDeltaTimeInSeconds() * timeScale;
      physicsEngine.update(dt);
      sun.update(dt);
      updateCameraTarget();
      trackSelectedPlanet();
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  private void updateCameraTarget() {
    float sunRadius = sun.getRadius();
    if (selectedPlanet == 0 && sun != null) {
      renderer.updateCameraTarget(sun.getPosition());
    } else if (selectedPlanet > 0 && selectedPlanet - 1 < planets.size()) {
      Planet target = planets.get(selectedPlanet - 1);
      renderer.updateCameraTarget(target.getDrawPosition(sunRadius));
    } else {
      renderer.updateCameraTarget(new PVector(0, 0, 0));
    }
  }

  private void trackSelectedPlanet() {
    if (selectedPlanet == 0 && sun != null) {
      renderer.goTo(sun.getPosition(), renderer.getCameraRotationX(), renderer.getCameraRotationY(), renderer.getCameraDistance());
    } else if (selectedPlanet > 0 && selectedPlanet - 1 < planets.size()) {
      float sunRadius = sun.getRadius();
      Planet target = planets.get(selectedPlanet - 1);
      renderer.goTo(target.getDrawPosition(sunRadius), renderer.getCameraRotationX(), renderer.getCameraRotationY(), renderer.getCameraDistance());
    }
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    rwLock.readLock().lock();
    try {
      shaderManager.setUniform("sun", "time", pApplet.millis() / 1000.0f);
      pg.background(0, 10, 20);
      pg.pushMatrix();
        renderer.setupCamera(pg);
        sun.display(pg, showLabels, shaderManager);
        renderer.drawLighting(pg);
        if (showOrbits) renderer.drawPlanetOrbits(pg);
        renderer.drawPlanetsAndMoons(pg, showLabels, showMoonOrbits, shapeManager, shaderManager);
        renderer.drawSkySphere(pg, renderer.getRenderingMode());
      pg.popMatrix();
    } finally {
      rwLock.readLock().unlock();
    }
  }

  public void setupScene() {
    rwLock.writeLock().lock();
    try {
      sun = configLoader.loadSun();
      sun.buildShape(pApplet, shapeManager);
      planets = configLoader.loadConfiguration();
      physicsEngine = new PhysicsEngine(pApplet, planets, sun);
      renderer.setSun(sun);
      renderer.setPlanets(planets);
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  private void changeRenderingMode(int mode) {
    renderer.setRenderingMode(mode);
    sun.setRenderingMode(mode);
    sun.buildShape(pApplet, shapeManager);
    for (Planet p : planets) {
      p.setRenderingMode(mode);
      p.buildShape(pApplet, shapeManager);
      for (Moon m : p.moons) {
        m.setRenderingMode(mode);
        m.buildShape(pApplet, shapeManager);
      }
    }
  }

  private void applyScalingFactors() {
    rwLock.writeLock().lock();
    try {
      if (sun != null) sun.applyScalingFactors(simParams);
      if (planets != null) {
        for (Planet p : planets) p.applyScalingFactors(simParams);
      }
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  public void keyEvent(processing.event.KeyEvent event) {
    if (event.getAction() != processing.event.KeyEvent.PRESS) return;

    char key = event.getKey();
    switch (key) {
      case ' ': resetView(); break;
      case 'G': simParams.globalScale *= 1.1f; simParams.planetAmplification = 1.0f; applyScalingFactors(); break;
      case 'g': simParams.globalScale /= 1.1f; simParams.planetAmplification = 1.0f; applyScalingFactors(); break;
      case 'a': simParams.planetAmplification *= 1.1f; applyScalingFactors(); break;
      case 'z': simParams.planetAmplification /= 1.1f; applyScalingFactors(); break;
      case 'r': simParams.globalScale = 1.0f; simParams.planetAmplification = 1.0f; applyScalingFactors(); pApplet.println("[Scene1] Escala global e amplificação resetadas."); break;
      case 'w': changeRenderingMode(0); break;
      case 's': changeRenderingMode(1); break;
      case 't': changeRenderingMode(2); break;
      case '+': timeScale *= 1.2f; break;
      case '-': timeScale *= 0.8f; break;
      case 'o': showOrbits = !showOrbits; break;
      case 'l': showLabels = !showLabels; break;
      case 'p': showMoonOrbits = !showMoonOrbits; break;
      default:
        if (Character.isDigit(key)) {
          int num = Character.getNumericValue(key);
          if (num == 1) selectedPlanet = 0;
          else if (num > 1 && num <= planets.size() + 1) selectedPlanet = num - 1;
        }
        break;
    }
  }

  public void mouseEvent(processing.event.MouseEvent event) {
    switch (event.getAction()) {
      case MouseEvent.PRESS:
        prevMouseX = event.getX();
        prevMouseY = event.getY();
        break;

      case MouseEvent.DRAG:
        float dx = (event.getX() - prevMouseX) * 0.01f;
        float dy = (event.getY() - prevMouseY) * 0.01f;
        renderer.setCameraRotation(
          renderer.getCameraRotationX() + dy,
          renderer.getCameraRotationY() + dx
        );
        prevMouseX = event.getX();
        prevMouseY = event.getY();
        break;

      case MouseEvent.WHEEL:
        float scroll = event.getCount();

        // 🔍 Detecção heurística de scroll contínuo (trackpad)
        boolean isTrackpad = Math.abs(scroll) < 1.0f;

        float zoomFactor;
        if (isTrackpad) {
          // Mais sensível, resolução contínua
          zoomFactor = scroll * 0.5f;
        } else {
          // Bolinha tradicional: incremento fixo
          zoomFactor = scroll * 2.0f;
        }

        renderer.setCameraDistance(
          PApplet.constrain(renderer.getCameraDistance() + zoomFactor, -100000.0f, 100000.0f)
        );
        break;
    }
  }

  private void resetView() {
    renderer.setCameraRotation(PConstants.PI / 16, 0);
    renderer.setCameraDistance(20);
  }

  public void dispose() {
    if (textureManager != null) {
      textureManager.clear();
      textureManager = null;
    }
    if (configLoader != null) {
      configLoader.dispose();
      configLoader = null;
    }
    if (planets != null) {
      for (Planet p : planets) p.dispose();
      planets.clear();
      planets = null;
    }
    if (sun != null) {
      sun.dispose();
      sun = null;
    }
    if (physicsEngine != null) {
      physicsEngine.dispose();
      physicsEngine = null;
    }
    if (renderer != null) {
      renderer.dispose();
      renderer = null;
    }
    shapeManager = null;
    shaderManager = null;

    System.out.println("Disposing resources for scene: " + getName());
  }

  public String getName() {
    return "Sistema Solar Físico";
  }
}
