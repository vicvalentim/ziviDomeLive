import com.victorvalentim.zividomelive.*;
import controlP5.*;
import java.util.function.Consumer;

ziviDomeLive ziviDome;

void settings() {
  size(1280, 720, P3D);
  pixelDensity(1);
}

void setup() {
  ziviDome = new ziviDomeLive(this);
  ziviDome.setup();
  ziviDome.setScene(new PortLoopbackScene());
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}

class PortLoopbackScene implements Scene {
  private SceneServices services;
  private SceneCameraService camera;
  private ManualIntegerInput input;
  private SceneOutputPort<String> output;
  private int level = 3;
  private float phase;

  public void configure(SceneServices services) {
    this.services = services;
    this.camera = services.camera();
  }

  public void setupScene() {
    camera.setDistanceLimits(-2500f, -320f);
    camera.setCollapseGuard(260f);
    camera.setDragSensitivity(0.01f);
    camera.setLerpFactor(0.18f);
    camera.orbit().setWheelSteps(-80f, -0.001f);
    resetCamera();
    camera.setInputEnabled(true);
    input = new ManualIntegerInput();
    services.ports().connectInput(input, this::applyLevel);
    output = services.ports().connectOutput(new ConsoleOutput(services.applet()));
    services.applet().println(
      "[PortLoopback] use + and - for messages; drag/zoom to navigate; R resets camera.");
  }

  public void update() {
    phase += 0.018f;
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(18, 12, 30);
    pg.pushMatrix();
    try {
      camera.apply(pg);
      pg.noLights();
      pg.ambientLight(34f, 28f, 48f);
      pg.directionalLight(210f, 225f, 255f, -0.35f, 0.55f, -0.75f);
      pg.pointLight(255f, 90f, 190f, 0f, -120f, 0f);
      drawSignalRing(pg);
    } finally {
      pg.popMatrix();
    }
  }

  public void keyEvent(KeyEvent event) {
    if (event.getAction() != KeyEvent.PRESS || input == null) {
      return;
    }
    if (event.getKey() == 'r' || event.getKey() == 'R') {
      resetCamera();
    } else if (event.getKey() == '+' || event.getKey() == '=') {
      input.publish(level + 1);
    } else if (event.getKey() == '-') {
      input.publish(level - 1);
    }
  }

  public String getName() {
    return "Port Loopback";
  }

  public void dispose() {
    // ScenePorts owns and closes both connected adapters.
    input = null;
    output = null;
    camera = null;
    services = null;
  }

  private void applyLevel(Integer requestedLevel) {
    level = constrain(requestedLevel, 1, 8);
    output.offer("level=" + level);
  }

  private void resetCamera() {
    camera.snapToAxisAngle(0f, 0f, 0f, 1f, 0f, 0f, -0.28f, -1150f);
  }

  private void drawSignalRing(PGraphicsOpenGL pg) {
    pg.pushMatrix();
    try {
      pg.rotateY(phase * 0.22f);
      pg.stroke(120, 95, 190, 150);
      pg.strokeWeight(5f);
      pg.noFill();
      pg.beginShape();
      for (int segment = 0; segment < 64; segment++) {
        float ringAngle = TWO_PI * segment / 64f;
        pg.vertex(cos(ringAngle) * 360f, 155f, sin(ringAngle) * 360f);
      }
      pg.endShape(CLOSE);

      for (int index = 0; index < 8; index++) {
        float angle = TWO_PI * index / 8f;
        float x = cos(angle) * 360f;
        float z = sin(angle) * 360f;
        boolean active = index < level;
        float height = active ? 170f + index * 18f : 58f;

        pg.pushMatrix();
        try {
          pg.translate(x, 155f - height * 0.5f, z);
          pg.rotateY(-angle);
          pg.noStroke();
          if (active) {
            pg.fill(85 + index * 18, 125 + index * 10, 255 - index * 12);
            pg.specular(235f);
            pg.shininess(36f);
          } else {
            pg.fill(48, 42, 70);
            pg.specular(80f);
            pg.shininess(8f);
          }
          pg.box(76f, height, 76f);
        } finally {
          pg.popMatrix();
        }
      }

      float pulseAngle = phase * 2.4f;
      pg.pushMatrix();
      try {
        pg.translate(cos(pulseAngle) * 360f, -75f, sin(pulseAngle) * 360f);
        pg.noStroke();
        pg.emissive(255f, 80f, 190f);
        pg.fill(255, 110, 210);
        pg.sphereDetail(18);
        pg.sphere(34f);
        pg.emissive(0f, 0f, 0f);
      } finally {
        pg.popMatrix();
      }

      pg.pushMatrix();
      try {
        pg.translate(0f, 55f, 0f);
        pg.noStroke();
        pg.fill(115 + level * 12, 70, 205 + level * 5);
        pg.specular(255f);
        pg.shininess(54f);
        pg.sphereDetail(24);
        pg.sphere(70f + level * 7f);
      } finally {
        pg.popMatrix();
      }
    } finally {
      pg.popMatrix();
    }
  }
}

class ManualIntegerInput implements SceneInputPort<Integer> {
  private Consumer<? super Integer> receiver;

  public void start(Consumer<? super Integer> receiver) {
    this.receiver = receiver;
  }

  void publish(int value) {
    Consumer<? super Integer> current = receiver;
    if (current != null) {
      current.accept(value);
    }
  }

  public void close() {
    receiver = null;
  }
}

class ConsoleOutput implements SceneOutputPort<String> {
  private PApplet applet;

  ConsoleOutput(PApplet applet) {
    this.applet = applet;
  }

  public boolean offer(String value) {
    if (applet == null) {
      return false;
    }
    applet.println("[PortLoopback] " + value);
    return true;
  }

  public void close() {
    applet = null;
  }
}
