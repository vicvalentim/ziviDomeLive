package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.ViewType;
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
			assertEquals(ViewType.DOMEMASTER,
					outputManager.getViewForOutput(outputType));
		}
	}

	@Test
	void eachOutputCanSelectAViewIndependently() {
		outputManager.setNdiView(ViewType.EQUIRECTANGULAR);
		outputManager.setSpoutView(ViewType.SKYBOX);
		outputManager.setSyphonView(ViewType.STANDARD);

		assertEquals(ViewType.EQUIRECTANGULAR,
				outputManager.getViewForOutput(OutputManager.OutputType.NDI));
		assertEquals(ViewType.SKYBOX,
				outputManager.getViewForOutput(OutputManager.OutputType.SPOUT));
		assertEquals(ViewType.STANDARD,
				outputManager.getViewForOutput(OutputManager.OutputType.SYPHON));
	}

	@Test
	void initializedButDisabledOutputsDoNotCreateRenderRequirements() {
		for (ViewType viewType : ViewType.values()) {
			assertFalse(outputManager.requiresView(viewType),
					"Disabled outputs must not require rendering for " + viewType);
		}
		assertFalse(outputManager.isActive());
	}

	@Test
	@SuppressWarnings("deprecation")
	void deprecatedSingleViewSetterDoesNotMutatePerOutputRouting() {
		outputManager.setNdiView(ViewType.EQUIRECTANGULAR);
		outputManager.setSpoutView(ViewType.SKYBOX);
		outputManager.setSyphonView(ViewType.STANDARD);

		outputManager.setView(ViewType.DOMEMASTER);

		assertEquals(ViewType.EQUIRECTANGULAR,
				outputManager.getViewForOutput(OutputManager.OutputType.NDI));
		assertEquals(ViewType.SKYBOX,
				outputManager.getViewForOutput(OutputManager.OutputType.SPOUT));
		assertEquals(ViewType.STANDARD,
				outputManager.getViewForOutput(OutputManager.OutputType.SYPHON));
	}
}
