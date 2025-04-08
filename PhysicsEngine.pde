class PhysicsEngine {
  private PApplet pApplet;
  private ArrayList<Planet> planets;
  private float timeScale;
  private final float gravityFactor; // Nova definição

  // Vetores temporários para os cálculos físicos
  private final PVector tempVec1 = new PVector();
  private final PVector tempVec2 = new PVector();
  private final PVector tempVec3 = new PVector();
  private final PVector tempVec4 = new PVector();

  PhysicsEngine(PApplet pApplet, ArrayList<Planet> planets) {
    this.pApplet = pApplet;
    this.planets = planets;
    timeScale = 1.0f;
    // Inicializa o gravityFactor usando constantes globais
    this.gravityFactor = G_AU * SOL_MASS / (365.25f * 365.25f);
  }
  
  public void update(float dt) {
    if (planets.size() == 0) return;
    Planet sol = planets.get(0);
    for (int i = 1; i < planets.size(); i++) {
      Planet p = planets.get(i);
      updatePlanetPhysics(p, dt, sol);
      p.updateRotation(dt);
      p.updateMoons(dt);
    }
  }
  
  private void updatePlanetPhysics(Planet p, float dt, Planet sol) {
    PVector.sub(sol.position, p.position, tempVec1);
    float rSq_px = tempVec1.magSq();
    float r_px = pApplet.sqrt(rSq_px);
    float r_AU = r_px / PIXELS_PER_AU;
    float invRSq = 1.0f / (r_AU * r_AU);
    float aMag = gravityFactor * PIXELS_PER_AU * invRSq;
    
    tempVec2.set(tempVec1).normalize().mult(aMag);
    tempVec4.set(tempVec2);
    
    PVector velocityDt = PVector.mult(p.velocity, dt);
    tempVec2.mult(0.5f * dt * dt);
    tempVec3.set(velocityDt).add(tempVec2);
    p.position.add(tempVec3);
    
    PVector.sub(sol.position, p.position, tempVec1);
    float rSq_px_new = tempVec1.magSq();
    float r_px_new = pApplet.sqrt(rSq_px_new);
    float r_AU_new = r_px_new / PIXELS_PER_AU;
    float invRSq_new = 1.0f / (r_AU_new * r_AU_new);
    float aMag_new = gravityFactor * PIXELS_PER_AU * invRSq_new;
    tempVec1.normalize().mult(aMag_new);
    
    tempVec4.add(tempVec1).mult(0.5f * dt);
    p.velocity.add(tempVec4);
    p.acceleration.set(tempVec1);
  }
  
  public void setTimeScale(float ts) {
    this.timeScale = ts;
  }
  
  public float getTimeScale() {
    return timeScale;
  }

  public void dispose() {
    // Nenhum recurso extra para liberar aqui
  }
}
