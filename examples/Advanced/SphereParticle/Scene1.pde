class Scene1 implements Scene {
  private final ParticleState emptyParticles = new ParticleState(
    new float[0], new float[0], new float[0], new float[0],
    new float[0], new float[0], new float[0], new double[0]);
  private SceneServices services;
  private ParticleState particles = emptyParticles;
  private float rotation;

  public void configure(SceneServices services) {
    this.services = services;
  }

  public void setupScene() {
    services.applet().println(
      "[SphereParticle] worker CPU -> callback no frame boundary; sem locks no render.");
  }

  public void update() {
    rotation += 0.01f;
    ParticleState input = particles;
    if (input.size() == 0) {
      return;
    }

    double nowSeconds = services.frameClock().getElapsedSeconds();
    services.tasks().submitIfIdle(
      "particle-simulation",
      () -> simulate(input, nowSeconds),
      result -> {
        // Mouse input may have published a newer state while the worker was running.
        if (particles == input) {
          particles = result;
        }
      },
      error -> services.applet().println(
        "[SphereParticle] simulation failed: " + error.getMessage()));
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    ParticleState frame = particles;

    pg.background(22);
    pg.noStroke();
    pg.fill(64, 255, 255, 192);
    pg.ambientLight(64, 64, 64);
    pg.pointLight(255, 255, 255, 0, 0, 0);
    pg.translate(0, 0, 250);
    pg.rotateX(-PI * 0.5f * rotation);
    pg.specular(160);
    pg.shininess(10);
    pg.sphereDetail(15);

    for (int particle = 0; particle < frame.size(); particle++) {
      pg.pushMatrix();
      pg.translate(frame.x[particle], frame.y[particle], frame.z[particle]);
      pg.sphere(frame.mass[particle] * 500f);
      pg.popMatrix();
    }
  }

  public void keyEvent(processing.event.KeyEvent event) {
    if (event.getAction() == processing.event.KeyEvent.PRESS) {
      services.applet().println("[SphereParticle] key=" + event.getKey());
    }
  }

  public void mouseEvent(MouseEvent event) {
    if (event.getAction() == MouseEvent.PRESS || event.getAction() == MouseEvent.DRAG) {
      addNewParticle(event.getX() * 0.1f, event.getY() * 0.1f);
    }
  }

  public String getName() {
    return "Sphere Particles";
  }

  public void dispose() {
    particles = emptyParticles;
    services = null;
  }

  private void addNewParticle(float x, float y) {
    PApplet applet = services.applet();
    particles = particles.withAdded(
      applet.random(0.003f, 0.03f),
      x,
      y,
      applet.random(-200f, 200f),
      services.frameClock().getElapsedSeconds());
  }

  private ParticleState simulate(ParticleState input, double nowSeconds) {
    int aliveCount = 0;
    for (int index = 0; index < input.size(); index++) {
      if (nowSeconds - input.birthSeconds[index] <= 10.0) {
        aliveCount++;
      }
    }

    float[] mass = new float[aliveCount];
    float[] x = new float[aliveCount];
    float[] y = new float[aliveCount];
    float[] z = new float[aliveCount];
    float[] vx = new float[aliveCount];
    float[] vy = new float[aliveCount];
    float[] vz = new float[aliveCount];
    double[] birthSeconds = new double[aliveCount];

    int destination = 0;
    for (int source = 0; source < input.size(); source++) {
      if (nowSeconds - input.birthSeconds[source] > 10.0) {
        continue;
      }
      mass[destination] = input.mass[source];
      x[destination] = input.x[source];
      y[destination] = input.y[source];
      z[destination] = input.z[source];
      vx[destination] = input.vx[source];
      vy[destination] = input.vy[source];
      vz[destination] = input.vz[source];
      birthSeconds[destination] = input.birthSeconds[source];
      destination++;
    }

    for (int particleA = 0; particleA < aliveCount; particleA++) {
      if (Thread.currentThread().isInterrupted()) {
        throw new java.util.concurrent.CancellationException("activation stopped");
      }

      float accelerationX = 0f;
      float accelerationY = 0f;
      float accelerationZ = 0f;
      for (int particleB = 0; particleB < aliveCount; particleB++) {
        if (particleA == particleB) {
          continue;
        }
        float distanceX = x[particleB] - x[particleA];
        float distanceY = y[particleB] - y[particleA];
        float distanceZ = z[particleB] - z[particleA];
        float distance = (float) Math.sqrt(
          distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ);
        distance = Math.max(1f, distance);
        float force = (distance - 320f) * mass[particleB] / distance;
        accelerationX += force * distanceX;
        accelerationY += force * distanceY;
        accelerationZ += force * distanceZ;
      }

      vx[particleA] = vx[particleA] * 0.99f + accelerationX * mass[particleA];
      vy[particleA] = vy[particleA] * 0.99f + accelerationY * mass[particleA];
      vz[particleA] = vz[particleA] * 0.99f + accelerationZ * mass[particleA];
      x[particleA] += vx[particleA];
      y[particleA] += vy[particleA];
      z[particleA] += vz[particleA];
    }

    return new ParticleState(mass, x, y, z, vx, vy, vz, birthSeconds);
  }
}

final class ParticleState {
  final float[] mass;
  final float[] x;
  final float[] y;
  final float[] z;
  final float[] vx;
  final float[] vy;
  final float[] vz;
  final double[] birthSeconds;

  ParticleState(
      float[] mass,
      float[] x,
      float[] y,
      float[] z,
      float[] vx,
      float[] vy,
      float[] vz,
      double[] birthSeconds) {
    this.mass = mass;
    this.x = x;
    this.y = y;
    this.z = z;
    this.vx = vx;
    this.vy = vy;
    this.vz = vz;
    this.birthSeconds = birthSeconds;
  }

  int size() {
    return mass.length;
  }

  ParticleState withAdded(float newMass, float newX, float newY, float newZ, double bornAt) {
    int nextSize = size() + 1;
    float[] nextMass = java.util.Arrays.copyOf(mass, nextSize);
    float[] nextX = java.util.Arrays.copyOf(x, nextSize);
    float[] nextY = java.util.Arrays.copyOf(y, nextSize);
    float[] nextZ = java.util.Arrays.copyOf(z, nextSize);
    float[] nextVx = java.util.Arrays.copyOf(vx, nextSize);
    float[] nextVy = java.util.Arrays.copyOf(vy, nextSize);
    float[] nextVz = java.util.Arrays.copyOf(vz, nextSize);
    double[] nextBirthSeconds = java.util.Arrays.copyOf(birthSeconds, nextSize);
    int index = nextSize - 1;
    nextMass[index] = newMass;
    nextX[index] = newX;
    nextY[index] = newY;
    nextZ[index] = newZ;
    nextBirthSeconds[index] = bornAt;
    return new ParticleState(
      nextMass, nextX, nextY, nextZ, nextVx, nextVy, nextVz, nextBirthSeconds);
  }
}
