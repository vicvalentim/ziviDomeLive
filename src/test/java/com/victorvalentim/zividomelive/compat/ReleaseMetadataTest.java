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
	private static final String PROJECT_LICENSE = "Apache-2.0";

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

    @Test
    void releaseLicenseAndSolarSystemProvenanceAreAligned() throws IOException {
        assertTrue(read("CITATION.cff").contains("license: " + PROJECT_LICENSE));
        assertTrue(read(".zenodo.json").contains("\"license\": \"" + PROJECT_LICENSE + "\""));
        assertTrue(read("LICENSE").contains("Apache License"));
        assertTrue(read("LICENSE").contains("Version 2.0, January 2004"));

        String thirdParty = read("THIRD_PARTY.md");
        assertTrue(thirdParty.contains("Solar System Scope"));
        assertTrue(thirdParty.contains("INOVE"));
        assertTrue(thirdParty.contains("CC BY 4.0"));
        assertTrue(thirdParty.contains("ESO/S. Brunier"));
        assertTrue(thirdParty.contains("NASA/JPL"));

        String solarNotice = read("examples/Advanced/SolarSystem/THIRD_PARTY.md");
        assertTrue(solarNotice.contains("JPL Solar System Dynamics"));
        assertTrue(solarNotice.contains("Solar System Scope"));
        assertTrue(solarNotice.contains("NASA is an upstream data/imagery source"));
        assertTrue(solarNotice.contains("ESO/S. Brunier"));
        assertTrue(solarNotice.contains("CC BY 4.0"));

        String provenance = read("examples/Advanced/SolarSystem/ASSET_PROVENANCE.json");
        assertTrue(provenance.contains("\"projectLicense\": \"" + PROJECT_LICENSE + "\""));
        assertTrue(provenance.contains("\"license\": \"CC-BY-4.0\""));
        assertTrue(provenance.contains("\"creatorCredit\": \"ESO/S. Brunier\""));

        assertFalse(
                Files.exists(PROJECT_ROOT.resolve(
                        "examples/Advanced/SolarSystem/data/textures/background.jpg")),
                "unresolved SolarSystem background.jpg must not be present in a release-ready tree");
    }

    @Test
    void generatedProcessingMetadataKeepsReleaseQualificationFields() throws IOException {
        Properties release = loadProperties("release.properties");
        Properties generated = loadProperties("library.properties");

        for (String key : new String[] {
                "name", "version", "authors", "url", "categories", "sentence",
                "paragraph", "minRevision", "maxRevision", "tested.platform",
                "tested.processingVersion", "library.copyright",
                "library.keywords"
        }) {
            assertEquals(
                    release.getProperty(key), generated.getProperty(key),
                    "generated Processing metadata must preserve release.properties: " + key);
        }

        assertEquals("2.0.0", generated.getProperty("prettyVersion"));
        assertEquals("1285", generated.getProperty("minRevision"));

        String evidence = read("maintainer/release-evidence.md");
        boolean releaseEvidenceComplete = !java.util.regex.Pattern
                .compile("\\b(?:UNVERIFIED|PENDING)\\b|\\[ \\]")
                .matcher(evidence)
                .find();
        if (!releaseEvidenceComplete) {
            assertTrue(
                    generated.getProperty("tested.platform", "").isBlank(),
                    "tested.platform must stay blank until release qualification evidence is complete");
            assertTrue(
                    generated.getProperty("tested.processingVersion", "").isBlank(),
                    "tested.processingVersion must stay blank until release qualification evidence is complete");
        }

        String keywords = generated.getProperty("library.keywords", "");
        assertFalse(
                java.util.regex.Pattern.compile("(?i)(^|[,\\s])(VR|XR)([,\\s]|$)")
                        .matcher(keywords).find(),
                "generic VR/XR keywords are outside the 2.0 public contract");

        String zenodo = read(".zenodo.json");
        assertFalse(
                java.util.regex.Pattern.compile("(?i)(^|[\\\"',:\\s])(VR|XR)([\\\"',:\\s]|$)")
                        .matcher(zenodo).find(),
                "Zenodo metadata must not reintroduce a generic VR/XR product identity");
    }

    @Test
    void releasePackageAndWorkflowsKeepPublicationGates() throws IOException {
        String build = read("build.gradle.kts");
        String automated = read(".github/workflows/automated-qualification.yml");
        String preRelease = read(".github/workflows/pre-release.yml");
        String release = read(".github/workflows/release.yml");

        assertTrue(build.contains("verifyProcessingPackage"));
        assertTrue(build.contains("buildReleaseArtifacts"));

        // Continuous automated qualification.
        assertTrue(automated.contains("./gradlew qualificationTests --console=plain"));
        assertTrue(automated.contains("python3 tools/validate_documentation.py --root ."));
        assertTrue(automated.contains("python3 -m mkdocs build --strict"));
        assertTrue(automated.contains("actions/upload-artifact@v4"));

        // The actual release gate happens before a tag is created.
        assertTrue(preRelease.contains("./gradlew clean test build --console=plain"));
        assertTrue(preRelease.contains("./gradlew qualificationTests --console=plain"));
        assertTrue(preRelease.contains("--release-evidence"));
        assertTrue(preRelease.contains("python3 -m mkdocs build --strict"));
        assertTrue(preRelease.contains("./gradlew buildReleaseArtifacts --console=plain"));
        assertTrue(preRelease.contains("--package release/ziviDomeLive.zip"));
        assertTrue(preRelease.contains("actions/upload-artifact@v4"));
        assertTrue(preRelease.contains("build/reports/qualification/"));
        assertTrue(preRelease.indexOf("actions/upload-artifact@v4")
                < preRelease.indexOf("./gradlew buildReleaseArtifacts --console=plain"),
                "qualification evidence must be archived before buildReleaseArtifacts runs clean");

        // A tag reproduces and audits the already-qualified revision; it is not the
        // first place where release readiness is discovered.
        assertTrue(release.contains("./gradlew verifyReleaseTag"));
        assertTrue(release.contains("./gradlew qualificationTests --console=plain"));
        assertTrue(release.contains("actions/upload-artifact@v4"));
        assertTrue(release.contains("build/reports/qualification/"));
        assertTrue(release.contains("build/test-results/qualification/"));
        assertTrue(release.contains("--release-evidence"));
        assertTrue(release.contains("./gradlew buildReleaseArtifacts --console=plain"));
        assertTrue(release.contains("--package release/ziviDomeLive.zip"));
        assertTrue(release.contains("softprops/action-gh-release@v2"));
        assertTrue(release.indexOf("actions/upload-artifact@v4")
                < release.indexOf("./gradlew buildReleaseArtifacts --console=plain"),
                "tagged qualification evidence must be archived before buildReleaseArtifacts runs clean");
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
