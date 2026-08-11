class Scene2 implements Scene {
  private final zividomelive dome;
  private float spin = 0f;
  private int lastUpdateMillis;

  Scene2(zividomelive dome) {
    this.dome = dome;
  }

  public void setupScene() {
    lastUpdateMillis = dome.getPApplet().millis();
  }

  public void update() {
    int now = dome.getPApplet().millis();
    float deltaSeconds = min((now - lastUpdateMillis) / 1000f, 0.1f);
    spin += max(0f, deltaSeconds) * 0.35f;
    lastUpdateMillis = now;
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(7, 10, 28);
    pg.pushMatrix();
    pg.rotateX(-0.28f);
    pg.rotateY(spin);
    drawLabeledBox(pg, 420f);
    pg.popMatrix();
  }

  public void keyEvent(processing.event.KeyEvent event) {
    if (event.getAction() == processing.event.KeyEvent.PRESS
        && (event.getKey() == 'r' || event.getKey() == 'R')) {
      spin = 0f;
    }
  }

  public String getName() {
    return "Labeled Cube";
  }

  private void drawLabeledBox(PGraphicsOpenGL pg, float size) {
    pg.pushMatrix();

    pg.pushMatrix();
    pg.translate(0, 0, size / 2);
    drawFaceWithGrid(pg, size, "+Z Front", pg.color(236, 74, 74));
    pg.popMatrix();

    pg.pushMatrix();
    pg.translate(0, 0, -size / 2);
    pg.rotateY(PI);
    drawFaceWithGrid(pg, size, "-Z Back", pg.color(86, 194, 112));
    pg.popMatrix();

    pg.pushMatrix();
    pg.translate(size / 2, 0, 0);
    pg.rotateY(HALF_PI);
    drawFaceWithGrid(pg, size, "+X Right", pg.color(72, 139, 242));
    pg.popMatrix();

    pg.pushMatrix();
    pg.translate(-size / 2, 0, 0);
    pg.rotateY(-HALF_PI);
    drawFaceWithGrid(pg, size, "-X Left", pg.color(244, 205, 72));
    pg.popMatrix();

    pg.pushMatrix();
    pg.translate(0, -size / 2, 0);
    pg.rotateX(HALF_PI);
    drawFaceWithGrid(pg, size, "-Y Top", pg.color(203, 92, 220));
    pg.popMatrix();

    pg.pushMatrix();
    pg.translate(0, size / 2, 0);
    pg.rotateX(-HALF_PI);
    drawFaceWithGrid(pg, size, "+Y Bottom", pg.color(67, 205, 205));
    pg.popMatrix();

    pg.popMatrix();
  }

  private void drawFaceWithGrid(PGraphicsOpenGL pg, float size, String label, int faceColor) {
    pg.pushStyle();
    pg.noStroke();
    pg.fill(faceColor);
    pg.beginShape(QUADS);
    pg.vertex(-size / 2, -size / 2, 0);
    pg.vertex(size / 2, -size / 2, 0);
    pg.vertex(size / 2, size / 2, 0);
    pg.vertex(-size / 2, size / 2, 0);
    pg.endShape(CLOSE);

    pg.translate(0, 0, 0.8f);
    pg.stroke(15, 18, 30, 150);
    pg.strokeWeight(1);
    float step = size / 10f;
    for (float i = -size / 2; i <= size / 2; i += step) {
      pg.line(i, -size / 2, 0, i, size / 2, 0);
      pg.line(-size / 2, i, 0, size / 2, i, 0);
    }

    pg.noStroke();
    pg.fill(8, 10, 18);
    pg.textAlign(CENTER, CENTER);
    pg.textSize(size * 0.1f);
    pg.text(label, 0, 0, 1.2f);
    pg.popStyle();
  }
}
