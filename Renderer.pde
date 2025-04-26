import processing.opengl.*;
import java.util.List;

class Renderer {
  private final PApplet pApplet;
  private List<Planet> planets;
  private Sun sun;
  private final PShape skySphere;

  private final ShapeManager shapeManager;
  private final ShaderManager shaderManager;
  private final SimParams simParams;

  private float cameraRotationX = PI/16;
  private float cameraRotationY = 0;
  private float cameraDistance  = 20;
  private final PVector cameraTarget = new PVector(0, 0, 0);

  private int renderingMode = 2;

  public Renderer(PApplet pApplet,
                  List<Planet> planets,
                  PShape skySphere,
                  ShapeManager shapeManager,
                  ShaderManager shaderManager,
                  SimParams simParams) {
    this.pApplet       = pApplet;
    this.planets       = planets;
    this.skySphere     = skySphere;
    this.shapeManager  = shapeManager;
    this.shaderManager = shaderManager;
    this.simParams     = simParams;
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

  public void setupCamera(PGraphicsOpenGL pg) {
    pg.translate(0, 0, cameraDistance);
    pg.rotateX(cameraRotationX);
    pg.rotateY(cameraRotationY);
    pg.translate(-cameraTarget.x, -cameraTarget.y, -cameraTarget.z);
  }

  public void drawLighting(PGraphicsOpenGL pg) {
    pg.ambientLight(35, 35, 35);
    if (sun != null) {
      PVector sunPx = sun.getPositionAU().copy()
                         .mult(PIXELS_PER_AU * simParams.globalScale);
      pg.pointLight(255, 255, 220,
                    sunPx.x, sunPx.y, sunPx.z);
    }
  }



  /**
  * Desenha apenas as órbitas dos planetas no plano da eclíptica (XZ / Y-up),
  * centradas no foco (Sol) e usando o mesmo pipeline Ω→i→ω do KeplerMath.
  */
  public void drawPlanetOrbits(PGraphicsOpenGL pg) {
    pg.noFill();
    pg.stroke(200, 200, 255, 150);
    pg.strokeWeight(1);
    float scale = PIXELS_PER_AU * simParams.globalScale;
    int segments = 180;

    // offset para o foco (Sol)
    PVector sunPx = sun.getPositionAU().copy().mult(scale);

    // desenha todas as órbitas
    pg.pushMatrix();
    pg.translate(sunPx.x, sunPx.y, sunPx.z);

    for (Planet p : planets) {
      float peri = p.getPerihelionAU();
      float aphe = p.getAphelionAU();
      float e    = p.getEccentricity();
      float Ω    = p.getLongitudeAscendingNodeRad();
      float i    = p.getOrbitInclinationRad();
      float ω    = p.getArgumentOfPeriapsisRad();

      float a = 0.5f * (peri + aphe);
      float b = a * PApplet.sqrt(1 - e*e);

      pg.beginShape();
      for (int j = 0; j <= segments; j++) {
        float θ   = PApplet.TWO_PI * j / segments;
        float cosθ = PApplet.cos(θ), sinθ = PApplet.sin(θ);

        // ponto no plano orbital, já centrado no foco:
        float xp = a * (cosθ - e);
        float zp = b * sinθ;
        PVector vPlane = new PVector(xp, 0, zp);

        // aplica Ω→i→ω
        PVector v3d = applyOrbitalPlaneToGlobal(vPlane, Ω, i, ω);
        v3d.mult(scale);

        pg.vertex(v3d.x, v3d.y, v3d.z);
      }
      pg.endShape(PConstants.CLOSE);
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
  */
  public void drawMoons(PGraphicsOpenGL pg, boolean showLabels, boolean showMoonOrbits) {
    float scaleAUtoPx = PIXELS_PER_AU * simParams.globalScale;
    for (Planet p : planets) {
      // posição do planeta em px (pivô das luas)
      PVector pPosPx = p.getPositionAU().copy().mult(scaleAUtoPx);
      float   prPx   = p.getRadiusPx();

      pg.pushMatrix();
        pg.translate(pPosPx.x, pPosPx.y, pPosPx.z);

        for (Moon m : p.getMoons()) {
          if (showMoonOrbits) {
            // desenha a órbita de cada lua em torno do planeta
            m.displayOrbit(pg, prPx);
          }
          // desenha o próprio corpo da lua
          m.display(pg, showLabels, renderingMode, shapeManager, shaderManager);
        }
      pg.popMatrix();
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
                           .mult(PIXELS_PER_AU * simParams.globalScale);
        pg.translate(sunPx.x, sunPx.y, sunPx.z);
        pg.rotateY(cameraRotationY * 0.5f);
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

      float skyScale = -NEPTUNE_DIST * PIXELS_PER_AU * 2f * simParams.globalScale;
      pg.scale(skyScale);
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

  public void goTo(PVector newTarget,
                   float newRotX,
                   float newRotY,
                   float newDistance) {
    float smooth = 0.05f;
    cameraTarget.lerp(newTarget, smooth);
    cameraRotationX = lerp(cameraRotationX, newRotX, smooth);
    cameraRotationY = lerp(cameraRotationY, newRotY, smooth);
    cameraDistance  = lerp(cameraDistance, newDistance, smooth);
  }

  private float lerp(float start, float stop, float amt) {
    return start + (stop - start) * amt;
  }

  public void dispose() {
    planets = null;
    sun     = null;
  }
}
