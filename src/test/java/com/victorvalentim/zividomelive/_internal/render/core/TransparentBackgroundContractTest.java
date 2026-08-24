package com.victorvalentim.zividomelive;

// Package-private implementation test grouped physically under _internal/render/core.

import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransparentBackgroundContractTest {
	private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));

	@Test
	void libraryOwnedTargetsUseTransparentClearsInsteadOfOpaqueFallbacks() throws IOException {
		String standard = read("src/main/java/com/victorvalentim/zividomelive/"
				+ "_internal/render/modes/StandardRenderer.java");
		String cubemapCapture = read("src/main/java/com/victorvalentim/zividomelive/"
				+ "_internal/render/core/CubemapRenderer.java");
		String fisheye = read("src/main/java/com/victorvalentim/zividomelive/"
				+ "_internal/render/modes/FisheyeDomemaster.java");
		String equirectangular = read("src/main/java/com/victorvalentim/zividomelive/"
				+ "_internal/render/modes/EquirectangularRenderer.java");
		String skybox = read("src/main/java/com/victorvalentim/zividomelive/"
				+ "_internal/render/modes/CubemapViewRenderer.java");
		String facade = read("src/main/java/com/victorvalentim/zividomelive/ziviDomeLive.java");

		assertAll("transparent target clears",
				() -> assertTrue(standard.contains("standardView.clear();")),
				() -> assertTrue(standard.contains("skyColorExplicitlySet")),
				() -> assertTrue(cubemapCapture.contains("captureGraphics.clear();")),
				() -> assertFalse(cubemapCapture.contains("captureGraphics.background(")),
				() -> assertTrue(fisheye.contains("target.clear();")),
				() -> assertTrue(fisheye.contains("target.noStroke();")),
				() -> assertTrue(fisheye.contains("target.blendMode(PApplet.REPLACE);")),
				() -> assertFalse(fisheye.contains("target.background(")),
				() -> assertTrue(equirectangular.contains("target.clear();")),
				() -> assertTrue(equirectangular.contains("target.noStroke();")),
				() -> assertTrue(equirectangular.contains("target.blendMode(PApplet.REPLACE);")),
				() -> assertFalse(equirectangular.contains("target.background(")),
				() -> assertTrue(skybox.contains("cubemap.clear();")),
				() -> assertTrue(skybox.contains("cubemap.noStroke();")),
				() -> assertTrue(skybox.contains("cubemap.blendMode(PApplet.REPLACE);")),
				() -> assertFalse(skybox.contains("cubemap.background(")),
				() -> assertTrue(facade.contains("destination.clear();")),
				() -> assertTrue(facade.contains("p.background(0, 0, 0, 0);")));
	}

	@Test
	void projectionShadersPreserveCubemapAlphaAndUseTransparentEmptyPixels()
			throws IOException {
		String cubemap = read("shaders/samplercube/cubemap.frag");
		String equirectangular = read("shaders/samplercube/equirectangular.frag");
		String fisheye = read("shaders/samplercube/fisheye.frag");
		String skybox = read("shaders/samplercube/skybox.frag");

		assertAll("projection alpha",
				() -> assertTrue(cubemap.contains("fragColor = color;")),
				() -> assertFalse(cubemap.contains("vec4(color, 1.0)")),
				() -> assertTrue(equirectangular.contains(
						"FragColor = sampleCubemapDirection(dir);")),
				() -> assertTrue(fisheye.contains("FragColor = vec4(0.0);")),
				() -> assertTrue(fisheye.contains("color.a * coverage")),
				() -> assertTrue(skybox.contains("FragColor = vec4(0.0);")),
				() -> assertFalse(equirectangular.contains("vec4(color.rgb, 1.0)")),
				() -> assertFalse(skybox.contains("vec4(color.rgb, 1.0)")));
	}

	@Test
	void environmentIsOptionalAndPreservesSourceAlphaWhenEnabled() throws IOException {
		EnvironmentState state = new EnvironmentState();
		String standardPass = read("src/main/java/com/victorvalentim/zividomelive/"
				+ "_internal/render/core/EnvironmentBackgroundRenderer.java");
		String sphericalPass = read("src/main/java/com/victorvalentim/zividomelive/"
				+ "_internal/render/core/SphericalEnvironmentNativePass.java");
		String standardShader = read(
				"shaders/environment/standard_equirectangular_background.frag");
		String sphericalShader = read(
				"shaders/environment/spherical_equirectangular_background.frag");

		assertAll("optional Environment alpha",
				() -> assertFalse(state.hasSource()),
				() -> assertTrue(standardPass.contains(
						"state.isVisible() && state.hasSource()")),
				() -> assertTrue(sphericalPass.contains(
						"!state.isVisible() || !state.hasSource()")),
				() -> assertTrue(standardShader.contains("color.a")),
				() -> assertTrue(sphericalShader.contains("color.a")));
	}

	@Test
	void unavailableProjectionInputsClearExistingTargetsInsteadOfKeepingStaleFrames()
			throws Exception {
		StubApplet applet = new StubApplet();
		TrackingGraphics fisheyeTarget = new TrackingGraphics();
		TrackingGraphics equirectangularTarget = new TrackingGraphics();
		TrackingGraphics skyboxTarget = new TrackingGraphics();

		FisheyeDomemaster fisheye = new FisheyeDomemaster(64, "frag", "vert", applet);
		EquirectangularRenderer equirectangular =
				new EquirectangularRenderer(64, "frag", "vert", applet);
		CubemapViewRenderer skybox =
				new CubemapViewRenderer(applet, 64, "frag", "vert");
		installTarget(fisheye, "domemaster", fisheyeTarget);
		installTarget(equirectangular, "equirectangular", equirectangularTarget);
		installTarget(skybox, "cubemap", skyboxTarget);

		fisheye.applyShader(null, 210f);
		equirectangular.render(null);
		skybox.drawCubemapToGraphics(null);

		assertAll("unavailable projection clears",
				() -> assertEquals(1, fisheyeTarget.clearCalls),
				() -> assertEquals(1, fisheyeTarget.beginDrawCalls),
				() -> assertEquals(1, fisheyeTarget.endDrawCalls),
				() -> assertEquals(1, equirectangularTarget.clearCalls),
				() -> assertEquals(1, equirectangularTarget.beginDrawCalls),
				() -> assertEquals(1, equirectangularTarget.endDrawCalls),
				() -> assertEquals(1, skyboxTarget.clearCalls),
				() -> assertEquals(1, skyboxTarget.beginDrawCalls),
				() -> assertEquals(1, skyboxTarget.endDrawCalls));
	}

	private static String read(String relativePath) throws IOException {
		return Files.readString(PROJECT_ROOT.resolve(relativePath));
	}

	private static void installTarget(Object renderer, String fieldName, PGraphicsOpenGL target)
			throws Exception {
		Field field = renderer.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(renderer, target);
	}

	private static final class StubApplet extends PApplet {
		@Override
		public PShader loadShader(String fragmentPath, String vertexPath) {
			return null;
		}
	}

	private static final class TrackingGraphics extends PGraphicsOpenGL {
		private int beginDrawCalls;
		private int endDrawCalls;
		private int clearCalls;

		@Override
		public void beginDraw() {
			beginDrawCalls++;
		}

		@Override
		public void endDraw() {
			endDrawCalls++;
		}

		@Override
		public void clear() {
			clearCalls++;
		}
	}
}
