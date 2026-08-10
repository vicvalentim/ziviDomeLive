package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.zividomelive;
import me.walkerknapp.devolay.DevolayFrameFormatType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import processing.core.PApplet;

import java.lang.reflect.Field;

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
	void outputsAreDisabledByDefault() {
		assertFalse(outputManager.isNdiEnabled());
		assertFalse(outputManager.isSpoutEnabled());
		assertFalse(outputManager.isSyphonEnabled());
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

	@Test
	void ndiMetadataDefaultsToFacadeFrameRateAndProgressiveFrames() throws Exception {
		zividomelive lib = new zividomelive(new PApplet());
		lib.setTargetFrameRate(30);
		OutputManager manager = new OutputManager(lib);

		assertEquals(30, readIntField(manager, "ndiFrameRateNumerator"));
		assertEquals(1, readIntField(manager, "ndiFrameRateDenominator"));
		assertEquals(DevolayFrameFormatType.PROGRESSIVE, OutputManager.NDI_FRAME_FORMAT_TYPE);
	}

	@Test
	void ndiFrameRateSupportsFractionalMetadataAndRejectsInvalidValues() throws Exception {
		outputManager.setNdiFrameRate(60000, 1001);

		assertEquals(60000, readIntField(outputManager, "ndiFrameRateNumerator"));
		assertEquals(1001, readIntField(outputManager, "ndiFrameRateDenominator"));
		assertThrows(IllegalArgumentException.class, () -> outputManager.setNdiFrameRate(0, 1));
		assertThrows(IllegalArgumentException.class, () -> outputManager.setNdiFrameRate(60, 0));
	}

	@Test
	void facadeFrameRateChangesUpdateNdiMetadataAfterSetup() throws Exception {
		zividomelive lib = new zividomelive(new HeadlessApplet());
		lib.setup();

		lib.setTargetFrameRate(24);

		assertEquals(24, readIntField(lib.getOutputManager(), "ndiFrameRateNumerator"));
		assertEquals(1, readIntField(lib.getOutputManager(), "ndiFrameRateDenominator"));
	}

	private static int readIntField(OutputManager manager, String fieldName) throws Exception {
		Field field = OutputManager.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.getInt(manager);
	}

	private static class HeadlessApplet extends PApplet {
		@Override
		public void frameRate(float fps) {
			// No Processing surface exists in this unit test.
		}
	}
}
