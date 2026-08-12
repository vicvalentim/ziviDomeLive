package com.victorvalentim.zividomelive.render.gl;

import com.victorvalentim.zividomelive.render.camera.CubemapFace;
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
	void cubemapCopyDimensionsMustMatchSquareTarget() {
		assertDoesNotThrow(() -> ProcessingGlAdapter.validateCubemapCopyDimensions(1024, 1024, 1024));
		assertThrows(IllegalArgumentException.class,
				() -> ProcessingGlAdapter.validateCubemapCopyDimensions(0, 1024, 1024));
		assertThrows(IllegalArgumentException.class,
				() -> ProcessingGlAdapter.validateCubemapCopyDimensions(1024, 512, 1024));
		assertThrows(IllegalArgumentException.class,
				() -> ProcessingGlAdapter.validateCubemapCopyDimensions(512, 512, 1024));
	}

	@Test
	void cubemapCopyRejectsNullInputsBeforeTouchingGl() {
		ProcessingGlAdapter adapter = ProcessingGlAdapter.getDefault();

		assertThrows(NullPointerException.class,
				() -> adapter.copyTextureToCubemapFace(
						new PApplet(),
						null,
						null,
						CubemapFace.POSITIVE_X));
		assertThrows(NullPointerException.class,
				() -> adapter.copyTextureToCubemapFace(
						new PApplet(),
						new PGraphicsOpenGL(),
						null,
						CubemapFace.POSITIVE_X));
	}
}
