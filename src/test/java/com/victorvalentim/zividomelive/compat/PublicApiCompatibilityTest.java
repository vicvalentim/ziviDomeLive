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
			ziviDomeLive.StandardOutputAspectMode.class,
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
			OutputManager.OutputType.class,
			OutputManager.OutputState.class,
			Quaternion.class,
			SphericalOrientation.class,
			OrbitCamera.class
	};

	private static final Class<?>[] EXPERIMENTAL_TYPES = {
			PerformanceMode.class,
			PerformanceMetric.class,
			PerformanceSnapshot.class,
			PerformanceSnapshot.MetricStatistics.class,
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
	void publicEnumNamesAndOrderAreDeliberate() {
		assertArrayEquals(new ViewType[]{
				ViewType.STANDARD, ViewType.DOMEMASTER,
				ViewType.EQUIRECTANGULAR, ViewType.SKYBOX
		}, ViewType.values());
		assertArrayEquals(new RenderMode[]{
				RenderMode.FULL, RenderMode.STANDARD, RenderMode.DOMEMASTER,
				RenderMode.EQUIRECTANGULAR, RenderMode.SKYBOX
		}, RenderMode.values());
		assertArrayEquals(new LogMode[]{LogMode.DEBUG, LogMode.RELEASE}, LogMode.values());
		assertArrayEquals(new ziviDomeLive.StandardOutputAspectMode[]{
				ziviDomeLive.StandardOutputAspectMode.AUTO,
				ziviDomeLive.StandardOutputAspectMode.ASPECT_16_9,
				ziviDomeLive.StandardOutputAspectMode.ASPECT_16_10,
				ziviDomeLive.StandardOutputAspectMode.ASPECT_4_3,
				ziviDomeLive.StandardOutputAspectMode.ASPECT_1_1
		}, ziviDomeLive.StandardOutputAspectMode.values());
		assertArrayEquals(new OutputManager.OutputType[]{
				OutputManager.OutputType.NDI, OutputManager.OutputType.SPOUT,
				OutputManager.OutputType.SYPHON
		}, OutputManager.OutputType.values());
		assertArrayEquals(new OutputManager.OutputState[]{
				OutputManager.OutputState.UNAVAILABLE, OutputManager.OutputState.AVAILABLE,
				OutputManager.OutputState.INITIALIZED, OutputManager.OutputState.ENABLED,
				OutputManager.OutputState.STOPPING
		}, OutputManager.OutputState.values());
		assertArrayEquals(new PerformanceMode[]{
				PerformanceMode.OFF, PerformanceMode.CPU, PerformanceMode.CPU_GPU
		}, PerformanceMode.values());
		assertArrayEquals(new PerformanceMetric[]{
				PerformanceMetric.FRAME_TOTAL, PerformanceMetric.SCENE_UPDATE,
				PerformanceMetric.SCENE_RENDER, PerformanceMetric.RENDER_PIPELINE,
				PerformanceMetric.GRAPHICS_RESET, PerformanceMetric.STANDARD_RENDER,
				PerformanceMetric.STANDARD_PREVIEW, PerformanceMetric.STANDARD_OUTPUT,
				PerformanceMetric.CUBEMAP_TOTAL, PerformanceMetric.CUBEMAP_PREVIEW,
				PerformanceMetric.CUBEMAP_OUTPUT, PerformanceMetric.CUBEMAP_POS_X,
				PerformanceMetric.CUBEMAP_NEG_X, PerformanceMetric.CUBEMAP_POS_Y,
				PerformanceMetric.CUBEMAP_NEG_Y, PerformanceMetric.CUBEMAP_POS_Z,
				PerformanceMetric.CUBEMAP_NEG_Z, PerformanceMetric.CUBEMAP_BLIT,
				PerformanceMetric.CUBEMAP_MIPMAP, PerformanceMetric.DOMEMASTER,
				PerformanceMetric.DOMEMASTER_PREVIEW, PerformanceMetric.DOMEMASTER_OUTPUT,
				PerformanceMetric.EQUIRECTANGULAR, PerformanceMetric.EQUIRECTANGULAR_PREVIEW,
				PerformanceMetric.EQUIRECTANGULAR_OUTPUT, PerformanceMetric.SKYBOX,
				PerformanceMetric.SKYBOX_PREVIEW, PerformanceMetric.SKYBOX_OUTPUT,
				PerformanceMetric.OUTPUT_PIPELINE, PerformanceMetric.PREVIEW_PIPELINE,
				PerformanceMetric.PREVIEW_COPY, PerformanceMetric.PREVIEW_COMPOSITE,
				PerformanceMetric.FLOATING_PREVIEW, PerformanceMetric.CONTROL_PANEL,
				PerformanceMetric.NDI_CAPTURE, PerformanceMetric.NDI_CONVERSION,
				PerformanceMetric.NDI_QUEUE, PerformanceMetric.NDI_SEND,
				PerformanceMetric.SYPHON, PerformanceMetric.SPOUT
		}, PerformanceMetric.values());
		assertArrayEquals(new GpuTimerArchitecture[]{
				GpuTimerArchitecture.APPLE_SILICON, GpuTimerArchitecture.APPLE_INTEL,
				GpuTimerArchitecture.WINDOWS_ARM64, GpuTimerArchitecture.WINDOWS_X86_64,
				GpuTimerArchitecture.LINUX_ARM64, GpuTimerArchitecture.LINUX_X86_64,
				GpuTimerArchitecture.OTHER
		}, GpuTimerArchitecture.values());
		assertArrayEquals(new GpuTimerBackend[]{
				GpuTimerBackend.NONE, GpuTimerBackend.TIMESTAMP_PAIR,
				GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE, GpuTimerBackend.FENCE_COMPLETION
		}, GpuTimerBackend.values());
		assertArrayEquals(new GpuTimerPolicy[]{
				GpuTimerPolicy.SAFE, GpuTimerPolicy.ARCHITECTURE_AWARE,
				GpuTimerPolicy.TIME_ELAPSED_EXCLUSIVE
		}, GpuTimerPolicy.values());
	}

	@Test
	void publicApiDoesNotExposeMutableFields() {
		for (Class<?> type : concat(STABLE_TYPES, ADVANCED_STABLE_TYPES, EXPERIMENTAL_TYPES)) {
			assertTrue(Arrays.stream(type.getDeclaredFields())
					.filter(field -> Modifier.isPublic(field.getModifiers()))
					.allMatch(field -> type.isEnum() && field.isEnumConstant()), type.getName());
		}
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
	void quaternionDoesNotExposePre2MatrixAlias() {
		assertFalse(publicMethodNames(Quaternion.class).contains("toPMatrix"));
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
	void final20MethodSnapshotIsExact() {
		assertDeclaredApi(ziviDomeLive.class,
				"clearEnvironmentBackground():void",
				"controlEvent(ControlEvent):void",
				"disablePerformanceProfiling():void",
				"dispose():void",
				"draw():void",
				"enableDebugLogging():void",
				"enablePerformanceProfiling(PerformanceMode):void",
				"enablePerformanceProfiling(PerformanceMode,int):void",
				"enablePerformanceProfiling(PerformanceMode,int,GpuTimerPolicy):void",
				"enableReleaseLogging():void",
				"getCurrentView():ViewType",
				"getEnvironmentBackgroundIntensity():float",
				"getEnvironmentBackgroundYawOffset():float",
				"getFishSize():float",
				"getFov():float",
				"getGraphicsCapabilities():GraphicsCapabilities",
				"getLogMode():LogMode",
				"getOutputManager():OutputManager",
				"getOutputResolution():int",
				"getPApplet():PApplet",
				"getPerformanceSnapshot():PerformanceSnapshot",
				"getPitch():float",
				"getRenderMode():RenderMode",
				"getRoll():float",
				"getSceneCamera():OrbitCamera",
				"getSceneManager():SceneManager",
				"getStandardOutputAspectMode():StandardOutputAspectMode",
				"getTargetFrameRate():int",
				"getYaw():float",
				"hasEnvironmentBackground():boolean",
				"isEnvironmentBackgroundVisible():boolean",
				"isInitialized():boolean",
				"isSceneCameraInputEnabled():boolean",
				"isShowPreview():boolean",
				"isSphericalCaptureActive():boolean",
				"keyEvent(KeyEvent):void",
				"mouseEvent(MouseEvent):void",
				"pause():void",
				"post():void",
				"pre():void",
				"registerScene(Scene):void",
				"resetControls():void",
				"resetGraphics(int):void",
				"resetOrientation():void",
				"resetPerformanceStatistics():void",
				"resume():void",
				"setCurrentScene(Scene):void",
				"setCurrentView(ViewType):void",
				"setEnvironmentBackgroundIntensity(float):void",
				"setEnvironmentBackgroundVisible(boolean):void",
				"setEnvironmentBackgroundYawOffset(float):void",
				"setEquirectangularBackground(PImage):void",
				"setEquirectangularBackground(String):void",
				"setFishSize(float):void",
				"setFov(float):void",
				"setLogMode(LogMode):void",
				"setPerformanceOutputDemand(ViewType):void",
				"setPitch(float):void",
				"setRenderMode(RenderMode):void",
				"setRoll(float):void",
				"setScene(Scene):void",
				"setSceneCameraInputEnabled(boolean):void",
				"setSceneManager(SceneManager):void",
				"setShowPreview(boolean):void",
				"setStandardOutputAspectMode(StandardOutputAspectMode):void",
				"setTargetFrameRate(int):void",
				"setup():void",
				"setYaw(float):void",
				"stop():void");

		assertDeclaredApi(Scene.class,
				"configure(SceneServices):void", "dispose():void", "getName():String",
				"keyEvent(KeyEvent):void", "mouseEvent(MouseEvent):void",
				"sceneRender(PGraphicsOpenGL):void", "setupScene():void", "update():void");
		assertDeclaredApi(SceneManager.class,
				"activateScene(Scene):void", "clearScenes():void", "containsScene(Scene):boolean",
				"getCurrentScene():Scene", "getSceneCount():int", "nextScene():void",
				"previousScene():void", "registerScene(Scene):void",
				"reloadCurrentScene():boolean", "setCurrentSceneIndex(int):void");
		assertDeclaredApi(SceneServices.class,
				"actions():SceneActionMap", "applet():PApplet", "assets():SceneAssets",
				"camera():SceneCameraService", "environment():SceneEnvironmentService",
				"frameClock():FrameClock", "ports():ScenePorts", "requestReload():void",
				"tasks():SceneTaskGroup", "timeline():SimulationTimeline");
		assertDeclaredApi(FrameClock.class,
				"getDeltaSeconds():double", "getElapsedSeconds():double", "getFrameIndex():long",
				"getMaxDeltaSeconds():double", "setMaxDeltaSeconds(double):void");
		assertDeclaredApi(SimulationTimeline.class,
				"advance(double,DoubleConsumer):int", "getAccumulator():double",
				"getDroppedUnits():double", "getFixedStep():double", "getMaxSubSteps():int",
				"getPosition():double", "getRate():double", "isPaused():boolean", "jump(double):void",
				"pause():void", "reset():void", "resetAccumulator():void", "resume():void",
				"setFixedStep(double):void", "setMaxSubSteps(int):void", "setPosition(double):void",
				"setRate(double):void");
		assertDeclaredApi(SceneTaskGroup.class,
				"getInFlightCount():int", "getMaxInFlight():int", "isBusy(String):boolean",
				"submitIfIdle(String,Callable,Consumer):boolean",
				"submitIfIdle(String,Callable,Consumer,Consumer):boolean",
				"submitIfIdle(String,Runnable):boolean");
		assertDeclaredApi(SceneAssets.class,
				"cacheShape(String,PShape):PShape", "getOrCreateShape(String,Supplier):PShape",
				"loadImage(String):PImage", "loadShader(String):PShader",
				"loadShader(String,String,String):PShader", "removeShapesByPrefix(String):int");
		assertDeclaredApi(SceneActionMap.class,
				"bindKeyCodePressed(String,int,Runnable):void",
				"bindKeyPressed(String,char,Runnable):void",
				"bindMouse(String,int,Consumer):void", "clear():void", "register(String,Runnable):void",
				"size():int", "trigger(String):boolean", "unregister(String):void");
		assertDeclaredApi(SceneCameraService.class,
				"apply(PGraphicsOpenGL):void", "applyWithViewLighting(PGraphicsOpenGL):void",
				"clearTargetTracking():void",
				"isTrackingTarget():boolean", "orbit():OrbitCamera", "setInputEnabled(boolean):void",
				"setCollapseGuard(float):void", "setDistanceLimits(float,float):void",
				"setDragSensitivity(float):void", "setLerpFactor(float):void",
				"snapToAxisAngle(float,float,float,float,float,float,float,float):void",
				"trackTarget(Supplier):void");
		assertDeclaredApi(SceneEnvironmentService.class,
				"clear():void", "getIntensity():float", "getYawOffset():float", "isVisible():boolean",
				"resetOrientation():void",
				"setEquirectangular(PImage):void", "setIntensity(float):void",
				"setOrientationAxisAngle(float,float,float,float):void",
				"setVisible(boolean):void", "setYawOffset(float):void");
		assertDeclaredApi(ScenePorts.class,
				"connectInput(SceneInputPort,Consumer):void",
				"connectOutput(SceneOutputPort):SceneOutputPort",
				"getDroppedInputCount():long", "getPendingInputCount():int");
		assertDeclaredApi(SceneInputPort.class, "close():void", "start(Consumer):void");
		assertDeclaredApi(SceneOutputPort.class, "close():void", "offer(Object):boolean");

		assertDeclaredApi(OutputManager.class,
				"getLocalTextureBackendName():String", "getLocalTextureView():ViewType",
				"getNdiCapturedFrames():long", "getNdiDroppedFrames():long",
				"getNdiFailedFrames():long", "getNdiSentFrames():long",
				"getOutputFailureReason(OutputType):String", "getOutputState(OutputType):OutputState",
				"getViewForOutput(OutputType):ViewType", "isActive():boolean",
				"isLocalTextureAvailable():boolean", "isLocalTextureInitialized():boolean",
				"isNdiEnabled():boolean", "isOutputEnabled(OutputType):boolean",
				"isSpoutEnabled():boolean", "isSyphonEnabled():boolean",
				"setLocalTextureView(ViewType):void", "setNdiFrameRate(int,int):void",
				"setNdiView(ViewType):void", "setOutputEnabled(OutputType,boolean):void",
				"setSpoutView(ViewType):void", "setSyphonView(ViewType):void",
				"setViewForOutput(OutputType,ViewType):void", "toggleOutput(OutputType):void");
		assertDeclaredApi(Quaternion.class,
				"fromAxisAngle(PVector,float):Quaternion",
				"fromAxisAngle(float,float,float,float):Quaternion",
				"multiply(Quaternion):Quaternion", "normalized():Quaternion",
				"slerp(Quaternion,float):Quaternion", "toMatrix():PMatrix3D",
				"toMatrix(PMatrix3D):void", "w():float", "x():float", "y():float", "z():float");
		assertDeclaredApi(SphericalOrientation.class,
				"getPitch():float", "getQuaternion():Quaternion", "getRoll():float", "getYaw():float",
				"reset():void", "setPitch(float):void", "setRoll(float):void", "setYaw(float):void");
		assertDeclaredApi(OrbitCamera.class,
				"apply(PGraphicsOpenGL):void", "getDistance():float", "getOrientation():Quaternion",
				"getTarget():PVector", "goTo(PVector,Quaternion,float):void",
				"mouseEvent(MouseEvent):void", "reset(float):void", "resetInputState():void",
				"rotateAround(PVector,float):void", "rotateAround(float,float,float,float):void",
				"rotateAroundImmediate(PVector,float):void",
				"rotateAroundImmediate(float,float,float,float):void",
				"setCollapseGuard(float):void", "setDistance(float):void",
				"setDistanceImmediate(float):void", "setDistanceLimits(float,float):void",
				"setDragSensitivity(float):void", "setLerpFactor(float):void",
				"setOrientation(Quaternion):void", "setOrientationImmediate(Quaternion):void",
				"setTarget(PVector):void", "setTarget(float,float,float):void",
				"setTargetImmediate(PVector):void", "setWheelSteps(float,float):void",
				"snapTo(PVector,Quaternion,float):void",
				"snapTo(float,float,float,Quaternion,float):void", "update():void", "zoom(float):void",
				"zoomImmediate(float):void");

		assertDeclaredApi(PerformanceSnapshot.class,
				"getCalls(PerformanceMetric,int):int", "getCubemapCaptureViolations():long",
				"getDiagnostics():List", "getDurationNanos(PerformanceMetric,int):long",
				"getEffectiveMode():PerformanceMode", "getGpuCalls(PerformanceMetric,int):int",
				"getGpuDurationNanos(PerformanceMetric,int):long",
				"getGpuStatistics(PerformanceMetric):MetricStatistics",
				"getGpuTimerArchitecture():GpuTimerArchitecture",
				"getGpuTimerBackend():GpuTimerBackend", "getGpuTimerPolicy():GpuTimerPolicy",
				"getInvariantViolations():long", "getOverwrittenFrames():long",
				"getRequestedMode():PerformanceMode", "getStatistics(PerformanceMetric):MetricStatistics",
				"getStoredFrames():int", "getTotalFrames():long",
				"getUnexpectedPassViolations():long", "hasGpuTimings():boolean");
		assertDeclaredApi(PerformanceSnapshot.MetricStatistics.class,
				"getAverageCallsPerFrame():double", "getAverageFps():double",
				"getAverageMilliseconds():double", "getFramesOver16Point67Milliseconds():long",
				"getFramesOver33Point33Milliseconds():long", "getFramesOver50Milliseconds():long",
				"getMaximumMilliseconds():double", "getOnePercentLowFps():double",
				"getP50Milliseconds():double", "getP95Milliseconds():double",
				"getP99Milliseconds():double", "getSampledFrames():int", "getTotalCalls():long");
		assertDeclaredApi(GraphicsCapabilities.class,
				"isHardwareRasterizer():boolean", "isHardwareRasterizerKnown():boolean",
				"isOpenGlRenderer():boolean", "joglProfile():String", "renderer():String",
				"shadingLanguageVersion():String", "supportsAnisotropicFiltering():boolean",
				"supportsCubemap():boolean", "supportsFramebuffer():boolean",
				"supportsGpuTimerQuery():boolean", "supportsPixelBufferObject():boolean",
				"supportsSeamlessCubemap():boolean", "supportsSyncFence():boolean",
				"supportsTexture():boolean", "vendor():String", "version():String");
		assertDeclaredApi(GpuTimerPolicy.class,
				"allowsElapsedFallback():boolean", "allowsFenceFallback():boolean",
				"selectBackend(GpuTimerArchitecture,int,int):GpuTimerBackend");
		assertDeclaredApi(GpuTimerArchitecture.class,
				"detect(String,String,String,String):GpuTimerArchitecture");
	}

	@Test
	void final20PublicConstructorsAreExact() {
		assertPublicConstructors(ziviDomeLive.class, "ziviDomeLive(PApplet)");
		assertPublicConstructors(SceneManager.class, "SceneManager()");
		assertPublicConstructors(Quaternion.class, "Quaternion(float,float,float,float)");
		assertPublicConstructors(SphericalOrientation.class, "SphericalOrientation()");
		assertPublicConstructors(OrbitCamera.class,
				"OrbitCamera()", "OrbitCamera(PVector,float)", "OrbitCamera(float)");
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

	private static void assertDeclaredApi(Class<?> type, String... expected) {
		assertEquals(Set.of(expected), Arrays.stream(type.getDeclaredMethods())
				.filter(method -> Modifier.isPublic(method.getModifiers()))
				.filter(method -> !method.isSynthetic())
				.filter(method -> !type.isEnum()
						|| !(method.getName().equals("values") || method.getName().equals("valueOf")))
				.map(method -> method.getName() + "(" + Arrays.stream(method.getParameterTypes())
						.map(Class::getSimpleName)
						.collect(Collectors.joining(",")) + "):" + method.getReturnType().getSimpleName())
				.collect(Collectors.toSet()), type.getName());
	}

	private static void assertPublicConstructors(Class<?> type, String... expected) {
		assertEquals(Set.of(expected), Arrays.stream(type.getDeclaredConstructors())
				.filter(constructor -> Modifier.isPublic(constructor.getModifiers()))
				.map(constructor -> type.getSimpleName() + "(" + Arrays.stream(constructor.getParameterTypes())
						.map(Class::getSimpleName)
						.collect(Collectors.joining(",")) + ")")
				.collect(Collectors.toSet()), type.getName());
	}

	private static Class<?>[] concat(Class<?>[]... groups) {
		return Arrays.stream(groups).flatMap(Arrays::stream).toArray(Class<?>[]::new);
	}
}
