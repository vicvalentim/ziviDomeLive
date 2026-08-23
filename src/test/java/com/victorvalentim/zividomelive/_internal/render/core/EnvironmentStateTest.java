package com.victorvalentim.zividomelive;

// Package-private implementation test grouped physically under _internal/render/core.

import org.junit.jupiter.api.Test;
import com.victorvalentim.zividomelive.render.Quaternion;
import processing.core.PApplet;
import processing.core.PImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentStateTest {
	private static final float EPSILON = 1.0e-5f;

	@Test
	void defaultsDescribeAnOptionalVisibleLdrBackground() {
		EnvironmentState state = new EnvironmentState();

		assertFalse(state.hasSource());
		assertTrue(state.isVisible());
		assertEquals(1.0f, state.getIntensity(), 1.0e-6f);
		assertEquals(0.0f, state.getYawOffset(), 1.0e-6f);
	}

	@Test
	void replacementAndClearOnlyChangeTheBorrowedSourceReference() {
		EnvironmentState state = new EnvironmentState();
		PImage first = new PImage(4, 2, PApplet.ARGB);
		PImage replacement = new PImage(8, 4, PApplet.ARGB);

		state.setLdrEquirectangularSource(first);
		assertSame(first, state.getLdrEquirectangularSource());

		state.setLdrEquirectangularSource(replacement);
		assertSame(replacement, state.getLdrEquirectangularSource());

		state.clearSource();
		assertFalse(state.hasSource());
	}

	@Test
	void visualControlsAreCentralizedAndIntensityIsNonNegative() {
		EnvironmentState state = new EnvironmentState();

		state.setVisible(false);
		state.setIntensity(-3.0f);
		state.setYawOffset(0.75f);

		assertFalse(state.isVisible());
		assertEquals(0.0f, state.getIntensity(), 1.0e-6f);
		assertEquals(0.75f, state.getYawOffset(), 1.0e-6f);
	}

	@Test
	void visualControlsIgnoreNonFiniteValues() {
		EnvironmentState state = new EnvironmentState();
		state.setIntensity(1.5f);
		state.setYawOffset(0.75f);

		state.setIntensity(Float.NaN);
		state.setYawOffset(Float.POSITIVE_INFINITY);

		assertEquals(1.5f, state.getIntensity(), 1.0e-6f);
		assertEquals(0.75f, state.getYawOffset(), 1.0e-6f);
	}

	@Test
	void sceneCameraOrientationDefaultsToIdentityAndUsesImmutableValues() {
		EnvironmentState state = new EnvironmentState();
		Quaternion initial = state.getSceneCameraOrientation();

		assertEquals(0f, initial.x(), EPSILON);
		assertEquals(0f, initial.y(), EPSILON);
		assertEquals(0f, initial.z(), EPSILON);
		assertEquals(1f, initial.w(), EPSILON);

		Quaternion source = Quaternion.fromAxisAngle(0f, 1f, 0f, 0.75f);
		state.setSceneCameraOrientation(source);

		Quaternion stored = state.getSceneCameraOrientation();
		assertEquals(Quaternion.fromAxisAngle(0f, 1f, 0f, 0.75f).x(), stored.x(), EPSILON);
	}

	@Test
	void nullSceneCameraOrientationRestoresIdentity() {
		EnvironmentState state = new EnvironmentState();
		state.setSceneCameraOrientation(Quaternion.fromAxisAngle(1f, 0f, 0f, 1f));

		state.setSceneCameraOrientation(null);

		Quaternion stored = state.getSceneCameraOrientation();
		assertEquals(0f, stored.x(), EPSILON);
		assertEquals(0f, stored.y(), EPSILON);
		assertEquals(0f, stored.z(), EPSILON);
		assertEquals(1f, stored.w(), EPSILON);
	}
}
