package com.victorvalentim.zividomelive.render;

import processing.core.PConstants;

/**
 * Gimbal-lock-free spherical orientation controlled by cyclic pitch, yaw, and roll values.
 *
 * <p>The three values are cyclic control accumulators, not an Euler decomposition of the current
 * attitude. Each value change is converted to the shortest angular delta and composed directly
 * into a unit quaternion in event order. Getter values preserve the corresponding setter values
 * for facade compatibility.</p>
 */
public final class SphericalOrientation {
    private Quaternion orientation;
    private float pitch;
    private float yaw;
    private float roll;

    /** Creates an identity orientation with all control accumulators at zero. */
    public SphericalOrientation() {
        reset();
    }

    /**
     * Applies a pitch change around the current local X axis.
     *
     * @param value pitch control value in radians
     */
    public void setPitch(float value) {
        if (!Float.isFinite(value)) {
            return;
        }
        applyLocalDelta(1.0f, 0.0f, 0.0f, shortestDelta(pitch, value));
        pitch = value;
    }

    /**
     * Applies a yaw change around the current local Z axis.
     *
     * @param value yaw control value in radians
     */
    public void setYaw(float value) {
        if (!Float.isFinite(value)) {
            return;
        }
        applyLocalDelta(0.0f, 0.0f, 1.0f, shortestDelta(yaw, value));
        yaw = value;
    }

    /**
     * Applies a roll change around the current local Y axis.
     *
     * @param value roll control value in radians
     */
    public void setRoll(float value) {
        if (!Float.isFinite(value)) {
            return;
        }
        applyLocalDelta(0.0f, 1.0f, 0.0f, shortestDelta(roll, value));
        roll = value;
    }

    /**
     * Returns the pitch control accumulator.
     *
     * @return pitch control value in radians
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * Returns the yaw control accumulator.
     *
     * @return yaw control value in radians
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * Returns the roll control accumulator.
     *
     * @return roll control value in radians
     */
    public float getRoll() {
        return roll;
    }

    /**
     * Returns a defensive copy of the current unit quaternion.
     *
     * @return current spherical orientation
     */
    public Quaternion getQuaternion() {
        return new Quaternion(orientation.x, orientation.y, orientation.z, orientation.w);
    }

    /** Restores the identity quaternion and zeroes all control accumulators. */
    public void reset() {
        orientation = new Quaternion(0.0f, 0.0f, 0.0f, 1.0f);
        pitch = 0.0f;
        yaw = 0.0f;
        roll = 0.0f;
    }

    private void applyLocalDelta(float axisX, float axisY, float axisZ, float angle) {
        if (angle == 0.0f) {
            return;
        }
        Quaternion delta = Quaternion.fromAxisAngle(axisX, axisY, axisZ, angle);
        orientation = orientation.multiply(delta).normalize();
    }

    private static float normalizeAngle(float angle) {
        if (!Float.isFinite(angle)) {
            return angle;
        }
        float fullTurn = PConstants.TWO_PI;
        float normalized = (angle + PConstants.PI) % fullTurn;
        if (normalized < 0.0f) {
            normalized += fullTurn;
        }
        return normalized - PConstants.PI;
    }

    private static float shortestDelta(float current, float target) {
        return normalizeAngle(target - current);
    }
}
