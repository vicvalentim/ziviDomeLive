class Scene1 implements Scene {
  private final zividomelive dome;
  private final int pillarCount = 8;
  private final int[] colors = {
      0xFFFF5252,
      0xFF55D878,
      0xFF4E8FF5,
      0xFFF4CD48,
      0xFFCB5CDC,
      0xFF43CDCD,
      0xFFF2F2F2,
      0xFFFF963F
  };
  private float angularSpeed = 0.6f;
  private float radius = 700f;
  private float phase = 0f;
  private int lastUpdateMillis;

  Scene1(zividomelive dome) {
    this.dome = dome;
  }

  public void setupScene() {
    lastUpdateMillis = dome.getPApplet().millis();
  }

  public void update() {
    int now = dome.getPApplet().millis();
    float deltaSeconds = min((now - lastUpdateMillis) / 1000f, 0.1f);
    phase += angularSpeed * max(0f, deltaSeconds);
    lastUpdateMillis = now;
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(5, 9, 24);
    pg.ambientLight(55, 55, 70);
    pg.directionalLight(255, 245, 220, -0.4f, 0.7f, -1f);
    pg.noStroke();

    float angleStep = TWO_PI / pillarCount;
    for (int i = 0; i < pillarCount; i++) {
      float angle = angleStep * i + phase;
      float x = cos(angle) * radius;
      float y = (i % 2 == 0) ? -120f : 120f;
      float z = sin(angle) * radius;

      pg.pushMatrix();
      pg.translate(x, y, z);
      pg.rotateX(phase * 0.7f + i * 0.18f);
      pg.rotateY(-angle);
      pg.fill(colors[i % colors.length]);
      pg.box(170, 320, 170);
      pg.popMatrix();
    }
  }

  public void keyEvent(processing.event.KeyEvent event) {
    if (event.getAction() != processing.event.KeyEvent.PRESS) {
      return;
    }

    switch (event.getKey()) {
      case '1': dome.setRenderMode(RenderMode.FULL); break;
      case '2': dome.setRenderMode(RenderMode.STANDARD); break;
      case '3': dome.setRenderMode(RenderMode.DOMEMASTER); break;
      case '4': dome.setRenderMode(RenderMode.EQUIRECTANGULAR); break;
      case '5': dome.setRenderMode(RenderMode.SKYBOX); break;
      case '+':
      case '=': angularSpeed = min(2.4f, angularSpeed + 0.15f); break;
      case '-': angularSpeed = max(0f, angularSpeed - 0.15f); break;
      case 'r':
      case 'R': angularSpeed = 0.6f; radius = 700f; break;
    }
  }

  public void mouseEvent(MouseEvent event) {
    if (event.getAction() == MouseEvent.WHEEL) {
      radius = constrain(radius + event.getCount() * 30f, 320f, 1200f);
    }
  }

  public String getName() {
    return "Orbiting Pillars";
  }
}
