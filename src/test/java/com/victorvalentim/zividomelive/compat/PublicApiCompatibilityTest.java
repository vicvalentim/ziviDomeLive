package com.victorvalentim.zividomelive.compat;

import com.victorvalentim.zividomelive.FrameViews;
import com.victorvalentim.zividomelive.Scene;
import com.victorvalentim.zividomelive.SceneManager;
import com.victorvalentim.zividomelive.RenderMode;
import com.victorvalentim.zividomelive.ViewType;
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
import com.victorvalentim.zividomelive.ziviDomeLive;
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
		assertEquals("com.victorvalentim.zividomelive.ziviDomeLive", ziviDomeLive.class.getName());
		assertTrue(Modifier.isPublic(ziviDomeLive.class.getModifiers()));
		assertNotNull(ziviDomeLive.class.getConstructor(PApplet.class));
	}

	@Test
	void viewTypeIsTopLevelWithFinalNamesAndOrder() {
		assertEquals("com.victorvalentim.zividomelive.ViewType", ViewType.class.getName());
		assertTrue(Modifier.isPublic(ViewType.class.getModifiers()));
		assertArrayEquals(new ViewType[]{
				ViewType.STANDARD,
				ViewType.DOMEMASTER,
				ViewType.EQUIRECTANGULAR,
				ViewType.SKYBOX
		}, ViewType.values());
		assertFalse(Arrays.stream(ziviDomeLive.class.getDeclaredClasses())
				.anyMatch(type -> type.getSimpleName().equals("ViewType")));
	}

	@Test
	void viewRoutingMethodsUseTopLevelViewType() throws Exception {
		assertEquals(ViewType.class, ziviDomeLive.class.getMethod("getCurrentView").getReturnType());
		assertNotNull(ziviDomeLive.class.getMethod("setCurrentView", ViewType.class));
		assertEquals(ViewType.class, OutputManager.class
				.getMethod("getViewForOutput", OutputManager.OutputType.class)
				.getReturnType());
		assertNotNull(OutputManager.class.getMethod(
				"setViewForOutput", OutputManager.OutputType.class, ViewType.class));
		assertNotNull(OutputManager.class.getMethod("setNdiView", ViewType.class));
		assertNotNull(OutputManager.class.getMethod("setSpoutView", ViewType.class));
		assertNotNull(OutputManager.class.getMethod("setSyphonView", ViewType.class));
		assertNotNull(OutputManager.class.getMethod("setLocalTextureView", ViewType.class));
		assertNotNull(OutputManager.class.getMethod("requiresView", ViewType.class));
	}

	@Test
	void frameViewsExposeOnlyCompletedTargetsByLogicalView() throws Exception {
		assertTrue(Modifier.isPublic(FrameViews.class.getModifiers()));
		assertTrue(FrameViews.class.isInterface());
		assertEquals(PGraphicsOpenGL.class,
				FrameViews.class.getMethod("getFrame", ViewType.class).getReturnType());
		assertNotNull(OutputManager.class.getMethod("sendOutput", FrameViews.class));
		assertNotNull(OutputManager.class.getMethod("sendOutput"));
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
		assertArrayEquals(new ziviDomeLive.InitState[]{
				ziviDomeLive.InitState.NOT_INITIALIZED,
				ziviDomeLive.InitState.SETUP_COMPLETE,
				ziviDomeLive.InitState.MANAGERS_READY,
				ziviDomeLive.InitState.READY
		}, ziviDomeLive.InitState.values());
		assertArrayEquals(new ziviDomeLive.StandardOutputAspectMode[]{
				ziviDomeLive.StandardOutputAspectMode.AUTO,
				ziviDomeLive.StandardOutputAspectMode.ASPECT_16_9,
				ziviDomeLive.StandardOutputAspectMode.ASPECT_16_10,
				ziviDomeLive.StandardOutputAspectMode.ASPECT_4_3,
				ziviDomeLive.StandardOutputAspectMode.ASPECT_1_1
		}, ziviDomeLive.StandardOutputAspectMode.values());
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
				ViewType.class,
				FrameViews.class,
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
			Method method = ziviDomeLive.class.getMethod(name);
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
		Method method = ziviDomeLive.class.getMethod(name, parameterTypes);
		assertTrue(Modifier.isPublic(method.getModifiers()), name + " must remain public");
	}

	private static void assertControlEventMethod() {
		boolean found = Arrays.stream(ziviDomeLive.class.getMethods())
				.anyMatch(method -> method.getName().equals("controlEvent")
						&& Modifier.isPublic(method.getModifiers())
						&& method.getParameterCount() == 1
						&& method.getParameterTypes()[0].getName().equals("controlP5.ControlEvent"));
		assertTrue(found, "controlEvent(ControlEvent) must remain public");
	}
}
