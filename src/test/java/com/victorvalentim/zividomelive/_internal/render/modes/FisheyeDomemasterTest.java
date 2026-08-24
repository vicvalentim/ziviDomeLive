package com.victorvalentim.zividomelive;

// Package-private implementation test grouped physically under _internal/render/modes.

import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.opengl.PShader;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FisheyeDomemasterTest {

	/**
	 * Headless PApplet stub that avoids any GPU/graphics context.
	 * loadShader returns null so shader-dependent paths can be exercised safely.
	 */
	private static class StubApplet extends PApplet {
		private final List<String> loadedShaders = new ArrayList<>();

		@Override
		public PShader loadShader(String fragFilename, String vertFilename) {
			loadedShaders.add(fragFilename + "|" + vertFilename);
			return null;
		}
	}

	private FisheyeDomemaster newFisheye() {
		return new FisheyeDomemaster(1024, "frag", "vert", new StubApplet());
	}

	@Test
	void sizePercentageDefaultsTo100() {
		assertEquals(100.0f, newFisheye().getSizePercentage(), 1e-6f);
	}

	@Test
	void setSizePercentageConstrainsToValidRange() {
		FisheyeDomemaster fisheye = newFisheye();

		fisheye.setSizePercentage(150f);
		assertEquals(100f, fisheye.getSizePercentage(), 1e-6f);

		fisheye.setSizePercentage(-10f);
		assertEquals(0f, fisheye.getSizePercentage(), 1e-6f);

		fisheye.setSizePercentage(42.5f);
		assertEquals(42.5f, fisheye.getSizePercentage(), 1e-6f);
	}

	@Test
	void applyShaderWithNullInputsDoesNotThrow() {
		FisheyeDomemaster fisheye = newFisheye();
		assertDoesNotThrow(() -> fisheye.applyShader(null, 210f));
	}

	@Test
	void constructorLoadsSamplerCubeShaderWhenConfigured() {
		StubApplet applet = new StubApplet();

		new FisheyeDomemaster(1024, "samplercube.frag", "samplercube.vert", applet);

		assertEquals(List.of("samplercube.frag|samplercube.vert"), applet.loadedShaders);
	}

	@Test
	void ownsExactlyOnePGraphicsTarget() {
		List<Field> graphicsFields = Arrays.stream(FisheyeDomemaster.class.getDeclaredFields())
				.filter(field -> PGraphics.class.isAssignableFrom(field.getType()))
				.toList();

		assertEquals(1, graphicsFields.size());
		assertEquals("domemaster", graphicsFields.get(0).getName());
	}

	@Test
	void shaderAppliesSizePercentageInCenteredCoordinates() throws Exception {
		String shader = Files.readString(Path.of(
				System.getProperty("user.dir"), "shaders/samplercube/fisheye.frag"));

		assertTrue(shader.contains("uniform float sizePercentage;"));
		assertTrue(shader.contains("sizePercentage, 0.0, 100.0"));
		assertTrue(shader.contains("uv /= sizeScale;"));
		assertTrue(shader.contains("FragColor = vec4(0.0);"));
		assertTrue(shader.contains("color.a * coverage"));
		assertFalse(shader.contains("image("));
	}
}
