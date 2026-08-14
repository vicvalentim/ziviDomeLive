// Implementação da cena otimizando o cálculo e renderização de partículas
class Scene1 implements Scene {
  SceneServices services;
  PGraphics pg;
  ArrayList<Float> mass = new ArrayList<>();
  ArrayList<Float> positionX = new ArrayList<>();
  ArrayList<Float> positionY = new ArrayList<>();
  ArrayList<Float> positionZ = new ArrayList<>();
  ArrayList<Float> velocityX = new ArrayList<>();
  ArrayList<Float> velocityY = new ArrayList<>();
  ArrayList<Float> velocityZ = new ArrayList<>();
  ArrayList<Long> birthTime = new ArrayList<>();

  public void configure(SceneServices services) {
    this.services = services;
  }

  public void setupScene() {
    noStroke();
    fill(64, 255, 255, 192);
  }

  public void update() {
    // A API rejeita outra tarefa com a mesma chave enquanto esta estiver em voo.
    services.tasks().submitIfIdle("particle-simulation", this::updateParticles);
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(22);
    pg.noStroke();
    pg.fill(64, 255, 255, 192);

    pg.ambientLight(64, 64, 64);
    pg.pointLight(255, 255, 255, 0, 0, 0);

    pg.translate(0, 0, 250);
    pg.rotateX(-PI / 2 * (frameCount * 0.01));

    // Renderiza um snapshot protegido das posições atualizadas.
    lock.lock();
    try {
      for (int particle = 0; particle < mass.size(); particle++) {
        pg.pushMatrix();
        pg.translate(positionX.get(particle), positionY.get(particle), positionZ.get(particle));
        pg.pushStyle();
        pg.specular(160);
        pg.shininess(10);
        pg.sphereDetail(15);
        pg.sphere(mass.get(particle) * 500);
        pg.popStyle();
        pg.popMatrix();
      }
    } finally {
      lock.unlock();
    }
  }

  public void keyEvent(processing.event.KeyEvent event) {
      if (event.getAction() == processing.event.KeyEvent.PRESS) { // Only handle key press events
          char key = event.getKey();
          println("Key pressed in Scene1: " + key);
          
      }
  }

  public void mouseEvent(MouseEvent event) {
    if (event.getAction() == MouseEvent.PRESS || event.getAction() == MouseEvent.DRAG) {
      addNewParticle((event.getX()) * 0.1, (event.getY()) * 0.1);
    }
  }
  
  public void controlEvent(controlP5.ControlEvent theEvent) {
      println("Control event in Scene1: " + theEvent.getName());
  }

  public String getName() {
      return "Scene1";
  }

  public void dispose() {
    // SceneServices cancela tarefas antes deste cleanup de domínio.
    lock.lock();
    try {
      mass.clear();
      positionX.clear();
      positionY.clear();
      positionZ.clear();
      velocityX.clear();
      velocityY.clear();
      velocityZ.clear();
      birthTime.clear();
    } finally {
      lock.unlock();
    }
  }
  
  void addNewParticle(float x, float y) {
    lock.lock();
    try {
      mass.add(random(0.003f, 0.03f));
      positionX.add(x);
      positionY.add(y);
      positionZ.add(random(-200, 200));
      velocityX.add(0f);
      velocityY.add(0f);
      velocityZ.add(0f);
      birthTime.add(Long.valueOf(millis()));
    } finally {
      lock.unlock();
    }
  }

  // Executa uma atualização completa no worker compartilhado.
  void updateParticles() {
    long currentTime = millis();

    lock.lock();
    try {
      for (int particleA = mass.size() - 1; particleA >= 0; particleA--) {
        if (Thread.currentThread().isInterrupted()) return;
        if (currentTime - birthTime.get(particleA) > 10000) {
          removeParticle(particleA);
          continue;
        }

        float accelerationX = 0, accelerationY = 0, accelerationZ = 0;

        for (int particleB = 0; particleB < mass.size(); particleB++) {
          if (particleA != particleB) {
            float distanceX = positionX.get(particleB) - positionX.get(particleA);
            float distanceY = positionY.get(particleB) - positionY.get(particleA);
            float distanceZ = positionZ.get(particleB) - positionZ.get(particleA);

            float distance = PApplet.sqrt(distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ);
            if (distance < 1) distance = 1;

            float force = (distance - 320) * mass.get(particleB) / distance;
            accelerationX += force * distanceX;
            accelerationY += force * distanceY;
            accelerationZ += force * distanceZ;
          }
        }

        velocityX.set(particleA, velocityX.get(particleA) * 0.99f + accelerationX * mass.get(particleA));
        velocityY.set(particleA, velocityY.get(particleA) * 0.99f + accelerationY * mass.get(particleA));
        velocityZ.set(particleA, velocityZ.get(particleA) * 0.99f + accelerationZ * mass.get(particleA));

        positionX.set(particleA, positionX.get(particleA) + velocityX.get(particleA));
        positionY.set(particleA, positionY.get(particleA) + velocityY.get(particleA));
        positionZ.set(particleA, positionZ.get(particleA) + velocityZ.get(particleA));
      }
    } finally {
      lock.unlock();
    }
  }

  void removeParticle(int index) {
    mass.remove(index);
    positionX.remove(index);
    positionY.remove(index);
    positionZ.remove(index);
    velocityX.remove(index);
    velocityY.remove(index);
    velocityZ.remove(index);
    birthTime.remove(index);
  }
}
