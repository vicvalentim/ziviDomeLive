import processing.core.PVector;

/**
 * Interface comum a todos os corpos celestes do sistema:
 * Sun, Planet e Moon.
 */
public interface CelestialBody {
    // ——— Unidades físicas ———

    /** Posição no espaço, em AU. */
    PVector getPositionAU();

    /** Velocidade em AU por dia. */
    PVector getVelocityAU();

    /** Massa, em massas solares (M☉). */
    float getMassSolar();

    /** Retorna o corpo-foco (Sol para planetas; planeta para luas). */
    CelestialBody getCentralBody();

    /** Propaga a órbita por dtDays via solver Kepleriano. */
    void propagateKepler(float dtDays);

    // ——— Elementos orbitais ———

    /** Distância do periélio, em AU. */
    float getPerihelionAU();

    /** Distância do afélio, em AU. */
    float getAphelionAU();

    /** Excentricidade da órbita. */
    float getEccentricity();

    /** Inclinação orbital, em radianos. */
    float getOrbitInclinationRad();

    /** Argumento do periastro, em radianos. */
    float getArgumentOfPeriapsisRad();


    // ——— Métodos auxiliares (opcionais) ———

    /** Raio físico, em AU (para render). */
    float getRadiusAU();

    /** Período de rotação em dias (para animação). */
    float getRotationPeriodDays();
}
