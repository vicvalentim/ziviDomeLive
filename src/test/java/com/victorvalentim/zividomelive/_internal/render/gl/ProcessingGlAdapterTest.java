package com.victorvalentim.zividomelive;

// Package-private implementation test grouped physically under _internal/render/gl.

import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.opengl.PGraphicsOpenGL;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessingGlAdapterTest {

	@Test
	void defaultAdapterIsShared() {
		assertSame(ProcessingGlAdapter.getDefault(), ProcessingGlAdapter.getDefault());
	}

	@Test
	void queryCapabilitiesWithoutOpenGlRendererIsUnavailable() {
		ProcessingGlCapabilities capabilities = ProcessingGlAdapter.getDefault()
				.queryCapabilities(new PApplet());

		assertFalse(capabilities.isOpenGlRenderer());
	}

	@Test
	void createGraphicsRejectsInvalidArgumentsBeforeTouchingProcessing() {
		ProcessingGlAdapter adapter = ProcessingGlAdapter.getDefault();

		assertThrows(IllegalArgumentException.class,
				() -> adapter.createGraphics(null, 1, 1, PApplet.P2D));
		assertThrows(IllegalArgumentException.class,
				() -> adapter.createGraphics(new PApplet(), 0, 1, PApplet.P2D));
		assertThrows(IllegalArgumentException.class,
				() -> adapter.createGraphics(new PApplet(), 1, 0, PApplet.P2D));
	}

	@Test
	void nullTargetsHaveNoTextureAndNoPixels() {
		ProcessingGlAdapter adapter = ProcessingGlAdapter.getDefault();

		assertFalse(adapter.hasTexture(null));
		assertFalse(adapter.copyPixels(null, new int[1], 1));
		assertFalse(adapter.copyPixels(new PGraphicsOpenGL(), null, 1));
		assertFalse(adapter.copyPixels(new PGraphicsOpenGL(), new int[0], 1));
	}

	@Test
	void cubemapSamplerTextureUnitMustBeNonNegative() {
		assertDoesNotThrow(() -> ProcessingGlAdapter.validateTextureUnit(0));
		assertDoesNotThrow(() -> ProcessingGlAdapter.validateTextureUnit(1));
		assertThrows(IllegalArgumentException.class,
				() -> ProcessingGlAdapter.validateTextureUnit(-1));
	}

	@Test
	void restoringAnInactiveScopedBindingIsAHeadlessSafeNoOp() {
		ProcessingGlAdapter adapter = ProcessingGlAdapter.getDefault();
		ProcessingGlAdapter.CubemapBindingState state =
				new ProcessingGlAdapter.CubemapBindingState();

		assertDoesNotThrow(() -> adapter.restoreCubemapTexture(null, state));
	}

	@Test
	void gpuTimerQuerySessionValidatesPoolBeforeTouchingGl() {
		ProcessingGlAdapter adapter = ProcessingGlAdapter.getDefault();
		PApplet parent = new PApplet();

		assertThrows(IllegalArgumentException.class,
				() -> adapter.createGpuTimerQuerySession(null, 8));
		assertThrows(IllegalArgumentException.class,
				() -> adapter.createGpuTimerQuerySession(parent, 1));
		assertThrows(IllegalArgumentException.class,
				() -> adapter.createGpuTimerQuerySession(parent, 65));
		assertDoesNotThrow(() -> adapter.createGpuTimerQuerySession(parent, 8).close());
	}

}
