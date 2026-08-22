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
		String source = Files.readString(projectRoot.resolve("download_dependencies.sh"));

		// Bootstrap must fail hard and verify both downloaded archives and extracted JARs.
		assertTrue(source.contains("set -euo pipefail"));
		assertTrue(source.contains("archive checksum mismatch"));
		assertTrue(source.contains("JAR checksum mismatch"));

		// Processing dependencies must use public release downloads rather than
		// GitHub API asset transport, which may require authentication or hit API limits.
		assertTrue(source.contains(
				"https://github.com/Syphon/Processing/releases/download/latest/Syphon.zip"));
		assertTrue(source.contains(
				"https://github.com/leadedge/SpoutProcessing/releases/download/latest/spout.zip"));

		assertFalse(source.contains("api.github.com/repos/Syphon/Processing/releases/assets/"));
		assertFalse(source.contains("api.github.com/repos/leadedge/SpoutProcessing/releases/assets/"));
		assertFalse(source.contains("Accept: application/octet-stream"));
		assertFalse(source.contains("use_github_api"));
	}
}
