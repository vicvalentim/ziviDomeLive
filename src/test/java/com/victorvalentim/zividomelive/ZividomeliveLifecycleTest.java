package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.manager.OutputManager;
import com.victorvalentim.zividomelive.render.modes.FisheyeDomemaster;
import com.victorvalentim.zividomelive.support.ThreadManager;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.opengl.PShader;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ZividomeliveLifecycleTest {

	@Test
	void constructorRejectsNullApplet() {
		assertThrows(IllegalArgumentException.class, () -> new zividomelive(null));
	}

	@Test
	void initialStateIsNotInitialized() {
		zividomelive lib = new zividomelive(new PApplet());
		assertEquals(zividomelive.InitState.NOT_INITIALIZED, lib.getInitState());
	}

	@Test
	void initializeManagersBeforeSetupKeepsStateUnchanged() {
		zividomelive lib = new zividomelive(new PApplet());
		lib.initializeManagers();
		assertEquals(zividomelive.InitState.NOT_INITIALIZED, lib.getInitState(),
				"initializeManagers must not advance state before setup completes");
	}

	@Test
	void setupTransitionsToSetupComplete() {
		zividomelive lib = new zividomelive(new PApplet());
		lib.setup();
		assertEquals(zividomelive.InitState.SETUP_COMPLETE, lib.getInitState());
	}

	@Test
	void targetFrameRateDefaultsTo60AndRejectsInvalidValues() {
		zividomelive lib = new zividomelive(new PApplet());
		assertEquals(60, lib.getTargetFrameRate());

		lib.setTargetFrameRate(0);
		assertEquals(60, lib.getTargetFrameRate(), "Non-positive values must be ignored");

		lib.setTargetFrameRate(-30);
		assertEquals(60, lib.getTargetFrameRate(), "Non-positive values must be ignored");

		lib.setTargetFrameRate(120);
		assertEquals(120, lib.getTargetFrameRate());
	}

	@Test
	void setupStartsWithOutputsDisabled() {
		zividomelive lib = new zividomelive(new PApplet());
		lib.setup();
		assertFalse(lib.isEnableOutput(), "Outputs should remain opt-in after setup");
	}

	@Test
	void replacementFisheyeInheritsFacadeSizePercentage() {
		StubApplet applet = new StubApplet();
		zividomelive lib = new zividomelive(applet);
		lib.setFishSize(42.5f);

		FisheyeDomemaster replacement = new FisheyeDomemaster(1024, "frag", "vert", applet);
		lib.setFisheyeDomemaster(replacement);

		assertEquals(42.5f, replacement.getSizePercentage(), 1e-6f);
	}

	@Test
	void failedRendererInitializationKeepsPostHookForRetry() {
		TrackingApplet applet = new TrackingApplet();
		zividomelive lib = new FailingRendererDome(applet);
		lib.setup();

		lib.post();

		assertEquals(zividomelive.InitState.SETUP_COMPLETE, lib.getInitState());
		assertFalse(lib.isInitialized());
		assertFalse(applet.postUnregistered, "post hook must remain registered after a partial failure");
	}

	@Test
	void pauseResumeRestoresOnlyOutputsThatWereEnabled() throws Exception {
		zividomelive lib = new zividomelive(new PApplet());
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
		zividomelive lib = new zividomelive(new PApplet());
		lib.setup();

		OutputManagerState state = readOutputState(lib);
		assertEquals(state.anyEnabled, lib.isEnableOutput(),
				"isEnableOutput must mirror the OutputManager enabled flags");
	}

	@Test
	void stopDoesNotShutdownSharedThreadManager() {
		zividomelive lib = new zividomelive(new PApplet());
		lib.setup();

		lib.stop();

		assertFalse(ThreadManager.isShutdown(),
				"The shared ThreadManager executor must stay alive across library instances");
	}

	@Test
	void disposeDoesNotShutdownSharedThreadManager() {
		zividomelive lib = new zividomelive(new PApplet());
		lib.setup();

		lib.dispose();

		assertFalse(ThreadManager.isShutdown(),
				"The shared ThreadManager executor must stay alive after disposing one library instance");
	}

	private record OutputManagerState(boolean anyEnabled) {}

	private static void setOutputManager(zividomelive lib, OutputManager outputManager) throws Exception {
		Field field = zividomelive.class.getDeclaredField("outputManager");
		field.setAccessible(true);
		field.set(lib, outputManager);
	}

	private OutputManagerState readOutputState(zividomelive lib) {
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

	private static class TrackingApplet extends StubApplet {
		private boolean postUnregistered;

		@Override
		public void unregisterMethod(String methodName, Object target) {
			if ("post".equals(methodName)) {
				postUnregistered = true;
			}
		}
	}

	private static class FailingRendererDome extends zividomelive {
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

		FakeOutputManager(zividomelive parent) {
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
