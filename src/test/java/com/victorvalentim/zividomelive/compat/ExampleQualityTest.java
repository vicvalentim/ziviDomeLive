package com.victorvalentim.zividomelive.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

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
	void examplesDoNotOwnWorkerPools() throws IOException {
		try (Stream<Path> files = Files.walk(EXAMPLES)) {
			for (Path file : files
					.filter(path -> path.toString().endsWith(".pde"))
					.filter(path -> !path.startsWith(EXAMPLES.resolve("SolarSystem")))
					.toList()) {
				String source = Files.readString(file);
				String relativePath = PROJECT_ROOT.relativize(file).toString();
				assertFalse(source.contains("ExecutorService"), relativePath);
				assertFalse(source.contains("Executors."), relativePath);
			}
		}
	}

	@Test
	void singleSceneSketchesUseDirectSceneRegistration() throws IOException {
		for (String relativePath : List.of(
				"EmptyProject/EmptyProject.pde",
				"SphereParticle/SphereParticle.pde",
				"CompatibilityLock/CompatibilityLock.pde",
				"FulldomePBR/FulldomePBR.pde")) {
			String source = read(relativePath);
			assertTrue(source.contains("ziviDome.setScene("), relativePath);
			assertFalse(source.contains("new SceneManager()"), relativePath);
		}
	}

	@Test
	void particleSimulationMutatesStateOnlyDuringUpdateAndInput() throws IOException {
		String source = read("SphereParticle/Scene1.pde");
		int renderStart = source.indexOf("public void sceneRender(PGraphicsOpenGL pg)");
		int inputStart = source.indexOf("public void keyEvent", renderStart);

		assertTrue(renderStart >= 0 && inputStart > renderStart);
		String renderBody = source.substring(renderStart, inputStart);
		assertTrue(source.contains("particle.update(deltaSeconds, now)"));
		assertFalse(renderBody.contains("particles.add("));
		assertFalse(renderBody.contains("particles.remove("));
		assertFalse(renderBody.contains("particle.update("));
	}

	@Test
	void compatibilityLockUsesNinetyDegreeOrientationSteps() throws IOException {
		String source = read("CompatibilityLock/ReferenceScene.pde");
		assertTrue(source.contains("setPitch(dome.getPitch() + HALF_PI)"));
		assertTrue(source.contains("setYaw(dome.getYaw() + HALF_PI)"));
		assertTrue(source.contains("setRoll(dome.getRoll() + HALF_PI)"));
		assertFalse(source.contains("HALF_PI / 2"));
	}

	private static String read(String relativePath) throws IOException {
		return Files.readString(EXAMPLES.resolve(relativePath));
	}
}
