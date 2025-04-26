// ———————————————————————————————————————————————————————————————————————————————
// KeplerMath.pde
// Helpers de rotação, solver Kepler e projeção do plano orbital
// ———————————————————————————————————————————————————————————————————————————————

final int   KEPLER_MAX_ITER = 50;
final float KEPLER_EPS      = 1e-6f;

/**
 * Resolve M = E - e·sin(E) para E via Newton–Raphson.
 */
float solveKeplerEquation(float M, float e) {
  float E = M;
  for (int i = 0; i < KEPLER_MAX_ITER; i++) {
    float f  = E - e * sin(E) - M;
    float df = 1 - e * cos(E);
    E -= f/df;
    if (abs(f) < KEPLER_EPS) break;
  }
  return E;
}

/** Roda um vetor em torno do eixo X (não afeta PGraphics). */
PVector rotateVecX(PVector v, float θ) {
  float c = cos(θ), s = sin(θ);
  return new PVector(v.x, c*v.y - s*v.z, s*v.y + c*v.z);
}

/** Roda um vetor em torno do eixo Y (não afeta PGraphics). */
PVector rotateVecY(PVector v, float θ) {
  float c = cos(θ), s = sin(θ);
  return new PVector(c*v.x + s*v.z, v.y, -s*v.x + c*v.z);
}

/** Roda um vetor em torno do eixo Z (não afeta PGraphics). */
PVector rotateVecZ(PVector v, float θ) {
  float c = cos(θ), s = sin(θ);
  return new PVector(c*v.x - s*v.y, s*v.x + c*v.y, v.z);
}

/**
 * Converte um vetor no plano orbital XZ (Y=0) → referencial global (Y-up),
 * aplicando rotações na ordem: RY(Ω) → RX(i) → RY(ω).
 */
PVector applyOrbitalPlaneToGlobal(PVector vPlane,
                                  float Ω,
                                  float iRad,
                                  float ω) {
  PVector v1 = rotateVecY(vPlane, Ω);
  PVector v2 = rotateVecX(v1, iRad);
  return rotateVecY(v2, ω);
}

/**
 * Solver Kepleriano: dado foco, elementos e Δt em dias, atualiza pos e vel.
 */
void keplerSolve(PVector focusPos,
                 PVector pos,
                 PVector vel,
                 float periAU,
                 float apheAU,
                 float e,
                 float incRad,
                 float raanRad,
                 float argPerRad,
                 float meanAnomaly0,
                 float dtDays,
                 float massFocus) {

  float a  = 0.5f * (periAU + apheAU);
  float μ  = G_DAY * massFocus;
  float n  = sqrt(μ/(a*a*a));
  float M  = meanAnomaly0 + n * dtDays;
  float E  = solveKeplerEquation(M, e);

  float cosE = cos(E), sinE = sin(E);
  float xOrb = a * (cosE - e);
  float zOrb = a * sqrt(1 - e*e) * sinE;
  float r    = a * (1 - e*cosE);

  float vxOrb = -sinE * sqrt(μ * a) / r;
  float vzOrb =  cosE * sqrt(μ * a * (1 - e*e)) / r;

  PVector rOrb = new PVector(xOrb, 0, zOrb);
  PVector vOrb = new PVector(vxOrb,0, vzOrb);

  PVector rEcl = applyOrbitalPlaneToGlobal(rOrb,   raanRad, incRad, argPerRad);
  PVector vEcl = applyOrbitalPlaneToGlobal(vOrb,   raanRad, incRad, argPerRad);

  pos.set(PVector.add(focusPos, rEcl));
  vel.set(vEcl);
}
