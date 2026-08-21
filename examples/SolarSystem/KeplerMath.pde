// KeplerMath.pde
// ———————————————————————————————————————————————————————————————————————————————
// Helpers de rotação, solver Kepler e projeção do plano orbital
// ———————————————————————————————————————————————————————————————————————————————

final int    KEPLER_MAX_ITER = 50;
final double KEPLER_EPS      = 1e-13;
final double TWO_PI_DOUBLE   = Math.PI * 2.0;

// ———————————————————————————————————————————————————————————————————————————————
// Resolve M = E - e·sin(E) para E via Newton–Raphson.
// ———————————————————————————————————————————————————————————————————————————————
double normalizeRadians(double angle) {
  double normalized = angle % TWO_PI_DOUBLE;
  return normalized < 0.0 ? normalized + TWO_PI_DOUBLE : normalized;
}

double solveKeplerEquation(double M, double e) {
  // Reduzir o argumento evita perda de precisão trigonométrica após longos períodos.
  double normalizedM = Math.IEEEremainder(M, TWO_PI_DOUBLE);
  double E = e < 0.8 ? normalizedM : Math.copySign(Math.PI, normalizedM);
  for (int i = 0; i < KEPLER_MAX_ITER; i++) {
    double f = E - e * Math.sin(E) - normalizedM;
    double df = 1.0 - e * Math.cos(E);
    double correction = f / df;
    E -= correction;
    if (Math.abs(correction) <= KEPLER_EPS) {
      break;
    }
  }
  return E;
}

// ———————————————————————————————————————————————————————————————————————————————
// Roda um vetor em torno do eixo X (não afeta PGraphics).
// ———————————————————————————————————————————————————————————————————————————————
PVector rotateVecX(PVector v, float θ) {
  float c = cos(θ), s = sin(θ);
  return new PVector(
    v.x,
    c * v.y - s * v.z,
    s * v.y + c * v.z
  );
}

// ———————————————————————————————————————————————————————————————————————————————
// Roda um vetor em torno do eixo Y (não afeta PGraphics).
// ———————————————————————————————————————————————————————————————————————————————
PVector rotateVecY(PVector v, float θ) {
  float c = cos(θ), s = sin(θ);
  return new PVector(
    c * v.x + s * v.z,
    v.y,
    -s * v.x + c * v.z
  );
}

// ———————————————————————————————————————————————————————————————————————————————
// Roda um vetor em torno do eixo Z (não afeta PGraphics).
// ———————————————————————————————————————————————————————————————————————————————
PVector rotateVecZ(PVector v, float θ) {
  float c = cos(θ), s = sin(θ);
  return new PVector(
    c * v.x - s * v.y,
    s * v.x + c * v.y,
    v.z
  );
}

/**
 * Igual ao initialState “padrão”, mas usando μ = G_DAY * massFocus,
 * para verse-los não apenas em torno do Sol, mas de qualquer corpo.
 *
 * @param a          semi-eixo maior (AU)
 * @param e          excentricidade
 * @param M0         anomalia média inicial (rad)
 * @param massFocus  massa do foco em M☉
 * @param rOrb       saída: posição no plano orbital XZ (AU)
 * @param vOrb       saída: velocidade no plano orbital XZ (AU/dia)
 */
void initialState(double a,
                  double e,
                  double M0,
                  double massFocus,
                  PVector rOrb,
                  PVector vOrb) {
  double[] precisePosition = new double[3];
  double[] preciseVelocity = new double[3];
  initialState(a, e, M0, massFocus, precisePosition, preciseVelocity);
  rOrb.set((float) precisePosition[0], 0f, (float) precisePosition[2]);
  vOrb.set((float) preciseVelocity[0], 0f, (float) preciseVelocity[2]);
}

void initialState(double a,
                  double e,
                  double M0,
                  double massFocus,
                  double[] rOrb,
                  double[] vOrb) {
  double E = solveKeplerEquation(M0, e);
  double cosE = Math.cos(E);
  double sinE = Math.sin(E);
  double oneMinusESquared = 1.0 - e * e;

  rOrb[0] = a * (cosE - e);
  rOrb[1] = 0.0;
  rOrb[2] = a * Math.sqrt(oneMinusESquared) * sinE;

  double mu = G_DAY * massFocus;
  double rMag = a * (1.0 - e * cosE);
  double sqrtMuA = Math.sqrt(mu * a);
  vOrb[0] = -sinE * sqrtMuA / rMag;
  vOrb[1] = 0.0;
  vOrb[2] = cosE * sqrtMuA * Math.sqrt(oneMinusESquared) / rMag;
}

// ———————————————————————————————————————————————————————————————————————————————
// Converte um vetor no plano orbital XZ (Y=0) → referencial global (Y-up),
// aplicando rotações na ordem: RY(Ω) → RX(i) → RY(ω).
// ———————————————————————————————————————————————————————————————————————————————
PVector applyOrbitalPlaneToGlobal(PVector vPlane,
                                  float Ω,
                                  float iRad,
                                  float ω) {
  PVector v1 = rotateVecY(vPlane, Ω);
  PVector v2 = rotateVecX(v1, iRad);
  return rotateVecY(v2, ω);
}

void applyOrbitalPlaneToGlobal(double[] source,
                               double ascendingNode,
                               double inclination,
                               double periapsis,
                               double[] destination) {
  double cosNode = Math.cos(ascendingNode);
  double sinNode = Math.sin(ascendingNode);
  double x1 = cosNode * source[0] + sinNode * source[2];
  double y1 = source[1];
  double z1 = -sinNode * source[0] + cosNode * source[2];

  double cosInclination = Math.cos(inclination);
  double sinInclination = Math.sin(inclination);
  double x2 = x1;
  double y2 = cosInclination * y1 - sinInclination * z1;
  double z2 = sinInclination * y1 + cosInclination * z1;

  double cosPeriapsis = Math.cos(periapsis);
  double sinPeriapsis = Math.sin(periapsis);
  destination[0] = cosPeriapsis * x2 + sinPeriapsis * z2;
  destination[1] = y2;
  destination[2] = -sinPeriapsis * x2 + cosPeriapsis * z2;
}

// ———————————————————————————————————————————————————————————————————————————————
// Solver Kepleriano: dado foco, elementos e Δt em dias, atualiza pos e vel.
// ———————————————————————————————————————————————————————————————————————————————
void keplerSolve(PVector focusPos,
                 PVector pos,
                 PVector vel,
                 double periAU,
                 double apheAU,
                 double e,
                 double incRad,
                 double raanRad,
                 double argPerRad,
                 double meanAnomaly0,
                 double dtDays,
                 double massFocus,
                 double[] rOrb,
                 double[] vOrb,
                 double[] rEcl,
                 double[] vEcl) {

  // 1) parâmetros
  double a = 0.5 * (periAU + apheAU);
  double mu = G_DAY * massFocus;
  double n = Math.sqrt(mu / (a * a * a));

  // 2) anomalia média no tempo t = M0 + n · dt
  double M = meanAnomaly0 + n * dtDays;
  double E = solveKeplerEquation(M, e);

  // 3) coordenadas no plano orbital XZ
  double cosE = Math.cos(E);
  double sinE = Math.sin(E);
  double oneMinusESquared = 1.0 - e * e;
  double xOrb = a * (cosE - e);
  double zOrb = a * Math.sqrt(oneMinusESquared) * sinE;
  double r = a * (1.0 - e * cosE);

  // 4) velocidades no plano XZ
  double vxOrb = -sinE * Math.sqrt(mu * a) / r;
  double vzOrb = cosE * Math.sqrt(mu * a * oneMinusESquared) / r;

  rOrb[0] = xOrb;
  rOrb[1] = 0.0;
  rOrb[2] = zOrb;
  vOrb[0] = vxOrb;
  vOrb[1] = 0.0;
  vOrb[2] = vzOrb;

  // 5) rotaciona para o referencial global e soma ao foco
  applyOrbitalPlaneToGlobal(rOrb, raanRad, incRad, argPerRad, rEcl);
  applyOrbitalPlaneToGlobal(vOrb, raanRad, incRad, argPerRad, vEcl);

  pos.set(
    (float) (focusPos.x + rEcl[0]),
    (float) (focusPos.y + rEcl[1]),
    (float) (focusPos.z + rEcl[2])
  );
  vel.set((float) vEcl[0], (float) vEcl[1], (float) vEcl[2]);
}
