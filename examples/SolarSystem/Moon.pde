public class Moon implements CelestialBody {
    // ——————————————— Display ———————————————
    private final PApplet pApplet;
    private float radiusPx;
    private final int col;
    private final String name;
    private final PImage texture;
    private int renderingMode = 2;

    // caches para performance de render
    private PShape cachedShape;
    private PShape orbitShapeUniform;
    private int cachedRenderingMode = -1;

    // rotação visual
    private float rotationAngle = 0;
    private final float rotationSpeed;

    // ——————————————— Física ———————————————
    private final double massSolar;
    private final double radiusAU;
    private final double rotationPeriodDays;
    private final double semiMajorAxisAU;
    private final double perihelionAU;
    private final double aphelionAU;
    private final double eccentricity;
    private final PVector positionAU;
    private final PVector velocityAU;
    private CelestialBody centralBody;

    // elementos orbitais
    private final double orbitInclinationRad;
    private final double argumentOfPeriapsisRad;
    private final double longitudeAscendingNodeRad;
    private final double meanAnomalyRad;
    private final boolean alignWithAxis;
    private double currentMeanAnomalyRad;
    private double elapsedOrbitDays;
    private double elapsedOrbitCompensation;
    private final double[] preciseOrbitalPosition = new double[3];
    private final double[] preciseOrbitalVelocity = new double[3];
    private final double[] preciseGlobalPosition = new double[3];
    private final double[] preciseGlobalVelocity = new double[3];

    // Estado J2000 original (posição e velocidade em UA / UA·dia⁻¹)
    PVector initialPosAU;
    PVector initialVelAU;

    /** Construtor ajustado */
    public Moon(PApplet pApplet,
                double massSolar,
                double radiusAU,
                double rotationPeriodDays,
                double semiMajorAxisAU,
                double perihelionAU,
                double aphelionAU,
                double eccentricity,
                PVector initialPosAU,
                PVector initialVelAU,
                String name,
                int displayColor,
                PImage texture,
                CelestialBody parent,
                double orbitInclinationRad,
                double argumentOfPeriapsisRad,
                double longitudeAscendingNodeRad,
                double meanAnomalyRad,
                boolean alignWithAxis) {

        this.pApplet                   = pApplet;
        this.massSolar                 = massSolar;
        this.radiusAU                  = radiusAU;
        this.rotationPeriodDays        = rotationPeriodDays;
        this.semiMajorAxisAU           = semiMajorAxisAU;
        this.perihelionAU              = perihelionAU;
        this.aphelionAU                = aphelionAU;
        this.eccentricity              = eccentricity;
        this.positionAU                = initialPosAU.copy();
        this.velocityAU                = initialVelAU.copy();
        this.initialPosAU              = positionAU.copy();
        this.initialVelAU              = velocityAU.copy();
        this.name                      = name;
        this.col                       = displayColor;
        this.texture                   = texture;
        this.centralBody               = parent;
        this.orbitInclinationRad       = orbitInclinationRad;
        this.argumentOfPeriapsisRad    = argumentOfPeriapsisRad;
        this.longitudeAscendingNodeRad = longitudeAscendingNodeRad;
        this.meanAnomalyRad            = meanAnomalyRad;
        this.alignWithAxis             = alignWithAxis;
        this.currentMeanAnomalyRad     = meanAnomalyRad;

        this.rotationSpeed = (float) (PApplet.TWO_PI / rotationPeriodDays);

        buildOrbitShapeUniform();
    }

    public void setRadiusPx(float px) {
        this.radiusPx = px;
    }

    // ——————————————— Escala visual ———————————————
    public void applyScalingFactors() {
        if (centralBody instanceof Planet) {
            Planet parent = (Planet) centralBody;
            float sizeRatio = (float) (radiusAU / parent.getRadiusAU());
            this.radiusPx = parent.getRadiusPx() * sizeRatio;
        }
    }

    // ——————————————— Implementação de CelestialBody ———————————————
    @Override public PVector getPositionAU()               { return positionAU; }
    @Override public PVector getVelocityAU()               { return velocityAU; }
    @Override public double  getMassSolar()                { return massSolar; }
    @Override public CelestialBody getCentralBody()        { return centralBody; }
    @Override public void    setCentralBody(CelestialBody c){ this.centralBody = c; }

    @Override public double getSemiMajorAxisAU()           { return semiMajorAxisAU; }
    @Override public double getPerihelionAU()              { return perihelionAU; }
    @Override public double getAphelionAU()                { return aphelionAU; }
    @Override public double getEccentricity()              { return eccentricity; }
    @Override public double getOrbitInclinationRad()       { return orbitInclinationRad; }
    @Override public double getArgumentOfPeriapsisRad()    { return argumentOfPeriapsisRad; }
    @Override public double getLongitudeAscendingNodeRad() { return longitudeAscendingNodeRad; }
    @Override public double getMeanAnomalyRad()            { return meanAnomalyRad; }
    @Override public double getRadiusAU()                  { return radiusAU; }
    @Override public double getRotationPeriodDays()        { return rotationPeriodDays; }

    @Override
    public void propagateKepler(double dtDays) {
        if (centralBody == null) return;

        double a = 0.5 * (perihelionAU + aphelionAU);
        double mu = G_DAY * centralBody.getMassSolar();
        double n = Math.sqrt(mu / (a * a * a));

        advanceOrbitTime(dtDays);
        currentMeanAnomalyRad = normalizeRadians(meanAnomalyRad + n * elapsedOrbitDays);

        initialState(
          a,
          eccentricity,
          currentMeanAnomalyRad,
          centralBody.getMassSolar(),
          preciseOrbitalPosition,
          preciseOrbitalVelocity
        );

        applyOrbitalPlaneToGlobal(
          preciseOrbitalPosition,
          longitudeAscendingNodeRad,
          orbitInclinationRad,
          argumentOfPeriapsisRad,
          preciseGlobalPosition
        );
        applyOrbitalPlaneToGlobal(
          preciseOrbitalVelocity,
          longitudeAscendingNodeRad,
          orbitInclinationRad,
          argumentOfPeriapsisRad,
          preciseGlobalVelocity
        );

        // 5) composição absoluta: foco + componente orbital
        PVector focusPos = centralBody.getPositionAU();
        PVector focusVel = centralBody.getVelocityAU();

        positionAU.set(
          (float) (focusPos.x + preciseGlobalPosition[0]),
          (float) (focusPos.y + preciseGlobalPosition[1]),
          (float) (focusPos.z + preciseGlobalPosition[2])
        );
        velocityAU.set(
          (float) (focusVel.x + preciseGlobalVelocity[0]),
          (float) (focusVel.y + preciseGlobalVelocity[1]),
          (float) (focusVel.z + preciseGlobalVelocity[2])
        );
    }

    /** Soma de Kahan: preserva deltas temporais muito pequenos em execuções longas. */
    private void advanceOrbitTime(double dtDays) {
        double correctedDelta = dtDays - elapsedOrbitCompensation;
        double nextElapsed = elapsedOrbitDays + correctedDelta;
        elapsedOrbitCompensation = (nextElapsed - elapsedOrbitDays) - correctedDelta;
        elapsedOrbitDays = nextElapsed;
    }

    // ——————————————— Atualização de rotação ———————————————
    /** Atualiza a rotação visual da lua */
    public void updateRotation(double dtDays) {
        rotationAngle = (float) ((rotationAngle + rotationSpeed * dtDays) % PApplet.TWO_PI);
    }

    // ——————————————— Atualização geral ———————————————
    /**
    * Atualiza a simulação da órbita e a rotação visual.
    * Deve ser chamada a cada frame com dt em dias.
    */
    public void update(double dtDays) {
        // 2) atualiza ângulo de rotação
        updateRotation(dtDays);
    }

    // ——————————————— Desenha órbitas pontilhadas das luas ———————————————
    // chame isto uma vez após os parâmetros da lua estarem prontos:
    public void buildOrbitShapeUniform() {
    int seg = 180;
    orbitShapeUniform = createShape();
    orbitShapeUniform.beginShape(PConstants.LINE_LOOP);
    orbitShapeUniform.noFill();
    orbitShapeUniform.stroke(150, 150, 255, 150);
    orbitShapeUniform.strokeWeight(1);

    double a = 0.5 * (perihelionAU + aphelionAU);
    double b = a * Math.sqrt(1.0 - eccentricity * eccentricity);
    double e = eccentricity;

    for (int j = 0; j < seg; j++) {
        double angle = TWO_PI_DOUBLE * j / seg;
        double[] vPlane = {
          a * (Math.cos(angle) - e),
          0.0,
          b * Math.sin(angle)
        };
        double[] v3d = new double[3];
        applyOrbitalPlaneToGlobal(
          vPlane,
          longitudeAscendingNodeRad,
          orbitInclinationRad,
          argumentOfPeriapsisRad,
          v3d
        );
        orbitShapeUniform.vertex((float) v3d[0], (float) v3d[1], (float) v3d[2]);
    }

    orbitShapeUniform.endShape();
    }

    public void displayOrbit(PGraphicsOpenGL pg) {
    if (orbitShapeUniform == null) return;
    float baseScale  = pxPerAU();
    float orbitScale = baseScale * bodyScale;
    PVector focusPx  = centralBody.getPositionAU().copy().mult(baseScale);

    pg.pushMatrix();
        pg.translate(focusPx.x, focusPx.y, focusPx.z);
        pg.scale(orbitScale);
        pg.shape(orbitShapeUniform);
    pg.popMatrix();
    }

    // ——————————————— Desenho do corpo ———————————————
    /** Desenha a lua em si, com órbitas e corpos ampliados pelo mesmo bodyScale
    *  e mantendo o raio proporcional ao do planeta-pai. */
    public void display(PGraphicsOpenGL pg,
                        boolean showLabel,
                        int renderingMode,
                        ShapeManager shapeManager) {

        // 1) recalcule radiusPx relativo ao pai
        applyScalingFactors();
        // agora this.radiusPx == parent.getRadiusPx() * (this.radiusAU / parent.getRadiusAU())

        // 2) escalas físicas
        float baseScale  = pxPerAU();            // UA → px
        float orbitScale = baseScale * bodyScale; // amplificação unificada

        // 3) foco fixo em px (planeta-pai)
        PVector focusPx = centralBody.getPositionAU().copy().mult(baseScale);

        // 4) deslocamento da lua em UA → px já ampliado
        PVector relAU  = PVector.sub(positionAU, centralBody.getPositionAU());
        PVector offset = relAU.mult(orbitScale);

        // 5) posição final
        PVector posPx  = PVector.add(focusPx, offset);

        pg.pushMatrix();
        pg.translate(posPx.x, posPx.y, posPx.z);
        pg.rotateZ(alignWithAxis ? 0f : (float) argumentOfPeriapsisRad);

        // 6) usa o radiusPx já calculado
        pg.scale(radiusPx);

        // 7) modos de render
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
        pg.fill(255);
        pg.textAlign(PConstants.CENTER, PConstants.BOTTOM);
        pg.text(name, posPx.x, posPx.y - (radiusPx + 5), posPx.z);
        }
    }


    private PShape getCachedShape(ShapeManager shapeManager) {
        if (cachedShape == null || cachedRenderingMode != renderingMode) {
            cachedShape = shapeManager.getShape(name, renderingMode, texture);
            cachedRenderingMode = renderingMode;
        }
        return cachedShape;
    }

    public void buildShape(PApplet p, ShapeManager shapeManager) {
        this.cachedShape = null;
    }

    public void setRenderingMode(int mode) {
        this.renderingMode = mode;
        this.cachedShape = null;
    }

    public int getRenderingMode() {
        return renderingMode;
    }

    public String getName() {
        return name;
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
        this.cachedShape = null;
    }
}
