import processing.opengl.*;

class Renderer {
  private PApplet pApplet;
  private ArrayList<Planet> planets;
  private PShape skySphere;
  
  // Parâmetros e controle de câmera
  private float cameraRotationX, cameraRotationY, cameraDistance;
  private PVector cameraTarget;
  
  Renderer(PApplet pApplet, ArrayList<Planet> planets, PShape skySphere) {
    this.pApplet = pApplet;
    this.planets = planets;
    this.skySphere = skySphere;
    
    // Valores iniciais para a câmera
    cameraRotationX = PI / 16;
    cameraRotationY = 0;
    cameraDistance = 100;
    cameraTarget = new PVector(0, 0, 0);
    
    pApplet.println("[Renderer] Initialized: cameraDistance=" + cameraDistance + 
                      ", cameraRotationX=" + cameraRotationX + ", cameraRotationY=" + cameraRotationY);
  }
  
  // Configura a câmera usando as funções do PGraphicsOpenGL
  public void setupCamera(PGraphicsOpenGL pg) {
    pg.translate(0, 0, cameraDistance);
    pg.rotateX(cameraRotationX);
    pg.rotateY(cameraRotationY);
    pg.translate(-cameraTarget.x, -cameraTarget.y, -cameraTarget.z);
  }
  
  // Desenha a iluminação
  public void drawLighting(PGraphicsOpenGL pg) {
    pg.ambientLight(100, 100, 100);
    if (planets.size() > 0) {
      Planet sol = planets.get(0);
      PVector sunPos = sol.getDrawPosition();
      pg.pointLight(255, 255, 220, sunPos.x, sunPos.y, sunPos.z);
    }
  }
  
  // Desenha as órbitas dos planetas
  public void drawPlanetOrbits(PGraphicsOpenGL pg) {
    pg.noFill();
    pg.strokeWeight(1.5f);
    pg.stroke(200, 200, 255, 150);
    for (int i = 1; i < planets.size(); i++) {
      Planet p = planets.get(i);
      pg.pushMatrix();
        pg.rotateX(PI / 2);
        pg.rotateX(p.orbitInclination);
        float orbitalRadius = p.orbitRadius + SUN_VISUAL_RADIUS;
        pg.ellipse(0, 0, orbitalRadius * 2, orbitalRadius * 2);
      pg.popMatrix();
    }
  }
  
  // Desenha os planetas e as luas
  public void drawPlanetsAndMoons(PGraphicsOpenGL pg, boolean showLabels, boolean showMoonOrbits) {
    for (Planet p : planets) {
      p.display(pg, showLabels, false);
      for (Moon m : p.moons) {
        if (showMoonOrbits) m.displayOrbit(pg);
        m.display(pg, showLabels);
      }
    }
  }
  
  // Desenha o sky sphere (fundo)
  public void drawSkySphere(PGraphicsOpenGL pg) {
    pg.pushMatrix();
      // Centraliza o sky sphere na posição do Sol
      Planet sol = planets.get(0);
      PVector sunPos = sol.getDrawPosition();
      pg.translate(sunPos.x, sunPos.y, sunPos.z);
      
      // Combina a rotação do Sol com parte da rotação da câmera para dar uma sensação de fundo "fixo" mas dinâmico
      float combinedRotationY = sol.rotationAngle + cameraRotationY * 0.5f;
      pg.rotateY(combinedRotationY);
      
      // Usa PGL para desabilitar o culling e renderizar as faces internas
      PGL pgl = pg.beginPGL();
      pgl.disable(PGL.CULL_FACE);
      pg.endPGL();
      
      pg.scale(-NEPTUNE_DIST * PIXELS_PER_AU * 2.0f);
      pg.shape(skySphere);
      
      // Restaura o culling de faces
      pgl = pg.beginPGL();
      pgl.enable(PGL.CULL_FACE);
      pg.endPGL();
      
    pg.popMatrix();
  }
  
  // Atualiza o alvo da câmera (por exemplo, via movimento do mouse ou via transição)
  public void updateCameraTarget(PVector newTarget) {
    cameraTarget.lerp(newTarget, 0.01f);
  }
  
  // Métodos getters e setters para os parâmetros de câmera
  public void setCameraRotation(float rotX, float rotY) {
    cameraRotationX = rotX;
    cameraRotationY = rotY;
  }
  
  public float getCameraRotationX() {
    return cameraRotationX;
  }
  
  public float getCameraRotationY() {
    return cameraRotationY;
  }
  
  public void setCameraDistance(float distance) {
    cameraDistance = distance;
  }
  
  public float getCameraDistance() {
    return cameraDistance;
  }
  
  public void setPlanets(ArrayList<Planet> planets) {
    this.planets = planets;
  }
  
  // Exemplo: retorna o valor padrão de ROTATION_FACTOR (definido nas constantes)
  public float getRotationFactor() {
    return ROTATION_FACTOR;
  }
  
  // Método de navegação "goTo" para interpolar os parâmetros da câmera de forma suave
  public void goTo(PVector newTarget, float newRotX, float newRotY, float newDistance) {
    float smoothing = 0.05f;  // Determina a velocidade da transição (ajuste conforme necessário)
    
    // Interpola o alvo (cameraTarget) usando uma transição suave (lerp)
    cameraTarget.lerp(newTarget, smoothing);
    // Transição suave para as rotações e a distância (zoom)
    cameraRotationX = lerp(cameraRotationX, newRotX, smoothing);
    cameraRotationY = lerp(cameraRotationY, newRotY, smoothing);
    cameraDistance = lerp(cameraDistance, newDistance, smoothing);
  }

  // Função de interpolação linear
  private float lerp(float start, float stop, float amt) {
    return start + (stop - start) * amt;
  }
  
  // Método dispose para liberar referências
  public void dispose() {
    skySphere = null;
    cameraTarget = null;
    planets = null;
    pApplet.println("[Renderer] dispose() called; resources released.");
  }
  
}
