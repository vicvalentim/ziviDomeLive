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

	@Test
	void generatedProcessingMetadataKeepsReleaseQualificationFields() throws IOException {
		Properties release = loadProperties("release.properties");
		Properties library = loadProperties("library.properties");
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
		assertEquals("https://vicvalentim.github.io/ziviDomeLive/", release.getProperty("url"));
		assertEquals("https://vicvalentim.github.io/ziviDomeLive/", library.getProperty("url"));
		assertEquals("Renders fulldome, equirectangular, skybox, and Standard views from Processing sketches.",
				library.getProperty("sentence"));
		assertFalse(library.getProperty("sentence").toLowerCase().contains("library"));
		assertTrue(library.getProperty("paragraph").contains("platform-specific dependencies"));
	}

	@Test
	void releaseDocumentationExposesNewApiAndQualificationBoundary() throws IOException {
		String readme = read("README.md");
		String qualification = read("docs/en/qualification/2.0-release-readiness.md");
		String publication = read("docs/en/qualification/processing-publication.md");

		assertTrue(readme.contains("setRenderMode(RenderMode.FULL)"));
		assertTrue(readme.contains("OutputManager.OutputState"));
		assertTrue(readme.contains("Platform Matrix"));
		assertTrue(readme.contains("Processing Publication"));
		assertTrue(qualification.contains("native cubemap"));
		assertTrue(qualification.contains("CalibrationTool"));
		assertTrue(qualification.contains("No golden images"));
		assertTrue(qualification.contains("Processing Publication"));
		assertTrue(publication.contains("reference/index.html"));
		assertTrue(publication.contains("ziviDomeLive.zip"));
		assertTrue(publication.contains("Contribution Manager"));
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
		assertTrue(websiteWorkflow.contains("cp -R build/docs/javadoc site/reference"));
		assertTrue(websiteWorkflow.contains("cp -R build/docs/javadoc site/pt/reference"));
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
