package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.manager.OutputManager;
import com.victorvalentim.zividomelive.render.modes.FisheyeDomemaster;
import com.victorvalentim.zividomelive.render.modes.StandardRenderer;
import com.victorvalentim.zividomelive.support.ThreadManager;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.core.PImage;
import processing.opengl.PShader;
import processing.opengl.PGraphicsOpenGL;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ZividomeliveLifecycleTest {

	@Test
	void constructorRejectsNullApplet() {
		assertThrows(IllegalArgumentException.class, () -> new ziviDomeLive(null));
	}

	@Test
	void initialStateIsNotInitialized() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		assertEquals(ziviDomeLive.InitState.NOT_INITIALIZED, lib.getInitState());
	}

	@Test
	void initializeManagersBeforeSetupKeepsStateUnchanged() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		lib.initializeManagers();
		assertEquals(ziviDomeLive.InitState.NOT_INITIALIZED, lib.getInitState(),
				"initializeManagers must not advance state before setup completes");
	}

	@Test
	void setupTransitionsToSetupComplete() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		lib.setup();
		assertEquals(ziviDomeLive.InitState.SETUP_COMPLETE, lib.getInitState());
	}

	@Test
	void setupIsIdempotent() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		lib.setup();
		OutputManager firstOutputManager = lib.getOutputManager();
		Scene firstScene = lib.getSceneManager().getCurrentScene();

		lib.setup();

		assertSame(firstOutputManager, lib.getOutputManager());
		assertSame(firstScene, lib.getSceneManager().getCurrentScene());
		assertEquals(ziviDomeLive.InitState.SETUP_COMPLETE, lib.getInitState());
	}

	@Test
	void explicitSceneReplacesDefaultFallbackRegistration() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		lib.setup();
		TrackingScene explicitScene = new TrackingScene("Explicit");

		lib.setScene(explicitScene);

		assertEquals(1, lib.getSceneManager().getSceneCount());
		assertSame(explicitScene, lib.getSceneManager().getCurrentScene());
	}

	@Test
	void targetFrameRateDefaultsTo60AndRejectsInvalidValues() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		assertEquals(60, lib.getTargetFrameRate());

		lib.setTargetFrameRate(0);
		assertEquals(60, lib.getTargetFrameRate(), "Non-positive values must be ignored");

		lib.setTargetFrameRate(-30);
		assertEquals(60, lib.getTargetFrameRate(), "Non-positive values must be ignored");

		lib.setTargetFrameRate(120);
		assertEquals(120, lib.getTargetFrameRate());
	}

	@Test
	void environmentBackgroundStateIsConfigurableBeforeRendererInitialization() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		PImage image = new PImage(4, 2);

		assertFalse(lib.hasEnvironmentBackground());
		assertTrue(lib.isEnvironmentBackgroundVisible());
		assertEquals(1.0f, lib.getEnvironmentBackgroundIntensity(), 1e-6f);
		assertEquals(0.0f, lib.getEnvironmentBackgroundYawOffset(), 1e-6f);

		lib.setEquirectangularBackground(image);
		lib.setEnvironmentBackgroundVisible(false);
		lib.setEnvironmentBackgroundIntensity(-2.0f);
		lib.setEnvironmentBackgroundYawOffset(0.75f);

		assertTrue(lib.hasEnvironmentBackground());
		assertFalse(lib.isEnvironmentBackgroundVisible());
		assertEquals(0.0f, lib.getEnvironmentBackgroundIntensity(), 1e-6f);
		assertEquals(0.75f, lib.getEnvironmentBackgroundYawOffset(), 1e-6f);

		lib.clearEnvironmentBackground();
		assertFalse(lib.hasEnvironmentBackground());
	}

	@Test
	void terminalDisposeReleasesTheFacadeBorrowedSourceReference() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		lib.setEquirectangularBackground(new PImage(4, 2));

		lib.dispose();

		assertFalse(lib.hasEnvironmentBackground());
	}

	@Test
	void targetFrameRateDoesNotRestartAppletWhenValueIsUnchanged() throws Exception {
		FrameRateTrackingApplet applet = new FrameRateTrackingApplet();
		ziviDomeLive lib = new ziviDomeLive(applet);
		setInitState(lib, ziviDomeLive.InitState.SETUP_COMPLETE);

		lib.setTargetFrameRate(60);
		assertEquals(0, applet.frameRateCalls);

		lib.setTargetFrameRate(30);
		lib.setTargetFrameRate(30);
		assertEquals(1, applet.frameRateCalls);
		assertEquals(30f, applet.lastFrameRate);
	}

	@Test
	void setupStartsWithOutputsDisabled() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		lib.setup();
		assertFalse(lib.isEnableOutput(), "Outputs should remain opt-in after setup");
	}

	@Test
	void replacementFisheyeInheritsFacadeSizePercentage() {
		StubApplet applet = new StubApplet();
		ziviDomeLive lib = new ziviDomeLive(applet);
		lib.setFishSize(42.5f);

		FisheyeDomemaster replacement = new FisheyeDomemaster(1024, "frag", "vert", applet);
		lib.setFisheyeDomemaster(replacement);

		assertEquals(42.5f, replacement.getSizePercentage(), 1e-6f);
	}

	@Test
	void failedRendererInitializationKeepsPostHookForRetry() {
		TrackingApplet applet = new TrackingApplet();
		ziviDomeLive lib = new FailingRendererDome(applet);
		lib.setup();

		lib.post();

		assertEquals(ziviDomeLive.InitState.SETUP_COMPLETE, lib.getInitState());
		assertFalse(lib.isInitialized());
		assertFalse(applet.postUnregistered, "post hook must remain registered after a partial failure");
	}

	@Test
	void pauseResumeRestoresOnlyOutputsThatWereEnabled() throws Exception {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		FakeOutputManager outputs = new FakeOutputManager(lib);
		outputs.ndiEnabled = true;
		outputs.syphonEnabled = true;
		setOutputManager(lib, outputs);

		lib.pause();
		lib.pause();

		assertEquals(1, outputs.shutdownCalls, "Repeated pause must not overwrite the resume snapshot");
		assertFalse(outputs.ndiEnabled);
		assertFalse(outputs.spoutEnabled);
		assertFalse(outputs.syphonEnabled);

		lib.resume();
		lib.resume();

		assertTrue(outputs.ndiEnabled);
		assertFalse(outputs.spoutEnabled);
		assertTrue(outputs.syphonEnabled);
		assertEquals(2, outputs.toggleCalls, "Repeated resume must not toggle restored outputs off");
	}

	@Test
	void isEnableOutputDelegatesToOutputManager() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		lib.setup();

		OutputManagerState state = readOutputState(lib);
		assertEquals(state.anyEnabled, lib.isEnableOutput(),
				"isEnableOutput must mirror the OutputManager enabled flags");
	}

	@Test
	void stopDoesNotShutdownSharedThreadManager() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		lib.setup();

		lib.stop();

		assertFalse(ThreadManager.isShutdown(),
				"The shared ThreadManager executor must stay alive across library instances");
		assertFalse(lib.isInitialized());
		assertEquals(ziviDomeLive.InitState.NOT_INITIALIZED, lib.getInitState());
	}

	@Test
	void disposeDoesNotShutdownSharedThreadManager() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		lib.setup();

		lib.dispose();

		assertFalse(ThreadManager.isShutdown(),
				"The shared ThreadManager executor must stay alive after disposing one library instance");
	}

	@Test
	void sceneManagerIsTheAuthorityForSceneUpdates() throws Exception {
		PApplet applet = new PApplet();
		ziviDomeLive lib = new ziviDomeLive(applet);
		SceneManager scenes = new SceneManager();
		TrackingScene first = new TrackingScene("A");
		TrackingScene second = new TrackingScene("B");
		scenes.registerScene(first);
		scenes.registerScene(second);
		lib.setSceneManager(scenes);
		StandardRenderer outputRenderer = new StandardRenderer(applet, 0, 0, first);
		StandardRenderer previewRenderer = new StandardRenderer(applet, 0, 0, first);
		setField(lib, "standardRenderer", outputRenderer);
		setField(lib, "standardRendererPreview", previewRenderer);
		setInitState(lib, ziviDomeLive.InitState.MANAGERS_READY);

		scenes.nextScene();
		lib.pre();

		assertEquals(0, first.updateCount);
		assertEquals(1, second.updateCount,
				"Facade must query SceneManager instead of a cached scene reference");
		assertSame(second, readRendererScene(outputRenderer));
		assertSame(second, readRendererScene(previewRenderer));
	}

	@Test
	void replacingSceneManagerDisposesPreviouslyOwnedActiveScene() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		TrackingScene previous = new TrackingScene("Previous");
		lib.setScene(previous);

		SceneManager replacement = new SceneManager();
		replacement.registerScene(new TrackingScene("Replacement"));
		lib.setSceneManager(replacement);

		assertEquals(1, previous.disposeCount);
		assertSame(replacement, lib.getSceneManager());
	}

	@Test
	void pauseBlocksSceneUpdatesUntilResume() throws Exception {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		TrackingScene scene = new TrackingScene("Paused");
		lib.setScene(scene);
		setInitState(lib, ziviDomeLive.InitState.MANAGERS_READY);

		lib.pause();
		lib.pre();
		assertEquals(0, scene.updateCount);

		lib.resume();
		lib.pre();
		assertEquals(1, scene.updateCount);
	}

	@Test
	void disposeIsTerminalIdempotentAndDisposesActiveSceneOnce() {
		TrackingApplet applet = new TrackingApplet();
		ziviDomeLive lib = new ziviDomeLive(applet);
		TrackingScene scene = new TrackingScene("Owned");
		lib.setScene(scene);

		lib.dispose();
		lib.dispose();
		lib.setup();

		assertEquals(1, scene.disposeCount);
		assertEquals(0, lib.getSceneManager().getSceneCount());
		assertEquals(ziviDomeLive.InitState.NOT_INITIALIZED, lib.getInitState());
		assertFalse(lib.isInitialized());
		assertFalse(applet.registeredMethods.isEmpty());
		assertEquals(applet.registeredMethods, applet.unregisteredMethods);
	}

	@Test
	void serviceAwareSceneIsConfiguredBeforeSetupAndClosedAfterDispose() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		ServiceAwareScene scene = new ServiceAwareScene();

		lib.setScene(scene);

		assertEquals(1, scene.configureCount);
		assertEquals(1, scene.setupCount);
		assertNotNull(scene.servicesSeenDuringSetup);
		SceneServices activation = scene.servicesSeenDuringSetup;

		lib.dispose();

		assertEquals(1, scene.disposeCount);
		assertTrue(activation.isClosed());
	}

	@Test
	void inactiveRegisteredSceneDoesNotOwnAnActivationContext() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		ServiceAwareScene active = new ServiceAwareScene();
		ServiceAwareScene inactive = new ServiceAwareScene();

		lib.setScene(active);
		lib.registerScene(inactive);

		assertSame(active.services, lib.getSceneServices(active));
		assertNull(lib.getSceneServices(inactive));
		lib.dispose();
	}

	@Test
	void untouchedSceneEnvironmentServicePreservesFacadeOwnedBackground() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		PImage facadeSource = new PImage(4, 2);
		lib.setEquirectangularBackground(facadeSource);
		ServiceAwareScene first = new ServiceAwareScene();
		ServiceAwareScene second = new ServiceAwareScene();

		lib.setScene(first);
		lib.registerScene(second);
		lib.getSceneManager().nextScene();

		assertSame(facadeSource, lib.getEnvironmentBackgroundSource());
		lib.dispose();
	}

	@Test
	void sceneEnvironmentServiceRestoresTheStateItTemporarilyOwned() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		PImage facadeSource = new PImage(4, 2);
		PImage sceneSource = new PImage(8, 4);
		lib.setEquirectangularBackground(facadeSource);
		lib.setEnvironmentBackgroundVisible(false);
		lib.setEnvironmentBackgroundIntensity(0.75f);
		lib.setEnvironmentBackgroundYawOffset(0.25f);
		ServiceAwareScene scene = new ServiceAwareScene();
		ServiceAwareScene next = new ServiceAwareScene();
		lib.setScene(scene);
		lib.registerScene(next);

		scene.services.environment().setEquirectangular(sceneSource);
		scene.services.environment().setVisible(true);
		scene.services.environment().setIntensity(1.5f);
		scene.services.environment().setYawOffset(0.5f);
		lib.getSceneManager().nextScene();

		assertSame(facadeSource, lib.getEnvironmentBackgroundSource());
		assertFalse(lib.isEnvironmentBackgroundVisible());
		assertEquals(0.75f, lib.getEnvironmentBackgroundIntensity(), 1e-6f);
		assertEquals(0.25f, lib.getEnvironmentBackgroundYawOffset(), 1e-6f);
		lib.dispose();
	}

	@Test
	void requestedReloadIsExecutedAtFrameBoundaryWithFreshServices() throws Exception {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		ServiceAwareScene scene = new ServiceAwareScene();
		lib.setScene(scene);
		SceneServices firstActivation = scene.services;
		setInitState(lib, ziviDomeLive.InitState.MANAGERS_READY);

		firstActivation.requestReload();
		lib.pre();

		assertEquals(1, scene.disposeCount);
		assertEquals(2, scene.setupCount);
		assertEquals(2, scene.configureCount);
		assertTrue(firstActivation.isClosed());
		assertNotSame(firstActivation, scene.services);
		assertFalse(scene.services.isClosed());
		lib.dispose();
	}

	@Test
	void facadeDispatchesSceneActionsWithoutReplacingRawCallbacks() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		ServiceAwareScene scene = new ServiceAwareScene();
		lib.setScene(scene);
		scene.services.actions().bindKeyPressed("test", 'x', () -> scene.actionCount++);

		lib.keyEvent(new processing.event.KeyEvent(
				null, 0, processing.event.KeyEvent.PRESS, 0, 'x', 0));

		assertEquals(1, scene.actionCount);
		assertEquals(1, scene.rawKeyCount);
		lib.dispose();
	}

	@Test
	void cameraTargetTrackingRunsAfterSceneUpdate() throws Exception {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());
		ServiceAwareScene scene = new ServiceAwareScene();
		lib.setScene(scene);
		scene.services.camera().trackTarget(() -> new processing.core.PVector(100, 20, -5));
		setInitState(lib, ziviDomeLive.InitState.MANAGERS_READY);

		lib.pre();

		assertTrue(lib.getSceneCamera().getTarget().x > 0f);
		lib.dispose();
	}

	private record OutputManagerState(boolean anyEnabled) {}

	private static void setOutputManager(ziviDomeLive lib, OutputManager outputManager) throws Exception {
		Field field = ziviDomeLive.class.getDeclaredField("outputManager");
		field.setAccessible(true);
		field.set(lib, outputManager);
	}

	private static void setInitState(ziviDomeLive lib, ziviDomeLive.InitState state) throws Exception {
		Field field = ziviDomeLive.class.getDeclaredField("initState");
		field.setAccessible(true);
		field.set(lib, state);
	}

	private static void setField(ziviDomeLive lib, String fieldName, Object value) throws Exception {
		Field field = ziviDomeLive.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(lib, value);
	}

	private static Scene readRendererScene(StandardRenderer renderer) throws Exception {
		Field field = StandardRenderer.class.getDeclaredField("currentScene");
		field.setAccessible(true);
		return (Scene) field.get(renderer);
	}

	private OutputManagerState readOutputState(ziviDomeLive lib) {
		var om = lib.getOutputManager();
		assertNotNull(om, "OutputManager must be created during setup");
		return new OutputManagerState(om.isNdiEnabled() || om.isSpoutEnabled() || om.isSyphonEnabled());
	}

	private static class StubApplet extends PApplet {
		@Override
		public PShader loadShader(String fragFilename, String vertFilename) {
			return null;
		}
	}

	private static class FrameRateTrackingApplet extends StubApplet {
		private int frameRateCalls;
		private float lastFrameRate;

		@Override
		public void frameRate(float fps) {
			frameRateCalls++;
			lastFrameRate = fps;
		}
	}

	private static class TrackingApplet extends StubApplet {
		private boolean postUnregistered;
		private final Set<String> registeredMethods = new LinkedHashSet<>();
		private final Set<String> unregisteredMethods = new LinkedHashSet<>();

		@Override
		public void registerMethod(String methodName, Object target) {
			registeredMethods.add(methodName);
		}

		@Override
		public void unregisterMethod(String methodName, Object target) {
			unregisteredMethods.add(methodName);
			if ("post".equals(methodName)) {
				postUnregistered = true;
			}
		}
	}

	private static class TrackingScene implements Scene {
		private final String name;
		private int updateCount;
		private int disposeCount;

		private TrackingScene(String name) {
			this.name = name;
		}

		@Override
		public void sceneRender(PGraphicsOpenGL graphics) {
		}

		@Override
		public void update() {
			updateCount++;
		}

		@Override
		public void dispose() {
			disposeCount++;
		}

		@Override
		public String getName() {
			return name;
		}
	}

	private static class ServiceAwareScene implements Scene {
		private SceneServices services;
		private SceneServices servicesSeenDuringSetup;
		private int configureCount;
		private int setupCount;
		private int disposeCount;
		private int actionCount;
		private int rawKeyCount;

		@Override
		public void configure(SceneServices services) {
			this.services = services;
			configureCount++;
		}

		@Override
		public void setupScene() {
			servicesSeenDuringSetup = services;
			setupCount++;
		}

		@Override
		public void sceneRender(PGraphicsOpenGL graphics) {
		}

		@Override
		public void keyEvent(processing.event.KeyEvent event) {
			rawKeyCount++;
		}

		@Override
		public void dispose() {
			disposeCount++;
		}
	}

	private static class FailingRendererDome extends ziviDomeLive {
		FailingRendererDome(PApplet applet) {
			super(applet);
		}

		@Override
		void initializeRenderers() {
			throw new IllegalStateException("expected renderer initialization failure");
		}
	}

	private static class FakeOutputManager extends OutputManager {
		private boolean ndiEnabled;
		private boolean spoutEnabled;
		private boolean syphonEnabled;
		private int shutdownCalls;
		private int toggleCalls;

		FakeOutputManager(ziviDomeLive parent) {
			super(parent);
		}

		@Override
		public boolean isNdiEnabled() {
			return ndiEnabled;
		}

		@Override
		public boolean isSpoutEnabled() {
			return spoutEnabled;
		}

		@Override
		public boolean isSyphonEnabled() {
			return syphonEnabled;
		}

		@Override
		public void shutdownOutputs() {
			shutdownCalls++;
			ndiEnabled = false;
			spoutEnabled = false;
			syphonEnabled = false;
		}

		@Override
		public void toggleOutput(String method) {
			toggleCalls++;
			switch (method) {
				case "ndi":
					ndiEnabled = !ndiEnabled;
					break;
				case "spout":
					spoutEnabled = !spoutEnabled;
					break;
				case "syphon":
					syphonEnabled = !syphonEnabled;
					break;
				default:
					throw new IllegalArgumentException("Unexpected output: " + method);
			}
		}
	}
}
