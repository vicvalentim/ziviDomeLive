import processing.opengl.*;
import java.util.ArrayList;
import java.util.List;

public class Planet implements CelestialBody {
    // ——————————————— Campos de display ———————————————
    private final PApplet pApplet;
    private final SimParams simParams;
    private float radiusPx;                   // raio para render em pixels
    private final float baseRatio;            // radiusAU / SUN_VISUAL_RADIUS
    private final color col;
    private final String name;
    private final PImage texture, ringTexture;
    private int renderingMode = 2;

    // caches para performance de render
    private PShape cachedShape;
    private int cachedRenderingMode = -1;

    // rotação visual
    private float rotationAngle = 0;
    private final float rotationFactor = 0.2f;
    private final float rotationSpeed;        // rad/dia
    private final boolean hasRings;

    // ——————————————— Campos de física ———————————————
    private final float massSolar;            // massa em M☉
    private final float radiusAU;             // raio físico em AU
    private final float rotationPeriodDays;   // dias
    private final PVector positionAU;         // AU
    private final PVector velocityAU;         // AU/dia
    private CelestialBody centralBody;        // foco de Kepler

    // elementos orbitais
    private final float perihelionAU;
    private final float aphelionAU;
    private final float eccentricity;
    private final float orbitInclinationRad;
    private final float argumentOfPeriapsisRad = 0f;
    private final float orbitalPeriodDays;
    private final float orbitalVelocityAUperDay;
    private final float axisTiltRad;

    // luas
    private final List<Moon> moons = new ArrayList<>();

    // Anéis de Saturno
    private float ringRotationAngle = 0;
    private final float ringRotationSpeed = 0.05f;
    private PShape saturnRingsShape;
    private int cachedRingMode = -1;

    /** Construtor */
    public Planet(PApplet pApplet,
                  SimParams simParams,
                  float massSolar,
                  float radiusAU,
                  float sunRadiusAU,               // NOVO
                  float rotationPeriodDays,
                  PVector initialPosAU,
                  PVector initialVelAU,
                  color displayColor,
                  String name,
                  PImage texture,
                  PImage ringTexture,
                  float orbitInclinationRad,
                  float axisTiltRad,
                  float perihelionAU,
                  float aphelionAU,
                  float eccentricity,
                  float orbitalPeriodDays,
                  float orbitalVelocityAUperDay) {

        this.pApplet                 = pApplet;
        this.simParams               = simParams;
        this.massSolar               = massSolar;
        this.radiusAU                = radiusAU;
        this.rotationPeriodDays      = rotationPeriodDays;
        this.positionAU              = initialPosAU.copy();
        this.velocityAU              = initialVelAU.copy();
        this.col                     = displayColor;
        this.name                    = name;
        this.texture                 = texture;
        this.ringTexture             = ringTexture;
        this.orbitInclinationRad     = orbitInclinationRad;
        this.axisTiltRad             = axisTiltRad;
        this.perihelionAU            = perihelionAU;
        this.aphelionAU              = aphelionAU;
        this.eccentricity            = eccentricity;
        this.orbitalPeriodDays       = orbitalPeriodDays;
        this.orbitalVelocityAUperDay = orbitalVelocityAUperDay;

        this.rotationSpeed = PApplet.TWO_PI / rotationPeriodDays;
        this.baseRatio     = radiusAU / sunRadiusAU; // <<<< AQUI é o correto!
        this.hasRings      = "Saturn".equals(name);

        applyScalingFactors(simParams);
    }

    // ——————————————— Escala visual ———————————————
    public void applyScalingFactors(SimParams simParams) {
        this.radiusPx = SUN_VISUAL_RADIUS
                      * baseRatio
                      * simParams.globalScale
                      * simParams.planetAmplification;
        this.cachedRingMode = -1;
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
            orbitInclinationRad,
            argumentOfPeriapsisRad,  // já é 0f por declaração
            dtDays,
            centralBody.getMassSolar()
          );
        }
      }


    @Override public float getPerihelionAU()            { return perihelionAU; }
    @Override public float getAphelionAU()              { return aphelionAU; }
    @Override public float getEccentricity()            { return eccentricity; }
    @Override public float getOrbitInclinationRad()     { return orbitInclinationRad; }
    @Override public float getArgumentOfPeriapsisRad()  { return argumentOfPeriapsisRad; }
    @Override public float getRadiusAU()                { return radiusAU; }
    @Override public float getRotationPeriodDays()      { return rotationPeriodDays; }

    // ——————————————— Luas ———————————————
    public void addMoon(Moon m)     { moons.add(m); }
    public List<Moon> getMoons()    { return new ArrayList<>(moons); }

    // ——————————————— Renderização ———————————————
    public void updateRotation(float dtDays) {
        rotationAngle += rotationSpeed * dtDays * rotationFactor;
        if (hasRings) {
            ringRotationAngle += ringRotationSpeed * dtDays;
        }
    }

    /** Atualiza posição orbital e rotação axial */
    public void update(float dtDays) {
        updateRotation(dtDays);
    }


    public void display(PGraphicsOpenGL pg,
                        boolean showLabel,
                        boolean drawMoonOrbits,
                        int renderingMode,
                        ShapeManager shapeManager,
                        ShaderManager shaderManager) {

        float scale = PIXELS_PER_AU * simParams.globalScale;
        PVector posPx = positionAU.copy().mult(scale);

        pg.pushMatrix();

        if (hasRings) {
            drawSaturnRings(pg, renderingMode); // Desenha os anéis primeiro
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
        } else if (renderingMode == 2) {
            pg.noStroke();
            if (texture != null) {
                PShader shader = shaderManager.getShader("planet");
                if (shader != null) {
                    pg.shader(shader);
                }
            } else {
                pg.fill(col);
            }
        }

        PShape s = getCachedShape(shapeManager);
        pg.shape(s);
        pg.resetShader();

        pg.popMatrix();

        if (showLabel) {
            pg.pushMatrix();
            pg.translate(posPx.x, posPx.y - (radiusPx + 5), posPx.z);
            pg.fill(255);
            pg.textSize(Math.max(10, radiusPx * 0.5f));
            pg.textAlign(CENTER, BOTTOM);
            pg.text(name, 0, 0);
            pg.popMatrix();
        }
    }

    private PShape getCachedShape(ShapeManager shapeManager) {
        if (cachedShape==null || cachedRenderingMode!=renderingMode) {
          cachedShape = shapeManager.getShape(name, renderingMode, texture);
          cachedRenderingMode = renderingMode;
          if (renderingMode==1 && cachedShape!=null) cachedShape.setFill(col);
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
        if (axisTiltRad > PApplet.HALF_PI) {
            pg.rotateY(PApplet.PI);
        }
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

    public void buildShape(PApplet p, ShapeManager shapeManager) {
        this.cachedShape = null;
        this.cachedRingMode = -1; // <<<<<<<<<<<<<< ADICIONE esta linha
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

    // ——————————————— Dispose ———————————————
    public void dispose() {
      cachedShape      = null;
      saturnRingsShape = null;
      moons.forEach(Moon::dispose);
      moons.clear();
    }
}
