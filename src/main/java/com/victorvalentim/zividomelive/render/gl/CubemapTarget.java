package com.victorvalentim.zividomelive.render.gl;

import com.victorvalentim.zividomelive.render.camera.CubemapFace;
import processing.core.PApplet;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

import java.nio.IntBuffer;
import java.util.Objects;

/**
 * Native OpenGL cubemap texture owned by the spherical rendering pipeline.
 *
 * <p>This target owns the {@code GL_TEXTURE_CUBE_MAP} allocation and reusable
 * framebuffer objects used to copy rendered Processing face textures into it.
 * Sampling it from projection shaders is introduced by later migration steps.</p>
 */
public final class CubemapTarget implements AutoCloseable {
	private static final int GL_TEXTURE_CUBE_MAP_SEAMLESS = 0x884F;

	private final PApplet parent;
	private final ProcessingGlAdapter glAdapter;
	private final int resolution;
	private int readFramebufferId;
	private int drawFramebufferId;
	private int textureId;

	private CubemapTarget(
			PApplet parent,
			ProcessingGlAdapter glAdapter,
			int resolution,
			int readFramebufferId,
			int drawFramebufferId,
			int textureId) {
		this.parent = Objects.requireNonNull(parent, "parent");
		this.glAdapter = Objects.requireNonNull(glAdapter, "glAdapter");
		this.resolution = resolution;
		this.readFramebufferId = readFramebufferId;
		this.drawFramebufferId = drawFramebufferId;
		this.textureId = textureId;
	}

	/**
	 * Allocates a native OpenGL cubemap texture with conservative 2.0 defaults
	 * and reusable framebuffer objects for GPU-side face copies.
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
		if (!capabilities.supportsCubemap()) {
			throw new IllegalStateException("OpenGL cubemap textures are not supported.");
		}
		if (!capabilities.supportsFramebuffer()) {
			throw new IllegalStateException("OpenGL framebuffer objects are not supported.");
		}

		int[] resources = glAdapter.withPgl(parent, pgl -> allocateResources(pgl, resolution, capabilities));
		return new CubemapTarget(parent, glAdapter, resolution, resources[1], resources[2], resources[0]);
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
	 * Copies a rendered Processing face texture into the matching cubemap face.
	 *
	 * @param source rendered Processing face
	 * @param face target cubemap face
	 */
	public void copyFaceFrom(PGraphicsOpenGL source, CubemapFace face) {
		ensureAllocated();
		glAdapter.copyTextureToCubemapFace(parent, source, this, face);
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
	 * Releases the owned cubemap texture and copy framebuffer ids.
	 */
	@Override
	public void close() {
		dispose();
	}

	/**
	 * Releases the owned cubemap texture and copy framebuffer ids.
	 */
	public void dispose() {
		int texture = textureId;
		int readFramebuffer = readFramebufferId;
		int drawFramebuffer = drawFramebufferId;
		if (texture == 0 && readFramebuffer == 0 && drawFramebuffer == 0) {
			return;
		}
		textureId = 0;
		readFramebufferId = 0;
		drawFramebufferId = 0;
		glAdapter.withPgl(parent, pgl -> {
			if (readFramebuffer != 0 || drawFramebuffer != 0) {
				IntBuffer framebufferBuffer = IntBuffer.allocate(2);
				framebufferBuffer.put(0, readFramebuffer);
				framebufferBuffer.put(1, drawFramebuffer);
				pgl.deleteFramebuffers(2, framebufferBuffer);
			}
			if (texture != 0) {
				IntBuffer textureBuffer = IntBuffer.allocate(1);
				textureBuffer.put(0, texture);
				pgl.deleteTextures(1, textureBuffer);
			}
			return null;
		});
	}

	int readFramebufferId() {
		return readFramebufferId;
	}

	int drawFramebufferId() {
		return drawFramebufferId;
	}

	void ensureAllocated() {
		if (!isAllocated() || readFramebufferId == 0 || drawFramebufferId == 0) {
			throw new IllegalStateException("Cubemap target has been disposed.");
		}
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
		int textureId = allocateTexture(pgl, resolution, capabilities);
		try {
			int[] framebufferIds = allocateFramebuffers(pgl);
			return new int[]{textureId, framebufferIds[0], framebufferIds[1]};
		} catch (RuntimeException error) {
			IntBuffer textureBuffer = IntBuffer.allocate(1);
			textureBuffer.put(0, textureId);
			pgl.deleteTextures(1, textureBuffer);
			throw error;
		}
	}

	private static int[] allocateFramebuffers(PGL pgl) {
		IntBuffer framebufferBuffer = IntBuffer.allocate(2);
		pgl.genFramebuffers(2, framebufferBuffer);
		int readFramebufferId = framebufferBuffer.get(0);
		int drawFramebufferId = framebufferBuffer.get(1);
		if (readFramebufferId == 0 || drawFramebufferId == 0) {
			pgl.deleteFramebuffers(2, framebufferBuffer);
			throw new IllegalStateException("OpenGL did not allocate cubemap copy framebuffers.");
		}
		return new int[]{readFramebufferId, drawFramebufferId};
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
			pgl.bindTexture(PGL.TEXTURE_CUBE_MAP, textureId);
			pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_MIN_FILTER, PGL.LINEAR);
			pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_MAG_FILTER, PGL.LINEAR);
			pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_S, PGL.CLAMP_TO_EDGE);
			pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_T, PGL.CLAMP_TO_EDGE);
			pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_R, PGL.CLAMP_TO_EDGE);

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

			if (capabilities.supportsSeamlessCubemap()) {
				pgl.enable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
			}
			return textureId;
		} catch (RuntimeException error) {
			textureBuffer.put(0, textureId);
			pgl.deleteTextures(1, textureBuffer);
			throw error;
		} finally {
			pgl.bindTexture(PGL.TEXTURE_CUBE_MAP, 0);
		}
	}
}
