class PhysicsEngine {
  private PApplet pApplet;
  private ArrayList<Planet> planets;
  private Sun sun;  // Agora o Sol é um objeto separado
  private float timeScale;
  private final float gravityFactor; // Nova definição
  
  // Vetores temporários para os cálculos físicos
  private final PVector tempVec1 = new PVector();
  private final PVector tempVec2 = new PVector();
  private final PVector tempVec3 = new PVector();
  private final PVector tempVec4 = new PVector();

  // Construtor da classe PhysicsEngine
  PhysicsEngine(PApplet pApplet, ArrayList<Planet> planets, Sun sun) {
    this.pApplet = pApplet;
    this.planets = planets;
    this.sun = sun; // Inicializa a instância do Sol
    this.timeScale = 1.0f; // Inicializa o tempo da simulação
    this.gravityFactor = G_AU * SOL_MASS / (365.25f * 365.25f);  // Fator de gravidade baseado na normalização
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
    PVector sunPos = sun.getPosition();

    // Distância Sol → planeta em AU
    PVector.sub(sunPos, p.position, tempVec1);
    float r_px = tempVec1.mag();
    float r_AU = r_px / PIXELS_PER_AU;

    // Aceleração gravitacional AU/dia²
    float aMag = gravityFactor * sun.getMass() / (r_AU * r_AU);
    tempVec2.set(tempVec1).normalize().mult(aMag); // aceleração inicial
    tempVec4.set(tempVec2);

    // Atualiza posição com velocidade e aceleração
    PVector velocityDt = PVector.mult(p.velocity, dt);
    tempVec2.mult(0.5f * dt * dt * PIXELS_PER_AU); // aceleração → deslocamento em px
    tempVec3.set(velocityDt).add(tempVec2);
    p.position.add(tempVec3);

    // Recalcula aceleração após movimento
    PVector.sub(sunPos, p.position, tempVec1);
    float r_AU_new = tempVec1.mag() / PIXELS_PER_AU;
    float aMag_new = gravityFactor * sun.getMass() / (r_AU_new * r_AU_new);
    tempVec1.normalize().mult(aMag_new); // nova aceleração

    // Atualiza velocidade com aceleração média
    tempVec4.add(tempVec1).mult(0.5f * dt * PIXELS_PER_AU); // agora em pixels/dia
    p.velocity.add(tempVec4);
    p.acceleration.set(tempVec1); // última aceleração
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
