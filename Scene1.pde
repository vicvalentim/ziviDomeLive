import java.util.concurrent.locks.ReentrantReadWriteLock;

class Scene1 implements Scene {
  // Instâncias principais
  private zividomelive parent;
  private PApplet pApplet;
  private ArrayList<Planet> planets;
  
  // Módulos responsáveis pela configuração, física e renderização
  private TextureManager textureManager;
  private ConfigLoader configLoader;
  private PhysicsEngine physicsEngine;
  private Renderer renderer;
  
  // Parâmetros da simulação
  private float timeScale = 1.0f; // aceleração do tempo (em dias simulados)
  private boolean showOrbits = true;
  private boolean showMoonOrbits = true;
  private boolean showLabels = true;
  private int selectedPlanet = -1;
  
  // Controle de câmera – dados que serão repassados para o Renderer
  private int prevMouseX, prevMouseY;
  private PVector cameraTarget = new PVector(0, 0, 0);
  
  // Parâmetros visuais e de escalas
  private float planetAmplification = 8.0f;
  private float globalScale = 1.0f;
  private float bodyScale = 1.0f;
  private float rotationFactor = 0.2f;

  
  // Variável de gravidade calculada com base nas constantes globais
  private final float gravityFactor = G_AU * SOL_MASS / (365.25f * 365.25f);
  
  // Lock para sincronização
  private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
  
  // Construtor
  Scene1(zividomelive parent, PApplet pApplet) {
    this.parent = parent;
    this.pApplet = pApplet;
    
    // Inicializa o gerenciador de texturas e pré-carrega as texturas básicas
    textureManager = new TextureManager(pApplet);
    textureManager.preloadTextures(new String[] { "2k_sun.jpg", "background.jpg" });
    
    // Carrega a configuração do sistema solar (planetas, luas e sky sphere) a partir do JSON
    configLoader = new ConfigLoader(pApplet, textureManager);
    planets = configLoader.loadConfiguration();
    
    // Cria os módulos de física e renderização
    physicsEngine = new PhysicsEngine(pApplet, planets);
    renderer = new Renderer(pApplet, planets, configLoader.getSkySphere());
    
    // Configura a câmera
    configureCamera();
    
    // Inicia a thread de física
    startPhysicsThread();
  }
  
  // Atualiza a câmera utilizando as constantes
  private void configureCamera() {
    float neptuneCenter = NEPTUNE_DIST * PIXELS_PER_AU;
    float neptuneDrawPos = neptuneCenter + SUN_VISUAL_RADIUS;
    renderer.setCameraDistance(neptuneDrawPos * 1.2f);
  }
  
  // Inicia uma thread que chama periodicamente o método update do PhysicsEngine
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
  
  // -------------------------- Métodos da Interface Scene --------------------------
  
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
  
  // update: atualizações não físicas podem ser implementadas aqui, se necessário
  public void update() {
    trackSelectedPlanet();  // Atualiza continuamente o alvo da câmera
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
        renderer.drawPlanetsAndMoons(pg, showLabels, showMoonOrbits, globalScale, planetAmplification);
        renderer.drawSkySphere(pg);
      pg.popMatrix();
    } finally {
      rwLock.readLock().unlock();
    }
  }

  private void trackSelectedPlanet() {
    if (selectedPlanet >= 0 && selectedPlanet < planets.size()) {
      Planet target = planets.get(selectedPlanet);
      // Usa a posição atual do planeta para atualizar o alvo da câmera
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
        case 'a':  // Aumenta a amplificação dos planetas
          planetAmplification *= 1.1f;
          pApplet.println("planetAmplification: " + planetAmplification);
          break;
        case 'z':  // Diminui a amplificação dos planetas
          planetAmplification /= 1.1f;
          pApplet.println("planetAmplification: " + planetAmplification);
          break;
        case 'G':  // Aumenta a escala global
          globalScale *= 1.1f;
          pApplet.println("globalScale: " + globalScale);
          break;
        case 'g':  // Diminui a escala global
          globalScale /= 1.1f;
          pApplet.println("globalScale: " + globalScale);
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
        case 'r':
          rotationFactor *= 0.8f;
          pApplet.println("rotationFactor reduzido para: " + rotationFactor);
          break;
        case 'R':
          rotationFactor *= 1.2f;
          pApplet.println("rotationFactor aumentado para: " + rotationFactor);
          break;
        default:
          // Se a tecla é um dígito, seleciona o planeta correspondente (1 para o índice 0, etc.)
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
        // Atualiza a rotação da câmera através do Renderer:
        float newRotX = renderer.getCameraRotationX() + dy;
        float newRotY = renderer.getCameraRotationY() + dx;
        renderer.setCameraRotation(newRotX, newRotY);
        prevMouseX = event.getX();
        prevMouseY = event.getY();
        break;
      case MouseEvent.WHEEL:
        // Atualiza a distância da câmera via Renderer
        renderer.setCameraDistance(renderer.getCameraDistance() + event.getCount() * 0.01f);
        break;
    }
  }

  private void resetView() {
      // Apenas reseta a câmera, sem recarregar a configuração dos planetas.
      renderer.setCameraRotation(-PI / 16, 0);
      renderer.setCameraDistance(500);
      pApplet.println("[Scene1] Reset view: Camera reset, but configuration remains unchanged.");
  }

  public void dispose() {
    // Libera o TextureManager e seu cache
    if (textureManager != null) {
      textureManager.clear();
      textureManager = null;
    }
    
    // Libera o ConfigLoader, se houver recursos alocados
    if (configLoader != null) {
      configLoader.dispose();
      configLoader = null;
    }
    
    // Libera os planetas e chama o dispose() individual de cada um
    if (planets != null) {
      for (Planet p : planets) {
        p.dispose();
      }
      planets.clear();
      planets = null;
    }
    
    // Libera o módulo de física
    physicsEngine = null;
    
    // Libera o Renderer
    if (renderer != null) {
      renderer.dispose();
      renderer = null;
    }
    
    System.out.println("Disposing resources for scene: " + getName());
  }

  // Retorna o nome da cena
  public String getName() {
    return "Sistema Solar Físico";
  }
}
  

