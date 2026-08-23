package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.core.PImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardRendererTest {
	private static final float EPSILON = 1.0e-5f;

	@Test
	void standardAndCubemapRenderersConsumeOneBorrowedState() {
		EnvironmentState state = new EnvironmentState();
		StandardRenderer standard = new StandardRenderer(new PApplet(), 0, 0, null, state);
		CubemapRenderer spherical = new CubemapRenderer(
						64, new PApplet(), state);
		PImage source = new PImage(4, 2, PApplet.ARGB);

		standard.setEquirectangularBackground(source);
		standard.setEnvironmentBackgroundVisible(false);
		standard.setEnvironmentIntensity(0.4f);
		standard.setEnvironmentYawOffset(1.25f);

		assertSame(state, standard.getEnvironmentState());
		assertSame(state, spherical.getEnvironmentState());
		assertTrue(spherical.hasEnvironmentBackground());
		assertSame(source, state.getLdrEquirectangularSource());
		assertEquals(0.4f, spherical.getEnvironmentIntensity(), EPSILON);
		assertEquals(1.25f, spherical.getEnvironmentYawOffset(), EPSILON);

		standard.dispose();
		spherical.dispose();
		assertSame(source, state.getLdrEquirectangularSource(),
				"Renderers must not clear or dispose a shared borrowed source");
	}
}
