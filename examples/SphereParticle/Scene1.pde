class ParticleFieldScene implements Scene {
  private final zividomelive dome;
  private final ArrayList<Particle> particles = new ArrayList<Particle>();
  private final int maximumParticles = 240;
  private float fieldRotation = 0f;
  private int lastUpdateMillis;
  private int lastBurstMillis;

  ParticleFieldScene(zividomelive dome) {
    this.dome = dome;
  }

  public void setupScene() {
    if (particles.isEmpty()) {
      resetField();
    }
    lastUpdateMillis = dome.getPApplet().millis();
  }

  public void update() {
    int now = dome.getPApplet().millis();
    float deltaSeconds = min((now - lastUpdateMillis) / 1000f, 0.05f);
    lastUpdateMillis = now;
    if (deltaSeconds <= 0f) {
      return;
    }

    fieldRotation += deltaSeconds * 0.12f;
    for (int i = particles.size() - 1; i >= 0; i--) {
      Particle particle = particles.get(i);
      if (now - particle.birthMillis > particle.lifetimeMillis) {
        particles.remove(i);
      } else {
        particle.update(deltaSeconds, now);
      }
    }
    while (particles.size() < 72) {
      addAmbientParticle(now);
    }
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(4, 8, 18);
    pg.ambientLight(48, 52, 70);
    pg.pointLight(255, 245, 220, 0, -160, 120);
    pg.directionalLight(90, 150, 255, 0.4f, 0.2f, -1f);
    pg.noStroke();
    pg.sphereDetail(10);

    pg.pushMatrix();
    pg.rotateY(fieldRotation);

    pg.pushMatrix();
    pg.fill(255, 205, 100);
    pg.emissive(45, 28, 8);
    pg.sphere(34);
    pg.popMatrix();
    pg.emissive(0);

    for (Particle particle : particles) {
      pg.pushMatrix();
      pg.translate(particle.position.x, particle.position.y, particle.position.z);
      pg.fill(particle.displayColor);
      pg.specular(150);
      pg.shininess(12);
      pg.sphere(particle.radius);
      pg.popMatrix();
    }

    pg.popMatrix();
  }

  public void keyEvent(KeyEvent event) {
    if (event.getAction() != KeyEvent.PRESS) {
      return;
    }

    switch (event.getKey()) {
      case 'c':
      case 'C': particles.clear(); break;
      case 'r':
      case 'R': resetField(); break;
      case ' ': addBurst(0f, 0f, 0f, 18); break;
    }
  }

  public void mouseEvent(MouseEvent event) {
    if (event.getAction() != MouseEvent.PRESS && event.getAction() != MouseEvent.DRAG) {
      return;
    }

    int now = dome.getPApplet().millis();
    if (event.getAction() == MouseEvent.DRAG && now - lastBurstMillis < 60) {
      return;
    }

    float worldX = map(event.getX(), 0, width, -520f, 520f);
    float worldY = map(event.getY(), 0, height, -320f, 320f);
    addBurst(worldX, worldY, random(-180f, 180f), 8);
    lastBurstMillis = now;
  }

  public String getName() {
    return "Particle Field";
  }

  private void resetField() {
    particles.clear();
    fieldRotation = 0f;
    int now = dome.getPApplet().millis();

    for (int i = 0; i < 90; i++) {
      addAmbientParticle(now);
    }
  }

  private void addAmbientParticle(int now) {
    float angle = random(TWO_PI);
    float ringRadius = random(240f, 680f);
    PVector position = new PVector(
        cos(angle) * ringRadius,
        random(-260f, 260f),
        sin(angle) * ringRadius);
    PVector velocity = new PVector(-position.z, random(-80f, 80f), position.x);
    velocity.normalize().mult(random(35f, 95f));
    addParticle(position, velocity, now);
  }

  private void addBurst(float x, float y, float z, int count) {
    int now = dome.getPApplet().millis();
    for (int i = 0; i < count && particles.size() < maximumParticles; i++) {
      PVector position = new PVector(
          x + random(-35f, 35f),
          y + random(-35f, 35f),
          z + random(-35f, 35f));
      PVector velocity = PVector.random3D().mult(random(80f, 220f));
      addParticle(position, velocity, now);
    }
  }

  private void addParticle(PVector position, PVector velocity, int now) {
    if (particles.size() >= maximumParticles) {
      particles.remove(0);
    }
    particles.add(new Particle(position, velocity, now));
  }

  class Particle {
    final PVector position;
    final PVector velocity;
    final float radius;
    final float wavePhase;
    final int displayColor;
    final int birthMillis;
    final int lifetimeMillis;

    Particle(PVector position, PVector velocity, int birthMillis) {
      this.position = position;
      this.velocity = velocity;
      this.birthMillis = birthMillis;
      this.lifetimeMillis = int(random(9000f, 16000f));
      this.radius = random(8f, 24f);
      this.wavePhase = random(TWO_PI);
      this.displayColor = lerpColor(
          color(60, 205, 225),
          color(232, 105, 190),
          random(1f));
    }

    void update(float deltaSeconds, int now) {
      PVector acceleration = PVector.mult(position, -0.32f);
      PVector tangent = new PVector(-position.z, 0f, position.x);
      if (tangent.magSq() > 0.001f) {
        tangent.normalize().mult(22f);
        acceleration.add(tangent);
      }
      acceleration.y += sin(now * 0.0015f + wavePhase) * 24f;

      velocity.add(PVector.mult(acceleration, deltaSeconds));
      velocity.mult(pow(0.988f, deltaSeconds * 60f));
      position.add(PVector.mult(velocity, deltaSeconds));
    }
  }
}
