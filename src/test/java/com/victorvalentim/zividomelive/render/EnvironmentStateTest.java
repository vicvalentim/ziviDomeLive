package com.victorvalentim.zividomelive.render;

import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.core.PImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentStateTest {

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
}
