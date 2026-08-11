package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderRequirementsPolicyTest {

	@Test
	void previewRequirementsPreserveEveryLegacyView() {
		assertRequirements(
				RenderRequirementsPolicy.forPreview(zividomelive.ViewType.FISHEYE_DOMEMASTER, false),
				true, true, false, false, true);
		assertRequirements(
				RenderRequirementsPolicy.forPreview(zividomelive.ViewType.EQUIRECTANGULAR, false),
				false, true, false, false, true);
		assertRequirements(
				RenderRequirementsPolicy.forPreview(zividomelive.ViewType.CUBEMAP, false),
				false, false, true, false, true);
		assertRequirements(
				RenderRequirementsPolicy.forPreview(zividomelive.ViewType.STANDARD, false),
				false, false, false, true, false);
	}

	@Test
	void floatingPreviewAddsFisheyeChainWithoutDroppingSelectedView() {
		RenderRequirementsPolicy.Requirements standard = RenderRequirementsPolicy.forPreview(
				zividomelive.ViewType.STANDARD, true);
		assertRequirements(standard, true, true, false, true, true);

		RenderRequirementsPolicy.Requirements cubemap = RenderRequirementsPolicy.forPreview(
				zividomelive.ViewType.CUBEMAP, true);
		assertRequirements(cubemap, true, true, true, false, true);
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
					equirectangular || fisheye,
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
