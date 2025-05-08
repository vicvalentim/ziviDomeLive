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
    private final float massSolar;
    private final float radiusAU;
    private final float rotationPeriodDays;
    private final float semiMajorAxisAU;
    private final float perihelionAU;
    private final float aphelionAU;
    private final float eccentricity;
    private final PVector positionAU;
    private final PVector velocityAU;
    private CelestialBody centralBody;

    // elementos orbitais
    private final float orbitInclinationRad;
    private final float argumentOfPeriapsisRad;
    private final float longitudeAscendingNodeRad;
    private final float meanAnomalyRad;
    private final boolean alignWithAxis;
    private float currentMeanAnomalyRad;

    // Estado J2000 original (posição e velocidade em UA / UA·dia⁻¹)
    PVector initialPosAU;
    PVector initialVelAU;

    /** Construtor ajustado */
    public Moon(PApplet pApplet,
                float massSolar,
                float radiusAU,
                float rotationPeriodDays,
                float semiMajorAxisAU,
                float perihelionAU,
                float aphelionAU,
                float eccentricity,
                PVector initialPosAU,
                PVector initialVelAU,
                String name,
                int displayColor,
                PImage texture,
                CelestialBody parent,
                float orbitInclinationRad,
                float argumentOfPeriapsisRad,
                float longitudeAscendingNodeRad,
                float meanAnomalyRad,
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

        this.rotationSpeed = PApplet.TWO_PI / rotationPeriodDays;

        buildOrbitShapeUniform();
    }

    public void setRadiusPx(float px) {
        this.radiusPx = px;
    }

    // ——————————————— Escala visual ———————————————
    public void applyScalingFactors() {
        if (centralBody instanceof Planet) {
            Planet parent = (Planet) centralBody;
            float sizeRatio = radiusAU / parent.getRadiusAU();
            this.radiusPx = parent.getRadiusPx() * sizeRatio;
        }
    }

    // ——————————————— Implementação de CelestialBody ———————————————
    @Override public PVector getPositionAU()               { return positionAU; }
    @Override public PVector getVelocityAU()               { return velocityAU; }
    @Override public float   getMassSolar()                { return massSolar; }
    @Override public CelestialBody getCentralBody()        { return centralBody; }
    @Override public void    setCentralBody(CelestialBody c){ this.centralBody = c; }

    @Override public float getSemiMajorAxisAU()            { return semiMajorAxisAU; }
    @Override public float getPerihelionAU()               { return perihelionAU; }
    @Override public float getAphelionAU()                 { return aphelionAU; }
    @Override public float getEccentricity()               { return eccentricity; }
    @Override public float getOrbitInclinationRad()        { return orbitInclinationRad; }
    @Override public float getArgumentOfPeriapsisRad()     { return argumentOfPeriapsisRad; }
    @Override public float getLongitudeAscendingNodeRad()  { return longitudeAscendingNodeRad; }
    @Override public float getMeanAnomalyRad()             { return meanAnomalyRad; }
    @Override public float getRadiusAU()                   { return radiusAU; }
    @Override public float getRotationPeriodDays()         { return rotationPeriodDays; }

    @Override
    public void propagateKepler(float dtDays) {
        if (centralBody == null) return;

        // 1) parâmetros da órbita
        float a   = 0.5f * (perihelionAU + aphelionAU);
        float mu  = G_DAY * centralBody.getMassSolar();
        float n   = PApplet.sqrt(mu / (a * a * a));

        // 2) atualiza a anomalia média
        currentMeanAnomalyRad = (currentMeanAnomalyRad + n * dtDays) % PApplet.TWO_PI;

        // 3) resolve via initialState (que já usa μ = G_DAY * massFocus)
        PVector rOrb = new PVector(), vOrb = new PVector();
        initialState(
        a,
        eccentricity,
        currentMeanAnomalyRad,
        centralBody.getMassSolar(),
        rOrb,
        vOrb
        );

        // 4) gira do plano orbital → eclíptica
        PVector rEcl = applyOrbitalPlaneToGlobal(rOrb, longitudeAscendingNodeRad, orbitInclinationRad, argumentOfPeriapsisRad);
        PVector vEcl = applyOrbitalPlaneToGlobal(vOrb, longitudeAscendingNodeRad, orbitInclinationRad, argumentOfPeriapsisRad);

        // 5) composição absoluta: foco + componente orbital
        PVector focusPos = centralBody.getPositionAU();
        PVector focusVel = centralBody.getVelocityAU();

        positionAU.set(PVector.add(focusPos, rEcl));
        velocityAU.set(PVector.add(focusVel, vEcl));
    }

    // ——————————————— Atualização de rotação ———————————————
    /** Atualiza a rotação visual da lua */
    public void updateRotation(float dtDays) {
        rotationAngle = (rotationAngle + rotationSpeed * dtDays) % PApplet.TWO_PI;
    }

    // ——————————————— Atualização geral ———————————————
    /**
    * Atualiza a simulação da órbita e a rotação visual.
    * Deve ser chamada a cada frame com dt em dias.
    */
    public void update(float dtDays) {
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

    float a = 0.5f * (perihelionAU + aphelionAU);
    float b = a * sqrt(1 - eccentricity*eccentricity);
    float e = eccentricity;

    for (int j = 0; j < seg; j++) {
        float θ  = TWO_PI * j / seg;
        float xp = a * (cos(θ) - e);
        float zp = b * sin(θ);
        PVector vPlane = new PVector(xp, 0, zp);
        PVector v3d    = applyOrbitalPlaneToGlobal(
                        vPlane,
                        longitudeAscendingNodeRad,
                        orbitInclinationRad,
                        argumentOfPeriapsisRad);
        orbitShapeUniform.vertex(v3d.x, v3d.y, v3d.z);
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
                        ShapeManager shapeManager,
                        ShaderManager shaderManager) {

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
        pg.rotateZ(alignWithAxis ? 0 : argumentOfPeriapsisRad);

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
            if (texture != null && shaderManager.getShader("planet") != null) {
                pg.shader(shaderManager.getShader("planet"));
            } else {
                pg.fill(col);
            }
        }

        pg.shape(getCachedShape(shapeManager));
        pg.resetShader();
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
    }

    public void dispose() {
        this.cachedShape = null;
    }
}
