package com.victorvalentim.zividomelive.render.gl;

import com.victorvalentim.zividomelive.render.camera.CubemapFace;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;

import java.nio.IntBuffer;
import java.util.Objects;

/**
 * Narrow boundary for Processing/OpenGL operations used by the current renderer pipeline.
 *
 * <p>This class deliberately keeps ownership with Processing. It centralizes current
 * operations such as target allocation, texture presence checks, CPU pixel readback, disposal,
 * and capability queries; native cubemap, PBO, and fence objects are introduced only by later
 * PRs that need them directly.</p>
 */
public final class ProcessingGlAdapter {
	private static final ProcessingGlAdapter DEFAULT = new ProcessingGlAdapter();
	private static final int GL_READ_FRAMEBUFFER_BINDING = 0x8CAA;
	private static final int GL_DRAW_FRAMEBUFFER_BINDING = 0x8CA6;
	private static final ThreadLocal<IntBuffer> READ_FRAMEBUFFER_SCRATCH =
			ThreadLocal.withInitial(() -> IntBuffer.allocate(1));
	private static final ThreadLocal<IntBuffer> DRAW_FRAMEBUFFER_SCRATCH =
			ThreadLocal.withInitial(() -> IntBuffer.allocate(1));

	private ProcessingGlAdapter() {
	}

	/**
	 * Returns the shared adapter instance.
	 *
	 * @return shared Processing GL adapter
	 */
	public static ProcessingGlAdapter getDefault() {
		return DEFAULT;
	}

	/**
	 * Queries capabilities from the active Processing OpenGL renderer.
	 *
	 * @param parent Processing parent with an initialized renderer
	 * @return capabilities snapshot, or unavailable capabilities when no PGL context exists
	 */
	public ProcessingGlCapabilities queryCapabilities(PApplet parent) {
		if (parent == null || !(parent.g instanceof PGraphicsOpenGL graphics)) {
			return ProcessingGlCapabilities.unavailable();
		}

		PGL pgl = graphics.beginPGL();
		try {
			if (pgl == null) {
				return ProcessingGlCapabilities.unavailable();
			}
			return ProcessingGlCapabilities.fromOpenGlStrings(
					pgl.getString(PGL.VERSION),
					pgl.getString(PGL.VENDOR),
					pgl.getString(PGL.RENDERER),
					pgl.getString(PGL.EXTENSIONS));
		} finally {
			graphics.endPGL();
		}
	}

	<T> T withPgl(PApplet parent, PglOperation<T> operation) {
		if (parent == null || !(parent.g instanceof PGraphicsOpenGL graphics)) {
			throw new IllegalStateException("Processing OpenGL renderer is not available.");
		}
		PGL pgl = graphics.beginPGL();
		try {
			if (pgl == null) {
				throw new IllegalStateException("Processing PGL context is not available.");
			}
			return operation.apply(pgl);
		} finally {
			graphics.endPGL();
		}
	}

	/**
	 * Creates a Processing OpenGL graphics target.
	 *
	 * @param parent Processing parent used to allocate the target
	 * @param width target width in pixels
	 * @param height target height in pixels
	 * @param renderer Processing renderer constant such as {@link PApplet#P2D} or {@link PApplet#P3D}
	 * @return allocated OpenGL graphics target
	 */
	public PGraphicsOpenGL createGraphics(PApplet parent, int width, int height, String renderer) {
		if (parent == null) {
			throw new IllegalArgumentException("Processing parent must not be null.");
		}
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("Graphics dimensions must be positive.");
		}
		PGraphics graphics = parent.createGraphics(width, height, renderer);
		if (!(graphics instanceof PGraphicsOpenGL openGlGraphics)) {
			throw new IllegalStateException("Processing did not create an OpenGL graphics target.");
		}
		return openGlGraphics;
	}

	/**
	 * Reports whether a graphics target currently exposes a Processing texture.
	 *
	 * @param graphics target to inspect
	 * @return {@code true} when the target has a texture object
	 */
	public boolean hasTexture(PGraphicsOpenGL graphics) {
		return graphics != null && graphics.getTexture() != null;
	}

	/**
	 * Loads and copies Processing ARGB pixels into a caller-owned buffer.
	 *
	 * @param source rendered Processing target
	 * @param destination caller-owned ARGB destination buffer
	 * @param pixelCount number of pixels to copy
	 * @return {@code true} when the source pixel buffer was complete and copied
	 */
	public boolean copyPixels(PGraphicsOpenGL source, int[] destination, int pixelCount) {
		if (source == null || destination == null || pixelCount < 0 || destination.length < pixelCount) {
			return false;
		}
		source.loadPixels();
		if (source.pixels == null || source.pixels.length < pixelCount) {
			return false;
		}
		System.arraycopy(source.pixels, 0, destination, 0, pixelCount);
		return true;
	}

	/**
	 * Copies a rendered Processing texture into a native cubemap face using GPU-side FBO blit.
	 *
	 * @param parent Processing parent with the active GL context
	 * @param source rendered Processing graphics target
	 * @param target native cubemap target
	 * @param face target cubemap face
	 */
	public void copyTextureToCubemapFace(
			PApplet parent,
			PGraphicsOpenGL source,
			CubemapTarget target,
			CubemapFace face) {
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(face, "face");
		target.ensureAllocated();

		Texture sourceTexture = source.getTexture();
		if (sourceTexture == null || sourceTexture.glName == 0) {
			throw new IllegalStateException("Processing source texture is not available for cubemap copy.");
		}
		validateCubemapCopyDimensions(source.width, source.height, target.resolution());
		int glFaceTarget = CubemapTarget.glTargetFor(face);

		withPgl(parent, pgl -> {
			blitTextureToCubemapFace(
					pgl,
					sourceTexture.glTarget,
					sourceTexture.glName,
					target.textureId(),
					target.readFramebufferId(),
					target.drawFramebufferId(),
					glFaceTarget,
					target.resolution());
			return null;
		});
	}

	/**
	 * Disposes a Processing graphics target when present.
	 *
	 * @param graphics graphics target to dispose, may be {@code null}
	 */
	public void dispose(PGraphics graphics) {
		if (graphics != null) {
			graphics.dispose();
		}
	}

	static void validateCubemapCopyDimensions(int sourceWidth, int sourceHeight, int targetResolution) {
		if (sourceWidth <= 0 || sourceHeight <= 0) {
			throw new IllegalArgumentException("Source dimensions must be positive.");
		}
		if (sourceWidth != targetResolution || sourceHeight != targetResolution) {
			throw new IllegalArgumentException("Source face dimensions must match cubemap resolution.");
		}
	}

	private static void blitTextureToCubemapFace(
			PGL pgl,
			int sourceTextureTarget,
			int sourceTextureId,
			int cubemapTextureId,
			int readFramebufferId,
			int drawFramebufferId,
			int cubemapFaceTarget,
			int resolution) {
		IntBuffer savedReadFramebuffer = READ_FRAMEBUFFER_SCRATCH.get();
		IntBuffer savedDrawFramebuffer = DRAW_FRAMEBUFFER_SCRATCH.get();
		savedReadFramebuffer.clear();
		savedDrawFramebuffer.clear();
		pgl.getIntegerv(GL_READ_FRAMEBUFFER_BINDING, savedReadFramebuffer);
		pgl.getIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, savedDrawFramebuffer);
		int savedReadFramebufferId = savedReadFramebuffer.get(0);
		int savedDrawFramebufferId = savedDrawFramebuffer.get(0);

		try {
			pgl.bindFramebuffer(PGL.READ_FRAMEBUFFER, readFramebufferId);
			pgl.framebufferTexture2D(
					PGL.READ_FRAMEBUFFER,
					PGL.COLOR_ATTACHMENT0,
					sourceTextureTarget,
					sourceTextureId,
					0);
			pgl.readBuffer(PGL.COLOR_ATTACHMENT0);
			ensureFramebufferComplete(pgl, PGL.READ_FRAMEBUFFER, "read");

			pgl.bindFramebuffer(PGL.DRAW_FRAMEBUFFER, drawFramebufferId);
			pgl.framebufferTexture2D(
					PGL.DRAW_FRAMEBUFFER,
					PGL.COLOR_ATTACHMENT0,
					cubemapFaceTarget,
					cubemapTextureId,
					0);
			pgl.drawBuffer(PGL.COLOR_ATTACHMENT0);
			ensureFramebufferComplete(pgl, PGL.DRAW_FRAMEBUFFER, "draw");

			pgl.blitFramebuffer(
					0, 0, resolution, resolution,
					0, 0, resolution, resolution,
					PGL.COLOR_BUFFER_BIT,
					PGL.NEAREST);
		} finally {
			pgl.bindFramebuffer(PGL.READ_FRAMEBUFFER, readFramebufferId);
			pgl.framebufferTexture2D(PGL.READ_FRAMEBUFFER, PGL.COLOR_ATTACHMENT0, sourceTextureTarget, 0, 0);
			pgl.bindFramebuffer(PGL.DRAW_FRAMEBUFFER, drawFramebufferId);
			pgl.framebufferTexture2D(PGL.DRAW_FRAMEBUFFER, PGL.COLOR_ATTACHMENT0, cubemapFaceTarget, 0, 0);
			pgl.bindFramebuffer(PGL.READ_FRAMEBUFFER, savedReadFramebufferId);
			pgl.bindFramebuffer(PGL.DRAW_FRAMEBUFFER, savedDrawFramebufferId);
		}
	}

	private static void ensureFramebufferComplete(PGL pgl, int target, String label) {
		int status = pgl.checkFramebufferStatus(target);
		if (status != PGL.FRAMEBUFFER_COMPLETE) {
			throw new IllegalStateException("Cubemap copy " + label
					+ " framebuffer is incomplete: 0x" + Integer.toHexString(status));
		}
	}

	@FunctionalInterface
	interface PglOperation<T> {
		T apply(PGL pgl);
	}
}
