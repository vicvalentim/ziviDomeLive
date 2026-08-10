package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.zividomelive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;

import static org.junit.jupiter.api.Assertions.*;

class OutputRoutingCompatibilityTest {

	private OutputManager outputManager;

	@BeforeEach
	void createOutputManager() {
		outputManager = new OutputManager(new zividomelive(new PApplet()));
	}

	@Test
	void everyOutputDefaultsToFisheyeDomemaster() {
		for (OutputManager.OutputType outputType : OutputManager.OutputType.values()) {
			assertEquals(zividomelive.ViewType.FISHEYE_DOMEMASTER,
					outputManager.getViewForOutput(outputType));
		}
	}

	@Test
	void eachOutputCanSelectAViewIndependently() {
		outputManager.setNdiView(zividomelive.ViewType.EQUIRECTANGULAR);
		outputManager.setSpoutView(zividomelive.ViewType.CUBEMAP);
		outputManager.setSyphonView(zividomelive.ViewType.STANDARD);

		assertEquals(zividomelive.ViewType.EQUIRECTANGULAR,
				outputManager.getViewForOutput(OutputManager.OutputType.NDI));
		assertEquals(zividomelive.ViewType.CUBEMAP,
				outputManager.getViewForOutput(OutputManager.OutputType.SPOUT));
		assertEquals(zividomelive.ViewType.STANDARD,
				outputManager.getViewForOutput(OutputManager.OutputType.SYPHON));
	}

	@Test
	void initializedButDisabledOutputsDoNotCreateRenderRequirements() {
		for (zividomelive.ViewType viewType : zividomelive.ViewType.values()) {
			assertFalse(outputManager.requiresView(viewType),
					"Disabled outputs must not require rendering for " + viewType);
		}
		assertFalse(outputManager.isActive());
	}

	@Test
	@SuppressWarnings("deprecation")
	void legacySingleViewSetterDoesNotMutatePerOutputRouting() {
		outputManager.setNdiView(zividomelive.ViewType.EQUIRECTANGULAR);
		outputManager.setSpoutView(zividomelive.ViewType.CUBEMAP);
		outputManager.setSyphonView(zividomelive.ViewType.STANDARD);

		outputManager.setView(zividomelive.ViewType.FISHEYE_DOMEMASTER);

		assertEquals(zividomelive.ViewType.EQUIRECTANGULAR,
				outputManager.getViewForOutput(OutputManager.OutputType.NDI));
		assertEquals(zividomelive.ViewType.CUBEMAP,
				outputManager.getViewForOutput(OutputManager.OutputType.SPOUT));
		assertEquals(zividomelive.ViewType.STANDARD,
				outputManager.getViewForOutput(OutputManager.OutputType.SYPHON));
	}
}
