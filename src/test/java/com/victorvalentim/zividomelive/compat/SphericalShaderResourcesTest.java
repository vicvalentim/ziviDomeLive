package com.victorvalentim.zividomelive.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
	void environmentBackgroundShadersArePackagedForLdrEquirectangularMaps() throws IOException {
		Path shaderRoot = projectRoot().resolve("shaders/environment");
		String renderer = Files.readString(projectRoot().resolve(
				"src/main/java/com/victorvalentim/zividomelive/render/EnvironmentBackgroundRenderer.java"));

		assertAll("environment background shader resources",
				() -> assertShader(shaderRoot, "equirectangular_background.vert", "gl_VertexID"),
				() -> assertShader(shaderRoot, "equirectangular_background.vert", "FULLSCREEN_TRIANGLE"),
				() -> assertNativeShader(shaderRoot, "equirectangular_background.frag", "uniform sampler2D environmentMap"),
				() -> assertNativeShader(shaderRoot, "equirectangular_background.frag", "faceResolution"),
				() -> assertNativeShader(shaderRoot, "equirectangular_background.frag", "directionForCanonicalFace"),
				() -> assertNativeShader(shaderRoot, "equirectangular_background.frag", "equirectangularUv"),
				() -> assertNativeShader(shaderRoot, "equirectangular_background.frag", "environmentRotation"),
				() -> assertNativeShader(shaderRoot, "equirectangular_background.frag", "yawOffset"),
				() -> assertNativeShader(shaderRoot, "equirectangular_background.frag", "intensity"),
				() -> assertTrue(renderer.contains("pgl.drawArrays(PGL.TRIANGLES, 0, 3)")),
				() -> assertTrue(renderer.contains("pgl.depthFunc(PGL.LEQUAL)")),
				() -> assertTrue(renderer.contains("pgl.depthMask(false)")),
				() -> assertFalse(renderer.contains("target.rect(")));
	}

	@Test
	void sixTextureProcessingShaderResourcesAreRemoved() {
		Path activeShaderRoot = projectRoot().resolve("shaders");

		assertAll("six-texture Processing shader resources removed",
				() -> assertFalse(Files.exists(activeShaderRoot.resolve("equirectangular.frag"))),
				() -> assertFalse(Files.exists(activeShaderRoot.resolve("equirectangular.vert"))),
				() -> assertFalse(Files.exists(activeShaderRoot.resolve("domemaster.frag"))),
				() -> assertFalse(Files.exists(activeShaderRoot.resolve("domemaster.vert"))));
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
				() -> assertTrue(equirectangular.contains("vec4 sampleCubemapEAC(vec3 dir)")),
				() -> assertTrue(equirectangular.contains("FragColor = sampleCubemapEAC(dir)")));
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
				() -> assertTrue(fisheye.contains("vec4 sampleCubemapEAC(vec3 dir)")),
				() -> assertTrue(fisheye.contains("FragColor = sampleCubemapEAC(dir)")));
	}

	@Test
	void samplerCubeSkyboxShaderUsesNativeSampleCubeLayout() throws IOException {
		Path shaderRoot = projectRoot().resolve("shaders/samplercube");

		String skybox = Files.readString(shaderRoot.resolve("skybox.frag"));

		assertAll("samplerCube skybox layout",
				() -> assertTrue(skybox.contains("uniform int layoutFaces[6]")),
				() -> assertTrue(skybox.contains("uniform int faceRotations[6]")),
				() -> assertTrue(skybox.contains("uniform int faceInversions[6]")),
				() -> assertTrue(skybox.contains("faceIndex = layoutFaces[SLOT_TOP]")),
				() -> assertTrue(skybox.contains("faceIndex = layoutFaces[SLOT_LEFT]")),
				() -> assertTrue(skybox.contains("faceIndex = layoutFaces[SLOT_CENTER]")),
				() -> assertTrue(skybox.contains("faceIndex = layoutFaces[SLOT_RIGHT]")),
				() -> assertTrue(skybox.contains("faceIndex = layoutFaces[SLOT_FAR_RIGHT]")),
				() -> assertTrue(skybox.contains("faceIndex = layoutFaces[SLOT_BOTTOM]")),
				() -> assertTrue(skybox.contains("vec3 directionForCanonicalFace(int faceIndex, vec2 faceUV)")),
				() -> assertTrue(skybox.contains("dir = applyLegacyFaceTransform(dir, faceRotations[faceIndex], faceInversions[faceIndex])")),
				() -> assertTrue(skybox.contains("dir.z = -dir.z")),
				() -> assertTrue(skybox.contains("FragColor = sampleCubemapEAC(dir)")));
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

	private static void assertNativeShader(
			Path shaderRoot,
			String fileName,
			String requiredSnippet) throws IOException {
		Path shader = shaderRoot.resolve(fileName);
		String source = Files.readString(shader);

		assertAll(fileName,
				() -> assertTrue(Files.isRegularFile(shader)),
				() -> assertTrue(source.startsWith("#version 410 core")),
				() -> assertTrue(source.contains(requiredSnippet)),
				() -> assertFalse(source.contains("#define PROCESSING_")));
	}

	private static Path projectRoot() {
		return Path.of(System.getProperty("user.dir"));
	}
}
