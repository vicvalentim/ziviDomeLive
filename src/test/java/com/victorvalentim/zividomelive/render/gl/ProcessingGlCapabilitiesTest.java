package com.victorvalentim.zividomelive.render.gl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessingGlCapabilitiesTest {

	@Test
	void unavailableReportsNoGlFeatures() {
		ProcessingGlCapabilities capabilities = ProcessingGlCapabilities.unavailable();

		assertFalse(capabilities.isOpenGlRenderer());
		assertFalse(capabilities.supportsTexture());
		assertFalse(capabilities.supportsFramebuffer());
		assertFalse(capabilities.supportsCubemap());
		assertFalse(capabilities.supportsSeamlessCubemap());
		assertFalse(capabilities.supportsAnisotropicFiltering());
		assertFalse(capabilities.supportsPixelBufferObject());
		assertFalse(capabilities.supportsSyncFence());
	}

	@Test
	void modernCoreVersionEnablesRequiredFutureFeatureFlags() {
		ProcessingGlCapabilities capabilities = ProcessingGlCapabilities.fromOpenGlStrings(
				"4.1 ATI-5.5.17",
				"Vendor",
				"Renderer",
				"");

		assertTrue(capabilities.isOpenGlRenderer());
		assertTrue(capabilities.supportsTexture());
		assertTrue(capabilities.supportsFramebuffer());
		assertTrue(capabilities.supportsCubemap());
		assertTrue(capabilities.supportsSeamlessCubemap());
		assertFalse(capabilities.supportsAnisotropicFiltering());
		assertTrue(capabilities.supportsPixelBufferObject());
		assertTrue(capabilities.supportsSyncFence());
	}

	@Test
	void extensionStringsEnableCapabilitiesWhenVersionIsOld() {
		ProcessingGlCapabilities capabilities = ProcessingGlCapabilities.fromOpenGlStrings(
				"1.2 Mesa",
				"Vendor",
				"Renderer",
				"GL_ARB_framebuffer_object GL_ARB_texture_cube_map "
						+ "GL_ARB_seamless_cube_map GL_EXT_texture_filter_anisotropic "
						+ "GL_ARB_pixel_buffer_object GL_ARB_sync");

		assertTrue(capabilities.supportsFramebuffer());
		assertTrue(capabilities.supportsCubemap());
		assertTrue(capabilities.supportsSeamlessCubemap());
		assertTrue(capabilities.supportsAnisotropicFiltering());
		assertTrue(capabilities.supportsPixelBufferObject());
		assertTrue(capabilities.supportsSyncFence());
	}

	@Test
	void oldContextWithoutExtensionsOnlyReportsBasicTextureSupport() {
		ProcessingGlCapabilities capabilities = ProcessingGlCapabilities.fromOpenGlStrings(
				"1.1",
				"Vendor",
				"Renderer",
				"");

		assertTrue(capabilities.supportsTexture());
		assertFalse(capabilities.supportsFramebuffer());
		assertFalse(capabilities.supportsCubemap());
		assertFalse(capabilities.supportsSeamlessCubemap());
		assertFalse(capabilities.supportsAnisotropicFiltering());
		assertFalse(capabilities.supportsPixelBufferObject());
		assertFalse(capabilities.supportsSyncFence());
	}

	@Test
	void openglEsVersionDoesNotImplyDesktopSeamlessCubemap() {
		ProcessingGlCapabilities capabilities = ProcessingGlCapabilities.fromOpenGlStrings(
				"OpenGL ES 3.2",
				"Vendor",
				"Renderer",
				"");

		assertFalse(capabilities.supportsSeamlessCubemap());
	}
}
