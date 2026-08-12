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
	void samplerCubeEquirectangularShaderUsesNativeSampleCubeOrientation() throws IOException {
		Path shaderRoot = projectRoot().resolve("shaders/samplercube");

		String equirectangular = Files.readString(shaderRoot.resolve("equirectangular.frag"));

		assertAll("samplerCube equirectangular orientation",
				() -> assertTrue(equirectangular.contains("float theta = -(uv.x * 2.0 * PI - PI)")),
				() -> assertTrue(equirectangular.contains("float phi = uv.y * PI - PI / 2.0")),
				() -> assertTrue(equirectangular.contains("-cosPhi * sinTheta")),
				() -> assertTrue(equirectangular.contains("sinPhi")),
				() -> assertTrue(equirectangular.contains("-cosPhi * cosTheta")),
				() -> assertTrue(equirectangular.contains("texture(cubemap, applyEAC(dir))")));
	}

	@Test
	void samplerCubeFisheyeShaderUsesNativeSampleCubeOrientation() throws IOException {
		Path shaderRoot = projectRoot().resolve("shaders/samplercube");

		String fisheye = Files.readString(shaderRoot.resolve("fisheye.frag"));

		assertAll("samplerCube fisheye orientation",
				() -> assertTrue(fisheye.contains("uv.y *= resolution.y / resolution.x")),
				() -> assertTrue(fisheye.contains("sin(theta) * cos(phi)")),
				() -> assertTrue(fisheye.contains("sin(theta) * sin(phi)")),
				() -> assertTrue(fisheye.contains("cos(theta)")),
				() -> assertTrue(fisheye.contains("dir.z = -dir.z")),
				() -> assertTrue(fisheye.contains("texture(cubemap, applyEAC(normalize(dir)))")));
	}

	@Test
	void samplerCubeSkyboxShaderUsesNativeSampleCubeLayout() throws IOException {
		Path shaderRoot = projectRoot().resolve("shaders/samplercube");

		String skybox = Files.readString(shaderRoot.resolve("skybox.frag"));

		assertAll("samplerCube skybox layout",
				() -> assertTrue(skybox.contains("uniform int faceRotations[6]")),
				() -> assertTrue(skybox.contains("uniform bool faceInversions[6]")),
				() -> assertTrue(skybox.contains("faceIndex = 1")),
				() -> assertTrue(skybox.contains("faceIndex = 2")),
				() -> assertTrue(skybox.contains("faceIndex = 3")),
				() -> assertTrue(skybox.contains("faceIndex = 4")),
				() -> assertTrue(skybox.contains("faceIndex = 0")),
				() -> assertTrue(skybox.contains("faceIndex = 5")),
				() -> assertTrue(skybox.contains("dir = applyTransformations(dir, faceRotations[faceIndex], faceInversions[faceIndex])")),
				() -> assertTrue(skybox.contains("dir.z = -dir.z")));
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
