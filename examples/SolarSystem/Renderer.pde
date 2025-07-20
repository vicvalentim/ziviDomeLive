import processing.opengl.*;
import java.util.List;

class Renderer {
  private final PApplet pApplet;
  private List<Planet> planets;
  private List<PShape> planetOrbitShapesUniform;
  private Sun sun;
  private final PShape skySphere;

  private final ShapeManager shapeManager;
  private final ShaderManager shaderManager;

  private int renderingMode = 2;

  public Renderer(PApplet pApplet,
                  List<Planet> planets,
                  PShape skySphere,
                  ShapeManager shapeManager,
                  ShaderManager shaderManager) {
    this.pApplet       = pApplet;
    this.planets       = planets;
    this.skySphere     = skySphere;
    this.shapeManager  = shapeManager;
    this.shaderManager = shaderManager;

    // logo após planets estarem definidos:
    buildPlanetOrbitShapesUniform();
  }

  public void setSun(Sun sun) {
    this.sun = sun;
  }

  public void setPlanets(List<Planet> planets) {
    this.planets = planets;
  }

  public void setRenderingMode(int mode) {
    this.renderingMode = mode;
  }

  public int getRenderingMode() {
    return renderingMode;
  }

  public void drawLighting(PGraphicsOpenGL pg) {
    pg.ambientLight(35, 35, 35);
    if (sun != null) {
      PVector sunPx = sun.getPositionAU().copy()
                         .mult(pxPerAU());
      pg.pointLight(255, 255, 220,
                    sunPx.x, sunPx.y, sunPx.z);
    }
  }

  // chame isto uma vez, depois de carregar/sincronizar o lista de planetas:
  void buildPlanetOrbitShapesUniform() {
    planetOrbitShapesUniform = new ArrayList<PShape>();
    int segments = 180;

    for (Planet p : planets) {
      PShape shp = createShape();
      shp.beginShape(PConstants.LINE_LOOP);
      shp.noFill();
      shp.stroke(200, 200, 255, 150);
      shp.strokeWeight(1);

      float peri = p.getPerihelionAU();
      float aphe = p.getAphelionAU();
      float e    = p.getEccentricity();
      float Ω    = p.getLongitudeAscendingNodeRad();
      float i    = p.getOrbitInclinationRad();
      float ω    = p.getArgumentOfPeriapsisRad();
      float a    = 0.5f * (peri + aphe);
      float b    = a * sqrt(1 - e*e);

      for (int j = 0; j < segments; j++) {
        float θ  = TWO_PI * j / segments;
        float xp = a * (cos(θ) - e);
        float zp = b * sin(θ);
        PVector vPlane = new PVector(xp, 0, zp);
        PVector v3d = applyOrbitalPlaneToGlobal(vPlane, Ω, i, ω);
        shp.vertex(v3d.x, v3d.y, v3d.z);
      }

      shp.endShape();
      planetOrbitShapesUniform.add(shp);
    }
  }

  // substitua drawPlanetOrbits por isto:
  public void drawPlanetOrbits(PGraphicsOpenGL pg) {
    if (planetOrbitShapesUniform == null) return;
    float scale = pxPerAU();
    PVector sunPx = sun.getPositionAU().copy().mult(scale);

    pg.pushMatrix();
      pg.translate(sunPx.x, sunPx.y, sunPx.z);
      pg.scale(scale);
      for (PShape shp : planetOrbitShapesUniform) {
        pg.shape(shp);
      }
    pg.popMatrix();
  }

  /**
  * Desenha todos os planetas.
  */
  public void drawPlanets(PGraphicsOpenGL pg, boolean showLabels) {
      for (Planet p : planets) {
          pg.pushMatrix();
              p.display(pg, showLabels, false,
                        renderingMode, shapeManager, shaderManager);
          pg.popMatrix();
      }
  }

  /**
  * Desenha todas as luas — órbitas (opcional) + shape.
  * Agora usa o próprio SimParams para que a órbita
  * infle/encolha junto com `globalScale` e `bodyScale`.
  */
  public void drawMoons(PGraphicsOpenGL pg,
                        boolean showLabels,
                        boolean showMoonOrbits) {

      for (Planet p : planets) {
          for (Moon m : p.getMoons()) {

              if (showMoonOrbits) {
                  m.displayOrbit(pg);
              }

              // o resto continua igual
              m.display(pg,
                        showLabels,
                        renderingMode,
                        shapeManager,
                        shaderManager);
          }
      }
  }

  /**
  * Dispatcher que chama drawPlanets e drawMoons.
  */
  public void drawPlanetsAndMoons(PGraphicsOpenGL pg,
                                  boolean showLabels,
                                  boolean showMoonOrbits) {
    drawPlanets(pg, showLabels);
    drawMoons  (pg, showLabels, showMoonOrbits);
  }

  public void drawSkySphere(PGraphicsOpenGL pg) {
    if (renderingMode != 2) return;
    pg.pushMatrix();
      if (sun != null) {
        PVector sunPx = sun.getPositionAU().copy()
                           .mult(pxPerAU());
        pg.translate(sunPx.x, sunPx.y, sunPx.z);
        //pg.rotateY(cameraRotationY * 0.5f);
      }

      PGL pgl = pg.beginPGL();
      pgl.disable(PGL.CULL_FACE);
      pg.endPGL();

      boolean shaderApplied = false;
      PShader skyShader = shaderManager.getShader("sky_hdri");
      if (skyShader != null) {
        try {
          shaderManager.applyShader(pg, "sky_hdri");
          shaderApplied = true;
        } catch (Exception e) {
          // fallback
        }
      }
      if (!shaderApplied) {
        pg.noLights();
        pg.textureMode(NORMAL);
        pg.fill(255);
      }

      float skyScale = -NEPTUNE_DIST * pxPerAU() * 2f;
      pg.scale(skyScale);
      pg.shape(skySphere);
      pg.resetShader();

      pgl = pg.beginPGL();
      pgl.enable(PGL.CULL_FACE);
      pg.endPGL();
    pg.popMatrix();
  }

  public void dispose() {
    planets = null;
    sun     = null;
  }
}
