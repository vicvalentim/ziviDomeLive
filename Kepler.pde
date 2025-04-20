// ———————————————————————————————————————————————————————————————————————————————
// Kepler — solver Kepleriano para propagar órbitas elípticas em AU e dias
// ———————————————————————————————————————————————————————————————————————————————
final int   KEPLER_MAX_ITER = 50;
final float KEPLER_EPS      = 1e-6f;
// G em AU³ / (M☉·dia²):
final float G_DAY          = 2.9591220828559093e-4f;

/**
 * Avança posição e velocidade em dtDays (dias), em torno de focusPos,
 * agora respeitando o plano da eclíptica (XZ) e a inclinação real.
 */
void keplerSolve(PVector focusPos,
                 PVector pos,
                 PVector vel,
                 float periAU, float apheAU, float e,
                 float incRad, float argPerRad,
                 float dtDays,
                 float massFocus) {

  // 1) semi‑eixo e parâmetro gravitacional
  float a  = (periAU + apheAU) * 0.5f;
  float mu = G_DAY * massFocus;
  float n  = sqrt(mu / (a * a * a));  // rad/dia

  // 2) traz para o plano orbital XY:
  //    inverte as rotações de draw: planoXZ(PI/2), argPer, inc
  PVector r0 = PVector.sub(pos, focusPos);
  PVector op = rotateX(
                 rotateZ(
                   rotateX(r0, -incRad),
                   -argPerRad),
                 -PConstants.HALF_PI);

  // 3) anomalia verdadeira inicial
  float f0 = atan2(op.y, op.x);

  // 4) anomalia excêntrica inicial (E₀) – exata
  float E0 = 2 * atan( sqrt((1 - e)/(1 + e)) * tan(f0 * 0.5f) );

  // 5) anomalia média inicial (M₀)
  float M0 = E0 - e * sin(E0);

  // 6) avança M
  float M  = M0 + n * dtDays;

  // 7) Newton–Raphson para achar E em M = E – e·sin E
  float E = M;
  for (int i = 0; i < KEPLER_MAX_ITER; i++) {
    float F  = E - e * sin(E) - M;
    float dF = 1 - e * cos(E);
    float dE = -F / dF;
    E += dE;
    if (abs(dE) < KEPLER_EPS) break;
  }

  // 8) posição e velocidade no plano orbital
  float cosE = cos(E), sinE = sin(E);
  float rNew = a * (1 - e * cosE);
  float xOp  = a * (cosE - e);
  float yOp  = a * sqrt(1 - e*e) * sinE;
  float factor = sqrt(mu / a) / rNew;
  float vxOp   = -factor * sinE;
  float vyOp   =  factor * sqrt(1 - e*e) * cosE;

  // 9) volta ao sistema 3D: aplica rotações de draw
  PVector newPos = rotateX(
                     rotateZ(
                       rotateX(new PVector(xOp, yOp, 0),
                               PConstants.HALF_PI),
                       argPerRad),
                     incRad)
                   .add(focusPos);

  PVector newVel = rotateX(
                     rotateZ(
                       rotateX(new PVector(vxOp, vyOp, 0),
                               PConstants.HALF_PI),
                       argPerRad),
                     incRad);

  pos.set(newPos);
  vel.set(newVel);
}

// ——————————————— Helpers de rotação ———————————————
PVector rotateX(PVector v, float angle) {
  float ca = cos(angle), sa = sin(angle);
  return new PVector(
    v.x,
    ca * v.y - sa * v.z,
    sa * v.y + ca * v.z
  );
}

PVector rotateY(PVector v, float angle) {
  float ca = cos(angle), sa = sin(angle);
  return new PVector(
    ca * v.x + sa * v.z,
    v.y,
    -sa * v.x + ca * v.z
  );
}

PVector rotateZ(PVector v, float angle) {
  float ca = cos(angle), sa = sin(angle);
  return new PVector(
    ca * v.x - sa * v.y,
    sa * v.x + ca * v.y,
    v.z
  );
}
