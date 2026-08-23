package com.victorvalentim.zividomelive;

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
	void renderWithMissingNativeCubemapDoesNotThrow() {
		EquirectangularRenderer renderer = new EquirectangularRenderer(1024, "frag", "vert", new StubApplet());
		assertDoesNotThrow(() -> renderer.render(null));
	}

	@Test
	void constructorLoadsSamplerCubeShaderWhenConfigured() {
		StubApplet applet = new StubApplet();

		new EquirectangularRenderer(1024, "samplercube.frag", "samplercube.vert", applet);

		assertEquals(List.of("samplercube.frag|samplercube.vert"), applet.loadedShaders);
	}
}
