import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class PhysicsEngine {
  private final PApplet pApplet;
  private final ArrayList<Planet> planets;
  private final Sun sun;  // Agora o Sol é um objeto separado
  private float timeScale;
  private final float gravityFactor;

  private final int availableThreads;

  // Vetores temporários (thread-local não é necessário com paralelismo simples por tarefa)
  private final ThreadLocal<PVector> tempVec1 = ThreadLocal.withInitial(PVector::new);
  private final ThreadLocal<PVector> tempVec2 = ThreadLocal.withInitial(PVector::new);
  private final ThreadLocal<PVector> tempVec3 = ThreadLocal.withInitial(PVector::new);
  private final ThreadLocal<PVector> tempVec4 = ThreadLocal.withInitial(PVector::new);

  PhysicsEngine(PApplet pApplet, ArrayList<Planet> planets, Sun sun) {
    this.pApplet = pApplet;
    this.planets = planets;
    this.sun = sun;
    this.timeScale = 1.0f;
    this.gravityFactor = G_AU * SOL_MASS / (365.25f * 365.25f);
    this.availableThreads = Runtime.getRuntime().availableProcessors();
  }

  public void update(float dt) {
    if (planets == null || sun == null || planets.size() == 0) return;
    updatePlanetsParallel(planets, dt);
  }

  private void updatePlanetsParallel(ArrayList<Planet> planets, float dt) {
    ExecutorService executor = Executors.newFixedThreadPool(availableThreads);
    for (Planet p : planets) {
      executor.submit(() -> updatePlanetPhysics(p, dt, sun));
    }

    // Espera todos os planetas terminarem sua atualização
    executor.shutdown();
    try {
      executor.awaitTermination(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      System.err.println("[PhysicsEngine] Erro ao aguardar tarefas físicas: " + e.getMessage());
    }
  }

  private void updatePlanetPhysics(Planet p, float dt, Sun sun) {
    final PVector temp1 = tempVec1.get();
    final PVector temp2 = tempVec2.get();
    final PVector temp3 = tempVec3.get();
    final PVector temp4 = tempVec4.get();

    PVector sunPos = sun.getPosition();

    PVector.sub(sunPos, p.position, temp1);
    float r_px = temp1.mag();
    float r_AU = r_px / PIXELS_PER_AU;
    float aMag = gravityFactor * sun.getMass() / (r_AU * r_AU);
    temp2.set(temp1).normalize().mult(aMag);
    temp4.set(temp2);

    PVector velocityDt = PVector.mult(p.velocity, dt);
    temp2.mult(0.5f * dt * dt * PIXELS_PER_AU);
    temp3.set(velocityDt).add(temp2);

    // Alteração aqui com a atualização do cache:
    p.position.add(temp3);
    p.drawPositionDirty = true;  // ← Integração do cache

    PVector.sub(sunPos, p.position, temp1);
    float r_AU_new = temp1.mag() / PIXELS_PER_AU;
    float aMag_new = gravityFactor * sun.getMass() / (r_AU_new * r_AU_new);
    temp1.normalize().mult(aMag_new);

    temp4.add(temp1).mult(0.5f * dt * PIXELS_PER_AU);
    p.velocity.add(temp4);
    p.acceleration.set(temp1);

    // Atualizações adicionais sincronizadas (rotação e luas)
    synchronized (p) {
      p.updateRotation(dt);
      float sunRadius = SUN_VISUAL_RADIUS * p.simParams.globalScale;
        PVector drawPos = p.getDrawPosition(sunRadius); // já calcula aqui
        p.updateMoons(dt, drawPos, p.velocity);         // passa como argumento
    }
  }

  public void setTimeScale(float ts) {
    this.timeScale = ts;
  }

  public float getTimeScale() {
    return timeScale;
  }

  public void dispose() {
    // Nenhum recurso explícito para limpar
  }
}
