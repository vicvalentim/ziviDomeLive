package com.victorvalentim.zividomelive.render;

import com.victorvalentim.zividomelive.render.camera.CubemapFace;
import com.victorvalentim.zividomelive.support.LogManager;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PMatrix3D;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Draws a library-owned equirectangular environment behind spherical scene capture.
 *
 * <p>This is intentionally a background service, not scene geometry. The renderer draws after
 * {@code Scene.sceneRender(PGraphicsOpenGL)} at far-plane depth, so scene-owned
 * {@code background()} calls cannot erase it and foreground geometry remains in front.</p>
 */
public final class EnvironmentBackgroundRenderer implements PConstants {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final String EQUIRECTANGULAR_BACKGROUND_VERT =
			"data/shaders/environment/equirectangular_background.vert";
	private static final String EQUIRECTANGULAR_BACKGROUND_FRAG =
			"data/shaders/environment/equirectangular_background.frag";

	private final PShader equirectangularShader;
	private PImage equirectangularImage;
	private boolean visible = true;
	private float intensity = 1.0f;
	private float yawOffset = 0.0f;
	private boolean unavailableWarningLogged;
	private boolean renderFailureWarningLogged;

	/**
	 * Creates a background renderer and loads its packaged shader resources.
	 *
	 * @param parent Processing parent used for shader loading
	 */
	public EnvironmentBackgroundRenderer(PApplet parent) {
		Objects.requireNonNull(parent, "parent cannot be null");
		PShader shader = null;
		try {
			shader = parent.loadShader(EQUIRECTANGULAR_BACKGROUND_FRAG, EQUIRECTANGULAR_BACKGROUND_VERT);
		} catch (RuntimeException error) {
			LOGGER.warning("Environment background shader unavailable: " + error.getMessage());
			unavailableWarningLogged = true;
		}
		this.equirectangularShader = shader;
	}

	/**
	 * Sets the LDR equirectangular image used as the environment background.
	 *
	 * @param image Processing image, or {@code null} to clear the background
	 */
	public void setEquirectangularImage(PImage image) {
		this.equirectangularImage = image;
		unavailableWarningLogged = false;
		renderFailureWarningLogged = false;
	}

	/** Clears the current environment image. */
	public void clear() {
		setEquirectangularImage(null);
	}

	/**
	 * Reports whether an environment image is currently configured.
	 *
	 * @return {@code true} when an image is available
	 */
	public boolean hasEquirectangularImage() {
		return equirectangularImage != null;
	}

	/**
	 * Shows or hides the environment background without releasing the image.
	 *
	 * @param visible {@code true} to draw the background
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	/**
	 * Reports whether the environment background is visible.
	 *
	 * @return {@code true} when visible
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * Sets the linear multiplier applied to sampled background colour.
	 *
	 * @param intensity non-negative colour multiplier
	 */
	public void setIntensity(float intensity) {
		this.intensity = Math.max(0.0f, intensity);
	}

	/**
	 * Returns the current environment colour multiplier.
	 *
	 * @return non-negative intensity
	 */
	public float getIntensity() {
		return intensity;
	}

	/**
	 * Rotates the equirectangular environment around the vertical axis.
	 *
	 * @param yawOffset radians added to the source longitude lookup
	 */
	public void setYawOffset(float yawOffset) {
		this.yawOffset = yawOffset;
	}

	/**
	 * Returns the current environment yaw offset.
	 *
	 * @return yaw offset in radians
	 */
	public float getYawOffset() {
		return yawOffset;
	}

	/**
	 * Draws the configured equirectangular background into one native cubemap face.
	 *
	 * @param target active Processing OpenGL command target
	 * @param face target cubemap face
	 * @param sphericalOrientation orientation shared by the spherical capture
	 * @return {@code true} when a background pass was drawn
	 */
	public boolean renderCubemapFace(
			PGraphicsOpenGL target,
			CubemapFace face,
			Quaternion sphericalOrientation) {
		if (!visible || equirectangularImage == null) {
			return false;
		}
		if (target == null || face == null || equirectangularShader == null) {
			if (!unavailableWarningLogged) {
				LOGGER.warning("Environment background unavailable; cubemap face background skipped.");
				unavailableWarningLogged = true;
			}
			return false;
		}

		try {
			PMatrix3D orientationMatrix = sphericalOrientation == null
					? new PMatrix3D()
					: sphericalOrientation.toMatrix();
			renderFullscreenBackground(target, face, orientationMatrix);
			renderFailureWarningLogged = false;
			unavailableWarningLogged = false;
			return true;
		} catch (RuntimeException error) {
			if (!renderFailureWarningLogged) {
				LOGGER.warning("Environment background render failed; background skipped: "
						+ error.getMessage());
				renderFailureWarningLogged = true;
			}
			return false;
		}
	}

	private void renderFullscreenBackground(
			PGraphicsOpenGL target,
			CubemapFace face,
			PMatrix3D orientationMatrix) {
		target.pushStyle();
		target.pushMatrix();
		try {
			target.resetMatrix();
			target.ortho();
			target.hint(ENABLE_DEPTH_TEST);
			target.hint(DISABLE_DEPTH_MASK);
			target.noStroke();
			equirectangularShader.set("environmentMap", equirectangularImage);
			equirectangularShader.set("resolution", target.width, target.height);
			equirectangularShader.set("faceIndex", face.ordinal());
			equirectangularShader.set("environmentRotation", orientationMatrix);
			equirectangularShader.set("yawOffset", yawOffset);
			equirectangularShader.set("intensity", intensity);
			target.shader(equirectangularShader);
			target.rect(0, 0, target.width, target.height);
		} finally {
			target.hint(ENABLE_DEPTH_MASK);
			target.resetShader();
			target.popMatrix();
			target.popStyle();
		}
	}
}
