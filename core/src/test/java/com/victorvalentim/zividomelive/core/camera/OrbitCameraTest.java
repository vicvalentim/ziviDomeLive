package com.victorvalentim.zividomelive.core.camera;

import com.victorvalentim.zividomelive.core.math.Quaternion;
import com.victorvalentim.zividomelive.core.math.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrbitCameraTest {

    private static final float EPSILON = 1.0e-4f;

    @Test
    void defaultsAndInitialTargetMatchGoldenCamera() {
        OrbitCamera defaultCamera = new OrbitCamera();
        assertEquals(1500.0f, defaultCamera.getDistance());
        assertEquals(Vec3.zero(), defaultCamera.getTarget());

        Vec3 target = new Vec3(1.0f, 2.0f, 3.0f);
        OrbitCamera camera = new OrbitCamera(target, -250.0f);
        assertEquals(target, camera.getTarget());
        assertEquals(target, camera.getGoalTarget());
        assertEquals(-250.0f, camera.getDistance());
        assertEquals(-250.0f, camera.getGoalDistance());
    }

    @Test
    void signedNegativeAndPositiveDistanceRangesArePreserved() {
        OrbitCamera camera = new OrbitCamera(-100.0f);
        camera.setDistanceLimits(-1000.0f, 1000.0f);
        assertEquals(-100.0f, camera.getDistance());
        camera.setDistanceImmediate(100.0f);
        assertEquals(100.0f, camera.getDistance());
        camera.setDistanceImmediate(-100.0f);
        assertEquals(-100.0f, camera.getDistance());
    }

    @Test
    void zoomClampsToDistanceLimits() {
        OrbitCamera camera = new OrbitCamera(100.0f);
        camera.setDistanceLimits(10.0f, 200.0f);
        camera.zoom(10_000.0f);
        settle(camera);
        assertEquals(200.0f, camera.getDistance(), EPSILON);
        camera.zoom(-10_000.0f);
        settle(camera);
        assertEquals(10.0f, camera.getDistance(), EPSILON);
    }

    @Test
    void smoothZoomCannotCrossZeroWhenCollapseGuardIsEnabled() {
        OrbitCamera positive = new OrbitCamera(50.0f);
        positive.setDistanceLimits(-1000.0f, 1000.0f);
        positive.setCollapseGuard(5.0f);
        positive.zoom(-100.0f);
        settle(positive);
        assertEquals(5.0f, positive.getDistance(), EPSILON);

        OrbitCamera negative = new OrbitCamera(-50.0f);
        negative.setDistanceLimits(-1000.0f, 1000.0f);
        negative.setCollapseGuard(5.0f);
        negative.zoom(100.0f);
        settle(negative);
        assertEquals(-5.0f, negative.getDistance(), EPSILON);
    }

    @Test
    void collapseGuardPushesDistanceOutsideDeadZone() {
        OrbitCamera camera = new OrbitCamera(2.0f);
        camera.setDistanceLimits(-1000.0f, 1000.0f);
        camera.setCollapseGuard(10.0f);
        assertEquals(10.0f, camera.getDistance());
        camera.setDistance(-2.0f);
        settle(camera);
        assertEquals(-10.0f, camera.getDistance(), EPSILON);
    }

    @Test
    void programmaticPoseInterpolatesExactlyOncePerUpdate() {
        OrbitCamera camera = new OrbitCamera(100.0f);
        camera.setLerpFactor(0.15f);
        Quaternion goal = Quaternion.fromAxisAngle(0.0f, 1.0f, 0.0f, 0.75f);
        camera.goTo(new Vec3(10.0f, 20.0f, 30.0f), goal, 200.0f);

        assertEquals(Vec3.zero(), camera.getTarget());
        assertEquals(100.0f, camera.getDistance());
        camera.update();
        assertVector(camera.getTarget(), 1.5f, 3.0f, 4.5f);
        assertEquals(115.0f, camera.getDistance(), EPSILON);
        assertTrue(Math.abs(dot(Quaternion.identity(), camera.getOrientation())) < 1.0f);
        assertTrue(Math.abs(dot(goal, camera.getOrientation())) < 1.0f);
    }

    @Test
    void snapAndImmediateManipulationSynchronizeEveryGoal() {
        OrbitCamera camera = new OrbitCamera(100.0f);
        Quaternion initial = Quaternion.fromAxisAngle(1.0f, 0.0f, 0.0f, 0.5f);
        camera.snapTo(new Vec3(4.0f, 5.0f, 6.0f), initial, -75.0f);
        assertEquals(camera.getPose(), camera.getGoalPose());

        camera.rotateAroundImmediate(0.0f, 1.0f, 0.0f, 0.25f);
        camera.zoomImmediate(5.0f);
        camera.setTargetImmediate(new Vec3(7.0f, 8.0f, 9.0f));
        CameraPose beforeUpdate = camera.getPose();
        camera.update();

        assertEquals(beforeUpdate, camera.getPose());
        assertEquals(camera.getPose(), camera.getGoalPose());
        Quaternion expected = Quaternion.fromAxisAngle(0.0f, 1.0f, 0.0f, 0.25f)
                .multiply(initial).normalized();
        assertEquivalent(expected, camera.getOrientation());
    }

    @Test
    void smoothWorldSpaceRotationUsesLeftMultiplication() {
        OrbitCamera camera = new OrbitCamera(100.0f);
        Quaternion x = Quaternion.fromAxisAngle(1.0f, 0.0f, 0.0f, 0.5f);
        camera.setOrientationImmediate(x);
        camera.rotateAround(0.0f, 1.0f, 0.0f, 0.25f);

        Quaternion expectedGoal = Quaternion.fromAxisAngle(0.0f, 1.0f, 0.0f, 0.25f)
                .multiply(x).normalized();
        assertEquivalent(expectedGoal, camera.getGoalOrientation());
        assertTrue(Math.abs(dot(expectedGoal, camera.getOrientation())) < 1.0f);
        settle(camera);
        assertEquivalent(expectedGoal, camera.getOrientation());
    }

    @Test
    void updateAtRestReusesImmutableOrientation() {
        OrbitCamera camera = new OrbitCamera(100.0f);
        Quaternion before = camera.getOrientation();
        camera.update();
        assertSame(before, camera.getOrientation());
    }

    @Test
    void goToAndSnapAcceptImmutablePoseValues() {
        CameraPose pose = new CameraPose(
                new Vec3(1.0f, 2.0f, 3.0f),
                Quaternion.fromAxisAngle(0.0f, 0.0f, 1.0f, 0.3f),
                -300.0f);
        OrbitCamera smooth = new OrbitCamera(100.0f);
        smooth.setDistanceLimits(-1000.0f, 1000.0f);
        smooth.goTo(pose);
        settle(smooth);
        assertPoseEquivalent(pose, smooth.getPose());

        OrbitCamera snapped = new OrbitCamera(100.0f);
        snapped.setDistanceLimits(-1000.0f, 1000.0f);
        snapped.snapTo(pose);
        assertPoseEquivalent(pose, snapped.getPose());
        assertNotSame(pose, snapped.getPose());
    }

    @Test
    void resetRestoresOriginIdentityAndRequestedDistance() {
        OrbitCamera camera = new OrbitCamera(100.0f);
        camera.snapTo(1.0f, 2.0f, 3.0f,
                Quaternion.fromAxisAngle(0.0f, 1.0f, 0.0f, 0.7f), 200.0f);
        camera.reset(300.0f);
        assertEquals(Vec3.zero(), camera.getTarget());
        assertEquivalent(Quaternion.identity(), camera.getOrientation());
        assertEquals(300.0f, camera.getDistance());
    }

    @Test
    void configurationIsFiniteOrderedAndClamped() {
        OrbitCamera camera = new OrbitCamera(100.0f);
        camera.setLerpFactor(0.0f);
        assertEquals(0.001f, camera.getLerpFactor());
        camera.setLerpFactor(2.0f);
        assertEquals(1.0f, camera.getLerpFactor());
        camera.setCollapseGuard(-5.0f);
        assertEquals(0.0f, camera.getCollapseGuard());

        assertThrows(IllegalArgumentException.class,
                () -> camera.setDistanceLimits(10.0f, -10.0f));
        assertThrows(IllegalArgumentException.class,
                () -> camera.setDistanceLimits(Float.NaN, 10.0f));
        assertThrows(IllegalArgumentException.class,
                () -> camera.setCollapseGuard(Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> camera.setLerpFactor(Float.NaN));
    }

    @Test
    void invalidPoseAndControlValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new OrbitCamera(Float.NaN));
        assertThrows(NullPointerException.class, () -> new OrbitCamera(null, 1.0f));
        assertThrows(IllegalArgumentException.class,
                () -> new Vec3(Float.NaN, 0.0f, 0.0f));
        OrbitCamera camera = new OrbitCamera();
        assertThrows(IllegalArgumentException.class, () -> camera.setDistance(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> camera.zoom(Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> camera.setTarget(0.0f, Float.POSITIVE_INFINITY, 0.0f));
        assertThrows(IllegalArgumentException.class, () -> camera.setOrientation(null));
        assertThrows(NullPointerException.class,
                () -> camera.goTo(null, Quaternion.identity(), 1.0f));
    }

    private static void settle(OrbitCamera camera) {
        for (int index = 0; index < 200; index++) {
            camera.update();
        }
    }

    private static void assertPoseEquivalent(CameraPose expected, CameraPose actual) {
        assertVector(actual.target(), expected.target().x(), expected.target().y(), expected.target().z());
        assertEquivalent(expected.orientation(), actual.orientation());
        assertEquals(expected.distance(), actual.distance(), EPSILON);
    }

    private static void assertVector(Vec3 actual, float x, float y, float z) {
        assertEquals(x, actual.x(), EPSILON);
        assertEquals(y, actual.y(), EPSILON);
        assertEquals(z, actual.z(), EPSILON);
    }

    private static void assertEquivalent(Quaternion expected, Quaternion actual) {
        assertEquals(1.0f, Math.abs(dot(expected, actual)), EPSILON);
    }

    private static float dot(Quaternion left, Quaternion right) {
        return left.x() * right.x() + left.y() * right.y()
                + left.z() * right.z() + left.w() * right.w();
    }
}
