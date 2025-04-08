// -------------------------------------------------
// Classe Moon com atualização incremental de ângulo
// -------------------------------------------------
public class Moon {
    private PApplet pApplet;
    private final float pixelsPerAU;
    private final float G_AU;
    private final float moonOrbitCalibration;
    
    float mass;
    float moonSizeRatio;
    float moonOrbitFactor;
    PVector position;
    PVector velocity;
    PVector acceleration;
    float orbitAngle = 0;
    color col;
    String name;
    Planet parent;

    // Parâmetros orbitais
    float inclination;
    float eccentricity;
    float argumentPeriapsis;
    boolean alignWithPlanetAxis;

    // Matriz de rotação pré-calculada
    PMatrix3D rotationMatrix;

    // Variáveis para atualização incremental do ângulo orbital
    float orbitalAngle;
    float cosOrbital, sinOrbital;

    // PShape para renderização
    PShape shape;

    // Construtor atualizado com 16 parâmetros
    public Moon(PApplet pApplet,
                float mass,
                float moonSizeRatio,
                float moonOrbitFactor,
                PVector pos,
                PVector vel,
                color col,
                String name,
                Planet parent,
                float inclination,
                float eccentricity,
                float argumentPeriapsis,
                boolean alignWithPlanetAxis,
                float pixelsPerAU,
                float G_AU,
                float moonOrbitCalibration) {
        // Inicializa a referência do PApplet e os parâmetros recebidos
        this.pApplet = pApplet;
        this.pixelsPerAU = pixelsPerAU;
        this.G_AU = G_AU;
        this.moonOrbitCalibration = moonOrbitCalibration;
        
        // Inicialização dos demais atributos
        this.mass = mass;
        this.moonSizeRatio = moonSizeRatio;
        this.moonOrbitFactor = moonOrbitFactor;
        this.position = pos.copy();
        this.velocity = vel.copy();
        this.acceleration = new PVector();
        this.col = col;
        this.name = name;
        this.parent = parent;
        this.inclination = inclination;
        this.eccentricity = eccentricity;
        this.argumentPeriapsis = argumentPeriapsis;
        this.alignWithPlanetAxis = alignWithPlanetAxis;
      
        // Configura a matriz de rotação conforme o alinhamento desejado
        rotationMatrix = new PMatrix3D();
        if (alignWithPlanetAxis) {
            rotationMatrix.rotate(parent.axisTilt, 0, 0, 1);
        } else {
            rotationMatrix.rotate(argumentPeriapsis, 0, 1, 0);
            rotationMatrix.rotate(inclination, 1, 0, 0);
        }
      
        // Inicializa os valores para a atualização incremental do ângulo orbital
        orbitalAngle = PApplet.atan2(position.z, position.x);
        cosOrbital = PApplet.cos(orbitalAngle);
        sinOrbital = PApplet.sin(orbitalAngle);
      
        // Cria o shape usando a referência do PApplet
        shape = pApplet.createShape(PConstants.SPHERE, 1);
        shape.setFill(true);
        shape.setStroke(false);
        shape.setFill(col);
    }

    public void update(float dt, PVector parentPos, PVector parentVel) {
        // Declaração local da variável temporária
        PVector tempVec1 = new PVector();

        float r_AU_physical = (parent.radius * moonOrbitFactor) / pixelsPerAU;
        float v_AU_per_day = PApplet.sqrt(G_AU * parent.mass / r_AU_physical);
        float v_pixels_per_day = v_AU_per_day * pixelsPerAU;

        float r_px_visual = parent.radius * (1 + (moonOrbitFactor / moonOrbitCalibration));

        float deltaAngle = (v_pixels_per_day / r_px_visual) * dt;

        float cosDelta = PApplet.cos(deltaAngle);
        float sinDelta = PApplet.sin(deltaAngle);
        float newCos = cosOrbital * cosDelta - sinOrbital * sinDelta;
        float newSin = sinOrbital * cosDelta + cosOrbital * sinDelta;
        cosOrbital = newCos;
        sinOrbital = newSin;
        orbitalAngle += deltaAngle;

        float newX = r_px_visual * cosOrbital;
        float newZ = r_px_visual * sinOrbital;
      
        // Atualiza a posição usando o vetor temporário
        tempVec1.set(newX, 0, newZ);
        rotationMatrix.mult(tempVec1, tempVec1);
        position.set(tempVec1);

        // Atualiza a velocidade utilizando o mesmo vetor temporário
        tempVec1.set(-cosOrbital, 0, sinOrbital);
        rotationMatrix.mult(tempVec1, tempVec1);
        tempVec1.normalize();
        tempVec1.mult(v_pixels_per_day);
        velocity.set(tempVec1);
    }

    public float getDrawnRadius() {
        return parent.radius * moonSizeRatio;
    }

    public PVector getDrawPosition() {
        return PVector.add(parent.getDrawPosition(), position);
    }

    public void displayOrbit(PGraphicsOpenGL pg) {
        pg.pushMatrix();
            PVector parentDraw = parent.getDrawPosition();
            pg.translate(parentDraw.x, parentDraw.y, parentDraw.z);
            pg.rotateX(PI / 2);
            float orbitRadius = parent.radius * (1 + (moonOrbitFactor / moonOrbitCalibration));
            pg.noFill();
            pg.stroke(150, 150, 255, 150);
            pg.strokeWeight(1);
            pg.ellipse(0, 0, orbitRadius * 2, orbitRadius * 2);
        pg.popMatrix();
    }

    public void display(PGraphicsOpenGL pg, boolean showLabel) {
        PVector d = getDrawPosition();
        pg.pushMatrix();
            pg.translate(d.x, d.y, d.z);
            pg.scale(getDrawnRadius());
            pg.shape(shape);

            if (showLabel) {
                float labelSize = PApplet.max(10, getDrawnRadius() * 0.5f);
                pg.fill(255);
                pg.textSize(labelSize);
                pg.textAlign(PConstants.CENTER, PConstants.BOTTOM);
                pg.text(name, 0, -getDrawnRadius() - 5);
            }
        pg.popMatrix();
        pg.resetShader();
    }

    public void dispose() {
    // Libera a forma associada, se existir
    if (shape != null) {
        shape = null;
    }
    // Outras referências podem ser definidas como null se necessário
    }
}
