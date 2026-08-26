package com.victorvalentim.zividomelive.core.projection;

import com.victorvalentim.zividomelive.core.math.Quaternion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SphericalOrientationTest {

    private static final float EPSILON = 1.0e-5f;
    private static final float PI = (float) Math.PI;
    private static final float TWO_PI = PI * 2.0f;

    @Test
    void defaultsAndResetProduceIdentity() {
        SphericalOrientation orientation = new SphericalOrientation();
        assertIdentity(orientation);
        orientation.setPitch(0.7f);
        orientation.setYaw(-1.1f);
        orientation.setRoll(0.4f);
        orientation.reset();
        assertIdentity(orientation);
    }

    @Test
    void pitchYawAndRollUseLocalXLocalZLocalY() {
        SphericalOrientation pitch = new SphericalOrientation();
        pitch.setPitch(0.4f);
        assertEquivalent(Quaternion.fromAxisAngle(1.0f, 0.0f, 0.0f, 0.4f),
                pitch.getQuaternion());

        SphericalOrientation yaw = new SphericalOrientation();
        yaw.setYaw(0.4f);
        assertEquivalent(Quaternion.fromAxisAngle(0.0f, 0.0f, 1.0f, 0.4f),
                yaw.getQuaternion());

        SphericalOrientation roll = new SphericalOrientation();
        roll.setRoll(0.4f);
        assertEquivalent(Quaternion.fromAxisAngle(0.0f, 1.0f, 0.0f, 0.4f),
                roll.getQuaternion());
    }

    @Test
    void changesComposeInEventOrder() {
        SphericalOrientation orientation = new SphericalOrientation();
        orientation.setPitch(PI * 0.5f);
        orientation.setYaw(0.35f);
        orientation.setRoll(-0.2f);

        Quaternion expected = Quaternion.fromAxisAngle(1.0f, 0.0f, 0.0f, PI * 0.5f)
                .multiply(Quaternion.fromAxisAngle(0.0f, 0.0f, 1.0f, 0.35f))
                .multiply(Quaternion.fromAxisAngle(0.0f, 1.0f, 0.0f, -0.2f))
                .normalized();
        assertEquivalent(expected, orientation.getQuaternion());
    }

    @Test
    void cyclicBoundaryUsesShortestDelta() {
        SphericalOrientation orientation = new SphericalOrientation();
        orientation.setYaw(PI - 0.01f);
        Quaternion before = orientation.getQuaternion();
        orientation.setYaw(-PI + 0.01f);

        Quaternion expected = before
                .multiply(Quaternion.fromAxisAngle(0.0f, 0.0f, 1.0f, 0.02f))
                .normalized();
        assertEquivalent(expected, orientation.getQuaternion());
    }

    @Test
    void multiTurnControlsArePreservedWhileRotationWraps() {
        SphericalOrientation orientation = new SphericalOrientation();
        float multiTurn = TWO_PI + 0.4f;
        orientation.setPitch(multiTurn);

        assertEquals(multiTurn, orientation.getPitch());
        assertEquivalent(Quaternion.fromAxisAngle(1.0f, 0.0f, 0.0f, 0.4f),
                orientation.getQuaternion());
    }

    @Test
    void nonFiniteControlsAreIgnoredWithoutMutatingAccumulators() {
        SphericalOrientation orientation = new SphericalOrientation();
        orientation.setPitch(0.25f);
        Quaternion before = orientation.getQuaternion();
        orientation.setPitch(Float.NaN);
        orientation.setYaw(Float.POSITIVE_INFINITY);
        orientation.setRoll(Float.NEGATIVE_INFINITY);

        assertEquals(0.25f, orientation.getPitch());
        assertEquals(0.0f, orientation.getYaw());
        assertEquals(0.0f, orientation.getRoll());
        assertEquivalent(before, orientation.getQuaternion());
    }

    @Test
    void longSequencesRemainNormalizedAndEventOrdered() {
        SphericalOrientation orientation = new SphericalOrientation();
        for (int index = 1; index <= 50_000; index++) {
            orientation.setPitch(index * 0.0031f);
            orientation.setYaw(index * -0.0027f);
            orientation.setRoll(index * 0.0019f);
        }

        Quaternion value = orientation.getQuaternion();
        float magnitude = (float) Math.sqrt(value.x() * value.x() + value.y() * value.y()
                + value.z() * value.z() + value.w() * value.w());
        assertEquals(1.0f, magnitude, 2.0e-5f);
    }

    private static void assertIdentity(SphericalOrientation orientation) {
        assertEquals(0.0f, orientation.getPitch());
        assertEquals(0.0f, orientation.getYaw());
        assertEquals(0.0f, orientation.getRoll());
        assertEquivalent(Quaternion.identity(), orientation.getQuaternion());
    }

    private static void assertEquivalent(Quaternion expected, Quaternion actual) {
        float dot = Math.abs(expected.x() * actual.x() + expected.y() * actual.y()
                + expected.z() * actual.z() + expected.w() * actual.w());
        assertEquals(1.0f, dot, EPSILON);
    }
}
