package com.victorvalentim.zividomelive.core.math;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuaternionTest {

    private static final float EPSILON = 1.0e-5f;

    @Test
    void identityAndComponentsAreImmutable() throws Exception {
        Quaternion identity = Quaternion.identity();
        assertQuaternion(identity, 0.0f, 0.0f, 0.0f, 1.0f);
        assertTrue(Modifier.isFinal(Quaternion.class.getModifiers()));
        for (String name : new String[]{"x", "y", "z", "w"}) {
            int modifiers = Quaternion.class.getDeclaredField(name).getModifiers();
            assertTrue(Modifier.isPrivate(modifiers));
            assertTrue(Modifier.isFinal(modifiers));
        }
    }

    @Test
    void axisAngleNormalizesAxisAndZeroAngleIsIdentity() {
        assertSame(Quaternion.identity(), Quaternion.fromAxisAngle(0.0f, 0.0f, 0.0f, 0.0f));
        Quaternion unit = Quaternion.fromAxisAngle(0.0f, 1.0f, 0.0f, 0.75f);
        Quaternion scaled = Quaternion.fromAxisAngle(0.0f, 25.0f, 0.0f, 0.75f);
        assertQuaternionEquals(unit, scaled);
    }

    @Test
    void axisAngleHasQualifiedFloatComponents() {
        float halfSqrtTwo = (float) (Math.sqrt(2.0) / 2.0);
        Quaternion quarterTurn = Quaternion.fromAxisAngle(
                0.0f, 0.0f, 1.0f, (float) (Math.PI / 2.0));
        assertQuaternion(quarterTurn, 0.0f, 0.0f, halfSqrtTwo, halfSqrtTwo);

        Quaternion halfTurn = Quaternion.fromAxisAngle(0.0f, 1.0f, 0.0f, (float) Math.PI);
        assertQuaternion(halfTurn, 0.0f, 1.0f, 0.0f, 0.0f, 1.0e-4f);
    }

    @Test
    void multiplicationOrderIsPreservedAndNonCommutative() {
        Quaternion x = Quaternion.fromAxisAngle(1.0f, 0.0f, 0.0f, 0.5f);
        Quaternion y = Quaternion.fromAxisAngle(0.0f, 1.0f, 0.0f, 0.25f);
        Quaternion xy = x.multiply(y);
        Quaternion yx = y.multiply(x);

        assertNotEquals(xy.z(), yx.z(), EPSILON);
        assertQuaternionEquals(x, Quaternion.identity().multiply(x));
        assertQuaternionEquals(x, x.multiply(Quaternion.identity()));
        assertQuaternion(xy, 0.24547364f, 0.12079889f, 0.030845024f, 0.96135264f);
    }

    @Test
    void normalizationProducesUnitLengthAndRejectsZero() {
        Quaternion value = new Quaternion(3.0f, 0.0f, 0.0f, 4.0f);
        Quaternion normalized = value.normalized();
        assertQuaternion(normalized, 0.6f, 0.0f, 0.0f, 0.8f);
        assertUnit(normalized);
        assertSame(Quaternion.identity(), Quaternion.identity().normalized());
        assertThrows(IllegalStateException.class,
                () -> new Quaternion(0.0f, 0.0f, 0.0f, 0.0f).normalized());
    }

    @Test
    void slerpHonorsEndpointsAndClampsFactor() {
        Quaternion start = Quaternion.identity();
        Quaternion end = Quaternion.fromAxisAngle(0.0f, 0.0f, 1.0f, 1.0f);
        assertSame(start, start.slerp(end, 0.0f));
        assertQuaternionEquals(start, start.slerp(end, -5.0f));
        assertQuaternionEquals(end, start.slerp(end, 1.0f));
        assertQuaternionEquals(end, start.slerp(end, 5.0f));
    }

    @Test
    void slerpUsesShortestPathForAntipodalTarget() {
        Quaternion start = Quaternion.fromAxisAngle(1.0f, 0.0f, 0.0f, 0.4f);
        Quaternion antipodal = new Quaternion(-start.x(), -start.y(), -start.z(), -start.w());

        Quaternion result = start.slerp(antipodal, 0.5f);

        assertSame(start, result);
        assertQuaternionEquivalent(start, result);
    }

    @Test
    void slerpHalfwayProducesExpectedUnitRotation() {
        Quaternion start = Quaternion.identity();
        Quaternion end = Quaternion.fromAxisAngle(0.0f, 1.0f, 0.0f, (float) Math.PI);

        Quaternion halfway = start.slerp(end, 0.5f);

        assertQuaternionEquivalent(
                Quaternion.fromAxisAngle(0.0f, -1.0f, 0.0f, (float) (Math.PI / 2.0)),
                halfway);
        assertUnit(halfway);
    }

    @Test
    void invalidValuesAndOperandsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Quaternion(Float.NaN, 0.0f, 0.0f, 1.0f));
        assertThrows(IllegalArgumentException.class,
                () -> Quaternion.fromAxisAngle(0.0f, 0.0f, 0.0f, 1.0f));
        assertThrows(IllegalArgumentException.class,
                () -> Quaternion.fromAxisAngle(1.0f, 0.0f, 0.0f, Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> Quaternion.identity().multiply(null));
        assertThrows(IllegalArgumentException.class, () -> Quaternion.identity().slerp(null, 0.5f));
        assertThrows(IllegalArgumentException.class,
                () -> Quaternion.identity().slerp(Quaternion.identity(), Float.NaN));
    }

    @Test
    void allNonFiniteComponentsAxesAnglesAndFactorsRejectConsistently() {
        for (float invalid : new float[]{
                Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Quaternion(invalid, 0.0f, 0.0f, 1.0f));
            assertThrows(IllegalArgumentException.class,
                    () -> new Quaternion(0.0f, invalid, 0.0f, 1.0f));
            assertThrows(IllegalArgumentException.class,
                    () -> new Quaternion(0.0f, 0.0f, invalid, 1.0f));
            assertThrows(IllegalArgumentException.class,
                    () -> new Quaternion(0.0f, 0.0f, 0.0f, invalid));
            assertThrows(IllegalArgumentException.class,
                    () -> Quaternion.fromAxisAngle(invalid, 0.0f, 0.0f, 0.0f));
            assertThrows(IllegalArgumentException.class,
                    () -> Quaternion.fromAxisAngle(1.0f, 0.0f, 0.0f, invalid));
            assertThrows(IllegalArgumentException.class,
                    () -> Quaternion.identity().slerp(Quaternion.identity(), invalid));
        }
    }

    @Test
    void zeroAngleWithFiniteZeroAxisIsIdentityButNonZeroAngleRejectsTheAxis() {
        assertSame(Quaternion.identity(),
                Quaternion.fromAxisAngle(-0.0f, 0.0f, 0.0f, -0.0f));
        assertThrows(IllegalArgumentException.class,
                () -> Quaternion.fromAxisAngle(-0.0f, 0.0f, 0.0f, Float.MIN_VALUE));
    }

    private static void assertUnit(Quaternion value) {
        float magnitude = (float) Math.sqrt(value.x() * value.x() + value.y() * value.y()
                + value.z() * value.z() + value.w() * value.w());
        assertEquals(1.0f, magnitude, EPSILON);
    }

    private static void assertQuaternionEquals(Quaternion expected, Quaternion actual) {
        assertQuaternion(actual, expected.x(), expected.y(), expected.z(), expected.w());
    }

    private static void assertQuaternionEquivalent(Quaternion expected, Quaternion actual) {
        float dot = Math.abs(expected.x() * actual.x() + expected.y() * actual.y()
                + expected.z() * actual.z() + expected.w() * actual.w());
        assertEquals(1.0f, dot, EPSILON);
    }

    private static void assertQuaternion(
            Quaternion actual, float x, float y, float z, float w) {
        assertQuaternion(actual, x, y, z, w, EPSILON);
    }

    private static void assertQuaternion(
            Quaternion actual, float x, float y, float z, float w, float epsilon) {
        assertEquals(x, actual.x(), epsilon);
        assertEquals(y, actual.y(), epsilon);
        assertEquals(z, actual.z(), epsilon);
        assertEquals(w, actual.w(), epsilon);
    }
}
