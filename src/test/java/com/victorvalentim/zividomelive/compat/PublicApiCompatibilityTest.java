package com.victorvalentim.zividomelive.compat;

import com.victorvalentim.zividomelive.Scene;
import com.victorvalentim.zividomelive.SceneManager;
import com.victorvalentim.zividomelive.RenderMode;
import com.victorvalentim.zividomelive.manager.OutputManager;
import com.victorvalentim.zividomelive.render.CubemapRenderer;
import com.victorvalentim.zividomelive.render.Quaternion;
import com.victorvalentim.zividomelive.render.SphericalOrientation;
import com.victorvalentim.zividomelive.render.camera.CameraManager;
import com.victorvalentim.zividomelive.render.camera.MouseControlledCamera;
import com.victorvalentim.zividomelive.render.camera.OrbitCamera;
import com.victorvalentim.zividomelive.render.modes.CubemapViewRenderer;
import com.victorvalentim.zividomelive.render.modes.EquirectangularRenderer;
import com.victorvalentim.zividomelive.render.modes.FisheyeDomemaster;
import com.victorvalentim.zividomelive.render.modes.StandardRenderer;
import com.victorvalentim.zividomelive.support.LibraryMetadata;
import com.victorvalentim.zividomelive.support.LogManager;
import com.victorvalentim.zividomelive.support.ThreadManager;
import com.victorvalentim.zividomelive.zividomelive;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class PublicApiCompatibilityTest {

	@Test
	void publicFacadeClassNameAndConstructorRemainStable() throws Exception {
		assertEquals("com.victorvalentim.zividomelive.zividomelive", zividomelive.class.getName());
		assertTrue(Modifier.isPublic(zividomelive.class.getModifiers()));
		assertNotNull(zividomelive.class.getConstructor(PApplet.class));
	}

	@Test
	void viewTypeOrderRemainsIndexCompatibleWithControlDropdowns() {
		assertArrayEquals(new zividomelive.ViewType[]{
				zividomelive.ViewType.FISHEYE_DOMEMASTER,
				zividomelive.ViewType.EQUIRECTANGULAR,
				zividomelive.ViewType.CUBEMAP,
				zividomelive.ViewType.STANDARD
		}, zividomelive.ViewType.values());
	}

	@Test
	void publicEnumsRemainSourceCompatible() {
		assertArrayEquals(new RenderMode[]{
				RenderMode.FULL,
				RenderMode.STANDARD,
				RenderMode.DOMEMASTER,
				RenderMode.EQUIRECTANGULAR,
				RenderMode.SKYBOX
		}, RenderMode.values());
		assertArrayEquals(new zividomelive.InitState[]{
				zividomelive.InitState.NOT_INITIALIZED,
				zividomelive.InitState.SETUP_COMPLETE,
				zividomelive.InitState.MANAGERS_READY,
				zividomelive.InitState.READY
		}, zividomelive.InitState.values());
		assertArrayEquals(new zividomelive.StandardOutputAspectMode[]{
				zividomelive.StandardOutputAspectMode.AUTO,
				zividomelive.StandardOutputAspectMode.ASPECT_16_9,
				zividomelive.StandardOutputAspectMode.ASPECT_16_10,
				zividomelive.StandardOutputAspectMode.ASPECT_4_3,
				zividomelive.StandardOutputAspectMode.ASPECT_1_1
		}, zividomelive.StandardOutputAspectMode.values());
		assertArrayEquals(new OutputManager.OutputType[]{
				OutputManager.OutputType.NDI,
				OutputManager.OutputType.SPOUT,
				OutputManager.OutputType.SYPHON
		}, OutputManager.OutputType.values());
		assertArrayEquals(new OutputManager.OutputState[]{
				OutputManager.OutputState.UNAVAILABLE,
				OutputManager.OutputState.AVAILABLE,
				OutputManager.OutputState.INITIALIZED,
				OutputManager.OutputState.ENABLED,
				OutputManager.OutputState.STOPPING
		}, OutputManager.OutputState.values());
		assertArrayEquals(new LogManager.Mode[]{
				LogManager.Mode.DEBUG,
				LogManager.Mode.RELEASE
		}, LogManager.Mode.values());
	}

	@Test
	void primaryPublicTypesRemainAvailable() {
		Class<?>[] publicTypes = {
				RenderMode.class,
				Scene.class,
				SceneManager.class,
				OutputManager.class,
				CubemapRenderer.class,
				Quaternion.class,
				SphericalOrientation.class,
				CameraManager.class,
				MouseControlledCamera.class,
				OrbitCamera.class,
				CubemapViewRenderer.class,
				EquirectangularRenderer.class,
				FisheyeDomemaster.class,
				StandardRenderer.class,
				LibraryMetadata.class,
				LogManager.class,
				ThreadManager.class
		};

		for (Class<?> type : publicTypes) {
			assertTrue(Modifier.isPublic(type.getModifiers()), type.getName() + " must remain public");
		}
	}

	@Test
	void facadeOperationalMethodsRemainAvailable() throws Exception {
		assertMethod("setup");
		assertMethod("draw");
		assertMethod("pre");
		assertMethod("post");
		assertMethod("pause");
		assertMethod("resume");
		assertMethod("stop");
		assertMethod("dispose");
		assertMethod("setScene", Scene.class);
		assertMethod("setCurrentScene", Scene.class);
		assertMethod("setSceneManager", SceneManager.class);
		assertMethod("getRenderMode");
		assertMethod("setRenderMode", RenderMode.class);
		assertMethod("resetOrientation");
		assertMethod("keyEvent", KeyEvent.class);
		assertMethod("mouseEvent", MouseEvent.class);
		assertControlEventMethod();
	}

	@Test
	void facadeRendererCameraAndOutputAccessorsRemainAvailable() throws Exception {
		assertMethod("getOutputManager");
		assertMethod("getFisheyeDomemaster");
		assertMethod("getEquirectangularRenderer");
		assertMethod("getCubemapViewRenderer");
		assertMethod("getStandardRenderer");
		assertMethod("getSceneCamera");
		assertMethod("setSceneCameraInputEnabled", boolean.class);
		assertMethod("isSceneCameraInputEnabled");
	}

	@Test
	void outputLifecycleAndTelemetryMethodsRemainAvailable() throws Exception {
		assertNotNull(OutputManager.class.getMethod(
				"getOutputState", OutputManager.OutputType.class));
		assertNotNull(OutputManager.class.getMethod(
				"getOutputFailureReason", OutputManager.OutputType.class));
		assertNotNull(OutputManager.class.getMethod("getNdiCapturedFrames"));
		assertNotNull(OutputManager.class.getMethod("getNdiSentFrames"));
		assertNotNull(OutputManager.class.getMethod("getNdiDroppedFrames"));
		assertNotNull(OutputManager.class.getMethod("getNdiFailedFrames"));
	}

	@Test
	void deprecatedRenderConvenienceMethodsRemainDeprecatedCompatibilityShims() throws Exception {
		for (String name : Arrays.asList(
				"renderFisheyeDomemaster",
				"renderEquirectangular",
				"renderCubemap",
				"renderStandard")) {
			Method method = zividomelive.class.getMethod(name);
			assertTrue(method.isAnnotationPresent(Deprecated.class), name + " must remain deprecated");
		}
	}

	@Test
	void sceneContractSignatureRemainsPGraphicsOpenGL() throws Exception {
		Method render = Scene.class.getMethod("sceneRender", PGraphicsOpenGL.class);
		assertEquals(void.class, render.getReturnType());
		assertTrue(Modifier.isAbstract(render.getModifiers()));
	}

	private static void assertMethod(String name, Class<?>... parameterTypes) throws Exception {
		Method method = zividomelive.class.getMethod(name, parameterTypes);
		assertTrue(Modifier.isPublic(method.getModifiers()), name + " must remain public");
	}

	private static void assertControlEventMethod() {
		boolean found = Arrays.stream(zividomelive.class.getMethods())
				.anyMatch(method -> method.getName().equals("controlEvent")
						&& Modifier.isPublic(method.getModifiers())
						&& method.getParameterCount() == 1
						&& method.getParameterTypes()[0].getName().equals("controlP5.ControlEvent"));
		assertTrue(found, "controlEvent(ControlEvent) must remain public");
	}
}
