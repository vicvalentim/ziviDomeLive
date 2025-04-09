import processing.core.*;
import processing.opengl.*;
import java.util.ArrayList;

public class Planet {
  private PApplet pApplet;
  float mass;
  float radius;
  PVector position;
  PVector velocity;
  PVector acceleration;
  color col;
  String name;
  ArrayList<Moon> moons;

  float rotationAngle;
  float rotationSpeed;
  float orbitRadius;
  float orbitInclination;
  float anomaly;
  float axisTilt;

  boolean hasRings = false;
  private float rotationFactor = 0.2f;
  PImage texture;
  PImage ringTexture;

  private float ringRotationAngle = 0;
  private float ringRotationSpeed = 0.05f;
  private PShape saturnRingsShape;
  private int cachedRingMode = -1;
  private int renderingMode = 2;

  public Planet(PApplet pApplet,
                float m,
                float r,
                PVector pos,
                PVector vel,
                color c,
                String n,
                float rotationPeriod,
                float orbitInclination,
                float axisTilt,
                PImage texture,
                PImage ringTexture) {
    this.pApplet = pApplet;
    mass = m;
    radius = r;
    position = pos.copy();
    velocity = vel.copy();
    acceleration = new PVector();
    col = c;
    name = n;
    moons = new ArrayList<Moon>();
    rotationAngle = 0;
    rotationSpeed = PApplet.TWO_PI / rotationPeriod;
    orbitRadius = pos.mag();
    this.orbitInclination = orbitInclination;
    this.axisTilt = axisTilt;
    this.texture = texture;
    this.ringTexture = ringTexture;

    if (name.equals("Saturn")) {
      hasRings = true;
    }
  }

  public void updateRotation(float dt) {
    rotationAngle += rotationSpeed * dt * rotationFactor;
    if (hasRings) {
      ringRotationAngle += ringRotationSpeed * dt;
    }
  }

  public void updateMoons(float dt) {
    for (Moon m : moons) {
      m.update(dt, getDrawPosition(), velocity);
    }
  }

  public void addMoon(Moon m) {
    moons.add(m);
  }

  public PVector getDrawPosition() {
    PVector d = position.copy();
    if (d.mag() > 0) {
      d.setMag(d.mag() + SUN_VISUAL_RADIUS);
    }
    return d;
  }

  public void display(PGraphicsOpenGL pg, boolean showLabel, boolean selected, int renderingMode,
                      ShapeManager shapeManager, ShaderManager shaderManager) {
    pg.pushMatrix();
    PVector d = getDrawPosition();
    pg.translate(d.x, d.y, d.z);

    if (hasRings) {
      drawSaturnRings(pg, renderingMode);
    }

    pg.rotateZ(axisTilt);
    pg.rotateY(rotationAngle);

    float scaleFactor = selected ? radius * 1.1f : radius;
    pg.scale(scaleFactor);

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
      pg.pushMatrix();
      PVector labelPos = getDrawPosition();
      labelPos.y -= (radius + 5);
      pg.translate(labelPos.x, labelPos.y, labelPos.z);
      float labelSize = pApplet.max(10, radius * 0.5f);
      pg.fill(255);
      pg.textSize(labelSize);
      pg.textAlign(PConstants.CENTER, PConstants.BOTTOM);
      pg.text(name, 0, 0);
      pg.popMatrix();
    }
  }

  private void drawLabel(PGraphicsOpenGL pg) {
    pg.pushMatrix();
    PVector labelPos = getDrawPosition();
    labelPos.y -= (radius + 5);
    pg.translate(labelPos.x, labelPos.y, labelPos.z);
    float labelSize = pApplet.max(10, radius * 0.5f);
    pg.fill(255);
    pg.textSize(labelSize);
    pg.textAlign(PConstants.CENTER, PConstants.BOTTOM);
    pg.text(name, 0, 0);
    pg.popMatrix();
  }

  private void drawSaturnRings(PGraphicsOpenGL pg, int renderingMode) {
    if (cachedRingMode != renderingMode || saturnRingsShape == null) {
      buildSaturnRingsShape(renderingMode);
      cachedRingMode = renderingMode;
    }

    pg.pushMatrix();
    pg.rotateZ(axisTilt);
    if (axisTilt > PApplet.HALF_PI) {
      pg.rotateY(PApplet.PI);
    }
    pg.rotateY(ringRotationAngle);
    pg.shape(saturnRingsShape);
    pg.popMatrix();
  }

  private void buildSaturnRingsShape(int renderingMode) {
    saturnRingsShape = pApplet.createShape();
    saturnRingsShape.beginShape(PConstants.QUAD_STRIP);
    saturnRingsShape.textureMode(PConstants.NORMAL);

    if (renderingMode == 0) {
      saturnRingsShape.noFill();
      saturnRingsShape.stroke(180);
      saturnRingsShape.strokeWeight(0.5f);
    } else {
      saturnRingsShape.noStroke();
    }

    float innerRingRatio = 1.15f;
    float outerRingRatio = 2.35f;
    int ringBands = 60;
    int ringSegments = 90;
    float step = (outerRingRatio - innerRingRatio) / ringBands;

    float[] gapCenters = {117500f / 60000f, 133600f / 60000f, 136500f / 60000f};
    float[] gapWidths = {4800f / 60000f, 325f / 60000f, 42f / 60000f};

    for (int i = 0; i < ringBands; i++) {
      float r1Ratio = innerRingRatio + i * step;
      float r2Ratio = r1Ratio + step;

      boolean insideGap = false;
      if (renderingMode == 1) {
        for (int g = 0; g < gapCenters.length; g++) {
          float gapStart = gapCenters[g] - gapWidths[g] / 2f;
          float gapEnd = gapCenters[g] + gapWidths[g] / 2f;
          if (r1Ratio >= gapStart && r2Ratio <= gapEnd) {
            insideGap = true;
            break;
          }
        }
      }
      if (insideGap) continue;

      float r1 = radius * r1Ratio;
      float r2 = radius * r2Ratio;

      float u1 = 0.5f + (r1Ratio - 1.75f) * 0.5f;
      float u2 = 0.5f + (r2Ratio - 1.75f) * 0.5f;

      if (renderingMode == 1) {
        float wave = PApplet.sin(i * 0.3f);
        float alphaFactor = 0.6f + 0.4f * wave;
        int alpha = (int)(alphaFactor * 200);
        int base = (int)PApplet.lerp(240, 255, PApplet.sin(i * 0.2f) * 0.5f + 0.5f);
        saturnRingsShape.fill(base, base, base, alpha);
      } else if (renderingMode == 2) {
        saturnRingsShape.fill(255);
      }

      for (int j = 0; j <= ringSegments; j++) {
        float angle = PApplet.TWO_PI * j / ringSegments;
        float cosA = PApplet.cos(angle);
        float sinA = PApplet.sin(angle);
        saturnRingsShape.vertex(r1 * cosA, 0, r1 * sinA, u1, 0.0f);
        saturnRingsShape.vertex(r2 * cosA, 0, r2 * sinA, u2, 1.0f);
      }
    }

    saturnRingsShape.endShape();

    if (renderingMode == 2 && ringTexture != null) {
      saturnRingsShape.setTexture(ringTexture);
    }
  }

  public void setRenderingMode(int mode) {
    this.renderingMode = mode;
  }

  public int getRenderingMode() {
    return renderingMode;
  }

  public void buildShape(PApplet p, ShapeManager shapeManager) {
    // Cria ou atualiza a forma no cache
    shapeManager.buildShape(name, renderingMode, texture);

    // Aplica a cor no modo sólido
    if (renderingMode == 1) {
      PShape shape = shapeManager.getShape(name, renderingMode, texture);
      if (shape != null) {
        shape.setFill(col);
      }
    }

    // Força reconstrução dos anéis se necessário
    if (hasRings && ringTexture != null && renderingMode == 2) {
      cachedRingMode = -1;
    }
  }

  public void dispose() {
    if (moons != null) {
      for (Moon m : moons) m.dispose();
      moons.clear();
      moons = null;
    }
  }
}
