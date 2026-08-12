package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.Test;
import processing.core.PApplet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderModeTest {

	@Test
	void fullIsTheDefaultAndNullIsIgnored() {
		ziviDomeLive dome = new ziviDomeLive(new PApplet());

		assertEquals(RenderMode.FULL, dome.getRenderMode());
		assertEquals(ViewType.DOMEMASTER, dome.getCurrentView());
		dome.setRenderMode(null);
		assertEquals(RenderMode.FULL, dome.getRenderMode());
	}

	@Test
	void dedicatedModesMapToTheirViewTypes() {
		assertEquals(
				ViewType.STANDARD,
				RenderRequirementsPolicy.resolveView(
						RenderMode.STANDARD, ViewType.DOMEMASTER));
		assertEquals(
				ViewType.DOMEMASTER,
				RenderRequirementsPolicy.resolveView(
						RenderMode.DOMEMASTER, ViewType.STANDARD));
		assertEquals(
				ViewType.EQUIRECTANGULAR,
				RenderRequirementsPolicy.resolveView(
						RenderMode.EQUIRECTANGULAR, ViewType.SKYBOX));
		assertEquals(
				ViewType.SKYBOX,
				RenderRequirementsPolicy.resolveView(
						RenderMode.SKYBOX, ViewType.STANDARD));
	}

	@Test
	void fullPreservesIndependentViewSelection() {
		for (ViewType view : ViewType.values()) {
			assertEquals(view, RenderRequirementsPolicy.resolveView(RenderMode.FULL, view));
		}
	}

	@Test
	void returningToFullRestoresConfiguredPreviewView() {
		ziviDomeLive dome = new ziviDomeLive(new PApplet());
		dome.setCurrentView(ViewType.SKYBOX);

		dome.setRenderMode(RenderMode.STANDARD);
		assertEquals(ViewType.SKYBOX, dome.getCurrentView());

		dome.setRenderMode(RenderMode.FULL);
		assertEquals(ViewType.SKYBOX, dome.getCurrentView());
	}

	@Test
	void floatingDomemasterRemainsAvailableInDedicatedStandardMode() {
		RenderRequirementsPolicy.Requirements requirements = RenderRequirementsPolicy.forPreview(
				RenderMode.STANDARD, ViewType.SKYBOX, true);

		assertTrue(requirements.needsStandard());
		assertTrue(requirements.needsFisheye());
		assertFalse(requirements.needsEquirectangular());
		assertTrue(requirements.needsCubemapSource());
	}
}
