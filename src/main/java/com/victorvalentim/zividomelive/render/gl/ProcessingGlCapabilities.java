package com.victorvalentim.zividomelive.render.gl;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Snapshot of the OpenGL capabilities exposed through Processing's active PGL context.
 */
public final class ProcessingGlCapabilities {
	private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)");

	private final boolean openGlRenderer;
	private final String version;
	private final String vendor;
	private final String renderer;
	private final boolean textureSupported;
	private final boolean framebufferSupported;
	private final boolean cubemapSupported;
	private final boolean pixelBufferObjectSupported;
	private final boolean syncFenceSupported;

	private ProcessingGlCapabilities(
			boolean openGlRenderer,
			String version,
			String vendor,
			String renderer,
			boolean textureSupported,
			boolean framebufferSupported,
			boolean cubemapSupported,
			boolean pixelBufferObjectSupported,
			boolean syncFenceSupported) {
		this.openGlRenderer = openGlRenderer;
		this.version = normalize(version);
		this.vendor = normalize(vendor);
		this.renderer = normalize(renderer);
		this.textureSupported = textureSupported;
		this.framebufferSupported = framebufferSupported;
		this.cubemapSupported = cubemapSupported;
		this.pixelBufferObjectSupported = pixelBufferObjectSupported;
		this.syncFenceSupported = syncFenceSupported;
	}

	/**
	 * Returns a capabilities snapshot for a missing or non-OpenGL Processing renderer.
	 *
	 * @return unavailable capabilities snapshot
	 */
	public static ProcessingGlCapabilities unavailable() {
		return new ProcessingGlCapabilities(false, "", "", "", false, false, false, false, false);
	}

	/**
	 * Builds a capabilities snapshot from raw OpenGL strings.
	 *
	 * @param version OpenGL version string
	 * @param vendor OpenGL vendor string
	 * @param renderer OpenGL renderer string
	 * @param extensions OpenGL extension string
	 * @return parsed capabilities snapshot
	 */
	public static ProcessingGlCapabilities fromOpenGlStrings(
			String version,
			String vendor,
			String renderer,
			String extensions) {
		Version parsed = parseVersion(version);
		String normalizedExtensions = normalize(extensions).toLowerCase(Locale.ROOT);

		boolean framebuffer = parsed.atLeast(3, 0)
				|| hasExtension(normalizedExtensions, "gl_arb_framebuffer_object")
				|| hasExtension(normalizedExtensions, "gl_ext_framebuffer_object");
		boolean cubemap = parsed.atLeast(1, 3)
				|| hasExtension(normalizedExtensions, "gl_arb_texture_cube_map")
				|| hasExtension(normalizedExtensions, "gl_ext_texture_cube_map");
		boolean pbo = parsed.atLeast(2, 1)
				|| hasExtension(normalizedExtensions, "gl_arb_pixel_buffer_object")
				|| hasExtension(normalizedExtensions, "gl_ext_pixel_buffer_object");
		boolean fence = parsed.atLeast(3, 2)
				|| hasExtension(normalizedExtensions, "gl_arb_sync")
				|| hasExtension(normalizedExtensions, "gl_apple_sync");

		return new ProcessingGlCapabilities(
				true,
				version,
				vendor,
				renderer,
				true,
				framebuffer,
				cubemap,
				pbo,
				fence);
	}

	/**
	 * Reports whether the active Processing renderer is OpenGL-backed.
	 *
	 * @return {@code true} when Processing exposes a PGL context
	 */
	public boolean isOpenGlRenderer() {
		return openGlRenderer;
	}

	/**
	 * Returns the raw OpenGL version string.
	 *
	 * @return version string, or empty when unavailable
	 */
	public String version() {
		return version;
	}

	/**
	 * Returns the raw OpenGL vendor string.
	 *
	 * @return vendor string, or empty when unavailable
	 */
	public String vendor() {
		return vendor;
	}

	/**
	 * Returns the raw OpenGL renderer string.
	 *
	 * @return renderer string, or empty when unavailable
	 */
	public String renderer() {
		return renderer;
	}

	/**
	 * Reports whether regular texture operations are available.
	 *
	 * @return {@code true} for an OpenGL-backed Processing renderer
	 */
	public boolean supportsTexture() {
		return textureSupported;
	}

	/**
	 * Reports whether framebuffer object operations are advertised.
	 *
	 * @return {@code true} when FBO support is core or extension-backed
	 */
	public boolean supportsFramebuffer() {
		return framebufferSupported;
	}

	/**
	 * Reports whether cubemap texture targets are advertised.
	 *
	 * @return {@code true} when cubemap support is core or extension-backed
	 */
	public boolean supportsCubemap() {
		return cubemapSupported;
	}

	/**
	 * Reports whether pixel buffer objects are advertised.
	 *
	 * @return {@code true} when PBO support is core or extension-backed
	 */
	public boolean supportsPixelBufferObject() {
		return pixelBufferObjectSupported;
	}

	/**
	 * Reports whether OpenGL sync fences are advertised.
	 *
	 * @return {@code true} when sync/fence support is core or extension-backed
	 */
	public boolean supportsSyncFence() {
		return syncFenceSupported;
	}

	private static boolean hasExtension(String extensions, String extension) {
		return (" " + extensions + " ").contains(" " + extension + " ");
	}

	private static Version parseVersion(String version) {
		Matcher matcher = VERSION_PATTERN.matcher(normalize(version));
		if (!matcher.find()) {
			return new Version(0, 0);
		}
		return new Version(
				Integer.parseInt(matcher.group(1)),
				Integer.parseInt(matcher.group(2)));
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim();
	}

	private record Version(int major, int minor) {
		boolean atLeast(int requiredMajor, int requiredMinor) {
			return major > requiredMajor || (major == requiredMajor && minor >= requiredMinor);
		}
	}
}
