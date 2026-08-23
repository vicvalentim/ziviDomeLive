package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.performance.GraphicsCapabilities;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Snapshot of the OpenGL capabilities exposed through Processing's active PGL context.
 */
final class ProcessingGlCapabilities implements GraphicsCapabilities {
	private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)");
	private static final int REQUIRED_OPENGL_MAJOR = 4;
	private static final int REQUIRED_OPENGL_MINOR = 1;
	private static final int REQUIRED_GLSL_MAJOR = 4;
	private static final int REQUIRED_GLSL_MINOR = 10;

	private final boolean openGlRenderer;
	private final String version;
	private final String shadingLanguageVersion;
	private final String vendor;
	private final String renderer;
	private final String joglProfile;
	private final boolean hardwareRasterizerKnown;
	private final boolean hardwareRasterizer;
	private final boolean textureSupported;
	private final boolean framebufferSupported;
	private final boolean cubemapSupported;
	private final boolean seamlessCubemapSupported;
	private final boolean anisotropicFilteringSupported;
	private final boolean pixelBufferObjectSupported;
	private final boolean syncFenceSupported;
	private final boolean gpuTimerQuerySupported;

	/**
	 * Compatibility constructor for capability snapshots that do not include
	 * JOGL hardware-rasterizer metadata.
	 */
	private ProcessingGlCapabilities(
			boolean openGlRenderer,
			String version,
			String shadingLanguageVersion,
			String vendor,
			String renderer,
			boolean textureSupported,
			boolean framebufferSupported,
			boolean cubemapSupported,
			boolean seamlessCubemapSupported,
			boolean anisotropicFilteringSupported,
			boolean pixelBufferObjectSupported,
			boolean syncFenceSupported,
			boolean gpuTimerQuerySupported) {
		this(
				openGlRenderer,
				version,
				shadingLanguageVersion,
				vendor,
				renderer,
				"",
				false,
				false,
				textureSupported,
				framebufferSupported,
				cubemapSupported,
				seamlessCubemapSupported,
				anisotropicFilteringSupported,
				pixelBufferObjectSupported,
				syncFenceSupported,
				gpuTimerQuerySupported);
	}

	/**
	 * Complete constructor including JOGL hardware-rasterizer metadata.
	 */
	private ProcessingGlCapabilities(
			boolean openGlRenderer,
			String version,
			String shadingLanguageVersion,
			String vendor,
			String renderer,
			String joglProfile,
			boolean hardwareRasterizerKnown,
			boolean hardwareRasterizer,
			boolean textureSupported,
			boolean framebufferSupported,
			boolean cubemapSupported,
			boolean seamlessCubemapSupported,
			boolean anisotropicFilteringSupported,
			boolean pixelBufferObjectSupported,
			boolean syncFenceSupported,
			boolean gpuTimerQuerySupported) {
		this.openGlRenderer = openGlRenderer;
		this.version = normalize(version);
		this.shadingLanguageVersion = normalize(shadingLanguageVersion);
		this.vendor = normalize(vendor);
		this.renderer = normalize(renderer);
		this.joglProfile = normalize(joglProfile);
		this.hardwareRasterizerKnown = hardwareRasterizerKnown;
		this.hardwareRasterizer = hardwareRasterizer;
		this.textureSupported = textureSupported;
		this.framebufferSupported = framebufferSupported;
		this.cubemapSupported = cubemapSupported;
		this.seamlessCubemapSupported = seamlessCubemapSupported;
		this.anisotropicFilteringSupported = anisotropicFilteringSupported;
		this.pixelBufferObjectSupported = pixelBufferObjectSupported;
		this.syncFenceSupported = syncFenceSupported;
		this.gpuTimerQuerySupported = gpuTimerQuerySupported;
	}

	/**
	 * Returns a capabilities snapshot for a missing or non-OpenGL Processing renderer.
	 *
	 * @return unavailable capabilities snapshot
	 */
	public static ProcessingGlCapabilities unavailable() {
		return new ProcessingGlCapabilities(
				false,
				"",
				"",
				"",
				"",
				false,
				false,
				false,
				false,
				false,
				false,
				false,
				false);
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
		boolean desktopGl = !normalize(version)
				.toLowerCase(Locale.ROOT)
				.contains("opengl es");
		String normalizedExtensions = normalize(extensions)
				.toLowerCase(Locale.ROOT);

		boolean framebuffer = parsed.atLeast(3, 0)
				|| hasExtension(normalizedExtensions, "gl_arb_framebuffer_object")
				|| hasExtension(normalizedExtensions, "gl_ext_framebuffer_object");

		boolean cubemap = parsed.atLeast(1, 3)
				|| hasExtension(normalizedExtensions, "gl_arb_texture_cube_map")
				|| hasExtension(normalizedExtensions, "gl_ext_texture_cube_map");

		boolean seamlessCubemap = (desktopGl && parsed.atLeast(3, 2))
				|| hasExtension(normalizedExtensions, "gl_arb_seamless_cube_map");

		boolean anisotropicFiltering =
				hasExtension(
						normalizedExtensions,
						"gl_ext_texture_filter_anisotropic")
						|| hasExtension(
						normalizedExtensions,
						"gl_arb_texture_filter_anisotropic");

		boolean pbo = parsed.atLeast(2, 1)
				|| hasExtension(normalizedExtensions, "gl_arb_pixel_buffer_object")
				|| hasExtension(normalizedExtensions, "gl_ext_pixel_buffer_object");

		boolean fence = parsed.atLeast(3, 2)
				|| hasExtension(normalizedExtensions, "gl_arb_sync")
				|| hasExtension(normalizedExtensions, "gl_apple_sync");

		boolean gpuTimerQuery = desktopGl
				&& (parsed.atLeast(3, 3)
				|| hasExtension(
				normalizedExtensions,
				"gl_arb_timer_query"));

		return new ProcessingGlCapabilities(
				true,
				version,
				"",
				vendor,
				renderer,
				true,
				framebuffer,
				cubemap,
				seamlessCubemap,
				anisotropicFiltering,
				pbo,
				fence,
				gpuTimerQuery);
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

	ProcessingGlCapabilities withShadingLanguageVersion(
			String shadingLanguageVersion) {
		return new ProcessingGlCapabilities(
				openGlRenderer,
				version,
				shadingLanguageVersion,
				vendor,
				renderer,
				joglProfile,
				hardwareRasterizerKnown,
				hardwareRasterizer,
				textureSupported,
				framebufferSupported,
				cubemapSupported,
				seamlessCubemapSupported,
				anisotropicFilteringSupported,
				pixelBufferObjectSupported,
				syncFenceSupported,
				gpuTimerQuerySupported);
	}

	/**
	 * Adds JOGL profile and hardware-rasterizer evidence to this immutable
	 * capabilities snapshot.
	 *
	 * <p>The hardware classification is diagnostic metadata and does not alter
	 * the existing OpenGL/GLSL spherical-profile compatibility contract.</p>
	 *
	 * @param joglProfile raw JOGL profile description
	 * @param hardwareRasterizer whether JOGL classifies the profile as hardware-backed
	 * @return capabilities snapshot containing JOGL hardware metadata
	 */
	ProcessingGlCapabilities withJoglProfile(
			String joglProfile,
			boolean hardwareRasterizer) {
		return new ProcessingGlCapabilities(
				openGlRenderer,
				version,
				shadingLanguageVersion,
				vendor,
				renderer,
				joglProfile,
				true,
				hardwareRasterizer,
				textureSupported,
				framebufferSupported,
				cubemapSupported,
				seamlessCubemapSupported,
				anisotropicFilteringSupported,
				pixelBufferObjectSupported,
				syncFenceSupported,
				gpuTimerQuerySupported);
	}

	/**
	 * Returns the raw OpenGL Shading Language version string.
	 *
	 * @return GLSL version string, or empty when unavailable
	 */
	public String shadingLanguageVersion() {
		return shadingLanguageVersion;
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
	 * Returns the JOGL profile associated with the active OpenGL context.
	 *
	 * @return JOGL profile description, or empty when unavailable
	 */
	public String joglProfile() {
		return joglProfile;
	}

	/**
	 * Reports whether JOGL supplied explicit hardware-rasterizer evidence.
	 *
	 * <p>This distinguishes an unavailable classification from an explicit
	 * software-rasterizer result.</p>
	 *
	 * @return {@code true} when the hardware classification is known
	 */
	public boolean isHardwareRasterizerKnown() {
		return hardwareRasterizerKnown;
	}

	/**
	 * Reports whether JOGL classifies the active OpenGL profile as
	 * hardware-backed.
	 *
	 * <p>This value is meaningful when
	 * {@link #isHardwareRasterizerKnown()} returns {@code true}.</p>
	 *
	 * @return {@code true} when JOGL reports a hardware rasterizer
	 */
	public boolean isHardwareRasterizer() {
		return hardwareRasterizer;
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
	 * Reports whether seamless cubemap sampling is advertised.
	 *
	 * @return {@code true} when seamless cubemap support is core or extension-backed
	 */
	public boolean supportsSeamlessCubemap() {
		return seamlessCubemapSupported;
	}

	/**
	 * Reports whether anisotropic texture filtering is advertised.
	 *
	 * @return {@code true} when anisotropic filtering support is extension-backed
	 */
	public boolean supportsAnisotropicFiltering() {
		return anisotropicFilteringSupported;
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

	/**
	 * Reports whether desktop OpenGL advertises the core/ARB GPU timer-query API.
	 * OpenGL ES disjoint timers are intentionally excluded because they require
	 * different validity handling. Runtime collection additionally requires non-zero
	 * timestamp counter bits from the active context.
	 *
	 * @return {@code true} for desktop OpenGL 3.3+ or ARB_timer_query
	 */
	public boolean supportsGpuTimerQuery() {
		return gpuTimerQuerySupported;
	}

	boolean supportsRequiredSphericalShaderProfile() {
		if (!openGlRenderer
				|| normalize(version)
				.toLowerCase(Locale.ROOT)
				.contains("opengl es")) {
			return false;
		}

		Version openGlVersion = parseVersion(version);
		Version glslVersion = parseVersion(shadingLanguageVersion);

		return openGlVersion.atLeast(
				REQUIRED_OPENGL_MAJOR,
				REQUIRED_OPENGL_MINOR)
				&& glslVersion.atLeast(
				REQUIRED_GLSL_MAJOR,
				REQUIRED_GLSL_MINOR);
	}

	private static boolean hasExtension(
			String extensions,
			String extension) {
		return (" " + extensions + " ")
				.contains(" " + extension + " ");
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
		boolean atLeast(
				int requiredMajor,
				int requiredMinor) {
			return major > requiredMajor
					|| (major == requiredMajor
					&& minor >= requiredMinor);
		}
	}
}
