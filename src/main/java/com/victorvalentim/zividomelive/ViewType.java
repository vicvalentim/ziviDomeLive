package com.victorvalentim.zividomelive;

/**
 * Selects the final view used by the preview and independent output routes in
 * {@link RenderMode#FULL}.
 *
 * <p>Dedicated render modes temporarily override configured view selections without
 * mutating them.</p>
 *
 * @since 2.0.0
 */
public enum ViewType {
	/** Conventional perspective view, independent from spherical capture. */
	STANDARD,
	/** Circular fulldome projection. */
	DOMEMASTER,
	/** Equirectangular spherical projection. */
	EQUIRECTANGULAR,
	/** Cubemap skybox view. */
	SKYBOX
}
