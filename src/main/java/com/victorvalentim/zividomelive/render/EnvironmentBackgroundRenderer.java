package com.victorvalentim.zividomelive.render;

import com.victorvalentim.zividomelive.render.camera.CubemapFace;
import com.victorvalentim.zividomelive.support.LogManager;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PMatrix3D;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;
import processing.opengl.Texture;

import java.nio.IntBuffer;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Draws a library-owned equirectangular environment behind spherical scene capture.
 *
 * <p>This is a native background pass, not scene geometry. After Processing flushes the scene
 * into its reusable scratch framebuffer, the renderer emits one fullscreen triangle at far
 * depth. The completed scratch colour is then copied into the native cubemap face. The pass
 * therefore survives scene-owned {@code background()} calls while leaving foreground depth
 * intact.</p>
 */
public final class EnvironmentBackgroundRenderer {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final int GL_DEPTH_FUNC = 0x0B74;
	private static final String EQUIRECTANGULAR_BACKGROUND_VERT =
			"data/shaders/environment/equirectangular_background.vert";
	private static final String EQUIRECTANGULAR_BACKGROUND_FRAG =
			"data/shaders/environment/equirectangular_background.frag";

	private final NativeEnvironmentShader equirectangularShader;
	private final IntBuffer savedDepthFunction = IntBuffer.allocate(1);
	private final IntBuffer savedDepthMask = IntBuffer.allocate(1);
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
		NativeEnvironmentShader shader = null;
		try {
			shader = new NativeEnvironmentShader(
					parent,
					EQUIRECTANGULAR_BACKGROUND_VERT,
					EQUIRECTANGULAR_BACKGROUND_FRAG);
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
	 * @param yawOffset rotation offset, in radians, applied to the source longitude lookup
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
	 * Draws the configured equirectangular background into one qualified cubemap scratch face.
	 *
	 * @param target active Processing OpenGL scratch target
	 * @param pgl already-active PGL context owning the scratch framebuffer
	 * @param face target cubemap face
	 * @param sphericalOrientation orientation shared by the spherical capture
	 * @return {@code true} when a background pass was drawn
	 */
	public boolean renderCubemapFace(
			PGraphicsOpenGL target,
			PGL pgl,
			CubemapFace face,
			Quaternion sphericalOrientation) {
		PMatrix3D orientationMatrix = sphericalOrientation == null
				? new PMatrix3D()
				: sphericalOrientation.toMatrix();
		return renderCubemapFace(target, pgl, face, orientationMatrix);
	}

	/**
	 * Draws one qualified cubemap-face background into the active Processing scratch frame.
	 * The caller owns {@code beginDraw()/endDraw()}; this method owns only the nested PGL scope.
	 */
	boolean renderScratchCubemapFace(
			PGraphicsOpenGL target,
			CubemapFace face,
			PMatrix3D orientationMatrix) {
		if (!visible || equirectangularImage == null) {
			return false;
		}
		if (target == null || face == null || equirectangularShader == null) {
			if (!unavailableWarningLogged) {
				LOGGER.warning("Environment background unavailable; scratch face background skipped.");
				unavailableWarningLogged = true;
			}
			return false;
		}

		PGL pgl = target.beginPGL();
		try {
			return renderCubemapFace(target, pgl, face, orientationMatrix);
		} finally {
			target.endPGL();
		}
	}

	private boolean renderCubemapFace(
			PGraphicsOpenGL target,
			PGL pgl,
			CubemapFace face,
			PMatrix3D orientationMatrix) {
		if (!visible || equirectangularImage == null) {
			return false;
		}
		if (target == null || pgl == null || face == null || equirectangularShader == null) {
			if (!unavailableWarningLogged) {
				LOGGER.warning("Environment background unavailable; cubemap face background skipped.");
				unavailableWarningLogged = true;
			}
			return false;
		}

		try {
			renderFullscreenBackground(
					target,
					pgl,
					face,
					orientationMatrix == null ? new PMatrix3D() : orientationMatrix);
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
			PGL pgl,
			CubemapFace face,
			PMatrix3D orientationMatrix) {
		Texture environmentTexture = target.getTexture(equirectangularImage);
		if (environmentTexture == null || !environmentTexture.available()) {
			throw new IllegalStateException("Environment image has no available OpenGL texture.");
		}

		boolean depthTestEnabled = pgl.isEnabled(PGL.DEPTH_TEST);
		boolean blendEnabled = pgl.isEnabled(PGL.BLEND);
		boolean cullFaceEnabled = pgl.isEnabled(PGL.CULL_FACE);
		boolean scissorTestEnabled = pgl.isEnabled(PGL.SCISSOR_TEST);
		savedDepthFunction.clear();
		savedDepthMask.clear();
		pgl.getIntegerv(GL_DEPTH_FUNC, savedDepthFunction);
		pgl.getBooleanv(PGL.DEPTH_WRITEMASK, savedDepthMask);

		float maxU = environmentTexture.maxTexcoordU();
		float maxV = environmentTexture.maxTexcoordV();
		float scaleU = environmentTexture.invertedX() ? -maxU : maxU;
		float scaleV = environmentTexture.invertedY() ? -maxV : maxV;
		float offsetU = environmentTexture.invertedX() ? maxU : 0.0f;
		float offsetV = environmentTexture.invertedY() ? maxV : 0.0f;

		try {
			equirectangularShader.set("environmentMap", equirectangularImage);
			equirectangularShader.set("faceResolution", target.width, target.height);
			equirectangularShader.set("environmentUvScale", scaleU, scaleV);
			equirectangularShader.set("environmentUvOffset", offsetU, offsetV);
			equirectangularShader.set("faceIndex", face.index());
			equirectangularShader.set("environmentRotation", orientationMatrix);
			equirectangularShader.set("yawOffset", yawOffset);
			equirectangularShader.set("intensity", intensity);

			pgl.enable(PGL.DEPTH_TEST);
			pgl.depthFunc(PGL.LEQUAL);
			pgl.depthMask(false);
			pgl.disable(PGL.BLEND);
			pgl.disable(PGL.CULL_FACE);
			pgl.disable(PGL.SCISSOR_TEST);

			equirectangularShader.bindFor(target);
			pgl.drawArrays(PGL.TRIANGLES, 0, 3);
		} finally {
			if (equirectangularShader.bound()) {
				equirectangularShader.unbind();
			}
			pgl.depthMask(savedDepthMask.get(0) != 0);
			pgl.depthFunc(savedDepthFunction.get(0));
			restoreCapability(pgl, PGL.DEPTH_TEST, depthTestEnabled);
			restoreCapability(pgl, PGL.BLEND, blendEnabled);
			restoreCapability(pgl, PGL.CULL_FACE, cullFaceEnabled);
			restoreCapability(pgl, PGL.SCISSOR_TEST, scissorTestEnabled);
		}
	}

	private static void restoreCapability(PGL pgl, int capability, boolean enabled) {
		if (enabled) {
			pgl.enable(capability);
		} else {
			pgl.disable(capability);
		}
	}

	/** Releases the native shader program owned by this renderer. */
	void dispose() {
		clear();
		if (equirectangularShader != null) {
			equirectangularShader.disposeResources();
		}
	}

	/** Keeps Processing texture management while allowing a native draw call. */
	private static final class NativeEnvironmentShader extends PShader {
		private NativeEnvironmentShader(PApplet parent, String vertexPath, String fragmentPath) {
			super(parent, vertexPath, fragmentPath);
		}

		private void bindFor(PGraphicsOpenGL target) {
			setRenderer(target);
			bind();
		}

		private void disposeResources() {
			super.dispose();
		}
	}
}
