package com.victorvalentim.zividomelive.render.camera;

import com.victorvalentim.zividomelive.render.Quaternion;
import org.junit.jupiter.api.Test;
import processing.core.PVector;

import static org.junit.jupiter.api.Assertions.*;

class OrbitCameraTest {

	private static final float EPSILON = 1e-4f;

	@Test
	void defaultConstructorUsesDefaultDistance() {
		OrbitCamera camera = new OrbitCamera();
		assertEquals(1500f, camera.getDistance(), EPSILON);
	}

	@Test
	void targetConstructorCopiesInitialTarget() {
		PVector initialTarget = new PVector(1f, 2f, 3f);
		OrbitCamera camera = new OrbitCamera(initialTarget, 250f);
		initialTarget.set(9f, 9f, 9f);

		assertEquals(250f, camera.getDistance(), EPSILON);
		assertEquals(1f, camera.getTarget().x, EPSILON);
		assertEquals(2f, camera.getTarget().y, EPSILON);
		assertEquals(3f, camera.getTarget().z, EPSILON);
	}

	@Test
	void zoomIsClampedToDistanceLimits() {
		OrbitCamera camera = new OrbitCamera(100f);
		camera.setDistanceLimits(10f, 200f);

		camera.zoom(10000f);
		settle(camera);
		assertEquals(200f, camera.getDistance(), EPSILON);

		camera.zoom(-10000f);
		settle(camera);
		assertEquals(10f, camera.getDistance(), EPSILON);
	}

	@Test
	void collapseGuardPreventsCrossingZero() {
		OrbitCamera camera = new OrbitCamera(50f);
		camera.setDistanceLimits(-1000f, 1000f);
		camera.setCollapseGuard(5f);

		// Attempt to zoom through zero from the positive side.
		camera.zoom(-100f);
		settle(camera);
		assertEquals(5f, camera.getDistance(), EPSILON,
				"Distance should stop at the collapse guard boundary instead of crossing zero");
	}

	@Test
	void collapseGuardKeepsDistanceOutsideDeadZone() {
		OrbitCamera camera = new OrbitCamera(50f);
		camera.setDistanceLimits(-1000f, 1000f);
		camera.setCollapseGuard(10f);

		camera.setDistance(2f); // inside the dead zone
		settle(camera);
		assertEquals(10f, camera.getDistance(), EPSILON);
	}

	@Test
	void snapToAppliesImmediately() {
		OrbitCamera camera = new OrbitCamera(100f);
		Quaternion q = new Quaternion(0, 0, 0, 1);
		camera.snapTo(1f, 2f, 3f, q, 42f);

		assertEquals(42f, camera.getDistance(), EPSILON);
		assertEquals(1f, camera.getTarget().x, EPSILON);
		assertEquals(2f, camera.getTarget().y, EPSILON);
		assertEquals(3f, camera.getTarget().z, EPSILON);
	}

	@Test
	void goToInterpolatesTheWholePoseWithoutMutatingInputs() {
		OrbitCamera camera = new OrbitCamera(100f);
		PVector target = new PVector(10f, 20f, 30f);
		Quaternion orientation = Quaternion.fromAxisAngle(0f, 1f, 0f, 0.75f);

		camera.goTo(target, orientation, 200f);
		target.set(99f, 99f, 99f);
		settle(camera);

		assertEquals(10f, camera.getTarget().x, EPSILON);
		assertEquals(20f, camera.getTarget().y, EPSILON);
		assertEquals(30f, camera.getTarget().z, EPSILON);
		assertEquals(200f, camera.getDistance(), EPSILON);
		assertQuaternionEquivalent(orientation, camera.getOrientation());
	}

	@Test
	void immediateComponentSettersSynchronizeCurrentAndGoalPose() {
		OrbitCamera camera = new OrbitCamera(100f);
		Quaternion orientation = Quaternion.fromAxisAngle(1f, 0f, 0f, 0.5f);

		camera.setTargetImmediate(new PVector(4f, 5f, 6f));
		camera.setOrientationImmediate(orientation);
		camera.setDistanceImmediate(75f);
		camera.update();

		assertEquals(4f, camera.getTarget().x, EPSILON);
		assertEquals(5f, camera.getTarget().y, EPSILON);
		assertEquals(6f, camera.getTarget().z, EPSILON);
		assertEquals(75f, camera.getDistance(), EPSILON);
		assertQuaternionEquivalent(orientation, camera.getOrientation());
	}

	@Test
	void vectorRotationOverloadsSupportSmoothAndImmediateMotion() {
		OrbitCamera camera = new OrbitCamera(100f);
		PVector yAxis = new PVector(0f, 1f, 0f);

		camera.rotateAroundImmediate(yAxis, 0.25f);
		Quaternion immediate = camera.getOrientation();
		assertQuaternionEquivalent(
				Quaternion.fromAxisAngle(yAxis, 0.25f),
				immediate);

		camera.rotateAround(yAxis, 0.25f);
		settle(camera);
		assertQuaternionEquivalent(
				Quaternion.fromAxisAngle(yAxis, 0.5f),
				camera.getOrientation());
	}

	@Test
	void resetRestoresOriginAndDistance() {
		OrbitCamera camera = new OrbitCamera(100f);
		camera.setTarget(5f, 5f, 5f);
		camera.zoom(50f);
		settle(camera);

		camera.reset(300f);
		assertEquals(300f, camera.getDistance(), EPSILON);
		assertEquals(0f, camera.getTarget().x, EPSILON);
		assertEquals(0f, camera.getTarget().y, EPSILON);
		assertEquals(0f, camera.getTarget().z, EPSILON);
		Quaternion orientation = camera.getOrientation();
		assertEquals(0f, orientation.x, EPSILON);
		assertEquals(0f, orientation.y, EPSILON);
		assertEquals(0f, orientation.z, EPSILON);
		assertEquals(1f, Math.abs(orientation.w), EPSILON);
	}

	private static void settle(OrbitCamera camera) {
		for (int i = 0; i < 200; i++) {
			camera.update();
		}
	}

	private static void assertQuaternionEquivalent(Quaternion expected, Quaternion actual) {
		float dot = Math.abs(
				expected.x * actual.x
						+ expected.y * actual.y
						+ expected.z * actual.z
						+ expected.w * actual.w);
		assertEquals(1f, dot, EPSILON);
	}
}
