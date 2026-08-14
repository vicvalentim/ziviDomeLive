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
  private ziviDomeLive parent;
  private PApplet pApplet;
  private List<Planet> planets;
  private Sun sun;
  private List<CelestialBody> planetaryBodies;

  // ————————————————————————————————
  // Managers
  // ————————————————————————————————
  private TextureManager textureManager;
  private ShapeManager shapeManager;
  private ConfigLoader configLoader;
  private PhysicsEngine physicsEngine;
  private Renderer renderer;
  private OrbitCamera sceneCamera;

  // ————————————————————————————————
  // Parâmetros de visualização
  // ————————————————————————————————
  private boolean showOrbits     = true;
  private boolean showMoonOrbits = true;
  private boolean showLabels     = false;
  private int     selectedPlanet = -1;

  private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

  private SimulatedClock clock; // Novo relógio em dias
  private boolean needsReload = false;
  private boolean debugClockPrint = false;
  private boolean initialized = false;

 
  // ————————————————————————————————
  // Construtor de Scene1
  // ————————————————————————————————
  Scene1(ziviDomeLive parent, PApplet pApplet) {
    this.parent  = parent;
    this.pApplet = pApplet;
  }

  /** Constrói todos os recursos pertencentes à ativação atual da cena. */
  private void initializeSceneResources(boolean resetCamera) {
    // 1) Managers gráficos são criados no Processing thread.
    textureManager = new TextureManager(pApplet);
    shapeManager   = new ShapeManager(pApplet);

    // 2) Carrega Sol e planetas
    configLoader = new ConfigLoader(pApplet, textureManager);
    sun     = configLoader.loadSun();
    planets = configLoader.loadConfiguration();
    if (sun == null || planets == null) {
      throw new IllegalStateException("Não foi possível carregar a configuração do Sistema Solar.");
    }
    configureEnvironmentBackground();

    // configura central bodies
    for (Planet p : planets) {
      p.setCentralBody(sun);
      for (Moon m : p.getMoons()) {
        m.setCentralBody(p);
      }
    }

    // 3) Monta lista de corpos (Sol + planetas + luas) e PhysicsEngine.
    planetaryBodies = new ArrayList<>();
    planetaryBodies.add(sun);
    for (Planet p : planets) {
      planetaryBodies.add(p);
      planetaryBodies.addAll(p.getMoons());
    }
    physicsEngine = new PhysicsEngine(planetaryBodies);

    // 4) Renderer e shapes pertencem à ativação atual.
    renderer = new Renderer(
      pApplet,
      planets,
      shapeManager
    );
    renderer.setSun(sun);
    
    sun.buildShape(pApplet, shapeManager);
    for (Planet p : planets) {
      p.buildShape(pApplet, shapeManager);
      for (Moon m : p.getMoons()) {
        m.buildShape(pApplet, shapeManager);
      }
    }

    // 5) Inicializa o relógio absoluto e propaga até hoje.
    clock = new SimulatedClock();
    setClockToNowUTC();
    propagateSinceJ2000();

    // 6) Configura o serviço de OrbitCamera compartilhado da biblioteca.
    sceneCamera = parent.getSceneCamera();
    sceneCamera.setDistanceLimits(-1e6f, 1e6f);
    sceneCamera.setLerpFactor(0.1f);
    sceneCamera.setDragSensitivity(0.01f);
    sceneCamera.setWheelSteps(80f, 0.001f);
    parent.setSceneCameraInputEnabled(true);

    if (resetCamera) {
      resetView();
    }
    selectedPlanet = -1;
    initialized = true;
  }

  /** Libera recursos da ativação atual sem encerrar serviços globais da biblioteca. */
  private void releaseSceneResources(boolean resetCamera) {
    parent.clearEnvironmentBackground();

    if (renderer != null) renderer.dispose();
    if (physicsEngine != null) physicsEngine.dispose();
    if (planets != null) {
      for (Planet planet : planets) planet.dispose();
    }
    if (sun != null) sun.dispose();
    if (configLoader != null) configLoader.dispose();
    if (shapeManager != null) shapeManager.dispose();
    if (textureManager != null) textureManager.clear();

    renderer = null;
    physicsEngine = null;
    planets = null;
    sun = null;
    configLoader = null;
    shapeManager = null;
    textureManager = null;
    clock = null;
    if (planetaryBodies != null) planetaryBodies.clear();
    planetaryBodies = null;
    initialized = false;

    if (resetCamera && sceneCamera != null) {
      parent.setSceneCameraInputEnabled(false);
      sceneCamera.reset(1500f);
    }
  }

  /**
   * Ajusta o relógio para a data atual UTC.
   * O relógio é ajustado para o instante atual em UTC.
   */
  private void setClockToNowUTC() {
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
    pApplet.println("[Scene1] Data inicial UTC: " + clock.getCalendarUTCString());
  }

  /**
   * Propaga os corpos celestes desde J2000 até o instante atual.
   * Isso é feito para evitar que a simulação comece em J2000.
   */
  private void propagateSinceJ2000() {
    double days = clock.getDaysSinceJ2000();
    if (days <= 0) return;
    float  total   = (float) days;
    float  maxStep = 0.5f;
    int    steps   = (int) Math.ceil(total / maxStep);
    float  dt      = total / steps;
    for (int i = 0; i < steps; i++) {
      physicsEngine.update(dt);
    }
  }

  public void setupScene() {
    rwLock.writeLock().lock();
    try {
      if (!initialized) {
        initializeSceneResources(true);
      }
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  private void reloadScene() {
    rwLock.writeLock().lock();
    try {
      releaseSceneResources(false);
      initializeSceneResources(false);
      needsReload = false;
      pApplet.println("[Scene1] Recursos recarregados com sucesso.");
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  // ————————————————————————————————
  // update() — agora baseado em double do relógio
  // ————————————————————————————————
  @Override
  public void update() {
    if (needsReload) {
      reloadScene();
    }

    rwLock.writeLock().lock();
    try {
      if (!initialized) return;
      // 1) Δt em dias simulados desde o último frame
      double totalDt = clock.update();
      if (totalDt > 0.0) {
        // subdivide em subpassos ≤ 0.5 dias
        double maxStep = 0.5;
        int steps = (int) Math.ceil(totalDt / maxStep);
        double dt = totalDt / steps;
        for (int i = 0; i < steps; i++) {
          physicsEngine.update((float) dt);
        }
        // rotação axial visual de Sol, planetas e luas
        sun.update((float) totalDt);
        for (Planet p : planets) {
          p.update((float) totalDt);
          for (Moon m : p.getMoons()) {
            m.update((float) totalDt);
          }
        }
      }

      // 2) Câmera: escolhe o novo alvo em px
      float scale = pxPerAU();
      PVector newTarget;
      if (selectedPlanet == 0) {
        newTarget = sun.getPositionAU().copy().mult(scale);
      } else if (selectedPlanet > 0 && selectedPlanet <= planets.size()) {
        newTarget = planets
          .get(selectedPlanet - 1)
          .getPositionAU()
          .copy()
          .mult(scale);
      } else {
        newTarget = new PVector(0, 0, 0);
      }

      // 3) Atualiza somente o alvo. Orientação, distância e suavização pertencem à API.
      sceneCamera.setTarget(newTarget);

    } finally {
      rwLock.writeLock().unlock();
    }
  }


  // ————————————————————————————————
  // render() — renderiza o frame atual
  // ————————————————————————————————
  public void sceneRender(PGraphicsOpenGL pg) {
    rwLock.readLock().lock();
    try {
      if (!initialized) return;
      pg.background(0, 10, 20);
      if (debugClockPrint) {
        println(clock.getCalendarUTCString());
      }
      pg.pushMatrix();
      sceneCamera.apply(pg);

        // 1) Sol
        pg.noLights();
        PVector sunPx = sun.getPositionAU().copy()
                          .mult(pxPerAU());
        pg.pushMatrix();
          pg.translate(sunPx.x, sunPx.y, sunPx.z);
          sun.display(pg, showLabels);
        pg.popMatrix();

        // 2) iluminação
        renderer.drawLighting(pg);

        // 3) Órbitas de planetas
        if (showOrbits) {
          renderer.drawPlanetOrbits(pg);
        }

        // 4) Planetas + luas (com órbitas de luas via m.displayOrbit())
        renderer.drawPlanetsAndMoons(pg, showLabels, showMoonOrbits);

      pg.popMatrix();
    } finally {
      rwLock.readLock().unlock();
    }
  }
  
  // ————————————————————————————————
  // Funções auxiliares
  // ————————————————————————————————
  private void configureEnvironmentBackground() {
    PImage environmentTexture = configLoader.getSkyTexture();
    parent.setEquirectangularBackground(environmentTexture);
    if (environmentTexture != null) {
      parent.setEnvironmentBackgroundIntensity(1.5f);
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

  /**
   * Converte a distância em AU para pixels na tela.
   * @return Fator de conversão de AU para pixels
   */
  private void applyScalingFactors() {
    rwLock.writeLock().lock();
    try {
      // Sol
      if (sun != null) sun.applyScalingFactors();

      // Planetas e luas
      for (Planet p : planets) {
        p.applyScalingFactors();
        // reconstrói o PShape do planeta (opcional, só se quiser limpar cache)
        p.buildShape(pApplet, shapeManager);

        for (Moon m : p.getMoons()) {
          // isso recalcula m.radiusPx = parent.radiusPx * (m.radiusAU/parent.radiusAU)
          m.applyScalingFactors();
          // e limpa o cache da forma para usar o novo size
          m.buildShape(pApplet, shapeManager);
        }
      }
    } finally {
      rwLock.writeLock().unlock();
    }
  }
  
  /**
   * Função de callback para eventos de teclado.
   * @param event Evento de teclado
   */ 
  public void keyEvent(processing.event.KeyEvent event) {
    if (event.getAction() != processing.event.KeyEvent.PRESS) return;
    if (!initialized) return;
    char key = event.getKey();
    switch (key) {
      case ' ': resetView(); break;
      case 'G': globalScale *= 1.1f; applyScalingFactors(); break;
      case 'g': globalScale /= 1.1f; applyScalingFactors(); break;
      case 'A': planetAmplification *= 1.1f; applyScalingFactors(); break;
      case 'a': planetAmplification /= 1.1f; applyScalingFactors(); break;
      case 'B': bodyScale   *= 1.1f;   applyScalingFactors(); break;
      case 'b': bodyScale   /= 1.1f;   applyScalingFactors(); break;
      case 'r': globalScale = 1.0f; planetAmplification = 1.0f; bodyScale = 1.0f; applyScalingFactors(); pApplet.println("[Scene1] Escalas resetadas."); break;
      case 'R': needsReload = true; pApplet.println("[Scene1] Reset Geral"); break;
      case 'w': changeRenderingMode(0); break;
      case 's': changeRenderingMode(1); break;
      case 't': changeRenderingMode(2); break;
      case '+': clock.setTimeScale(clock.getTimeScale() * 1.2); break;
      case '-': clock.setTimeScale(clock.getTimeScale() * 0.8); break;
      case 'o': showOrbits = !showOrbits; break;
      case 'l': showLabels = !showLabels; break;
      case 'p': showMoonOrbits = !showMoonOrbits; break;
      case 'n':
        debugClockPrint = !debugClockPrint;
        pApplet.println("[Scene1] Debug do relógio UTC " + (debugClockPrint ? "ativado" : "desativado") + ".");
        break;
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

  /**
  * Reseta a câmera para a posição inicial, olhar para o Sol
  * mas mais afastado, usando NEPTUNE_DIST.
  */
  private void resetView() {
      Quaternion orientation = Quaternion.fromAxisAngle(
        new PVector(1, 0, 0),
        PI / 16f
      );
      float dist = -NEPTUNE_DIST * pxPerAU();
      sceneCamera.snapTo(new PVector(0, 0, 0), orientation, dist);
  }

  /**
   * Limpa todos os recursos utilizados pela cena.
   */
  public void dispose() {
    rwLock.writeLock().lock();
    try {
      releaseSceneResources(true);
      needsReload = false;
      pApplet.println("[Scene1] Recursos liberados.");
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  /**
   * Retorna o nome da cena.
   * @return Nome da cena
   */
  public String getName() {
    return "Sistema Solar Físico";
  }
}
