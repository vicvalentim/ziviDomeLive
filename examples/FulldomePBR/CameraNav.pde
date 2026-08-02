// Scene-space camera navigation for the FulldomePBR example.
//
// This mirrors the approach used in the SolarSystem example: the camera lives
// inside the SCENE (it transforms the PGraphics modelview), so it works across
// every ziviDomeLive projection (fisheye, equirectangular, cubemap, standard)
// without ever touching the dome parameters articulated by the ControlManager
// (yaw / pitch / roll / fov). Rotations use quaternions for stable, gimbal-lock
// free orbiting, and every target/orientation/distance change is smoothly
// interpolated (LERP/SLERP) for fluid motion through space.

/**
 * Minimal quaternion for 3D rotations and SLERP.
 */
class SpaceQuat {
  float w, x, y, z;

  SpaceQuat(float w_, float x_, float y_, float z_) {
    w = w_; x = x_; y = y_; z = z_;
  }

  // Build from a (unit) axis and angle in radians.
  SpaceQuat fromAxisAngle(PVector axis, float angle) {
    float half = angle * 0.5f;
    float s = sin(half);
    return new SpaceQuat(cos(half), axis.x * s, axis.y * s, axis.z * s).normalize();
  }

  SpaceQuat normalize() {
    float m = sqrt(w * w + x * x + y * y + z * z);
    if (m > 1e-9f) { w /= m; x /= m; y /= m; z /= m; }
    return this;
  }

  // Compose rotations (this * q).
  SpaceQuat multiply(SpaceQuat q) {
    return new SpaceQuat(
      w * q.w - x * q.x - y * q.y - z * q.z,
      w * q.x + x * q.w + y * q.z - z * q.y,
      w * q.y - x * q.z + y * q.w + z * q.x,
      w * q.z + x * q.y - y * q.x + z * q.w
    );
  }

  // Column-major 4x4 rotation matrix.
  float[] toMatrix() {
    float[] m = new float[16];
    m[0] = 1 - 2 * (y * y + z * z);
    m[1] = 2 * (x * y + z * w);
    m[2] = 2 * (x * z - y * w);
    m[3] = 0;

    m[4] = 2 * (x * y - z * w);
    m[5] = 1 - 2 * (x * x + z * z);
    m[6] = 2 * (y * z + x * w);
    m[7] = 0;

    m[8] = 2 * (x * z + y * w);
    m[9] = 2 * (y * z - x * w);
    m[10] = 1 - 2 * (x * x + y * y);
    m[11] = 0;

    m[12] = m[13] = m[14] = 0;
    m[15] = 1;
    return m;
  }

  // SLERP between this and q2.
  SpaceQuat slerp(SpaceQuat q2, float t) {
    float dot = w * q2.w + x * q2.x + y * q2.y + z * q2.z;
    dot = constrain(dot, -1, 1);
    float theta = acos(dot);
    if (theta < 1e-6f) return this;
    float sinT = sin(theta);
    float w1 = sin((1 - t) * theta) / sinT;
    float w2 = sin(t * theta) / sinT;
    return new SpaceQuat(
      w1 * w + w2 * q2.w,
      w1 * x + w2 * q2.x,
      w1 * y + w2 * q2.y,
      w1 * z + w2 * q2.z
    ).normalize();
  }
}

/**
 * Quaternion orbit camera that operates in scene space with smooth interpolation.
 */
class SpaceCamera {
  PVector target;           // point the camera looks at
  float distance;           // distance to the target
  SpaceQuat orientation;    // current orientation

  PVector goalTarget;
  SpaceQuat goalOrientation;
  float goalDistance;
  float lerpFactor = 0.15f; // smoothing amount

  float minDistance = 200f;
  float maxDistance = 6000f;

  SpaceCamera(PVector initialTarget, float initialDistance) {
    target = initialTarget.copy();
    goalTarget = initialTarget.copy();
    distance = initialDistance;
    goalDistance = initialDistance;
    orientation = new SpaceQuat(1, 0, 0, 0);
    goalOrientation = orientation;
  }

  // Apply the camera transform to the scene graphics.
  void apply(PGraphicsOpenGL pg) {
    pg.translate(0, 0, -distance);
    float[] M = orientation.toMatrix();
    pg.applyMatrix(
      M[0], M[4], M[8], M[12],
      M[1], M[5], M[9], M[13],
      M[2], M[6], M[10], M[14],
      M[3], M[7], M[11], M[15]
    );
    pg.translate(-target.x, -target.y, -target.z);
  }

  // Smooth interpolation each frame.
  void update() {
    orientation = orientation.slerp(goalOrientation, lerpFactor);
    target = PVector.lerp(target, goalTarget, lerpFactor);
    distance = lerp(distance, goalDistance, lerpFactor);
  }

  // Orbit around a world axis (updates the goal so motion stays smooth).
  void rotateAround(PVector axis, float angle) {
    SpaceQuat delta = new SpaceQuat(1, 0, 0, 0).fromAxisAngle(axis, angle);
    goalOrientation = delta.multiply(goalOrientation).normalize();
  }

  // Zoom by changing the goal distance (clamped).
  void zoom(float amount) {
    goalDistance = constrain(goalDistance + amount, minDistance, maxDistance);
  }

  // Direct setters (with smoothing preserved through the goals).
  void setTarget(PVector t) { goalTarget = t.copy(); }
  void setDistance(float d) { goalDistance = constrain(d, minDistance, maxDistance); }
  void setOrientation(SpaceQuat q) { goalOrientation = q.normalize(); }

  // Snap immediately (no interpolation) — used on reset.
  void snapTo(PVector t, SpaceQuat q, float d) {
    target = goalTarget = t.copy();
    orientation = goalOrientation = q.normalize();
    distance = goalDistance = constrain(d, minDistance, maxDistance);
  }

  float getDistance() { return distance; }
  SpaceQuat getOrientation() { return orientation; }
}

