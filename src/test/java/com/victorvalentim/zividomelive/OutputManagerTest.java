package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.manager.OutputManager;
import me.walkerknapp.devolay.DevolayFrameFormatType;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class OutputManagerTest {

	private OutputManagerImpl outputManager;

	@BeforeEach
	void createOutputManager() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());

		try {
			outputManager = new OutputManagerImpl(lib);
		} catch (Throwable t) {
			/*
			 * If OutputManager itself cannot be constructed because a required
			 * platform/native binding is unavailable in the current test
			 * environment, the whole fixture is not testable.
			 *
			 * This is different from Syphon/Spout platform selection below:
			 * those cases are part of the normal contract and must result in
			 * assertions rather than assumptions.
			 */
			Assumptions.assumeTrue(
					false,
					"OutputManager unavailable on this platform: " + t
			);
		}
	}

	@AfterEach
	void releaseOutputs() {
		if (outputManager != null) {
			outputManager.shutdownOutputsTerminal();
		}
	}

	@Test
	void outputsAreDisabledByDefault() {
		assertFalse(outputManager.isNdiEnabled());
		assertFalse(outputManager.isSpoutEnabled());
		assertFalse(outputManager.isSyphonEnabled());
	}

	@Test
	void typedOutputControlRejectsNull() {
		assertThrows(IllegalArgumentException.class,
				() -> outputManager.toggleOutput(null));
		assertThrows(IllegalArgumentException.class,
				() -> outputManager.setOutputEnabled(null, true));
		assertThrows(IllegalArgumentException.class,
				() -> outputManager.isOutputEnabled(null));
	}

	@Test
	void typedEnabledQueriesMatchDedicatedQueries() {
		assertEquals(outputManager.isNdiEnabled(),
				outputManager.isOutputEnabled(OutputManager.OutputType.NDI));
		assertEquals(outputManager.isSpoutEnabled(),
				outputManager.isOutputEnabled(OutputManager.OutputType.SPOUT));
		assertEquals(outputManager.isSyphonEnabled(),
				outputManager.isOutputEnabled(OutputManager.OutputType.SYPHON));
	}

	@Test
	void spoutToggleMatchesCurrentPlatformContract() {
		boolean windows = isWindows();

		assertFalse(
				outputManager.isSpoutEnabled(),
				"Spout must start disabled"
		);

		assertDoesNotThrow(
				() -> outputManager.toggleOutput(OutputManager.OutputType.SPOUT),
				"Spout toggle must not throw on any platform"
		);

		boolean enabledAfterToggle = outputManager.isSpoutEnabled();

		if (!windows) {
			/*
			 * Spout is a Windows-only backend.
			 *
			 * On macOS, Linux and other platforms the request must be ignored
			 * and the backend must remain disabled.
			 */
			assertFalse(
					enabledAfterToggle,
					"Spout must remain disabled outside Windows"
			);
			return;
		}

		/*
		 * Windows is the supported Spout platform.
		 *
		 * A unit/headless environment may still lack a usable native Spout
		 * runtime or graphics context. Remaining disabled is therefore a valid
		 * runtime result here.
		 *
		 * If the backend does become enabled, verify that its toggle contract
		 * is reversible.
		 */
		if (enabledAfterToggle) {
			assertDoesNotThrow(
					() -> outputManager.toggleOutput(OutputManager.OutputType.SPOUT),
					"Spout must toggle off cleanly on Windows"
			);

			assertFalse(
					outputManager.isSpoutEnabled(),
					"Spout must disable again after a second toggle"
			);
		}
	}

	@Test
	void syphonToggleMatchesCurrentPlatformContract() {
		boolean macOS = isMacOS();

		assertFalse(
				outputManager.isSyphonEnabled(),
				"Syphon must start disabled"
		);

		assertDoesNotThrow(
				() -> outputManager.toggleOutput(OutputManager.OutputType.SYPHON),
				"Syphon toggle must not throw on any platform"
		);

		boolean enabledAfterToggle = outputManager.isSyphonEnabled();

		if (!macOS) {
			/*
			 * Syphon is a macOS-only backend.
			 *
			 * On Windows, Linux and other platforms the request must be ignored
			 * and the backend must remain disabled.
			 */
			assertFalse(
					enabledAfterToggle,
					"Syphon must remain disabled outside macOS"
			);
			return;
		}

		/*
		 * macOS is the supported Syphon platform.
		 *
		 * A unit/headless environment may still lack a usable native Syphon
		 * runtime or graphics context. Remaining disabled is therefore a valid
		 * runtime result here.
		 *
		 * If the backend does become enabled, verify that its toggle contract
		 * is reversible.
		 */
		if (enabledAfterToggle) {
			assertDoesNotThrow(
					() -> outputManager.toggleOutput(OutputManager.OutputType.SYPHON),
					"Syphon must toggle off cleanly on macOS"
			);

			assertFalse(
					outputManager.isSyphonEnabled(),
					"Syphon must disable again after a second toggle"
			);
		}
	}

	@Test
	void ndiToggleEnablesAndDisablesWhenAvailable() {
		boolean before = outputManager.isNdiEnabled();

		outputManager.toggleOutput(OutputManager.OutputType.NDI);

		boolean after = outputManager.isNdiEnabled();

		if (after != before) {
			/*
			 * NDI natives are available: toggling back must restore the
			 * previous state.
			 */
			outputManager.setOutputEnabled(OutputManager.OutputType.NDI, false);

			assertEquals(
					before,
					outputManager.isNdiEnabled()
			);
		} else {
			/*
			 * NDI is unavailable in the current runtime: state must remain
			 * disabled.
			 */
			assertFalse(after);
		}
	}

	@Test
	void outputViewsDefaultToFisheyeDomemaster() {
		for (OutputManager.OutputType type : OutputManager.OutputType.values()) {
			assertEquals(
					ViewType.DOMEMASTER,
					outputManager.getViewForOutput(type)
			);
		}
	}

	@Test
	void setViewForOutputUpdatesMapping() {
		outputManager.setViewForOutput(
				OutputManager.OutputType.NDI,
				ViewType.EQUIRECTANGULAR
		);

		assertEquals(
				ViewType.EQUIRECTANGULAR,
				outputManager.getViewForOutput(OutputManager.OutputType.NDI)
		);
	}

	@Test
	void ndiMetadataDefaultsToFacadeFrameRateAndProgressiveFrames() {
		ziviDomeLive lib = new ziviDomeLive(new PApplet());

		lib.setTargetFrameRate(30);

		OutputManagerImpl manager = new OutputManagerImpl(lib);

		assertEquals(
				30,
				manager.ndiFrameRateNumerator()
		);

		assertEquals(
				1,
				manager.ndiFrameRateDenominator()
		);

		assertEquals(
				DevolayFrameFormatType.PROGRESSIVE,
				OutputManagerImpl.NDI_FRAME_FORMAT_TYPE
		);
	}

	@Test
	void ndiFrameRateSupportsFractionalMetadataAndRejectsInvalidValues() {
		outputManager.setNdiFrameRate(60000, 1001);

		assertEquals(
				60000,
				outputManager.ndiFrameRateNumerator()
		);

		assertEquals(
				1001,
				outputManager.ndiFrameRateDenominator()
		);

		assertThrows(
				IllegalArgumentException.class,
				() -> outputManager.setNdiFrameRate(0, 1)
		);

		assertThrows(
				IllegalArgumentException.class,
				() -> outputManager.setNdiFrameRate(60, 0)
		);
	}

	@Test
	void facadeFrameRateChangesUpdateNdiMetadataAfterSetup() {
		ziviDomeLive lib = new ziviDomeLive(new HeadlessApplet());

		lib.setup();
		lib.setTargetFrameRate(24);

		assertEquals(
				24,
				lib.outputManagerInternal().ndiFrameRateNumerator()
		);

		assertEquals(
				1,
				lib.outputManagerInternal().ndiFrameRateDenominator()
		);
	}

	private static boolean isWindows() {
		return normalizedOsName().contains("win");
	}

	private static boolean isMacOS() {
		return normalizedOsName().contains("mac");
	}

	private static String normalizedOsName() {
		return System.getProperty("os.name", "")
				.toLowerCase(Locale.ROOT);
	}

	private static class HeadlessApplet extends PApplet {

		@Override
		public void frameRate(float fps) {
			// No Processing surface exists in this unit test.
		}
	}
}
