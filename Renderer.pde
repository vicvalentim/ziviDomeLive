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
    pg.ambientLight(15, 15, 15);
    if (sun != null) {
      // converte posição em AU para pixels
      PVector sunPx = sun.getPositionAU().copy()
                         .mult(PIXELS_PER_AU * simParams.globalScale);
      pg.pointLight(255, 255, 220,
                    sunPx.x, sunPx.y, sunPx.z);
    }
  }

  /**
  * Desenha apenas as órbitas dos planetas no plano da eclíptica (XZ).
  */
  public void drawPlanetOrbits(PGraphicsOpenGL pg) {
    pg.noFill();
    pg.strokeWeight(1.5f);
    pg.stroke(200, 200, 255, 150);

    float scale = PIXELS_PER_AU * simParams.globalScale;

    for (Planet p : planets) {
      float peri   = p.getPerihelionAU();
      float aphe   = p.getAphelionAU();
      float e      = p.getEccentricity();
      float inc    = p.getOrbitInclinationRad();
      float argPer = p.getArgumentOfPeriapsisRad();

      float a   = (peri + aphe) * 0.5f;
      float b   = a * sqrt(1 - e * e);
      float aPx = a * scale;
      float bPx = b * scale;
      float fPx = a * e * scale;

      pg.pushMatrix();
        pg.rotateX(PConstants.PI/2);
        if (argPer != 0) pg.rotateZ(argPer);
        pg.rotateX(inc);
        pg.translate(-fPx, 0, 0);
        pg.ellipse(0, 0, 2 * aPx, 2 * bPx);
      pg.popMatrix();
    }
  }

  /**
  * Desenha apenas as órbitas das luas no plano da eclíptica (XZ).
  */
  public void drawMoonOrbits(PGraphicsOpenGL pg) {
    pg.noFill();
    pg.strokeWeight(1f);
    pg.stroke(150,150,255,150);

    float scale = PIXELS_PER_AU * simParams.globalScale;

    for (Planet p : planets) {
      PVector pPos = p.getPositionAU().copy().mult(scale);
      for (Moon m : p.getMoons()) {
        float mPeri   = m.getPerihelionAU();
        float mAphe   = m.getAphelionAU();
        float me      = m.getEccentricity();
        float mInc    = m.getOrbitInclinationRad();
        float mArg    = m.getArgumentOfPeriapsisRad();

        float ma   = (mPeri + mAphe) * 0.5f;
        float mb   = ma * sqrt(1 - me * me);
        float maPx = ma * scale;
        float mbPx = mb * scale;
        float mfPx = ma * me * scale;

        pg.pushMatrix();
          pg.translate(pPos.x, pPos.y, pPos.z);
          pg.rotateX(PConstants.PI/2);
          if (mArg != 0) pg.rotateZ(mArg);
          pg.rotateX(mInc);
          pg.translate(-mfPx, 0, 0);
          pg.ellipse(0, 0, 2 * maPx, 2 * mbPx);
        pg.popMatrix();
      }
    }
  }

  public void drawPlanetsAndMoons(PGraphicsOpenGL pg,
                                boolean showLabels,
                                boolean showMoonOrbits) {
    float scaleAUtoPx = PIXELS_PER_AU * simParams.globalScale;
    float sunRadiusPx = sun.getRadius();

    for (Planet p : planets) {
        PVector posPx = p.getPositionAU().copy().mult(scaleAUtoPx);

        pg.pushMatrix();
          pg.translate(posPx.x, posPx.y, posPx.z);

          // 1) Desenha o planeta
          p.display(pg, showLabels, showMoonOrbits, renderingMode, shapeManager, shaderManager);

          // 2) Dentro da mesma matriz de planeta, desenha suas luas
          for (Moon m : p.getMoons()) {
              if (showMoonOrbits) {
                  m.displayOrbit(pg, sunRadiusPx); // órbita é desenhada
              }
              m.display(pg, showLabels, renderingMode, shapeManager, shaderManager);
          }

        pg.popMatrix(); // fecha transformação do planeta
    }
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
    // libera referências
    planets   = null;
    sun       = null;
  }
}
