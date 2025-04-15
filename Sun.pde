import processing.core.*;
import processing.opengl.*;

public class Sun {
  private PApplet pApplet;
  private float radius;
  private float baseRatio;
  private float mass;  // ← Agora vindo do ConfigLoader
  private PVector position;
  private color col;
  private float rotationAngle = 0;
  private float rotationSpeed = 0.01f;
  private PImage texture;
  private PShape shape;
  private int renderingMode = 2;

  public Sun(PApplet pApplet, float radius, float mass, PVector position, color col, PImage texture) {
    this.pApplet = pApplet;
    this.radius = radius;
    this.mass = mass;
    this.position = position.copy();
    this.col = col;
    this.texture = texture;

    // Salva o ratio base com relação ao SUN_VISUAL_RADIUS para futura reescala
    this.baseRatio = radius / SUN_VISUAL_RADIUS;
  }

  // Novo método para reescalar o raio com base em simParams
  public void applyScalingFactors(SimParams simParams) {
    this.radius = SUN_VISUAL_RADIUS * baseRatio * simParams.globalScale;
  }

  public void update(float dt) {
    rotationAngle += rotationSpeed * dt;
  }

  public void display(PGraphicsOpenGL pg, boolean showLabel, ShaderManager shaderManager) {
    pg.pushMatrix();
    pg.translate(position.x, position.y, position.z);
    pg.rotateY(rotationAngle);
    pg.scale(radius);

    if (renderingMode == 0) {
      pg.noFill();
      pg.stroke(WIREFRAME_COLOR);
      pg.strokeWeight(WIREFRAME_STROKE_WEIGHT);
    } else if (renderingMode == 2) {
      PShader shader = shaderManager.getShader("sun");
      if (shader != null && texture != null) {
        shader.set("texSampler", texture); // ⚠️ crucial para o sampler funcionar
        pg.shader(shader);
      } else {
        pg.fill(col);
      }
      pg.noStroke();
    } else {
      pg.fill(col);
      pg.noStroke();
    }

    if (shape != null) {
      pg.shape(shape);
    }

    pg.resetShader();
    pg.popMatrix();

    if (showLabel) {
      drawLabel(pg);
    }
  }

  private void drawLabel(PGraphicsOpenGL pg) {
    pg.pushMatrix();
    PVector labelPos = position.copy();
    labelPos.y -= (radius * 1.2f);
    pg.translate(labelPos.x, labelPos.y, labelPos.z);
    pg.fill(255);
    pg.textSize(pApplet.max(10, radius * 0.4f));
    pg.textAlign(CENTER, BOTTOM);
    pg.text("Sun", 0, 0);
    pg.popMatrix();
  }

  public void setRenderingMode(int mode) {
    this.renderingMode = mode;
  }

  public int getRenderingMode() {
    return renderingMode;
  }

  public void buildShape(PApplet p, ShapeManager shapeManager) {
    // Obtém o shape do sol, de acordo com o modo de renderização e texturização desejado
    shape = shapeManager.getShape("Sun", renderingMode, texture);
    
    // Caso o modo sólido seja usado, aplica a cor base
    if (renderingMode == 1 && shape != null) {
      shape.setFill(col);
    }
    
    // Configuração dos parâmetros de material para simular auto-iluminação e realces intensos:
    // Ajuste a tonalidade base do sol
    //shape.setAmbient(p.color(255, 200, 0));
    // Reflexos intensos e concentrados
    //shape.setSpecular(p.color(255, 255, 255));
    // Auto-iluminação: faz o sol "brilhar" internamente
    //shape.setEmissive(p.color(255, 255, 255));
    // Valor alto para concentrar os brilhos
    //shape.setShininess(100.0);
  }


  public PVector getPosition() {
    return position.copy();
  }

  public float getRadius() {
    return radius;
  }

  public float getMass() {
    return mass;
  }

  public void dispose() {
    shape = null;
    texture = null;
  }
}
