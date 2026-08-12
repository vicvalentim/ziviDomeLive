package com.victorvalentim.zividomelive.render.modes;

import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.opengl.PShader;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EquirectangularRendererTest {

	private static class StubApplet extends PApplet {
		private final List<String> loadedShaders = new ArrayList<>();

		@Override
		public PShader loadShader(String fragFilename, String vertFilename) {
			loadedShaders.add(fragFilename + "|" + vertFilename);
			return null;
		}
	}

	@Test
	void renderWithNullShaderAndMissingFacesDoesNotThrow() {
		EquirectangularRenderer renderer = new EquirectangularRenderer(1024, "frag", "vert", new StubApplet());
		assertDoesNotThrow(() -> renderer.render(null));
		assertDoesNotThrow(() -> renderer.render(null, null));
	}

	@Test
	void constructorLoadsSamplerCubeShaderWhenConfigured() {
		StubApplet applet = new StubApplet();

		new EquirectangularRenderer(
				1024,
				"legacy.frag",
				"legacy.vert",
				"samplercube.frag",
				"samplercube.vert",
				applet);

		assertEquals(List.of(
				"legacy.frag|legacy.vert",
				"samplercube.frag|samplercube.vert"), applet.loadedShaders);
	}
}
