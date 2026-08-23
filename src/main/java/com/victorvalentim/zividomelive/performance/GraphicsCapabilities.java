package com.victorvalentim.zividomelive.performance;

/**
 * Read-only report of the active Processing renderer and graphics capabilities.
 *
 * <p>This is Experimental API intended for diagnostics and benchmark reports. The runtime
 * owns capability detection; sketches obtain a report from the facade.</p>
 *
 * @since 2.0.0
 */
public interface GraphicsCapabilities {

	boolean isOpenGlRenderer();

	String version();

	String shadingLanguageVersion();

	String vendor();

	String renderer();

	String joglProfile();

	boolean isHardwareRasterizerKnown();

	boolean isHardwareRasterizer();

	boolean supportsTexture();

	boolean supportsFramebuffer();

	boolean supportsCubemap();

	boolean supportsSeamlessCubemap();

	boolean supportsAnisotropicFiltering();

	boolean supportsPixelBufferObject();

	boolean supportsSyncFence();

	boolean supportsGpuTimerQuery();
}
