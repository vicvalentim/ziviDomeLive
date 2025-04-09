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
  private int selectedPlanet = -1; // -1 = nenhum, 0 = sol, 1.. = planetas

  private int prevMouseX, prevMouseY;
  private PVector cameraTarget = new PVector(0, 0, 0);

  private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

  Scene1(zividomelive parent, PApplet pApplet) {
    this.parent = parent;
    this.pApplet = pApplet;

    simParams = new SimParams();
    textureManager = new TextureManager(pApplet);
    shapeManager = new ShapeManager(pApplet);
    shaderManager = new ShaderManager(pApplet);

    loadAllShaders();

    configLoader = new ConfigLoader(pApplet, textureManager);
    sun = configLoader.loadSun(); // Novo: carregar o Sol separado
    planets = configLoader.loadConfiguration();

    physicsEngine = new PhysicsEngine(pApplet, planets, sun);  // Passa o objeto sun para o construtor de PhysicsEngine
    renderer = new Renderer(pApplet, planets, configLoader.getSkySphere(), shapeManager, shaderManager);
    renderer.setSun(sun);

    configureCamera();
    startPhysicsThread();
  }

  private void loadAllShaders() {
    //shaderManager.loadShader("planet", "shader/planet.frag", "shader/planet.vert");
    //shaderManager.loadShader("sky", "shader/sky_hdri.frag", "shader/sky_hdri.vert");
    //shaderManager.loadShader("rings", "shader/rings.frag", "shader/rings.vert");
    //shaderManager.loadShader("sun", "shader/sun.frag", "shader/sun.vert");
    pApplet.println("[Scene1] Shaders carregados com sucesso.");
  }

  private void configureCamera() {
    float neptuneCenter = NEPTUNE_DIST * PIXELS_PER_AU;
    float neptuneDrawPos = neptuneCenter + SUN_VISUAL_RADIUS;
    renderer.setCameraDistance(neptuneDrawPos * 1.2f);
  }

  private void startPhysicsThread() {
    Thread physicsThread = new Thread(() -> {
      while (true) {
        rwLock.writeLock().lock();
        try {
          physicsEngine.update(timeScale * 0.1f);
          sun.update(timeScale * 0.1f); // Atualiza rotação do Sol
          updateCameraTarget();
        } finally {
          rwLock.writeLock().unlock();
        }
        try {
          Thread.sleep(10);
        } catch (InterruptedException ignored) {}
      }
    });
    physicsThread.start();
  }

  private void updateCameraTarget() {
    if (selectedPlanet == 0 && sun != null) {
      renderer.updateCameraTarget(sun.getPosition());
    } else if (selectedPlanet > 0 && selectedPlanet - 1 < planets.size()) {
      renderer.updateCameraTarget(planets.get(selectedPlanet - 1).getDrawPosition());
    } else {
      renderer.updateCameraTarget(new PVector(0, 0, 0));
    }
  }

  public void setupScene() {
      rwLock.writeLock().lock();
      try {
          sun = configLoader.loadSun();
          sun.buildShape(pApplet, shapeManager);  // Garantir que o Sol seja desenhado logo no início
          planets = configLoader.loadConfiguration();
          physicsEngine = new PhysicsEngine(pApplet, planets, sun);
          renderer.setSun(sun);
          renderer.setPlanets(planets);
      } finally {
          rwLock.writeLock().unlock();
      }
  }

  public void update() {
    trackSelectedPlanet();
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    rwLock.readLock().lock();
    try {
      shaderManager.setUniform("sun", "time", pApplet.millis() / 1000.0f);
      pg.background(0, 10, 20);
      pg.pushMatrix();
        renderer.setupCamera(pg);
        sun.display(pg, showLabels, shaderManager); // Novo: renderização do Sol
        renderer.drawLighting(pg);
        if (showOrbits) renderer.drawPlanetOrbits(pg);
        renderer.drawPlanetsAndMoons(pg, showLabels, showMoonOrbits, shapeManager, shaderManager);
        //renderer.drawSkySphere(pg);
      pg.popMatrix();
    } finally {
      rwLock.readLock().unlock();
    }
  }

  private void trackSelectedPlanet() {
    if (selectedPlanet == 0 && sun != null) {
      renderer.goTo(sun.getPosition(), renderer.getCameraRotationX(), renderer.getCameraRotationY(), renderer.getCameraDistance());
    } else if (selectedPlanet > 0 && selectedPlanet - 1 < planets.size()) {
      Planet target = planets.get(selectedPlanet - 1);
      renderer.goTo(target.getDrawPosition(), renderer.getCameraRotationX(), renderer.getCameraRotationY(), renderer.getCameraDistance());
    }
  }

  private void changeRenderingMode(int mode) {
    renderer.setRenderingMode(mode);

    if (sun != null) {
      sun.setRenderingMode(mode);
      sun.buildShape(pApplet, shapeManager);
    }

    if (planets != null) {
      for (Planet p : planets) {
        p.setRenderingMode(mode);
        p.buildShape(pApplet, shapeManager);

        for (Moon m : p.moons) {
          m.setRenderingMode(mode);
          m.buildShape(pApplet, shapeManager);
        }
      }
    }
  }

  public void keyEvent(processing.event.KeyEvent event) {
    if (event.getAction() != processing.event.KeyEvent.PRESS) return;

    char key = event.getKey();
    switch (key) {
      case ' ': resetView(); break;

      case 'G': simParams.globalScale *= 1.1f; break;
      case 'g': simParams.globalScale /= 1.1f; break;

      case 'a': simParams.planetAmplification *= 1.1f; break;
      case 'z': simParams.planetAmplification /= 1.1f; break;

      case 'w': changeRenderingMode(0); break; // Wireframe
      case 's': changeRenderingMode(1); break; // Solid
      case 't': changeRenderingMode(2); break; // Textured

      case '+': timeScale *= 1.2f; break;
      case '-': timeScale *= 0.8f; break;

      case 'o': showOrbits = !showOrbits; break;
      case 'l': showLabels = !showLabels; break;
      case 'p': showMoonOrbits = !showMoonOrbits; break;

      default:
        if (Character.isDigit(key)) {
          int num = Character.getNumericValue(key);
          if (num == 1) {
            selectedPlanet = 0; // Tecla 1 → Sol
          } else if (num > 1 && num <= planets.size() + 1) {
            selectedPlanet = num - 1; // Tecla 2+ → planetas
          }
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
        renderer.setCameraRotation(renderer.getCameraRotationX() + dy, renderer.getCameraRotationY() + dx);
        prevMouseX = event.getX();
        prevMouseY = event.getY();
        break;
      case MouseEvent.WHEEL:
        renderer.setCameraDistance(renderer.getCameraDistance() + event.getCount() * 0.01f);
        break;
    }
  }

  private void resetView() {
    renderer.setCameraRotation(-PI / 16, 0);
    renderer.setCameraDistance(500);
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
      sun.dispose(); // Novo: liberar recursos do Sol
      sun = null;
    }
    physicsEngine = null;
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
