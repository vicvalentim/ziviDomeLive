// ——————————————————————————————————————————————————————————————————————
// Sun — Classe que representa o Sol no sistema (corpo fixo)
// ——————————————————————————————————————————————————————————————————————
public class Sun implements CelestialBody {
    private final PApplet pApplet;

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
    private final double massSolar;
    private final double radiusAU;
    private final double rotationPeriodDays;
    private final PVector positionAU = new PVector(0, 0, 0);
    private final PVector velocityAU = new PVector(0, 0, 0);

    // ——————————————— Construtor ———————————————
    public Sun(PApplet pApplet,
               float radiusPx,
               double massSolar,
               double radiusAU,
               double rotationPeriodDays,
               PVector initialPixelPos,
               color displayColor,
               PImage texture) {
        this.pApplet = pApplet;
        this.radiusPx = radiusPx;
        this.baseRatio = radiusPx / sunRadiusPx();
        this.massSolar = massSolar;
        this.radiusAU = radiusAU;
        this.rotationPeriodDays = rotationPeriodDays;
        this.rotationSpeed = (float) (PApplet.TWO_PI / rotationPeriodDays);
        this.position = initialPixelPos.copy();
        this.col = displayColor;
        this.texture = texture;
    }

    // ——————————————— Escala visual ———————————————
    public void applyScalingFactors() {
        this.radiusPx = sunRadiusPx() * baseRatio;
    }

    // ——————————————— Atualização ———————————————
    public void update(double dtDays) {
        rotationAngle += rotationSpeed * dtDays;
    }

    // ——————————————— Renderização ———————————————
    public void display(PGraphicsOpenGL pg,
                        boolean showLabel) {
        pg.pushMatrix();
            pg.translate(position.x, position.y, position.z);
            pg.rotateY(rotationAngle);
            pg.scale(radiusPx);

            if (renderingMode == 0) {
                pg.noFill();
                pg.stroke(WIREFRAME_COLOR);
                pg.strokeWeight(WIREFRAME_STROKE_WEIGHT);
            } else if (renderingMode == 2) {
                pg.fill(texture != null ? 255 : col);
                pg.noStroke();
            } else {
                pg.fill(col);
                pg.noStroke();
            }

            if (shape != null) {
                pg.shape(shape);
            }
        pg.popMatrix();

        if (showLabel) {
            drawLabel(pg);
        }
    }

    private void drawLabel(PGraphicsOpenGL pg) {
        pg.pushMatrix();
            pg.translate(position.x, position.y - radiusPx * 1.2f, position.z);
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

    public double getMass() {
        return massSolar;
    }

    public void dispose() {
        shape = null;
    }

    // ——————————————— Implementação de CelestialBody ———————————————
    @Override public PVector getPositionAU() { return positionAU; }
    @Override public PVector getVelocityAU() { return velocityAU; }
    @Override public double getMassSolar() { return massSolar; }
    @Override public CelestialBody getCentralBody() { return null; }
    @Override public void setCentralBody(CelestialBody c) { /* não aplicável ao Sol */ }
    @Override public void propagateKepler(double dtDays) { /* não se move */ }
    @Override public double getPerihelionAU() { return 0.0; }
    @Override public double getAphelionAU() { return 0.0; }
    @Override public double getEccentricity() { return 0.0; }
    @Override public double getOrbitInclinationRad() { return 0.0; }
    @Override public double getArgumentOfPeriapsisRad() { return 0.0; }
    @Override public double getRadiusAU() { return radiusAU; }
    @Override public double getRotationPeriodDays() { return rotationPeriodDays; }
    @Override public double getSemiMajorAxisAU()          { return 0.0; }
    @Override public double getLongitudeAscendingNodeRad(){ return 0.0; }
    @Override public double getMeanAnomalyRad()            { return 0.0; }
}
