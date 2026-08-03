package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.Test;
import processing.core.PApplet;

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
	void isEnableOutputDelegatesToOutputManager() {
		zividomelive lib = new zividomelive(new PApplet());
		lib.setup();

		OutputManagerState state = readOutputState(lib);
		assertEquals(state.anyEnabled, lib.isEnableOutput(),
				"isEnableOutput must mirror the OutputManager enabled flags");
	}

	private record OutputManagerState(boolean anyEnabled) {}

	private OutputManagerState readOutputState(zividomelive lib) {
		var om = lib.getOutputManager();
		assertNotNull(om, "OutputManager must be created during setup");
		return new OutputManagerState(om.isNdiEnabled() || om.isSpoutEnabled() || om.isSyphonEnabled());
	}
}
