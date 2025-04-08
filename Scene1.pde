import java.util.concurrent.locks.ReentrantReadWriteLock;

class Scene1 implements Scene {
  // Instâncias principais
  private zividomelive parent;
  private PApplet pApplet;
  private ArrayList<Planet> planets;

  // Instância dos parâmetros dinâmicos
  private SimParams simParams;
  
  // Módulos responsáveis pela configuração, física e renderização
  private TextureManager textureManager;
  private ConfigLoader configLoader;
  private PhysicsEngine physicsEngine;
  private Renderer renderer;
  
  // Parâmetros da simulação
  private float timeScale = 1.0f; // aceleração do tempo (em dias simulados)
  private boolean showOrbits = true;
  private boolean showMoonOrbits = true;
  private boolean showLabels = false;
  private int selectedPlanet = -1;
  
  // Controle de câmera – dados que serão repassados para o Renderer
  private int prevMouseX, prevMouseY;
  private PVector cameraTarget = new PVector(0, 0, 0);
  
  // Lock para sincronização
  private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
  
  // Construtor
  Scene1(zividomelive parent, PApplet pApplet) {
    this.parent = parent;
    this.pApplet = pApplet;

    // Inicializa os parâmetros dinâmicos da simulação
    simParams = new SimParams();
    
    // Inicializa o gerenciador de texturas e pré-carrega as texturas básicas
    textureManager = new TextureManager(pApplet);
    textureManager.preloadTextures(new String[] { "2k_sun.jpg", "background.jpg" });
    
    // Cria o ConfigLoader (que usa os parâmetros globais definidos em SimParams)
    configLoader = new ConfigLoader(pApplet, textureManager);
    planets = configLoader.loadConfiguration();
    
    // Cria os módulos de física e renderização
    physicsEngine = new PhysicsEngine(pApplet, planets);
    renderer = new Renderer(pApplet, planets, configLoader.getSkySphere());
    
    // Configura a câmera, usando constantes para conversão
    configureCamera();
    
    // Inicia a thread de física
    startPhysicsThread();
  }
  
  // Configura a câmera utilizando constantes (p.ex.: NEPTUNE_DIST, PIXELS_PER_AU, SUN_VISUAL_RADIUS)
  private void configureCamera() {
    float neptuneCenter = NEPTUNE_DIST * PIXELS_PER_AU;
    float neptuneDrawPos = neptuneCenter + SUN_VISUAL_RADIUS;
    renderer.setCameraDistance(neptuneDrawPos * 1.2f);
  }
  
  // Inicia uma thread que chama periodicamente o update do PhysicsEngine
  private void startPhysicsThread() {
    Thread physicsThread = new Thread(new Runnable() {
      public void run() {
        while (true) {
          rwLock.writeLock().lock();
          try {
            physicsEngine.update(timeScale * 0.1f);
            updateCameraTarget();
          } finally {
            rwLock.writeLock().unlock();
          }
          try {
            Thread.sleep(10);
          } catch (InterruptedException e) {}
        }
      }
    });
    physicsThread.start();
  }
  
  // Atualiza o alvo da câmera com base no planeta selecionado
  private void updateCameraTarget() {
    if (selectedPlanet >= 0) {
      Planet p = planets.get(selectedPlanet);
      renderer.updateCameraTarget(p.getDrawPosition());
    } else {
      renderer.updateCameraTarget(new PVector(0, 0, 0));
    }
  }
  
  // --------------------- Métodos da Interface Scene ---------------------
  
  // setupScene: relê a configuração e reinicializa os módulos, se necessário
  public void setupScene() {
    rwLock.writeLock().lock();
    try {
      planets = configLoader.loadConfiguration();
      physicsEngine = new PhysicsEngine(pApplet, planets);
      renderer.setPlanets(planets);
    } finally {
      rwLock.writeLock().unlock();
    }
  }
  
  // update: atualizações não físicas podem ser implementadas aqui (exemplo: tracking do planeta selecionado)
  public void update() {
    trackSelectedPlanet();
  }
  
  // sceneRender: delega a renderização à classe Renderer
  public void sceneRender(PGraphicsOpenGL pg) {
    rwLock.readLock().lock();
    try {
      pg.background(0, 10, 20);
      pg.pushMatrix();
        renderer.setupCamera(pg);
        renderer.drawLighting(pg);
        if (showOrbits) renderer.drawPlanetOrbits(pg);
        renderer.drawPlanetsAndMoons(pg, showLabels, showMoonOrbits);
        renderer.drawSkySphere(pg);
      pg.popMatrix();
    } finally {
      rwLock.readLock().unlock();
    }
  }
  
  private void trackSelectedPlanet() {
    if (selectedPlanet >= 0 && selectedPlanet < planets.size()) {
      Planet target = planets.get(selectedPlanet);
      renderer.goTo(target.getDrawPosition(), renderer.getCameraRotationX(), renderer.getCameraRotationY(), renderer.getCameraDistance());
    }
  }
  
  public void keyEvent(processing.event.KeyEvent event) {
    if (event.getAction() == processing.event.KeyEvent.PRESS) {
      char key = event.getKey();
      switch (key) {
        case ' ':
          resetView();
          break;
        // Ajuste da amplificação dos planetas – agora usa SimParams
        case 'G':
          simParams.globalScale *= 1.1f;
          pApplet.println("globalScale: " + simParams.globalScale);
          break;
        case 'g':
          simParams.globalScale /= 1.1f;
          pApplet.println("globalScale: " + simParams.globalScale);
          break;
        case 'a':
          simParams.planetAmplification *= 1.1f;
          pApplet.println("planetAmplification: " + simParams.planetAmplification);
          break;
        case 'z':
          simParams.planetAmplification /= 1.1f;
          pApplet.println("planetAmplification: " + simParams.planetAmplification);
          break;
          case 'w':
        renderer.setRenderingMode(0);
        pApplet.println("Render Mode: WIREFRAME");
        break;
      case 's':
        renderer.setRenderingMode(1);
        pApplet.println("Render Mode: SOLID");
        break;
      case 't':
        renderer.setRenderingMode(2);
        pApplet.println("Render Mode: TEXTURED");
        break;
        case '+':
          timeScale *= 1.2f;
          break;
        case '-':
          timeScale *= 0.8f;
          break;
        case 'o':
          showOrbits = !showOrbits;
          break;
        case 'l':
          showLabels = !showLabels;
          break;
        case 'p':
          showMoonOrbits = !showMoonOrbits;
          break;
        // Para rotação, se necessário (pode ser mantida ou implementada via constantes)
        case 'r':
          pApplet.println("rotationFactor adjustment not implemented here; use ROTATION_FACTOR if needed");
          break;
        case 'R':
          pApplet.println("rotationFactor adjustment not implemented here; use ROTATION_FACTOR if needed");
          break;
        default:
          if (Character.isDigit(key)) {
            int num = Character.getNumericValue(key);
            if (num > 0 && num <= planets.size()) {
              selectedPlanet = num - 1;
            }
          }
          break;
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
        float dx = (event.getX() - prevMouseX) * 0.01f;
        float dy = (event.getY() - prevMouseY) * 0.01f;
        float newRotX = renderer.getCameraRotationX() + dy;
        float newRotY = renderer.getCameraRotationY() + dx;
        renderer.setCameraRotation(newRotX, newRotY);
        prevMouseX = event.getX();
        prevMouseY = event.getY();
        break;
      case MouseEvent.WHEEL:
        renderer.setCameraDistance(renderer.getCameraDistance() + event.getCount() * 0.01f);
        break;
    }
  }
  
  private void resetView() {
    // Apenas reseta a câmera, sem recarregar a configuração dos planetas.
    renderer.setCameraRotation(-PI / 16, 0);
    renderer.setCameraDistance(500);
    pApplet.println("[Scene1] Reset view: Camera reset, configuration unchanged.");
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
      for (Planet p : planets) {
        p.dispose();
      }
      planets.clear();
      planets = null;
    }
    physicsEngine = null;
    if (renderer != null) {
      renderer.dispose();
      renderer = null;
    }
    System.out.println("Disposing resources for scene: " + getName());
  }
  
  public String getName() {
    return "Sistema Solar Físico";
  }
}
