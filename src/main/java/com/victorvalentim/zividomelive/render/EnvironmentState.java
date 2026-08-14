package com.victorvalentim.zividomelive.render;

import processing.core.PImage;

/**
 * Shared logical state for the visual LDR environment background.
 *
 * <p>The {@link PImage} is a borrowed Processing-friendly source. Render passes resolve its
 * Processing-managed OpenGL texture when needed and never take ownership of, or dispose, that
 * texture. This state deliberately contains no lighting, exposure, HDR, IBL, PBR, or AO data.</p>
 *
 * @since 2.0.0
 */
public final class EnvironmentState {
	private PImage ldrEquirectangularSource;
	private boolean visible = true;
	private float intensity = 1.0f;
	private float yawOffset;

	/**
	 * Sets the borrowed LDR equirectangular source, or {@code null} to clear it.
	 * @param source borrowed source, or {@code null}
	 */
	public void setLdrEquirectangularSource(PImage source) {
		ldrEquirectangularSource = source;
	}

	/**
	 * Returns the borrowed LDR equirectangular source, or {@code null}.
	 * @return borrowed source, or {@code null}
	 */
	public PImage getLdrEquirectangularSource() {
		return ldrEquirectangularSource;
	}

	/** Clears the borrowed source reference without disposing Processing resources. */
	public void clearSource() {
		ldrEquirectangularSource = null;
	}

	/**
	 * Returns whether a source is configured.
	 * @return {@code true} when a source is configured
	 */
	public boolean hasSource() {
		return ldrEquirectangularSource != null;
	}

	/**
	 * Sets whether the visual background should be rendered.
	 * @param visible {@code true} to render the visual background
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	/**
	 * Returns whether the visual background is enabled.
	 * @return {@code true} when enabled
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * Sets the non-negative visual colour multiplier.
	 * @param intensity visual colour multiplier
	 */
	public void setIntensity(float intensity) {
		this.intensity = Math.max(0.0f, intensity);
	}

	/**
	 * Returns the non-negative visual colour multiplier.
	 * @return visual colour multiplier
	 */
	public float getIntensity() {
		return intensity;
	}

	/**
	 * Sets the source-longitude offset in radians.
	 * @param yawOffset source-longitude offset in radians
	 */
	public void setYawOffset(float yawOffset) {
		this.yawOffset = yawOffset;
	}

	/**
	 * Returns the source-longitude offset in radians.
	 * @return source-longitude offset in radians
	 */
	public float getYawOffset() {
		return yawOffset;
	}
}
