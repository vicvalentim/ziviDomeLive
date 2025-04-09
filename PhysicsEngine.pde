class PhysicsEngine {
  private PApplet pApplet;
  private ArrayList<Planet> planets;
  private Sun sun;  // Mudança: Agora usamos o Sun como um objeto separado
  private float timeScale;
  private final float gravityFactor; // Nova definição

  // Vetores temporários para os cálculos físicos
  private final PVector tempVec1 = new PVector();
  private final PVector tempVec2 = new PVector();
  private final PVector tempVec3 = new PVector();
  private final PVector tempVec4 = new PVector();

  PhysicsEngine(PApplet pApplet, ArrayList<Planet> planets, Sun sun) {
    this.pApplet = pApplet;
    this.planets = planets;
    this.sun = sun; // Inicializa a instância do Sol
    timeScale = 1.0f;
    // Inicializa o gravityFactor usando constantes globais
    this.gravityFactor = G_AU * SOL_MASS / (365.25f * 365.25f);
  }
  
  public void update(float dt) {
    if (planets.size() == 0 || sun == null) return;  // Verifica se o Sol está presente
    for (int i = 0; i < planets.size(); i++) {
      Planet p = planets.get(i);
      updatePlanetPhysics(p, dt, sun);  // Passa o Sol separado para o cálculo da física
      p.updateRotation(dt);
      p.updateMoons(dt);
    }
  }
  
  private void updatePlanetPhysics(Planet p, float dt, Sun sun) {
    // Calcular a distância e a gravidade entre o Sol e o planeta
    PVector.sub(sun.getPosition(), p.position, tempVec1);
    float rSq_px = tempVec1.magSq();
    float r_px = pApplet.sqrt(rSq_px);
    float r_AU = r_px / PIXELS_PER_AU;
    float invRSq = 1.0f / (r_AU * r_AU);
    float aMag = gravityFactor * PIXELS_PER_AU * invRSq;  // Aceleração gravitacional
    
    tempVec2.set(tempVec1).normalize().mult(aMag);
    tempVec4.set(tempVec2);
    
    // Atualização da posição do planeta
    PVector velocityDt = PVector.mult(p.velocity, dt);
    tempVec2.mult(0.5f * dt * dt);
    tempVec3.set(velocityDt).add(tempVec2);
    p.position.add(tempVec3);
    
    // Atualiza a velocidade do planeta
    PVector.sub(sun.getPosition(), p.position, tempVec1);
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
