import java.util.concurrent.locks.ReentrantReadWriteLock;
import processing.opengl.*;
import java.util.*;
import javax.swing.JOptionPane;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

/**
 * Scene1 — integra ConfigLoader → PhysicsEngine → Renderer com SimulatedClock.
 */
class Scene1 implements Scene {
  private zividomelive parent;
  private PApplet pApplet;
  private List<Planet> planets;
  private Sun sun;
  private List<CelestialBody> planetaryBodies;

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

 
  // ————————————————————————————————
  // Construtor de Scene1
  // ————————————————————————————————
  Scene1(zividomelive parent, PApplet pApplet) {
      this.parent   = parent;
      this.pApplet  = pApplet;

      // 1) Inicializa managers e params...
      simParams      = new SimParams();
      textureManager = new TextureManager(pApplet);
      shapeManager   = new ShapeManager(pApplet);
      shaderManager  = new ShaderManager(pApplet);
      loadAllShaders();

      // 2) Carrega Sol, planetas e configura central bodies
      configLoader = new ConfigLoader(pApplet, textureManager, simParams);
      configLoader.sendTexturesToShaderManager(shaderManager);
      sun     = configLoader.loadSun();
      planets = configLoader.loadConfiguration();
      for (Planet p : planets) {
        p.setCentralBody(sun);
        for (Moon m : p.getMoons()) {
          m.setCentralBody(p);
        }
      }

      // 3) Monta lista de corpos (Sol + planetas + luas) e PhysicsEngine
      planetaryBodies = new ArrayList<>();
      planetaryBodies.add(sun);
      for (Planet p : planets) {
        planetaryBodies.add(p);
        for (Moon m : p.getMoons()) {
          planetaryBodies.add(m);
        }
      }
      physicsEngine = new PhysicsEngine(planetaryBodies);

      // 4) Renderer
      renderer = new Renderer(pApplet, planets, configLoader.getSkySphere(),
                              shapeManager, shaderManager, simParams);
      renderer.setSun(sun);

      // 5) Relógio absoluto e inicialização para “hoje” UTC
      clock = new SimulatedClock();

      Instant now = Instant.now();
      ZonedDateTime utc = now.atZone(ZoneOffset.UTC);
      clock.setCalendarUTC(
        utc.getYear(),
        utc.getMonthValue(),
        utc.getDayOfMonth(),
        utc.getHour(),
        utc.getMinute(),
        utc.getSecond() + utc.get(ChronoField.MILLI_OF_SECOND)/1000.0
      );

      // 6) Propaga todos os corpos desde J2000 até a data/hora escolhida
      double daysSinceJ2000 = clock.getDaysSinceJ2000();
      if (daysSinceJ2000 > 0) {
        float total = (float) daysSinceJ2000;
        float maxStep = 0.5f;
        int   steps   = (int) Math.ceil(total / maxStep);
        float dt      = total / steps;
        for (int i = 0; i < steps; i++) {
          physicsEngine.update(dt);
        }
      pApplet.println("[Scene1] Data inicial UTC: " + clock.getCalendarUTCString());
      }

      // 7) Ajusta a câmera
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

      // central bodies
      planetaryBodies.clear();
      planetaryBodies.add(sun);
      for (Planet p : planets) {
        p.setCentralBody(sun);
        planetaryBodies.add(p);
        for (Moon m : p.getMoons()) {
          m.setCentralBody(p);
          planetaryBodies.add(m);
        }
      }

      // reinicia o engine COM luas
      physicsEngine = new PhysicsEngine(planetaryBodies);

      // renderer
      renderer.setSun(sun);
      renderer.setPlanets(planets);

      // rebuild shapes
      sun.buildShape(pApplet, shapeManager);
      for (Planet p : planets) {
        p.buildShape(pApplet, shapeManager);
        for (Moon m : p.getMoons()) {
          m.buildShape(pApplet, shapeManager);
        }
      }

      // reaplica escalas (planetas + luas)
      applyScalingFactors();
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  // ————————————————————————————————
  // update() — agora baseado em double do relógio
  // ————————————————————————————————
  @Override
  public void update() {
    rwLock.writeLock().lock();
    try {
      // Δt em dias simulados desde o último frame
      double totalDt = clock.update();
      if (totalDt > 0.0) {
        // subdivide em subpassos ≤ 0.5d
        double maxStep = 0.5;
        int steps = (int)Math.ceil(totalDt / maxStep);
        double dt = totalDt / steps;
        for (int i = 0; i < steps; i++) {
          physicsEngine.update((float)dt);
        }
        // rotação axial visual
        sun.update((float)totalDt);
        for (Planet p : planets) {
          p.update((float)totalDt);
        }
      }
      updateCameraTarget();
      trackSelectedPlanet();
    } finally {
      rwLock.writeLock().unlock();
    }
  }


  /**
   * Atualiza o alvo da câmera com base no planeta selecionado.
   * Se nenhum planeta estiver selecionado, a câmera foca no Sol.
   */
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
      pg.fill(255);
      pg.textSize(12);
      pg.text(clock.getCalendarUTCString(), 10, 20);
      pg.pushMatrix();
        renderer.setupCamera(pg);

        // 1) Sol
        PVector sunPx = sun.getPositionAU().copy()
                          .mult(PIXELS_PER_AU * simParams.globalScale);
        pg.pushMatrix();
          pg.translate(sunPx.x, sunPx.y, sunPx.z);
          sun.display(pg, showLabels, shaderManager);
        pg.popMatrix();

        // 2) iluminação
        renderer.drawLighting(pg);

        // 3) Órbitas de planetas
        if (showOrbits) {
          renderer.drawPlanetOrbits(pg);
        }

        // 4) Planetas + luas (com órbitas de luas via m.displayOrbit())
        renderer.drawPlanetsAndMoons(pg, showLabels, showMoonOrbits);

        // 5) Céu
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
      if (sun != null) sun.applyScalingFactors(simParams);
      if (planets != null) {
        for (Planet p : planets) {
          p.applyScalingFactors(simParams);
          for (Moon m : p.getMoons()) {
            m.applyScalingFactors(simParams);
          }
        }
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
      case 'G': simParams.globalScale *= 1.1f; simParams.planetAmplification = 1; applyScalingFactors(); break;
      case 'g': simParams.globalScale /= 1.1f; simParams.planetAmplification = 1; applyScalingFactors(); break;
      case 'a': simParams.planetAmplification *= 1.1f; applyScalingFactors(); break;
      case 'z': simParams.planetAmplification /= 1.1f; applyScalingFactors(); break;
      case 'r': simParams.globalScale = 1; simParams.planetAmplification = 1; applyScalingFactors(); pApplet.println("[Scene1] Escalas resetadas."); break;
      case 'w': changeRenderingMode(0); break;
      case 's': changeRenderingMode(1); break;
      case 't': changeRenderingMode(2); break;
      case '+': clock.setTimeScale(clock.getTimeScale() * 1.2); break;
      case '-': clock.setTimeScale(clock.getTimeScale() * 0.8); break;
      case 'o': showOrbits = !showOrbits; break;
      case 'l': showLabels = !showLabels; break;
      case 'p': showMoonOrbits = !showMoonOrbits; break;
      case 'D': String input = JOptionPane.showInputDialog(
      "Data UTC (AAAA-MM-DD HH:MM:SS):"
    );
    if (input != null) {
      try {
        // 1) parse da string
        String[] sp = input.trim().split("\\s+");
        String[] d = sp[0].split("-");
        String[] t = sp[1].split(":");
        int Y = Integer.parseInt(d[0]),
            M = Integer.parseInt(d[1]),
           Dd = Integer.parseInt(d[2]),
            h = Integer.parseInt(t[0]),
            m = Integer.parseInt(t[1]);
        double s = Double.parseDouble(t[2]);

        // 2) ajusta o relógio
        clock.setCalendarUTC(Y, M, Dd, h, m, s);

        // 3) reset a todos os corpos para J2000
        for (Planet p : planets) {
          p.resetToJ2000();
          for (Moon m_ : p.getMoons()) {
            m_.resetToJ2000();
          }
        }

        // 4) propaga até o novo instante
        float days0 = (float)clock.getDaysSinceJ2000();
        physicsEngine.setEnablePerturbations(false);
        if (days0 > 0) {
          // subdivida em subpassos como no update()
          float maxStep = 0.5f;
          int steps = (int)Math.ceil(days0 / maxStep);
          float dt    = days0 / steps;
          for (int i = 0; i < steps; i++) {
            physicsEngine.update(dt);
          }
        }
        physicsEngine.setEnablePerturbations(true);

      } catch (Exception ex) {
        pApplet.println("Data inválida: " + ex.getMessage());
      }
    }
    pApplet.println("[Scene1] Data alterada UTC: " + clock.getCalendarUTCString());
    break;
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
