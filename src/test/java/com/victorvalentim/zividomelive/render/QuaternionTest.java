package com.victorvalentim.zividomelive.render;

import org.junit.jupiter.api.Test;
import processing.core.PMatrix3D;
import processing.core.PVector;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Quaternion}.
 *
 * <p>All tests verify pure mathematical properties: construction, normalization,
 * multiplication, axis-angle conversion, SLERP and matrix output. No OpenGL
 * context is required; the only Processing dependency is the {@code PApplet}
 * static math helpers ({@code sin}, {@code cos}, {@code sqrt}, {@code constrain})
 * which delegate directly to {@link Math}.</p>
 */
class QuaternionTest {

    private static final float DELTA = 1e-5f;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    @Test
    void constructor_storesAllComponents() {
        Quaternion q = new Quaternion(1f, 2f, 3f, 4f);

        assertEquals(1f, q.x(), DELTA);
        assertEquals(2f, q.y(), DELTA);
        assertEquals(3f, q.z(), DELTA);
        assertEquals(4f, q.w(), DELTA);
    }

    // -----------------------------------------------------------------------
    // fromAxisAngle
    // -----------------------------------------------------------------------

    @Test
    void fromAxisAngle_zeroAngle_returnsIdentity() {
        Quaternion q = Quaternion.fromAxisAngle(0f, 1f, 0f, 0f);

        assertEquals(0f, q.x(), DELTA);
        assertEquals(0f, q.y(), DELTA);
        assertEquals(0f, q.z(), DELTA);
        assertEquals(1f, q.w(), DELTA, "Zero-angle rotation must yield the identity quaternion");
    }

    @Test
    void fromAxisAngle_halfTurnAroundY_hasExpectedComponents() {
        // 180° rotation around Y: half = PI/2, sin(PI/2) = 1, cos(PI/2) ≈ 0
        Quaternion q = Quaternion.fromAxisAngle(0f, 1f, 0f, (float) Math.PI);

        assertEquals(0f, q.x(), DELTA);
        assertEquals(1f, q.y(), DELTA);
        assertEquals(0f, q.z(), DELTA);
        assertEquals(0f, q.w(), 1e-4f, "cos(PI/2) must be approximately zero");
    }

    @Test
    void fromAxisAngle_quarterTurnAroundZ_hasExpectedComponents() {
        // 90° rotation around Z: half = PI/4, sin = cos = 1/√2
        float halfSqrt2 = (float) (Math.sqrt(2) / 2);
        Quaternion q = Quaternion.fromAxisAngle(0f, 0f, 1f, (float) (Math.PI / 2));

        assertEquals(0f, q.x(), DELTA);
        assertEquals(0f, q.y(), DELTA);
        assertEquals(halfSqrt2, q.z(), DELTA);
        assertEquals(halfSqrt2, q.w(), DELTA);
    }

    @Test
    void fromAxisAngle_acceptsProcessingVector() {
        Quaternion q = Quaternion.fromAxisAngle(
                new PVector(0f, 1f, 0f),
                (float) Math.PI);

        assertEquals(0f, q.x(), DELTA);
        assertEquals(1f, q.y(), DELTA);
        assertEquals(0f, q.z(), DELTA);
        assertEquals(0f, q.w(), 1e-4f);
    }

    // -----------------------------------------------------------------------
    // normalized
    // -----------------------------------------------------------------------

    @Test
    void normalized_identityQuaternionRemainsIdentity() {
        Quaternion q = new Quaternion(0f, 0f, 0f, 1f).normalized();

        assertEquals(0f, q.x(), DELTA);
        assertEquals(0f, q.y(), DELTA);
        assertEquals(0f, q.z(), DELTA);
        assertEquals(1f, q.w(), DELTA);
    }

    @Test
    void normalized_nonUnitQuaternionProducesUnitLength() {
        // (3, 0, 0, 4) has magnitude 5 → normalized (0.6, 0, 0, 0.8)
        Quaternion q = new Quaternion(3f, 0f, 0f, 4f).normalized();

        assertEquals(0.6f, q.x(), DELTA);
        assertEquals(0f,   q.y(), DELTA);
        assertEquals(0f,   q.z(), DELTA);
        assertEquals(0.8f, q.w(), DELTA);

        float mag = (float) Math.sqrt(q.x() * q.x() + q.y() * q.y() + q.z() * q.z() + q.w() * q.w());
        assertEquals(1f, mag, DELTA, "Normalized quaternion must have unit length");
    }

    @Test
    void normalized_zeroQuaternionIsRejected() {
        Quaternion q = new Quaternion(0f, 0f, 0f, 0f);
        assertThrows(IllegalStateException.class, q::normalized);
    }

    @Test
    void normalized_doesNotMutateTheOriginalValue() {
        Quaternion original = new Quaternion(3f, 0f, 0f, 4f);

        Quaternion normalized = original.normalized();

        assertNotSame(original, normalized);
        assertEquals(3f, original.x(), DELTA);
        assertEquals(4f, original.w(), DELTA);
    }

    @Test
    void componentsArePrivateFinalFields() throws Exception {
        for (String component : java.util.List.of("x", "y", "z", "w")) {
            java.lang.reflect.Field field = Quaternion.class.getDeclaredField(component);
            assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isFinal(field.getModifiers()));
        }
        assertTrue(java.lang.reflect.Modifier.isFinal(Quaternion.class.getModifiers()));
    }

    @Test
    void fromAxisAngleRejectsAZeroAxisForANonZeroRotation() {
        assertThrows(IllegalArgumentException.class,
                () -> Quaternion.fromAxisAngle(0f, 0f, 0f, 1f));
    }

    @Test
    void fromAxisAngleNormalizesTheSuppliedAxis() {
        Quaternion unitAxis = Quaternion.fromAxisAngle(0f, 1f, 0f, 0.75f);
        Quaternion scaledAxis = Quaternion.fromAxisAngle(0f, 25f, 0f, 0.75f);

        assertEquals(unitAxis.x(), scaledAxis.x(), DELTA);
        assertEquals(unitAxis.y(), scaledAxis.y(), DELTA);
        assertEquals(unitAxis.z(), scaledAxis.z(), DELTA);
        assertEquals(unitAxis.w(), scaledAxis.w(), DELTA);
    }

    // -----------------------------------------------------------------------
    // multiply
    // -----------------------------------------------------------------------

    @Test
    void multiply_byIdentity_returnsOriginal() {
        Quaternion identity = new Quaternion(0f, 0f, 0f, 1f);
        Quaternion q = new Quaternion(0.5f, 0.3f, 0.1f, 0.8f);

        Quaternion result = identity.multiply(q);

        assertEquals(q.x(), result.x(), DELTA);
        assertEquals(q.y(), result.y(), DELTA);
        assertEquals(q.z(), result.z(), DELTA);
        assertEquals(q.w(), result.w(), DELTA);
    }

    @Test
    void multiply_identityByQ_returnsQ() {
        Quaternion identity = new Quaternion(0f, 0f, 0f, 1f);
        Quaternion q = new Quaternion(0.5f, 0.3f, 0.1f, 0.8f);

        Quaternion result = q.multiply(identity);

        assertEquals(q.x(), result.x(), DELTA);
        assertEquals(q.y(), result.y(), DELTA);
        assertEquals(q.z(), result.z(), DELTA);
        assertEquals(q.w(), result.w(), DELTA);
    }

    @Test
    void multiply_isNotCommutative() {
        // Quaternion multiplication is generally non-commutative: p*q ≠ q*p
        Quaternion p = Quaternion.fromAxisAngle(1f, 0f, 0f, (float) (Math.PI / 4));
        Quaternion q = Quaternion.fromAxisAngle(0f, 1f, 0f, (float) (Math.PI / 4));

        Quaternion pq = p.multiply(q);
        Quaternion qp = q.multiply(p);

        // At least one component must differ
        boolean differs = Math.abs(pq.x() - qp.x()) > DELTA
                || Math.abs(pq.y() - qp.y()) > DELTA
                || Math.abs(pq.z() - qp.z()) > DELTA
                || Math.abs(pq.w() - qp.w()) > DELTA;
        assertTrue(differs, "Quaternion multiplication must be non-commutative for non-parallel axes");
    }

    // -----------------------------------------------------------------------
    // toMatrix
    // -----------------------------------------------------------------------

    @Test
    void toMatrix_identityQuaternion_returnsIdentityMatrix() {
        PMatrix3D m = new Quaternion(0f, 0f, 0f, 1f).toMatrix();

        assertEquals(1f, m.m00, DELTA);
        assertEquals(0f, m.m01, DELTA);
        assertEquals(0f, m.m02, DELTA);
        assertEquals(1f, m.m11, DELTA);
        assertEquals(0f, m.m12, DELTA);
        assertEquals(1f, m.m22, DELTA);
        assertEquals(0f, m.m03, DELTA);
        assertEquals(0f, m.m13, DELTA);
        assertEquals(0f, m.m23, DELTA);
        assertEquals(1f, m.m33, DELTA);
    }

    @Test
    void toMatrix_180DegAroundY_flipsXandZAxes() {
        // 180° rotation around Y: X → -X, Z → -Z, Y unchanged
        Quaternion q = Quaternion.fromAxisAngle(0f, 1f, 0f, (float) Math.PI).normalized();
        PMatrix3D m = q.toMatrix();

        assertEquals(-1f, m.m00, 1e-4f, "X-axis must be flipped");
        assertEquals( 1f, m.m11, 1e-4f, "Y-axis must be unchanged");
        assertEquals(-1f, m.m22, 1e-4f, "Z-axis must be flipped");
    }

    // -----------------------------------------------------------------------
    // slerp
    // -----------------------------------------------------------------------

    @Test
    void slerp_identicalQuaternions_returnsCopy() {
        Quaternion q = new Quaternion(0f, 0f, 0f, 1f);
        Quaternion result = q.slerp(q, 0.5f);

        assertEquals(q.x(), result.x(), DELTA);
        assertEquals(q.y(), result.y(), DELTA);
        assertEquals(q.z(), result.z(), DELTA);
        assertEquals(q.w(), result.w(), DELTA);
    }

    @Test
    void slerp_atTEqualZero_returnsStartQuaternion() {
        // identity → 90° around Z: t=0 should give identity (or very close)
        Quaternion start = new Quaternion(0f, 0f, 0f, 1f);
        Quaternion end   = Quaternion.fromAxisAngle(0f, 0f, 1f, (float) (Math.PI / 2));

        Quaternion result = start.slerp(end, 0f);

        assertEquals(start.x(), result.x(), DELTA);
        assertEquals(start.y(), result.y(), DELTA);
        assertEquals(start.z(), result.z(), DELTA);
        assertEquals(start.w(), result.w(), DELTA);
    }

    @Test
    void slerp_atHalfway_producesUnitLengthResult() {
        // identity (0,0,0,1) → 180° around Z (0,0,1,0): half-way = 90° around Z
        Quaternion start = new Quaternion(0f, 0f, 0f, 1f);
        Quaternion end   = Quaternion.fromAxisAngle(0f, 0f, 1f, (float) Math.PI);

        Quaternion mid = start.slerp(end, 0.5f);

        float mag = (float) Math.sqrt(mid.x() * mid.x() + mid.y() * mid.y() + mid.z() * mid.z() + mid.w() * mid.w());
        assertEquals(1f, mag, DELTA, "SLERP result must be a unit quaternion");
    }

    @Test
    void slerpTreatsAntipodalQuaternionValuesAsTheSameRotation() {
        Quaternion identity = new Quaternion(0f, 0f, 0f, 1f);
        Quaternion antipodalIdentity = new Quaternion(0f, 0f, 0f, -1f);

        Quaternion result = identity.slerp(antipodalIdentity, 0.5f);

        assertEquals(0f, result.x(), DELTA);
        assertEquals(0f, result.y(), DELTA);
        assertEquals(0f, result.z(), DELTA);
        assertEquals(1f, Math.abs(result.w()), DELTA);
    }
}
