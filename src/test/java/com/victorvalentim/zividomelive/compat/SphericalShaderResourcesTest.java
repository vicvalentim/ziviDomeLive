package com.victorvalentim.zividomelive.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SphericalShaderResourcesTest {

	private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));

	@Test
	void requiredShaderResourcesExistWithTheExpectedSamplerRoles() throws IOException {
		Path samplerCube = PROJECT_ROOT.resolve("shaders/samplercube");
		Path environment = PROJECT_ROOT.resolve("shaders/environment");

		assertAll("required shader resources",
				() -> assertShader(samplerCube.resolve("cubemap.vert"), false, null),
				() -> assertShader(samplerCube.resolve("cubemap.frag"), true, "samplerCube"),
				() -> assertShader(samplerCube.resolve("equirectangular.vert"), false, null),
				() -> assertShader(samplerCube.resolve("equirectangular.frag"), true, "samplerCube"),
				() -> assertShader(samplerCube.resolve("fisheye.vert"), false, null),
				() -> assertShader(samplerCube.resolve("fisheye.frag"), true, "samplerCube"),
				() -> assertShader(samplerCube.resolve("skybox.vert"), false, null),
				() -> assertShader(samplerCube.resolve("skybox.frag"), true, "samplerCube"),
				() -> assertShader(environment.resolve(
						"standard_equirectangular_background.vert"), false, null),
				() -> assertShader(environment.resolve(
						"standard_equirectangular_background.frag"), true, "sampler2D"),
				() -> assertNativeShader(environment.resolve(
						"spherical_equirectangular_background.vert"), null),
				() -> assertNativeShader(environment.resolve(
						"spherical_equirectangular_background.frag"), "sampler2D"));
	}

	@Test
	void buildPackagesTheShaderTreeAtTheRuntimeDataPath() throws IOException {
		String buildScript = Files.readString(PROJECT_ROOT.resolve("build.gradle.kts"));

		assertAll("shader packaging",
				() -> assertTrue(buildScript.contains("from(\"shaders\")")),
				() -> assertTrue(buildScript.contains("into(\"data/shaders\")")));
	}

	private static void assertShader(
			Path shader,
			boolean processingFragment,
			String samplerType) throws IOException {
		String source = Files.readString(shader);

		assertAll(shader.getFileName().toString(),
				() -> assertTrue(Files.isRegularFile(shader)),
				() -> assertTrue(source.startsWith("#version 410 core")),
				() -> assertFalse(source.isBlank()),
				() -> assertTrue(!processingFragment
						|| source.contains("#define PROCESSING_COLOR_SHADER")),
				() -> assertTrue(samplerType == null || source.contains(samplerType)));
	}

	private static void assertNativeShader(Path shader, String samplerType) throws IOException {
		String source = Files.readString(shader);

		assertAll(shader.getFileName().toString(),
				() -> assertTrue(Files.isRegularFile(shader)),
				() -> assertTrue(source.startsWith("#version 410 core")),
				() -> assertFalse(source.contains("#define PROCESSING_")),
				() -> assertTrue(samplerType == null || source.contains(samplerType)));
	}
}
