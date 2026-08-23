package com.victorvalentim.zividomelive;

import com.jogamp.opengl.GL2ES2;
import com.victorvalentim.zividomelive.performance.GpuTimerArchitecture;
import com.victorvalentim.zividomelive.performance.GpuTimerBackend;
import com.victorvalentim.zividomelive.performance.GpuTimerPolicy;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PConstants;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PJOGL;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * Narrow boundary for Processing/OpenGL operations used by the current renderer pipeline.
 *
 * <p>This class deliberately keeps ownership with Processing. It centralizes current
 * operations such as target allocation, texture presence checks, native cubemap binding,
 * CPU pixel readback, disposal, and capability queries. Native resources remain owned by the
 * specialized render/output classes that need them directly.</p>
 */
final class ProcessingGlAdapter {
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
	static final class CubemapBindingState {
		private final IntBuffer savedActiveTexture = IntBuffer.allocate(1);
		private final IntBuffer savedCubemapBinding = IntBuffer.allocate(1);
		private int textureUnit;
		private boolean seamlessEnabled;
		private boolean bound;
	}

	/** Receives a completed asynchronous elapsed query without exposing native query IDs. */
	@FunctionalInterface
	interface GpuTimerResultConsumer {
		/**
		 * Accepts one query result.
		 *
		 * @param frameId absolute frame index associated with the query
		 * @param profilingSessionId profiling-session token associated with the query
		 * @param elapsedNanos GPU timestamp delta in nanoseconds
		 */
		void accept(long frameId, long profilingSessionId, long elapsedNanos);
	}

	/**
	 * Bounded asynchronous GPU timer-query pool owned by the Processing GL
	 * context. Queries are read only after {@code GL_QUERY_RESULT_AVAILABLE}; a saturated pool
	 * declines new samples instead of blocking the render thread.
	 *
	 * <p>The session is intentionally limited to one library-owned active interval. Processing's
	 * {@code beginPGL()} flushes queued renderer commands at each boundary, preserving command
	 * order without {@code glFinish()}.</p>
	 */
	static final class GpuTimerQuerySession implements AutoCloseable {
		private final PApplet parent;
		private final int[] queryIds;
		private final boolean[] pending;
		private final long[] frameIds;
		private final long[] profilingSessionIds;
		private final int[] availableScratch = new int[1];
		private final int[] counterBitsScratch = new int[1];
		private final long[] timestampScratch = new long[1];
		private final long[] durationScratch = new long[1];
		private final GpuTimerPolicy policy;
		private GpuTimerBackend backend = GpuTimerBackend.NONE;
		private GpuTimerArchitecture architecture = GpuTimerArchitecture.OTHER;
		private Object contextIdentity;
		private int activeSlot = -1;
		private boolean allocated;
		private boolean closed;

		private GpuTimerQuerySession(
				PApplet parent,
				int poolSize,
				GpuTimerPolicy policy) {
			this.parent = parent;
			this.policy = policy;
			this.queryIds = new int[poolSize * 2];
			this.pending = new boolean[poolSize];
			this.frameIds = new long[poolSize];
			this.profilingSessionIds = new long[poolSize];
		}

		/**
		 * Collects ready results and begins one query when a pool slot is free.
		 *
		 * @param frameId absolute frame index to associate with this interval
		 * @param profilingSessionId profiling-session token for stale-result rejection
		 * @param consumer preallocated result consumer
		 * @return {@code true} when an elapsed query was begun for this frame
		 */
		public boolean begin(
				long frameId,
				long profilingSessionId,
				GpuTimerResultConsumer consumer) {
			Objects.requireNonNull(consumer, "consumer");
			if (closed) {
				throw new IllegalStateException("GPU timer query session is closed.");
			}
			if (activeSlot >= 0) {
				throw new IllegalStateException("A GPU timer interval is already active.");
			}
			PGraphicsOpenGL graphics = requireGraphics();
			PGL pgl = graphics.beginPGL();
			try {
				if (pgl == null) {
					throw new IllegalStateException("Processing PGL context is not available.");
				}
				GL2ES2 gl = ensureContext(pgl);
				collectAvailable(gl, consumer);
				int freeSlot = findFreeSlot();
				if (freeSlot < 0) {
					return false;
				}
				if (backend == GpuTimerBackend.TIMESTAMP_PAIR) {
					gl.glQueryCounter(startQueryId(freeSlot), GL2ES2.GL_TIMESTAMP);
				} else {
					gl.glBeginQuery(GL2ES2.GL_TIME_ELAPSED, startQueryId(freeSlot));
				}
				frameIds[freeSlot] = frameId;
				profilingSessionIds[freeSlot] = profilingSessionId;
				activeSlot = freeSlot;
				return true;
			} finally {
				graphics.endPGL();
			}
		}

		/** Ends the active interval with a timestamp, leaving it pending for a later read. */
		public void end() {
			if (closed || activeSlot < 0) {
				return;
			}
			PGraphicsOpenGL graphics = requireGraphics();
			PGL pgl = graphics.beginPGL();
			try {
				if (pgl == null) {
					throw new IllegalStateException("Processing PGL context is not available.");
				}
				PJOGL pjogl = requirePjogl(pgl);
				if (!allocated || pjogl.context != contextIdentity) {
					abandonContext();
					return;
				}
				GL2ES2 gl = requireGl(pjogl);
				int endingSlot = activeSlot;
				try {
					if (backend == GpuTimerBackend.TIMESTAMP_PAIR) {
						gl.glQueryCounter(endQueryId(endingSlot), GL2ES2.GL_TIMESTAMP);
					} else {
						gl.glEndQuery(GL2ES2.GL_TIME_ELAPSED);
					}
					pending[endingSlot] = true;
				} finally {
					activeSlot = -1;
				}
			} finally {
				graphics.endPGL();
			}
		}

		/**
		 * Polls all ready results once and never waits for unavailable queries.
		 *
		 * @param consumer preallocated result consumer
		 */
		public void collectAvailable(GpuTimerResultConsumer consumer) {
			Objects.requireNonNull(consumer, "consumer");
			if (closed || !allocated) {
				return;
			}
			PGraphicsOpenGL graphics = requireGraphics();
			PGL pgl = graphics.beginPGL();
			try {
				if (pgl == null) {
					throw new IllegalStateException("Processing PGL context is not available.");
				}
				GL2ES2 gl = ensureContext(pgl);
				collectAvailable(gl, consumer);
			} finally {
				graphics.endPGL();
			}
		}

		/** @return ended query results that have not yet been collected */
		public int pendingResultCount() {
			int count = 0;
			for (boolean value : pending) {
				if (value) count++;
			}
			return count;
		}

		/** @return backend selected after the first successful begin call */
		public GpuTimerBackend getBackend() {
			return backend;
		}

		/** @return normalized platform architecture used by backend selection */
		public GpuTimerArchitecture getArchitecture() {
			return architecture;
		}

		/**
		 * Deletes timer-query objects only when their owning context is current. Pending results are
		 * abandoned without waiting; context-loss objects are left to the destroyed context.
		 */
		@Override
		public void close() {
			if (closed) {
				return;
			}
			try {
				if (allocated) {
					PGraphicsOpenGL graphics = requireGraphics();
					PGL pgl = graphics.beginPGL();
					try {
						if (pgl == null) {
							throw new IllegalStateException("Processing PGL context is not available.");
						}
						PJOGL pjogl = requirePjogl(pgl);
						if (pjogl.context == contextIdentity) {
							GL2ES2 gl = requireGl(pjogl);
							if (activeSlot >= 0
									&& backend == GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE) {
								gl.glEndQuery(GL2ES2.GL_TIME_ELAPSED);
								activeSlot = -1;
							}
							gl.glDeleteQueries(queryIds.length, queryIds, 0);
						}
					} finally {
						graphics.endPGL();
					}
				}
			} finally {
				abandonContext();
				closed = true;
			}
		}

		private GL2ES2 ensureContext(PGL pgl) {
			PJOGL pjogl = requirePjogl(pgl);
			GL2ES2 gl = requireGl(pjogl);
			if (!allocated || pjogl.context != contextIdentity) {
				abandonContext();
				ProcessingGlCapabilities capabilities = ProcessingGlCapabilities.fromOpenGlStrings(
						pgl.getString(PGL.VERSION),
						pgl.getString(PGL.VENDOR),
						pgl.getString(PGL.RENDERER),
						pgl.getString(PGL.EXTENSIONS));
				if (!capabilities.supportsGpuTimerQuery()) {
					throw new IllegalStateException(
							"Desktop OpenGL GPU timer queries are not supported by the active context.");
				}
				architecture = GpuTimerArchitecture.detect(
						System.getProperty("os.name"),
						System.getProperty("os.arch"),
						capabilities.vendor(),
						capabilities.renderer());
				counterBitsScratch[0] = 0;
				gl.glGetQueryiv(
						GL2ES2.GL_TIMESTAMP,
						GL2ES2.GL_QUERY_COUNTER_BITS,
						counterBitsScratch,
						0);
				int timestampCounterBits = counterBitsScratch[0];
				counterBitsScratch[0] = 0;
				gl.glGetQueryiv(
						GL2ES2.GL_TIME_ELAPSED,
						GL2ES2.GL_QUERY_COUNTER_BITS,
						counterBitsScratch,
						0);
				int elapsedCounterBits = counterBitsScratch[0];
				backend = policy.selectBackend(
						architecture,
						timestampCounterBits,
						elapsedCounterBits);
				if (backend == GpuTimerBackend.NONE) {
					throw new IllegalStateException("No GPU timer backend satisfies policy "
							+ policy + " on " + architecture + " (timestampBits="
							+ timestampCounterBits + ", elapsedBits=" + elapsedCounterBits + ").");
				}
				if (backend == GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE) {
					counterBitsScratch[0] = 0;
					gl.glGetQueryiv(
							GL2ES2.GL_TIME_ELAPSED,
							GL2ES2.GL_CURRENT_QUERY,
							counterBitsScratch,
							0);
					if (counterBitsScratch[0] != 0) {
						throw new IllegalStateException(
								"GL_TIME_ELAPSED is already owned by another query.");
					}
				}
				gl.glGenQueries(queryIds.length, queryIds, 0);
				contextIdentity = pjogl.context;
				allocated = true;
			}
			return gl;
		}

		private void collectAvailable(GL2ES2 gl, GpuTimerResultConsumer consumer) {
			for (int slot = 0; slot < pending.length; slot++) {
				if (!pending[slot]) {
					continue;
				}
				availableScratch[0] = 0;
				int resultQueryId = backend == GpuTimerBackend.TIMESTAMP_PAIR
						? endQueryId(slot)
						: startQueryId(slot);
				gl.glGetQueryObjectiv(
						resultQueryId,
						GL2ES2.GL_QUERY_RESULT_AVAILABLE,
						availableScratch,
						0);
				if (availableScratch[0] == 0) {
					continue;
				}
				long elapsedNanos;
				if (backend == GpuTimerBackend.TIMESTAMP_PAIR) {
					availableScratch[0] = 0;
					gl.glGetQueryObjectiv(
							startQueryId(slot),
							GL2ES2.GL_QUERY_RESULT_AVAILABLE,
							availableScratch,
							0);
					if (availableScratch[0] == 0) continue;
					gl.glGetQueryObjectui64v(
							startQueryId(slot), GL2ES2.GL_QUERY_RESULT, timestampScratch, 0);
					gl.glGetQueryObjectui64v(
							endQueryId(slot), GL2ES2.GL_QUERY_RESULT, durationScratch, 0);
					if (durationScratch[0] <= timestampScratch[0]) {
						throw new IllegalStateException(
								"The GPU timestamp counter did not advance across the render pipeline.");
					}
					elapsedNanos = durationScratch[0] - timestampScratch[0];
				} else {
					gl.glGetQueryObjectui64v(
							startQueryId(slot), GL2ES2.GL_QUERY_RESULT, durationScratch, 0);
					if (durationScratch[0] <= 0L) {
						throw new IllegalStateException(
								"The GPU elapsed query returned no useful duration.");
					}
					elapsedNanos = durationScratch[0];
				}
				pending[slot] = false;
				consumer.accept(
						frameIds[slot],
						profilingSessionIds[slot],
						elapsedNanos);
			}
		}

		private int findFreeSlot() {
			for (int slot = 0; slot < pending.length; slot++) {
				if (!pending[slot] && slot != activeSlot) {
					return slot;
				}
			}
			return -1;
		}

		private int startQueryId(int slot) {
			return queryIds[slot * 2];
		}

		private int endQueryId(int slot) {
			return queryIds[slot * 2 + 1];
		}

		private void abandonContext() {
			Arrays.fill(queryIds, 0);
			Arrays.fill(pending, false);
			Arrays.fill(frameIds, 0L);
			Arrays.fill(profilingSessionIds, 0L);
			contextIdentity = null;
			backend = GpuTimerBackend.NONE;
			architecture = GpuTimerArchitecture.OTHER;
			activeSlot = -1;
			allocated = false;
		}

		private static PJOGL requirePjogl(PGL pgl) {
			if (!(pgl instanceof PJOGL pjogl) || pjogl.context == null) {
				throw new IllegalStateException("Processing PJOGL context is not available.");
			}
			return pjogl;
		}

		private static GL2ES2 requireGl(PJOGL pjogl) {
			if (pjogl.gl == null || !pjogl.gl.isGL2ES2()) {
				throw new IllegalStateException("The active JOGL profile does not expose GL2ES2 queries.");
			}
			return pjogl.gl.getGL2ES2();
		}

		private PGraphicsOpenGL requireGraphics() {
			if (!(parent.g instanceof PGraphicsOpenGL graphics)) {
				throw new IllegalStateException("Processing OpenGL renderer is not available.");
			}
			return graphics;
		}
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
	 * Creates a lazy, bounded safe GPU timer-query session. Native query IDs remain private to
	 * the adapter and are allocated only on the first render-thread {@code begin()} call.
	 *
	 * @param parent Processing parent that owns the OpenGL context
	 * @param poolSize maximum number of in-flight asynchronous queries
	 * @return timer-query session
	 */
	public GpuTimerQuerySession createGpuTimerQuerySession(PApplet parent, int poolSize) {
		return createGpuTimerQuerySession(parent, poolSize, GpuTimerPolicy.SAFE);
	}

	/**
	 * Creates a timer session using the requested ownership/fallback policy.
	 *
	 * @param parent Processing parent that owns the OpenGL context
	 * @param poolSize maximum number of in-flight asynchronous queries
	 * @param policy timer backend selection and elapsed-query ownership policy
	 * @return timer-query session
	 */
	public GpuTimerQuerySession createGpuTimerQuerySession(
			PApplet parent,
			int poolSize,
			GpuTimerPolicy policy) {
		if (parent == null) {
			throw new IllegalArgumentException("Processing parent must not be null.");
		}
		if (poolSize < 2 || poolSize > 64) {
			throw new IllegalArgumentException("GPU timer query pool size must be between 2 and 64.");
		}
		if (policy == null) {
			throw new IllegalArgumentException("GPU timer policy must not be null.");
		}
		return new GpuTimerQuerySession(parent, poolSize, policy);
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

			ProcessingGlCapabilities capabilities =
					ProcessingGlCapabilities.fromOpenGlStrings(
									pgl.getString(PGL.VERSION),
									pgl.getString(PGL.VENDOR),
									pgl.getString(PGL.RENDERER),
									pgl.getString(PGL.EXTENSIONS))
							.withShadingLanguageVersion(
									pgl.getString(PGL.SHADING_LANGUAGE_VERSION));

			if (pgl instanceof PJOGL pjogl && pjogl.gl != null) {
				var profile = pjogl.gl.getGLProfile();

				if (profile != null) {
					capabilities = capabilities.withJoglProfile(
							profile.toString(),
							profile.isHardwareRasterizer());
				}
			}

			return capabilities;
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
