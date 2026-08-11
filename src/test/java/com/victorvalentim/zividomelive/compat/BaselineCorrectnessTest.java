package com.victorvalentim.zividomelive.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaselineCorrectnessTest {

	private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));

	@Test
	void examplesDoNotInvokeRegisteredDrawHookManually() throws IOException {
		try (Stream<Path> files = Files.walk(PROJECT_ROOT.resolve("examples"))) {
			for (Path file : files.filter(path -> path.toString().endsWith(".pde")).toList()) {
				String source = Files.readString(file);
				assertFalse(source.matches("(?s).*\\bziviDome\\.draw\\(\\);.*"),
						() -> "Manual draw call renders twice: " + PROJECT_ROOT.relativize(file));
			}
		}
	}

	@Test
	void examplesDoNotReselectTheSceneManagerCurrentScene() throws IOException {
		try (Stream<Path> files = Files.walk(PROJECT_ROOT.resolve("examples"))) {
			for (Path file : files.filter(path -> path.toString().endsWith(".pde")).toList()) {
				String source = Files.readString(file);
				assertFalse(source.contains("setScene(sceneManager.getCurrentScene())"),
						() -> "SceneManager is already authoritative: " + PROJECT_ROOT.relativize(file));
			}
		}
	}

	@Test
	void scenesDoNotOwnGraphicsBeginEndLifecycle() throws IOException {
		try (Stream<Path> files = Files.walk(PROJECT_ROOT.resolve("examples"))) {
			for (Path file : files.filter(path -> path.getFileName().toString().startsWith("Scene"))
					.filter(path -> path.toString().endsWith(".pde")).toList()) {
				String source = Files.readString(file);
				assertFalse(source.matches("(?s).*\\bpg\\.(beginDraw|endDraw)\\s*\\(.*"),
						() -> "Scene must not own PGraphics lifecycle: " + PROJECT_ROOT.relativize(file));
			}
		}
	}

	@Test
	void quickstartsUseCurrentSceneContractWithoutManualForwarding() throws IOException {
		for (String relativePath : new String[]{
				"README.md",
				"docs/en/getting-started/quickstart.md",
				"docs/pt/getting-started/quickstart.md"}) {
			String source = Files.readString(PROJECT_ROOT.resolve(relativePath));
			assertFalse(source.contains("ziviDome.draw();"), relativePath);
			assertFalse(source.contains("ziviDome.mouseEvent("), relativePath);
			assertFalse(source.contains("ziviDome.controlEvent("), relativePath);
			assertFalse(source.contains("sceneRender(PGraphics pg)"), relativePath);
			assertTrue(source.contains("sceneRender(PGraphicsOpenGL pg)"), relativePath);
		}
	}

	@Test
	void controlEventFallbackDoesNotToggleOutputPublication() throws IOException {
		String source = Files.readString(PROJECT_ROOT.resolve(
				"src/main/java/com/victorvalentim/zividomelive/manager/ControlManager.java"));
		int methodStart = source.indexOf("public void handleEvent(ControlEvent theEvent)");
		int methodEnd = source.indexOf("public boolean isNumberboxActive()", methodStart);

		assertTrue(methodStart >= 0 && methodEnd > methodStart);
		assertFalse(source.substring(methodStart, methodEnd).contains("toggleOutput("),
				"Output toggles must be owned only by their onChange callbacks");
	}

	@Test
	void controlManagerDisposalUnregistersNumberboxKeyHooks() throws IOException {
		String source = Files.readString(PROJECT_ROOT.resolve(
				"src/main/java/com/victorvalentim/zividomelive/manager/ControlManager.java"));
		int methodStart = source.indexOf("public void dispose()");
		int methodEnd = source.indexOf("public class NumberboxInput", methodStart);

		assertTrue(methodStart >= 0 && methodEnd > methodStart);
		assertTrue(source.substring(methodStart, methodEnd)
				.contains("p.unregisterMethod(\"keyEvent\", input)"),
				"ControlManager must release NumberboxInput Processing hooks");
	}

	@Test
	void splashScreenDisposesOwnedGraphicsLayers() throws IOException {
		String source = Files.readString(PROJECT_ROOT.resolve(
				"src/main/java/com/victorvalentim/zividomelive/support/SplashScreen.java"));

		assertTrue(source.contains("backgroundLayer = disposeLayer(\"background\", backgroundLayer)"));
		assertTrue(source.contains("animationLayer = disposeLayer(\"animation\", animationLayer)"));
	}
}
