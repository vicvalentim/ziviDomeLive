package com.victorvalentim.zividomelive;

/**
 * Selects the final view used by the preview and independent output routes in
 * {@link RenderMode#FULL}.
 *
 * <p>Dedicated render modes temporarily override configured view selections without
 * mutating them.</p>
 *
 * <p>The declaration order is part of the 2.x contract because the built-in Processing UI maps
 * view choices by ordinal.</p>
 *
 * <p><strong>API stability:</strong> Stable.</p>
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
