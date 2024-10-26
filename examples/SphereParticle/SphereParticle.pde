import com.victorvalentim.zividomelive.*;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;

// Main instances
zividomelive ziviDome;  // Instance of the zividomelive library
Scene currentScene;     // Current scene implementing the Scene interface

ExecutorService particleProcessors;  // Gerenciador de threads
ReentrantLock lock = new ReentrantLock();

void settings() {
  size(1280, 720, P3D);  // Set the window size and P3D mode
}

void setup() {
  ziviDome = new zividomelive(this);
  ziviDome.setup();

  // Initialize the scene and set it in the library
  currentScene = new Scene1(ziviDome);
  ziviDome.setScene(currentScene);

  // Inicializa ExecutorService
  int numThreads = Runtime.getRuntime().availableProcessors();
  println("Usando " + numThreads + " threads para processamento de partículas.");
  particleProcessors = Executors.newFixedThreadPool(numThreads);
}

void draw() {
  ziviDome.draw();
}

// Eventos e forwarding de eventos para o ziviDome
void keyPressed() {
  ziviDome.keyPressed();
  if (currentScene != null) currentScene.keyPressed(key);
}

void mouseEvent(processing.event.MouseEvent event) {
  ziviDome.mouseEvent(event);
  if (currentScene != null) currentScene.mouseEvent(event);
}

void controlEvent(controlP5.ControlEvent theEvent) {
  ziviDome.controlEvent(theEvent);
}

// Implementação da cena otimizando o cálculo e renderização de partículas
class Scene1 implements Scene {
  zividomelive parent;
  PGraphics pg;
  ArrayList<Float> mass = new ArrayList<>();
  ArrayList<Float> positionX = new ArrayList<>();
  ArrayList<Float> positionY = new ArrayList<>();
  ArrayList<Float> positionZ = new ArrayList<>();
  ArrayList<Float> velocityX = new ArrayList<>();
  ArrayList<Float> velocityY = new ArrayList<>();
  ArrayList<Float> velocityZ = new ArrayList<>();
  ArrayList<Long> birthTime = new ArrayList<>();

  Scene1(zividomelive parent) {
    this.parent = parent;
  }

  public void setupScene() {
    noStroke();
    fill(64, 255, 255, 192);
  }

  public void sceneRender(PGraphics pg) {
    pg.beginDraw();
    pg.background(22);
    pg.noStroke();
    pg.fill(64, 255, 255, 192);

    pg.ambientLight(64, 64, 64);
    pg.pointLight(255, 255, 255, 0, 0, 0);

    pg.translate(0, 0, 250);
    pg.rotateX(-PI / 2 * (frameCount * 0.01));

    // Inicia tarefas para cada chunk
    int chunkSize = mass.size() / Runtime.getRuntime().availableProcessors();
    for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
      int start = i * chunkSize;
      int end = (i == Runtime.getRuntime().availableProcessors() - 1) ? mass.size() : (i + 1) * chunkSize;
      particleProcessors.submit(new ParticleProcessor(start, end));
    }

    // Renderiza partículas com posições atualizadas
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
        pg.popMatrix();
      }
    } finally {
      lock.unlock();
    }

    pg.endDraw();
  }

  public void keyPressed(char key) {}

  public void mouseEvent(MouseEvent event) {
    if (event.getAction() == MouseEvent.PRESS || event.getAction() == MouseEvent.DRAG) {
      addNewParticle((event.getX()) * 0.1, (event.getY()) * 0.1);
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

  // Processador de partículas com cálculos consistentes
  class ParticleProcessor implements Runnable {
    int start, end;

    ParticleProcessor(int start, int end) {
      this.start = start;
      this.end = end;
    }

    public void run() {
      long currentTime = millis();

      lock.lock();
      try {
        for (int particleA = start; particleA < end; particleA++) {
          if (particleA >= mass.size()) break;
          if (currentTime - birthTime.get(particleA) > 10000) {
            mass.remove(particleA);
            positionX.remove(particleA);
            positionY.remove(particleA);
            positionZ.remove(particleA);
            velocityX.remove(particleA);
            velocityY.remove(particleA);
            velocityZ.remove(particleA);
            birthTime.remove(particleA);
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
  }
}
