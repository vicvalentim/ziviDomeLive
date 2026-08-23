package com.victorvalentim.zividomelive.performance;

import java.util.Locale;

/**
 * Normalized host and renderer architecture recorded with experimental GPU measurements.
 *
 * <p>Capability checks and runtime qualification remain authoritative; architecture is diagnostic
 * evidence rather than proof that a timer backend is safe.</p>
 *
 * <p><strong>API stability:</strong> Experimental.</p>
 *
 * @since 2.0.0
 */
public enum GpuTimerArchitecture {
	/** Native or renderer-identified Apple Silicon host. */
	APPLE_SILICON,
	/** Intel Mac without an Apple Silicon renderer identity. */
	APPLE_INTEL,
	/** Windows on a 64-bit ARM JVM. */
	WINDOWS_ARM64,
	/** Windows on an x86-64 JVM. */
	WINDOWS_X86_64,
	/** Linux on a 64-bit ARM JVM. */
	LINUX_ARM64,
	/** Linux on an x86-64 JVM. */
	LINUX_X86_64,
	/** Host whose OS/architecture pair is not recognized. */
	OTHER;

	/**
	 * Detects a normalized architecture from JVM and active OpenGL identity strings.
	 *
	 * @param osName JVM operating-system name
	 * @param osArchitecture JVM architecture name
	 * @param glVendor active OpenGL vendor
	 * @param glRenderer active OpenGL renderer
	 * @return normalized benchmark architecture
	 */
	public static GpuTimerArchitecture detect(
			String osName,
			String osArchitecture,
			String glVendor,
			String glRenderer) {
		String os = normalize(osName);
		String architecture = normalize(osArchitecture);
		String graphics = normalize(glVendor) + " " + normalize(glRenderer);
		boolean arm64 = architecture.equals("aarch64")
				|| architecture.equals("arm64")
				|| architecture.contains("armv8");
		boolean x86_64 = architecture.equals("x86_64") || architecture.equals("amd64");

		if (os.contains("mac") || os.contains("darwin")) {
			// Renderer identity catches translated JVM launches where os.arch may report x86_64.
			if (arm64 || graphics.contains("apple m") || graphics.contains("apple gpu")) {
				return APPLE_SILICON;
			}
			return APPLE_INTEL;
		}
		if (os.contains("win")) {
			return arm64 ? WINDOWS_ARM64 : x86_64 ? WINDOWS_X86_64 : OTHER;
		}
		if (os.contains("linux")) {
			return arm64 ? LINUX_ARM64 : x86_64 ? LINUX_X86_64 : OTHER;
		}
		return OTHER;
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
