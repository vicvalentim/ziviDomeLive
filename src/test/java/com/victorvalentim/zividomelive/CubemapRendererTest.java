package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.render.Quaternion;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.core.PImage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubemapRendererTest {
	private static final float EPSILON = 1.0e-5f;

	@Test
	void headlessLifecycleClearsEnvironmentAndRemainsIdempotent() {
		CubemapRenderer renderer = new CubemapRenderer(64, new PApplet());
		renderer.setEquirectangularBackground(new PImage(2, 1));

		assertTrue(renderer.hasEnvironmentBackground());
		assertDoesNotThrow(renderer::dispose);
		assertFalse(renderer.hasEnvironmentBackground());
		assertDoesNotThrow(renderer::dispose);
	}

	@Test
	void environmentOrientationComposesDomeThenSceneCameraRotation() {
		Quaternion dome = Quaternion.fromAxisAngle(1f, 0f, 0f, 0.35f);
		Quaternion sceneCamera = Quaternion.fromAxisAngle(0f, 1f, 0f, -0.6f);

		Quaternion composed = CubemapRenderer.composeEnvironmentOrientation(dome, sceneCamera);
		Quaternion expected = dome.multiply(sceneCamera).normalized();
		float dot = Math.abs(
				expected.x() * composed.x()
						+ expected.y() * composed.y()
						+ expected.z() * composed.z()
						+ expected.w() * composed.w());

		assertEquals(1f, dot, EPSILON);
	}
}
