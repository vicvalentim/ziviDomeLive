// Sun.java
import processing.core.*;
import processing.opengl.*;

public class Sun implements CelestialBody {
  private final PApplet pApplet;

  // —— Display ——
  private float radiusPx;           // raio para display em pixels
  private final float baseRatio;    // radiusPx / SUN_VISUAL_RADIUS
  private final color col;
  private final PVector position;   // sempre (0,0,0) + escala
  private float rotationAngle = 0;
  private final float rotationSpeed;    // rad/dia
  private final PImage texture;
  private PShape shape;
  private int renderingMode = 2;

  // —— Física ——
  private final float massSolar;         // em M☉
  private final float radiusAU;          // em AU
  private final float rotationPeriodDays;// em dias
  private final PVector positionAU = new PVector(0,0,0);
  private final PVector velocityAU = new PVector(0,0,0);

  /**
   * @param pApplet               Sketch
   * @param radiusPx              Raio visual em pixels
   * @param massSolar             Massa em M☉
   * @param radiusAU              Raío físico em AU
   * @param rotationPeriodDays    Período de rotação em dias
   * @param initialPixelPos       Posição em pixels (normalmente 0,0,0)
   * @param displayColor          Cor RGB
   * @param texture               Textura do Sol
   */
  public Sun(PApplet pApplet,
             float radiusPx,
             float massSolar,
             float radiusAU,
             float rotationPeriodDays,
             PVector initialPixelPos,
             color displayColor,
             PImage texture) {
    this.pApplet            = pApplet;
    this.radiusPx           = radiusPx;
    this.baseRatio          = radiusPx / SUN_VISUAL_RADIUS;
    this.massSolar          = massSolar;
    this.radiusAU           = radiusAU;
    this.rotationPeriodDays = rotationPeriodDays;
    this.rotationSpeed      = PApplet.TWO_PI / rotationPeriodDays;
    this.position           = initialPixelPos.copy();
    this.col                = displayColor;
    this.texture            = texture;
  }

  // —— Ajuste de escala visual ——
  public void applyScalingFactors(SimParams simParams) {
    this.radiusPx = SUN_VISUAL_RADIUS * baseRatio * simParams.globalScale;
  }

  // —— Animação visual ——
  public void update(float dtDays) {
    rotationAngle += rotationSpeed * dtDays;
  }

  public void display(PGraphicsOpenGL pg,
                      boolean showLabel,
                      ShaderManager shaderManager) {
    pg.pushMatrix();
      pg.translate(position.x, position.y, position.z);
      pg.rotateY(rotationAngle);
      pg.scale(radiusPx);

      if (renderingMode == 0) {
        pg.noFill();
        pg.stroke(WIREFRAME_COLOR);
        pg.strokeWeight(WIREFRAME_STROKE_WEIGHT);
      } else if (renderingMode == 2) {
        PShader sh = shaderManager.getShader("sun");
        if (sh != null && texture != null) {
          sh.set("texSampler", texture);
          pg.shader(sh);
        } else {
          pg.fill(col);
        }
        pg.noStroke();
      } else {
        pg.fill(col);
        pg.noStroke();
      }

      if (shape != null) {
        pg.shape(shape);
      }
      pg.resetShader();
    pg.popMatrix();

    if (showLabel) drawLabel(pg);
  }

  private void drawLabel(PGraphicsOpenGL pg) {
    pg.pushMatrix();
      PVector lp = position.copy();
      lp.y -= radiusPx * 1.2f;
      pg.translate(lp.x, lp.y, lp.z);
      pg.fill(255);
      pg.textSize(pApplet.max(10, radiusPx * 0.4f));
      pg.textAlign(CENTER, BOTTOM);
      pg.text("Sun", 0, 0);
    pg.popMatrix();
  }

  public void setRenderingMode(int mode) {
    this.renderingMode = mode;
  }
  public int getRenderingMode() {
    return renderingMode;
  }

  public void buildShape(PApplet p, ShapeManager shapeManager) {
    shape = shapeManager.getShape("Sun", renderingMode, texture);
    if (renderingMode == 1 && shape != null) {
      shape.setFill(col);
    }
  }

  public float getRadius() {
    return radiusPx;
  }

  public float getMass() {
    return massSolar;
  }

  public void dispose() {
    shape = null;
  }

  // —— CelestialBody ——
  @Override public PVector getPositionAU()         { return positionAU; }
  @Override public PVector getVelocityAU()         { return velocityAU; }
  @Override public float   getMassSolar()          { return massSolar; }
  @Override public CelestialBody getCentralBody()  { return null;            }
  @Override public void    propagateKepler(float dtDays) { /* nada */ }

  @Override public float getPerihelionAU()         { return 0; }
  @Override public float getAphelionAU()           { return 0; }
  @Override public float getEccentricity()         { return 0; }
  @Override public float getOrbitInclinationRad()  { return 0; }
  @Override public float getArgumentOfPeriapsisRad(){ return 0; }

  @Override public float getRadiusAU()             { return radiusAU; }
  @Override public float getRotationPeriodDays()   { return rotationPeriodDays; }
}
