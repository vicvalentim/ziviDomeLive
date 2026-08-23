package com.victorvalentim.zividomelive.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects the physical source taxonomy without widening Java visibility. */
class InternalSourceLayoutTest {

	private static final Path API_ROOT = Path.of(
			"src/main/java/com/victorvalentim/zividomelive");
	private static final Path INTERNAL_ROOT = API_ROOT.resolve("_internal");
	private static final Pattern PUBLIC_TOP_LEVEL_TYPE = Pattern.compile(
			"(?m)^public\\s+(?:abstract\\s+|final\\s+|sealed\\s+|non-sealed\\s+)?"
					+ "(?:class|interface|enum|record)\\s+");

	@Test
	void apiRootContainsOnlyDeliberateArtistFacingSources() throws IOException {
		try (var files = Files.list(API_ROOT)) {
			assertEquals(Set.of(
					"FrameClock.java", "LogMode.java", "RenderMode.java", "Scene.java",
					"SceneActionMap.java", "SceneAssets.java", "SceneCameraService.java",
					"SceneEnvironmentService.java", "SceneInputPort.java", "SceneManager.java",
					"SceneOutputPort.java", "ScenePorts.java", "SceneServices.java",
					"SceneTaskGroup.java", "SimulationTimeline.java", "ViewType.java",
					"ziviDomeLive.java"),
					files.filter(path -> path.getFileName().toString().endsWith(".java"))
							.map(path -> path.getFileName().toString())
							.collect(Collectors.toSet()));
		}
	}

	@Test
	void internalSourcesUseApprovedCategoriesAndRemainPackagePrivate() throws IOException {
		List<Path> internalSources;
		try (var files = Files.walk(INTERNAL_ROOT)) {
			internalSources = files
					.filter(path -> path.getFileName().toString().endsWith(".java"))
					.toList();
		}
		assertFalse(internalSources.isEmpty());
		assertEquals(Set.of(
				"output", "performance", "render/camera", "render/core", "render/gl",
				"render/modes", "runtime", "scene", "support", "ui"),
				internalSources.stream()
						.map(path -> INTERNAL_ROOT.relativize(path).getParent().toString()
								.replace('\\', '/'))
						.collect(Collectors.toSet()));

		for (Path source : internalSources) {
			String java = Files.readString(source);
			assertTrue(java.startsWith("package com.victorvalentim.zividomelive;"), source.toString());
			assertFalse(PUBLIC_TOP_LEVEL_TYPE.matcher(java).find(), source.toString());
		}
	}
}
