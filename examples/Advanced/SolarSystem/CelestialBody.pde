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
  double  getMassSolar();

  /** Corpo-foco (Sol → planetas, planeta → luas). */
  CelestialBody getCentralBody();
  void          setCentralBody(CelestialBody c);

  /** Propaga a órbita por <code>dtDays</code> usando o solver Kepleriano. */
  void propagateKepler(double dtDays);

  // ───────────────────────────── Elementos orbitais ─────────────────────────
  /** Semi-eixo maior <i>a</i> em AU. */                         // ★ novo
  double getSemiMajorAxisAU();

  /** Distância de periélio (q = a·(1-e)) em AU. */
  double getPerihelionAU();

  /** Distância de afélio (Q = a·(1+e)) em AU. */
  double getAphelionAU();

  /** Excentricidade e. */
  double getEccentricity();

  /** Inclinação orbital <i>i</i> (rad).  */
  double getOrbitInclinationRad();

  /** Longitude do nó ascendente Ω (rad). */                       // ★ novo
  double getLongitudeAscendingNodeRad();

  /** Argumento do periastro ω (rad). */
  double getArgumentOfPeriapsisRad();

  /** Anomalia média <i>M</i> no epoch J2000 (rad). */             // ★ novo
  double getMeanAnomalyRad();

  // ───────────────────────────── Auxiliares de render ───────────────────────
  /** Raio físico (AU). */
  double getRadiusAU();

  /** Período de rotação sideral (dias). */
  double getRotationPeriodDays();
}
