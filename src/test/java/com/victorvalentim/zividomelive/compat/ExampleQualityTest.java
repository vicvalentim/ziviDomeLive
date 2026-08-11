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
		assertTrue(sketch.contains("setTargetFrameRate(60)"));
		assertFalse(sketch.contains("HALF_PI / 2"));
	}

	@Test
	void calibrationToolProvidesPrecisionGlslAndOriginalBourkeReferences()
			throws IOException, NoSuchAlgorithmException {
		String cubeScene = read("CalibrationTool/CubeCalibrationScene.pde");
		String sphereScene = read("CalibrationTool/BourkeSphereScene.pde");
		String vertexShader = read("CalibrationTool/data/cube-calibration.vert");
		String fragmentShader = read("CalibrationTool/data/cube-calibration.frag");
		String thirdPartyNotice = read("CalibrationTool/THIRD_PARTY.md");

		assertTrue(cubeScene.contains("TARGET_SIZE = 1800f"));
		assertTrue(cubeScene.contains("GRID_DIVISIONS = 24"));
		assertTrue(cubeScene.contains("pg.noLights()"));
		assertTrue(cubeScene.contains("drawMappedFacePattern"));
		assertTrue(cubeScene.contains("pg.beginShape(QUADS)"));
		assertFalse(cubeScene.contains("pg.hint(DISABLE_DEPTH_TEST)"));
		assertTrue(cubeScene.contains("ANNOTATION_TEXTURE_SIZE = 1024"));
		assertTrue(cubeScene.contains("pg.texture(annotationMaps[index])"));
		assertTrue(cubeScene.contains("ANNOTATION_BIAS = 2f"));
		assertTrue(vertexShader.startsWith("#version 410 core"));
		assertTrue(vertexShader.contains("in vec2 texCoord"));
		assertTrue(vertexShader.contains("faceUv = texCoord"));
		assertTrue(fragmentShader.startsWith("#version 410 core"));
		assertTrue(fragmentShader.contains("in vec2 faceUv"));
		assertTrue(fragmentShader.contains("uniform sampler2D annotationMap"));
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
		assertTrue(sphereScene.contains("dome.isEnableOutput()"));
		assertTrue(sphereScene.contains("dome.getOutputResolution()"));
		assertTrue(sphereScene.contains("min(dome.getPApplet().width, dome.getPApplet().height)"));
		assertTrue(sphereScene.contains("dome.getTargetFrameRate()"));
		assertTrue(sphereScene.contains("framesPerRevolution()"));
		assertFalse(sphereScene.contains("deltaSeconds"));

		for (BourkeImage expected : List.of(
				new BourkeImage("spherical2400.png", 2400, 1200,
						"96ab696ee684b851efbe274e78415b5bd50a2ba57330ed68fd2debb8ef7847af"),
				new BourkeImage("spherical4096.png", 4096, 2048,
						"eff4da1fac68089208b32cd72d736a44997290be467b6260546877e138098ca8"),
				new BourkeImage("spherical4800.png", 4800, 2400,
						"2f636316d4499a203baacb26c74b90f99553a21cf4036d368cd0e3ea87df20d9"),
				new BourkeImage("spherical8192.png", 8192, 4096,
						"e54f3eecf6de84218bd8e7b061e428c5d3c4ec70ed981b632b0d48e4f54d952f"))) {
			Path imagePath = EXAMPLES.resolve("CalibrationTool/data/img").resolve(expected.name());
			BufferedImage image = ImageIO.read(imagePath.toFile());
			assertEquals(expected.width(), image.getWidth(), expected.name());
			assertEquals(expected.height(), image.getHeight(), expected.name());
			String digest = HexFormat.of().formatHex(
					MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(imagePath)));
			assertEquals(expected.sha256(), digest, expected.name());
			assertTrue(thirdPartyNotice.contains(expected.name()));
			assertTrue(thirdPartyNotice.contains(expected.sha256()));
		}
		assertFalse(Files.exists(EXAMPLES.resolve("CalibrationTool/data/spherical8192.png")));
		assertTrue(thirdPartyNotice.contains("Fulldome test pattern by Paul Bourke."));
		assertTrue(thirdPartyNotice.contains("not modified"));
		assertTrue(thirdPartyNotice.contains("license notice remains included"));
		assertFalse(Files.exists(EXAMPLES.resolve("CompatibilityLock")));
	}

	private record BourkeImage(String name, int width, int height, String sha256) {
	}

	private static String read(String relativePath) throws IOException {
		return Files.readString(EXAMPLES.resolve(relativePath));
	}
}
