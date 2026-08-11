package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.Test;
import processing.core.PApplet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderModeTest {

	@Test
	void fullIsTheDefaultAndNullIsIgnored() {
		ziviDomeLive dome = new ziviDomeLive(new PApplet());

		assertEquals(RenderMode.FULL, dome.getRenderMode());
		dome.setRenderMode(null);
		assertEquals(RenderMode.FULL, dome.getRenderMode());
	}

	@Test
	void dedicatedModesMapToTheirLegacyRepresentations() {
		assertEquals(
				ziviDomeLive.ViewType.STANDARD,
				RenderRequirementsPolicy.resolveView(
						RenderMode.STANDARD, ziviDomeLive.ViewType.FISHEYE_DOMEMASTER));
		assertEquals(
				ziviDomeLive.ViewType.FISHEYE_DOMEMASTER,
				RenderRequirementsPolicy.resolveView(
						RenderMode.DOMEMASTER, ziviDomeLive.ViewType.STANDARD));
		assertEquals(
				ziviDomeLive.ViewType.EQUIRECTANGULAR,
				RenderRequirementsPolicy.resolveView(
						RenderMode.EQUIRECTANGULAR, ziviDomeLive.ViewType.CUBEMAP));
		assertEquals(
				ziviDomeLive.ViewType.CUBEMAP,
				RenderRequirementsPolicy.resolveView(
						RenderMode.SKYBOX, ziviDomeLive.ViewType.STANDARD));
	}

	@Test
	void fullPreservesIndependentLegacySelection() {
		for (ziviDomeLive.ViewType view : ziviDomeLive.ViewType.values()) {
			assertEquals(view, RenderRequirementsPolicy.resolveView(RenderMode.FULL, view));
		}
	}

	@Test
	void returningToFullRestoresConfiguredPreviewView() {
		ziviDomeLive dome = new ziviDomeLive(new PApplet());
		dome.setCurrentView(ziviDomeLive.ViewType.CUBEMAP);

		dome.setRenderMode(RenderMode.STANDARD);
		assertEquals(ziviDomeLive.ViewType.CUBEMAP, dome.getCurrentView());

		dome.setRenderMode(RenderMode.FULL);
		assertEquals(ziviDomeLive.ViewType.CUBEMAP, dome.getCurrentView());
	}

	@Test
	void floatingDomemasterRemainsAvailableInDedicatedStandardMode() {
		RenderRequirementsPolicy.Requirements requirements = RenderRequirementsPolicy.forPreview(
				RenderMode.STANDARD, ziviDomeLive.ViewType.CUBEMAP, true);

		assertTrue(requirements.needsStandard());
		assertTrue(requirements.needsFisheye());
		assertTrue(requirements.needsEquirectangular());
		assertTrue(requirements.needsCubemapSource());
	}
}
