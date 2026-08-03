// Fulldome example scene with full PBR lighting via GLSL shaders and
// retained-mode primitives (PShape / VBO) built from PVector geometry.
//
// The library owns beginDraw()/endDraw(); this scene only draws content.
// Camera navigation uses the native ziviDomeLive OrbitCamera service.
class Scene1 implements Scene {
  private final zividomelive parent;
  private final PApplet pApplet;

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

  // --- Native scene-space camera service (ziviDomeLive OrbitCamera) ---
  private final float initialDistance = -900f;

  // --- PBR pipeline ---
  private PShader pbr;              // null => fall back to fixed-function lighting
  private boolean usePbr = false;
  private PShape unitSphere;        // radius 1
  private PShape unitBox;           // size 1, centered
  private PShape unitCylinder;      // radius 1, height 1, axis Y, centered

  // Light data (scene/world space) uploaded to the shader each frame.
  private final int lightCount = 4;
  private final float[] lightPos = new float[lightCount * 3];
  private final float[] lightColor = new float[lightCount * 3];
  private final float[] lightType = new float[lightCount]; // 0 = directional, 1 = point
  private final float[] ambient = { 22f / 255f, 22f / 255f, 30f / 255f };
  // Hemispheric IBL environment (enrichment): sky above, ground below.
  private final float[] skyColor = { 0.10f, 0.14f, 0.26f };
  private final float[] groundColor = { 0.02f, 0.02f, 0.05f };
  private final float envIntensity = 1.0f;
  private final PMatrix3D viewMatrix = new PMatrix3D();

  Scene1(zividomelive parent) {
    this.parent = parent;
    this.pApplet = parent.getPApplet();
    // Configure and enable the native scene camera.
    parent.setSceneCameraInputEnabled(true);
    parent.getSceneCamera().setDistanceLimits(-1200f, 1200f);
    // Protect against the collapse point at distance 0 (keeps the sign, no crossing).
    parent.getSceneCamera().setCollapseGuard(250f);
    resetCamera();
  }

  public void setupScene() {
    buildStarShell();
    buildPrimitives();
    loadPbrShader();
    configureLights();
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
    parent.getSceneCamera().apply(pg);

    // Capture the view matrix (camera only, before world spin / object transforms)
    // so the PBR shader can transform world-space lights into eye space.
    pg.getMatrix(viewMatrix);

    pg.pushMatrix();
    pg.rotateY(time * 0.11f);

    // Background elements use the default pipeline (no PBR lighting).
    pg.resetShader();
    renderStarShell(pg);
    renderDomeGrid(pg);

    // PBR-lit content.
    if (usePbr) {
      pg.shader(pbr);
      uploadLightUniforms();
      renderCentralCluster(pg);
      renderOrbitingModules(pg);
      renderSupportPillars(pg);
      pg.resetShader();
    } else {
      // Fixed-function fallback keeps the example working without shaders.
      applySceneLights(pg);
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

  public void mouseEvent(MouseEvent event) {
    // Camera navigation is handled natively by the library
    // (setSceneCameraInputEnabled(true) in the constructor).
  }

  public void controlEvent(controlP5.ControlEvent theEvent) {
    println("Control event in FulldomePBR: " + theEvent.getName());
  }

  public String getName() {
    return "FulldomePBR";
  }

  private void resetCamera() {
    // Gentle downward tilt so the composition reads well on the dome.
    Quaternion q = Quaternion.fromAxisAngle(1, 0, 0, PI / 12);
    parent.getSceneCamera().snapTo(0, 0, 0, q, initialDistance);
  }

  // -------------------------------------------------------------------------
  // Lighting
  // -------------------------------------------------------------------------

  private void configureLights() {
    // 0: key directional (cool white)
    setLight(0, 0f, -0.45f, -0.75f, -0.25f, 140f, 140f, 160f);
    // 1: fill directional (blue)
    setLight(1, 0f, 0.35f, -0.2f, 0.9f, 40f, 70f, 120f);
    // 2: warm point light
    setLight(2, 1f, 0f, -240f, 280f, 255f, 205f, 170f);
    // 3: cool point light
    setLight(3, 1f, -520f, 180f, -260f, 90f, 150f, 255f);
  }

  private void setLight(int i, float type, float x, float y, float z, float r, float g, float b) {
    lightType[i] = type;
    lightPos[i * 3] = x;
    lightPos[i * 3 + 1] = y;
    lightPos[i * 3 + 2] = z;
    lightColor[i * 3] = r / 255f;
    lightColor[i * 3 + 1] = g / 255f;
    lightColor[i * 3 + 2] = b / 255f;
  }

  private void uploadLightUniforms() {
    pbr.set("uViewMatrix", viewMatrix);
    pbr.set("uLightCount", lightCount);
    pbr.set("uAmbient", ambient[0], ambient[1], ambient[2]);
    pbr.set("uLightPos", lightPos, 3);
    pbr.set("uLightColor", lightColor, 3);
    pbr.set("uLightType", lightType, 1);
    // Hemispheric IBL environment.
    pbr.set("uSkyColor", skyColor[0], skyColor[1], skyColor[2]);
    pbr.set("uGroundColor", groundColor[0], groundColor[1], groundColor[2]);
    pbr.set("uEnvIntensity", envIntensity);
  }

  // Fixed-function equivalent of the shader lights (fallback path).
  private void applySceneLights(PGraphicsOpenGL pg) {
    pg.ambientLight(22, 22, 30);
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
    pg.pushMatrix();
    pg.translate(0, 40f + sin(time * 1.9f) * 18f, 0);

    // Glowing core.
    material(pg, 255, 180, 120, 0.9f, 0.08f, 0.35f);
    pg.pushMatrix();
    pg.rotateY(time * 0.7f);
    drawSphere(pg, 170f + 14f * sin(time * 2.7f));
    pg.popMatrix();

    // Mid shell.
    material(pg, paletteC[0], paletteC[1], paletteC[2], 0.15f, 0.55f, 0.0f);
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
        material(pg, paletteA[0], paletteA[1], paletteA[2], 0.75f, 0.18f, 0.08f);
        drawSphere(pg, 42f);
      } else {
        material(pg, paletteB[0], paletteB[1], paletteB[2], 0.25f, 0.4f, 0.03f);
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
        material(pg, 245, 245, 248, 0.95f, 0.12f, 0.0f);
        drawBox(pg, 82f, 82f, 82f);
      } else if (i % 3 == 1) {
        material(pg, 96, 208, 255, 0.5f, 0.22f, 0.0f);
        drawCylinder(pg, 30f, 128f);
      } else {
        material(pg, 255, 165, 110, 0.2f, 0.65f, 0.1f);
        drawSphere(pg, 58f);
      }

      // Highlight cap.
      pg.pushMatrix();
      pg.translate(0, -70f, 0);
      material(pg, 255, 250, 225, 0.1f, 0.1f, 0.15f);
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
      material(pg, 170, 185, 215, 0.85f, 0.35f, 0.0f);
      drawCylinder(pg, 24f, 340f);
      pg.popMatrix();
    }
  }

  // -------------------------------------------------------------------------
  // Resource construction
  // -------------------------------------------------------------------------

  private void loadPbrShader() {
    try {
      pbr = pApplet.loadShader("pbr.frag", "pbr.vert");
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

