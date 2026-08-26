package com.victorvalentim.zividomelive.core.math;

/** Small immutable finite three-dimensional vector value. */
public record Vec3(float x, float y, float z) {

    private static final Vec3 ZERO = new Vec3(0.0f, 0.0f, 0.0f);

    /** Validates that every component is finite. */
    public Vec3 {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
    }

    /** @return the shared immutable zero vector */
    public static Vec3 zero() {
        return ZERO;
    }

    private static void requireFinite(float value, String label) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Vector " + label + " must be finite.");
        }
    }
}
