package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.support.LogManager;
import com.victorvalentim.zividomelive.zividomelive;
import me.walkerknapp.devolay.*;
import processing.core.PConstants;
import codeanticode.syphon.SyphonServer;
import processing.opengl.PGraphicsOpenGL;
import spout.Spout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages frame output to NDI, Spout (Windows), and Syphon (macOS).
 *
 * <h3>Hot-path design</h3>
 * <ul>
 *   <li>Syphon and Spout are sent first — GPU-to-GPU share, no pixel copy, on the draw thread.
 *   <li>NDI uses a producer-consumer model with {@value NDI_SLOT_COUNT} typed slots:
 *       <ol>
 *         <li>Draw thread: check slot availability <em>before</em> {@code loadPixels()},
 *             then {@code System.arraycopy()} raw ARGB pixels into the slot.
 *         <li>Dedicated NDI worker: ARGB→RGBA conversion, frame configuration, and
 *             {@code sendVideoFrame()} — all off the draw thread.
 *         <li>Worker returns slot to the free pool when done.
 *       </ol>
 *   <li>If no slot is free when the draw thread checks, the frame is dropped
 *       ({@link #getNdiDroppedFrames()}) without calling {@code loadPixels()}.
 *   <li>Graphics references are resolved per-frame so they remain valid after
 *       {@code resetGraphics()}.
 *   <li>The NDI worker runs in a dedicated single-thread executor that is created on
 *       NDI activation and shut down on deactivation, preventing stale tasks after restart.
 * </ul>
 */
public class OutputManager implements PConstants {

	/** Supported output types. */
	public enum OutputType {
		/** NDI output (cross-platform, pixel-copy path). */
		NDI,
		/** Spout output (Windows only, GPU texture share). */
		SPOUT,
		/** Syphon output (macOS only, GPU texture share). */
		SYPHON
	}

	private static final int NDI_SLOT_COUNT = 3;

	// -------------------------------------------------------------------------
	// NDI slot — owns pixel data from copy to send; exclusive ownership via queues.
	// -------------------------------------------------------------------------

	private static final class NdiSlot implements AutoCloseable {
		int[] argbPixels = new int[0];
		ByteBuffer rgbaBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.LITTLE_ENDIAN);
		final DevolayVideoFrame frame;
		int width;
		int height;

		NdiSlot() { frame = new DevolayVideoFrame(); }

		/** Copies src into this slot and records dimensions. Resizes buffers if needed. */
		void prepare(int[] src, int w, int h) {
			int count = w * h;
			if (argbPixels.length < count) {
				argbPixels = new int[count];
				rgbaBuffer = ByteBuffer.allocateDirect(count * 4).order(ByteOrder.LITTLE_ENDIAN);
			}
			System.arraycopy(src, 0, argbPixels, 0, count);
			this.width  = w;
			this.height = h;
		}

		@Override
		public void close() {
			try { frame.close(); } catch (Exception ignored) {}
		}
	}

	private final Logger logger = LogManager.getLogger();
	private final Map<OutputType, zividomelive.ViewType> outputViews;
	private final zividomelive parent;

	private DevolaySender ndiSender;
	private Spout spoutSender;
	private SyphonServer syphonServer;

	private volatile boolean ndiEnabled = false;
	private boolean spoutEnabled  = false;
	private boolean syphonEnabled = false;

	private final boolean isMacOS;
	private final boolean isWindows;

	// NDI slot queues — capacity enforces backpressure without blocking the draw thread.
	private final ArrayBlockingQueue<NdiSlot> freeSlots  = new ArrayBlockingQueue<>(NDI_SLOT_COUNT);
	private final ArrayBlockingQueue<NdiSlot> readySlots = new ArrayBlockingQueue<>(NDI_SLOT_COUNT - 1);

	// Dedicated NDI worker — one instance per activation cycle; prevents stale tasks after restart.
	private ExecutorService ndiWorkerExecutor;

	// NDI frame rate metadata (set via setNdiFrameRate; default 60/1)
	private int ndiFrameRateNum = 60;
	private int ndiFrameRateDen = 1;

	// NDI metrics
	private final AtomicLong ndiCaptured = new AtomicLong(0);
	private final AtomicLong ndiSent     = new AtomicLong(0);
	private final AtomicLong ndiDropped  = new AtomicLong(0);

	// Spout state — reset to 0 on init/shutdown to force explicit createSender on next send.
	private int spoutLastWidth  = 0;
	private int spoutLastHeight = 0;

	// setView() deprecation: warn once to avoid log spam.
	private boolean setViewWarningLogged = false;

	/**
	 * Constructs the OutputManager.
	 *
	 * @param parent the zividomelive instance
	 */
	public OutputManager(zividomelive parent) {
		this.parent = parent;
		this.outputViews = new EnumMap<>(OutputType.class);
		for (OutputType type : OutputType.values()) {
			outputViews.put(type, zividomelive.ViewType.FISHEYE_DOMEMASTER);
		}
		String osName = System.getProperty("os.name").toLowerCase();
		this.isMacOS   = osName.contains("mac");
		this.isWindows = osName.contains("win");
	}

	// -------------------------------------------------------------------------
	// View management
	// -------------------------------------------------------------------------

	/**
	 * Returns the view type configured for a specific output type.
	 *
	 * @param outputType the output type to query
	 * @return the ViewType, or FISHEYE_DOMEMASTER if not set
	 */
	public zividomelive.ViewType getViewForOutput(OutputType outputType) {
		return outputViews.getOrDefault(outputType, zividomelive.ViewType.FISHEYE_DOMEMASTER);
	}

	/**
	 * Configures which view a specific output type should send.
	 *
	 * @param outputType the output type to configure
	 * @param viewType   the ViewType to assign
	 */
	public void setViewForOutput(OutputType outputType, zividomelive.ViewType viewType) {
		if (outputType != null && viewType != null) {
			outputViews.put(outputType, viewType);
			logger.info("Set view for " + outputType + " to " + viewType);
		}
	}

	/**
	 * Legacy single-view setter — kept for API compatibility only.  Logs a one-time warning.
	 *
	 * @param viewType ignored
	 * @deprecated Use {@link #setViewForOutput(OutputType, zividomelive.ViewType)} per output.
	 */
	@Deprecated
	public void setView(zividomelive.ViewType viewType) {
		if (!setViewWarningLogged) {
			logger.warning("setView() is deprecated and has no effect. "
					+ "Use setViewForOutput() per output type.");
			setViewWarningLogged = true;
		}
	}

	/**
	 * Returns {@code true} if at least one enabled output is configured to receive
	 * the given view type.  Used by {@code updateRenderViews()} to ensure a view is
	 * rendered even when it is not the active preview mode.
	 *
	 * @param viewType the view type to check
	 * @return true if any enabled output needs this view
	 */
	public boolean requiresView(zividomelive.ViewType viewType) {
		if (viewType == null) return false;
		if (ndiEnabled    && getViewForOutput(OutputType.NDI)    == viewType) return true;
		if (spoutEnabled  && getViewForOutput(OutputType.SPOUT)  == viewType) return true;
		if (syphonEnabled && getViewForOutput(OutputType.SYPHON) == viewType) return true;
		return false;
	}

	// -------------------------------------------------------------------------
	// Per-frame graphics resolution
	// -------------------------------------------------------------------------

	private PGraphicsOpenGL resolveGraphics(OutputType type) {
		return resolveGraphicsForView(
				outputViews.getOrDefault(type, zividomelive.ViewType.FISHEYE_DOMEMASTER));
	}

	private PGraphicsOpenGL resolveGraphicsForView(zividomelive.ViewType viewType) {
		try {
			switch (viewType) {
				case FISHEYE_DOMEMASTER:
					return parent.getFisheyeDomemaster() != null
							? parent.getFisheyeDomemaster().getDomemasterGraphics() : null;
				case EQUIRECTANGULAR:
					return parent.getEquirectangularRenderer() != null
							? parent.getEquirectangularRenderer().getEquirectangular() : null;
				case CUBEMAP:
					return parent.getCubemapViewRenderer() != null
							? parent.getCubemapViewRenderer().getCubemap() : null;
				case STANDARD:
					return parent.getStandardRenderer() != null
							? parent.getStandardRenderer().getStandardView() : null;
				default:
					return null;
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "resolveGraphics failed for " + viewType, e);
			return null;
		}
	}

	// -------------------------------------------------------------------------
	// Initialization / shutdown
	// -------------------------------------------------------------------------

	private void initNDI() {
		if (ndiEnabled || ndiSender != null) return;
		try {
			ndiSender = new DevolaySender("ziviDomeLive NDI Output");
			for (int i = 0; i < NDI_SLOT_COUNT; i++) {
				freeSlots.offer(new NdiSlot());
			}
			ndiEnabled = true;
			ndiWorkerExecutor = Executors.newSingleThreadExecutor(r -> {
				Thread t = new Thread(r, "zividomelive-ndi-worker");
				t.setDaemon(true);
				return t;
			});
			ndiWorkerExecutor.submit(this::runNdiWorker);
			logger.info("NDI output initialized.");
		} catch (LinkageError | IllegalStateException e) {
			cleanupFailedNdiInit();
			logger.log(Level.WARNING, "initNDI failed: NDI unavailable on this platform.", e);
		}
	}

	/**
	 * Long-running NDI worker loop — runs in {@link #ndiWorkerExecutor}.
	 * Consumes ready slots, performs ARGB→RGBA conversion, and sends via NDI.
	 */
	private void runNdiWorker() {
		while (ndiEnabled && !Thread.currentThread().isInterrupted()) {
			NdiSlot slot = null;
			try {
				slot = readySlots.poll(100, TimeUnit.MILLISECONDS);
				if (slot == null) continue;

				// ARGB → RGBA conversion (off the draw thread)
				int count = slot.width * slot.height;
				slot.rgbaBuffer.clear();
				for (int i = 0; i < count; i++) {
					int px = slot.argbPixels[i];
					slot.rgbaBuffer.put((byte) ((px >> 16) & 0xFF)); // R
					slot.rgbaBuffer.put((byte) ((px >> 8)  & 0xFF)); // G
					slot.rgbaBuffer.put((byte)  (px        & 0xFF)); // B
					slot.rgbaBuffer.put((byte) ((px >> 24) & 0xFF)); // A
				}
				slot.rgbaBuffer.flip();

				slot.frame.setResolution(slot.width, slot.height);
				slot.frame.setData(slot.rgbaBuffer);
				slot.frame.setFourCCType(DevolayFrameFourCCType.RGBA);
				slot.frame.setLineStride(slot.width * 4);
				slot.frame.setFormatType(DevolayFrameFormatType.INTERLEAVED);
				slot.frame.setFrameRate(ndiFrameRateNum, ndiFrameRateDen);

				// Check sender while holding lock to guard against concurrent shutdown.
				DevolaySender sender;
				synchronized (this) {
					sender = ndiEnabled ? ndiSender : null;
				}
				if (sender != null) {
					sender.sendVideoFrame(slot.frame);
					ndiSent.incrementAndGet();
				}
				freeSlots.offer(slot);

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				if (slot != null) freeSlots.offer(slot);
				break;
			} catch (Exception e) {
				logger.log(Level.WARNING, "NDI worker error.", e);
				if (slot != null) freeSlots.offer(slot);
			}
		}
		// Drain any remaining ready slots back to the free pool.
		NdiSlot s;
		while ((s = readySlots.poll()) != null) {
			freeSlots.offer(s);
		}
	}

	private void initSpout() {
		try {
			if (spoutSender == null) {
				spoutSender = new Spout(parent.getPApplet());
			}
			spoutEnabled    = true;
			spoutLastWidth  = 0; // force explicit createSender on first sendOutput
			spoutLastHeight = 0;
			logger.info("Spout initialized for Windows.");
		} catch (Exception | LinkageError e) {
			spoutSender  = null;
			spoutEnabled = false;
			logger.log(Level.WARNING, "initSpout failed: Spout unavailable on this platform.", e);
		}
	}

	private void initSyphon() {
		try {
			if (syphonServer == null) {
				syphonServer = new SyphonServer(parent.getPApplet(), "ziviDomeLive Syphon");
			}
			syphonEnabled = true;
			logger.info("SyphonServer initialized for macOS.");
		} catch (Exception | LinkageError e) {
			syphonServer  = null;
			syphonEnabled = false;
			logger.log(Level.WARNING, "initSyphon failed: Syphon unavailable on this platform.", e);
		}
	}

	/**
	 * Toggles the specified output method on or off.
	 *
	 * @param method "ndi", "spout", or "syphon" (case-insensitive)
	 */
	public void toggleOutput(String method) {
		if (method == null || method.trim().isEmpty()) {
			logger.warning("Ignoring output toggle request with empty method.");
			return;
		}
		switch (method.trim().toLowerCase(Locale.ROOT)) {
			case "ndi":
				if (!ndiEnabled) initNDI(); else shutdownNDI();
				break;
			case "spout":
				if (!isWindows) { logger.warning("Spout toggle ignored: unsupported platform."); return; }
				if (!spoutEnabled) initSpout(); else shutdownSpout();
				break;
			case "syphon":
				if (!isMacOS) { logger.warning("Syphon toggle ignored: unsupported platform."); return; }
				if (!syphonEnabled) initSyphon(); else shutdownSyphon();
				break;
			default:
				logger.warning("Unknown output method: " + method.trim());
		}
	}

	// -------------------------------------------------------------------------
	// sendOutput — hot path, called every draw frame
	// -------------------------------------------------------------------------

	/**
	 * Sends the current rendered frame to all active outputs.
	 *
	 * <p>Must be called from the Processing draw thread after all render views have
	 * been updated (i.e., after {@code updateRenderViews()} completes).
	 *
	 * <p>Syphon and Spout are sent first (GPU texture share, minimal overhead).
	 * NDI availability is checked before {@code loadPixels()} to avoid unnecessary
	 * CPU work when the NDI worker is busy.
	 */
	public void sendOutput() {
		// Syphon — macOS GPU-to-GPU, stays on draw thread
		if (syphonEnabled && syphonServer != null && isMacOS) {
			try {
				PGraphicsOpenGL pg = resolveGraphics(OutputType.SYPHON);
				if (pg != null) {
					syphonServer.sendImage(pg);
				}
			} catch (Exception | LinkageError e) {
				logger.log(Level.WARNING, "sendOutput: Syphon error.", e);
			}
		}

		// Spout — Windows GPU-to-GPU, stays on draw thread
		if (spoutEnabled && spoutSender != null && isWindows) {
			try {
				PGraphicsOpenGL pg = resolveGraphics(OutputType.SPOUT);
				if (pg != null) {
					if (spoutLastWidth == 0 || spoutLastHeight == 0) {
						// Explicit sender creation on first frame avoids the lost-first-frame
						// issue caused by Spout's lazy createSender inside sendTexture().
						spoutSender.createSender("ziviDomeLive Spout", pg.width, pg.height);
						spoutLastWidth  = pg.width;
						spoutLastHeight = pg.height;
					} else if (pg.width != spoutLastWidth || pg.height != spoutLastHeight) {
						// Explicit update before sendTexture prevents the lost-frame-on-resize
						// issue caused by Spout's lazy updateSender inside sendTexture().
						spoutSender.updateSender(pg.width, pg.height);
						spoutLastWidth  = pg.width;
						spoutLastHeight = pg.height;
					}
					spoutSender.sendTexture(pg);
				}
			} catch (Exception | LinkageError e) {
				logger.log(Level.WARNING, "sendOutput: Spout error.", e);
			}
		}

		// NDI — pixel copy on draw thread, conversion + send in dedicated worker
		if (ndiEnabled && ndiSender != null) {
			try {
				PGraphicsOpenGL pg = resolveGraphics(OutputType.NDI);
				if (pg == null || pg.width <= 0 || pg.height <= 0) return;

				// Availability check BEFORE loadPixels — drops frame without CPU work if busy.
				NdiSlot slot = freeSlots.poll();
				if (slot == null) {
					ndiDropped.incrementAndGet();
					return;
				}

				pg.loadPixels(); // must run on draw thread
				int[] pixels = pg.pixels;
				if (pixels == null || pixels.length < pg.width * pg.height) {
					freeSlots.offer(slot); // return slot on bad state
					return;
				}

				// System.arraycopy is significantly faster than element-wise copy.
				slot.prepare(pixels, pg.width, pg.height);
				ndiCaptured.incrementAndGet();

				if (!readySlots.offer(slot)) {
					// readySlots full — worker is overloaded, drop and return slot.
					freeSlots.offer(slot);
					ndiDropped.incrementAndGet();
				}
			} catch (Exception | LinkageError e) {
				logger.log(Level.WARNING, "sendOutput: NDI capture error.", e);
			}
		}
	}

	// -------------------------------------------------------------------------
	// NDI metrics
	// -------------------------------------------------------------------------

	/** Returns the total number of NDI frames for which {@code loadPixels()} was called. */
	public long getNdiCapturedFrames() { return ndiCaptured.get(); }

	/** Returns the total number of NDI frames successfully transmitted. */
	public long getNdiSentFrames()     { return ndiSent.get(); }

	/**
	 * Returns the number of NDI frames dropped because no slot was available
	 * (worker busy) or the ready queue was full.
	 */
	public long getNdiDroppedFrames()  { return ndiDropped.get(); }

	// -------------------------------------------------------------------------
	// Shutdown
	// -------------------------------------------------------------------------

	/** Shuts down all active output methods and releases resources. */
	public void shutdownOutputs() {
		ndiEnabled    = false;
		spoutEnabled  = false;
		syphonEnabled = false;
		shutdownNDI();
		shutdownSpout();
		shutdownSyphon();
		logger.info("All output services have been shut down.");
	}

	private void shutdownNDI() {
		ndiEnabled = false; // volatile write — worker sees this immediately

		// Capture and clear the executor reference before waiting for termination
		// to avoid holding a lock across awaitTermination (which could deadlock if
		// the worker tries to acquire the same lock during shutdown).
		ExecutorService executor;
		synchronized (this) {
			executor = ndiWorkerExecutor;
			ndiWorkerExecutor = null;
		}
		if (executor != null) {
			executor.shutdownNow();
			try {
				executor.awaitTermination(500, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		// Close sender after worker has exited to avoid concurrent use.
		synchronized (this) {
			if (ndiSender != null) {
				ndiSender.close();
				ndiSender = null;
			}
		}
		drainAndCloseSlots(freeSlots);
		drainAndCloseSlots(readySlots);
		logger.info("NDI output shut down.");
	}

	private void shutdownSpout() {
		if (spoutSender != null) {
			spoutSender.dispose();
			spoutSender = null;
		}
		spoutEnabled    = false;
		spoutLastWidth  = 0;
		spoutLastHeight = 0;
		logger.info("Spout output shut down.");
	}

	private void shutdownSyphon() {
		if (syphonServer != null) {
			syphonServer.stop();
			syphonServer = null;
		}
		syphonEnabled = false;
		logger.info("Syphon output shut down.");
	}

	private void cleanupFailedNdiInit() {
		if (ndiSender != null) { ndiSender.close(); ndiSender = null; }
		ndiEnabled = false;
		drainAndCloseSlots(freeSlots);
		drainAndCloseSlots(readySlots);
	}

	/** Drains {@code queue}, calling {@link NdiSlot#close()} on each slot. */
	private void drainAndCloseSlots(Queue<NdiSlot> queue) {
		NdiSlot slot;
		while ((slot = queue.poll()) != null) {
			slot.close();
		}
	}

	// -------------------------------------------------------------------------
	// Status
	// -------------------------------------------------------------------------

	/** Returns true if NDI output is currently enabled. */
	public boolean isNdiEnabled()    { return ndiEnabled;    }

	/** Returns true if Spout output is currently enabled. */
	public boolean isSpoutEnabled()  { return spoutEnabled;  }

	/** Returns true if Syphon output is currently enabled. */
	public boolean isSyphonEnabled() { return syphonEnabled; }

	/** Returns true if at least one output method is currently active. */
	public boolean isActive() {
		return (ndiEnabled    && ndiSender    != null)
			|| (spoutEnabled  && spoutSender  != null)
			|| (syphonEnabled && syphonServer != null);
	}

	// -------------------------------------------------------------------------
	// Per-output view setters (ControlManager API)
	// -------------------------------------------------------------------------

	/** Sets the view type for NDI output. */
	public void setNdiView(zividomelive.ViewType view)    { setViewForOutput(OutputType.NDI,    view); }

	/** Sets the view type for Spout output. */
	public void setSpoutView(zividomelive.ViewType view)  { setViewForOutput(OutputType.SPOUT,  view); }

	/** Sets the view type for Syphon output. */
	public void setSyphonView(zividomelive.ViewType view) { setViewForOutput(OutputType.SYPHON, view); }

	/**
	 * Sets the NDI frame rate metadata. Common values:
	 * <ul>
	 *   <li>{@code 60, 1} → 60 fps (default)
	 *   <li>{@code 30, 1} → 30 fps
	 *   <li>{@code 60000, 1001} → 59.94 fps
	 * </ul>
	 *
	 * @param numerator   frame rate numerator (must be &gt; 0)
	 * @param denominator frame rate denominator (must be &gt; 0)
	 */
	public void setNdiFrameRate(int numerator, int denominator) {
		if (numerator > 0 && denominator > 0) {
			ndiFrameRateNum = numerator;
			ndiFrameRateDen = denominator;
		}
	}

	/** Stops all output methods. Alias for {@link #shutdownOutputs()}. */
	public void stopOutput() { shutdownOutputs(); }

	/** No-op — retained for call-site compatibility. Graphics are now resolved per-frame. */
	public void refreshCachedGraphics() { /* per-frame resolution — no cache to refresh */ }
}
