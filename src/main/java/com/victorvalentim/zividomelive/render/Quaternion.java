package com.victorvalentim.zividomelive.render;

import processing.core.PMatrix3D;
import processing.core.PApplet;

/**
 * Simple quaternion class for representing rotations.
 */
public class Quaternion {
    public float x;
    public float y;
    public float z;
    public float w;

    /**
     * Constructs a quaternion with the given components.
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
     * Creates a quaternion from Euler angles in radians using the order
     * pitch(X) -> roll(Y) -> yaw(Z).
     */
    public static Quaternion fromEuler(float pitch, float roll, float yaw) {
        Quaternion qx = fromAxisAngle(1f, 0f, 0f, pitch);
        Quaternion qy = fromAxisAngle(0f, 1f, 0f, roll);
        Quaternion qz = fromAxisAngle(0f, 0f, 1f, yaw);
        return qz.multiply(qy).multiply(qx);
    }

    /**
     * Multiplies this quaternion by another quaternion and returns the result.
     */
    public Quaternion multiply(Quaternion other) {
        float newW = w * other.w - x * other.x - y * other.y - z * other.z;
        float newX = w * other.x + x * other.w + y * other.z - z * other.y;
        float newY = w * other.y - x * other.z + y * other.w + z * other.x;
        float newZ = w * other.z + x * other.y - y * other.x + z * other.w;
        return new Quaternion(newX, newY, newZ, newW);
    }

    /**
     * Converts this quaternion to a 4x4 rotation matrix.
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
}
