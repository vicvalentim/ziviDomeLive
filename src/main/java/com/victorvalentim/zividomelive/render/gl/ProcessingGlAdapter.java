package com.victorvalentim.zividomelive.render.gl;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PConstants;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

import java.nio.IntBuffer;
import java.util.Objects;

/**
 * Narrow boundary for Processing/OpenGL operations used by the current renderer pipeline.
 *
 * <p>This class deliberately keeps ownership with Processing. It centralizes current
 * operations such as target allocation, texture presence checks, native cubemap binding,
 * CPU pixel readback, disposal, and capability queries. Native resources remain owned by the
 * specialized render/output classes that need them directly.</p>
 */
public final class ProcessingGlAdapter {
	private static final ProcessingGlAdapter DEFAULT = new ProcessingGlAdapter();
	private static final int GL_ACTIVE_TEXTURE = 0x84E0;
	private static final int GL_TEXTURE_BINDING_CUBE_MAP = 0x8514;
	private static final int GL_TEXTURE_CUBE_MAP_SEAMLESS = 0x884F;

	/**
	 * Reusable snapshot for one scoped native cubemap sampler binding.
	 *
	 * <p>The snapshot avoids per-frame buffer allocation and restores the caller's cubemap
	 * binding, active texture unit, and seamless-cubemap capability after the draw pass.</p>
	 */
	public static final class CubemapBindingState {
		private final IntBuffer savedActiveTexture = IntBuffer.allocate(1);
		private final IntBuffer savedCubemapBinding = IntBuffer.allocate(1);
		private int textureUnit;
		private boolean seamlessEnabled;
		private boolean bound;
	}

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
					pgl.getString(PGL.EXTENSIONS))
							.withShadingLanguageVersion(
									pgl.getString(PGL.SHADING_LANGUAGE_VERSION));
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
		/*
                 * Processing's generic OpenGL reporter is intentionally disabled
                 * for library-owned off-screen targets. ziviDomeLive performs
                 * contextual GL diagnostics through LogManager when required.
                 */
                openGlGraphics.hint(PConstants.DISABLE_OPENGL_ERRORS);
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
	 * Binds a native cubemap texture to the requested texture unit for samplerCube shaders.
	 *
	 * @param graphics active Processing graphics target
	 * @param target native cubemap target to bind
	 * @param textureUnit zero-based texture unit
	 */
	public void bindCubemapTexture(PGraphicsOpenGL graphics, CubemapTarget target, int textureUnit) {
		Objects.requireNonNull(target, "target");
		target.ensureAllocated();
		validateTextureUnit(textureUnit);
		withPgl(graphics, pgl -> {
			IntBuffer savedActiveTexture = IntBuffer.allocate(1);
			pgl.getIntegerv(GL_ACTIVE_TEXTURE, savedActiveTexture);
			pgl.activeTexture(PGL.TEXTURE0 + textureUnit);
			pgl.bindTexture(PGL.TEXTURE_CUBE_MAP, target.textureId());
			pgl.activeTexture(savedActiveTexture.get(0));
			return null;
		});
	}

	/**
	 * Binds a cubemap for a draw pass while snapshotting all GL state changed by the binding.
	 * The matching {@link #restoreCubemapTexture(PGraphicsOpenGL, CubemapBindingState)} call must
	 * run in a {@code finally} block on the same Processing target.
	 *
	 * @param graphics active Processing graphics target
	 * @param target native cubemap target to bind
	 * @param textureUnit zero-based texture unit
	 * @param state caller-owned reusable snapshot
	 */
	public void bindCubemapTextureScoped(
			PGraphicsOpenGL graphics,
			CubemapTarget target,
			int textureUnit,
			CubemapBindingState state) {
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(state, "state");
		target.ensureAllocated();
		validateTextureUnit(textureUnit);
		if (state.bound) {
			throw new IllegalStateException("Cubemap binding state is already active.");
		}

		withPgl(graphics, pgl -> {
			state.textureUnit = textureUnit;
			state.savedActiveTexture.clear();
			state.savedCubemapBinding.clear();
			pgl.getIntegerv(GL_ACTIVE_TEXTURE, state.savedActiveTexture);
			boolean snapshotComplete = false;
			try {
				pgl.activeTexture(PGL.TEXTURE0 + textureUnit);
				pgl.getIntegerv(GL_TEXTURE_BINDING_CUBE_MAP, state.savedCubemapBinding);
				state.seamlessEnabled = pgl.isEnabled(GL_TEXTURE_CUBE_MAP_SEAMLESS);
				snapshotComplete = true;
				pgl.enable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
				pgl.bindTexture(PGL.TEXTURE_CUBE_MAP, target.textureId());
				state.bound = true;
			} catch (RuntimeException error) {
				if (snapshotComplete) {
					try {
						pgl.bindTexture(
								PGL.TEXTURE_CUBE_MAP,
								state.savedCubemapBinding.get(0));
						restoreCapability(
								pgl,
								GL_TEXTURE_CUBE_MAP_SEAMLESS,
								state.seamlessEnabled);
					} catch (RuntimeException restoreError) {
						error.addSuppressed(restoreError);
					}
				}
				throw error;
			} finally {
				pgl.activeTexture(state.savedActiveTexture.get(0));
			}
			return null;
		});
	}

	/**
	 * Restores a cubemap sampler binding created by {@link #bindCubemapTextureScoped}.
	 * @param graphics active Processing graphics target
	 * @param state reusable snapshot populated by the scoped bind
	 */
	public void restoreCubemapTexture(
			PGraphicsOpenGL graphics,
			CubemapBindingState state) {
		Objects.requireNonNull(state, "state");
		if (!state.bound) {
			return;
		}

		try {
			withPgl(graphics, pgl -> {
				pgl.activeTexture(PGL.TEXTURE0 + state.textureUnit);
				try {
					pgl.bindTexture(PGL.TEXTURE_CUBE_MAP, state.savedCubemapBinding.get(0));
					restoreCapability(
							pgl,
							GL_TEXTURE_CUBE_MAP_SEAMLESS,
							state.seamlessEnabled);
				} finally {
					pgl.activeTexture(state.savedActiveTexture.get(0));
				}
				return null;
			});
		} finally {
			state.bound = false;
		}
	}

	private static void restoreCapability(PGL pgl, int capability, boolean enabled) {
		if (enabled) {
			pgl.enable(capability);
		} else {
			pgl.disable(capability);
		}
	}

	/**
	 * Unbinds any cubemap texture from the requested texture unit.
	 *
	 * @param graphics active Processing graphics target
	 * @param textureUnit zero-based texture unit
	 */
	public void unbindCubemapTexture(PGraphicsOpenGL graphics, int textureUnit) {
		validateTextureUnit(textureUnit);
		withPgl(graphics, pgl -> {
			IntBuffer savedActiveTexture = IntBuffer.allocate(1);
			pgl.getIntegerv(GL_ACTIVE_TEXTURE, savedActiveTexture);
			pgl.activeTexture(PGL.TEXTURE0 + textureUnit);
			pgl.bindTexture(PGL.TEXTURE_CUBE_MAP, 0);
			pgl.activeTexture(savedActiveTexture.get(0));
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

	static void validateTextureUnit(int textureUnit) {
		if (textureUnit < 0) {
			throw new IllegalArgumentException("Texture unit must be non-negative.");
		}
	}

	<T> T withPgl(PGraphicsOpenGL graphics, PglOperation<T> operation) {
		if (graphics == null) {
			throw new IllegalStateException("Processing OpenGL graphics target is not available.");
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

	@FunctionalInterface
	interface PglOperation<T> {
		T apply(PGL pgl);
	}
}
