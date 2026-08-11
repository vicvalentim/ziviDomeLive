package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.RenderMode;
import com.victorvalentim.zividomelive.zividomelive;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderModeOutputRoutingTest {

	@Test
	void everyDedicatedModeOverridesEffectiveRouteWithoutErasingConfiguration() {
		zividomelive dome = new zividomelive(new PApplet());
		OutputManager outputs = new OutputManager(dome);
		outputs.setNdiView(zividomelive.ViewType.EQUIRECTANGULAR);

		RenderMode[] modes = {
				RenderMode.STANDARD,
				RenderMode.DOMEMASTER,
				RenderMode.EQUIRECTANGULAR,
				RenderMode.SKYBOX
		};
		zividomelive.ViewType[] expectedViews = {
				zividomelive.ViewType.STANDARD,
				zividomelive.ViewType.FISHEYE_DOMEMASTER,
				zividomelive.ViewType.EQUIRECTANGULAR,
				zividomelive.ViewType.CUBEMAP
		};

		for (int index = 0; index < modes.length; index++) {
			dome.setRenderMode(modes[index]);
			assertEquals(expectedViews[index],
					outputs.resolveOutputView(
							outputs.getViewForOutput(OutputManager.OutputType.NDI)));
			assertEquals(zividomelive.ViewType.EQUIRECTANGULAR,
					outputs.getViewForOutput(OutputManager.OutputType.NDI));
		}

		dome.setRenderMode(RenderMode.FULL);
		assertEquals(zividomelive.ViewType.EQUIRECTANGULAR,
				outputs.resolveOutputView(outputs.getViewForOutput(OutputManager.OutputType.NDI)));
	}

	@Test
	void renderRequirementsFollowEffectiveDedicatedRoute() {
		zividomelive dome = new zividomelive(new PApplet());
		EnabledNdiOutputManager outputs = new EnabledNdiOutputManager(dome);
		outputs.setNdiView(zividomelive.ViewType.STANDARD);

		dome.setRenderMode(RenderMode.DOMEMASTER);

		assertTrue(outputs.requiresView(zividomelive.ViewType.FISHEYE_DOMEMASTER));
		assertFalse(outputs.requiresView(zividomelive.ViewType.STANDARD));
	}

	private static final class EnabledNdiOutputManager extends OutputManager {
		private EnabledNdiOutputManager(zividomelive parent) {
			super(parent);
		}

		@Override
		public boolean isNdiEnabled() {
			return true;
		}
	}
}
