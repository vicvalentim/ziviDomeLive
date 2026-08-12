package com.victorvalentim.zividomelive.render.modes;

import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.opengl.PShader;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CubemapViewRendererTest {

	private static class StubApplet extends PApplet {
		private final List<String> loadedShaders = new ArrayList<>();

		@Override
		public PShader loadShader(String fragFilename, String vertFilename) {
			loadedShaders.add(fragFilename + "|" + vertFilename);
			return null;
		}
	}

	@Test
	void legacyConstructorDoesNotLoadSamplerCubeShader() {
		StubApplet applet = new StubApplet();

		assertDoesNotThrow(() -> new CubemapViewRenderer(applet, 1024));

		assertEquals(List.of(), applet.loadedShaders);
	}

	@Test
	void constructorLoadsSamplerCubeShaderWhenConfigured() {
		StubApplet applet = new StubApplet();

		new CubemapViewRenderer(
				applet,
				1024,
				"samplercube.frag",
				"samplercube.vert");

		assertEquals(List.of("samplercube.frag|samplercube.vert"), applet.loadedShaders);
	}

	@Test
	void nativePathSkipsWhenCubemapIsMissing() {
		CubemapViewRenderer renderer = new CubemapViewRenderer(new StubApplet(), 1024);

		assertDoesNotThrow(() -> renderer.drawCubemapToGraphics(null));
	}
}
