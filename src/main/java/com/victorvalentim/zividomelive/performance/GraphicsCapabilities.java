package com.victorvalentim.zividomelive.performance;

/**
 * Read-only report of the active Processing renderer and graphics capabilities.
 *
 * <p>This is Experimental API intended for diagnostics and benchmark reports. The runtime
 * owns capability detection; sketches obtain a report from the facade.</p>
 *
 * <p>Capability flags are observations of the active context, not guarantees that a driver path
 * will remain available after context loss or renderer replacement.</p>
 *
 * <p><strong>API stability:</strong> Experimental.</p>
 *
 * @since 2.0.0
 */
public interface GraphicsCapabilities {

	/** @return whether Processing currently exposes an OpenGL-backed renderer */
	boolean isOpenGlRenderer();

	/** @return raw OpenGL version string, or an empty string when unavailable */
	String version();

	/** @return raw shading-language version string, or an empty string when unavailable */
	String shadingLanguageVersion();

	/** @return raw OpenGL vendor string, or an empty string when unavailable */
	String vendor();

	/** @return raw OpenGL renderer string, or an empty string when unavailable */
	String renderer();

	/** @return JOGL profile description, or an empty string when unavailable */
	String joglProfile();

	/** @return whether JOGL reported a hardware-rasterizer classification */
	boolean isHardwareRasterizerKnown();

	/** @return JOGL's hardware-rasterizer result; meaningful only when known */
	boolean isHardwareRasterizer();

	/** @return whether basic texture support was detected */
	boolean supportsTexture();

	/** @return whether framebuffer-object support was detected */
	boolean supportsFramebuffer();

	/** @return whether cubemap texture support was detected */
	boolean supportsCubemap();

	/** @return whether seamless cubemap sampling was detected */
	boolean supportsSeamlessCubemap();

	/** @return whether anisotropic texture filtering was detected */
	boolean supportsAnisotropicFiltering();

	/** @return whether pixel-buffer-object support was detected */
	boolean supportsPixelBufferObject();

	/** @return whether asynchronous OpenGL sync fences were detected */
	boolean supportsSyncFence();

	/** @return whether OpenGL GPU timer-query support was detected */
	boolean supportsGpuTimerQuery();
}
