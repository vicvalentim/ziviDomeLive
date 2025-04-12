import processing.core.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

class PhysicsEngine {
  private final PApplet pApplet;
  private final ArrayList<Planet> planets;
  private final Sun sun;
  private float timeScale;
  private final float gravityFactor;
  private final int availableThreads;

  private final ExecutorService executor;

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

    executor = Executors.newFixedThreadPool(availableThreads);
  }

  public void update(float dt) {
    if (planets == null || sun == null || planets.isEmpty()) return;

    List<Callable<Void>> tasks = new ArrayList<>();
    for (Planet p : planets) {
      tasks.add(() -> {
        updatePlanetPhysics(p, dt, sun);
        return null;
      });
    }

    try {
      executor.invokeAll(tasks);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.err.println("[PhysicsEngine] Atualização interrompida: " + e.getMessage());
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

    p.position.add(temp3);
    p.drawPositionDirty = true;

    PVector.sub(sunPos, p.position, temp1);
    float r_AU_new = temp1.mag() / PIXELS_PER_AU;
    float aMag_new = gravityFactor * sun.getMass() / (r_AU_new * r_AU_new);
    temp1.normalize().mult(aMag_new);

    temp4.add(temp1).mult(0.5f * dt * PIXELS_PER_AU);
    p.velocity.add(temp4);
    p.acceleration.set(temp1);

    synchronized (p) {
      p.updateRotation(dt);
      float sunRadius = SUN_VISUAL_RADIUS * p.simParams.globalScale;
      PVector drawPos = p.getDrawPosition(sunRadius);
      p.updateMoons(dt, drawPos, p.velocity);
    }
  }

  public void setTimeScale(float ts) {
    this.timeScale = ts;
  }

  public float getTimeScale() {
    return timeScale;
  }

  public void dispose() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
      System.err.println("[PhysicsEngine] Executor interrompido durante shutdown.");
    }
  }
}
