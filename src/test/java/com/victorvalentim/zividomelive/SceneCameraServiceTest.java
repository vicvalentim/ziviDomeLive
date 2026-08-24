package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.render.Quaternion;
import com.victorvalentim.zividomelive.render.camera.OrbitCamera;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PMatrix3D;
import processing.core.PVector;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SceneCameraServiceTest {

    private static final float EPSILON = 1.0e-4f;

    @Test
    void viewLightStartsAtCameraAndPointsAtCurrentTarget() {
        OrbitCamera camera = new OrbitCamera(new PVector(10f, 20f, 30f), 100f);
        PVector position = new PVector();
        PVector direction = new PVector();

        SceneCameraService.calculateViewLightPose(
                camera, position, direction, new PMatrix3D());

        assertVector(position, 10f, 20f, 130f);
        assertVector(direction, 0f, 0f, -1f);
    }

    @Test
    void viewLightFollowsOrbitOrientationWithoutChangingItsUnitDirection() {
        OrbitCamera camera = new OrbitCamera(100f);
        camera.setOrientationImmediate(
                Quaternion.fromAxisAngle(0f, 1f, 0f, PConstants.HALF_PI));
        PVector position = new PVector();
        PVector direction = new PVector();

        SceneCameraService.calculateViewLightPose(
                camera, position, direction, new PMatrix3D());

        assertVector(position, -100f, 0f, 0f);
        assertVector(direction, 1f, 0f, 0f);
        assertEquals(1f, direction.mag(), EPSILON);
    }

    @Test
    void negativeOrbitDistanceStillAimsFromCameraTowardTarget() {
        OrbitCamera camera = new OrbitCamera(100f);
        camera.setDistanceLimits(-1000f, 1000f);
        camera.setDistanceImmediate(-100f);
        PVector position = new PVector();
        PVector direction = new PVector();

        SceneCameraService.calculateViewLightPose(
                camera, position, direction, new PMatrix3D());

        assertVector(position, 0f, 0f, -100f);
        assertVector(direction, 0f, 0f, 1f);
    }

    @Test
    void rootServiceBuildsAnAxisAnglePoseWithoutArtistQuaternionImports() {
        SceneCameraService service = new SceneCameraService(new ziviDomeLive(new PApplet()));

        service.setDistanceLimits(-1000f, 1000f);
        service.snapToAxisAngle(10f, 20f, 30f, 0f, 1f, 0f, PConstants.HALF_PI, -250f);

        assertVector(service.orbit().getTarget(), 10f, 20f, 30f);
        assertEquals(-250f, service.orbit().getDistance(), EPSILON);
        float sqrtHalf = (float) Math.sqrt(0.5);
        assertEquals(sqrtHalf, service.orbit().getOrientation().y(), EPSILON);
        assertEquals(sqrtHalf, service.orbit().getOrientation().w(), EPSILON);
    }

    private static void assertVector(PVector actual, float x, float y, float z) {
        assertEquals(x, actual.x, EPSILON);
        assertEquals(y, actual.y, EPSILON);
        assertEquals(z, actual.z, EPSILON);
    }
}
