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
	private static final String RELEASE_VERSION = "1.5.0";
	private static final String PROCESSING_RELEASE_NUMBER = "10";

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
	void generatedProcessingMetadataKeepsReleaseQualificationFields() throws IOException {
		Properties release = loadProperties("release.properties");
		String build = read("build.gradle.kts");

		for (String key : new String[]{
				"tested.platform",
				"tested.processingVersion",
				"library.copyright",
				"library.dependencies",
				"library.keywords"}) {
			assertTrue(release.containsKey(key), key);
			assertTrue(build.contains("property(\"" + key + "\""), key);
		}
		assertEquals("4.5.6", release.getProperty("tested.processingVersion"));
	}

	@Test
	void releaseDocumentationExposesNewApiAndQualificationBoundary() throws IOException {
		String readme = read("README.md");
		String qualification = read("docs/en/qualification/1.5-release-readiness.md");

		assertTrue(readme.contains("setRenderMode(RenderMode.FULL)"));
		assertTrue(readme.contains("OutputManager.OutputState"));
		assertTrue(readme.contains("Platform Matrix"));
		assertTrue(qualification.contains("GPU visual compatibility"));
		assertTrue(qualification.contains("No golden images"));
	}

	@Test
	void releasePackageAndWorkflowsKeepPublicationGates() throws IOException {
		String build = read("build.gradle.kts");
		String releaseWorkflow = read(".github/workflows/release.yml");
		String websiteWorkflow = read(".github/workflows/deploy_website.yml");
		String previewWorkflow = read(".github/workflows/pr_preview.yml");
		String copilotInstructions = read(".github/copilot-instructions.md");
		String documentationRequirements = read("requirements-docs.txt");

		for (String requiredFile : new String[]{
				"LICENSE", "CHANGELOG.md", "CITATION.cff", ".zenodo.json",
				"THIRD_PARTY.md", "licenses/Apache-2.0.txt"}) {
			assertTrue(build.contains("\"" + requiredFile + "\""), requiredFile);
		}
		assertTrue(build.contains("verifyReleaseTag"));
		assertTrue(build.contains("src/main/libs/**"));
		assertTrue(releaseWorkflow.contains("contents: write"));
		assertFalse(releaseWorkflow.contains("write-all"));
		assertTrue(releaseWorkflow.contains("verifyReleaseTag"));
		assertTrue(releaseWorkflow.contains("RELEASE_TAG: ${{ github.ref_name }}"));
		assertTrue(releaseWorkflow.contains("-PreleaseTag=\"$RELEASE_TAG\""));
		assertTrue(releaseWorkflow.contains("github.ref_name"));
		assertTrue(releaseWorkflow.contains("fail_on_unmatched_files: true"));
		assertTrue(websiteWorkflow.contains("pip install -r requirements-docs.txt"));
		assertTrue(websiteWorkflow.contains("mkdocs build --strict"));
		assertTrue(previewWorkflow.contains("pip install -r requirements-docs.txt"));
		assertTrue(previewWorkflow.contains("mkdocs build --strict"));
		assertTrue(documentationRequirements.contains("mkdocs>=1.6.1,<2.0"));
		assertTrue(documentationRequirements.contains("mkdocs-material>=9.7.7,<10.0"));
		assertTrue(copilotInstructions.contains("examples/CalibrationTool/"));
		assertFalse(copilotInstructions.contains("examples/CompatibilityLock/"));
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
