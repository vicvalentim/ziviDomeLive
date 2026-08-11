package com.victorvalentim.zividomelive.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExampleQualityTest {

	private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));
	private static final Path EXAMPLES = PROJECT_ROOT.resolve("examples");

	@Test
	void maintainedSketchesUseCurrentProcessingSetupContract() throws IOException {
		for (String relativePath : List.of(
				"Basic/Basic.pde",
				"EmptyProject/EmptyProject.pde",
				"SphereParticle/SphereParticle.pde",
				"CompatibilityLock/CompatibilityLock.pde",
				"FulldomePBR/FulldomePBR.pde")) {
			String source = read(relativePath);
			assertTrue(source.matches("(?s).*size\\([^;]*P3D\\s*\\);.*"), relativePath);
			assertTrue(source.contains("pixelDensity(1)"), relativePath);
			assertTrue(source.contains("ziviDome.setup()"), relativePath);
			assertFalse(source.contains("ziviDome.draw();"), relativePath);
		}
	}

	@Test
	void maintainedSketchesDeclareProcessingRuntimeDependencies() throws IOException {
		for (String relativePath : List.of(
				"Basic/Basic.pde",
				"EmptyProject/EmptyProject.pde",
				"SphereParticle/SphereParticle.pde",
				"CompatibilityLock/CompatibilityLock.pde",
				"FulldomePBR/FulldomePBR.pde")) {
			String source = read(relativePath);
			assertTrue(source.contains("import controlP5.*;"), relativePath);
			assertTrue(source.contains("import codeanticode.syphon.*;"), relativePath);
			assertTrue(source.contains("import spout.*;"), relativePath);
		}
	}

	@Test
	void examplesUsingDirectSceneRegistrationAvoidRedundantManagers() throws IOException {
		for (String relativePath : List.of(
				"CompatibilityLock/CompatibilityLock.pde",
				"FulldomePBR/FulldomePBR.pde")) {
			String source = read(relativePath);
			assertTrue(source.contains("ziviDome.setScene("), relativePath);
			assertFalse(source.contains("new SceneManager()"), relativePath);
		}
	}

	@Test
	void compatibilityLockUsesNinetyDegreeOrientationSteps() throws IOException {
		String source = read("CompatibilityLock/CalibrationScene.pde");
		assertTrue(source.contains("setPitch(dome.getPitch() + HALF_PI)"));
		assertTrue(source.contains("setYaw(dome.getYaw() + HALF_PI)"));
		assertTrue(source.contains("setRoll(dome.getRoll() + HALF_PI)"));
		assertFalse(source.contains("HALF_PI / 2"));
	}

	@Test
	void compatibilityLockProvidesAlignmentAndGlslColorReferences() throws IOException {
		String source = read("CompatibilityLock/CalibrationScene.pde");
		String vertexShader = read("CompatibilityLock/data/calibration-colors.vert");
		String fragmentShader = read("CompatibilityLock/data/calibration-colors.frag");

		assertTrue(source.contains("drawAlignmentGrid"));
		assertTrue(source.contains("drawOrientationCues"));
		assertTrue(source.contains("drawColorBarLabels"));
		assertTrue(source.contains("drawGrayRampLabels"));
		assertTrue(source.contains("pg.noLights()"));
		assertTrue(vertexShader.startsWith("#version 410 core"));
		assertTrue(fragmentShader.startsWith("#version 410 core"));
		assertTrue(fragmentShader.contains("vec3 colorBar(int index)"));
		assertTrue(fragmentShader.contains("float level = float(index) / 8.0"));
	}

	private static String read(String relativePath) throws IOException {
		return Files.readString(EXAMPLES.resolve(relativePath));
	}
}
