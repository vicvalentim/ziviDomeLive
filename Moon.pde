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
  
  // Parâmetros orbitais
  float inclination;
  float eccentricity;
  float argumentPeriapsis;
  boolean alignWithPlanetAxis;
  
  // Matriz de rotação pré-calculada
  PMatrix3D rotationMatrix;
  
  // Variáveis para atualização do ângulo orbital
  float orbitalAngle;
  float cosOrbital, sinOrbital;
  
  // Cache do PShape para renderização
  private PShape shapeWire;
  private PShape shapeSolid;
  private boolean shapesCached = false;
  
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
    
    // Inicializa o cache do shape
    shapeWire = pApplet.createShape(SPHERE, 1);
    shapeWire.disableStyle();
    
    shapeSolid = pApplet.createShape(SPHERE, 1);
    shapeSolid.disableStyle();
    shapeSolid.setFill(col);
    
    shapesCached = true;
  }
  
  public void update(float dt, PVector parentPos, PVector parentVel) {
    PVector tempVec1 = new PVector();
    
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
    
    tempVec1.set(newX, 0, newZ);
    rotationMatrix.mult(tempVec1, tempVec1);
    position.set(tempVec1);
    
    tempVec1.set(-cosOrbital, 0, sinOrbital);
    rotationMatrix.mult(tempVec1, tempVec1);
    tempVec1.normalize();
    tempVec1.mult(v_pixels_per_day);
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
      pg.rotateX(PI / 2);
      float orbitRadius = parent.radius * (1 + (moonOrbitFactor / moonOrbitCalibration));
      pg.noFill();
      pg.stroke(150, 150, 255, 150);
      pg.strokeWeight(1);
      pg.ellipse(0, 0, orbitRadius * 2, orbitRadius * 2);
    pg.popMatrix();
  }
  
  /**
   * Exibe a lua conforme o modo de renderização.
   * renderingMode: 0 = Wireframe, 1 = Solid, 2 = Textured (normalmente, luas caem no modo solid)
   */
  public void display(PGraphicsOpenGL pg, boolean showLabel, int renderingMode) {
    pg.pushMatrix();
      PVector d = getDrawPosition();
      pg.translate(d.x, d.y, d.z);
      pg.scale(getDrawnRadius());
      
      PShape myShape = pApplet.createShape(SPHERE, 1);
      myShape.disableStyle();
      
      if (renderingMode == 0) {  // WIREFRAME
        pg.noFill();
        pg.stroke(255);
      } else if (renderingMode == 1) {  // SOLID
        pg.noStroke();
        pg.fill(col);
        myShape.setFill(col);
      } else if (renderingMode == 2) {  // TEXTURED: para luas, fallback para solid
        pg.noStroke();
        pg.fill(col);
        myShape.setFill(col);
      } else {
        pg.noStroke();
        pg.fill(col);
        myShape.setFill(col);
      }
      
      pg.shape(myShape);
    pg.popMatrix();
    
    if (showLabel) {
      pg.pushMatrix();
        PVector labelPos = getDrawPosition();
        labelPos.y -= (getDrawnRadius() + 5);
        pg.translate(labelPos.x, labelPos.y, labelPos.z);
        float labelSize = pApplet.max(10, getDrawnRadius() * 0.5f);
        pg.fill(255);
        pg.textSize(labelSize);
        pg.textAlign(CENTER, BOTTOM);
        pg.text(name, 0, -getDrawnRadius() - 5);
      pg.popMatrix();
    }
    pg.resetShader();
  }
  
  public void dispose() {
    if (shapesCached) {
      shapeWire.disableStyle();
      shapeSolid.disableStyle();
      shapeWire = null;
      shapeSolid = null;
      shapesCached = false;
    }
  }
}
