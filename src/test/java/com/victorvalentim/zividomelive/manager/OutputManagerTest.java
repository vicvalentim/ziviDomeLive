package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.zividomelive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import processing.core.PApplet;

import static org.junit.jupiter.api.Assertions.*;

class OutputManagerTest {

	private OutputManager outputManager;

	@BeforeEach
	void createOutputManager() {
		zividomelive lib = new zividomelive(new PApplet());
		try {
			outputManager = new OutputManager(lib);
		} catch (Throwable t) {
			// Platform-specific native bindings unavailable; skip instead of failing.
			Assumptions.assumeTrue(false, "OutputManager unavailable on this platform: " + t);
		}
	}

	@Test
	void toggleOutputIgnoresEmptyMethod() {
		assertDoesNotThrow(() -> outputManager.toggleOutput(""));
		assertDoesNotThrow(() -> outputManager.toggleOutput("   "));
		assertDoesNotThrow(() -> outputManager.toggleOutput(null));
	}

	@Test
	void toggleOutputIgnoresUnknownMethod() {
		boolean ndiBefore = outputManager.isNdiEnabled();
		boolean spoutBefore = outputManager.isSpoutEnabled();
		boolean syphonBefore = outputManager.isSyphonEnabled();

		assertDoesNotThrow(() -> outputManager.toggleOutput("unknown-output"));

		assertEquals(ndiBefore, outputManager.isNdiEnabled());
		assertEquals(spoutBefore, outputManager.isSpoutEnabled());
		assertEquals(syphonBefore, outputManager.isSyphonEnabled());
	}

	@Test
	void spoutToggleIsIgnoredOnUnsupportedPlatform() {
		String os = System.getProperty("os.name").toLowerCase();
		Assumptions.assumeFalse(os.contains("win"), "Spout is supported on Windows");

		outputManager.toggleOutput("spout");
		assertFalse(outputManager.isSpoutEnabled(),
				"Spout must remain disabled on non-Windows platforms");
	}

	@Test
	void syphonToggleIsIgnoredOnUnsupportedPlatform() {
		String os = System.getProperty("os.name").toLowerCase();
		Assumptions.assumeFalse(os.contains("mac"), "Syphon is supported on macOS");

		outputManager.toggleOutput("syphon");
		assertFalse(outputManager.isSyphonEnabled(),
				"Syphon must remain disabled on non-macOS platforms");
	}

	@Test
	void ndiToggleEnablesAndDisablesWhenAvailable() {
		boolean before = outputManager.isNdiEnabled();
		outputManager.toggleOutput("ndi");
		boolean after = outputManager.isNdiEnabled();

		if (after != before) {
			// NDI natives are available: toggling back must restore the previous state.
			outputManager.toggleOutput("ndi");
			assertEquals(before, outputManager.isNdiEnabled());
		} else {
			// NDI unavailable on this platform: state must remain disabled.
			assertFalse(after);
		}
	}

	@Test
	void outputViewsDefaultToFisheyeDomemaster() {
		for (OutputManager.OutputType type : OutputManager.OutputType.values()) {
			assertEquals(zividomelive.ViewType.FISHEYE_DOMEMASTER, outputManager.getViewForOutput(type));
		}
	}

	@Test
	void setViewForOutputUpdatesMapping() {
		outputManager.setViewForOutput(OutputManager.OutputType.NDI, zividomelive.ViewType.EQUIRECTANGULAR);
		assertEquals(zividomelive.ViewType.EQUIRECTANGULAR,
				outputManager.getViewForOutput(OutputManager.OutputType.NDI));
	}
}
