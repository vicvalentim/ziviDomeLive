package com.victorvalentim.zividomelive.render.gl;

import com.victorvalentim.zividomelive.render.camera.CubemapFace;
import com.victorvalentim.zividomelive.support.LogManager;
import processing.core.PApplet;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Native OpenGL cubemap texture owned by the spherical rendering pipeline.
 *
 * <p>This target owns the {@code GL_TEXTURE_CUBE_MAP} allocation, a reusable direct-render
 * framebuffer, and a depth renderbuffer. Scene capture renders directly into cubemap face
 * attachments without a six-texture Processing bridge.</p>
 */
public final class CubemapTarget implements AutoCloseable {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final int MAX_GL_ERROR_LOGS = 4;
	private static final int MAX_GL_ERRORS_PER_CHECK = 2;
	private static final int GL_TEXTURE_CUBE_MAP_SEAMLESS = 0x884F;
	private static final int GL_ACTIVE_TEXTURE = 0x84E0;
	private static final int GL_TEXTURE_BINDING_CUBE_MAP = 0x8514;
	private static final int GL_READ_FRAMEBUFFER_BINDING = 0x8CAA;
	private static final int GL_DRAW_FRAMEBUFFER_BINDING = 0x8CA6;

	private final PApplet parent;
	private final ProcessingGlAdapter glAdapter;
	private final int resolution;
	private int renderFramebufferId;
	private int depthRenderbufferId;
	private int textureId;
	private boolean mipmapsValid;
	private int glErrorLogsRemaining = MAX_GL_ERROR_LOGS;

	private CubemapTarget(
			PApplet parent,
			ProcessingGlAdapter glAdapter,
			int resolution,
			int renderFramebufferId,
			int depthRenderbufferId,
			int textureId) {
		this.parent = Objects.requireNonNull(parent, "parent");
		this.glAdapter = Objects.requireNonNull(glAdapter, "glAdapter");
		this.resolution = resolution;
		this.renderFramebufferId = renderFramebufferId;
		this.depthRenderbufferId = depthRenderbufferId;
		this.textureId = textureId;
		this.mipmapsValid = true;
	}

	/**
	 * Allocates a native OpenGL cubemap texture with conservative defaults,
	 * a reusable direct-render framebuffer, and a depth renderbuffer.
	 *
	 * @param parent Processing parent with an active OpenGL renderer
	 * @param resolution square face size in pixels
	 * @return allocated cubemap target
	 */
	public static CubemapTarget create(PApplet parent, int resolution) {
		return create(parent, resolution, ProcessingGlAdapter.getDefault());
	}

	static CubemapTarget create(PApplet parent, int resolution, ProcessingGlAdapter glAdapter) {
		validateResolution(resolution);
		ProcessingGlCapabilities capabilities = glAdapter.queryCapabilities(parent);
		if (!capabilities.isOpenGlRenderer()) {
			throw new IllegalStateException("Processing OpenGL renderer is not available.");
		}

		validateSphericalShaderProfile(capabilities);
		if (!capabilities.supportsCubemap()) {
			throw new IllegalStateException("OpenGL cubemap textures are not supported.");
		}
		if (!capabilities.supportsFramebuffer()) {
			throw new IllegalStateException("OpenGL framebuffer objects are not supported.");
		}

		int[] resources = glAdapter.withPgl(parent, pgl -> allocateResources(pgl, resolution, capabilities));
		if (LogManager.isDebugEnabled()) {
			LOGGER.fine("Native CubemapTarget allocated: resolution=" + resolution
					+ ", textureId=" + resources[0]
					+ ", renderFbo=" + resources[1]
					+ ", depthRbo=" + resources[2]
					+ ", seamless=" + capabilities.supportsSeamlessCubemap()
					+ ", anisotropic=" + capabilities.supportsAnisotropicFiltering());
		}
		return new CubemapTarget(
				parent,
				glAdapter,
				resolution,
				resources[1],
				resources[2],
				resources[0]);
	}

	/**
	 * Returns the square face resolution in pixels.
	 *
	 * @return cubemap face resolution
	 */
	public int resolution() {
		return resolution;
	}

	/**
	 * Returns the native OpenGL texture id.
	 *
	 * @return texture id, or {@code 0} after disposal
	 */
	public int textureId() {
		return textureId;
	}

	/**
	 * Reports whether this target currently owns a native texture id.
	 *
	 * @return {@code true} while allocated
	 */
	public boolean isAllocated() {
		return textureId != 0;
	}

	/**
	 * Binds a cubemap face as the active draw framebuffer and runs the supplied render pass.
	 *
	 * <p>This mirrors the native PGL sampleCube sketch: one framebuffer is reused while
	 * {@code framebufferTexture2D} attaches each {@code GL_TEXTURE_CUBE_MAP_*} face directly.
	 * The Processing draw callback runs while that framebuffer is active, so scene code can
	 * keep using the current {@code PGraphicsOpenGL} contract.</p>
	 *
	 * @param face target cubemap face
	 * @param graphics Processing OpenGL target used to emit scene draw commands
	 * @param renderOperation operation that emits Processing/OpenGL draw commands
	 */
	public void renderFace(CubemapFace face, PGraphicsOpenGL graphics, Runnable renderOperation) {
		renderFace(face, graphics, ignored -> renderOperation.run());
	}

	/**
	 * Binds a cubemap face and exposes the already-active PGL context to the render pass.
	 *
	 * <p>The overload is intended for native passes that must share the exact framebuffer and
	 * context used by Processing scene capture. Callers must not invoke {@code beginPGL()} or
	 * {@code endPGL()} from inside the operation.</p>
	 *
	 * @param face target cubemap face
	 * @param graphics Processing OpenGL target used to emit scene draw commands
	 * @param renderOperation operation receiving the active PGL context
	 */
	public void renderFace(
			CubemapFace face,
			PGraphicsOpenGL graphics,
			Consumer<PGL> renderOperation) {
		Objects.requireNonNull(face, "face");
		Objects.requireNonNull(graphics, "graphics");
		Objects.requireNonNull(renderOperation, "renderOperation");
		ensureAllocated();

		glAdapter.withPgl(graphics, pgl -> {
			withCubemapFaceFramebuffer(
					pgl,
					glTargetFor(face),
					() -> renderOperation.accept(pgl));
			return null;
		});
		mipmapsValid = false;
	}

	/**
	 * Regenerates mipmaps after one or more cubemap faces have changed.
	 */
	public void generateMipmaps() {
		ensureAllocated();
		glAdapter.withPgl(parent, pgl -> {
			withTextureUnitZeroCubemapBound(pgl, textureId, () ->
					pgl.generateMipmap(PGL.TEXTURE_CUBE_MAP));
			return null;
		});
		mipmapsValid = true;
	}

	/**
	 * Reports whether mipmaps match the current base cubemap faces.
	 *
	 * @return {@code true} after allocation or the latest successful mipmap regeneration
	 */
	public boolean hasValidMipmaps() {
		return mipmapsValid;
	}

	/**
	 * Returns the OpenGL texture target for a canonical cubemap face.
	 *
	 * @param face canonical cubemap face
	 * @return matching {@code GL_TEXTURE_CUBE_MAP_*} target
	 */
	public static int glTargetFor(CubemapFace face) {
		return switch (Objects.requireNonNull(face, "face")) {
			case POSITIVE_X -> PGL.TEXTURE_CUBE_MAP_POSITIVE_X;
			case NEGATIVE_X -> PGL.TEXTURE_CUBE_MAP_NEGATIVE_X;
			case POSITIVE_Y -> PGL.TEXTURE_CUBE_MAP_POSITIVE_Y;
			case NEGATIVE_Y -> PGL.TEXTURE_CUBE_MAP_NEGATIVE_Y;
			case POSITIVE_Z -> PGL.TEXTURE_CUBE_MAP_POSITIVE_Z;
			case NEGATIVE_Z -> PGL.TEXTURE_CUBE_MAP_NEGATIVE_Z;
		};
	}

	/**
	 * Releases the owned cubemap texture and framebuffer ids.
	 */
	@Override
	public void close() {
		dispose();
	}

	/**
	 * Releases the owned cubemap texture and framebuffer ids.
	 */
	public void dispose() {
		int texture = textureId;
		int renderFramebuffer = renderFramebufferId;
		int depthRenderbuffer = depthRenderbufferId;
		if (texture == 0
				&& renderFramebuffer == 0
				&& depthRenderbuffer == 0) {
			return;
		}

		glAdapter.withPgl(parent, pgl -> {
			if (LogManager.isDebugEnabled()) {
				LOGGER.fine("Disposing native CubemapTarget: textureId=" + texture
						+ ", renderFbo=" + renderFramebuffer
						+ ", depthRbo=" + depthRenderbuffer);
			}

			if (depthRenderbufferId != 0) {
				deleteRenderbuffer(pgl, depthRenderbufferId);
				depthRenderbufferId = 0;
			}

			if (renderFramebufferId != 0) {
				deleteFramebuffer(pgl, renderFramebufferId);
				renderFramebufferId = 0;
			}

			if (textureId != 0) {
				deleteTexture(pgl, textureId);
				textureId = 0;
			}

			return null;
		});

		mipmapsValid = false;
	}

	void ensureAllocated() {
		if (!isAllocated()
				|| renderFramebufferId == 0
				|| depthRenderbufferId == 0) {
			throw new IllegalStateException("Cubemap target has been disposed.");
		}
	}

	static void validateSphericalShaderProfile(
			ProcessingGlCapabilities capabilities) {
		if (capabilities.supportsRequiredSphericalShaderProfile()) {
			return;
		}

		throw new IllegalStateException(
				"ziviDomeLive spherical pipeline requires desktop OpenGL 4.1 "
						+ "and GLSL 4.10 or newer. Detected OpenGL: "
						+ detectedValue(capabilities.version())
						+ "; GLSL: "
						+ detectedValue(capabilities.shadingLanguageVersion())
						+ "; Vendor: "
						+ detectedValue(capabilities.vendor())
						+ "; Renderer: "
						+ detectedValue(capabilities.renderer())
						+ ".");
	}

	private static String detectedValue(String value) {
		return value == null || value.isBlank() ? "<unavailable>" : value;
	}

	static void validateResolution(int resolution) {
		if (resolution <= 0) {
			throw new IllegalArgumentException("Cubemap resolution must be positive.");
		}
	}

	private static int[] allocateResources(
			PGL pgl,
			int resolution,
			ProcessingGlCapabilities capabilities) {
		int textureId = 0;
		int renderFramebufferId = 0;

		try {
			textureId = allocateTexture(pgl, resolution, capabilities);
			renderFramebufferId = allocateFramebuffer(pgl);
			int depthRenderbufferId = allocateDepthRenderbuffer(pgl, resolution);
			return new int[]{
					textureId,
					renderFramebufferId,
					depthRenderbufferId};
		} catch (RuntimeException error) {
			try {
				deleteFramebuffer(pgl, renderFramebufferId);
			} catch (RuntimeException cleanupError) {
				error.addSuppressed(cleanupError);
			}

			try {
				deleteTexture(pgl, textureId);
			} catch (RuntimeException cleanupError) {
				error.addSuppressed(cleanupError);
			}

			throw error;
		}
	}

	private static void deleteTexture(PGL pgl, int textureId) {
		if (textureId == 0) {
			return;
		}
		IntBuffer textureBuffer = IntBuffer.allocate(1);
		textureBuffer.put(0, textureId);
		pgl.deleteTextures(1, textureBuffer);
	}

	private static void deleteFramebuffer(PGL pgl, int framebufferId) {
		if (framebufferId == 0) {
			return;
		}
		IntBuffer framebufferBuffer = IntBuffer.allocate(1);
		framebufferBuffer.put(0, framebufferId);
		pgl.deleteFramebuffers(1, framebufferBuffer);
	}

	private static void deleteRenderbuffer(PGL pgl, int renderbufferId) {
		if (renderbufferId == 0) {
			return;
		}
		IntBuffer renderbufferBuffer = IntBuffer.allocate(1);
		renderbufferBuffer.put(0, renderbufferId);
		pgl.deleteRenderbuffers(1, renderbufferBuffer);
	}

	private static int allocateFramebuffer(PGL pgl) {
		IntBuffer framebufferBuffer = IntBuffer.allocate(1);
		pgl.genFramebuffers(1, framebufferBuffer);
		int renderFramebufferId = framebufferBuffer.get(0);
		if (renderFramebufferId == 0) {
			throw new IllegalStateException("OpenGL did not allocate cubemap framebuffer.");
		}
		return renderFramebufferId;
	}

	private static int allocateDepthRenderbuffer(PGL pgl, int resolution) {
		IntBuffer renderbufferBuffer = IntBuffer.allocate(1);
		pgl.genRenderbuffers(1, renderbufferBuffer);
		int renderbufferId = renderbufferBuffer.get(0);
		if (renderbufferId == 0) {
			throw new IllegalStateException("OpenGL did not allocate cubemap depth renderbuffer.");
		}

		RuntimeException failure = null;

		try {
			pgl.bindRenderbuffer(PGL.RENDERBUFFER, renderbufferId);
			pgl.renderbufferStorage(PGL.RENDERBUFFER, PGL.DEPTH_COMPONENT24, resolution, resolution);
		} catch (RuntimeException error) {
			failure = error;
		}

		try {
			pgl.bindRenderbuffer(PGL.RENDERBUFFER, 0);
		} catch (RuntimeException unbindError) {
			if (failure == null) {
				failure = unbindError;
			} else {
				failure.addSuppressed(unbindError);
			}
		}

		if (failure != null) {
			try {
				deleteRenderbuffer(pgl, renderbufferId);
			} catch (RuntimeException cleanupError) {
				failure.addSuppressed(cleanupError);
			}
			throw failure;
		}

		return renderbufferId;
	}

	private static int allocateTexture(
			PGL pgl,
			int resolution,
			ProcessingGlCapabilities capabilities) {
		IntBuffer textureBuffer = IntBuffer.allocate(1);
		pgl.genTextures(1, textureBuffer);
		int textureId = textureBuffer.get(0);
		if (textureId == 0) {
			throw new IllegalStateException("OpenGL did not allocate a cubemap texture id.");
		}

		try {
			withTextureUnitZeroCubemapBound(pgl, textureId, () -> {
				configureSampling(pgl, capabilities);

				for (CubemapFace face : CubemapFace.values()) {
					pgl.texImage2D(
							glTargetFor(face),
							0,
							PGL.RGBA8,
							resolution,
							resolution,
							0,
							PGL.RGBA,
							PGL.UNSIGNED_BYTE,
							null);
				}
				pgl.generateMipmap(PGL.TEXTURE_CUBE_MAP);
			});

			if (capabilities.supportsSeamlessCubemap()) {
				pgl.enable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
			}
			return textureId;
		} catch (RuntimeException error) {
			try {
				deleteTexture(pgl, textureId);
			} catch (RuntimeException cleanupError) {
				error.addSuppressed(cleanupError);
			}
			throw error;
		}
	}

	private static void configureSampling(PGL pgl, ProcessingGlCapabilities capabilities) {
		pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_MIN_FILTER, PGL.LINEAR_MIPMAP_LINEAR);
		pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_MAG_FILTER, PGL.LINEAR);
		pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_S, PGL.CLAMP_TO_EDGE);
		pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_T, PGL.CLAMP_TO_EDGE);
		pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_R, PGL.CLAMP_TO_EDGE);

		if (capabilities.supportsAnisotropicFiltering()) {
			pgl.texParameterf(
					PGL.TEXTURE_CUBE_MAP,
					PGL.TEXTURE_MAX_ANISOTROPY,
					maxSupportedAnisotropy(pgl));
		}
	}

	private static float maxSupportedAnisotropy(PGL pgl) {
		FloatBuffer maxAnisotropy = FloatBuffer.allocate(1);
		pgl.getFloatv(PGL.MAX_TEXTURE_MAX_ANISOTROPY, maxAnisotropy);
		return Math.max(1.0f, maxAnisotropy.get(0));
	}

	private static void withTextureUnitZeroCubemapBound(PGL pgl, int textureId, GlStateOperation operation) {
		IntBuffer savedActiveTexture = IntBuffer.allocate(1);
		IntBuffer savedCubemapBinding = IntBuffer.allocate(1);
		pgl.getIntegerv(GL_ACTIVE_TEXTURE, savedActiveTexture);
		pgl.activeTexture(PGL.TEXTURE0);
		pgl.getIntegerv(GL_TEXTURE_BINDING_CUBE_MAP, savedCubemapBinding);
		try {
			pgl.bindTexture(PGL.TEXTURE_CUBE_MAP, textureId);
			operation.run();
		} finally {
			pgl.bindTexture(PGL.TEXTURE_CUBE_MAP, savedCubemapBinding.get(0));
			pgl.activeTexture(savedActiveTexture.get(0));
		}
	}

	private void withCubemapFaceFramebuffer(PGL pgl, int cubemapFaceTarget, Runnable renderOperation) {
		IntBuffer savedReadFramebuffer = IntBuffer.allocate(1);
		IntBuffer savedDrawFramebuffer = IntBuffer.allocate(1);
		IntBuffer savedViewport = IntBuffer.allocate(4);
		pgl.getIntegerv(GL_READ_FRAMEBUFFER_BINDING, savedReadFramebuffer);
		pgl.getIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, savedDrawFramebuffer);
		pgl.getIntegerv(PGL.VIEWPORT, savedViewport);

		try {
			pgl.bindFramebuffer(PGL.FRAMEBUFFER, renderFramebufferId);
			pgl.framebufferTexture2D(
					PGL.FRAMEBUFFER,
					PGL.COLOR_ATTACHMENT0,
					cubemapFaceTarget,
					textureId,
					0);
			logGlErrorIfAny(pgl, "native cubemap framebufferTexture2D attach");
			pgl.framebufferRenderbuffer(
					PGL.FRAMEBUFFER,
					PGL.DEPTH_ATTACHMENT,
					PGL.RENDERBUFFER,
					depthRenderbufferId);
			logGlErrorIfAny(pgl, "native cubemap framebufferRenderbuffer attach");
			pgl.drawBuffer(PGL.COLOR_ATTACHMENT0);
			logGlErrorIfAny(pgl, "native cubemap drawBuffer");
			ensureFramebufferComplete(pgl, PGL.FRAMEBUFFER, "render");
			pgl.viewport(0, 0, resolution, resolution);
			pgl.clearColor(0f, 0f, 0f, 0f);
			pgl.clearDepth(1.0f);
			pgl.clear(PGL.COLOR_BUFFER_BIT | PGL.DEPTH_BUFFER_BIT);
			logGlErrorIfAny(pgl, "native cubemap framebuffer clear");

			renderOperation.run();
			logGlErrorIfAny(pgl, "native cubemap scene render");
			pgl.flush();
			logGlErrorIfAny(pgl, "native cubemap framebuffer flush");
		} finally {
			pgl.bindFramebuffer(PGL.FRAMEBUFFER, renderFramebufferId);
			pgl.framebufferTexture2D(PGL.FRAMEBUFFER, PGL.COLOR_ATTACHMENT0, cubemapFaceTarget, 0, 0);
			pgl.framebufferRenderbuffer(PGL.FRAMEBUFFER, PGL.DEPTH_ATTACHMENT, PGL.RENDERBUFFER, 0);
			logGlErrorIfAny(pgl, "native cubemap framebuffer detach");
			pgl.bindFramebuffer(PGL.READ_FRAMEBUFFER, savedReadFramebuffer.get(0));
			pgl.bindFramebuffer(PGL.DRAW_FRAMEBUFFER, savedDrawFramebuffer.get(0));
			pgl.viewport(
					savedViewport.get(0),
					savedViewport.get(1),
					savedViewport.get(2),
					savedViewport.get(3));
			logGlErrorIfAny(pgl, "native cubemap framebuffer restore");
		}
	}

	private void logGlErrorIfAny(PGL pgl, String label) {
		if (!LogManager.isDebugEnabled() || glErrorLogsRemaining <= 0) {
			return;
		}
		for (int i = 0; i < MAX_GL_ERRORS_PER_CHECK; i++) {
			int error = pgl.getError();
			if (error == 0) {
				return;
			}
			glErrorLogsRemaining--;
			LOGGER.warning(label + ": OpenGL error 0x" + Integer.toHexString(error));
			if (glErrorLogsRemaining <= 0) {
				LOGGER.warning("Native cubemap GL error logging limit reached for textureId=" + textureId);
				return;
			}
		}
	}

	private static void ensureFramebufferComplete(PGL pgl, int target, String label) {
		int status = pgl.checkFramebufferStatus(target);
		if (status != PGL.FRAMEBUFFER_COMPLETE) {
			throw new IllegalStateException("Cubemap " + label
					+ " framebuffer is incomplete: 0x" + Integer.toHexString(status));
		}
	}

	@FunctionalInterface
	private interface GlStateOperation {
		void run();
	}
}
