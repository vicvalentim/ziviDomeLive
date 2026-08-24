import java.util.List;

class Renderer {
  private final PApplet pApplet;
  private List<Planet> planets;
  private List<PShape> planetOrbitShapesUniform;
  private Sun sun;

  private final ShapeManager shapeManager;

  private int renderingMode = 2;

  public Renderer(PApplet pApplet,
                  List<Planet> planets,
                  ShapeManager shapeManager) {
    this.pApplet       = pApplet;
    this.planets       = planets;
    this.shapeManager  = shapeManager;

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
      float scale = pxPerAU();
      PVector sunPosition = sun.getPositionAU();
      pg.pointLight(255, 255, 220,
                    sunPosition.x * scale,
                    sunPosition.y * scale,
                    sunPosition.z * scale);
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

      double peri = p.getPerihelionAU();
      double aphe = p.getAphelionAU();
      double e    = p.getEccentricity();
      double Ω    = p.getLongitudeAscendingNodeRad();
      double i    = p.getOrbitInclinationRad();
      double ω    = p.getArgumentOfPeriapsisRad();
      double a    = 0.5 * (peri + aphe);
      double b    = a * Math.sqrt(1.0 - e * e);

      for (int j = 0; j < segments; j++) {
        double θ = TWO_PI_DOUBLE * j / segments;
        double[] vPlane = { a * (Math.cos(θ) - e), 0.0, b * Math.sin(θ) };
        double[] v3d = new double[3];
        applyOrbitalPlaneToGlobal(vPlane, Ω, i, ω, v3d);
        shp.vertex((float) v3d[0], (float) v3d[1], (float) v3d[2]);
      }

      shp.endShape();
      planetOrbitShapesUniform.add(shp);
    }
  }

  // substitua drawPlanetOrbits por isto:
  public void drawPlanetOrbits(PGraphicsOpenGL pg) {
    if (planetOrbitShapesUniform == null) return;
    float scale = pxPerAU();
    PVector sunPosition = sun.getPositionAU();

    pg.pushMatrix();
      pg.translate(
        sunPosition.x * scale,
        sunPosition.y * scale,
        sunPosition.z * scale);
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
                        renderingMode, shapeManager);
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
                        shapeManager);
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

  public void dispose() {
    planets = null;
    sun     = null;
  }
}
