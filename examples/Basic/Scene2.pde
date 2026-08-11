// Scene2 implementation (Example for additional scene)
class Scene2 implements Scene {
  private ziviDomeLive parent;

  Scene2(ziviDomeLive parent) {
      this.parent = parent;
  }

  public void setupScene() {
      println("Scene2 setup completed.");
  }

  public void update() {
      // Optional update logic
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    pg.pushMatrix();
    pg.background(25, 25, 112);
    pg.pushMatrix();
    pg.fill(255);
    drawLabeledBox(pg, 200); // Draw the cube with labels and mesh
    pg.popMatrix();
    pg.popMatrix();
  }

  public void keyEvent(processing.event.KeyEvent event) {
      println("Key pressed in Scene2.");
  }

  public void mouseEvent(MouseEvent event) {
      println("Mouse event in Scene2.");
  }

  public void controlEvent(controlP5.ControlEvent theEvent) {
      println("Control event in Scene2: " + theEvent.getName());
  }

  public String getName() {
      return "Scene2";
  }

  void drawLabeledBox(PGraphics pg, float size) {
    pg.pushMatrix();
    // Front (+Z)
    pg.pushMatrix();
    pg.translate(0, 0, size / 2);
    drawFaceWithMesh(pg, size, "+Z Front", pg.color(255, 0, 0)); // Red (Primary) // Purple // Vibrant blue
    pg.popMatrix();
    // Back (-Z)
    pg.pushMatrix();
    pg.translate(0, 0, -size / 2);
    pg.rotateY(PI); // Rotate so the text faces outward
    drawFaceWithMesh(pg, size, "-Z Back", pg.color(0, 255, 0)); // Green (Secondary) // Orange // Vibrant green
    pg.popMatrix();
    // Right (+X)
    pg.pushMatrix();
    pg.translate(size / 2, 0, 0);
    pg.rotateY(-HALF_PI); // Rotate so the text faces outward
    drawFaceWithMesh(pg, size, "+X Right", pg.color(0, 0, 255)); // Blue (Primary) // Teal // Vibrant red
    pg.popMatrix();
    // Left (-X)
    pg.pushMatrix();
    pg.translate(-size / 2, 0, 0);
    pg.rotateY(HALF_PI); // Rotate so the text faces outward
    drawFaceWithMesh(pg, size, "-X Left", pg.color(255, 255, 0)); // Yellow (Secondary) // Olive // Vibrant yellow
    pg.popMatrix();
    // Top (+Y)
    pg.pushMatrix();
    pg.translate(0, -size / 2, 0);
    pg.rotateX(-HALF_PI); // Rotate so the text faces outward
    drawFaceWithMesh(pg, size, "+Y Top", pg.color(255, 0, 255)); // Magenta (Secondary) // Indigo // Vibrant magenta
    pg.popMatrix();
    // Bottom (-Y)
    pg.pushMatrix();
    pg.translate(0, size / 2, 0);
    pg.rotateX(HALF_PI); // Rotate so the text faces outward
    drawFaceWithMesh(pg, size, "-Y Bottom", pg.color(0, 255, 255)); // Cyan (Secondary) // Deep Pink // Vibrant cyan
    pg.popMatrix();
    pg.popMatrix();
  }

  void drawFaceWithMesh(PGraphics pg, float size, String label, int faceColor) {
    pg.fill(faceColor);
    pg.beginShape();
    pg.vertex(-size / 2, -size / 2, 0);
    pg.vertex(size / 2, -size / 2, 0);
    pg.vertex(size / 2, size / 2, 0);
    pg.vertex(-size / 2, size / 2, 0);
    pg.endShape(CLOSE);
    pg.stroke(0);
    pg.strokeWeight(1);
    float step = size / 10.0;
    for (float i = -size / 2; i <= size / 2; i += step) {
      pg.line(i, -size / 2, 0, i, size / 2, 0); // Vertical lines
      pg.line(-size / 2, i, 0, size / 2, i, 0); // Horizontal lines
    }
    pg.fill(0);
    pg.textAlign(CENTER, CENTER);
    pg.textSize(30); // Increase font size
    pg.text(label, 0, 0, 0);
  }
}
