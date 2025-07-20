// KeplerMath.pde
// ———————————————————————————————————————————————————————————————————————————————
// Helpers de rotação, solver Kepler e projeção do plano orbital
// ———————————————————————————————————————————————————————————————————————————————

final int   KEPLER_MAX_ITER = 50;
final float KEPLER_EPS      = 1e-6f;

// ———————————————————————————————————————————————————————————————————————————————
// Resolve M = E - e·sin(E) para E via Newton–Raphson.
// ———————————————————————————————————————————————————————————————————————————————
float solveKeplerEquation(float M, float e) {
  float E = M;
  for (int i = 0; i < KEPLER_MAX_ITER; i++) {
    float f  = E - e * cos(E) * 0 - e * sin(E) * 0; // This line is incorrect; correct is below
    // Correction:
    f = E - e * sin(E) - M;
    float df = 1 - e * cos(E);
    E -= f / df;
    if (abs(f) < KEPLER_EPS) {
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
void initialState(float a,
                  float e,
                  float M0,
                  float massFocus,
                  PVector rOrb,
                  PVector vOrb) {
  // 1) resolve E
  float E    = solveKeplerEquation(M0, e);
  float cosE = cos(E), sinE = sin(E);

  // 2) posição no plano XZ
  float x = a * (cosE - e);
  float z = a * sqrt(1 - e*e) * sinE;
  rOrb.set(x, 0, z);

  // 3) velocidade no plano XZ, usando μ = G_DAY * massFocus
  float mu        = G_DAY * massFocus;
  float rMag      = a * (1 - e * cosE);
  float sqrt_mu_a = sqrt(mu * a);
  float vx = - sinE * sqrt_mu_a / rMag;
  float vz =   cosE * sqrt_mu_a * sqrt(1 - e*e) / rMag;
  vOrb.set(vx, 0, vz);
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

// ———————————————————————————————————————————————————————————————————————————————
// Solver Kepleriano: dado foco, elementos e Δt em dias, atualiza pos e vel.
// ———————————————————————————————————————————————————————————————————————————————
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

  // 1) parâmetros
  float a  = 0.5f * (periAU + apheAU);
  float μ  = G_DAY * massFocus;
  float n  = sqrt(μ / (a * a * a));

  // 2) anomalia média no tempo t = M0 + n · dt
  float M = meanAnomaly0 + n * dtDays;
  float E = solveKeplerEquation(M, e);

  // 3) coordenadas no plano orbital XZ
  float cosE = cos(E), sinE = sin(E);
  float xOrb = a * (cosE - e);
  float zOrb = a * sqrt(1 - e * e) * sinE;
  float r    = a * (1 - e * cosE);

  // 4) velocidades no plano XZ
  float vxOrb = -sinE * sqrt(μ * a) / r;
  float vzOrb =  cosE * sqrt(μ * a * (1 - e * e)) / r;

  PVector rOrb = new PVector(xOrb, 0, zOrb);
  PVector vOrb = new PVector(vxOrb, 0, vzOrb);

  // 5) rotaciona para o referencial global e soma ao foco
  PVector rEcl = applyOrbitalPlaneToGlobal(rOrb,   raanRad, incRad, argPerRad);
  PVector vEcl = applyOrbitalPlaneToGlobal(vOrb,   raanRad, incRad, argPerRad);

  pos.set(PVector.add(focusPos, rEcl));
  vel.set(vEcl);
}
