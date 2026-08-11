package com.victorvalentim.zividomelive;

/**
 * Internal policy that expands requested views into the render passes needed to produce them.
 *
 * <p>The policy is deliberately independent from renderer instances and output backends. This
 * keeps view selection separate from the 1.x rendering topology while providing one place for
 * dependency closure. Results are cached because the policy runs once per frame.</p>
 */
final class RenderRequirementsPolicy {

	private static final int FISHEYE = 1;
	private static final int EQUIRECTANGULAR = 1 << 1;
	private static final int CUBEMAP_LAYOUT = 1 << 2;
	private static final int STANDARD = 1 << 3;
	private static final int COMBINATION_COUNT = 1 << 4;
	private static final Requirements[] REQUIREMENTS = createRequirements();

	private RenderRequirementsPolicy() {
	}

	/**
	 * Resolves the passes needed by the application preview.
	 *
	 * @param renderMode active global render mode
	 * @param selectedView view composited into the Processing window
	 * @param floatingFisheye whether the floating domemaster preview is visible
	 * @return cached render requirements
	 */
	static Requirements forPreview(
			RenderMode renderMode,
			zividomelive.ViewType selectedView,
			boolean floatingFisheye) {
		int requestedViews = maskFor(resolveView(renderMode, selectedView));
		if (floatingFisheye) {
			requestedViews |= FISHEYE;
		}
		return REQUIREMENTS[requestedViews];
	}

	/**
	 * Resolves a configured legacy view under the active global render mode.
	 *
	 * @param renderMode active global mode
	 * @param configuredView independently configured legacy view
	 * @return configured view in FULL, otherwise the dedicated mode's representation
	 */
	static zividomelive.ViewType resolveView(
			RenderMode renderMode,
			zividomelive.ViewType configuredView) {
		if (renderMode == null || renderMode == RenderMode.FULL) {
			return configuredView;
		}

		switch (renderMode) {
			case STANDARD:
				return zividomelive.ViewType.STANDARD;
			case DOMEMASTER:
				return zividomelive.ViewType.FISHEYE_DOMEMASTER;
			case EQUIRECTANGULAR:
				return zividomelive.ViewType.EQUIRECTANGULAR;
			case SKYBOX:
				return zividomelive.ViewType.CUBEMAP;
			case FULL:
			default:
				return configuredView;
		}
	}

	/**
	 * Resolves the union of views requested by enabled external outputs.
	 *
	 * @param outputsActive whether at least one external output is enabled
	 * @param fisheyeRequested whether an enabled output requests fisheye
	 * @param equirectangularRequested whether an enabled output requests equirectangular
	 * @param cubemapRequested whether an enabled output requests the cubemap layout
	 * @param standardRequested whether an enabled output requests Standard
	 * @return cached render requirements
	 */
	static Requirements forOutputs(
			boolean outputsActive,
			boolean fisheyeRequested,
			boolean equirectangularRequested,
			boolean cubemapRequested,
			boolean standardRequested) {
		if (!outputsActive) {
			return REQUIREMENTS[0];
		}

		int requestedViews = 0;
		if (fisheyeRequested) {
			requestedViews |= FISHEYE;
		}
		if (equirectangularRequested) {
			requestedViews |= EQUIRECTANGULAR;
		}
		if (cubemapRequested) {
			requestedViews |= CUBEMAP_LAYOUT;
		}
		if (standardRequested) {
			requestedViews |= STANDARD;
		}
		return REQUIREMENTS[requestedViews];
	}

	private static int maskFor(zividomelive.ViewType view) {
		if (view == null) {
			return 0;
		}

		switch (view) {
			case FISHEYE_DOMEMASTER:
				return FISHEYE;
			case EQUIRECTANGULAR:
				return EQUIRECTANGULAR;
			case CUBEMAP:
				return CUBEMAP_LAYOUT;
			case STANDARD:
				return STANDARD;
			default:
				return 0;
		}
	}

	private static Requirements[] createRequirements() {
		Requirements[] requirements = new Requirements[COMBINATION_COUNT];
		for (int requestedViews = 0; requestedViews < requirements.length; requestedViews++) {
			requirements[requestedViews] = new Requirements(requestedViews);
		}
		return requirements;
	}

	/** Immutable dependency closure for one combination of requested views. */
	static final class Requirements {
		private final boolean needsFisheye;
		private final boolean needsEquirectangular;
		private final boolean needsCubemapLayout;
		private final boolean needsStandard;
		private final boolean needsCubemapSource;

		private Requirements(int requestedViews) {
			needsFisheye = (requestedViews & FISHEYE) != 0;
			needsEquirectangular = (requestedViews & EQUIRECTANGULAR) != 0 || needsFisheye;
			needsCubemapLayout = (requestedViews & CUBEMAP_LAYOUT) != 0;
			needsStandard = (requestedViews & STANDARD) != 0;
			needsCubemapSource = needsEquirectangular || needsCubemapLayout;
		}

		boolean needsFisheye() {
			return needsFisheye;
		}

		boolean needsEquirectangular() {
			return needsEquirectangular;
		}

		boolean needsCubemapLayout() {
			return needsCubemapLayout;
		}

		boolean needsStandard() {
			return needsStandard;
		}

		boolean needsCubemapSource() {
			return needsCubemapSource;
		}
	}
}
