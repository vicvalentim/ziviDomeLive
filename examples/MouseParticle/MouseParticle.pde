import com.victorvalentim.zividomelive.*;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;

// Main instances
zividomelive ziviDome;  // Instance of the zividomelive library
Scene currentScene;     // Current scene implementing the Scene interface

void settings() {
  size(1280, 720, P3D);  // Set the window size and P3D mode
}

void setup() {
  // Initialize the zividomelive library
  ziviDome = new zividomelive(this);
  ziviDome.setup();  // Initial setup of the library

  // Initialize the scene and set it in the library
  currentScene = new Scene1(ziviDome);
  ziviDome.setScene(currentScene);
}

void draw() {
  // Call the draw method of the library for additional rendering
  ziviDome.draw();
}

// Forward key events to the library
void keyPressed() {
  ziviDome.keyPressed();
  // Optionally call keyPressed of the current scene
  if (currentScene != null) {
    currentScene.keyPressed(key);
  }
}

// Forward mouse events to the library
void mouseEvent(processing.event.MouseEvent event) {
  ziviDome.mouseEvent(event);
  // Optionally call mouseEvent of the current scene
  if (currentScene != null) {
    currentScene.mouseEvent(event);
  }
}

// Forward control events to the library
void controlEvent(controlP5.ControlEvent theEvent) {
  ziviDome.controlEvent(theEvent);
}

// Implementation of Scene1 that uses the Scene interface
class Scene1 implements Scene {
  zividomelive parent;
  PGraphics pg;
  ArrayList<Float> mass;
  ArrayList<Float> positionX;
  ArrayList<Float> positionY;
  ArrayList<Float> positionZ;
  ArrayList<Float> velocityX;
  ArrayList<Float> velocityY;
  ArrayList<Float> velocityZ;
  ArrayList<Long> birthTime;

  Scene1(zividomelive parent) {
    this.parent = parent;
    mass = new ArrayList<Float>();
    positionX = new ArrayList<Float>();
    positionY = new ArrayList<Float>();
    positionZ = new ArrayList<Float>();
    velocityX = new ArrayList<Float>();
    velocityY = new ArrayList<Float>();
    velocityZ = new ArrayList<Float>();
    birthTime = new ArrayList<Long>();
  }

  @Override
  public void setupScene() {
  noStroke();
  fill(64, 255, 255, 192);  // Set fill color for particles
  }

  @Override
  public void sceneRender(PGraphics pg) {
    pg.beginDraw();
    pg.background(32);
    pg.noStroke();
    pg.fill(64, 255, 255, 192);  // Ensure fill color is applied in PGraphics

    // Ambient light and spotlight
    //pg.ambientLight(64, 64, 64);
    //pg.pointLight(255, 255, 255, 0, 0, 0);

    pg.translate(0,0,250);
    pg.rotateX(-PI/2 * (frameCount*0.01));

    long currentTime = millis();
    for (int particleA = mass.size() - 1; particleA >= 0; particleA--) {
      if (currentTime - birthTime.get(particleA) > 10000) {
        // Remove particle if it has existed for more than 10 seconds
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

      float accelerationX = 0;
      float accelerationY = 0;
      float accelerationZ = 0;

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
    }

    for (int particle = 0; particle < mass.size(); particle++) {
      positionX.set(particle, positionX.get(particle) + velocityX.get(particle));
      positionY.set(particle, positionY.get(particle) + velocityY.get(particle));
      positionZ.set(particle, positionZ.get(particle) + velocityZ.get(particle));



      pg.pushMatrix();
      pg.translate(positionX.get(particle), positionY.get(particle), positionZ.get(particle));
      pg.ellipse(0, 0, mass.get(particle) * 1000, mass.get(particle) * 1000);
      pg.popMatrix();
    }

    pg.endDraw();
  }

  @Override
  public void keyPressed(char key) {
    // Key response logic
  }

  @Override
  public void mouseEvent(MouseEvent event) {
    if (event.getAction() == MouseEvent.PRESS || event.getAction() == MouseEvent.DRAG) {
      addNewParticle((event.getX())*0.1, (event.getY())*0.1);
    }
  }

  void addNewParticle(float x, float y) {
    if (mass.size() >= 300) {
      return;
    }
    mass.add(random(0.003f, 0.03f));
    positionX.add(x);
    positionY.add(y);
    positionZ.add(random(-200, 200));
    velocityX.add(0f);
    velocityY.add(0f);
    velocityZ.add(0f);
    birthTime.add(Long.valueOf(millis()));
  }
}