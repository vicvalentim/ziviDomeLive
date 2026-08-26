package com.victorvalentim.zividomelive.core.environment;

import com.victorvalentim.zividomelive.core.math.Quaternion;

/**
 * Host-independent visual environment controls without an image, texture, or asset handle.
 *
 * <p>Source orientation aligns the environment image independently from spherical projection
 * controls and scene geometry. A host separately owns the actual image and camera composition.</p>
 */
public final class EnvironmentState {

    private boolean visible = true;
    private float intensity = 1.0f;
    private float yawOffset;
    private Quaternion sourceOrientation = Quaternion.identity();

    /** Enables or disables visual environment rendering. */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    /** Sets a non-negative multiplier; non-finite values are ignored. */
    public void setIntensity(float intensity) {
        if (!Float.isFinite(intensity)) {
            return;
        }
        this.intensity = Math.max(0.0f, intensity);
    }

    public float getIntensity() {
        return intensity;
    }

    /** Sets a source-longitude offset in radians; non-finite values are ignored. */
    public void setYawOffset(float yawOffset) {
        if (!Float.isFinite(yawOffset)) {
            return;
        }
        this.yawOffset = yawOffset;
    }

    public float getYawOffset() {
        return yawOffset;
    }

    /** Sets a normalized source alignment, or identity when null. */
    public void setSourceOrientation(Quaternion orientation) {
        if (orientation == null) {
            sourceOrientation = Quaternion.identity();
            return;
        }
        if (orientation == sourceOrientation) {
            return;
        }
        sourceOrientation = orientation.normalized();
    }

    public Quaternion getSourceOrientation() {
        return sourceOrientation;
    }

    /** Restores visible, unit intensity, zero yaw, and identity source alignment. */
    public void reset() {
        visible = true;
        intensity = 1.0f;
        yawOffset = 0.0f;
        sourceOrientation = Quaternion.identity();
    }
}
