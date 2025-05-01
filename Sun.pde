// ——————————————————————————————————————————————————————————————————————
// Sun — Classe que representa o Sol no sistema (corpo fixo)
// ——————————————————————————————————————————————————————————————————————
import processing.core.*;
import processing.opengl.*;

public class Sun implements CelestialBody {
    private final PApplet pApplet;
    private final SimParams simParams;

    // ——————————————— Display ———————————————
    private float radiusPx;
    private final float baseRatio;
    private final color col;
    private final PVector position;
    private float rotationAngle = 0;
    private final float rotationSpeed;
    private final PImage texture;
    private PShape shape;
    private int renderingMode = 2;

    // ——————————————— Física ———————————————
    private final float massSolar;
    private final float radiusAU;
    private final float rotationPeriodDays;
    private final PVector positionAU = new PVector(0, 0, 0);
    private final PVector velocityAU = new PVector(0, 0, 0);

    // ——————————————— Construtor ———————————————
    public Sun(PApplet pApplet,
               SimParams simParams,
               float radiusPx,
               float massSolar,
               float radiusAU,
               float rotationPeriodDays,
               PVector initialPixelPos,
               color displayColor,
               PImage texture) {
        this.pApplet = pApplet;
        this.simParams = simParams;
        this.radiusPx = radiusPx;
        this.baseRatio = radiusPx / sunRadiusPx(simParams);
        this.massSolar = massSolar;
        this.radiusAU = radiusAU;
        this.rotationPeriodDays = rotationPeriodDays;
        this.rotationSpeed = PApplet.TWO_PI / rotationPeriodDays;
        this.position = initialPixelPos.copy();
        this.col = displayColor;
        this.texture = texture;
    }

    // ——————————————— Escala visual ———————————————
    public void applyScalingFactors(SimParams simParams) {
        this.radiusPx = sunRadiusPx(simParams) * baseRatio;
    }

    // ——————————————— Atualização ———————————————
    public void update(float dtDays) {
        rotationAngle += rotationSpeed * dtDays;
    }

    // ——————————————— Renderização ———————————————
    public void display(PGraphicsOpenGL pg,
                        boolean showLabel,
                        ShaderManager shaderManager) {
        pg.pushMatrix();
            pg.translate(position.x, position.y, position.z);
            pg.rotateY(rotationAngle);
            pg.scale(radiusPx);

            if (renderingMode == 0) {
                pg.noFill();
                pg.stroke(WIREFRAME_COLOR);
                pg.strokeWeight(WIREFRAME_STROKE_WEIGHT);
            } else if (renderingMode == 2) {
                PShader shader = shaderManager.getShader("sun");
                if (shader != null && texture != null) {
                    shader.set("texSampler", texture);
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
            PVector lp = position.copy();
            lp.y -= radiusPx * 1.2f;
            pg.translate(lp.x, lp.y, lp.z);
            pg.fill(255);
            pg.textSize(pApplet.max(10, radiusPx * 0.4f));
            pg.textAlign(PConstants.CENTER, PConstants.BOTTOM);
            pg.text("Sun", 0, 0);
        pg.popMatrix();
    }

    // ——————————————— Controle de forma ———————————————
    public void buildShape(PApplet p, ShapeManager shapeManager) {
        shape = shapeManager.getShape("Sun", renderingMode, texture);
        if (renderingMode == 1 && shape != null) {
            shape.setFill(col);
        }
    }

    public void setRenderingMode(int mode) {
        this.renderingMode = mode;
    }

    public int getRenderingMode() {
        return renderingMode;
    }

    public float getRadius() {
        return radiusPx;
    }

    public float getMass() {
        return massSolar;
    }

    public void dispose() {
        shape = null;
    }

    // ——————————————— Implementação de CelestialBody ———————————————
    @Override public PVector getPositionAU() { return positionAU; }
    @Override public PVector getVelocityAU() { return velocityAU; }
    @Override public float getMassSolar() { return massSolar; }
    @Override public CelestialBody getCentralBody() { return null; }
    @Override public void setCentralBody(CelestialBody c) { /* não aplicável ao Sol */ }
    @Override public void propagateKepler(float dtDays) { /* não se move */ }
    @Override public float getPerihelionAU() { return 0; }
    @Override public float getAphelionAU() { return 0; }
    @Override public float getEccentricity() { return 0; }
    @Override public float getOrbitInclinationRad() { return 0; }
    @Override public float getArgumentOfPeriapsisRad() { return 0; }
    @Override public float getRadiusAU() { return radiusAU; }
    @Override public float getRotationPeriodDays() { return rotationPeriodDays; }
    @Override public float getSemiMajorAxisAU()           { return 0; }
    @Override public float getLongitudeAscendingNodeRad()  { return 0; }
    @Override public float getMeanAnomalyRad()             { return 0; }
}
