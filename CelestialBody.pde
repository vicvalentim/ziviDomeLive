/**
 * Corpo celeste genérico (Sol, planeta ou lua) – unidades:
 *  • posição-velocidade: AU  |  • tempo: dias  |  • massa: M☉
 */
public interface CelestialBody {

  // ───────────────────────────── Estado dinâmico ────────────────────────────
  /** Posição heliocêntrica (ou planetocêntrica, para luas) em AU. */
  PVector getPositionAU();

  /** Velocidade em AU/dia. */
  PVector getVelocityAU();

  /** Massa em massas solares (M☉). */
  float   getMassSolar();

  /** Corpo-foco (Sol → planetas, planeta → luas). */
  CelestialBody getCentralBody();
  void          setCentralBody(CelestialBody c);

  /** Propaga a órbita por <code>dtDays</code> usando o solver Kepleriano. */
  void propagateKepler(float dtDays);

  // ───────────────────────────── Elementos orbitais ─────────────────────────
  /** Semi-eixo maior <i>a</i> em AU. */                         // ★ novo
  float getSemiMajorAxisAU();

  /** Distância de periélio (q = a·(1-e)) em AU. */
  float getPerihelionAU();

  /** Distância de afélio (Q = a·(1+e)) em AU. */
  float getAphelionAU();

  /** Excentricidade e. */
  float getEccentricity();

  /** Inclinação orbital <i>i</i> (rad).  */
  float getOrbitInclinationRad();

  /** Longitude do nó ascendente Ω (rad). */                       // ★ novo
  float getLongitudeAscendingNodeRad();

  /** Argumento do periastro ω (rad). */
  float getArgumentOfPeriapsisRad();

  /** Anomalia média <i>M</i> no epoch J2000 (rad). */             // ★ novo
  float getMeanAnomalyRad();

  // ───────────────────────────── Auxiliares de render ───────────────────────
  /** Raio físico (AU). */
  float getRadiusAU();

  /** Período de rotação sideral (dias). */
  float getRotationPeriodDays();
}
