import processing.core.*;
import processing.opengl.*;

/**
 * Representa uma lua que orbita um planeta.
 * A Moon depende completamente de Planet para escala visual, posição e massa orbital.
 * Nenhuma constante como SUN_VISUAL_RADIUS é usada diretamente.
 */
public class Moon {
  private PApplet pApplet;
  private SimParams simParams;
  private final float pixelsPerAU;
  private final float G_AU;
  private final float moonOrbitCalibration;

  float mass;
  float moonSizeRatio;
  float moonOrbitFactor;
  PVector position;
  PVector velocity;
  PVector acceleration;
  color col;
  String name;
  Planet parent;

  float inclination;
  float eccentricity;
  float argumentPeriapsis;
  boolean alignWithPlanetAxis;

  PMatrix3D rotationMatrix;

  float orbitalAngle;
  float cosOrbital, sinOrbital;

  private int renderingMode = 2;
  private PImage texture;


  // Cache para posição visual
  private final PVector cachedDrawPosition = new PVector();
  private float cachedSunRadius = -1;
  private boolean drawPositionDirty = true;

  // Cache para forma e label e órbitas
  private PShape cachedShape;
  private int cachedRenderingMode = -1;

  private float cachedLabelSize = -1;
  private float cachedMoonRadius = -1;

  private float cachedVisualOrbitRadius = -1;
  private float cachedParentRadius = -1;


  public Moon(PApplet pApplet,
              float mass,
              float moonSizeRatio,
              float moonOrbitFactor,
              PVector pos,
              PVector vel,
              color col,
              String name,
              Planet parent,
              float inclination,
              float eccentricity,
              float argumentPeriapsis,
              boolean alignWithPlanetAxis,
              float pixelsPerAU,
              float G_AU,
              float moonOrbitCalibration) {
    this.pApplet = pApplet;
    this.mass = mass;
    this.moonSizeRatio = moonSizeRatio;
    this.moonOrbitFactor = moonOrbitFactor;
    this.position = pos.copy();
    this.velocity = vel.copy();
    this.acceleration = new PVector();
    this.col = col;
    this.name = name;
    this.parent = parent;
    this.simParams = parent.simParams;
    this.inclination = inclination;
    this.eccentricity = eccentricity;
    this.argumentPeriapsis = argumentPeriapsis;
    this.alignWithPlanetAxis = alignWithPlanetAxis;
    this.pixelsPerAU = pixelsPerAU;
    this.G_AU = G_AU;
    this.moonOrbitCalibration = moonOrbitCalibration;

    resetRotationMatrix();

    orbitalAngle = PApplet.atan2(pos.z, pos.x);
    cosOrbital = PApplet.cos(orbitalAngle);
    sinOrbital = PApplet.sin(orbitalAngle);
  }

  // exemplo de aplicação na sua função update:
  public void update(float dt, PVector parentPos, PVector parentVel) {
    float v_pixels_per_day = PApplet.sqrt(G_AU * parent.mass / getPhysicalOrbitRadiusAU()) * pixelsPerAU;
    float deltaAngle = (v_pixels_per_day / getVisualOrbitRadius()) * dt;

    float cosDelta = PApplet.cos(deltaAngle);
    float sinDelta = PApplet.sin(deltaAngle);
    float newCos = cosOrbital * cosDelta - sinOrbital * sinDelta;
    float newSin = sinOrbital * cosDelta + cosOrbital * sinDelta;

    cosOrbital = newCos;
    sinOrbital = newSin;
    orbitalAngle += deltaAngle;

    position.set(computePositionFromAngle(orbitalAngle));
    velocity.set(computeTangentialVelocityFromAngle(orbitalAngle));

    drawPositionDirty = true; // ← Marca o cache como sujo
  }


  public void applyScalingFactors(SimParams simParams) {
    float angle = PApplet.atan2(position.z, position.x);
    position.set(computePositionFromAngle(angle));
    velocity.set(computeTangentialVelocityFromAngle(angle));
    drawPositionDirty = true; // ← Marca o cache como sujo
  }

  public float getDrawnRadius() {
    return parent.getScaledRadius() * moonSizeRatio;
  }

  public PVector getDrawPosition(float sunRadius) {
    if (drawPositionDirty || cachedSunRadius != sunRadius) {
      cachedDrawPosition.set(parent.getDrawPosition(sunRadius)).add(position);
      cachedSunRadius = sunRadius;
      drawPositionDirty = false;
    }
    return cachedDrawPosition;
  }

  public void displayOrbit(PGraphicsOpenGL pg, float sunRadius) {
    pg.pushMatrix();
    PVector parentDraw = parent.getDrawPosition(sunRadius);
    pg.translate(parentDraw.x, parentDraw.y, parentDraw.z);
    pg.rotateX(PConstants.HALF_PI);

    // Usa raio visual cacheado da órbita
    float r_visual = getCachedVisualOrbitRadius();

    pg.noFill();
    pg.stroke(150, 150, 255, 150);
    pg.strokeWeight(1);
    pg.ellipse(0, 0, r_visual * 2, r_visual * 2);
    pg.popMatrix();
  }

  public void display(PGraphicsOpenGL pg, boolean showLabel,
                      int renderingMode, ShapeManager shapeManager,
                      ShaderManager shaderManager, float sunRadius) {
    this.renderingMode = renderingMode;

    pg.pushMatrix();
    PVector d = getDrawPosition(sunRadius);
    pg.translate(d.x, d.y, d.z);
    pg.scale(getDrawnRadius());

    // Usa shape cacheado
    PShape shape = getCachedShape(shapeManager);

    if (renderingMode == 0) {
      pg.noFill();
      pg.stroke(WIREFRAME_COLOR);
      pg.strokeWeight(WIREFRAME_STROKE_WEIGHT);
    } else if (renderingMode == 1) {
      pg.noStroke();
      pg.fill(col);
    } else if (renderingMode == 2) {
      pg.noStroke();
      if (texture != null) {
        PShader shader = shaderManager.getShader("planet");
        if (shader != null) pg.shader(shader);
      } else {
        pg.fill(col);
      }
    }

    pg.shape(shape);
    pg.resetShader();
    pg.popMatrix();

    if (showLabel) {
      drawLabel(pg, sunRadius, d);
    }
  }

  private void drawLabel(PGraphicsOpenGL pg, float sunRadius, PVector drawPos) {
    pg.pushMatrix();
    PVector labelPos = drawPos.copy();
    labelPos.y -= (getDrawnRadius() + 5);
    pg.translate(labelPos.x, labelPos.y, labelPos.z);

    // Usa tamanho cacheado para a label
    pg.fill(255);
    pg.textSize(getCachedLabelSize());
    pg.textAlign(PConstants.CENTER, PConstants.BOTTOM);
    pg.text(name, 0, -getDrawnRadius() - 5);
    pg.popMatrix();
  }


  // --- Métodos de Cache ---

  /**
  * Retorna o shape cacheado da lua.
  */
  private PShape getCachedShape(ShapeManager shapeManager) {
    if (cachedShape == null || cachedRenderingMode != renderingMode) {
      cachedShape = shapeManager.getShape(name, renderingMode, texture);
      cachedRenderingMode = renderingMode;
    }
    return cachedShape;
  }

  /**
  * Retorna o tamanho cacheado da label, dependente do raio atual da lua.
  */
  private float getCachedLabelSize() {
    float drawnRadius = getDrawnRadius();
    if (cachedMoonRadius != drawnRadius) {
      cachedLabelSize = pApplet.max(10, drawnRadius * 0.5f);
      cachedMoonRadius = drawnRadius;
    }
    return cachedLabelSize;
  }

  /**
  * Retorna o raio visual cacheado da órbita da lua.
  */
  private float getCachedVisualOrbitRadius() {
    float currentParentRadius = parent.getScaledRadius();
    if (cachedParentRadius != currentParentRadius) {
      cachedVisualOrbitRadius = currentParentRadius * (1 + moonOrbitFactor / moonOrbitCalibration);
      cachedParentRadius = currentParentRadius;
    }
    return cachedVisualOrbitRadius;
  }

  // ---------- Encapsulamento Estrutural ----------

  private float getVisualOrbitRadius() {
    return parent.getScaledRadius() * (1 + moonOrbitFactor / moonOrbitCalibration);
  }

  private float getPhysicalOrbitRadiusAU() {
    return (parent.getScaledRadius() * moonOrbitFactor) / pixelsPerAU;
  }

  private PVector computePositionFromAngle(float angle) {
    float r_px_visual = getVisualOrbitRadius();
    PVector pos = new PVector(PApplet.cos(angle) * r_px_visual, 0, PApplet.sin(angle) * r_px_visual);
    rotationMatrix.mult(pos, pos);
    return pos;
  }

  private PVector computeTangentialVelocityFromAngle(float angle) {
    float v_pixels_per_day = PApplet.sqrt(G_AU * parent.mass / getPhysicalOrbitRadiusAU()) * pixelsPerAU;
    PVector tangent = new PVector(-PApplet.sin(angle), 0, PApplet.cos(angle));
    rotationMatrix.mult(tangent, tangent);
    tangent.normalize().mult(v_pixels_per_day);
    return tangent;
  }

  public void resetRotationMatrix() {
    rotationMatrix = new PMatrix3D();
    if (alignWithPlanetAxis) {
      rotationMatrix.rotate(parent.axisTilt, 0, 0, 1);
    } else {
      rotationMatrix.rotate(argumentPeriapsis, 0, 1, 0);
      rotationMatrix.rotate(inclination, 1, 0, 0);
    }
  }

  public void setRenderingMode(int mode) {
    this.renderingMode = mode;
  }

  public int getRenderingMode() {
    return renderingMode;
  }

  public void buildShape(PApplet p, ShapeManager shapeManager) {
    shapeManager.buildShape(name, renderingMode, texture);
  }

  public void setTexture(PImage texture) {
    this.texture = texture;
  }

  public void dispose() {
    texture = null;
  }
}
