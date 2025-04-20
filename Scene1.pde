import java.util.concurrent.locks.ReentrantReadWriteLock;
import processing.opengl.*;
import java.util.*;

/**
 * Scene1 — integra ConfigLoader → PhysicsEngine → Renderer com SimulatedClock.
 */
class Scene1 implements Scene {
  private zividomelive parent;
  private PApplet pApplet;
  private List<Planet> planets;
  private Sun sun;
  private List<CelestialBody> bodies;  // Sol + planetas + luas

  private SimParams simParams;
  private TextureManager textureManager;
  private ShaderManager shaderManager;
  private ShapeManager shapeManager;
  private ConfigLoader configLoader;
  private PhysicsEngine physicsEngine;
  private Renderer renderer;

  private boolean showOrbits     = true;
  private boolean showMoonOrbits = true;
  private boolean showLabels     = false;
  private int     selectedPlanet = -1;

  private int prevMouseX, prevMouseY;
  private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

  private SimulatedClock clock; // Novo relógio em dias

  Scene1(zividomelive parent, PApplet pApplet) {
    this.parent = parent;
    this.pApplet = pApplet;

    // 1) Inicializa managers e params
    simParams      = new SimParams();
    textureManager = new TextureManager(pApplet);
    shapeManager   = new ShapeManager(pApplet);
    shaderManager  = new ShaderManager(pApplet);
    loadAllShaders();

    // 2) Configurações físicas (AU, dias)
    configLoader = new ConfigLoader(pApplet, textureManager, simParams);
    configLoader.sendTexturesToShaderManager(shaderManager);
    sun     = configLoader.loadSun();
    planets = configLoader.loadConfiguration();

    for (Planet p : planets) {
    p.setCentralBody(sun);
    }

    // 3) Monta lista de corpos para a física
    bodies = new ArrayList<>();
    bodies.add(sun);
    for (Planet p : planets) {
      bodies.add(p);
      bodies.addAll(p.getMoons());
    }
    physicsEngine = new PhysicsEngine(bodies);

    // 4) Renderer: converte AU→px apenas aqui
    renderer = new Renderer(pApplet, planets, configLoader.getSkySphere(),
                            shapeManager, shaderManager, simParams);
    renderer.setSun(sun);

    // 5) Relógio simulado (dias)
    clock = new SimulatedClock(0.0f, 1.0f);

    configureCamera();
  }

  private void loadAllShaders() {
    pApplet.println("[Scene1] Iniciando carregamento de shaders...");

    boolean allShadersOk = true;

    //allShadersOk &= tryLoadShader("planet", "planet.frag", "common.vert");
    //allShadersOk &= tryLoadShader("sun", "sun.frag", "common.vert");
    //allShadersOk &= tryLoadShader("rings", "rings.frag", "common.vert");
    //allShadersOk &= tryLoadShader("sky_hdri", "sky_hdri.frag", "sky_hdri.vert");

    if (allShadersOk) {
      pApplet.println("[Scene1] Todos os shaders carregados com sucesso.");
    } else {
      pApplet.println("[Scene1] ⚠️ Nem todos os shaders foram carregados corretamente. Verifique o log acima.");
    }
  }

  // Função auxiliar com tratamento de exceções
  private boolean tryLoadShader(String name, String fragFile, String vertFile) {
    try {
      shaderManager.loadShader(name, fragFile, vertFile);
      pApplet.println("[Shader] ✔ '" + name + "' carregado.");
      return true;
    } catch (Exception e) {
      pApplet.println("[Shader] ❌ Falha ao carregar '" + name + "': " + e.getMessage());
      return false;
    }
  }

  private void configureCamera() {
    // distância inicial baseada em Netuno
    float dist = NEPTUNE_DIST
               * PIXELS_PER_AU
               * simParams.globalScale
               * 1.2f;
    renderer.setCameraDistance(dist);
  }

  /** Recarrega tudo em runtime */
  public void setupScene() {
    rwLock.writeLock().lock();
    try {
      sun     = configLoader.loadSun();
      planets = configLoader.loadConfiguration();

      // 1) refresca os "central bodies"
      for (Planet p : planets) {
        p.setCentralBody(sun);
        for (Moon m : p.getMoons()) {
          m.setCentralBody(p);
        }
      }

      // 2) monta o array de corpos para a física
      bodies.clear();
      bodies.add(sun);
      for (Planet p : planets) {
        bodies.add(p);
        bodies.addAll(p.getMoons());
      }
      physicsEngine = new PhysicsEngine(bodies);

      // 3) renderer
      renderer.setSun(sun);
      renderer.setPlanets(planets);

      // 4) rebuild das shapes
      sun.buildShape(pApplet, shapeManager);
      for (Planet p : planets) {
        p.buildShape(pApplet, shapeManager);
        for (Moon m : p.getMoons()) {
          m.buildShape(pApplet, shapeManager);
        }
      }

      // 5) reaplica escalas visuais (zoom / amplificação)
      applyScalingFactors();
    } finally {
      rwLock.writeLock().unlock();
    }
  }
  
  /**
  * Atualiza a simulação subdividindo grandes passos de tempo em subpassos menores.
  */
  @Override
  public void update() {
    rwLock.writeLock().lock();
    try {
      float totalDt = clock.update();
      if (totalDt > 0f) {
        // Divide totalDt em subpassos para manter estabilidade da órbita
        float maxStep = 0.5f; // dias máximos por subpasso
        int steps    = (int) Math.ceil(totalDt / maxStep);
        float dt     = totalDt / steps;
        for (int i = 0; i < steps; i++) {
          physicsEngine.update(dt);
        }

        // Atualiza rotações visuais com o total do tempo
        sun.update(totalDt);
        for (Planet p : planets) {
          p.update(totalDt);
          for (Moon m : p.getMoons()) {
            m.update(totalDt);
          }
        }
      }

      updateCameraTarget();
      trackSelectedPlanet();
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  private void updateCameraTarget() {
    float scale = PIXELS_PER_AU * simParams.globalScale;
    if (selectedPlanet == 0) {
      // mira no Sol
      PVector sunPx = sun.getPositionAU().copy().mult(scale);
      renderer.updateCameraTarget(sunPx);
    } else if (selectedPlanet > 0 && selectedPlanet <= planets.size()) {
      Planet p = planets.get(selectedPlanet - 1);
      PVector tgtPx = p.getPositionAU().copy().mult(scale);
      renderer.updateCameraTarget(tgtPx);
    } else {
      renderer.updateCameraTarget(new PVector(0, 0, 0));
    }
  }

  private void trackSelectedPlanet() {
    float scale = PIXELS_PER_AU * simParams.globalScale;
    if (selectedPlanet == 0) {
      PVector sunPx = sun.getPositionAU().copy().mult(scale);
      renderer.goTo(sunPx,
                    renderer.getCameraRotationX(),
                    renderer.getCameraRotationY(),
                    renderer.getCameraDistance());
    } else if (selectedPlanet > 0 && selectedPlanet <= planets.size()) {
      Planet p = planets.get(selectedPlanet - 1);
      PVector tgtPx = p.getPositionAU().copy().mult(scale);
      renderer.goTo(tgtPx,
                    renderer.getCameraRotationX(),
                    renderer.getCameraRotationY(),
                    renderer.getCameraDistance());
    }
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    rwLock.readLock().lock();
    try {
      pg.background(0, 10, 20);
      pg.pushMatrix();
        renderer.setupCamera(pg);

        // Sol
        PVector sunPx = sun.getPositionAU().copy()
                          .mult(PIXELS_PER_AU * simParams.globalScale);
        pg.pushMatrix();
          pg.translate(sunPx.x, sunPx.y, sunPx.z);
          sun.display(pg, showLabels, shaderManager);
        pg.popMatrix();

        renderer.drawLighting(pg);

        // Órbitas de planetas
        if (showOrbits) {
          renderer.drawPlanetOrbits(pg);
        }
        // Órbitas de luas (independente)
        if (showMoonOrbits) {
          renderer.drawMoonOrbits(pg);
        }

        // Corpos
        renderer.drawPlanetsAndMoons(pg, showLabels, /* showMoonOrbits já só ativa labels/luas */ false);

        renderer.drawSkySphere(pg);
      pg.popMatrix();
    } finally {
      rwLock.readLock().unlock();
    }
  }

  private void changeRenderingMode(int mode) {
    renderer.setRenderingMode(mode);
    sun.setRenderingMode(mode);
    sun.buildShape(pApplet, shapeManager);
    for (Planet p : planets) {
      p.setRenderingMode(mode);
      p.buildShape(pApplet, shapeManager);
      for (Moon m : p.getMoons()) {
        m.setRenderingMode(mode);
        m.buildShape(pApplet, shapeManager);
      }
    }
  }

  private void applyScalingFactors() {
    rwLock.writeLock().lock();
    try {
      if (sun != null)  sun.applyScalingFactors(simParams);
      if (planets!=null) for (Planet p : planets) p.applyScalingFactors(simParams);
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  public void keyEvent(processing.event.KeyEvent event) {
    if (event.getAction() != processing.event.KeyEvent.PRESS) return;
    char key = event.getKey();
    switch (key) {
      case ' ': resetView(); break;
      case 'G': simParams.globalScale *= 1.1f; simParams.planetAmplification = 1; applyScalingFactors(); break;
      case 'g': simParams.globalScale /= 1.1f; simParams.planetAmplification = 1; applyScalingFactors(); break;
      case 'a': simParams.planetAmplification *= 1.1f; applyScalingFactors(); break;
      case 'z': simParams.planetAmplification /= 1.1f; applyScalingFactors(); break;
      case 'r': simParams.globalScale = 1; simParams.planetAmplification = 1; applyScalingFactors(); pApplet.println("[Scene1] Escalas resetadas."); break;
      case 'w': changeRenderingMode(0); break;
      case 's': changeRenderingMode(1); break;
      case 't': changeRenderingMode(2); break;
      case '+': clock.setTimeScale(clock.getTimeScale()*1.2f); break;
      case '-': clock.setTimeScale(clock.getTimeScale()*0.8f); break;
      case 'o': showOrbits = !showOrbits; break;
      case 'l': showLabels = !showLabels; break;
      case 'p': showMoonOrbits = !showMoonOrbits; break;
      default:
        if (Character.isDigit(key)) {
          int n = Character.getNumericValue(key);
          selectedPlanet = (n>=1 && n<=planets.size()+1) ? n-1 : selectedPlanet;
        }
    }
  }

  public void mouseEvent(processing.event.MouseEvent event) {
    switch (event.getAction()) {
      case MouseEvent.PRESS:
        prevMouseX = event.getX();
        prevMouseY = event.getY();
        break;
      case MouseEvent.DRAG:
        float dx = (event.getX()-prevMouseX)*0.01f;
        float dy = (event.getY()-prevMouseY)*0.01f;
        renderer.setCameraRotation(
          renderer.getCameraRotationX()+dy,
          renderer.getCameraRotationY()+dx
        );
        prevMouseX = event.getX();
        prevMouseY = event.getY();
        break;
      case MouseEvent.WHEEL:
        float scroll = event.getCount();
        boolean isPad = Math.abs(scroll)<1;
        float zoom = isPad ? scroll*0.01f : scroll*2f;
        renderer.setCameraDistance(
          PApplet.constrain(renderer.getCameraDistance()+zoom, -1e10f, 1e10f)
        );
        break;
    }
  }

  private void resetView() {
    renderer.setCameraRotation(PI/16, 0);
    renderer.setCameraDistance(20);
  }

  public void dispose() {
    textureManager.clear();
    configLoader.dispose();
    for (Planet p:planets) p.dispose();
    sun.dispose();
    physicsEngine.dispose();
    renderer.dispose();
    System.out.println("Disposed Scene1");
  }

  public String getName() {
    return "Sistema Solar Físico";
  }
}
