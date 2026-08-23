package com.victorvalentim.zividomelive.render;

import processing.core.PMatrix3D;
import processing.core.PVector;


/**
 * Immutable quaternion value for representing three-dimensional rotations.
 *
 * <p>Operations never mutate either operand. Methods that require a unit quaternion normalize
 * their inputs or account for magnitude explicitly, and all components must be finite.</p>
 *
 * <p><strong>API stability:</strong> Advanced Stable.</p>
 *
 * @since 2.0.0
 */
public final class Quaternion {
    private static final float SLERP_LINEAR_THRESHOLD = 0.9995f;
    private static final float UNIT_TOLERANCE = 1.0e-6f;
    private static final Quaternion IDENTITY = new Quaternion(0.0f, 0.0f, 0.0f, 1.0f);

    private final float x;
    private final float y;
    private final float z;
    private final float w;

    /**
     * Constructs a quaternion with the given components.
     *
     * @param x the X component (vector part)
     * @param y the Y component (vector part)
     * @param z the Z component (vector part)
     * @param w the W component (scalar part)
     */
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

    /** @return X component of the vector part */
    public float x() {
        return x;
    }

    /** @return Y component of the vector part */
    public float y() {
        return y;
    }

    /** @return Z component of the vector part */
    public float z() {
        return z;
    }

    /** @return scalar component */
    public float w() {
        return w;
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
        float half = angle / 2f;
        float sin = (float) Math.sin(half);
        float cos = (float) Math.cos(half);
        return new Quaternion(
                ax * inverseMagnitude * sin,
                ay * inverseMagnitude * sin,
                az * inverseMagnitude * sin,
                cos);
    }

    /**
     * Creates a quaternion from a Processing axis vector and angle.
     *
     * @param axis rotation axis; must not be {@code null}
     * @param angle rotation angle in radians
     * @return quaternion representing the rotation
     */
    public static Quaternion fromAxisAngle(PVector axis, float angle) {
        if (axis == null) {
            throw new IllegalArgumentException("Rotation axis cannot be null.");
        }
        return fromAxisAngle(axis.x, axis.y, axis.z, angle);
    }

   /**
    * Multiplies this quaternion by another quaternion and returns the result.
    *
    * @param other the quaternion to multiply with
    * @return the product quaternion
    */
   public Quaternion multiply(Quaternion other) {
       requireQuaternion(other, "Other quaternion");
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
        toMatrix(m);
        return m;
    }

    /**
     * Writes this quaternion's rotation matrix into a caller-owned matrix.
     *
     * <p>This allocation-free overload preserves the exact qualified orientation math used by
     * {@link #toMatrix()}.</p>
     *
     * @param m destination matrix; must not be {@code null}
     */
    public void toMatrix(PMatrix3D m) {
        if (m == null) {
            throw new IllegalArgumentException("Destination matrix cannot be null.");
        }
        float normSquared = w * w + x * x + y * y + z * z;
        if (normSquared == 0.0f) {
            throw new IllegalStateException("A zero quaternion does not define a rotation.");
        }
        float scale = 2.0f / normSquared;
        float xx = x * x;
        float yy = y * y;
        float zz = z * z;
        float xy = x * y;
        float xz = x * z;
        float yz = y * z;
        float wx = w * x;
        float wy = w * y;
        float wz = w * z;

        m.m00 = 1f - scale * (yy + zz);
        m.m01 = scale * (xy - wz);
        m.m02 = scale * (xz + wy);
        m.m10 = scale * (xy + wz);
        m.m11 = 1f - scale * (xx + zz);
        m.m12 = scale * (yz - wx);
        m.m20 = scale * (xz - wy);
        m.m21 = scale * (yz + wx);
        m.m22 = 1f - scale * (xx + yy);
        m.m03 = m.m13 = m.m23 = 0f;
        m.m30 = m.m31 = m.m32 = 0f;
        m.m33 = 1f;
    }

    /**
     * Returns this rotation normalized to unit length without mutating this value.
     *
     * @return a unit quaternion; this instance may be returned when it is already normalized
     * @throws IllegalStateException when all four components are zero
     */
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
     * Spherical linear interpolation (SLERP) between this quaternion and q2.
     *
     * @param q2 target quaternion
     * @param t  interpolation factor in [0,1]
     * @return interpolated quaternion
     */
    public Quaternion slerp(Quaternion q2, float t) {
        requireQuaternion(q2, "Target quaternion");
        requireFinite(t, "interpolation factor");
        float amount = Math.max(0.0f, Math.min(1.0f, t));
        Quaternion start = normalized();
        Quaternion end = q2.normalized();
        if (amount == 0.0f) {
            return start;
        }
        float dot = start.w * end.w + start.x * end.x + start.y * end.y + start.z * end.z;
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
