package com.victorvalentim.zividomelive.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyBootstrapTest {

	@Test
	void dependencyBootstrapIsPinnedAndChecksumVerified() throws IOException {
		Path projectRoot = Path.of(System.getProperty("user.dir"));
		String source = Files.readString(projectRoot.resolve("download_dependencies.sh"));

		assertTrue(source.contains("set -euo pipefail"));
		assertTrue(source.contains("archive checksum mismatch"));
		assertTrue(source.contains("JAR checksum mismatch"));
		assertFalse(source.contains("/download/latest/"));
	}
}
