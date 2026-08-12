package com.victorvalentim.zividomelive.render.gl;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

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
	 * Disposes a Processing graphics target when present.
	 *
	 * @param graphics graphics target to dispose, may be {@code null}
	 */
	public void dispose(PGraphics graphics) {
		if (graphics != null) {
			graphics.dispose();
		}
	}
}
