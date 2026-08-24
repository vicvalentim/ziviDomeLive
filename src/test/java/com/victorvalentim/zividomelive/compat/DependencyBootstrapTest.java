package com.victorvalentim.zividomelive.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyBootstrapTest {

	@Test
	void dependencyBootstrapUsesChecksumPinnedPublicReleaseDownloads() throws IOException {
		Path projectRoot = Path.of(System.getProperty("user.dir"));
		String source = Files.readString(projectRoot.resolve("build.gradle.kts"));

		// The JVM bootstrap must fail hard and verify both archives and extracted JARs.
		assertTrue(source.contains("MessageDigest.getInstance(\"SHA-256\")"));
		assertTrue(source.contains("archive checksum mismatch"));
		assertTrue(source.contains("JAR checksum mismatch"));
		assertTrue(source.contains("ZipFile(archive)"));
		assertFalse(source.contains("commandLine(\"bash\""));
		assertFalse(Files.exists(projectRoot.resolve("download_dependencies.sh")));

		// Upstreams publish these versions under a mutable tag, so use immutable asset IDs.
		assertTrue(source.contains(
				"https://api.github.com/repos/Syphon/Processing/releases/assets/59352362"));
		assertTrue(source.contains(
				"https://api.github.com/repos/leadedge/SpoutProcessing/releases/assets/188539046"));

		assertFalse(source.contains("/releases/download/latest/"));
	}

	@Test
	void sketchbookPathSupportsExplicitCrossPlatformOverrides() throws IOException {
		String source = Files.readString(
				Path.of(System.getProperty("user.dir")).resolve("build.gradle.kts"));
		assertTrue(source.contains("gradleProperty(\"processingSketchbook\")"));
		assertTrue(source.contains("PROCESSING_SKETCHBOOK"));
		assertTrue(source.contains("$userHome/Documents/Processing"));
		assertFalse(source.contains("My Documents/Processing/sketchbook"));
	}
}
