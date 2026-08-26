package com.victorvalentim.zividomelive.core.projection;

/** Qualified host-independent domemaster field-of-view and circle-size state. */
public final class DomemasterSettings {

    public static final float DEFAULT_FIELD_OF_VIEW_DEGREES = 210.0f;
    public static final float DEFAULT_SIZE_PERCENT = 100.0f;

    private float fieldOfViewDegrees = DEFAULT_FIELD_OF_VIEW_DEGREES;
    private float sizePercent = DEFAULT_SIZE_PERCENT;

    /** Sets FOV constrained to [0, 360] degrees; non-finite values are ignored. */
    public void setFieldOfViewDegrees(float fieldOfViewDegrees) {
        if (!Float.isFinite(fieldOfViewDegrees)) {
            return;
        }
        this.fieldOfViewDegrees = constrain(fieldOfViewDegrees, 0.0f, 360.0f);
    }

    public float getFieldOfViewDegrees() {
        return fieldOfViewDegrees;
    }

    /** Sets circle size constrained to [0, 100] percent; non-finite values are ignored. */
    public void setSizePercent(float sizePercent) {
        if (!Float.isFinite(sizePercent)) {
            return;
        }
        this.sizePercent = constrain(sizePercent, 0.0f, 100.0f);
    }

    public float getSizePercent() {
        return sizePercent;
    }

    /** Restores qualified defaults. */
    public void reset() {
        fieldOfViewDegrees = DEFAULT_FIELD_OF_VIEW_DEGREES;
        sizePercent = DEFAULT_SIZE_PERCENT;
    }

    private static float constrain(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
