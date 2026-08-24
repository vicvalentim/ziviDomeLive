package com.victorvalentim.zividomelive;

// Package-private implementation test grouped physically under _internal/render/core.

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderRequirementsPolicyTest {

	@Test
	void previewRequirementsPreserveEveryView() {
		assertRequirements(
				RenderRequirementsPolicy.forPreview(
						RenderMode.FULL, ViewType.DOMEMASTER, false),
				true, false, false, false, true);
		assertRequirements(
				RenderRequirementsPolicy.forPreview(
						RenderMode.FULL, ViewType.EQUIRECTANGULAR, false),
				false, true, false, false, true);
		assertRequirements(
				RenderRequirementsPolicy.forPreview(
						RenderMode.FULL, ViewType.SKYBOX, false),
				false, false, true, false, true);
		assertRequirements(
				RenderRequirementsPolicy.forPreview(
						RenderMode.FULL, ViewType.STANDARD, false),
				false, false, false, true, false);
	}

	@Test
	void floatingPreviewAddsFisheyeChainWithoutDroppingSelectedView() {
		RenderRequirementsPolicy.Requirements standard = RenderRequirementsPolicy.forPreview(
				RenderMode.FULL, ViewType.STANDARD, true);
		assertRequirements(standard, true, false, false, true, true);

		RenderRequirementsPolicy.Requirements cubemap = RenderRequirementsPolicy.forPreview(
				RenderMode.FULL, ViewType.SKYBOX, true);
		assertRequirements(cubemap, true, false, true, false, true);
	}

	@Test
	void enabledOutputsUseUnionOfIndependentRoutes() {
		RenderRequirementsPolicy.Requirements requirements = RenderRequirementsPolicy.forOutputs(
				true, false, true, true, true);

		assertRequirements(requirements, false, true, true, true, true);
	}

	@Test
	void inactiveOutputsNeverRequestRendering() {
		RenderRequirementsPolicy.Requirements requirements = RenderRequirementsPolicy.forOutputs(
				false, true, true, true, true);

		assertRequirements(requirements, false, false, false, false, false);
	}

	@Test
	void dependencyClosureHoldsForEveryRequestedViewCombination() {
		for (int mask = 0; mask < 16; mask++) {
			boolean fisheye = (mask & 1) != 0;
			boolean equirectangular = (mask & 2) != 0;
			boolean cubemap = (mask & 4) != 0;
			boolean standard = (mask & 8) != 0;
			RenderRequirementsPolicy.Requirements requirements = RenderRequirementsPolicy.forOutputs(
					true, fisheye, equirectangular, cubemap, standard);

			assertRequirements(
					requirements,
					fisheye,
					equirectangular,
					cubemap,
					standard,
					fisheye || equirectangular || cubemap);
		}
	}

	private static void assertRequirements(
			RenderRequirementsPolicy.Requirements requirements,
			boolean fisheye,
			boolean equirectangular,
			boolean cubemapLayout,
			boolean standard,
			boolean cubemapSource) {
		assertAll(
				() -> assertEquals(fisheye, requirements.needsFisheye()),
				() -> assertEquals(equirectangular, requirements.needsEquirectangular()),
				() -> assertEquals(cubemapLayout, requirements.needsCubemapLayout()),
				() -> assertEquals(standard, requirements.needsStandard()),
				() -> assertEquals(cubemapSource, requirements.needsCubemapSource()));
	}
}
