class ReferenceScene implements Scene {
  final zividomelive dome;

  ReferenceScene(zividomelive dome) {
    this.dome = dome;
  }

  @Override
  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(4, 6, 12);
    pg.lights();
    pg.pushMatrix();

    pg.strokeWeight(3);
    drawAxes(pg);
    drawFaceMarker(pg, "+X", 600, 0, 0, color(240, 60, 60), 70);
    drawFaceMarker(pg, "-X", -600, 0, 0, color(180, 30, 30), 45);
    drawFaceMarker(pg, "+Y", 0, 600, 0, color(60, 220, 80), 60);
    drawFaceMarker(pg, "-Y", 0, -600, 0, color(30, 140, 50), 40);
    drawFaceMarker(pg, "+Z", 0, 0, 600, color(70, 130, 255), 75);
    drawFaceMarker(pg, "-Z", 0, 0, -600, color(80, 70, 180), 50);

    pg.noFill();
    pg.stroke(255, 220, 80);
    pg.rotateX(0.23);
    pg.rotateY(0.41);
    pg.rotateZ(0.17);
    pg.box(360, 180, 90);

    pg.popMatrix();
  }

  void drawAxes(PGraphicsOpenGL pg) {
    pg.stroke(240, 60, 60);
    pg.line(-700, 0, 0, 700, 0, 0);
    pg.stroke(60, 220, 80);
    pg.line(0, -700, 0, 0, 700, 0);
    pg.stroke(70, 130, 255);
    pg.line(0, 0, -700, 0, 0, 700);
  }

  void drawFaceMarker(PGraphicsOpenGL pg, String label, float x, float y, float z, int c, float size) {
    pg.pushMatrix();
    pg.translate(x, y, z);
    pg.fill(c);
    pg.stroke(255);
    pg.sphere(size);

    pg.fill(255);
    pg.textAlign(CENTER, CENTER);
    pg.textSize(48);
    pg.text(label, 0, -size - 45, 0);
    pg.popMatrix();
  }

  @Override
  public String getName() {
    return "CompatibilityLockReferenceScene";
  }
}
