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
  
  // Spin e parâmetros orbitais
  float rotationAngle;
  float rotationSpeed;
  float orbitRadius;
  float orbitInclination;
  float anomaly;
  float axisTilt;
  
  // Renderização
  PShape shape;
  boolean hasRings = false;
  PShape saturnRingsShape;
  float ringRotationAngle = 0;
  float ringRotationSpeed = 0.05f;
  private float rotationFactor = 0.2f;
  
  // Textura para o Sol (se aplicável)
  PImage sunTexture;
  
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
    anomaly = 0;
    this.axisTilt = axisTilt;
    this.sunTexture = sunTexture;
    
    // Criação inicial do shape (pode ser recriado no display)
    shape = pApplet.createShape(SPHERE, 1);
    shape.disableStyle();  // força o uso do estilo corrente
    shape.setFill(c);
    
    if (n.equals("Saturno")) {
      hasRings = true;
      buildSaturnRingsShape();
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
      m.update(dt, position, velocity);
    }
  }
  
  public void addMoon(Moon m) {
    moons.add(m);
  }
  
  // Retorna a posição para desenho. Para planetas não-Sol, desloca adicionando o raio visual do Sol.
  public PVector getDrawPosition() {
    if (name.equals("Sol"))
      return position.copy();
    else {
      PVector d = position.copy();
      if (d.mag() > 0) {
        d.setMag(d.mag() + SUN_VISUAL_RADIUS);
      }
      return d;
    }
  }
  
  /**
   * Exibe o planeta conforme o modo de renderização.
   * renderingMode: 0 = Wireframe, 1 = Solid, 2 = Textured.
   */
  public void display(PGraphicsOpenGL pg, boolean showLabel, boolean selected, int renderingMode) {
    pg.pushMatrix();
      PVector d = getDrawPosition();
      pg.translate(d.x, d.y, d.z);
      
      // Aplica rotação: rotaciona Z pelo axisTilt e Y pela rotação atual
      pg.rotateZ(axisTilt);
      pg.rotateY(rotationAngle);
      
      // Calcula o fator de escala; assume que a escala foi aplicada na criação, ou seja, use "radius" conforme definido
      float baseSize = selected ? radius * 1.1f : radius;
      float scaleFactor = baseSize;
      
      // Cria um shape novo para esse frame e força o estilo atual
      PShape myShape = pApplet.createShape(SPHERE, 1);
      myShape.disableStyle();
      
      if (renderingMode == 0) {  // WIREFRAME
        pg.noFill();
        pg.stroke(255); // Apenas linhas brancas
      } else if (renderingMode == 1) {  // SOLID
        pg.noStroke();
        pg.fill(col);
      } else if (renderingMode == 2) {  // TEXTURED
        pg.noStroke();
        if (name.equals("Sol") && sunTexture != null) {
          myShape.setTexture(sunTexture);
        } else {
          pg.fill(col);
        }
      }
      
      pg.scale(scaleFactor);
      pg.shape(myShape);
    pg.popMatrix();
    
    if (showLabel) {
      pg.pushMatrix();
        PVector labelPos = getDrawPosition();
        labelPos.y -= (radius + 5);
        pg.translate(labelPos.x, labelPos.y, labelPos.z);
        float labelSize = pApplet.max(10, radius * 0.5f);
        pg.fill(255);
        pg.textSize(labelSize);
        pg.textAlign(CENTER, BOTTOM);
        pg.text(name, 0, 0);
      pg.popMatrix();
    }
    pg.resetShader();
  }
  
  private void buildSaturnRingsShape() {
    saturnRingsShape = pApplet.createShape();
    saturnRingsShape.beginShape(QUAD_STRIP);
    saturnRingsShape.noStroke();
    
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
      for (int g = 0; g < gapCenters.length; g++) {
        float gapStart = gapCenters[g] - gapWidths[g] / 2f;
        float gapEnd   = gapCenters[g] + gapWidths[g] / 2f;
        if (r1Ratio >= gapStart && r2Ratio <= gapEnd) {
          insideGap = true;
          break;
        }
      }
      if (insideGap) continue;
      
      float r1 = radius * r1Ratio;
      float r2 = radius * r2Ratio;
      
      float wave = pApplet.sin(i * 0.3f);
      float alphaFactor = 0.6f + 0.4f * wave;
      int alpha = (int)(alphaFactor * 200);
      int base = (int)pApplet.lerp(240, 255, pApplet.sin(i * 0.2f) * 0.5f + 0.5f);
      saturnRingsShape.fill(base, base, base, alpha);
      
      for (int j = 0; j <= ringSegments; j++) {
        float angle = TWO_PI * j / ringSegments;
        float cosA = pApplet.cos(angle);
        float sinA = pApplet.sin(angle);
        saturnRingsShape.vertex(r1 * cosA, 0, r1 * sinA);
        saturnRingsShape.vertex(r2 * cosA, 0, r2 * sinA);
      }
    }
    saturnRingsShape.endShape();
  }
  
  public void setRotationSpeed(float speed) {
    rotationSpeed = speed;
  }
  
  public float getDrawnRadius() {
    return radius;
  }
  
  public void dispose() {
    if (shape != null) {
      shape = null;
    }
    if (moons != null) {
      for (Moon m : moons) {
        m.dispose();
      }
      moons.clear();
      moons = null;
    }
  }
}
