package com.victorvalentim.zividomelive.core.projection;

import com.victorvalentim.zividomelive.core.math.Quaternion;

/**
 * Gimbal-lock-free spherical orientation controlled by cyclic pitch, yaw, and roll values.
 *
 * <p>The control values are accumulators, not an Euler decomposition. Each accepted change uses
 * its shortest angular delta and composes a local-axis quaternion in event order: pitch local X,
 * yaw local Z, and roll local Y.</p>
 */
public final class SphericalOrientation {

    private static final float PI = (float) Math.PI;
    private static final float TWO_PI = PI * 2.0f;

    private Quaternion orientation;
    private float pitch;
    private float yaw;
    private float roll;

    /** Creates an identity orientation with zero control accumulators. */
    public SphericalOrientation() {
        reset();
    }

    /** Applies a pitch control change around local X; non-finite values are ignored. */
    public void setPitch(float value) {
        if (!Float.isFinite(value)) {
            return;
        }
        applyLocalDelta(1.0f, 0.0f, 0.0f, shortestDelta(pitch, value));
        pitch = value;
    }

    /** Applies a yaw control change around local Z; non-finite values are ignored. */
    public void setYaw(float value) {
        if (!Float.isFinite(value)) {
            return;
        }
        applyLocalDelta(0.0f, 0.0f, 1.0f, shortestDelta(yaw, value));
        yaw = value;
    }

    /** Applies a roll control change around local Y; non-finite values are ignored. */
    public void setRoll(float value) {
        if (!Float.isFinite(value)) {
            return;
        }
        applyLocalDelta(0.0f, 1.0f, 0.0f, shortestDelta(roll, value));
        roll = value;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getRoll() {
        return roll;
    }

    /** @return the current immutable unit quaternion */
    public Quaternion getQuaternion() {
        return orientation;
    }

    /** Restores identity and zeros all three control accumulators. */
    public void reset() {
        orientation = Quaternion.identity();
        pitch = 0.0f;
        yaw = 0.0f;
        roll = 0.0f;
    }

    private void applyLocalDelta(float axisX, float axisY, float axisZ, float angle) {
        if (angle == 0.0f) {
            return;
        }
        Quaternion delta = Quaternion.fromAxisAngle(axisX, axisY, axisZ, angle);
        orientation = orientation.multiply(delta).normalized();
    }

    private static float normalizeAngle(float angle) {
        if (!Float.isFinite(angle)) {
            return angle;
        }
        float normalized = (angle + PI) % TWO_PI;
        if (normalized < 0.0f) {
            normalized += TWO_PI;
        }
        return normalized - PI;
    }

    private static float shortestDelta(float current, float target) {
        return normalizeAngle(target - current);
    }
}
