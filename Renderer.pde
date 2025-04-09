import processing.opengl.*;
import processing.opengl.PGL;

class Renderer {
  private PApplet pApplet;
  private ArrayList<Planet> planets;
  private PShape skySphere;
  private Sun sun;

  // Gerenciadores de formas e shaders
  private ShapeManager shapeManager;
  private ShaderManager shaderManager;

  // Controle de câmera
  private float cameraRotationX, cameraRotationY, cameraDistance;
  private PVector cameraTarget;

  // Modo de renderização: 0 = Wireframe, 1 = Solid, 2 = Textured
  private int renderingMode = 2;

  Renderer(PApplet pApplet, ArrayList<Planet> planets, PShape skySphere,
           ShapeManager shapeManager, ShaderManager shaderManager) {
    this.pApplet = pApplet;
    this.planets = planets;
    this.skySphere = skySphere;
    this.shapeManager = shapeManager;
    this.shaderManager = shaderManager;

    cameraRotationX = PI / 16;
    cameraRotationY = 0;
    cameraDistance = 100;
    cameraTarget = new PVector(0, 0, 0);
    pApplet.println("Renderer initialized: distance = " + cameraDistance 
                      + ", rotX = " + cameraRotationX + ", rotY = " + cameraRotationY);
  }

  // Novo método para definir o Sol
  public void setSun(Sun sun) {
    this.sun = sun;
  }

  public void setupCamera(PGraphicsOpenGL pg) {
    pg.translate(0, 0, cameraDistance);
    pg.rotateX(cameraRotationX);
    pg.rotateY(cameraRotationY);
    pg.translate(-cameraTarget.x, -cameraTarget.y, -cameraTarget.z);
  }

  public void drawLighting(PGraphicsOpenGL pg) {
    pg.ambientLight(15, 15, 15);
    if (sun != null) {
      PVector sunPos = sun.getPosition();
      pg.pointLight(255, 255, 220, sunPos.x, sunPos.y, sunPos.z);
    }
  }

  public void drawPlanetOrbits(PGraphicsOpenGL pg) {
    pg.noFill();
    pg.strokeWeight(1.5f);
    pg.stroke(200, 200, 255, 150);
    for (Planet p : planets) {
      pg.pushMatrix();
        pg.rotateX(PI / 2);
        pg.rotateX(p.orbitInclination);
        float orbitalRadius = p.orbitRadius + SUN_VISUAL_RADIUS;
        pg.ellipse(0, 0, orbitalRadius * 2, orbitalRadius * 2);
      pg.popMatrix();
    }
  }

  public void drawPlanetsAndMoons(PGraphicsOpenGL pg, boolean showLabels, boolean showMoonOrbits,
                                  ShapeManager shapeManager, ShaderManager shaderManager) {
    for (Planet p : planets) {
      p.display(pg, showLabels, false, renderingMode, shapeManager, shaderManager);
      for (Moon m : p.moons) {
        if (showMoonOrbits) {
          m.displayOrbit(pg);
        }
        m.display(pg, showLabels, renderingMode, shapeManager, shaderManager);
      }
    }
  }

  public void drawSkySphere(PGraphicsOpenGL pg) {
    pg.pushMatrix();
      if (sun != null) {
        PVector sunPos = sun.getPosition();
        pg.translate(sunPos.x, sunPos.y, sunPos.z);
        float combinedRotationY = cameraRotationY * 0.5f;
        pg.rotateY(combinedRotationY);
      }

      PGL pgl = pg.beginPGL();
      pgl.disable(PGL.CULL_FACE);
      pg.endPGL();

      PShader skyShader = shaderManager.getShader("sky");
      if (skyShader != null) {
        pg.shader(skyShader);
      }

      pg.scale(-NEPTUNE_DIST * PIXELS_PER_AU * 2.0f);
      pg.shape(skySphere);

      pg.resetShader();

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

  public void goTo(PVector newTarget, float newRotX, float newRotY, float newDistance) {
    float smoothing = 0.05f;
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
    sun = null;
  }
}
