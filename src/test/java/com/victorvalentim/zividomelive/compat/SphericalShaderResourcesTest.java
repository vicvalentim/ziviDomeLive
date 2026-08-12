package com.victorvalentim.zividomelive.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SphericalShaderResourcesTest {

	@Test
	void samplerCubeSphericalShadersAreStagedForNativeCubemapPipeline() throws IOException {
		Path shaderRoot = projectRoot().resolve("shaders/samplercube");

		assertAll("samplerCube shader resources",
				() -> assertTrue(Files.isRegularFile(shaderRoot.resolve("README.md"))),
				() -> assertShader(shaderRoot, "cubemap.vert", "out vec3 reflectDir"),
				() -> assertShader(shaderRoot, "cubemap.frag", "uniform samplerCube cubemap"),
				() -> assertShader(shaderRoot, "equirectangular.vert", "uniform mat4 transform"),
				() -> assertShader(shaderRoot, "equirectangular.frag", "uniform samplerCube cubemap"),
				() -> assertShader(shaderRoot, "fisheye.vert", "uniform mat4 transform"),
				() -> assertShader(shaderRoot, "fisheye.frag", "uniform samplerCube cubemap"),
				() -> assertShader(shaderRoot, "skybox.vert", "uniform mat4 transform"),
				() -> assertShader(shaderRoot, "skybox.frag", "uniform samplerCube cubemap"));
	}

	@Test
	void legacyProcessingShaderFallbackStillUsesCurrentGraphicsTexturePipeline() throws IOException {
		Path activeShaderRoot = projectRoot().resolve("shaders");

		String equirectangular = Files.readString(activeShaderRoot.resolve("equirectangular.frag"));
		String domemaster = Files.readString(activeShaderRoot.resolve("domemaster.frag"));

		assertAll("legacy Processing shader fallback",
				() -> assertTrue(equirectangular.contains("uniform sampler2D posX, negX, posY, negY, posZ, negZ")),
				() -> assertTrue(equirectangular.contains("bilinearInterpolate(posX")),
				() -> assertTrue(domemaster.contains("uniform sampler2D equirectangularMap")));
	}

	@Test
	void packagedJarTaskIncludesNestedShaderResources() throws IOException {
		String buildScript = Files.readString(projectRoot().resolve("build.gradle.kts"));

		assertAll("shader packaging",
				() -> assertTrue(buildScript.contains("from(\"shaders\")")),
				() -> assertTrue(buildScript.contains("into(\"data/shaders\")")));
	}

	private static void assertShader(Path shaderRoot, String fileName, String requiredSnippet) throws IOException {
		Path shader = shaderRoot.resolve(fileName);
		String source = Files.readString(shader);

		assertAll(fileName,
				() -> assertTrue(Files.isRegularFile(shader)),
				() -> assertTrue(source.startsWith("#version 410 core")),
				() -> assertTrue(source.contains(requiredSnippet)));

		if (fileName.endsWith(".frag")) {
			assertTrue(source.contains("#define PROCESSING_COLOR_SHADER"), fileName);
		}
	}

	private static Path projectRoot() {
		return Path.of(System.getProperty("user.dir"));
	}
}
