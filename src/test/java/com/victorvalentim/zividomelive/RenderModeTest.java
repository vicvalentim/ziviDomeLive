package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.Test;
import processing.core.PApplet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderModeTest {

	@Test
	void fullIsTheDefaultAndNullIsIgnored() {
		zividomelive dome = new zividomelive(new PApplet());

		assertEquals(RenderMode.FULL, dome.getRenderMode());
		dome.setRenderMode(null);
		assertEquals(RenderMode.FULL, dome.getRenderMode());
	}

	@Test
	void dedicatedModesMapToTheirLegacyRepresentations() {
		assertEquals(
				zividomelive.ViewType.STANDARD,
				RenderRequirementsPolicy.resolveView(
						RenderMode.STANDARD, zividomelive.ViewType.FISHEYE_DOMEMASTER));
		assertEquals(
				zividomelive.ViewType.FISHEYE_DOMEMASTER,
				RenderRequirementsPolicy.resolveView(
						RenderMode.DOMEMASTER, zividomelive.ViewType.STANDARD));
		assertEquals(
				zividomelive.ViewType.EQUIRECTANGULAR,
				RenderRequirementsPolicy.resolveView(
						RenderMode.EQUIRECTANGULAR, zividomelive.ViewType.CUBEMAP));
		assertEquals(
				zividomelive.ViewType.CUBEMAP,
				RenderRequirementsPolicy.resolveView(
						RenderMode.SKYBOX, zividomelive.ViewType.STANDARD));
	}

	@Test
	void fullPreservesIndependentLegacySelection() {
		for (zividomelive.ViewType view : zividomelive.ViewType.values()) {
			assertEquals(view, RenderRequirementsPolicy.resolveView(RenderMode.FULL, view));
		}
	}

	@Test
	void returningToFullRestoresConfiguredPreviewView() {
		zividomelive dome = new zividomelive(new PApplet());
		dome.setCurrentView(zividomelive.ViewType.CUBEMAP);

		dome.setRenderMode(RenderMode.STANDARD);
		assertEquals(zividomelive.ViewType.CUBEMAP, dome.getCurrentView());

		dome.setRenderMode(RenderMode.FULL);
		assertEquals(zividomelive.ViewType.CUBEMAP, dome.getCurrentView());
	}

	@Test
	void floatingDomemasterRemainsAvailableInDedicatedStandardMode() {
		RenderRequirementsPolicy.Requirements requirements = RenderRequirementsPolicy.forPreview(
				RenderMode.STANDARD, zividomelive.ViewType.CUBEMAP, true);

		assertTrue(requirements.needsStandard());
		assertTrue(requirements.needsFisheye());
		assertTrue(requirements.needsEquirectangular());
		assertTrue(requirements.needsCubemapSource());
	}
}
