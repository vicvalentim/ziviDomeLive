/**
 * Quaternion minimal para rotações 3D e SLERP.
 */
class Quaternion {
  float w, x, y, z;

  Quaternion(float w_, float x_, float y_, float z_) {
    w = w_; x = x_; y = y_; z = z_;
  }

  // constrói a partir de eixo (unitário) e ângulo (rad)
  Quaternion fromAxisAngle(PVector axis, float angle) {
    float half = angle * 0.5f;
    float s    = sin(half);
    return new Quaternion(
      cos(half),
      axis.x * s,
      axis.y * s,
      axis.z * s
    ).normalize();
  }

  // normaliza
  Quaternion normalize() {
    float m = sqrt(w*w + x*x + y*y + z*z);
    w /= m; x /= m; y /= m; z /= m;
    return this;
  }

  // compõe rotações
  Quaternion multiply(Quaternion q) {
    return new Quaternion(
      w*q.w - x*q.x - y*q.y - z*q.z,
      w*q.x + x*q.w + y*q.z - z*q.y,
      w*q.y - x*q.z + y*q.w + z*q.x,
      w*q.z + x*q.y - y*q.x + z*q.w
    );
  }

  // converte para matriz col-major 4×4
  float[] toMatrix() {
    float[] m = new float[16];
    m[0]  = 1 - 2*(y*y + z*z);
    m[1]  =   2*(x*y + z*w);
    m[2]  =   2*(x*z - y*w);
    m[3]  = 0;

    m[4]  =   2*(x*y - z*w);
    m[5]  = 1 - 2*(x*x + z*z);
    m[6]  =   2*(y*z + x*w);
    m[7]  = 0;

    m[8]  =   2*(x*z + y*w);
    m[9]  =   2*(y*z - x*w);
    m[10] = 1 - 2*(x*x + y*y);
    m[11] = 0;

    m[12] = m[13] = m[14] = 0;
    m[15] = 1;
    return m;
  }

  // SLERP entre this e q2
  Quaternion slerp(Quaternion q2, float t) {
    float dot = w*q2.w + x*q2.x + y*q2.y + z*q2.z;
    dot = constrain(dot, -1, 1);
    float theta = acos(dot);
    if (theta < 1e-6) return this;
    float sinT = sin(theta);
    float w1 = sin((1 - t) * theta) / sinT;
    float w2 = sin(t * theta)         / sinT;
    return new Quaternion(
      w1*w + w2*q2.w,
      w1*x + w2*q2.x,
      w1*y + w2*q2.y,
      w1*z + w2*q2.z
    ).normalize();
  }
}


/**
 * Controlador de câmera com quaternion para rotações estáveis
 * e interpolação suave entre posições e orientações.
 */
class CameraController {
  PVector target;            // ponto que a câmera olha
  float   distance;          // distância ao target
  Quaternion orientation;    // orientação atual

  PVector    goalTarget;
  Quaternion goalOrientation;
  float      goalDistance;
  float      lerpFactor = 0.1f;  // suavização

  CameraController(PVector initialTarget, float initialDistance) {
    target          = initialTarget.copy();
    goalTarget      = initialTarget.copy();
    distance        = initialDistance;
    goalDistance    = initialDistance;
    orientation     = new Quaternion(1, 0, 0, 0);
    goalOrientation = orientation;
  }

  // aplica no PGraphicsOpenGL
  void apply(PGraphicsOpenGL pg) {
    pg.translate(0, 0, -distance);
    float[] M = orientation.toMatrix();
    pg.applyMatrix(
      M[0],  M[4],  M[8],  M[12],
      M[1],  M[5],  M[9],  M[13],
      M[2],  M[6],  M[10], M[14],
      M[3],  M[7],  M[11], M[15]
    );
    pg.translate(-target.x, -target.y, -target.z);
  }

  // atualiza cada frame (SLERP/LERP)
  void update() {
    orientation = orientation.slerp(goalOrientation, lerpFactor);
    target      = PVector.lerp(target, goalTarget, lerpFactor);
    distance    = lerp(distance, goalDistance, lerpFactor);
  }

  // define nova meta (alvo, orientação, distância)
  void goTo(PVector t, Quaternion o, float d) {
    goalTarget      = t.copy();
    goalOrientation = o.normalize();
    goalDistance    = d;
  }

  // rotaciona em torno de um eixo do mundo
  void rotateAround(PVector axis, float angle) {
    Quaternion delta = new Quaternion(1,0,0,0).fromAxisAngle(axis, angle);
    orientation = delta.multiply(orientation).normalize();
    goalOrientation = orientation;
  }

  // setters diretos (sem suavização)
  void setTarget(PVector t)    { target = goalTarget = t.copy(); }
  void setOrientation(Quaternion q) {
    orientation = goalOrientation = q.normalize();
  }
  void setDistance(float d)    { distance = goalDistance = d; }

  // getters
  PVector    getTarget()      { return target.copy(); }
  float      getDistance()    { return distance; }
  Quaternion getOrientation() { return orientation; }

  // utilitário lerp
  float lerp(float a, float b, float f) {
    return a + (b - a) * f;
  }
}
