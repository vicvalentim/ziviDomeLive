package com.victorvalentim.zividomelive;

// Package-private implementation grouped physically under _internal/render/core.

import com.victorvalentim.zividomelive.render.Quaternion;
import processing.core.PImage;

/**
 * Shared logical state for the visual LDR environment background.
 *
 * <p>The {@link PImage} is a borrowed Processing-friendly source. Render passes resolve its
 * Processing-managed OpenGL texture when needed and never take ownership of, or dispose, that
 * texture. The shared scene-camera quaternion is visual orientation state only; orbit target
 * and distance are deliberately excluded. This state contains no lighting, exposure, HDR,
 * IBL, PBR, or AO data.</p>
 *
 * @since 2.0.0
 */
final class EnvironmentState {
	private PImage ldrEquirectangularSource;
	private boolean visible = true;
	private float intensity = 1.0f;
	private float yawOffset;
	private Quaternion sourceOrientation = new Quaternion(0.0f, 0.0f, 0.0f, 1.0f);
	private Quaternion sceneCameraOrientation = new Quaternion(0.0f, 0.0f, 0.0f, 1.0f);

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
	 * @param intensity visual colour multiplier; non-finite values are ignored
	 */
	public void setIntensity(float intensity) {
		if (!Float.isFinite(intensity)) {
			return;
		}
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
	 * @param yawOffset source-longitude offset in radians; non-finite values are ignored
	 */
	public void setYawOffset(float yawOffset) {
		if (!Float.isFinite(yawOffset)) {
			return;
		}
		this.yawOffset = yawOffset;
	}

	/**
	 * Returns the source-longitude offset in radians.
	 * @return source-longitude offset in radians
	 */
	public float getYawOffset() {
		return yawOffset;
	}

	/**
	 * Sets the fixed rotational alignment applied directly to source-image lookup directions.
	 * @param orientation source orientation, or {@code null} for identity
	 */
	public void setSourceOrientation(Quaternion orientation) {
		if (orientation == null) {
			sourceOrientation = new Quaternion(0.0f, 0.0f, 0.0f, 1.0f);
			return;
		}
		if (orientation == sourceOrientation) {
			return;
		}
		sourceOrientation = orientation.normalized();
	}

	/**
	 * Returns the fixed source-image orientation used by Environment lookup.
	 * @return current unit orientation
	 */
	public Quaternion getSourceOrientation() {
		return sourceOrientation;
	}

	/**
	 * Sets the rotational component of the shared scene camera used by the Environment lookup.
	 * Target and distance deliberately remain outside the Environment state so the background
	 * stays infinite and translation-invariant.
	 *
	 * @param orientation scene-camera orientation, or {@code null} for identity
	 */
	public void setSceneCameraOrientation(Quaternion orientation) {
		if (orientation == null) {
			sceneCameraOrientation = new Quaternion(0.0f, 0.0f, 0.0f, 1.0f);
			return;
		}
		if (orientation == sceneCameraOrientation) {
			return;
		}
		sceneCameraOrientation = orientation.normalized();
	}

	/**
	 * Returns the immutable scene-camera orientation used by the Environment lookup.
	 *
	 * @return current unit orientation quaternion
	 */
	public Quaternion getSceneCameraOrientation() {
		return sceneCameraOrientation;
	}
}
