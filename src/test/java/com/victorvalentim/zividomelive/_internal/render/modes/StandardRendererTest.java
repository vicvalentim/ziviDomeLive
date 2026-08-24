package com.victorvalentim.zividomelive;

// Package-private implementation test grouped physically under _internal/render/modes.

import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.core.PImage;
import processing.opengl.PGraphicsOpenGL;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardRendererTest {
	private static final float EPSILON = 1.0e-5f;

	@Test
	void sceneGeometryUsesFourSampleAntialiasing() {
		assertEquals(4, StandardRenderer.shapeAntialiasSamples());
	}

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

	@Test
	void sceneFailureStillBalancesTargetDrawLifecycle() throws Exception {
		PApplet applet = new PApplet();
		applet.width = 640;
		applet.height = 480;
		Scene throwingScene = graphics -> {
			throw new IllegalStateException("scene failure");
		};
		StandardRenderer renderer = new StandardRenderer(applet, 0, 0, throwingScene);
		TrackingGraphics target = new TrackingGraphics(640, 480);
		Field standardView = StandardRenderer.class.getDeclaredField("standardView");
		standardView.setAccessible(true);
		standardView.set(renderer, target);

		assertThrows(IllegalStateException.class, renderer::render);
		assertEquals(1, target.beginDrawCalls);
		assertEquals(1, target.endDrawCalls);
	}

	@Test
	void defaultFrameStartsTransparentWhenSceneAndEnvironmentDoNotDraw() throws Exception {
		PApplet applet = new PApplet();
		applet.width = 640;
		applet.height = 480;
		StandardRenderer renderer = new StandardRenderer(applet, 0, 0, graphics -> { });
		TrackingGraphics target = installTarget(renderer, 640, 480);

		renderer.render();

		assertEquals(1, target.clearCalls);
		assertEquals(0, target.backgroundCalls);
		assertFalse(renderer.hasEnvironmentBackground());
	}

	@Test
	void disabledEnvironmentLeavesDefaultFrameTransparent() throws Exception {
		PApplet applet = new PApplet();
		applet.width = 640;
		applet.height = 480;
		StandardRenderer renderer = new StandardRenderer(applet, 0, 0, graphics -> { });
		renderer.setEquirectangularBackground(new PImage(4, 2, PApplet.ARGB));
		renderer.setEnvironmentBackgroundVisible(false);
		TrackingGraphics target = installTarget(renderer, 640, 480);

		renderer.render();

		assertEquals(1, target.clearCalls);
		assertEquals(0, target.backgroundCalls);
	}

	@Test
	void skyColorBecomesOpaqueOnlyAfterExplicitRequest() throws Exception {
		PApplet applet = new PApplet();
		applet.width = 640;
		applet.height = 480;
		StandardRenderer renderer = new StandardRenderer(applet, 0, 0, graphics -> { });
		renderer.setSkyColor(10, 20, 30);
		TrackingGraphics target = installTarget(renderer, 640, 480);

		renderer.render();

		assertEquals(0, target.clearCalls);
		assertEquals(1, target.backgroundCalls);
	}

	@Test
	void clipFactorsAreFinitePositiveAndDerivedFromEffectiveNear() throws Exception {
		StandardRenderer renderer = new StandardRenderer(new PApplet(), 0, 0, null);

		renderer.setClipFactors(Float.NaN, Float.POSITIVE_INFINITY);
		float normalizedNear = readFloat(renderer, "nearFactor");
		float normalizedFar = readFloat(renderer, "farFactor");
		assertTrue(Float.isFinite(normalizedNear));
		assertTrue(Float.isFinite(normalizedFar));
		assertTrue(normalizedNear > 0.0f);
		assertTrue(normalizedFar > normalizedNear);
		assertEquals(normalizedNear * 10.0f, normalizedFar, EPSILON);

		renderer.setClipFactors(-5.0f, 0.0002f);
		float clampedNear = readFloat(renderer, "nearFactor");
		float clampedFar = readFloat(renderer, "farFactor");
		assertEquals(0.0001f, clampedNear, EPSILON);
		assertEquals(clampedNear * 10.0f, clampedFar, EPSILON);

		renderer.setClipFactors(Float.MAX_VALUE, Float.MAX_VALUE);
		assertTrue(Float.isFinite(readFloat(renderer, "nearFactor")));
		assertTrue(Float.isFinite(readFloat(renderer, "farFactor")));
		assertTrue(readFloat(renderer, "farFactor") > readFloat(renderer, "nearFactor"));
	}

	private static float readFloat(StandardRenderer renderer, String fieldName) throws Exception {
		Field field = StandardRenderer.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.getFloat(renderer);
	}

	private static TrackingGraphics installTarget(
			StandardRenderer renderer,
			int width,
			int height) throws Exception {
		TrackingGraphics target = new TrackingGraphics(width, height);
		Field standardView = StandardRenderer.class.getDeclaredField("standardView");
		standardView.setAccessible(true);
		standardView.set(renderer, target);
		return target;
	}

	private static final class TrackingGraphics extends PGraphicsOpenGL {
		private int beginDrawCalls;
		private int endDrawCalls;
		private int clearCalls;
		private int backgroundCalls;

		TrackingGraphics(int width, int height) {
			this.width = width;
			this.height = height;
		}

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

		@Override
		public void perspective(float fov, float aspect, float zNear, float zFar) {
			// No OpenGL context is needed for lifecycle verification.
		}

		@Override
		public void background(float red, float green, float blue) {
			backgroundCalls++;
		}

		@Override
		public void camera(
				float eyeX, float eyeY, float eyeZ,
				float centerX, float centerY, float centerZ,
				float upX, float upY, float upZ) {
			// No OpenGL context is needed for lifecycle verification.
		}
	}
}
