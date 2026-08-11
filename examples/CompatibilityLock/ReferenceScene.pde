class ReferenceScene implements Scene {
  private final zividomelive dome;

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

  public void keyEvent(KeyEvent event) {
    if (event.getAction() != KeyEvent.PRESS) {
      return;
    }

    char pressed = Character.toLowerCase(event.getKey());
    switch (pressed) {
      case '1': dome.setCurrentView(zividomelive.ViewType.FISHEYE_DOMEMASTER); break;
      case '2': dome.setCurrentView(zividomelive.ViewType.EQUIRECTANGULAR); break;
      case '3': dome.setCurrentView(zividomelive.ViewType.CUBEMAP); break;
      case '4': dome.setCurrentView(zividomelive.ViewType.STANDARD); break;
      case '[': dome.setFishSize(max(0f, dome.getFishSize() - 10f)); break;
      case ']': dome.setFishSize(min(100f, dome.getFishSize() + 10f)); break;
      case '-': dome.setFov(max(0f, dome.getFov() - 10f)); break;
      case '=':
      case '+': dome.setFov(min(360f, dome.getFov() + 10f)); break;
      case 'p': dome.setPitch(dome.getPitch() + HALF_PI); break;
      case 'y': dome.setYaw(dome.getYaw() + HALF_PI); break;
      case 'r': dome.setRoll(dome.getRoll() + HALF_PI); break;
      case 'f': dome.setShowPreview(!dome.isShowPreview()); break;
      case '0': resetQualificationState(); break;
      default: return;
    }

    printQualificationState();
  }

  private void printQualificationState() {
    println(
        "[CompatibilityLock] view=" + dome.getCurrentView()
        + " fov=" + nf(dome.getFov(), 0, 1)
        + " size=" + nf(dome.getFishSize(), 0, 1)
        + " pitch=" + nf(degrees(dome.getPitch()), 0, 1)
        + " yaw=" + nf(degrees(dome.getYaw()), 0, 1)
        + " roll=" + nf(degrees(dome.getRoll()), 0, 1)
        + " floatingPreview=" + dome.isShowPreview());
  }

  private void drawAxes(PGraphicsOpenGL pg) {
    pg.stroke(240, 60, 60);
    pg.line(-700, 0, 0, 700, 0, 0);
    pg.stroke(60, 220, 80);
    pg.line(0, -700, 0, 0, 700, 0);
    pg.stroke(70, 130, 255);
    pg.line(0, 0, -700, 0, 0, 700);
  }

  private void drawFaceMarker(PGraphicsOpenGL pg, String label, float x, float y, float z, int c, float size) {
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
    return "Compatibility Lock Reference";
  }
}
