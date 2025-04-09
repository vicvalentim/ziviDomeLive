import processing.core.*;
import processing.opengl.*;

public class Moon {
  private PApplet pApplet;
  private final float pixelsPerAU;
  private final float G_AU;
  private final float moonOrbitCalibration;

  float mass;
  float moonSizeRatio;
  float moonOrbitFactor;
  PVector position;
  PVector velocity;
  PVector acceleration;
  float orbitAngle = 0;
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

  private int renderingMode = 2; // 0: wireframe, 1: solid, 2: textured
  private PImage texture; // textura opcional por lua

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
    this.pixelsPerAU = pixelsPerAU;
    this.G_AU = G_AU;
    this.moonOrbitCalibration = moonOrbitCalibration;

    this.mass = mass;
    this.moonSizeRatio = moonSizeRatio;
    this.moonOrbitFactor = moonOrbitFactor;
    this.position = pos.copy();
    this.velocity = vel.copy();
    this.acceleration = new PVector();
    this.col = col;
    this.name = name;
    this.parent = parent;
    this.inclination = inclination;
    this.eccentricity = eccentricity;
    this.argumentPeriapsis = argumentPeriapsis;
    this.alignWithPlanetAxis = alignWithPlanetAxis;

    rotationMatrix = new PMatrix3D();
    if (alignWithPlanetAxis) {
      rotationMatrix.rotate(parent.axisTilt, 0, 0, 1);
    } else {
      rotationMatrix.rotate(argumentPeriapsis, 0, 1, 0);
      rotationMatrix.rotate(inclination, 1, 0, 0);
    }

    orbitalAngle = PApplet.atan2(position.z, position.x);
    cosOrbital = PApplet.cos(orbitalAngle);
    sinOrbital = PApplet.sin(orbitalAngle);
  }

  public void update(float dt, PVector parentPos, PVector parentVel) {
    float r_AU_physical = (parent.radius * moonOrbitFactor) / pixelsPerAU;
    float v_AU_per_day = PApplet.sqrt(G_AU * parent.mass / r_AU_physical);
    float v_pixels_per_day = v_AU_per_day * pixelsPerAU;

    float r_px_visual = parent.radius * (1 + (moonOrbitFactor / moonOrbitCalibration));
    float deltaAngle = (v_pixels_per_day / r_px_visual) * dt;

    float cosDelta = PApplet.cos(deltaAngle);
    float sinDelta = PApplet.sin(deltaAngle);
    float newCos = cosOrbital * cosDelta - sinOrbital * sinDelta;
    float newSin = sinOrbital * cosDelta + cosOrbital * sinDelta;
    cosOrbital = newCos;
    sinOrbital = newSin;
    orbitalAngle += deltaAngle;

    float newX = r_px_visual * cosOrbital;
    float newZ = r_px_visual * sinOrbital;

    PVector tempVec1 = new PVector(newX, 0, newZ);
    rotationMatrix.mult(tempVec1, tempVec1);
    position.set(tempVec1);

    tempVec1.set(-cosOrbital, 0, sinOrbital);
    rotationMatrix.mult(tempVec1, tempVec1);
    tempVec1.normalize().mult(v_pixels_per_day);
    velocity.set(tempVec1);
  }

  public float getDrawnRadius() {
    return parent.radius * moonSizeRatio;
  }

  public PVector getDrawPosition() {
    return PVector.add(parent.getDrawPosition(), position);
  }

  public void displayOrbit(PGraphicsOpenGL pg) {
    pg.pushMatrix();
    PVector parentDraw = parent.getDrawPosition();
    pg.translate(parentDraw.x, parentDraw.y, parentDraw.z);
    pg.rotateX(PConstants.HALF_PI);
    float orbitRadius = parent.radius * (1 + (moonOrbitFactor / moonOrbitCalibration));
    pg.noFill();
    pg.stroke(150, 150, 255, 150);
    pg.strokeWeight(1);
    pg.ellipse(0, 0, orbitRadius * 2, orbitRadius * 2);
    pg.popMatrix();
  }

  public void display(PGraphicsOpenGL pg, boolean showLabel,
                      int renderingMode, ShapeManager shapeManager,
                      ShaderManager shaderManager) {
    this.renderingMode = renderingMode;

    pg.pushMatrix();
    PVector d = getDrawPosition();
    pg.translate(d.x, d.y, d.z);
    pg.scale(getDrawnRadius());

    PShape shape = shapeManager.getShape(name, renderingMode, texture);

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
      drawLabel(pg);
    }
  }

  private void drawLabel(PGraphicsOpenGL pg) {
    pg.pushMatrix();
    PVector labelPos = getDrawPosition();
    labelPos.y -= (getDrawnRadius() + 5);
    pg.translate(labelPos.x, labelPos.y, labelPos.z);
    float labelSize = pApplet.max(10, getDrawnRadius() * 0.5f);
    pg.fill(255);
    pg.textSize(labelSize);
    pg.textAlign(PConstants.CENTER, PConstants.BOTTOM);
    pg.text(name, 0, -getDrawnRadius() - 5);
    pg.popMatrix();
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