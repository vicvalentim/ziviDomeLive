package com.victorvalentim.zividomelive.core.math;

/** Immutable float quaternion for three-dimensional rotations. */
public final class Quaternion {

    private static final float SLERP_LINEAR_THRESHOLD = 0.9995f;
    private static final float UNIT_TOLERANCE = 1.0e-6f;
    private static final Quaternion IDENTITY = new Quaternion(0.0f, 0.0f, 0.0f, 1.0f);

    private final float x;
    private final float y;
    private final float z;
    private final float w;

    /** Creates a quaternion whose four components must be finite. */
    public Quaternion(float x, float y, float z, float w) {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
        requireFinite(w, "w");
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /** @return the shared immutable identity rotation */
    public static Quaternion identity() {
        return IDENTITY;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float z() {
        return z;
    }

    public float w() {
        return w;
    }

    /**
     * Creates a quaternion from a normalized copy of an axis and a radian angle.
     * A zero angle returns identity even when the supplied axis is zero.
     */
    public static Quaternion fromAxisAngle(float ax, float ay, float az, float angle) {
        requireFinite(ax, "axis x");
        requireFinite(ay, "axis y");
        requireFinite(az, "axis z");
        requireFinite(angle, "angle");
        if (angle == 0.0f) {
            return IDENTITY;
        }
        float axisMagnitude = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (axisMagnitude == 0.0f) {
            throw new IllegalArgumentException("Rotation axis cannot have zero magnitude.");
        }
        float inverseMagnitude = 1.0f / axisMagnitude;
        float half = angle / 2.0f;
        float sin = (float) Math.sin(half);
        float cos = (float) Math.cos(half);
        return new Quaternion(
                ax * inverseMagnitude * sin,
                ay * inverseMagnitude * sin,
                az * inverseMagnitude * sin,
                cos);
    }

    /**
     * Returns {@code this * other}. The order is deliberate and is not commutative.
     */
    public Quaternion multiply(Quaternion other) {
        requireQuaternion(other, "Other quaternion");
        float newW = w * other.w - x * other.x - y * other.y - z * other.z;
        float newX = w * other.x + x * other.w + y * other.z - z * other.y;
        float newY = w * other.y - x * other.z + y * other.w + z * other.x;
        float newZ = w * other.z + x * other.y - y * other.x + z * other.w;
        return new Quaternion(newX, newY, newZ, newW);
    }

    /** Returns a unit quaternion without mutating this value. */
    public Quaternion normalized() {
        float magnitudeSquared = w * w + x * x + y * y + z * z;
        if (magnitudeSquared == 0.0f) {
            throw new IllegalStateException("A zero quaternion cannot be normalized.");
        }
        if (Math.abs(magnitudeSquared - 1.0f) <= UNIT_TOLERANCE) {
            return this;
        }
        float inverseMagnitude = 1.0f / (float) Math.sqrt(magnitudeSquared);
        return new Quaternion(
                x * inverseMagnitude,
                y * inverseMagnitude,
                z * inverseMagnitude,
                w * inverseMagnitude);
    }

    /**
     * Interpolates along the shortest rotational path. The factor is clamped to [0, 1].
     */
    public Quaternion slerp(Quaternion target, float factor) {
        requireQuaternion(target, "Target quaternion");
        requireFinite(factor, "interpolation factor");
        float amount = Math.max(0.0f, Math.min(1.0f, factor));
        Quaternion start = normalized();
        Quaternion end = target.normalized();
        if (amount == 0.0f) {
            return start;
        }

        float dot = start.w * end.w + start.x * end.x
                + start.y * end.y + start.z * end.z;
        float endX = end.x;
        float endY = end.y;
        float endZ = end.z;
        float endW = end.w;
        boolean negatedEnd = false;
        if (dot < 0.0f) {
            endX = -endX;
            endY = -endY;
            endZ = -endZ;
            endW = -endW;
            negatedEnd = true;
            dot = -dot;
        }
        dot = Math.max(-1.0f, Math.min(1.0f, dot));
        if (amount == 1.0f) {
            return negatedEnd ? new Quaternion(endX, endY, endZ, endW) : end;
        }
        if (dot >= 1.0f - UNIT_TOLERANCE) {
            return start;
        }
        if (dot > SLERP_LINEAR_THRESHOLD) {
            return new Quaternion(
                    start.x + amount * (endX - start.x),
                    start.y + amount * (endY - start.y),
                    start.z + amount * (endZ - start.z),
                    start.w + amount * (endW - start.w)).normalized();
        }
        float theta = (float) Math.acos(dot);
        float sinTheta = (float) Math.sin(theta);
        float startWeight = (float) Math.sin((1.0f - amount) * theta) / sinTheta;
        float endWeight = (float) Math.sin(amount * theta) / sinTheta;
        return new Quaternion(
                startWeight * start.x + endWeight * endX,
                startWeight * start.y + endWeight * endY,
                startWeight * start.z + endWeight * endZ,
                startWeight * start.w + endWeight * endW).normalized();
    }

    private static Quaternion requireQuaternion(Quaternion quaternion, String label) {
        if (quaternion == null) {
            throw new IllegalArgumentException(label + " cannot be null.");
        }
        return quaternion;
    }

    private static void requireFinite(float value, String label) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Quaternion " + label + " must be finite.");
        }
    }
}
