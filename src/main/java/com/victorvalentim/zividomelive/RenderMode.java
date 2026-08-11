package com.victorvalentim.zividomelive;

/**
 * Selects the global rendering behavior of a ziviDomeLive instance.
 *
 * <p>{@link #FULL} preserves the independent preview and external-output choices configured
 * through the legacy {@link zividomelive.ViewType} API. Dedicated modes temporarily override
 * those effective choices without erasing them.</p>
 */
public enum RenderMode {
	/** Preserve independent preview and external-output view selection. */
	FULL,
	/** Render the perspective Standard representation. */
	STANDARD,
	/** Render the fisheye domemaster representation. */
	DOMEMASTER,
	/** Render the equirectangular representation. */
	EQUIRECTANGULAR,
	/** Render the cubemap skybox representation. */
	SKYBOX
}
