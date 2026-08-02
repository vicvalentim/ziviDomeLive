// Fulldome example scene with primitive meshes and PBR-inspired materials.
// The library owns beginDraw()/endDraw(); this scene only draws content.
class Scene1 implements Scene {
  private final zividomelive parent;

  private float time = 0f;
  private float orbitRadius = 840f;
  private float orbitSpeed = 0.012f;
  private float ringTilt = 0.42f;
  private final int starCount = 220;
  private final PVector[] stars = new PVector[starCount];
  private final float[] starSizes = new float[starCount];
  private final int[] paletteA = { 244, 111, 94 };
  private final int[] paletteB = { 102, 210, 255 };
  private final int[] paletteC = { 218, 198, 255 };

  // --- Camera navigation state (drives the ziviDomeLive dome camera API) ---
  private boolean dragging = false;
  private int lastMouseX = 0;
  private int lastMouseY = 0;
  private final float lookSensitivity = 0.005f; // radians per pixel
  private final float rollSensitivity = 0.004f; // radians per pixel
  private final float fovMin = 30f;
  private final float fovMax = 220f;
  private final float defaultFov = 210f;

  Scene1(zividomelive parent) {
    this.parent = parent;
  }

  public void setupScene() {
    buildStarShell();
  }

  public void update() {
    time += orbitSpeed;
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(5, 7, 18);
    pg.noStroke();
    pg.sphereDetail(40);

    // Dome-friendly lighting stack: ambient + sun + rim + warm fill.
    pg.ambientLight(22, 22, 30);
    pg.directionalLight(140, 140, 160, -0.45f, -0.75f, -0.25f);
    pg.directionalLight(40, 70, 120, 0.35f, -0.2f, 0.9f);
    pg.pointLight(255, 205, 170, 0, -240, 280);
    pg.pointLight(90, 150, 255, -520, 180, -260);

    pg.pushMatrix();
    pg.rotateY(time * 0.11f);
    renderStarShell(pg);
    renderDomeGrid(pg);
    renderCentralCluster(pg);
    renderOrbitingModules(pg);
    renderSupportPillars(pg);
    pg.popMatrix();
  }

  public void keyEvent(processing.event.KeyEvent event) {
    if (event.getAction() != processing.event.KeyEvent.PRESS) {
      return;
    }

    switch (event.getKey()) {
      case '+':
      case '=':
        orbitSpeed = min(0.04f, orbitSpeed + 0.002f);
        break;
      case '-':
        orbitSpeed = max(0.002f, orbitSpeed - 0.002f);
        break;
      case '[':
        orbitRadius = constrain(orbitRadius - 40f, 520f, 1500f);
        break;
      case ']':
        orbitRadius = constrain(orbitRadius + 40f, 520f, 1500f);
        break;
      case 'v':
      case 'V':
        // Reset the dome camera through the library API.
        parent.setYaw(0f);
        parent.setPitch(0f);
        parent.setRoll(0f);
        parent.setFov(defaultFov);
        break;
      case 'r':
        orbitRadius = 840f;
        ringTilt = 0.42f;
        orbitSpeed = 0.012f;
        break;
    }
  }

  public void mouseEvent(MouseEvent event) {
    // In STANDARD view the library drives its own MouseControlledCamera,
    // so we only navigate the dome camera for the fulldome projections.
    boolean domeView = parent.getCurrentView() != zividomelive.ViewType.STANDARD;

    switch (event.getAction()) {
      case MouseEvent.PRESS:
        dragging = true;
        lastMouseX = event.getX();
        lastMouseY = event.getY();
        break;

      case MouseEvent.RELEASE:
        dragging = false;
        break;

      case MouseEvent.DRAG:
        if (domeView && dragging) {
          float dx = event.getX() - lastMouseX;
          float dy = event.getY() - lastMouseY;
          lastMouseX = event.getX();
          lastMouseY = event.getY();

          if (event.getButton() == RIGHT) {
            // Right-drag rolls the horizon.
            parent.setRoll(parent.getRoll() + dx * rollSensitivity);
          } else {
            // Left-drag looks around: horizontal = yaw, vertical = pitch.
            float yaw = parent.getYaw() + dx * lookSensitivity;
            float pitch = parent.getPitch() + dy * lookSensitivity;
            pitch = constrain(pitch, -HALF_PI, HALF_PI);
            parent.setYaw(yaw);
            parent.setPitch(pitch);
          }
        }
        break;

      case MouseEvent.WHEEL:
        if (domeView) {
          // Wheel zooms the dome via field of view.
          float fov = constrain(parent.getFov() + event.getCount() * 4f, fovMin, fovMax);
          parent.setFov(fov);
        } else {
          // Non-dome fallback: adjust module orbit radius.
          orbitRadius = constrain(orbitRadius + event.getCount() * 24f, 520f, 1500f);
        }
        break;

      default:
        break;
    }
  }

  public void controlEvent(controlP5.ControlEvent theEvent) {
    println("Control event in FulldomePBR: " + theEvent.getName());
  }

  public String getName() {
    return "FulldomePBR";
  }

  // -------------------------------------------------------------------------
  // Scene composition helpers
  // -------------------------------------------------------------------------

  private void renderStarShell(PGraphicsOpenGL pg) {
    pg.pushStyle();
    pg.stroke(220, 235, 255, 120);
    for (int i = 0; i < stars.length; i++) {
      PVector s = stars[i];
      pg.strokeWeight(starSizes[i]);
      pg.point(s.x, s.y, s.z);
    }
    pg.popStyle();
  }

  private void renderDomeGrid(PGraphicsOpenGL pg) {
    pg.pushStyle();
    pg.noFill();
    pg.stroke(120, 160, 255, 50);
    pg.strokeWeight(1.2f);

    float gridRadius = 1180f;
    for (int ring = 1; ring <= 4; ring++) {
      float t = ring / 4f;
      float y = lerp(-90f, 360f, t);
      pg.pushMatrix();
      pg.translate(0, y, 0);
      pg.rotateX(HALF_PI);
      pg.ellipse(0, 0, gridRadius * t * 2f, gridRadius * t * 2f);
      pg.popMatrix();
    }

    for (int i = 0; i < 16; i++) {
      float a = TWO_PI * i / 16f + time * 0.04f;
      pg.line(0, -100f, 0, cos(a) * gridRadius, 330f, sin(a) * gridRadius);
    }
    pg.popStyle();
  }

  private void renderCentralCluster(PGraphicsOpenGL pg) {
    pg.pushMatrix();
    pg.translate(0, 40f + sin(time * 1.9f) * 18f, 0);

    // Glowing core.
    pg.pushStyle();
    applyPbrMaterial(pg, 255, 180, 120, 0.9f, 0.08f, 0.35f);
    pg.pushMatrix();
    pg.rotateY(time * 0.7f);
    pg.sphere(170f + 14f * sin(time * 2.7f));
    pg.popMatrix();
    pg.popStyle();

    // Mid shell.
    pg.pushStyle();
    applyPbrMaterial(pg, paletteC[0], paletteC[1], paletteC[2], 0.15f, 0.55f, 0.0f);
    pg.pushMatrix();
    pg.rotateY(-time * 0.42f);
    pg.rotateZ(time * 0.16f);
    pg.box(150f, 150f, 150f);
    pg.popMatrix();
    pg.popStyle();

    // Small inner satellites.
    for (int i = 0; i < 6; i++) {
      float a = TWO_PI * i / 6f + time * 0.8f;
      float x = cos(a) * 260f;
      float y = sin(time * 2.1f + i) * 42f;
      float z = sin(a) * 260f;
      pg.pushMatrix();
      pg.translate(x, y, z);
      pg.rotateY(time * 0.9f + i * 0.2f);
      if (i % 2 == 0) {
        applyPbrMaterial(pg, paletteA[0], paletteA[1], paletteA[2], 0.75f, 0.18f, 0.08f);
        pg.sphere(42f);
      } else {
        applyPbrMaterial(pg, paletteB[0], paletteB[1], paletteB[2], 0.25f, 0.4f, 0.03f);
        pg.box(54f, 34f, 54f);
      }
      pg.popMatrix();
    }

    pg.popMatrix();
  }

  private void renderOrbitingModules(PGraphicsOpenGL pg) {
    for (int i = 0; i < 10; i++) {
      float a = TWO_PI * i / 10f + time * 0.36f;
      float b = TWO_PI * i / 10f * 0.5f + time * 0.24f;
      float x = cos(a) * orbitRadius;
      float y = sin(b) * 180f;
      float z = sin(a) * orbitRadius;

      pg.pushMatrix();
      pg.translate(x, y, z);
      pg.rotateY(a + time * 0.5f);
      pg.rotateX(ringTilt + sin(time + i) * 0.16f);

      if (i % 3 == 0) {
        pg.pushStyle();
        applyPbrMaterial(pg, 245, 245, 248, 0.95f, 0.12f, 0.0f);
        pg.box(82f, 82f, 82f);
        pg.popStyle();
      } else if (i % 3 == 1) {
        pg.pushStyle();
        applyPbrMaterial(pg, 96, 208, 255, 0.5f, 0.22f, 0.0f);
        drawCylinder(pg, 30f, 128f, 16);
        pg.popStyle();
      } else {
        pg.pushStyle();
        applyPbrMaterial(pg, 255, 165, 110, 0.2f, 0.65f, 0.1f);
        pg.sphere(58f);
        pg.popStyle();
      }

      // Highlight cap.
      pg.pushMatrix();
      pg.translate(0, -70f, 0);
      pg.pushStyle();
      applyPbrMaterial(pg, 255, 250, 225, 0.1f, 0.1f, 0.15f);
      pg.sphere(18f);
      pg.popStyle();
      pg.popMatrix();

      pg.popMatrix();
    }
  }

  private void renderSupportPillars(PGraphicsOpenGL pg) {
    float baseRadius = 560f;
    for (int i = 0; i < 8; i++) {
      float a = TWO_PI * i / 8f + time * 0.06f;
      float x = cos(a) * baseRadius;
      float z = sin(a) * baseRadius;

      pg.pushMatrix();
      pg.translate(x, 260f, z);
      pg.rotateY(-a + HALF_PI);
      pg.pushStyle();
      applyPbrMaterial(pg, 170, 185, 215, 0.85f, 0.35f, 0.0f);
      drawCylinder(pg, 24f, 340f, 12);
      pg.popStyle();
      pg.popMatrix();
    }
  }

  private void applyPbrMaterial(PGraphicsOpenGL pg, int r, int g, int b, float metallic, float roughness, float emissive) {
    float ambientScale = lerp(0.28f, 0.08f, metallic);
    float specStrength = lerp(80f, 255f, metallic);
    float shininess = lerp(10f, 120f, 1f - roughness);
    float emissiveScale = emissive;

    pg.noStroke();
    pg.ambient(r * ambientScale, g * ambientScale, b * ambientScale);
    pg.specular(specStrength, specStrength, specStrength);
    pg.shininess(shininess);
    if (emissiveScale > 0f) {
      pg.emissive(r * emissiveScale, g * emissiveScale, b * emissiveScale);
    }
    pg.fill(r, g, b);
  }

  private void drawCylinder(PGraphicsOpenGL pg, float radius, float height, int sides) {
    pg.pushStyle();
    float half = height * 0.5f;
    float step = TWO_PI / sides;

    pg.beginShape(QUAD_STRIP);
    for (int i = 0; i <= sides; i++) {
      float a = i * step;
      float nx = cos(a);
      float nz = sin(a);
      float x = nx * radius;
      float z = nz * radius;
      pg.normal(nx, 0, nz);
      pg.vertex(x, -half, z);
      pg.vertex(x, half, z);
    }
    pg.endShape();

    pg.beginShape(TRIANGLE_FAN);
    pg.normal(0, -1, 0);
    pg.vertex(0, -half, 0);
    for (int i = 0; i <= sides; i++) {
      float a = -i * step;
      pg.vertex(cos(a) * radius, -half, sin(a) * radius);
    }
    pg.endShape();

    pg.beginShape(TRIANGLE_FAN);
    pg.normal(0, 1, 0);
    pg.vertex(0, half, 0);
    for (int i = 0; i <= sides; i++) {
      float a = i * step;
      pg.vertex(cos(a) * radius, half, sin(a) * radius);
    }
    pg.endShape();
    pg.popStyle();
  }

  private void buildStarShell() {
    for (int i = 0; i < starCount; i++) {
      float u = random(-1f, 1f);
      float theta = random(TWO_PI);
      float s = sqrt(max(0f, 1f - u * u));
      float radius = random(2100f, 2600f);
      stars[i] = new PVector(cos(theta) * s * radius, u * radius, sin(theta) * s * radius);
      starSizes[i] = random(1.0f, 2.6f);
    }
  }
}
