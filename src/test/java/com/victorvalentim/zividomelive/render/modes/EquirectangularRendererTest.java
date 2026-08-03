package com.victorvalentim.zividomelive.render.modes;

import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.opengl.PShader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EquirectangularRendererTest {

	private static class StubApplet extends PApplet {
		@Override
		public PShader loadShader(String fragFilename, String vertFilename) {
			return null;
		}
	}

	@Test
	void renderWithNullShaderAndMissingFacesDoesNotThrow() {
		EquirectangularRenderer renderer = new EquirectangularRenderer(1024, "frag", "vert", new StubApplet());
		assertDoesNotThrow(() -> renderer.render(null));
	}
}
