package com.victorvalentim.zividomelive.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseMetadataTest {

	private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));
	private static final String RELEASE_VERSION = "2.0.0";
	private static final String PROCESSING_RELEASE_NUMBER = "11";

	@Test
	void releaseVersionIsAlignedAcrossMetadata() throws IOException {
		Properties release = loadProperties("release.properties");
		Properties library = loadProperties("library.properties");

		assertEquals(PROCESSING_RELEASE_NUMBER, release.getProperty("version"));
		assertEquals(PROCESSING_RELEASE_NUMBER, library.getProperty("version"));
		assertEquals(RELEASE_VERSION, library.getProperty("prettyVersion"));
		assertTrue(read("build.gradle.kts").contains("version = \"" + RELEASE_VERSION + "\""));
		assertTrue(read("CITATION.cff").contains("version: \"" + RELEASE_VERSION + "\""));
		assertTrue(read(".zenodo.json").contains("\"version\": \"" + RELEASE_VERSION + "\""));
		assertTrue(read("CHANGELOG.md").contains("## [" + RELEASE_VERSION + "]"));
	}

    @org.junit.jupiter.api.Test
    void generatedProcessingMetadataKeepsReleaseQualificationFields() throws Exception {
        java.util.Properties release = new java.util.Properties();
        java.util.Properties generated = new java.util.Properties();

        try (java.io.Reader reader = java.nio.file.Files.newBufferedReader(
                java.nio.file.Path.of("release.properties"),
                java.nio.charset.StandardCharsets.UTF_8)) {
            release.load(reader);
        }
        try (java.io.Reader reader = java.nio.file.Files.newBufferedReader(
                java.nio.file.Path.of("library.properties"),
                java.nio.charset.StandardCharsets.UTF_8)) {
            generated.load(reader);
        }

        for (String key : new String[] {
                "name", "version", "authors", "url", "categories", "sentence",
                "paragraph", "minRevision", "maxRevision", "library.copyright",
                "library.dependencies", "library.keywords"
        }) {
            org.junit.jupiter.api.Assertions.assertEquals(
                    release.getProperty(key), generated.getProperty(key),
                    "generated Processing metadata must preserve release.properties: " + key);
        }

        org.junit.jupiter.api.Assertions.assertEquals("2.0.0", generated.getProperty("prettyVersion"));
        org.junit.jupiter.api.Assertions.assertEquals("1285", generated.getProperty("minRevision"));

        org.junit.jupiter.api.Assertions.assertTrue(
                generated.containsKey("tested.platform"),
                "generated metadata keeps the optional qualification field");
        org.junit.jupiter.api.Assertions.assertTrue(
                generated.containsKey("tested.processingVersion"),
                "generated metadata keeps the optional qualification field");
        org.junit.jupiter.api.Assertions.assertTrue(
                generated.getProperty("tested.platform", "").isBlank(),
                "tested.platform must stay blank until release qualification evidence supports a claim");
        org.junit.jupiter.api.Assertions.assertTrue(
                generated.getProperty("tested.processingVersion", "").isBlank(),
                "tested.processingVersion must stay blank until release qualification evidence supports a claim");

        String keywords = generated.getProperty("library.keywords", "");
        org.junit.jupiter.api.Assertions.assertFalse(
                java.util.regex.Pattern.compile("(?i)(^|[,\\s])(VR|XR)([,\\s]|$)")
                        .matcher(keywords).find(),
                "generic VR/XR keywords are outside the 2.0 public contract");
    }

    @org.junit.jupiter.api.Test
    void releasePackageAndWorkflowsKeepPublicationGates() throws Exception {
        String build = java.nio.file.Files.readString(java.nio.file.Path.of("build.gradle.kts"));
        String automated = java.nio.file.Files.readString(
                java.nio.file.Path.of(".github/workflows/automated-qualification.yml"));
        String preRelease = java.nio.file.Files.readString(
                java.nio.file.Path.of(".github/workflows/pre-release.yml"));
        String release = java.nio.file.Files.readString(
                java.nio.file.Path.of(".github/workflows/release.yml"));

        org.junit.jupiter.api.Assertions.assertTrue(build.contains("verifyProcessingPackage"));
        org.junit.jupiter.api.Assertions.assertTrue(build.contains("buildReleaseArtifacts"));

        // Continuous automated qualification.
        org.junit.jupiter.api.Assertions.assertTrue(
                automated.contains("./gradlew qualificationTests --console=plain"));
        org.junit.jupiter.api.Assertions.assertTrue(
                automated.contains("python3 tools/validate_documentation.py --root ."));
        org.junit.jupiter.api.Assertions.assertTrue(
                automated.contains("python3 -m mkdocs build --strict"));
        org.junit.jupiter.api.Assertions.assertTrue(
                automated.contains("actions/upload-artifact@v4"));

        // The actual release gate happens before a tag is created.
        org.junit.jupiter.api.Assertions.assertTrue(
                preRelease.contains("./gradlew clean test build --console=plain"));
        org.junit.jupiter.api.Assertions.assertTrue(
                preRelease.contains("./gradlew qualificationTests --console=plain"));
        org.junit.jupiter.api.Assertions.assertTrue(preRelease.contains("--release-evidence"));
        org.junit.jupiter.api.Assertions.assertTrue(
                preRelease.contains("python3 -m mkdocs build --strict"));
        org.junit.jupiter.api.Assertions.assertTrue(
                preRelease.contains("./gradlew buildReleaseArtifacts --console=plain"));
        org.junit.jupiter.api.Assertions.assertTrue(
                preRelease.contains("--package release/ziviDomeLive.zip"));
        org.junit.jupiter.api.Assertions.assertTrue(
                preRelease.contains("actions/upload-artifact@v4"));

        // A tag reproduces and audits the already-qualified revision; it is not the
        // first place where release readiness is discovered.
        org.junit.jupiter.api.Assertions.assertTrue(release.contains("./gradlew verifyReleaseTag"));
        org.junit.jupiter.api.Assertions.assertTrue(
                release.contains("./gradlew qualificationTests --console=plain"));
        org.junit.jupiter.api.Assertions.assertTrue(
                release.contains("Upload tagged qualification evidence"));
        org.junit.jupiter.api.Assertions.assertTrue(release.contains("--release-evidence"));
        org.junit.jupiter.api.Assertions.assertTrue(
                release.contains("./gradlew buildReleaseArtifacts --console=plain"));
        org.junit.jupiter.api.Assertions.assertTrue(
                release.contains("--package release/ziviDomeLive.zip"));
        org.junit.jupiter.api.Assertions.assertTrue(release.contains("softprops/action-gh-release@v2"));
    }

	private static Properties loadProperties(String relativePath) throws IOException {
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(PROJECT_ROOT.resolve(relativePath))) {
			properties.load(input);
		}
		return properties;
	}

	private static String read(String relativePath) throws IOException {
		return Files.readString(PROJECT_ROOT.resolve(relativePath));
	}
}
