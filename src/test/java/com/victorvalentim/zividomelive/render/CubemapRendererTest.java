package com.victorvalentim.zividomelive.render;

import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.core.PImage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubemapRendererTest {

	@Test
	void headlessLifecycleClearsEnvironmentAndRemainsIdempotent() {
		CubemapRenderer renderer = new CubemapRenderer(64, new PApplet());
		renderer.setEquirectangularBackground(new PImage(2, 1));

		assertTrue(renderer.hasEnvironmentBackground());
		assertDoesNotThrow(renderer::dispose);
		assertFalse(renderer.hasEnvironmentBackground());
		assertDoesNotThrow(renderer::dispose);
	}
}
