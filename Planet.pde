// -------------------------------------------------
// Classe Planet
// -------------------------------------------------
public class Planet {
    private PApplet pApplet;
    float mass;
    float radius;
    PVector position;
    PVector velocity;
    PVector acceleration;
    color col;
    String name;
    ArrayList<Moon> moons;

    // Spin
    float rotationAngle;
    float rotationSpeed;

    // Parâmetros orbitais
    float orbitRadius;
    float orbitInclination;
    float anomaly;

    // Inclinação do eixo (axisTilt) em radianos
    float axisTilt;

    // Renderização
    PShape shape;
    boolean hasRings = false;
    PShape saturnRingsShape;

    // Parâmetros de rotação dos anéis
    float ringRotationAngle = 0;
    float ringRotationSpeed = 0.05f;
    
    // Fator de rotação
    private float rotationFactor = 0.2f;
    
    // Campo para a textura do Sol (caso o planeta seja o Sol)
    PImage sunTexture; 


    public Planet(PApplet pApplet,
                  float m,
                  float r,
                  PVector pos,
                  PVector vel,
                  color c,
                  String n,
                  float rotationPeriod,
                  float orbitInclination,
                  float axisTilt,
                  PImage sunTexture) {
        this.pApplet = pApplet;
        mass = m;
        radius = r;
        position = pos.copy();
        velocity = vel.copy();
        acceleration = new PVector();
        col = c;
        name = n;
        moons = new ArrayList<Moon>();
        rotationAngle = 0;
        rotationSpeed = TWO_PI / rotationPeriod;
        orbitRadius = pos.mag();
        this.orbitInclination = orbitInclination;
        anomaly = 0;
        this.axisTilt = axisTilt;
        this.sunTexture = sunTexture;  // Armazena a textura se fornecida

        shape = pApplet.createShape(PConstants.SPHERE, 1);
        shape.setFill(true);
        shape.setStroke(false);
        shape.setFill(col);

        if (name.equals("Saturno")) {
            hasRings = true;
            buildSaturnRingsShape();
        }
    }

    public void updateRotation(float dt) {
        rotationAngle += rotationSpeed * dt * rotationFactor;
        if (hasRings) {
            ringRotationAngle += ringRotationSpeed * dt;
        }
    }

    public void updateMoons(float dt) {
        for (Moon m : moons) {
            m.update(dt, position, velocity);
        }
    }

    public void addMoon(Moon m) {
        moons.add(m);
    }

    /**
     * Retorna a posição usada para desenhar o planeta.
     * Para planetas distintos do Sol, ajusta a posição adicionando o raio visual do Sol.
     */
    public PVector getDrawPosition() {
        if (name.equals("Sol")) {
            return position.copy();
        } else {
            PVector d = position.copy();
            if (d.mag() > 0) {
                d.setMag(d.mag() + SUN_VISUAL_RADIUS);
            }
            return d;
        }
    }

    public void display(PGraphicsOpenGL pg, boolean showLabel, boolean selected) {
        pg.pushMatrix();
            PVector d = getDrawPosition();
            pg.translate(d.x, d.y, d.z);

            // Desenha anéis (no caso de Saturno)
            if (hasRings && saturnRingsShape != null) {
                pg.pushMatrix();
                    pg.rotateZ(axisTilt);
                    // Se a inclinação do eixo for maior que 90°, inverte os anéis
                    if (axisTilt > PConstants.HALF_PI) {
                        pg.rotateY(PConstants.PI);
                    }
                    pg.rotateY(ringRotationAngle);
                    pg.shape(saturnRingsShape);
                pg.popMatrix();
            }

            // Aplica rotação do planeta
            pg.rotateZ(axisTilt);
            pg.rotateY(rotationAngle);

            // Calcula o fator de escala dinâmico:
            // O tamanho base (radius) é ajustado com os fatores globalScale e planetAmplification
            float baseSize = selected ? radius * 1.1f : radius;
            float scaleFactor = baseSize * globalScale * planetAmplification;

            // Configura o shape: se for o Sol, usa a textura; para os demais, usa a cor
            if (name.equals("Sol")) {
                shape = pApplet.createShape(PConstants.SPHERE, 1);
                shape.setStroke(false);
                if (sunTexture != null) {
                    shape.setTexture(sunTexture);
                }
            } else {
                shape = pApplet.createShape(PConstants.SPHERE, 1);
                shape.setFill(true);
                shape.setStroke(false);
                shape.setFill(col);
            }

            pg.scale(scaleFactor);
            pg.shape(shape);
        pg.popMatrix();

        if (showLabel) {
            pg.pushMatrix();
                PVector labelPos = getDrawPosition();
                labelPos.y -= (radius + 5);
                pg.translate(labelPos.x, labelPos.y, labelPos.z);
                float labelSize = pApplet.max(10, radius * 0.5f);
                pg.fill(255);
                pg.textSize(labelSize);
                pg.textAlign(PConstants.CENTER, PConstants.BOTTOM);
                pg.text(name, 0, 0);
            pg.popMatrix();
        }
        
        pg.resetShader();
    }

    private void buildSaturnRingsShape() {
        saturnRingsShape = pApplet.createShape();
        saturnRingsShape.beginShape(PConstants.QUAD_STRIP);
        saturnRingsShape.noStroke();

        // Proporções realistas em relação ao raio de Saturno
        float innerRingRatio = 1.15f;
        float outerRingRatio = 2.35f;
        int ringBands = 60;
        int ringSegments = 90;
        float step = (outerRingRatio - innerRingRatio) / ringBands;

        // Gaps conhecidos em proporção ao raio
        float[] gapCenters = {
            117500f / 60000f,  // Cassini (~1.96)
            133600f / 60000f,  // Encke (~2.23)
            136500f / 60000f   // Keeler (~2.275)
        };
        float[] gapWidths = {
            4800f / 60000f,    // Cassini
            325f / 60000f,     // Encke
            42f / 60000f       // Keeler
        };

        for (int i = 0; i < ringBands; i++) {
            float r1Ratio = innerRingRatio + i * step;
            float r2Ratio = r1Ratio + step;

            // Verifica se este trecho está dentro de um gap
            boolean insideGap = false;
            for (int g = 0; g < gapCenters.length; g++) {
                float gapStart = gapCenters[g] - gapWidths[g] / 2f;
                float gapEnd   = gapCenters[g] + gapWidths[g] / 2f;
                if (r1Ratio >= gapStart && r2Ratio <= gapEnd) {
                    insideGap = true;
                    break;
                }
            }
            if (insideGap) continue;

            float r1 = radius * r1Ratio;
            float r2 = radius * r2Ratio;

            // Calcula variação de opacidade suave
            float wave = PApplet.sin(i * 0.3f);
            float alphaFactor = 0.6f + 0.4f * wave;
            int alpha = (int)(alphaFactor * 200);

            // Gera uma cor branca com variação leve
            int base = (int)PApplet.lerp(240, 255, PApplet.sin(i * 0.2f) * 0.5f + 0.5f);
            saturnRingsShape.fill(base, base, base, alpha);

            for (int j = 0; j <= ringSegments; j++) {
                float angle = PApplet.TWO_PI * j / ringSegments;
                float cosA = PApplet.cos(angle);
                float sinA = PApplet.sin(angle);
                saturnRingsShape.vertex(r1 * cosA, 0, r1 * sinA);
                saturnRingsShape.vertex(r2 * cosA, 0, r2 * sinA);
            }
        }
        saturnRingsShape.endShape();
    }

    public void setRotationSpeed(float speed) {
        rotationSpeed = speed;
    }

    public float getDrawnRadius() {
        return radius;
    }

    public void dispose() {
        // Libera a forma associada, se existir
        if (shape != null) {
            shape = null;
        }
        // Libera e limpa todas as luas associadas
        if (moons != null) {
            for (Moon m : moons) {
                m.dispose();
            }
            moons.clear();
            moons = null;
        }
    }
}
