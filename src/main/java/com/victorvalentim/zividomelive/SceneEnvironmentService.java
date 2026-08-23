package com.victorvalentim.zividomelive;

import processing.core.PImage;

import java.util.Objects;

/**
 * Activation-scoped environment configuration that restores the facade state it replaced.
 *
 * <p>Restoration is ownership-safe: a property is restored only if it still contains the value
 * applied by this activation, so a later owner is not overwritten.</p>
 *
 * <p><strong>API stability:</strong> Advanced Stable.</p>
 *
 * @since 2.0.0
 */
public final class SceneEnvironmentService {

    private final ziviDomeLive parent;
    private PImage previousSource;
    private PImage appliedSource;
    private boolean previousVisible;
    private boolean appliedVisible;
    private float previousIntensity;
    private float appliedIntensity;
    private float previousYawOffset;
    private float appliedYawOffset;
    private boolean sourceTouched;
    private boolean visibleTouched;
    private boolean intensityTouched;
    private boolean yawOffsetTouched;
    private boolean closed;

    SceneEnvironmentService(ziviDomeLive parent) {
        this.parent = Objects.requireNonNull(parent, "parent");
    }

    /**
     * Sets the borrowed equirectangular source for this activation.
     *
     * @param image borrowed equirectangular source, or {@code null} to clear temporarily
     */
    public void setEquirectangular(PImage image) {
        ensureOpen();
        if (!sourceTouched) {
            previousSource = parent.getEnvironmentBackgroundSource();
            sourceTouched = true;
        }
        parent.setEquirectangularBackground(image);
        appliedSource = image;
    }

    /** Temporarily clears the current source for this activation. */
    public void clear() {
        setEquirectangular(null);
    }

    /**
     * Sets Environment visibility for this activation.
     *
     * @param visible true to render the Environment
     */
    public void setVisible(boolean visible) {
        ensureOpen();
        if (!visibleTouched) {
            previousVisible = parent.isEnvironmentBackgroundVisible();
            visibleTouched = true;
        }
        parent.setEnvironmentBackgroundVisible(visible);
        appliedVisible = visible;
    }

    /**
     * Sets the non-negative visual colour multiplier for this activation.
     *
     * @param intensity visual multiplier; finite negative values are clamped to zero and
     *                  non-finite values are ignored
     */
    public void setIntensity(float intensity) {
        ensureOpen();
        if (!intensityTouched) {
            previousIntensity = parent.getEnvironmentBackgroundIntensity();
            intensityTouched = true;
        }
        parent.setEnvironmentBackgroundIntensity(intensity);
        appliedIntensity = parent.getEnvironmentBackgroundIntensity();
    }

    /**
     * Sets the source-longitude offset for this activation.
     *
     * @param yawOffset offset in radians; non-finite values are ignored
     */
    public void setYawOffset(float yawOffset) {
        ensureOpen();
        if (!yawOffsetTouched) {
            previousYawOffset = parent.getEnvironmentBackgroundYawOffset();
            yawOffsetTouched = true;
        }
        parent.setEnvironmentBackgroundYawOffset(yawOffset);
        appliedYawOffset = parent.getEnvironmentBackgroundYawOffset();
    }

    /** @return current Environment visibility */
    public boolean isVisible() {
        ensureOpen();
        return parent.isEnvironmentBackgroundVisible();
    }

    /** @return current non-negative visual colour multiplier */
    public float getIntensity() {
        ensureOpen();
        return parent.getEnvironmentBackgroundIntensity();
    }

    /** @return current source-longitude offset in radians */
    public float getYawOffset() {
        ensureOpen();
        return parent.getEnvironmentBackgroundYawOffset();
    }

    void close() {
        if (closed) {
            return;
        }
        if (sourceTouched && parent.getEnvironmentBackgroundSource() == appliedSource) {
            parent.setEquirectangularBackground(previousSource);
        }
        if (visibleTouched && parent.isEnvironmentBackgroundVisible() == appliedVisible) {
            parent.setEnvironmentBackgroundVisible(previousVisible);
        }
        if (intensityTouched && sameFloat(
                parent.getEnvironmentBackgroundIntensity(), appliedIntensity)) {
            parent.setEnvironmentBackgroundIntensity(previousIntensity);
        }
        if (yawOffsetTouched && sameFloat(
                parent.getEnvironmentBackgroundYawOffset(), appliedYawOffset)) {
            parent.setEnvironmentBackgroundYawOffset(previousYawOffset);
        }
        closed = true;
    }

    private static boolean sameFloat(float left, float right) {
        return Float.floatToIntBits(left) == Float.floatToIntBits(right);
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Scene Environment service is closed.");
        }
    }
}
