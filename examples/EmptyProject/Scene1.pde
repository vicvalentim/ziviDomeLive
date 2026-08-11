class Scene1 implements Scene {
  private final zividomelive dome;
  private float rotation = 0f;
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
    rotation += max(0f, deltaSeconds) * 0.45f;
    lastUpdateMillis = now;
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(8, 12, 24);
    pg.ambientLight(70, 70, 80);
    pg.directionalLight(255, 245, 220, -0.4f, 0.6f, -1f);
    pg.noStroke();

    pg.pushMatrix();
    pg.rotateX(-0.35f);
    pg.rotateY(rotation);
    pg.fill(80, 190, 220);
    pg.box(260);
    pg.popMatrix();
  }

  public void keyEvent(KeyEvent event) {
    if (event.getAction() == KeyEvent.PRESS
        && (event.getKey() == 'r' || event.getKey() == 'R')) {
      rotation = 0f;
    }
  }

  public String getName() {
    return "Starter Scene";
  }
}
