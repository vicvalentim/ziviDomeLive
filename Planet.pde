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
  PImage sunTexture;

  // Cache de formas
  private PShape shapeWire, shapeSolid, shapeTextured;
  private boolean shapesCached = false;

  private PShape saturnRingsShape;
  private float ringRotationAngle = 0;
  private float ringRotationSpeed = 0.05f;
  private int cachedRingMode = -1;

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
                PImage sunTexture) {
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
    rotationSpeed = TWO_PI / rotationPeriod;
    orbitRadius = pos.mag();
    this.orbitInclination = orbitInclination;
    this.axisTilt = axisTilt;
    this.sunTexture = sunTexture;

    if (n.equals("Saturno")) {
      hasRings = true;
    }
  }

  private void cacheShapes() {
    shapeWire = pApplet.createShape(SPHERE, 1);
    shapeWire.disableStyle();

    shapeSolid = pApplet.createShape(SPHERE, 1);
    shapeSolid.disableStyle();

    shapeTextured = pApplet.createShape(SPHERE, 1);
    shapeTextured.disableStyle();
    if (sunTexture != null) {
      shapeTextured.setTexture(sunTexture);
    }

    shapesCached = true;
  }

  public void updateRotation(float dt) {
    rotationAngle += rotationSpeed * dt * rotationFactor;
    if (hasRings) {
      ringRotationAngle += ringRotationSpeed * dt;
    }
  }

  public void updateMoons(float dt) {
    for (Moon m : moons) {
      m.update(dt, position, velocity);
    }
  }

  public void addMoon(Moon m) {
    moons.add(m);
  }

  public PVector getDrawPosition() {
    if (name.equals("Sol")) {
      return position.copy();
    } else {
      PVector d = position.copy();
      if (d.mag() > 0) {
        d.setMag(d.mag() + SUN_VISUAL_RADIUS);
      }
      return d;
    }
  }

  public void display(PGraphicsOpenGL pg, boolean showLabel, boolean selected, int renderingMode) {
    pg.pushMatrix();
    PVector d = getDrawPosition();
    pg.translate(d.x, d.y, d.z);

    // Renderiza os anéis de Saturno antes do planeta
    if (hasRings) {
        drawSaturnRings(pg, renderingMode);
    }

    // Rotação do planeta
    pg.rotateZ(axisTilt);
    pg.rotateY(rotationAngle);

    float scaleFactor = selected ? radius * 1.1f : radius;
    pg.scale(scaleFactor);

    if (!shapesCached) cacheShapes();

    // Seleciona e desenha a forma conforme o modo de renderização
    if (renderingMode == 0) {
      pg.noFill();
      pg.stroke(255);
      pg.shape(shapeWire);
    } else if (renderingMode == 1) {
      pg.noStroke();
      pg.fill(col);
      pg.shape(shapeSolid);
    } else if (renderingMode == 2) {
      pg.noStroke();
      if (name.equals("Sol") && sunTexture != null) {
        pg.shape(shapeTextured);
      } else {
        pg.fill(col);
        pg.shape(shapeSolid);
      }
    } else {
      pg.noStroke();
      pg.fill(col);
      pg.shape(shapeSolid);
    }

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

    pg.resetShader();
  }

  private void drawSaturnRings(PGraphicsOpenGL pg, int renderingMode) {
    if (cachedRingMode != renderingMode || saturnRingsShape == null) {
      buildSaturnRingsShape(renderingMode);
      cachedRingMode = renderingMode;
    }

    pg.pushMatrix();
    pg.rotateZ(axisTilt);
    if (axisTilt > HALF_PI) {
      pg.rotateY(PI);
    }
    pg.rotateY(ringRotationAngle);
    pg.shape(saturnRingsShape);
    pg.popMatrix();
  }

  private void buildSaturnRingsShape(int renderingMode) {
    saturnRingsShape = pApplet.createShape();
    saturnRingsShape.beginShape(PConstants.QUAD_STRIP);

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

    float[] gapCenters = {
      117500f / 60000f,
      133600f / 60000f,
      136500f / 60000f
    };
    float[] gapWidths = {
      4800f / 60000f,
      325f / 60000f,
      42f / 60000f
    };

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

      if (renderingMode == 1) {
        float wave = PApplet.sin(i * 0.3f);
        float alphaFactor = 0.6f + 0.4f * wave;
        int alpha = (int)(alphaFactor * 200);
        int base = (int)PApplet.lerp(240, 255, PApplet.sin(i * 0.2f) * 0.5f + 0.5f);
        saturnRingsShape.fill(base, base, base, alpha);
      } else if (renderingMode == 2) {
        saturnRingsShape.fill(255, 255, 255, 180);
      }

      for (int j = 0; j <= ringSegments; j++) {
        float angle = PApplet.TWO_PI * j / ringSegments;
        float cosA = PApplet.cos(angle);
        float sinA = PApplet.sin(angle);
        saturnRingsShape.vertex(r1 * cosA, 0, r1 * sinA);
        saturnRingsShape.vertex(r2 * cosA, 0, r2 * sinA);
      }
    }

    saturnRingsShape.endShape();
  }

  public void dispose() {
    shapeWire = shapeSolid = shapeTextured = null;
    if (moons != null) {
      for (Moon m : moons) m.dispose();
      moons.clear();
      moons = null;
    }
  }
}
