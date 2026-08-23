package com.victorvalentim.zividomelive;

import processing.opengl.PGraphicsOpenGL;

/**
 * Provides completed final-frame graphics by logical view.
 *
 * <p>The render pipeline owns production of these views. Output publishers consume this
 * contract without knowing which renderer or projection chain produced each target.</p>
 *
 * @since 2.0.0
 */
@FunctionalInterface
interface FrameViews {

	/**
	 * Returns the completed graphics target for a logical view.
	 *
	 * @param view requested final view
	 * @return completed target, or {@code null} when that view is unavailable
	 */
	PGraphicsOpenGL getFrame(ViewType view);
}
