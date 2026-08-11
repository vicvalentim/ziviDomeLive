package com.victorvalentim.zividomelive.render;

import org.junit.jupiter.api.Test;
import processing.core.PConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SphericalOrientationTest {
    private static final float EPSILON = 1.0e-5f;

    @Test
    void startsAsIdentityQuaternion() {
        SphericalOrientation orientation = new SphericalOrientation();

        assertQuaternionEquals(
                new Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                orientation.getQuaternion());
        assertEquals(0.0f, orientation.getPitch());
        assertEquals(0.0f, orientation.getYaw());
        assertEquals(0.0f, orientation.getRoll());
    }

    @Test
    void composesControlChangesDirectlyInEventOrder() {
        SphericalOrientation orientation = new SphericalOrientation();
        orientation.setPitch(PConstants.HALF_PI);
        orientation.setYaw(0.35f);
        orientation.setRoll(-0.2f);

        Quaternion expected = Quaternion.fromAxisAngle(1.0f, 0.0f, 0.0f, PConstants.HALF_PI)
                .multiply(Quaternion.fromAxisAngle(0.0f, 0.0f, 1.0f, 0.35f))
                .multiply(Quaternion.fromAxisAngle(0.0f, 1.0f, 0.0f, -0.2f))
                .normalize();

        assertQuaternionEquals(expected, orientation.getQuaternion());
    }

    @Test
    void yawAndRollRemainIndependentAtNinetyDegreePitch() {
        SphericalOrientation yawChange = new SphericalOrientation();
        yawChange.setPitch(PConstants.HALF_PI);
        yawChange.setYaw(0.25f);

        SphericalOrientation rollChange = new SphericalOrientation();
        rollChange.setPitch(PConstants.HALF_PI);
        rollChange.setRoll(0.25f);

        Quaternion yawQuaternion = yawChange.getQuaternion();
        Quaternion rollQuaternion = rollChange.getQuaternion();
        float dot = Math.abs(
                yawQuaternion.x * rollQuaternion.x
                        + yawQuaternion.y * rollQuaternion.y
                        + yawQuaternion.z * rollQuaternion.z
                        + yawQuaternion.w * rollQuaternion.w);

        assertNotEquals(1.0f, dot, EPSILON);
    }

    @Test
    void cyclicBoundaryUsesTheShortestQuaternionDelta() {
        SphericalOrientation orientation = new SphericalOrientation();
        orientation.setYaw(PConstants.PI - 0.01f);
        Quaternion beforeWrap = orientation.getQuaternion();

        orientation.setYaw(-PConstants.PI + 0.01f);

        Quaternion expected = beforeWrap
                .multiply(Quaternion.fromAxisAngle(0.0f, 0.0f, 1.0f, 0.02f))
                .normalize();
        assertQuaternionEquivalent(expected, orientation.getQuaternion());
    }

    @Test
    void gettersPreserveMultiTurnFacadeValues() {
        SphericalOrientation orientation = new SphericalOrientation();
        float multiTurnPitch = PConstants.TWO_PI + 0.4f;

        orientation.setPitch(multiTurnPitch);

        assertEquals(multiTurnPitch, orientation.getPitch());
        assertQuaternionEquivalent(
                Quaternion.fromAxisAngle(1.0f, 0.0f, 0.0f, 0.4f),
                orientation.getQuaternion());
    }

    @Test
    void resetRestoresIdentityWithoutReversingEulerControls() {
        SphericalOrientation orientation = new SphericalOrientation();
        orientation.setPitch(0.7f);
        orientation.setYaw(-1.1f);
        orientation.setRoll(0.4f);

        orientation.reset();

        assertQuaternionEquals(
                new Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                orientation.getQuaternion());
        assertEquals(0.0f, orientation.getPitch());
        assertEquals(0.0f, orientation.getYaw());
        assertEquals(0.0f, orientation.getRoll());
    }

    private static void assertQuaternionEquals(Quaternion expected, Quaternion actual) {
        assertEquals(expected.x, actual.x, EPSILON);
        assertEquals(expected.y, actual.y, EPSILON);
        assertEquals(expected.z, actual.z, EPSILON);
        assertEquals(expected.w, actual.w, EPSILON);
        assertUnit(actual);
    }

    private static void assertQuaternionEquivalent(Quaternion expected, Quaternion actual) {
        float dot = Math.abs(
                expected.x * actual.x
                        + expected.y * actual.y
                        + expected.z * actual.z
                        + expected.w * actual.w);
        assertEquals(1.0f, dot, EPSILON);
        assertUnit(actual);
    }

    private static void assertUnit(Quaternion quaternion) {
        float magnitude = (float) Math.sqrt(
                quaternion.x * quaternion.x
                        + quaternion.y * quaternion.y
                        + quaternion.z * quaternion.z
                        + quaternion.w * quaternion.w);
        assertEquals(1.0f, magnitude, EPSILON);
    }
}
