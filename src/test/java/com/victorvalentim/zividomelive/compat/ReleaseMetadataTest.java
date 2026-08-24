package com.victorvalentim.zividomelive.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
        String build = read("build.gradle.kts");
        assertTrue(build.contains("compileProcessing4Baseline"));
        assertTrue(build.contains("processing4BaselineLibrary"));
        assertFalse(release.containsKey("library.dependencies"));
        assertFalse(generated.containsKey("library.dependencies"));
        assertFalse(build.contains("property(\"library.dependencies\""));

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
	void examplesRequireControlP5WithoutRequiringPlatformOutputLibraries() throws IOException {
		for (String relativePath : List.of(
				"GettingStarted/EmptyProject", "GettingStarted/Basic",
				"GettingStarted/NamedActions", "GettingStarted/PortLoopback",
				"Advanced/SphereParticle", "Advanced/InfiniteBackground",
				"Advanced/FulldomePBR", "Advanced/SolarSystem")) {
			String example = Path.of(relativePath).getFileName().toString();
			String source = read("examples/" + relativePath + "/" + example + ".pde");
			assertTrue(source.contains("import controlP5.*;"), example);
			assertFalse(source.contains("import codeanticode.syphon."), example);
			assertFalse(source.contains("import spout."), example);
			assertFalse(source.contains("import com.victorvalentim.zividomelive.render."), example);
			assertFalse(source.contains("setSceneManager("), example);
		}
		assertTrue(Files.isDirectory(PROJECT_ROOT.resolve("examples/Tools/CalibrationTool")));
		assertTrue(Files.isDirectory(PROJECT_ROOT.resolve("examples/Tools/BenchmarkTool")));
		for (String relativePath : List.of(
				"GettingStarted/EmptyProject", "GettingStarted/Basic",
				"GettingStarted/NamedActions", "GettingStarted/PortLoopback",
				"Advanced/SphereParticle", "Advanced/InfiniteBackground",
				"Advanced/FulldomePBR", "Advanced/SolarSystem",
				"Tools/CalibrationTool", "Tools/BenchmarkTool")) {
			assertTrue(
					Files.isRegularFile(PROJECT_ROOT.resolve("examples/" + relativePath + "/README.md")),
					relativePath + " must include its own README.md");
			String example = Path.of(relativePath).getFileName().toString();
			String source = read("examples/" + relativePath + "/" + example + ".pde");
			assertTrue(source.contains("import controlP5.*;"), relativePath);
			assertFalse(source.contains("import codeanticode.syphon."), relativePath);
			assertFalse(source.contains("import spout."), relativePath);
		}
		for (String quickstart : List.of(
				"docs/en/getting-started/quickstart.md",
				"docs/pt/getting-started/quickstart.md")) {
			String source = read(quickstart);
			assertTrue(source.contains("import controlP5.*;"), quickstart);
			assertFalse(source.contains("import codeanticode.syphon."), quickstart);
			assertFalse(source.contains("import spout."), quickstart);
		}
	}

	@Test
	void newExamplesCalibrationAndSecurityMatchTheFinalTwoPointZeroContracts()
			throws IOException {
		String actions = read("examples/GettingStarted/NamedActions/NamedActions.pde");
		assertTrue(actions.contains("applyWithViewLighting(pg)"));
		assertTrue(actions.contains("snapToAxisAngle("));
		assertTrue(actions.contains("camera.setInputEnabled(true);"));
		assertTrue(actions.contains("camera.orbit().setWheelSteps(-80f, -0.001f);"));
		assertTrue(actions.contains("MouseEvent.CLICK"));
		assertTrue(actions.contains("-1100f"));
		assertFalse(actions.contains("ziviDome.setPitch("));
		assertFalse(actions.contains("ziviDome.setYaw("));
		assertFalse(actions.contains("ziviDome.setRoll("));
		assertFalse(actions.contains("Quaternion"));
		assertTrue(actions.contains("pg.sphere("));
		assertTrue(actions.contains("pg.box("));
		assertTrue(actions.contains("public void update()"));

		String ports = read("examples/GettingStarted/PortLoopback/PortLoopback.pde");
		assertTrue(ports.contains("services.ports().connectInput"));
		assertTrue(ports.contains("snapToAxisAngle("));
		assertTrue(ports.contains("camera.setInputEnabled(true);"));
		assertTrue(ports.contains("camera.orbit().setWheelSteps(-80f, -0.001f);"));
		assertTrue(ports.contains("-1150f"));
		assertFalse(ports.contains("ziviDome.setPitch("));
		assertFalse(ports.contains("ziviDome.setYaw("));
		assertFalse(ports.contains("ziviDome.setRoll("));
		assertFalse(ports.contains("Quaternion"));
		assertTrue(ports.contains("services.ports().connectOutput"));
		assertTrue(ports.contains("pg.vertex("));
		assertTrue(ports.contains("pg.sphere("));
		assertTrue(ports.contains("pg.box("));
		assertTrue(ports.contains("public void update()"));

		String calibration = read(
				"examples/Tools/CalibrationTool/BourkeEnvironmentScene.pde");
		String calibrationTool = read(
				"examples/Tools/CalibrationTool/CalibrationTool.pde");
		assertTrue(calibration.contains("services.environment()"));
		assertTrue(calibration.contains("environment.setEquirectangular(pattern)"));
		assertTrue(calibration.contains(
				"environment.setOrientationAxisAngle(1f, 0f, 0f, SOURCE_PITCH)"));
		assertTrue(calibration.contains("pg.background(0, 0, 0, 0)"));
		assertFalse(calibration.contains("sphereVertex("));
		assertFalse(Files.exists(PROJECT_ROOT.resolve(
				"examples/Tools/CalibrationTool/BourkeSphereScene.pde")));
		assertTrue(calibrationTool.contains(
				"ziviDome.setScene(new BourkeEnvironmentScene(ziviDome));"));
		assertTrue(calibrationTool.contains(
				"ziviDome.registerScene(new CubeCalibrationScene());"));
		assertTrue(calibrationTool.indexOf("setScene(new BourkeEnvironmentScene")
				< calibrationTool.indexOf("registerScene(new CubeCalibrationScene"));
		assertTrue(calibrationTool.contains("ziviDome.setCurrentView(ViewType.DOMEMASTER);"));
		assertTrue(calibrationTool.contains("ziviDome.setFishSize(100f);"));
		assertTrue(calibrationTool.contains("ziviDome.setFov(210f);"));
		assertTrue(calibrationTool.contains("ziviDome.setPitch(0f);"));
		assertTrue(calibrationTool.contains("ziviDome.setYaw(0f);"));
		assertTrue(calibrationTool.contains("ziviDome.setRoll(0f);"));
		assertTrue(calibrationTool.indexOf("resetCalibrationState();")
				< calibrationTool.indexOf("printCalibrationState();"));

		String security = read("SECURITY.md");
		assertTrue(security.contains("| 2.0.x   | Yes"));
		assertTrue(security.contains("| 1.x.x   | No"));
		assertTrue(security.contains("Do not open a public issue"));
		assertTrue(security.contains("Report a vulnerability"));
	}

    @Test
    void releasePackageAndWorkflowsKeepPublicationGates() throws IOException {
        String build = read("build.gradle.kts");
        String automated = read(".github/workflows/automated-qualification.yml");
        String preRelease = read(".github/workflows/pre-release.yml");
        String release = read(".github/workflows/release.yml");
        String dependencySubmission = read(".github/workflows/gradle.yml");

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
        assertTrue(preRelease.contains("checkout-revision.txt"));
        assertTrue(preRelease.contains("--release-evidence"));
        assertTrue(preRelease.contains("python3 -m mkdocs build --strict"));
        assertTrue(preRelease.contains("./gradlew attachJavadocsToSite --console=plain"));
        assertTrue(preRelease.contains("--site-dir site"));
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
        assertTrue(release.contains("actions/setup-java@v4"));
        assertTrue(release.contains("java-version: 17"));
        assertTrue(release.contains("actions/setup-python@v5"));
        assertTrue(release.contains("python3 -m pip install -r requirements-docs.txt"));
        assertTrue(release.contains("./gradlew clean test build --console=plain"));
        assertTrue(release.contains("./gradlew qualificationTests --console=plain"));
        assertTrue(release.contains("checkout-revision.txt"));
        assertTrue(release.contains("actions/upload-artifact@v4"));
        assertTrue(release.contains("build/reports/qualification/"));
        assertTrue(release.contains("build/test-results/qualification/"));
        assertTrue(release.contains("--release-evidence"));
        assertTrue(release.contains("python3 -m mkdocs build --strict"));
        assertTrue(release.contains("./gradlew attachJavadocsToSite --console=plain"));
        assertTrue(release.contains("--site-dir site"));
        assertTrue(release.contains("./gradlew buildReleaseArtifacts --console=plain"));
        assertTrue(release.contains("--package release/ziviDomeLive.zip"));
        assertTrue(release.contains("softprops/action-gh-release@v2"));
        assertTrue(release.indexOf("actions/upload-artifact@v4")
                < release.indexOf("./gradlew buildReleaseArtifacts --console=plain"),
                "tagged qualification evidence must be archived before buildReleaseArtifacts runs clean");

        assertTrue(dependencySubmission.contains("name: Gradle Dependency Submission"));
        assertTrue(dependencySubmission.contains("dependency-submission:"));
        assertFalse(dependencySubmission.contains("build -x test"));
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
