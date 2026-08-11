package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.ziviDomeLive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;

import static org.junit.jupiter.api.Assertions.*;

class OutputRoutingCompatibilityTest {

	private OutputManager outputManager;

	@BeforeEach
	void createOutputManager() {
		outputManager = new OutputManager(new ziviDomeLive(new PApplet()));
	}

	@Test
	void everyOutputDefaultsToFisheyeDomemaster() {
		for (OutputManager.OutputType outputType : OutputManager.OutputType.values()) {
			assertEquals(ziviDomeLive.ViewType.FISHEYE_DOMEMASTER,
					outputManager.getViewForOutput(outputType));
		}
	}

	@Test
	void eachOutputCanSelectAViewIndependently() {
		outputManager.setNdiView(ziviDomeLive.ViewType.EQUIRECTANGULAR);
		outputManager.setSpoutView(ziviDomeLive.ViewType.CUBEMAP);
		outputManager.setSyphonView(ziviDomeLive.ViewType.STANDARD);

		assertEquals(ziviDomeLive.ViewType.EQUIRECTANGULAR,
				outputManager.getViewForOutput(OutputManager.OutputType.NDI));
		assertEquals(ziviDomeLive.ViewType.CUBEMAP,
				outputManager.getViewForOutput(OutputManager.OutputType.SPOUT));
		assertEquals(ziviDomeLive.ViewType.STANDARD,
				outputManager.getViewForOutput(OutputManager.OutputType.SYPHON));
	}

	@Test
	void initializedButDisabledOutputsDoNotCreateRenderRequirements() {
		for (ziviDomeLive.ViewType viewType : ziviDomeLive.ViewType.values()) {
			assertFalse(outputManager.requiresView(viewType),
					"Disabled outputs must not require rendering for " + viewType);
		}
		assertFalse(outputManager.isActive());
	}

	@Test
	@SuppressWarnings("deprecation")
	void legacySingleViewSetterDoesNotMutatePerOutputRouting() {
		outputManager.setNdiView(ziviDomeLive.ViewType.EQUIRECTANGULAR);
		outputManager.setSpoutView(ziviDomeLive.ViewType.CUBEMAP);
		outputManager.setSyphonView(ziviDomeLive.ViewType.STANDARD);

		outputManager.setView(ziviDomeLive.ViewType.FISHEYE_DOMEMASTER);

		assertEquals(ziviDomeLive.ViewType.EQUIRECTANGULAR,
				outputManager.getViewForOutput(OutputManager.OutputType.NDI));
		assertEquals(ziviDomeLive.ViewType.CUBEMAP,
				outputManager.getViewForOutput(OutputManager.OutputType.SPOUT));
		assertEquals(ziviDomeLive.ViewType.STANDARD,
				outputManager.getViewForOutput(OutputManager.OutputType.SYPHON));
	}
}
