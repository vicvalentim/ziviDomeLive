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

  // ————————————————————————————————
  // Managers
  // ————————————————————————————————
  private TextureManager textureManager;
  private ShaderManager shaderManager;
  private ShapeManager shapeManager;
  private ConfigLoader configLoader;
  private PhysicsEngine physicsEngine;
  private Renderer renderer;
  private CameraController cameraController;

  // ————————————————————————————————
  // Parâmetros de visualização
  // ————————————————————————————————
  private boolean showOrbits     = true;
  private boolean showMoonOrbits = true;
  private boolean showLabels     = false;
  private int     selectedPlanet = -1;

  private int prevMouseX, prevMouseY;
  private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

  private SimulatedClock clock; // Novo relógio em dias
  private boolean needsReload = false;

 
  // ————————————————————————————————
  // Construtor de Scene1
  // ————————————————————————————————
  Scene1(zividomelive parent, PApplet pApplet) {
    this.parent  = parent;
    this.pApplet = pApplet;

    // 1) Inicializa managers e params...
    textureManager = new TextureManager(pApplet);
    shapeManager   = new ShapeManager(pApplet);
    shaderManager  = new ShaderManager(pApplet);
    loadAllShaders();

    // 2) Carrega Sol e planetas
    configLoader = new ConfigLoader(pApplet, textureManager);
    configLoader.sendTexturesToShaderManager(shaderManager);

    sun     = configLoader.loadSun();
    planets = configLoader.loadConfiguration();

    // configura central bodies
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
        // aqui é ESSENCIAL:
        planetaryBodies.addAll(p.getMoons());
      }
    physicsEngine = new PhysicsEngine(planetaryBodies);


    // 4) Agora sim: cria o renderer com a lista de planetas já carregada
    renderer = new Renderer(
      pApplet,
      planets,
      configLoader.getSkySphere(),
      shapeManager,
      shaderManager
    );
    renderer.setSun(sun);
    
    sun.buildShape(pApplet, shapeManager);
    for (Planet p : planets) {
      p.buildShape(pApplet, shapeManager);
      for (Moon m : p.getMoons()) {
        m.buildShape(pApplet, shapeManager);
      }
    }

    // 5) Inicializa o relógio absoluto e propaga até “hoje”…
    clock = new SimulatedClock();
    setClockToNowUTC();
    propagateSinceJ2000();

    // 6) Inicializa o controlador de câmera
    // *** distância inicial é 1.2x a distância de Netuno ***

    // distância inicial
    float initDist = NEPTUNE_DIST * pxPerAU() * 1.2f;

    // instanciamos o controlador de câmera
    cameraController = new CameraController(
      new PVector(0, 0, 0),  // alvo inicial no (0,0,0)
      initDist               // distância inicial
    );

    resetView();
  }


  /** centraliza todo o loadSun()/loadConfiguration()/montagem de corpos + physics + renderer */
  private void initializeScene() {
      // 1) recarrega JSON se for reload
      // *** só há JSON fresco se veio de setupScene() ***
      sun     = configLoader.loadSun();
      planets = configLoader.loadConfiguration();

      // 2) fixa central bodies
      for (Planet p : planets) {
        p.setCentralBody(sun);
        for (Moon m : p.getMoons()) {
          m.setCentralBody(p);
        }
      }

      // 3) monta lista e physicsEngine
      planetaryBodies = new ArrayList<>();
      planetaryBodies.add(sun);
        for (Planet p : planets) {
          planetaryBodies.add(p);
          // aqui é ESSENCIAL:
          planetaryBodies.addAll(p.getMoons());
        }
      physicsEngine = new PhysicsEngine(planetaryBodies);

      // 4) passa ao renderer
      renderer.setSun(sun);
      renderer.setPlanets(planets);

      // 5) rebuild shapes e escalas
      sun.buildShape(pApplet, shapeManager);
      for (Planet p : planets) {
        p.buildShape(pApplet, shapeManager);
        for (Moon m : p.getMoons()) {
          m.buildShape(pApplet, shapeManager);
        }
      }
      applyScalingFactors();
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
    if (!needsReload) return;
    needsReload = false;

    rwLock.writeLock().lock();
    try {
      // 1) recarrega o JSON
      configLoader.reloadJson();
      // 2) faz todo o initializeScene de novo
      initializeScene();
      // 3) reajusta o relógio (opcional)
      propagateSinceJ2000();
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  // ————————————————————————————————
  // update() — agora baseado em double do relógio
  // ————————————————————————————————
  @Override
  public void update() {
    // 0) Se foi pedido reload, reinicializa tudo
    setupScene();

    rwLock.writeLock().lock();
    try {
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

      // 3) Atualiza meta e interpola suavemente
      cameraController.goTo(
        newTarget,
        cameraController.getOrientation(),
        cameraController.getDistance()
      );
      cameraController.update();

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
      pg.background(0, 10, 20);
      pg.fill(255);
      pg.textSize(12);
      pg.text(clock.getCalendarUTCString(), 10, 20);
      pg.pushMatrix();
      cameraController.apply(pg);

        // 1) Sol
        PVector sunPx = sun.getPositionAU().copy()
                          .mult(pxPerAU());
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
  
  // ————————————————————————————————
  // Funções auxiliares
  // ————————————————————————————————
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
    char key = event.getKey();
    switch (key) {
      case ' ': resetView(); break;
      case 'G': globalScale *= 1.1f; planetAmplification = 1; applyScalingFactors(); break;
      case 'g': globalScale /= 1.1f; planetAmplification = 1; applyScalingFactors(); break;
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
   * Função de callback para eventos do mouse.
   * @param event Evento do mouse
   */
  public void mouseEvent(processing.event.MouseEvent event) {
    switch (event.getAction()) {
      case MouseEvent.PRESS:
        prevMouseX = event.getX();
        prevMouseY = event.getY();
        break;

      case MouseEvent.DRAG:
        float dx = (event.getX() - prevMouseX) * 0.01f;
        float dy = (event.getY() - prevMouseY) * 0.01f;
        // Rotaciona a câmera usando o CameraController (quaternions)
        cameraController.rotateAround(new PVector(0, 1, 0), dx);
        cameraController.rotateAround(new PVector(1, 0, 0), dy);
        prevMouseX = event.getX();
        prevMouseY = event.getY();
        break;

      case MouseEvent.WHEEL:
        float scroll = event.getCount();
        boolean isPad = Math.abs(scroll) < 1;
        float zoom   = isPad ? scroll * 0.001f : scroll * 2f;
        // Ajusta distância da câmera com limites
        cameraController.setDistance(
          PApplet.constrain(
            cameraController.getDistance() + zoom,
            -1e6f,      // distância mínima
            1e6f      // distância máxima
          )
        );
        break;
    }
  }

  /**
  * Reseta a câmera para a posição inicial, olhar para o Sol
  * mas mais afastado, usando NEPTUNE_DIST.
  */
  private void resetView() {
      // 1) alvo: centro do Sol
      cameraController.setTarget(new PVector(0, 0, 0));

      // 2) pitch suave de PI/16
      Quaternion q = new Quaternion(1, 0, 0, 0)
                        .fromAxisAngle(new PVector(1, 0, 0), PI/16);
      cameraController.setOrientation(q);

      // 3) distância: igual ao initDist do construtor
      float dist = -NEPTUNE_DIST * pxPerAU();
      cameraController.setDistance(dist);
  }

  /**
   * Limpa todos os recursos utilizados pela cena.
   */
  public void dispose() {
    textureManager.clear();
    configLoader.dispose();
    for (Planet p:planets) p.dispose();
    sun.dispose();
    physicsEngine.dispose();
    renderer.dispose();
    System.out.println("Disposed Scene1");
  }

  /**
   * Retorna o nome da cena.
   * @return Nome da cena
   */
  public String getName() {
    return "Sistema Solar Físico";
  }
}
