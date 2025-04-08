import processing.opengl.*;
import processing.opengl.PGL;

class Renderer {
  private PApplet pApplet;
  private ArrayList<Planet> planets;
  private PShape skySphere;
  
  // Controle de câmera
  private float cameraRotationX, cameraRotationY, cameraDistance;
  private PVector cameraTarget;
  
  // Modo de renderização: 0 = Wireframe, 1 = Solid, 2 = Textured
  private int renderingMode = 2;
  
  Renderer(PApplet pApplet, ArrayList<Planet> planets, PShape skySphere) {
    this.pApplet = pApplet;
    this.planets = planets;
    this.skySphere = skySphere;
    
    cameraRotationX = PI / 16;
    cameraRotationY = 0;
    cameraDistance = 100;
    cameraTarget = new PVector(0, 0, 0);
    pApplet.println("Renderer initialized: distance = " + cameraDistance 
                      + ", rotX = " + cameraRotationX + ", rotY = " + cameraRotationY);
  }
  
  public void setupCamera(PGraphicsOpenGL pg) {
    pg.translate(0, 0, cameraDistance);
    pg.rotateX(cameraRotationX);
    pg.rotateY(cameraRotationY);
    pg.translate(-cameraTarget.x, -cameraTarget.y, -cameraTarget.z);
  }
  
  public void drawLighting(PGraphicsOpenGL pg) {
    pg.ambientLight(100, 100, 100);
    if (planets.size() > 0) {
      Planet sol = planets.get(0);
      PVector sunPos = sol.getDrawPosition();
      pg.pointLight(255, 255, 220, sunPos.x, sunPos.y, sunPos.z);
    }
  }
  
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
  
  public void drawPlanetsAndMoons(PGraphicsOpenGL pg, boolean showLabels, boolean showMoonOrbits) {
    for (Planet p : planets) {
      p.display(pg, showLabels, false, renderingMode);
      for (Moon m : p.moons) {
        if (showMoonOrbits) m.displayOrbit(pg);
        m.display(pg, showLabels, renderingMode);
      }
    }
  }
  
  public void drawSkySphere(PGraphicsOpenGL pg) {
    pg.pushMatrix();
      // Centraliza o sky sphere na posição do Sol
      Planet sol = planets.get(0);
      PVector sunPos = sol.getDrawPosition();
      pg.translate(sunPos.x, sunPos.y, sunPos.z);
      
      // Combina a rotação do Sol com a rotação da câmera para obter um efeito dinâmico
      float combinedRotationY = sol.rotationAngle + cameraRotationY * 0.5f;
      pg.rotateY(combinedRotationY);
      
      // Usa PGL para desabilitar o culling e renderizar as faces internas
      PGL pgl = pg.beginPGL();
      pgl.disable(PGL.CULL_FACE);
      pg.endPGL();
      
      pg.scale(-NEPTUNE_DIST * PIXELS_PER_AU * 2.0f);
      pg.shape(skySphere);
      
      pgl = pg.beginPGL();
      pgl.enable(PGL.CULL_FACE);
      pg.endPGL();
    pg.popMatrix();
  }
  
  public void updateCameraTarget(PVector newTarget) {
    cameraTarget.lerp(newTarget, 0.01f);
  }
  
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
  
  public void setRenderingMode(int mode) {
    renderingMode = mode;
  }
  
  public int getRenderingMode() {
    return renderingMode;
  }
  
  // Método de navegação "goTo" para interpolar a câmera suavemente
  public void goTo(PVector newTarget, float newRotX, float newRotY, float newDistance) {
    float smoothing = 0.05f;  // Velocidade de transição
    cameraTarget.lerp(newTarget, smoothing);
    cameraRotationX = lerp(cameraRotationX, newRotX, smoothing);
    cameraRotationY = lerp(cameraRotationY, newRotY, smoothing);
    cameraDistance = lerp(cameraDistance, newDistance, smoothing);
  }
  
  private float lerp(float start, float stop, float amt) {
    return start + (stop - start) * amt;
  }
  
  public void dispose() {
    skySphere = null;
    cameraTarget = null;
    planets = null;
  }
}
