package com.victorvalentim.zividomelive.compat;

import com.victorvalentim.zividomelive.FrameClock;
import com.victorvalentim.zividomelive.LogMode;
import com.victorvalentim.zividomelive.RenderMode;
import com.victorvalentim.zividomelive.Scene;
import com.victorvalentim.zividomelive.SceneActionMap;
import com.victorvalentim.zividomelive.SceneAssets;
import com.victorvalentim.zividomelive.SceneCameraService;
import com.victorvalentim.zividomelive.SceneEnvironmentService;
import com.victorvalentim.zividomelive.SceneInputPort;
import com.victorvalentim.zividomelive.SceneManager;
import com.victorvalentim.zividomelive.SceneOutputPort;
import com.victorvalentim.zividomelive.ScenePorts;
import com.victorvalentim.zividomelive.SceneServices;
import com.victorvalentim.zividomelive.SceneTaskGroup;
import com.victorvalentim.zividomelive.SimulationTimeline;
import com.victorvalentim.zividomelive.ViewType;
import com.victorvalentim.zividomelive.manager.OutputManager;
import com.victorvalentim.zividomelive.performance.GpuTimerArchitecture;
import com.victorvalentim.zividomelive.performance.GpuTimerBackend;
import com.victorvalentim.zividomelive.performance.GpuTimerPolicy;
import com.victorvalentim.zividomelive.performance.GraphicsCapabilities;
import com.victorvalentim.zividomelive.performance.PerformanceMetric;
import com.victorvalentim.zividomelive.performance.PerformanceMode;
import com.victorvalentim.zividomelive.performance.PerformanceSnapshot;
import com.victorvalentim.zividomelive.render.Quaternion;
import com.victorvalentim.zividomelive.render.SphericalOrientation;
import com.victorvalentim.zividomelive.render.camera.OrbitCamera;
import com.victorvalentim.zividomelive.ziviDomeLive;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.opengl.PGraphicsOpenGL;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects the new 2.0 public baseline rather than the removed 1.x surface. */
class PublicApiCompatibilityTest {

	private static final Class<?>[] STABLE_TYPES = {
			ziviDomeLive.class,
			Scene.class,
			SceneManager.class,
			RenderMode.class,
			ViewType.class,
			LogMode.class
	};

	private static final Class<?>[] ADVANCED_STABLE_TYPES = {
			SceneServices.class,
			FrameClock.class,
			SimulationTimeline.class,
			SceneTaskGroup.class,
			SceneAssets.class,
			SceneActionMap.class,
			SceneCameraService.class,
			SceneEnvironmentService.class,
			ScenePorts.class,
			SceneInputPort.class,
			SceneOutputPort.class,
			OutputManager.class,
			Quaternion.class,
			SphericalOrientation.class,
			OrbitCamera.class
	};

	private static final Class<?>[] EXPERIMENTAL_TYPES = {
			PerformanceMode.class,
			PerformanceMetric.class,
			PerformanceSnapshot.class,
			GraphicsCapabilities.class,
			GpuTimerPolicy.class,
			GpuTimerBackend.class,
			GpuTimerArchitecture.class
	};

	private static final String[] REMOVED_ENGINE_TYPES = {
			"com.victorvalentim.zividomelive.support.ThreadManager",
			"com.victorvalentim.zividomelive.support.LibraryMetadata",
			"com.victorvalentim.zividomelive.support.LogManager",
			"com.victorvalentim.zividomelive.support.SplashScreen",
			"com.victorvalentim.zividomelive.scene.DefaultScene",
			"com.victorvalentim.zividomelive.manager.ControlManager",
			"com.victorvalentim.zividomelive.manager.ControlP5KeyEventBridge",
			"com.victorvalentim.zividomelive.manager.ControlPanelLayout",
			"com.victorvalentim.zividomelive.manager.ControlScope",
			"com.victorvalentim.zividomelive.render.CubemapRenderer",
			"com.victorvalentim.zividomelive.render.EnvironmentBackgroundRenderer",
			"com.victorvalentim.zividomelive.render.EnvironmentState",
			"com.victorvalentim.zividomelive.render.SphericalEnvironmentNativePass",
			"com.victorvalentim.zividomelive.render.camera.CameraManager",
			"com.victorvalentim.zividomelive.render.camera.CameraOrientation",
			"com.victorvalentim.zividomelive.render.camera.CubemapFace",
			"com.victorvalentim.zividomelive.render.camera.MouseControlledCamera",
			"com.victorvalentim.zividomelive.render.gl.CubemapTarget",
			"com.victorvalentim.zividomelive.render.gl.ProcessingGlAdapter",
			"com.victorvalentim.zividomelive.render.gl.ProcessingGlCapabilities",
			"com.victorvalentim.zividomelive.render.gl.ProcessingGpuMeasurementSession",
			"com.victorvalentim.zividomelive.render.modes.CubemapViewRenderer",
			"com.victorvalentim.zividomelive.render.modes.EquirectangularRenderer",
			"com.victorvalentim.zividomelive.render.modes.FisheyeDomemaster",
			"com.victorvalentim.zividomelive.render.modes.StandardRenderer",
			"com.victorvalentim.zividomelive.internal.performance.PerformanceMonitor",
			"com.victorvalentim.zividomelive.internal.performance.GpuPerformanceTimer",
			"com.victorvalentim.zividomelive.internal.performance.GpuTimerQualification"
	};

	private static final String[] ROOT_INTERNAL_TYPES = {
			"com.victorvalentim.zividomelive.RenderThreadQueue",
			"com.victorvalentim.zividomelive.SharedTaskExecutor",
			"com.victorvalentim.zividomelive.OutputManagerImpl",
			"com.victorvalentim.zividomelive.NdiOutputBackend",
			"com.victorvalentim.zividomelive.CubemapRenderer",
			"com.victorvalentim.zividomelive.CubemapTarget",
			"com.victorvalentim.zividomelive.ProcessingGlAdapter",
			"com.victorvalentim.zividomelive.PerformanceMonitor",
			"com.victorvalentim.zividomelive.ControlManager"
	};

	@Test
	void finalApiTypesRemainJavaPublic() throws Exception {
		for (Class<?> type : concat(STABLE_TYPES, ADVANCED_STABLE_TYPES, EXPERIMENTAL_TYPES)) {
			assertTrue(Modifier.isPublic(type.getModifiers()), type.getName());
		}
		assertNotNull(ziviDomeLive.class.getConstructor(PApplet.class));
		assertTrue(PerformanceSnapshot.class.isInterface());
		assertEquals(0, PerformanceSnapshot.class.getConstructors().length);
	}

	@Test
	void stableEnumNamesAndOrderAreFrozen() {
		assertArrayEquals(new ViewType[]{
				ViewType.STANDARD, ViewType.DOMEMASTER,
				ViewType.EQUIRECTANGULAR, ViewType.SKYBOX
		}, ViewType.values());
		assertArrayEquals(new RenderMode[]{
				RenderMode.FULL, RenderMode.STANDARD, RenderMode.DOMEMASTER,
				RenderMode.EQUIRECTANGULAR, RenderMode.SKYBOX
		}, RenderMode.values());
		assertArrayEquals(new LogMode[]{LogMode.DEBUG, LogMode.RELEASE}, LogMode.values());
	}

	@Test
	void sceneContractKeepsOneRequiredRenderMethod() throws Exception {
		Method render = Scene.class.getMethod("sceneRender", PGraphicsOpenGL.class);
		assertTrue(Modifier.isAbstract(render.getModifiers()));
		assertTrue(Scene.class.getMethod("configure", SceneServices.class).isDefault());
		assertTrue(Scene.class.getMethod("setupScene").isDefault());
		assertTrue(Scene.class.getMethod("update").isDefault());
		assertTrue(Scene.class.getMethod("dispose").isDefault());
		assertFalse(publicMethodNames(Scene.class).contains("controlEvent"));
	}

	@Test
	void activationServicesExposeConsumersButNotOwnershipControls() {
		assertEquals(Set.of(
				"actions", "applet", "assets", "camera", "environment", "frameClock",
				"ports", "requestReload", "tasks", "timeline"
		), declaredPublicMethodNames(SceneServices.class));
		assertEquals(0, SceneServices.class.getConstructors().length);
		assertFalse(publicMethodNames(SceneServices.class).contains("close"));
	}

	@Test
	void taskApiIsCallbackBasedAndNeverReturnsFuture() {
		assertEquals(Set.of("getInFlightCount", "getMaxInFlight", "isBusy", "submitIfIdle"),
				declaredPublicMethodNames(SceneTaskGroup.class));
		assertTrue(Arrays.stream(SceneTaskGroup.class.getDeclaredMethods())
				.filter(method -> Modifier.isPublic(method.getModifiers()))
				.noneMatch(method -> java.util.concurrent.Future.class
						.isAssignableFrom(method.getReturnType())));
		assertFalse(publicMethodNames(SceneTaskGroup.class).contains("submit"));
		assertFalse(publicMethodNames(SceneTaskGroup.class).contains("trySubmit"));
	}

	@Test
	void portsExposeBackpressureTelemetryWithoutRuntimeDrainControls() {
		assertEquals(Set.of("connectInput", "connectOutput", "getDroppedInputCount", "getPendingInputCount"),
				declaredPublicMethodNames(ScenePorts.class));
		assertFalse(publicMethodNames(ScenePorts.class).contains("drain"));
		assertFalse(publicMethodNames(ScenePorts.class).contains("close"));
	}

	@Test
	void outputManagerIsTypedAndDoesNotExposeProducerOperations() {
		assertTrue(OutputManager.class.isInterface());
		assertNotNull(method(OutputManager.class, "setOutputEnabled",
				OutputManager.OutputType.class, boolean.class));
		assertNotNull(method(OutputManager.class, "isOutputEnabled", OutputManager.OutputType.class));
		assertNotNull(method(OutputManager.class, "toggleOutput", OutputManager.OutputType.class));
		for (String internal : Set.of(
				"sendOutput", "initializeLocalTextureOutput", "notifyResolutionChanged",
				"shutdownOutputs", "stopOutput", "requiresView", "refreshCachedGraphics", "setView")) {
			assertFalse(publicMethodNames(OutputManager.class).contains(internal), internal);
		}
		assertTrue(Arrays.stream(OutputManager.class.getMethods())
				.noneMatch(candidate -> Arrays.asList(candidate.getParameterTypes()).contains(String.class)));
	}

	@Test
	void facadeDoesNotExposeConcreteRenderGraphOrCompatibilityCommands() {
		assertFalse(processing.core.PConstants.class.isAssignableFrom(ziviDomeLive.class));
		assertFalse(processing.core.PConstants.class.isAssignableFrom(OrbitCamera.class));
		Set<String> names = publicMethodNames(ziviDomeLive.class);
		for (String removed : Set.of(
				"getFisheyeDomemaster", "setFisheyeDomemaster", "getEquirectangularRenderer",
				"getCubemapViewRenderer", "getStandardRenderer", "getWidth", "getHeight",
				"getInitState", "renderFisheyeDomemaster", "renderEquirectangular",
				"renderCubemap", "renderStandard")) {
			assertFalse(names.contains(removed), removed);
		}
	}

	@Test
	void removedEnginePackagesCannotReturnAsPublicApi() {
		for (String className : REMOVED_ENGINE_TYPES) {
			assertThrows(ClassNotFoundException.class, () -> Class.forName(className), className);
		}
	}

	@Test
	void rootEngineImplementationsRemainPackagePrivate() throws Exception {
		for (String className : ROOT_INTERNAL_TYPES) {
			Class<?> type = Class.forName(className);
			assertFalse(Modifier.isPublic(type.getModifiers()), className);
		}
	}

	@Test
	void activationServicesCannotBeConstructedOrClosedBySketches() {
		for (Class<?> type : new Class<?>[]{
				SceneServices.class, FrameClock.class, SceneTaskGroup.class, SceneAssets.class,
				SceneActionMap.class, SceneCameraService.class, SceneEnvironmentService.class,
				ScenePorts.class
		}) {
			assertEquals(0, Arrays.stream(type.getDeclaredConstructors())
					.filter(constructor -> Modifier.isPublic(constructor.getModifiers())).count(), type.getName());
			assertFalse(publicMethodNames(type).contains("close"), type.getName());
		}
	}

	private static Method method(Class<?> owner, String name, Class<?>... parameters) {
		try {
			return owner.getMethod(name, parameters);
		} catch (NoSuchMethodException exception) {
			throw new AssertionError(exception);
		}
	}

	private static Set<String> publicMethodNames(Class<?> type) {
		return Arrays.stream(type.getMethods()).map(Method::getName).collect(Collectors.toSet());
	}

	private static Set<String> declaredPublicMethodNames(Class<?> type) {
		return Arrays.stream(type.getDeclaredMethods())
				.filter(method -> Modifier.isPublic(method.getModifiers()))
				.map(Method::getName)
				.collect(Collectors.toSet());
	}

	private static Class<?>[] concat(Class<?>[]... groups) {
		return Arrays.stream(groups).flatMap(Arrays::stream).toArray(Class<?>[]::new);
	}
}
