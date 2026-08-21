import processing.opengl.*;
import java.util.ArrayList;
import java.util.List;

public class Planet implements CelestialBody {
    // ——————————————— Campos de display ———————————————
    private final PApplet pApplet;
    private float radiusPx;
    private final float baseRatio;
    private final int col;
    private final String name;
    private final PImage texture, ringTexture;
    private int renderingMode = 2;

    // caches para performance de render
    private PShape cachedShape;
    private int cachedRenderingMode = -1;

    // rotação visual
    private float rotationAngle = 0;
    private final float rotationSpeed;
    private final boolean hasRings;

    // ——————————————— Campos de física ———————————————
    private final double massSolar;
    private final double radiusAU;
    private final double rotationPeriodDays;
    private final PVector positionAU;
    private final PVector velocityAU;
    private CelestialBody centralBody;

    // elementos orbitais
    private final double perihelionAU;
    private final double aphelionAU;
    private final double eccentricity;
    private final double orbitInclinationRad;
    private final double argumentOfPeriapsisRad;
    private final double semiMajorAxisAU;
    private final double longitudeAscendingNodeRad;
    private final double meanAnomalyRad;
    private final double orbitalPeriodDays;
    private final double orbitalVelocityAUperDay;
    private final float axisTiltRad;
    // Mantida em double: incrementar um ângulo float pequeno fazia movimentos
    // lentos desaparecerem até acumular um salto visível.
    private double currentMeanAnomalyRad;
    private double elapsedOrbitDays;
    private double elapsedOrbitCompensation;
    private final double[] preciseOrbitalPosition = new double[3];
    private final double[] preciseOrbitalVelocity = new double[3];
    private final double[] preciseGlobalPosition = new double[3];
    private final double[] preciseGlobalVelocity = new double[3];

    // luas
    private final List<Moon> moons = new ArrayList<>();

    // Anéis de Saturno
    private float ringRotationAngle = 0;
    private final float ringRotationSpeed = 0.05f;
    private PShape saturnRingsShape;
    private int cachedRingMode = -1;

    // Estado J2000 original (posição e velocidade em UA / UA·dia⁻¹)
    PVector initialPosAU;
    PVector initialVelAU;

    /** Construtor */
    public Planet(PApplet pApplet,
                  double massSolar,
                  double radiusAU,
                  double sunRadiusAU,
                  double rotationPeriodDays,
                  PVector initialPosAU,
                  PVector initialVelAU,
                  int displayColor,
                  String name,
                  PImage texture,
                  PImage ringTexture,
                  double orbitInclinationRad,
                  float axisTiltRad,
                  double perihelionAU,
                  double aphelionAU,
                  double eccentricity,
                  double argumentOfPeriapsisRad,
                  double longitudeAscendingNodeRad,
                  double meanAnomalyRad,
                  double orbitalPeriodDays,
                  double orbitalVelocityAUperDay,
                  double semiMajorAxisAU) {

        this.pApplet                   = pApplet;
        this.massSolar                 = massSolar;
        this.radiusAU                  = radiusAU;
        this.rotationPeriodDays        = rotationPeriodDays;
        this.positionAU                = initialPosAU.copy();
        this.velocityAU                = initialVelAU.copy();
        this.initialPosAU              = positionAU.copy();
        this.initialVelAU              = velocityAU.copy();
        this.col                       = displayColor;
        this.name                      = name;
        this.texture                   = texture;
        this.ringTexture               = ringTexture;
        this.orbitInclinationRad       = orbitInclinationRad;
        this.axisTiltRad               = axisTiltRad;
        this.perihelionAU              = perihelionAU;
        this.aphelionAU                = aphelionAU;
        this.eccentricity              = eccentricity;
        this.argumentOfPeriapsisRad    = argumentOfPeriapsisRad;
        this.longitudeAscendingNodeRad = longitudeAscendingNodeRad;
        this.meanAnomalyRad            = meanAnomalyRad;
        this.orbitalPeriodDays         = orbitalPeriodDays;
        this.orbitalVelocityAUperDay   = orbitalVelocityAUperDay;
        this.semiMajorAxisAU           = semiMajorAxisAU;

        this.rotationSpeed = (float) (PApplet.TWO_PI / rotationPeriodDays);
        this.baseRatio     = (float) (radiusAU / sunRadiusAU);
        this.hasRings      = "Saturn".equals(name);
        this.currentMeanAnomalyRad = meanAnomalyRad;

        applyScalingFactors();
    }

    // ——————————————— Escala visual ———————————————
    public void applyScalingFactors() {
        this.radiusPx = sunRadiusPx()
                      * baseRatio
                      * planetAmplification;
        this.cachedRingMode = -1;
    }

    // ——————————————— Implementação CelestialBody ———————————————
    @Override public PVector getPositionAU()               { return positionAU; }
    @Override public PVector getVelocityAU()               { return velocityAU; }
    @Override public double  getMassSolar()                { return massSolar; }
    @Override public CelestialBody getCentralBody()        { return centralBody; }
    @Override public void    setCentralBody(CelestialBody c){ this.centralBody = c; }
    @Override public double getPerihelionAU()              { return perihelionAU; }
    @Override public double getAphelionAU()                { return aphelionAU; }
    @Override public double getEccentricity()              { return eccentricity; }
    @Override public double getOrbitInclinationRad()       { return orbitInclinationRad; }
    @Override public double getArgumentOfPeriapsisRad()    { return argumentOfPeriapsisRad; }
    @Override public double getSemiMajorAxisAU()           { return semiMajorAxisAU; }
    @Override public double getLongitudeAscendingNodeRad() { return longitudeAscendingNodeRad; }
    @Override public double getMeanAnomalyRad()            { return meanAnomalyRad; }
    @Override public double getRadiusAU()                  { return radiusAU; }
    @Override public double getRotationPeriodDays()        { return rotationPeriodDays; }

    @Override
    public void propagateKepler(double dtDays) {
        if (centralBody != null) {
            double a = 0.5 * (perihelionAU + aphelionAU);
            double mu = G_DAY * centralBody.getMassSolar();
            double n = Math.sqrt(mu / (a * a * a));

            advanceOrbitTime(dtDays);
            currentMeanAnomalyRad = normalizeRadians(meanAnomalyRad + n * elapsedOrbitDays);

            // 3) chame o solver com essa nova anomalia
            keplerSolve(
                centralBody.getPositionAU(),
                positionAU,
                velocityAU,
                perihelionAU,
                aphelionAU,
                eccentricity,
                orbitInclinationRad,
                longitudeAscendingNodeRad,
                argumentOfPeriapsisRad,
                currentMeanAnomalyRad,
                0.0,                       // já contei todo dt em currentMeanAnomalyRad
                centralBody.getMassSolar(),
                preciseOrbitalPosition,
                preciseOrbitalVelocity,
                preciseGlobalPosition,
                preciseGlobalVelocity
            );
        }
    }

    /** Soma de Kahan: preserva deltas temporais muito pequenos em execuções longas. */
    private void advanceOrbitTime(double dtDays) {
        double correctedDelta = dtDays - elapsedOrbitCompensation;
        double nextElapsed = elapsedOrbitDays + correctedDelta;
        elapsedOrbitCompensation = (nextElapsed - elapsedOrbitDays) - correctedDelta;
        elapsedOrbitDays = nextElapsed;
    }

    // ——————————————— Luas ———————————————
    public void addMoon(Moon m) { moons.add(m); }
    public List<Moon> getMoons() { return new ArrayList<>(moons); }
    public float getAxisTiltRad() { return axisTiltRad; }

    // ——————————————— Animação e Renderização ———————————————
    public void updateRotation(double dtDays) {
        rotationAngle += rotationSpeed * dtDays;
        if (hasRings) {
            ringRotationAngle += ringRotationSpeed * dtDays;
        }
    }

    public void update(double dtDays) {
        updateRotation(dtDays);
    }

    public void display(PGraphicsOpenGL pg,
                        boolean showLabel,
                        boolean drawMoonOrbits,
                        int renderingMode,
                        ShapeManager shapeManager) {

        float scale = pxPerAU();
        PVector posPx = positionAU.copy().mult(scale);

        pg.pushMatrix();
            pg.translate(posPx.x, posPx.y, posPx.z);

            if (hasRings) {
                drawSaturnRings(pg, renderingMode);
            }

            pg.rotateZ(axisTiltRad);
            pg.rotateY(rotationAngle);
            pg.scale(radiusPx);

            if (renderingMode == 0) {
                pg.noFill();
                pg.stroke(WIREFRAME_COLOR);
                pg.strokeWeight(WIREFRAME_STROKE_WEIGHT);
            } else if (renderingMode == 1) {
                pg.noStroke();
                pg.fill(col);
            } else {
                pg.noStroke();
                pg.fill(texture != null ? 255 : col);
            }

            pg.shape(getCachedShape(shapeManager));
        pg.popMatrix();

        if (showLabel) {
            pg.pushMatrix();
            pg.translate(posPx.x, posPx.y - (radiusPx + 5), posPx.z);
            pg.fill(255);
            pg.textSize(Math.max(10, radiusPx * 0.5f));
            pg.textAlign(PConstants.CENTER, PConstants.BOTTOM);
            pg.text(name, 0, 0);
            pg.popMatrix();
        }
    }

    private PShape getCachedShape(ShapeManager shapeManager) {
        if (cachedShape == null || cachedRenderingMode != renderingMode) {
            cachedShape = shapeManager.getShape(name, renderingMode, texture);
            cachedRenderingMode = renderingMode;
            if (renderingMode == 1 && cachedShape != null) {
                cachedShape.setFill(col);
            }
        }
        return cachedShape;
    }

    private void drawSaturnRings(PGraphicsOpenGL pg, int renderingMode) {
        if (cachedRingMode != renderingMode || saturnRingsShape == null) {
            buildSaturnRingsShape(renderingMode);
            cachedRingMode = renderingMode;
        }
        pg.pushMatrix();
            pg.rotateZ(axisTiltRad);
            if (axisTiltRad > PApplet.HALF_PI) pg.rotateY(PApplet.PI);
            pg.rotateY(ringRotationAngle);
            pg.shape(saturnRingsShape);
        pg.popMatrix();
    }

    private void buildSaturnRingsShape(int renderingMode) {
        saturnRingsShape = pApplet.createShape();
        saturnRingsShape.beginShape(QUAD_STRIP);
        saturnRingsShape.textureMode(NORMAL);

        if (renderingMode == 0) {
            saturnRingsShape.noFill();
            saturnRingsShape.stroke(180);
            saturnRingsShape.strokeWeight(0.5f);
        } else {
            saturnRingsShape.noStroke();
        }

        float innerRingRatio = 1.15f;
        float outerRingRatio = 2.35f;
        int ringBands = 60;
        int ringSegments = 90;
        float step = (outerRingRatio - innerRingRatio) / ringBands;

        // Gaps: Cassini, Encke
        float[] gapCenters = {117500f / 60000f, 133600f / 60000f, 136500f / 60000f};
        float[] gapWidths = {4800f / 60000f, 325f / 60000f, 42f / 60000f};

        for (int i = 0; i < ringBands; i++) {
            float r1Ratio = innerRingRatio + i * step;
            float r2Ratio = r1Ratio + step;

            boolean insideGap = false;
            if (renderingMode == 1) {
                for (int g = 0; g < gapCenters.length; g++) {
                    float gapStart = gapCenters[g] - gapWidths[g] / 2f;
                    float gapEnd = gapCenters[g] + gapWidths[g] / 2f;
                    if (r1Ratio >= gapStart && r2Ratio <= gapEnd) {
                        insideGap = true;
                        break;
                    }
                }
            }
            if (insideGap) continue;

            float r1 = radiusPx * r1Ratio;
            float r2 = radiusPx * r2Ratio;

            float u1 = 0.5f + (r1Ratio - 1.75f) * 0.5f;
            float u2 = 0.5f + (r2Ratio - 1.75f) * 0.5f;

            if (renderingMode == 1) {
                float wave = PApplet.sin(i * 0.3f);
                float alphaFactor = 0.6f + 0.4f * wave;
                int alpha = (int)(alphaFactor * 200);
                int base = (int)PApplet.lerp(240, 255, PApplet.sin(i * 0.2f) * 0.5f + 0.5f);
                saturnRingsShape.fill(base, base, base, alpha);
            } else if (renderingMode == 2) {
                saturnRingsShape.fill(255);
            }

            for (int j = 0; j <= ringSegments; j++) {
                float angle = PApplet.TWO_PI * j / ringSegments;
                float cosA = PApplet.cos(angle);
                float sinA = PApplet.sin(angle);
                saturnRingsShape.vertex(r1 * cosA, 0, r1 * sinA, u1, 0.0f);
                saturnRingsShape.vertex(r2 * cosA, 0, r2 * sinA, u2, 1.0f);
            }
        }
        saturnRingsShape.endShape();
        if (renderingMode == 2 && ringTexture != null) {
            saturnRingsShape.setTexture(ringTexture);
        }
    }

    // ——————————————— Dispose, getters, setters ———————————————
    public void buildShape(PApplet p, ShapeManager sm) {
        cachedShape = null;
        cachedRingMode = -1;
    }

    public void setRenderingMode(int mode) {
        this.renderingMode = mode;
        this.cachedShape   = null;
    }

    public int getRenderingMode() {
        return renderingMode;
    }

    public String getName() {
        return name;
    }

    public float getRadiusPx() {
        return radiusPx;
    }

    /**
    * Restaura exatamente a posição e velocidade que o corpo
    * tinha na época J2000 (gravadas em initialPosAU/initialVelAU).
    */
    void resetToJ2000() {
        positionAU.set(initialPosAU);
        velocityAU.set(initialVelAU);
        currentMeanAnomalyRad = meanAnomalyRad;
        elapsedOrbitDays = 0.0;
        elapsedOrbitCompensation = 0.0;
    }

    public void dispose() {
        cachedShape = null;
        saturnRingsShape = null;
        moons.forEach(Moon::dispose);
        moons.clear();
    }
}
