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
 * Draws a shared LDR equirectangular environment as an infinite visual background.
 *
 * <p>The renderer owns shader programs only. Its {@link EnvironmentState}, source
 * {@link PImage}, and Processing-managed {@link Texture} are borrowed. Spherical capture uses
 * a fullscreen pass; Standard draws an observer-centred sky sphere. Both run after scene
 * rendering, so scene {@code background()} calls survive while foreground depth remains
 * authoritative.</p>
 */
public final class EnvironmentBackgroundRenderer {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final int GL_DEPTH_FUNC = 0x0B74;
	private static final String FULLSCREEN_BACKGROUND_VERT =
			"data/shaders/environment/equirectangular_background.vert";
	private static final String STANDARD_BACKGROUND_VERT =
			"data/shaders/environment/standard_equirectangular_background.vert";
	private static final String CUBEMAP_BACKGROUND_FRAG =
			"data/shaders/environment/equirectangular_background.frag";
	private static final String STANDARD_BACKGROUND_FRAG =
			"data/shaders/environment/standard_equirectangular_background.frag";

	private final PApplet parent;
	private final EnvironmentState state;
	private final IntBuffer savedDepthFunction = IntBuffer.allocate(1);
	private final IntBuffer savedDepthMask = IntBuffer.allocate(1);
	private NativeEnvironmentShader cubemapShader;
	private NativeEnvironmentShader standardShader;
	private boolean cubemapShaderLoadAttempted;
	private boolean standardShaderLoadAttempted;
	private boolean unavailableWarningLogged;
	private boolean renderFailureWarningLogged;
	private final PMatrix3D standardEnvironmentRotation = new PMatrix3D();

	/**
	 * Creates a renderer with an independent environment state for compatibility.
	 * @param parent Processing parent used for shaders and texture resolution
	 */
	public EnvironmentBackgroundRenderer(PApplet parent) {
		this(parent, new EnvironmentState());
	}

	/**
	 * Creates a renderer that consumes the supplied shared logical state.
	 *
	 * @param parent Processing parent used for shader and texture resolution
	 * @param state shared borrowed environment state
	 */
	public EnvironmentBackgroundRenderer(PApplet parent, EnvironmentState state) {
		this.parent = Objects.requireNonNull(parent, "parent cannot be null");
		this.state = Objects.requireNonNull(state, "state cannot be null");
	}

	/**
	 * Sets the borrowed LDR source.
	 * @param image borrowed source, or {@code null}
	 */
	public void setEquirectangularImage(PImage image) {
		state.setLdrEquirectangularSource(image);
		unavailableWarningLogged = false;
		renderFailureWarningLogged = false;
	}

	/** Clears the borrowed source reference. */
	public void clear() {
		state.clearSource();
	}

	/**
	 * Returns whether a source is configured.
	 * @return {@code true} when a source is configured
	 */
	public boolean hasEquirectangularImage() {
		return state.hasSource();
	}

	/**
	 * Shows or hides the visual background.
	 * @param visible {@code true} to draw the background
	 */
	public void setVisible(boolean visible) {
		state.setVisible(visible);
	}

	/**
	 * Returns whether the visual background is enabled.
	 * @return {@code true} when enabled
	 */
	public boolean isVisible() {
		return state.isVisible();
	}

	/**
	 * Sets the non-negative visual colour multiplier.
	 * @param intensity visual colour multiplier
	 */
	public void setIntensity(float intensity) {
		state.setIntensity(intensity);
	}

	/**
	 * Returns the visual colour multiplier.
	 * @return visual colour multiplier
	 */
	public float getIntensity() {
		return state.getIntensity();
	}

	/**
	 * Sets the source-longitude offset in radians.
	 * @param yawOffset source-longitude offset in radians
	 */
	public void setYawOffset(float yawOffset) {
		state.setYawOffset(yawOffset);
	}

	/**
	 * Returns the source-longitude offset in radians.
	 * @return source-longitude offset in radians
	 */
	public float getYawOffset() {
		return state.getYawOffset();
	}

	/**
	 * Returns the shared logical state consumed by this renderer.
	 * @return logical Environment state
	 */
	public EnvironmentState getEnvironmentState() {
		return state;
	}

	/**
	 * Compatibility entry point that derives the spherical matrix from a quaternion.
	 * @param target active Processing target
	 * @param pgl already-active PGL context
	 * @param face canonical cubemap face
	 * @param sphericalOrientation spherical orientation quaternion
	 * @return {@code true} when the background was drawn
	 */
	public boolean renderCubemapFace(
			PGraphicsOpenGL target,
			PGL pgl,
			CubemapFace face,
			Quaternion sphericalOrientation) {
		Quaternion spherical = sphericalOrientation == null
				? new Quaternion(0.0f, 0.0f, 0.0f, 1.0f)
				: sphericalOrientation;
		Quaternion environmentOrientation = spherical
				.multiply(state.getSceneCameraOrientation())
				.normalize();
		PMatrix3D orientationMatrix = environmentOrientation.toMatrix();
		return renderCubemapFace(target, pgl, face, orientationMatrix);
	}

	/** Draws one spherical face while the caller owns the Processing draw lifecycle. */
	boolean renderScratchCubemapFace(
			PGraphicsOpenGL target,
			CubemapFace face,
			PMatrix3D orientationMatrix) {
		if (!isRenderable()) {
			return false;
		}
		NativeEnvironmentShader shader = cubemapShader();
		if (target == null || face == null || shader == null) {
			warnUnavailable("spherical face");
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
		if (!isRenderable()) {
			return false;
		}
		NativeEnvironmentShader shader = cubemapShader();
		if (target == null || pgl == null || face == null || shader == null) {
			warnUnavailable("spherical face");
			return false;
		}

		try {
			PMatrix3D effectiveOrientation = orientationMatrix == null
					? new PMatrix3D()
					: orientationMatrix;
			shader.set("faceResolution", target.width, target.height);
			shader.set("faceIndex", face.index());
			shader.set("environmentRotation", effectiveOrientation);
			return renderFullscreenBackground(target, pgl, shader);
		} catch (RuntimeException error) {
			warnRenderFailure(error);
			return false;
		}
	}

	/**
	 * Compatibility overload for the Standard environment pass.
	 *
	 * @param target active Standard target whose scene drawing has been flushed
	 * @param cameraRotation retained for source compatibility; the target camera is authoritative
	 * @param verticalFovRadians retained for source compatibility
	 * @param aspect retained for source compatibility
	 * @return {@code true} when the pass was drawn
	 */
	public boolean renderStandard(
			PGraphicsOpenGL target,
			PMatrix3D cameraRotation,
			float verticalFovRadians,
			float aspect) {
		return renderStandard(target);
	}

	/**
	 * Draws the Standard environment on a sky sphere centred on the current camera.
	 *
	 * <p>The sphere is placed in camera space just inside the active far plane. Its directions
	 * are converted back to world space through the current inverse camera basis, so the
	 * panorama responds naturally to rotation while remaining centred on the eye without
	 * parallax.</p>
	 *
	 * @param target active Standard target whose scene drawing has been flushed
	 * @return {@code true} when the pass was drawn
	 */
	public boolean renderStandard(PGraphicsOpenGL target) {
		if (!isRenderable()) {
			return false;
		}
		NativeEnvironmentShader shader = standardShader();
		if (target == null || shader == null) {
			warnUnavailable("Standard view");
			return false;
		}

		configureEnvironmentSampler(target, shader);
		shader.set("cameraRight", target.cameraInv.m00, target.cameraInv.m10, target.cameraInv.m20);
		shader.set("cameraUp", target.cameraInv.m01, target.cameraInv.m11, target.cameraInv.m21);
		shader.set("cameraBackward", target.cameraInv.m02, target.cameraInv.m12, target.cameraInv.m22);
		state.getSceneCameraOrientation().toMatrix(standardEnvironmentRotation);
		shader.set("environmentRotation", standardEnvironmentRotation);

		PGL pgl = target.beginPGL();
		try {
			snapshotAndConfigureBackgroundState(pgl);
		} catch (RuntimeException error) {
			warnRenderFailure(error);
			return false;
		} finally {
			target.endPGL();
		}

		try {
			target.pushStyle();
			target.pushMatrix();
			target.resetMatrix();
			target.noStroke();
			target.shader(shader);
			target.sphere(Math.max(target.cameraNear * 2.0f, target.cameraFar * 0.95f));
			target.flush();
			renderFailureWarningLogged = false;
			unavailableWarningLogged = false;
			return true;
		} catch (RuntimeException error) {
			warnRenderFailure(error);
			return false;
		} finally {
			target.resetShader();
			target.popMatrix();
			target.popStyle();
			PGL restorePgl = target.beginPGL();
			try {
				restoreBackgroundState(restorePgl);
			} finally {
				target.endPGL();
			}
		}
	}

	private boolean renderFullscreenBackground(
			PGraphicsOpenGL target,
			PGL pgl,
			NativeEnvironmentShader shader) {
		configureEnvironmentSampler(target, shader);
		snapshotAndConfigureBackgroundState(pgl);

		try {
			shader.bindFor(target);
			pgl.drawArrays(PGL.TRIANGLES, 0, 3);
			renderFailureWarningLogged = false;
			unavailableWarningLogged = false;
			return true;
		} finally {
			if (shader.bound()) {
				shader.unbind();
			}
			restoreBackgroundState(pgl);
		}
	}

	private void configureEnvironmentSampler(
			PGraphicsOpenGL target,
			NativeEnvironmentShader shader) {
		PImage source = state.getLdrEquirectangularSource();
		Texture environmentTexture = target.getTexture(source);
		if (environmentTexture == null || !environmentTexture.available()) {
			throw new IllegalStateException("Environment source has no available OpenGL texture.");
		}

		float maxU = environmentTexture.maxTexcoordU();
		float maxV = environmentTexture.maxTexcoordV();
		float scaleU = environmentTexture.invertedX() ? -maxU : maxU;
		float scaleV = environmentTexture.invertedY() ? -maxV : maxV;
		float offsetU = environmentTexture.invertedX() ? maxU : 0.0f;
		float offsetV = environmentTexture.invertedY() ? maxV : 0.0f;

		shader.set("environmentMap", source);
		shader.set("environmentUvScale", scaleU, scaleV);
		shader.set("environmentUvOffset", offsetU, offsetV);
		shader.set("yawOffset", state.getYawOffset());
		shader.set("intensity", state.getIntensity());
	}

	private boolean savedDepthTestEnabled;
	private boolean savedBlendEnabled;
	private boolean savedCullFaceEnabled;
	private boolean savedScissorTestEnabled;

	private void snapshotAndConfigureBackgroundState(PGL pgl) {
		savedDepthTestEnabled = pgl.isEnabled(PGL.DEPTH_TEST);
		savedBlendEnabled = pgl.isEnabled(PGL.BLEND);
		savedCullFaceEnabled = pgl.isEnabled(PGL.CULL_FACE);
		savedScissorTestEnabled = pgl.isEnabled(PGL.SCISSOR_TEST);
		savedDepthFunction.clear();
		savedDepthMask.clear();
		pgl.getIntegerv(GL_DEPTH_FUNC, savedDepthFunction);
		pgl.getBooleanv(PGL.DEPTH_WRITEMASK, savedDepthMask);

		pgl.enable(PGL.DEPTH_TEST);
		pgl.depthFunc(PGL.LEQUAL);
		pgl.depthMask(false);
		pgl.disable(PGL.BLEND);
		pgl.disable(PGL.CULL_FACE);
		pgl.disable(PGL.SCISSOR_TEST);
	}

	private void restoreBackgroundState(PGL pgl) {
		pgl.depthMask(savedDepthMask.get(0) != 0);
		pgl.depthFunc(savedDepthFunction.get(0));
		restoreCapability(pgl, PGL.DEPTH_TEST, savedDepthTestEnabled);
		restoreCapability(pgl, PGL.BLEND, savedBlendEnabled);
		restoreCapability(pgl, PGL.CULL_FACE, savedCullFaceEnabled);
		restoreCapability(pgl, PGL.SCISSOR_TEST, savedScissorTestEnabled);
	}

	private boolean isRenderable() {
		return state.isVisible() && state.hasSource();
	}

	private NativeEnvironmentShader cubemapShader() {
		if (!cubemapShaderLoadAttempted) {
			cubemapShaderLoadAttempted = true;
			cubemapShader = loadShader(CUBEMAP_BACKGROUND_FRAG);
		}
		return cubemapShader;
	}

	private NativeEnvironmentShader standardShader() {
		if (!standardShaderLoadAttempted) {
			standardShaderLoadAttempted = true;
			standardShader = loadShader(STANDARD_BACKGROUND_VERT, STANDARD_BACKGROUND_FRAG);
		}
		return standardShader;
	}

	private NativeEnvironmentShader loadShader(String fragmentPath) {
		return loadShader(FULLSCREEN_BACKGROUND_VERT, fragmentPath);
	}

	private NativeEnvironmentShader loadShader(String vertexPath, String fragmentPath) {
		try {
			return new NativeEnvironmentShader(parent, vertexPath, fragmentPath);
		} catch (RuntimeException error) {
			LOGGER.warning("Environment background shader unavailable: " + error.getMessage());
			unavailableWarningLogged = true;
			return null;
		}
	}

	private void warnUnavailable(String pass) {
		if (!unavailableWarningLogged) {
			LOGGER.warning("Environment background unavailable; " + pass + " background skipped.");
			unavailableWarningLogged = true;
		}
	}

	private void warnRenderFailure(RuntimeException error) {
		if (!renderFailureWarningLogged) {
			LOGGER.warning("Environment background render failed; background skipped: "
					+ error.getMessage());
			renderFailureWarningLogged = true;
		}
	}

	private static void restoreCapability(PGL pgl, int capability, boolean enabled) {
		if (enabled) {
			pgl.enable(capability);
		} else {
			pgl.disable(capability);
		}
	}

	/** Releases owned shader programs but leaves the shared borrowed state untouched. */
	public void dispose() {
		if (cubemapShader != null) {
			cubemapShader.disposeResources();
			cubemapShader = null;
		}
		if (standardShader != null) {
			standardShader.disposeResources();
			standardShader = null;
		}
		cubemapShaderLoadAttempted = false;
		standardShaderLoadAttempted = false;
	}

	/** Keeps Processing shader compilation while allowing a native fullscreen draw. */
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
