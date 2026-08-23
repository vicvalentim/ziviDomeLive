import processing.opengl.*;
import java.util.*;
import javax.swing.JOptionPane;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

/**
 * Scene1 — domínio astronômico apoiado pelos serviços de cena da ziviDomeLive.
 */
class Scene1 implements Scene {
  private SceneServices services;
  private PApplet pApplet;
  private List<Planet> planets;
  private Sun sun;
  private List<CelestialBody> planetaryBodies;

  private ShapeManager shapeManager;
  private ConfigLoader configLoader;
  private PhysicsEngine physicsEngine;
  private Renderer renderer;
  private OrbitCamera sceneCamera;
  private SimulatedClock clock;

  private boolean showOrbits     = true;
  private boolean showMoonOrbits = true;
  private boolean showLabels     = false;
  private int selectedPlanet = -1;
  private boolean debugClockPrint = false;
  private boolean initialized = false;
  private final PVector frameSunPosition = new PVector();
  private final PVector trackedCameraTarget = new PVector();

  @Override
  public void configure(SceneServices services) {
    this.services = services;
    this.pApplet = services.applet();
  }

  @Override
  public void setupScene() {
    if (services == null) {
      throw new IllegalStateException("SceneServices não foi configurado.");
    }
    initializeSceneResources();
    configureActions();
  }

  /** Constrói recursos de domínio; assets, câmera e Environment pertencem à API. */
  private void initializeSceneResources() {
    shapeManager = new ShapeManager(pApplet, services.assets());
    configLoader = new ConfigLoader(pApplet, services.assets());

    sun = configLoader.loadSun();
    planets = configLoader.loadConfiguration();
    if (sun == null || planets == null) {
      throw new IllegalStateException("Não foi possível carregar a configuração do Sistema Solar.");
    }
    configureEnvironmentBackground();

    for (Planet planet : planets) {
      planet.setCentralBody(sun);
      for (Moon moon : planet.getMoons()) {
        moon.setCentralBody(planet);
      }
    }

    planetaryBodies = new ArrayList<>();
    planetaryBodies.add(sun);
    for (Planet planet : planets) {
      planetaryBodies.add(planet);
      planetaryBodies.addAll(planet.getMoons());
    }
    physicsEngine = new PhysicsEngine(planetaryBodies);

    renderer = new Renderer(pApplet, planets, shapeManager);
    renderer.setSun(sun);
    sun.buildShape(pApplet, shapeManager);
    for (Planet planet : planets) {
      planet.buildShape(pApplet, shapeManager);
      for (Moon moon : planet.getMoons()) {
        moon.buildShape(pApplet, shapeManager);
      }
    }

    // O SimulatedClock reduz este passo junto com a escala de tempo para evitar
    // translação quantizada em velocidades baixas.
    services.timeline().setFixedStep(1.0 / 120.0);
    services.timeline().setMaxSubSteps(8);
    services.timeline().setRate(1.0); // um dia simulado por segundo real
    clock = new SimulatedClock(services.timeline());
    setClockToNowUTC();
    propagateSinceJ2000();

    sceneCamera = services.camera().orbit();
    sceneCamera.setDistanceLimits(-1e6f, 1e6f);
    sceneCamera.setLerpFactor(0.1f);
    // Mantém a manipulação direta da versão 1.5.0: mouse imediato, alvo rastreado suave.
    sceneCamera.setDragSensitivity(0.01f);
    services.camera().setInputEnabled(true);
    services.camera().trackTarget(this::resolveCameraTarget);

    resetView();
    selectedPlanet = -1;
    initialized = true;
    publishFrameState();
  }

  private void configureEnvironmentBackground() {
    PImage environmentTexture = configLoader.getSkyTexture();
    services.environment().setEquirectangular(environmentTexture);
    if (environmentTexture != null) {
      services.environment().setIntensity(1.5f);
    }
  }

  private void setClockToNowUTC() {
    Instant now = Instant.now();
    ZonedDateTime utc = now.atZone(ZoneOffset.UTC);
    clock.setCalendarUTC(
      utc.getYear(),
      utc.getMonthValue(),
      utc.getDayOfMonth(),
      utc.getHour(),
      utc.getMinute(),
      utc.getSecond() + utc.get(ChronoField.MILLI_OF_SECOND) / 1000.0
    );
    pApplet.println("[Scene1] Data inicial UTC: " + clock.getCalendarUTCString());
  }

  private void propagateSinceJ2000() {
    propagateFromJ2000(clock.getDaysSinceJ2000(), true);
  }

  private void propagateFromJ2000(double days, boolean perturbations) {
    if (days <= 0.0) return;
    physicsEngine.setEnablePerturbations(perturbations);
    double maxStep = 0.5;
    int steps = (int) Math.ceil(days / maxStep);
    double dt = days / steps;
    for (int i = 0; i < steps; i++) {
      physicsEngine.update(dt);
    }
    physicsEngine.setEnablePerturbations(true);
  }

  @Override
  public void update() {
    if (!initialized) return;
    services.timeline().advance(
      services.frameClock().getDeltaSeconds(),
      this::updateSimulation
    );
    publishFrameState();
    if (debugClockPrint) {
      pApplet.println(clock.getCalendarUTCString());
    }
  }

  private void updateSimulation(double dtDays) {
    physicsEngine.update(dtDays);
    sun.update(dtDays);
    for (Planet planet : planets) {
      planet.update(dtDays);
      for (Moon moon : planet.getMoons()) {
        moon.update(dtDays);
      }
    }
  }

  private PVector resolveCameraTarget() {
    trackedCameraTarget.set(0f, 0f, 0f);
    if (!initialized) return trackedCameraTarget;
    float scale = pxPerAU();
    if (selectedPlanet == 0) {
      trackedCameraTarget.set(sun.getPositionAU()).mult(scale);
      return trackedCameraTarget;
    }
    if (selectedPlanet > 0 && selectedPlanet <= planets.size()) {
      trackedCameraTarget.set(planets.get(selectedPlanet - 1).getPositionAU()).mult(scale);
    }
    return trackedCameraTarget;
  }

  private void publishFrameState() {
    frameSunPosition.set(sun.getPositionAU()).mult(pxPerAU());
  }

  @Override
  public void sceneRender(PGraphicsOpenGL pg) {
    if (!initialized) return;
    pg.background(0, 10, 20);

    pg.pushMatrix();
    services.camera().apply(pg);

      pg.noLights();
      pg.pushMatrix();
        pg.translate(frameSunPosition.x, frameSunPosition.y, frameSunPosition.z);
        sun.display(pg, showLabels);
      pg.popMatrix();

      renderer.drawLighting(pg);
      if (showOrbits) {
        renderer.drawPlanetOrbits(pg);
      }
      renderer.drawPlanetsAndMoons(pg, showLabels, showMoonOrbits);

    pg.popMatrix();
  }

  private void configureActions() {
    SceneActionMap actions = services.actions();
    actions.bindKeyPressed("camera.reset", ' ', this::resetView);
    actions.bindKeyPressed("scale.global.increase", 'G', () -> { globalScale *= 1.1f; applyScalingFactors(); });
    actions.bindKeyPressed("scale.global.decrease", 'g', () -> { globalScale /= 1.1f; applyScalingFactors(); });
    actions.bindKeyPressed("scale.planets.increase", 'A', () -> { planetAmplification *= 1.1f; applyScalingFactors(); });
    actions.bindKeyPressed("scale.planets.decrease", 'a', () -> { planetAmplification /= 1.1f; applyScalingFactors(); });
    actions.bindKeyPressed("scale.bodies.increase", 'B', () -> { bodyScale *= 1.1f; applyScalingFactors(); });
    actions.bindKeyPressed("scale.bodies.decrease", 'b', () -> { bodyScale /= 1.1f; applyScalingFactors(); });
    actions.bindKeyPressed("scale.reset", 'r', this::resetScales);
    actions.bindKeyPressed("scene.reload", 'R', () -> {
      services.requestReload();
      pApplet.println("[Scene1] Reload solicitado.");
    });
    actions.bindKeyPressed("render.wireframe", 'w', () -> changeRenderingMode(0));
    actions.bindKeyPressed("render.solid", 's', () -> changeRenderingMode(1));
    actions.bindKeyPressed("render.textured", 't', () -> changeRenderingMode(2));
    actions.bindKeyPressed("time.faster", '+', () -> clock.setTimeScale(clock.getTimeScale() * 1.2));
    actions.bindKeyPressed("time.slower", '-', () -> clock.setTimeScale(clock.getTimeScale() * 0.8));
    actions.bindKeyPressed("orbits.toggle", 'o', () -> showOrbits = !showOrbits);
    actions.bindKeyPressed("labels.toggle", 'l', () -> showLabels = !showLabels);
    actions.bindKeyPressed("moon-orbits.toggle", 'p', () -> showMoonOrbits = !showMoonOrbits);
    actions.bindKeyPressed("clock.debug", 'n', this::toggleClockDebug);
    actions.bindKeyPressed("clock.set-date", 'D', this::promptForDate);

    for (int digit = 1; digit <= 9; digit++) {
      final int selection = digit - 1;
      actions.bindKeyPressed("target." + digit, Character.forDigit(digit, 10), () -> {
        if (selection <= planets.size()) selectedPlanet = selection;
      });
    }
  }

  private void resetScales() {
    globalScale = 1.0f;
    planetAmplification = 1.0f;
    bodyScale = 1.0f;
    applyScalingFactors();
    pApplet.println("[Scene1] Escalas resetadas.");
  }

  private void toggleClockDebug() {
    debugClockPrint = !debugClockPrint;
    pApplet.println("[Scene1] Debug do relógio UTC " +
      (debugClockPrint ? "ativado" : "desativado") + ".");
  }

  private void promptForDate() {
    String input = JOptionPane.showInputDialog("Data UTC (AAAA-MM-DD HH:MM:SS):");
    if (input == null) return;
    try {
      String[] parts = input.trim().split("\\s+");
      String[] date = parts[0].split("-");
      String[] time = parts[1].split(":");
      int year = Integer.parseInt(date[0]);
      int month = Integer.parseInt(date[1]);
      int day = Integer.parseInt(date[2]);
      int hour = Integer.parseInt(time[0]);
      int minute = Integer.parseInt(time[1]);
      double second = Double.parseDouble(time[2]);

      clock.setCalendarUTC(year, month, day, hour, minute, second);
      resetBodiesToJ2000();
      propagateFromJ2000((float) clock.getDaysSinceJ2000(), false);
      pApplet.println("[Scene1] Data alterada UTC: " + clock.getCalendarUTCString());
    } catch (Exception error) {
      pApplet.println("Data inválida: " + error.getMessage());
    }
  }

  private void resetBodiesToJ2000() {
    for (Planet planet : planets) {
      planet.resetToJ2000();
      for (Moon moon : planet.getMoons()) {
        moon.resetToJ2000();
      }
    }
  }

  private void changeRenderingMode(int mode) {
    renderer.setRenderingMode(mode);
    sun.setRenderingMode(mode);
    sun.buildShape(pApplet, shapeManager);
    for (Planet planet : planets) {
      planet.setRenderingMode(mode);
      planet.buildShape(pApplet, shapeManager);
      for (Moon moon : planet.getMoons()) {
        moon.setRenderingMode(mode);
        moon.buildShape(pApplet, shapeManager);
      }
    }
  }

  private void applyScalingFactors() {
    if (sun != null) sun.applyScalingFactors();
    for (Planet planet : planets) {
      planet.applyScalingFactors();
      planet.buildShape(pApplet, shapeManager);
      for (Moon moon : planet.getMoons()) {
        moon.applyScalingFactors();
        moon.buildShape(pApplet, shapeManager);
      }
    }
  }

  private void resetView() {
    Quaternion orientation = Quaternion.fromAxisAngle(new PVector(1, 0, 0), PI / 16f);
    float distance = -NEPTUNE_DIST * pxPerAU();
    sceneCamera.snapTo(new PVector(), orientation, distance);
  }

  @Override
  public void dispose() {
    initialized = false;
    if (renderer != null) renderer.dispose();
    if (physicsEngine != null) physicsEngine.dispose();
    if (planets != null) {
      for (Planet planet : planets) planet.dispose();
    }
    if (sun != null) sun.dispose();
    if (configLoader != null) configLoader.dispose();

    renderer = null;
    physicsEngine = null;
    planets = null;
    sun = null;
    configLoader = null;
    shapeManager = null;
    clock = null;
    sceneCamera = null;
    if (planetaryBodies != null) planetaryBodies.clear();
    planetaryBodies = null;
    pApplet.println("[Scene1] Recursos de domínio liberados.");
  }

  @Override
  public String getName() {
    return "Sistema Solar Físico";
  }
}
