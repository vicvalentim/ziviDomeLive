// Fulldome example scene with full PBR lighting via GLSL shaders and
// retained-mode primitives (PShape / VBO) built from PVector geometry.
//
// The library owns beginDraw()/endDraw(); this scene only draws content.
// Camera navigation uses the root ziviDomeLive SceneCameraService.
class FulldomePbrScene implements Scene {
  private SceneServices services;
  private SceneCameraService camera;
  private PApplet pApplet;

  private float time = 0f;
  private float orbitRadius = 840f;
  private float orbitSpeed = 0.012f;
  private float ringTilt = 0.42f;
  private final int starCount = 1220;
  private final PVector[] stars = new PVector[starCount];
  private final float[] starSizes = new float[starCount];
  private final int[] paletteA = { 244, 111, 94 };
  private final int[] paletteB = { 102, 210, 255 };
  private final int[] paletteC = { 218, 198, 255 };

  // --- Native scene-space camera service ---
  private final float initialDistance = -900f;

  // --- PBR pipeline ---
  private PShader pbr;              // null => fall back to fixed-function lighting
  private boolean usePbr = false;
  private PShape unitSphere;        // radius 1
  private PShape unitBox;           // size 1, centered
  private PShape unitCylinder;      // radius 1, height 1, axis Y, centered

  /*
   * Lighting is installed through PGraphicsOpenGL itself.
   *
   * Processing transforms light positions/directions into the eye space of
   * the currently active render camera. During cubemap capture this means
   * every face receives a coherent native light state without the example
   * reconstructing the face view matrix manually.
   */

  public void configure(SceneServices services) {
    this.services = services;
    this.camera = services.camera();
    this.pApplet = services.applet();
  }

  public void setupScene() {
    // Configure the native scene camera.
    camera.setDistanceLimits(-1200f, 1200f);
    // Protect against the collapse point at distance 0 (keeps the sign, no crossing).
    camera.setCollapseGuard(0f);
    resetCamera();
    camera.setInputEnabled(true);
    buildStarShell();
    buildPrimitives();
    loadPbrShader();
  }

  public void update() {
    time += orbitSpeed;
    // Camera smoothing is advanced natively by the library each frame.
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(5, 7, 18);
    pg.noStroke();

    pg.pushMatrix();

    // Move through space using the native scene-space camera service.
    camera.apply(pg);

    /*
     * Install the lights BEFORE the animated world transform.
     *
     * At this point Processing's modelview contains:
     *
     *     cubemap face camera * scene camera
     *
     * Processing therefore transforms the scene-space lights into the
     * correct eye space for the current cubemap face. The following world
     * rotation affects the objects, not the physical light rig.
     *
     * This is the same native light-state contract used by Processing's
     * own LIGHT shader pipeline.
     */
    applySceneLights(pg);

    pg.pushMatrix();
    pg.rotateY(time * 0.11f);

    // Background elements use the default pipeline (no PBR shading).
    pg.resetShader();
    renderStarShell(pg);
    renderDomeGrid(pg);

    // PBR-lit content.
    if (usePbr) {
      pg.shader(pbr);
      renderCentralCluster(pg);
      renderOrbitingModules(pg);
      renderSupportPillars(pg);
      pg.resetShader();
    } else {
      // The same native light rig drives Processing's fixed-function path.
      renderCentralCluster(pg);
      renderOrbitingModules(pg);
      renderSupportPillars(pg);
    }

    pg.popMatrix();
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
      case 'p':
      case 'P':
        // Toggle between PBR shader and fixed-function fallback.
        if (pbr != null) {
          usePbr = !usePbr;
          println("[FulldomePBR] PBR shader " + (usePbr ? "ON" : "OFF"));
        }
        break;
      case 'v':
      case 'V':
        resetCamera();
        break;
      case 'r':
        orbitRadius = 840f;
        ringTilt = 0.42f;
        orbitSpeed = 0.012f;
        break;
    }
  }

  public void dispose() {
    pbr = null;
    unitSphere = null;
    unitBox = null;
    unitCylinder = null;
    camera = null;
    pApplet = null;
    services = null;
  }

  public String getName() {
    return "FulldomePBR";
  }

  private void resetCamera() {
    // Gentle downward tilt so the composition reads well on the dome.
    camera.snapToAxisAngle(0, 0, 0, 1, 0, 0, PI / 12, initialDistance);
  }

  // -------------------------------------------------------------------------
  // Lighting
  // -------------------------------------------------------------------------

  // Fixed-function equivalent of the shader lights (fallback path).
  private void applySceneLights(PGraphicsOpenGL pg) {
    // Do not inherit stale light state from another render pass or cubemap face.
    pg.noLights();

    pg.ambientLight(40, 42, 68);
    pg.directionalLight(140, 140, 160, -0.45f, -0.75f, -0.25f);
    pg.directionalLight(40, 70, 120, 0.35f, -0.2f, 0.9f);
    pg.pointLight(255, 205, 170, 0, -240, 280);
    pg.pointLight(90, 150, 255, -520, 180, -260);
  }

  // -------------------------------------------------------------------------
  // Materials
  // -------------------------------------------------------------------------

  // Sets the active material either as PBR uniforms or fixed-function state.
  private void material(PGraphicsOpenGL pg, int r, int g, int b, float metallic, float roughness, float emissive) {
    if (usePbr) {
      pbr.set("uAlbedo", r / 255f, g / 255f, b / 255f);
      pbr.set("uMetallic", metallic);
      pbr.set("uRoughness", roughness);
      pbr.set("uEmissive", (r / 255f) * emissive, (g / 255f) * emissive, (b / 255f) * emissive);
    } else {
      float ambientScale = lerp(0.28f, 0.08f, metallic);
      float specStrength = lerp(80f, 255f, metallic);
      float shininess = lerp(10f, 120f, 1f - roughness);
      pg.noStroke();
      pg.ambient(r * ambientScale, g * ambientScale, b * ambientScale);
      pg.specular(specStrength, specStrength, specStrength);
      pg.shininess(shininess);
      if (emissive > 0f) {
        pg.emissive(r * emissive, g * emissive, b * emissive);
      } else {
        pg.emissive(0, 0, 0);
      }
      pg.fill(r, g, b);
    }
  }

  // Vibrant material defined in HSB. h in [0,360), s/v in [0,1].
  // Uses a pure HSB->RGB conversion so it never touches the buffer's colorMode.
  private void materialHSB(PGraphicsOpenGL pg, float h, float s, float v, float metallic, float roughness, float emissive) {
    int[] rgb = hsb2rgb(h, s, v);
    material(pg, rgb[0], rgb[1], rgb[2], metallic, roughness, emissive);
  }

  // Pure HSB -> RGB (0..255). h in degrees [0,360), s and v in [0,1].
  private int[] hsb2rgb(float h, float s, float v) {
    h = ((h % 360f) + 360f) % 360f;
    s = constrain(s, 0f, 1f);
    v = constrain(v, 0f, 1f);
    float c = v * s;
    float hp = h / 60f;
    float x = c * (1f - abs((hp % 2f) - 1f));
    float r1 = 0f, g1 = 0f, b1 = 0f;
    if (hp < 1f)      { r1 = c; g1 = x; }
    else if (hp < 2f) { r1 = x; g1 = c; }
    else if (hp < 3f) { g1 = c; b1 = x; }
    else if (hp < 4f) { g1 = x; b1 = c; }
    else if (hp < 5f) { r1 = x; b1 = c; }
    else              { r1 = c; b1 = x; }
    float m = v - c;
    return new int[] {
      round((r1 + m) * 255f),
      round((g1 + m) * 255f),
      round((b1 + m) * 255f)
    };
  }

  // -------------------------------------------------------------------------
  // Scene composition (retained-mode primitives)
  // -------------------------------------------------------------------------

  private void drawSphere(PGraphicsOpenGL pg, float radius) {
    pg.pushMatrix();
    pg.scale(radius);
    pg.shape(unitSphere);
    pg.popMatrix();
  }

  private void drawBox(PGraphicsOpenGL pg, float sx, float sy, float sz) {
    pg.pushMatrix();
    pg.scale(sx, sy, sz);
    pg.shape(unitBox);
    pg.popMatrix();
  }

  private void drawCylinder(PGraphicsOpenGL pg, float radius, float height) {
    pg.pushMatrix();
    pg.scale(radius, height, radius);
    pg.shape(unitCylinder);
    pg.popMatrix();
  }

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
    float hueShift = time * 8f; // gentle animated hue rotation for liveliness
    pg.pushMatrix();
    pg.translate(0, 40f + sin(time * 1.9f) * 18f, 0);

    // Glowing core (vivid gold).
    materialHSB(pg, 36f + 10f * sin(time * 0.7f), 0.95f, 1.0f, 0.9f, 0.08f, 0.55f);
    pg.pushMatrix();
    pg.rotateY(time * 0.7f);
    drawSphere(pg, 170f + 14f * sin(time * 2.7f));
    pg.popMatrix();

    // Mid shell (electric violet).
    materialHSB(pg, 275f, 0.72f, 0.96f, 0.15f, 0.55f, 0.0f);
    pg.pushMatrix();
    pg.rotateY(-time * 0.42f);
    pg.rotateZ(time * 0.16f);
    drawBox(pg, 150f, 150f, 150f);
    pg.popMatrix();

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
        materialHSB(pg, 348f + hueShift, 0.9f, 1.0f, 0.75f, 0.18f, 0.12f); // hot magenta
        drawSphere(pg, 42f);
      } else {
        materialHSB(pg, 190f + hueShift, 0.88f, 1.0f, 0.25f, 0.4f, 0.05f); // aqua
        drawBox(pg, 54f, 34f, 54f);
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
        materialHSB(pg, 205f, 0.18f, 0.98f, 0.95f, 0.12f, 0.0f); // bright polished metal
        drawBox(pg, 82f, 82f, 82f);
      } else if (i % 3 == 1) {
        materialHSB(pg, 192f, 0.9f, 1.0f, 0.5f, 0.22f, 0.03f);   // vivid cyan
        drawCylinder(pg, 30f, 128f);
      } else {
        materialHSB(pg, 28f, 0.95f, 1.0f, 0.2f, 0.65f, 0.14f);   // glowing orange
        drawSphere(pg, 58f);
      }

      // Highlight cap.
      pg.pushMatrix();
      pg.translate(0, -70f, 0);
      materialHSB(pg, 48f, 0.22f, 1.0f, 0.1f, 0.1f, 0.2f);       // warm glow
      drawSphere(pg, 18f);
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
      materialHSB(pg, 218f, 0.42f, 0.9f, 0.85f, 0.35f, 0.0f); // steel blue
      drawCylinder(pg, 24f, 340f);
      pg.popMatrix();
    }
  }

  // -------------------------------------------------------------------------
  // Resource construction
  // -------------------------------------------------------------------------

  private void loadPbrShader() {
    try {
      pbr = services.assets().loadShader("fulldome-pbr", "pbr.frag", "pbr.vert");
      usePbr = pbr != null;
      println("[FulldomePBR] PBR shader loaded: " + usePbr);
    } catch (Exception e) {
      pbr = null;
      usePbr = false;
      println("[FulldomePBR] PBR shader failed to load, using fixed-function: " + e.getMessage());
    }
  }

  private void buildPrimitives() {
    unitSphere = buildSphere(48, 32);
    unitBox = buildBox();
    unitCylinder = buildCylinder(32);
  }

  // UV sphere of radius 1 built from PVector vertices (normals == positions).
  private PShape buildSphere(int lon, int lat) {
    PShape s = pApplet.createShape();
    s.beginShape(TRIANGLES);
    s.noStroke();
    s.fill(255);
    for (int j = 0; j < lat; j++) {
      float t0 = map(j, 0, lat, 0, PI);
      float t1 = map(j + 1, 0, lat, 0, PI);
      for (int i = 0; i < lon; i++) {
        float p0 = map(i, 0, lon, 0, TWO_PI);
        float p1 = map(i + 1, 0, lon, 0, TWO_PI);

        PVector a = sphPoint(t0, p0);
        PVector b = sphPoint(t1, p0);
        PVector c = sphPoint(t1, p1);
        PVector d = sphPoint(t0, p1);

        addVertex(s, a);
        addVertex(s, b);
        addVertex(s, c);

        addVertex(s, a);
        addVertex(s, c);
        addVertex(s, d);
      }
    }
    s.endShape();
    return s;
  }

  private PVector sphPoint(float theta, float phi) {
    float x = sin(theta) * cos(phi);
    float y = cos(theta);
    float z = sin(theta) * sin(phi);
    return new PVector(x, y, z);
  }

  // Unit cube centered at origin (size 1), with per-face normals.
  private PShape buildBox() {
    PShape s = pApplet.createShape();
    s.beginShape(TRIANGLES);
    s.noStroke();
    s.fill(255);
    float h = 0.5f;

    // +X
    quad(s, new PVector(1, 0, 0), new PVector(h, -h, -h), new PVector(h, h, -h), new PVector(h, h, h), new PVector(h, -h, h));
    // -X
    quad(s, new PVector(-1, 0, 0), new PVector(-h, -h, h), new PVector(-h, h, h), new PVector(-h, h, -h), new PVector(-h, -h, -h));
    // +Y
    quad(s, new PVector(0, 1, 0), new PVector(-h, h, -h), new PVector(-h, h, h), new PVector(h, h, h), new PVector(h, h, -h));
    // -Y
    quad(s, new PVector(0, -1, 0), new PVector(-h, -h, h), new PVector(-h, -h, -h), new PVector(h, -h, -h), new PVector(h, -h, h));
    // +Z
    quad(s, new PVector(0, 0, 1), new PVector(-h, -h, h), new PVector(h, -h, h), new PVector(h, h, h), new PVector(-h, h, h));
    // -Z
    quad(s, new PVector(0, 0, -1), new PVector(h, -h, -h), new PVector(-h, -h, -h), new PVector(-h, h, -h), new PVector(h, h, -h));

    s.endShape();
    return s;
  }

  // Unit cylinder: radius 1, height 1 (y in [-0.5, 0.5]), axis Y.
  private PShape buildCylinder(int sides) {
    PShape s = pApplet.createShape();
    s.beginShape(TRIANGLES);
    s.noStroke();
    s.fill(255);
    float half = 0.5f;
    float step = TWO_PI / sides;

    for (int i = 0; i < sides; i++) {
      float a0 = i * step;
      float a1 = (i + 1) * step;
      float x0 = cos(a0), z0 = sin(a0);
      float x1 = cos(a1), z1 = sin(a1);

      // Side quad (radial normals).
      PVector nA = new PVector(x0, 0, z0);
      PVector nB = new PVector(x1, 0, z1);
      addVertexN(s, new PVector(x0, -half, z0), nA);
      addVertexN(s, new PVector(x0, half, z0), nA);
      addVertexN(s, new PVector(x1, half, z1), nB);

      addVertexN(s, new PVector(x0, -half, z0), nA);
      addVertexN(s, new PVector(x1, half, z1), nB);
      addVertexN(s, new PVector(x1, -half, z1), nB);

      // Top cap (+Y).
      PVector up = new PVector(0, 1, 0);
      addVertexN(s, new PVector(0, half, 0), up);
      addVertexN(s, new PVector(x0, half, z0), up);
      addVertexN(s, new PVector(x1, half, z1), up);

      // Bottom cap (-Y).
      PVector down = new PVector(0, -1, 0);
      addVertexN(s, new PVector(0, -half, 0), down);
      addVertexN(s, new PVector(x1, -half, z1), down);
      addVertexN(s, new PVector(x0, -half, z0), down);
    }

    s.endShape();
    return s;
  }

  private void quad(PShape s, PVector n, PVector a, PVector b, PVector c, PVector d) {
    addVertexN(s, a, n);
    addVertexN(s, b, n);
    addVertexN(s, c, n);
    addVertexN(s, a, n);
    addVertexN(s, c, n);
    addVertexN(s, d, n);
  }

  private void addVertex(PShape s, PVector p) {
    s.normal(p.x, p.y, p.z);
    s.vertex(p.x, p.y, p.z);
  }

  private void addVertexN(PShape s, PVector p, PVector n) {
    s.normal(n.x, n.y, n.z);
    s.vertex(p.x, p.y, p.z);
  }

  private void buildStarShell() {
    for (int i = 0; i < starCount; i++) {
      float u = random(-1f, 1f);
      float theta = random(TWO_PI);
      float ss = sqrt(max(0f, 1f - u * u));
      float radius = random(2100f, 2600f);
      stars[i] = new PVector(cos(theta) * ss * radius, u * radius, sin(theta) * ss * radius);
      starSizes[i] = random(1.0f, 2.6f);
    }
  }
}
