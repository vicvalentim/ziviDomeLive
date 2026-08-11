package com.victorvalentim.zividomelive.compat;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
				"CalibrationTool/CalibrationTool.pde",
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
				"CalibrationTool/CalibrationTool.pde",
				"FulldomePBR/FulldomePBR.pde")) {
			String source = read(relativePath);
			assertTrue(source.contains("import controlP5.*;"), relativePath);
			assertTrue(source.contains("import codeanticode.syphon.*;"), relativePath);
			assertTrue(source.contains("import spout.*;"), relativePath);
		}
	}

	@Test
	void fulldomePbrUsesDirectSceneRegistration() throws IOException {
		String source = read("FulldomePBR/FulldomePBR.pde");
		assertTrue(source.contains("ziviDome.setScene("));
		assertFalse(source.contains("new SceneManager()"));
	}

	@Test
	void calibrationToolUsesTwoScenesAndNinetyDegreeOrientationSteps() throws IOException {
		String sketch = read("CalibrationTool/CalibrationTool.pde");
		assertTrue(sketch.contains("new SceneManager()"));
		assertTrue(sketch.contains("new CubeCalibrationScene(ziviDome)"));
		assertTrue(sketch.contains("new BourkeSphereScene(ziviDome)"));
		assertTrue(sketch.contains("setPitch(ziviDome.getPitch() + HALF_PI)"));
		assertTrue(sketch.contains("setYaw(ziviDome.getYaw() + HALF_PI)"));
		assertTrue(sketch.contains("setRoll(ziviDome.getRoll() + HALF_PI)"));
		assertFalse(sketch.contains("HALF_PI / 2"));
	}

	@Test
	void calibrationToolProvidesPrecisionGlslAndOriginalBourkeReference()
			throws IOException, NoSuchAlgorithmException {
		String cubeScene = read("CalibrationTool/CubeCalibrationScene.pde");
		String sphereScene = read("CalibrationTool/BourkeSphereScene.pde");
		String vertexShader = read("CalibrationTool/data/cube-calibration.vert");
		String fragmentShader = read("CalibrationTool/data/cube-calibration.frag");
		String thirdPartyNotice = read("CalibrationTool/THIRD_PARTY.md");

		assertTrue(cubeScene.contains("TARGET_SIZE = 1800f"));
		assertTrue(cubeScene.contains("GRID_DIVISIONS = 24"));
		assertTrue(cubeScene.contains("pg.noLights()"));
		assertTrue(vertexShader.startsWith("#version 410 core"));
		assertTrue(fragmentShader.startsWith("#version 410 core"));
		assertTrue(fragmentShader.contains("starBurst"));
		assertTrue(fragmentShader.contains("blackColorWhiteRamp"));
		assertTrue(fragmentShader.contains("clippingLevel"));

		assertTrue(sphereScene.contains("ROTATION_PERIOD_SECONDS = 60f"));
		assertTrue(sphereScene.contains("SPHERE_CENTER_X = 0f"));
		assertTrue(sphereScene.contains("SPHERE_CENTER_Y = 0f"));
		assertTrue(sphereScene.contains("SPHERE_CENTER_Z = 0f"));
		assertTrue(sphereScene.contains("SPHERE_DIAMETER = 1800f"));
		assertTrue(sphereScene.contains("float latitude0 = HALF_PI - PI * v0"));
		assertTrue(sphereScene.contains("SPHERE_CENTER_Z + SPHERE_RADIUS * sin(latitude)"));
		assertTrue(sphereScene.contains("pg.rotateZ(patternRotation)"));
		assertTrue(sphereScene.contains("pg.textureSampling(POINT)"));
		assertTrue(sphereScene.contains("pg.hint(DISABLE_TEXTURE_MIPMAPS)"));

		Path imagePath = EXAMPLES.resolve(
				"CalibrationTool/data/spherical8192.png");
		BufferedImage image = ImageIO.read(imagePath.toFile());
		assertEquals(8192, image.getWidth());
		assertEquals(4096, image.getHeight());
		String digest = HexFormat.of().formatHex(
				MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(imagePath)));
		assertEquals("e54f3eecf6de84218bd8e7b061e428c5d3c4ec70ed981b632b0d48e4f54d952f", digest);
		assertTrue(thirdPartyNotice.contains("Fulldome test pattern by Paul Bourke."));
		assertTrue(thirdPartyNotice.contains("not modified"));
		assertTrue(thirdPartyNotice.contains("license notice remains included"));
		assertFalse(Files.exists(EXAMPLES.resolve("CompatibilityLock")));
	}

	private static String read(String relativePath) throws IOException {
		return Files.readString(EXAMPLES.resolve(relativePath));
	}
}
