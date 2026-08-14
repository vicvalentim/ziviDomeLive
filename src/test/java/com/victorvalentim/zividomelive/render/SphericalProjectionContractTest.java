package com.victorvalentim.zividomelive.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SphericalProjectionContractTest {
	private static final double EPSILON = 1.0e-6;

	@Test
	void equirectangularCardinalsPreserveFrontAndTopContract() {
		assertAll("qualified equirectangular directions",
				() -> assertDirection(equirectangularDirection(0.50, 0.50), 0.0, 0.0, 1.0),
				() -> assertDirection(equirectangularDirection(0.25, 0.50), -1.0, 0.0, 0.0),
				() -> assertDirection(equirectangularDirection(0.75, 0.50), 1.0, 0.0, 0.0),
				() -> assertDirection(equirectangularDirection(0.50, 0.00), 0.0, 1.0, 0.0),
				() -> assertDirection(equirectangularDirection(0.50, 1.00), 0.0, -1.0, 0.0));
	}

	@Test
	void fisheyeAtOneHundredEightyDegreesPreservesFrontAndCardinalAxes() {
		assertAll("qualified fisheye directions",
				() -> assertDirection(fisheyeDirection(0.0, 0.0, 180.0), 0.0, 0.0, 1.0),
				() -> assertDirection(fisheyeDirection(1.0, 0.0, 180.0), 1.0, 0.0, 0.0),
				() -> assertDirection(fisheyeDirection(-1.0, 0.0, 180.0), -1.0, 0.0, 0.0),
				() -> assertDirection(fisheyeDirection(0.0, -1.0, 180.0), 0.0, 1.0, 0.0),
				() -> assertDirection(fisheyeDirection(0.0, 1.0, 180.0), 0.0, -1.0, 0.0));
	}

	@Test
	void environmentLookupIsTheInverseOfTheQualifiedEquirectangularProjection() {
		assertAll("qualified environment lookup",
				() -> assertUv(environmentUv(new double[]{0.0, 0.0, 1.0}, 0.0), 0.50, 0.50),
				() -> assertUv(environmentUv(new double[]{-1.0, 0.0, 0.0}, 0.0), 0.25, 0.50),
				() -> assertUv(environmentUv(new double[]{1.0, 0.0, 0.0}, 0.0), 0.75, 0.50),
				() -> assertEquals(0.0, environmentUv(new double[]{0.0, 1.0, 0.0}, 0.0)[1], EPSILON),
				() -> assertEquals(1.0, environmentUv(new double[]{0.0, -1.0, 0.0}, 0.0)[1], EPSILON),
				() -> assertUv(environmentUv(new double[]{0.0, 0.0, 1.0}, Math.PI * 0.5), 0.25, 0.50));
	}

	@Test
	void directionsAcrossNegativeZAreAdjacentAcrossThePeriodicLongitudeSeam() {
		double epsilon = 1.0e-5;
		double leftU = environmentUv(new double[]{-epsilon, 0.0, -1.0}, 0.0)[0];
		double rightU = environmentUv(new double[]{epsilon, 0.0, -1.0}, 0.0)[0];
		double periodicDistance = Math.min(
				Math.abs(leftU - rightU),
				1.0 - Math.abs(leftU - rightU));

		assertTrue(leftU < 0.01 || leftU > 0.99);
		assertTrue(rightU < 0.01 || rightU > 0.99);
		assertTrue(periodicDistance < epsilon,
				"Longitude must wrap instead of clamping at the -Z seam");
	}

	private static double[] equirectangularDirection(double u, double v) {
		double theta = u * 2.0 * Math.PI;
		double phi = v * Math.PI;
		double sinPhi = Math.sin(phi);

		return new double[]{
				-sinPhi * Math.sin(theta),
				Math.cos(phi),
				-sinPhi * Math.cos(theta)};
	}

	/**
	 * Uses Processing output coordinates: {@code y=-1} is the visible top of the target.
	 */
	private static double[] fisheyeDirection(
			double x,
			double y,
			double fieldOfViewDegrees) {
		double radius = Math.hypot(x, y);
		double phi = Math.atan2(y, x);
		double theta = radius * Math.toRadians(fieldOfViewDegrees) * 0.5;

		return new double[]{
				Math.sin(theta) * Math.cos(phi),
				-Math.sin(theta) * Math.sin(phi),
				Math.cos(theta)};
	}

	private static double[] environmentUv(double[] direction, double yawOffset) {
		double length = Math.sqrt(
				direction[0] * direction[0]
						+ direction[1] * direction[1]
						+ direction[2] * direction[2]);
		double x = direction[0] / length;
		double y = direction[1] / length;
		double z = direction[2] / length;
		double theta = Math.atan2(-x, -z) - yawOffset;
		double u = theta / (2.0 * Math.PI);
		u -= Math.floor(u);
		double v = Math.acos(Math.max(-1.0, Math.min(1.0, y))) / Math.PI;
		return new double[]{u, v};
	}

	private static void assertDirection(
			double[] actual,
			double expectedX,
			double expectedY,
			double expectedZ) {
		assertEquals(expectedX, actual[0], EPSILON, "x");
		assertEquals(expectedY, actual[1], EPSILON, "y");
		assertEquals(expectedZ, actual[2], EPSILON, "z");
	}

	private static void assertUv(
			double[] actual,
			double expectedU,
			double expectedV) {
		assertEquals(expectedU, actual[0], EPSILON, "u");
		assertEquals(expectedV, actual[1], EPSILON, "v");
	}
}
