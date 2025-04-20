import processing.core.*;
import processing.opengl.*;

/**
 * Moon — implementa CelestialBody, lógica de órbita elíptica e display.
 */
public class Moon implements CelestialBody {
    // ——————————————— Física ———————————————
    private final float massSolar;             // M☉
    private final float radiusAU;              // semi-eixo maior em AU
    private final float rotationPeriodDays;    // dias
    private final PVector positionAU;          // AU
    private final PVector velocityAU;          // AU/dia
    private CelestialBody centralBody;         // planeta pai

    // elementos orbitais
    private final float perihelionAU;
    private final float aphelionAU;
    private final float eccentricity;
    private final float inclinationRad;
    private final float argumentOfPeriapsisRad;
    private final boolean alignWithPlanetAxis;

    // ——————————————— Display ———————————————
    private final PApplet pApplet;
    private final SimParams simParams;
    private final String name;
    private final color displayColor;
    private final PImage texture;
    private int renderingMode = 2;

    private float radiusPx;                    // px
    private final float baseRatio;             // (raio da Lua) / (raio do Sol)
    private float rotationAngle = 0;
    private final float rotationSpeed;         // rad/dia

    // caches
    private final PVector cachedDrawPosition = new PVector();
    private float cachedCentralRadius = -1;
    private boolean drawPositionDirty = true;

    private PShape cachedShape;
    private int cachedRenderingMode = -1;

    private int cachedOrbitMode = -1;
    private float cachedSemiMajor = -1, cachedSemiMinor = -1, cachedFocus = -1;

    private float cachedLabelSize = -1, cachedRadiusForLabel = -1;

    /** Construtor unificado */
    public Moon(PApplet pApplet,
                SimParams simParams,
                float massSolar,
                float radiusAU,
                float sunRadiusAU,            // novo parâmetro
                float rotationPeriodDays,
                PVector initialPosAU,
                PVector initialVelAU,
                String name,
                color displayColor,
                PImage texture,
                CelestialBody centralBody,
                float inclinationRad,
                float argumentOfPeriapsisRad,
                boolean alignWithPlanetAxis,
                float perihelionAU,
                float aphelionAU,
                float eccentricity) {
        this.pApplet               = pApplet;
        this.simParams             = simParams;
        this.massSolar             = massSolar;
        this.radiusAU              = radiusAU;
        this.rotationPeriodDays    = rotationPeriodDays;
        this.positionAU            = initialPosAU.copy();
        this.velocityAU            = initialVelAU.copy();
        this.name                  = name;
        this.displayColor          = displayColor;
        this.texture               = texture;
        this.centralBody           = centralBody;
        this.inclinationRad        = inclinationRad;
        this.argumentOfPeriapsisRad= argumentOfPeriapsisRad;
        this.alignWithPlanetAxis   = alignWithPlanetAxis;
        this.perihelionAU          = perihelionAU;
        this.aphelionAU             = aphelionAU;
        this.eccentricity          = eccentricity;

        this.rotationSpeed = PApplet.TWO_PI / rotationPeriodDays;
        this.baseRatio     = radiusAU / sunRadiusAU;  // corrigido aqui

        applyScalingFactors(simParams);
    }

    // ——————————————— Escala visual ———————————————
    public void applyScalingFactors(SimParams simParams) {
        this.radiusPx = SUN_VISUAL_RADIUS
                      * baseRatio
                      * simParams.globalScale
                      * simParams.planetAmplification;
        this.drawPositionDirty = true;
        this.cachedOrbitMode   = -1;
    }

    // ——————————————— CelestialBody (física) ———————————————
    @Override public PVector getPositionAU()         { return positionAU; }
    @Override public PVector getVelocityAU()         { return velocityAU; }
    @Override public float   getMassSolar()          { return massSolar; }
    @Override public CelestialBody getCentralBody()  { return centralBody; }
    public  void    setCentralBody(CelestialBody c)  { this.centralBody = c; }

    @Override
    public void propagateKepler(float dtDays) {
        if (centralBody != null) {
            keplerSolve(
                centralBody.getPositionAU(),
                positionAU,
                velocityAU,
                perihelionAU,
                aphelionAU,
                eccentricity,
                inclinationRad,
                argumentOfPeriapsisRad,
                dtDays,
                centralBody.getMassSolar()
            );
            this.drawPositionDirty = true;
        }
    }

    @Override public float getPerihelionAU()           { return perihelionAU; }
    @Override public float getAphelionAU()             { return aphelionAU; }
    @Override public float getEccentricity()           { return eccentricity; }
    @Override public float getOrbitInclinationRad()    { return inclinationRad; }
    @Override public float getArgumentOfPeriapsisRad() { return argumentOfPeriapsisRad; }
    @Override public float getRadiusAU()               { return radiusAU; }
    @Override public float getRotationPeriodDays()     { return rotationPeriodDays; }

    // ——————————————— Renderização ———————————————
    public void updateRotation(float dtDays) {
        rotationAngle += rotationSpeed * dtDays;
    }

    /** Atualiza órbita e rotação da lua */
    public void update(float dtDays) {
      updateRotation(dtDays);
    }

    public PVector getDrawPosition(float centralRadiusPx) {
        if (drawPositionDirty || cachedCentralRadius != centralRadiusPx) {
            PVector parentPx = centralBody.getPositionAU()
                                .copy().mult(PIXELS_PER_AU * simParams.globalScale);
            PVector selfOff  = positionAU.copy().mult(PIXELS_PER_AU * simParams.globalScale);
            cachedDrawPosition.set(parentPx).add(selfOff);
            cachedCentralRadius = centralRadiusPx;
            drawPositionDirty  = false;
        }
        return cachedDrawPosition;
    }

    public void displayOrbit(PGraphicsOpenGL pg, float centralRadiusPx) {
        if (cachedOrbitMode != renderingMode) {
            cachedSemiMajor = radiusAU * PIXELS_PER_AU * simParams.globalScale;
            cachedSemiMinor = cachedSemiMajor * PApplet.sqrt(1 - eccentricity*eccentricity);
            cachedFocus     = cachedSemiMajor * eccentricity;
            cachedOrbitMode = renderingMode;
        }
        PVector center = centralBody.getPositionAU()
                           .copy().mult(PIXELS_PER_AU * simParams.globalScale);
        pg.pushMatrix();
          pg.translate(center.x, center.y, center.z);
          pg.rotateX(HALF_PI);
          if (alignWithPlanetAxis) {
            pg.rotateZ(((Planet)centralBody).axisTiltRad);
          } else {
            pg.rotateZ(argumentOfPeriapsisRad);
          }
          pg.rotateX(inclinationRad);
          pg.translate(-cachedFocus, 0, 0);

          pg.noFill();
          pg.stroke(150,150,255,150);
          pg.strokeWeight(1);
          pg.ellipse(0,0,2*cachedSemiMajor,2*cachedSemiMinor);
        pg.popMatrix();
    }

    public void display(PGraphicsOpenGL pg,
                        boolean showLabel,
                        int renderingMode,
                        ShapeManager shapeManager,
                        ShaderManager shaderManager) {

        //updateRotation(1f/60f);

        float scale = PIXELS_PER_AU * simParams.globalScale;
        PVector pos = getDrawPosition(centralBody.getRadiusAU() * scale); // <- pega posição correta
        pg.pushMatrix();
          pg.translate(pos.x, pos.y, pos.z); // <- translate para posição orbital correta

          pg.rotateZ(rotationAngle);
          pg.scale(radiusPx);

          if (renderingMode == 0) {
            pg.noFill(); 
            pg.stroke(WIREFRAME_COLOR); 
            pg.strokeWeight(WIREFRAME_STROKE_WEIGHT);
          } else if (renderingMode == 1) {
            pg.noStroke(); 
            pg.fill(displayColor);
          } else {
            pg.noStroke();
            PShader sh = shaderManager.getShader("planet");
            if (sh != null && texture != null) pg.shader(sh);
            else pg.fill(displayColor);
          }

          if (cachedShape == null || cachedRenderingMode != renderingMode) {
            cachedShape = shapeManager.getShape(name, renderingMode, texture);
            cachedRenderingMode = renderingMode;
          }
          pg.shape(cachedShape);
          pg.resetShader();
        pg.popMatrix();

        if (showLabel) {
          if (cachedRadiusForLabel != radiusPx) {
            cachedLabelSize = PApplet.max(10, radiusPx * 0.5f);
            cachedRadiusForLabel = radiusPx;
          }
          pg.pushMatrix();
            pg.translate(pos.x, pos.y - (radiusPx + 5), pos.z); // <- também corrige a label
            pg.fill(255);
            pg.textSize(cachedLabelSize);
            pg.textAlign(CENTER, BOTTOM);
            pg.text(name, 0, 0);
          pg.popMatrix();
        }
    }

    public void buildShape(PApplet p, ShapeManager shapeManager) {
        this.cachedShape = null;
    }

    public void setRenderingMode(int mode) {
        this.renderingMode = mode;
        this.cachedShape   = null;
    }
    public int getRenderingMode() {
        return renderingMode;
    }

    public void dispose() {
        cachedShape    = null;
    }
}
