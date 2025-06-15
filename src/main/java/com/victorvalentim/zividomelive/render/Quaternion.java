package com.victorvalentim.zividomelive.render;

import processing.core.PMatrix3D;
import processing.core.PApplet;


/**
 * Simple quaternion class for representing rotations.
 */
public class Quaternion {
// Components of the quaternion representing the rotation.
    // These are the four values that define the quaternion:
    // x, y, z are the vector part, and w is the scalar part.

        /**
         * X component of the quaternion, representing the vector part along the X axis.
         */
        public float x;

        /**
         * Y component of the quaternion, representing the vector part along the Y axis.
         */
        public float y;

        /**
         * Z component of the quaternion, representing the vector part along the Z axis.
         */
        public float z;

        /**
         * W component of the quaternion, representing the scalar part of the rotation.
         */
        public float w;

    /**
     * Constructs a quaternion with the given components.
     *
     * @param x the X component (vector part)
     * @param y the Y component (vector part)
     * @param z the Z component (vector part)
     * @param w the W component (scalar part)
     */
    public Quaternion(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /**
     * Creates a quaternion from an axis and angle.
     *
     * @param ax axis x component
     * @param ay axis y component
     * @param az axis z component
     * @param angle rotation angle in radians
     * @return quaternion representing the rotation
     */
    public static Quaternion fromAxisAngle(float ax, float ay, float az, float angle) {
        float half = angle / 2f;
        float sin = PApplet.sin(half);
        float cos = PApplet.cos(half);
        return new Quaternion(ax * sin, ay * sin, az * sin, cos);
    }

   /**
    * Multiplies this quaternion by another quaternion and returns the result.
    *
    * @param other the quaternion to multiply with
    * @return the product quaternion
    */
   public Quaternion multiply(Quaternion other) {
       float newW = w * other.w - x * other.x - y * other.y - z * other.z;
       float newX = w * other.x + x * other.w + y * other.z - z * other.y;
       float newY = w * other.y - x * other.z + y * other.w + z * other.x;
       float newZ = w * other.z + x * other.y - y * other.x + z * other.w;
       return new Quaternion(newX, newY, newZ, newW);
   }

    /**
     * Converts this quaternion to a 3x3 rotation matrix stored in a {@link PMatrix3D}.
     * The resulting matrix can be used for 3D transformations.
     *
     * @return a {@link PMatrix3D} representing the rotation of this quaternion
     */
    public PMatrix3D toMatrix() {
        PMatrix3D m = new PMatrix3D();
        float xx = x * x;
        float yy = y * y;
        float zz = z * z;
        float xy = x * y;
        float xz = x * z;
        float yz = y * z;
        float wx = w * x;
        float wy = w * y;
        float wz = w * z;

        m.m00 = 1f - 2f * (yy + zz);
        m.m01 = 2f * (xy - wz);
        m.m02 = 2f * (xz + wy);
        m.m10 = 2f * (xy + wz);
        m.m11 = 1f - 2f * (xx + zz);
        m.m12 = 2f * (yz - wx);
        m.m20 = 2f * (xz - wy);
        m.m21 = 2f * (yz + wx);
        m.m22 = 1f - 2f * (xx + yy);
        m.m03 = m.m13 = m.m23 = 0f;
        m.m30 = m.m31 = m.m32 = 0f;
        m.m33 = 1f;
        return m;
    }

    /**
     * Normalizes this quaternion to unit length and returns itself.
     *
     * @return this quaternion after normalization
     */
    public Quaternion normalize() {
        float mag = PApplet.sqrt(w * w + x * x + y * y + z * z);
        if (mag == 0) return this;
        w /= mag;
        x /= mag;
        y /= mag;
        z /= mag;
        return this;
    }

    /**
     * Spherical linear interpolation (SLERP) between this quaternion and q2.
     *
     * @param q2 target quaternion
     * @param t  interpolation factor in [0,1]
     * @return interpolated quaternion
     */
    public Quaternion slerp(Quaternion q2, float t) {
        float dot = w * q2.w + x * q2.x + y * q2.y + z * q2.z;
        dot = PApplet.constrain(dot, -1f, 1f);
        float theta = (float) Math.acos(dot);
        if (theta < 1e-6) return new Quaternion(x, y, z, w);
        float sinT = PApplet.sin(theta);
        float w1 = PApplet.sin((1 - t) * theta) / sinT;
        float w2 = PApplet.sin(t * theta) / sinT;
        return new Quaternion(
                w1 * x + w2 * q2.x,
                w1 * y + w2 * q2.y,
                w1 * z + w2 * q2.z,
                w1 * w + w2 * q2.w
        ).normalize();
    }

    /**
     * Returns a new {@link PMatrix3D} representing the rotation of this quaternion.
     *
     * @return a 4x4 rotation matrix as a {@link PMatrix3D}
     */
    public PMatrix3D toPMatrix() {
        return new PMatrix3D(
                toMatrix().m00, toMatrix().m01, toMatrix().m02, toMatrix().m03,
                toMatrix().m10, toMatrix().m11, toMatrix().m12, toMatrix().m13,
                toMatrix().m20, toMatrix().m21, toMatrix().m22, toMatrix().m23,
                toMatrix().m30, toMatrix().m31, toMatrix().m32, toMatrix().m33
        );
    }
}