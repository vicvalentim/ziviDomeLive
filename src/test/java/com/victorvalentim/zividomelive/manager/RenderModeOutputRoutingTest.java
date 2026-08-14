package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.RenderMode;
import com.victorvalentim.zividomelive.ViewType;
import com.victorvalentim.zividomelive.ziviDomeLive;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderModeOutputRoutingTest {

	@Test
	void everyDedicatedModeOverridesEffectiveRouteWithoutErasingConfiguration() {
		ziviDomeLive dome = new ziviDomeLive(new PApplet());
		OutputManager outputs = new OutputManager(dome);
		outputs.setNdiView(ViewType.EQUIRECTANGULAR);

		RenderMode[] modes = {
				RenderMode.STANDARD,
				RenderMode.DOMEMASTER,
				RenderMode.EQUIRECTANGULAR,
				RenderMode.SKYBOX
		};
		ViewType[] expectedViews = {
				ViewType.STANDARD,
				ViewType.DOMEMASTER,
				ViewType.EQUIRECTANGULAR,
				ViewType.SKYBOX
		};

		for (int index = 0; index < modes.length; index++) {
			dome.setRenderMode(modes[index]);
			assertEquals(expectedViews[index],
					outputs.resolveOutputView(
							outputs.getViewForOutput(OutputManager.OutputType.NDI)));
			assertEquals(ViewType.EQUIRECTANGULAR,
					outputs.getViewForOutput(OutputManager.OutputType.NDI));
		}

		dome.setRenderMode(RenderMode.FULL);
		assertEquals(ViewType.EQUIRECTANGULAR,
				outputs.resolveOutputView(outputs.getViewForOutput(OutputManager.OutputType.NDI)));
	}

	@Test
	void renderRequirementsFollowEffectiveDedicatedRoute() {
		ziviDomeLive dome = new ziviDomeLive(new PApplet());
		EnabledNdiOutputManager outputs = new EnabledNdiOutputManager(dome);
		outputs.setNdiView(ViewType.STANDARD);

		dome.setRenderMode(RenderMode.DOMEMASTER);

		assertTrue(outputs.requiresView(ViewType.DOMEMASTER));
		assertFalse(outputs.requiresView(ViewType.STANDARD));
	}

	private static final class EnabledNdiOutputManager extends OutputManager {
		private EnabledNdiOutputManager(ziviDomeLive parent) {
			super(parent);
		}

		@Override
		public boolean isNdiEnabled() {
			return true;
		}
	}
}
