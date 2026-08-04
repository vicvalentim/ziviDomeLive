package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.support.LogManager;
import com.victorvalentim.zividomelive.support.ThreadManager;
import com.victorvalentim.zividomelive.zividomelive;
import me.walkerknapp.devolay.*;
import processing.core.PConstants;
import codeanticode.syphon.SyphonServer;
import processing.opengl.PGraphicsOpenGL;
import spout.Spout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages frame output to NDI, Spout (Windows), and Syphon (macOS).
 *
 * <p>Hot-path design:
 * <ul>
 *   <li>Syphon and Spout are sent first — GPU-to-GPU share, no pixel copy, on the draw thread.
 *   <li>NDI pixels are captured via {@code loadPixels()} on the draw thread, then
 *       ARGB→RGBA conversion + {@code sendVideoFrame()} run on a shared worker thread.
 *   <li>Three pre-allocated NDI frame slots rotate round-robin. An {@link AtomicBoolean}
 *       guard ensures at most one NDI task is queued at any time; excess frames are dropped
 *       (counted via {@link #getNdiDroppedFrames()}) to keep latency bounded.
 *   <li>Graphics references are resolved per-frame so they are always valid after
 *       a {@code resetGraphics()} call.
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

	private final Logger logger = LogManager.getLogger();
	private final Map<OutputType, zividomelive.ViewType> outputViews;
	private final zividomelive parent;

	private DevolaySender ndiSender;
	private Spout spoutSender;
	private SyphonServer syphonServer;

	private boolean ndiEnabled    = false;
	private boolean spoutEnabled  = false;
	private boolean syphonEnabled = false;

	private final boolean isMacOS;
	private final boolean isWindows;

	// NDI triple-buffering
	private final DevolayVideoFrame[] ndiFrames  = new DevolayVideoFrame[NDI_SLOT_COUNT];
	private final ByteBuffer[]        ndiBuffers = new ByteBuffer[NDI_SLOT_COUNT];
	private int ndiSlot = 0;
	private final AtomicBoolean ndiTaskPending = new AtomicBoolean(false);

	// NDI metrics
	private final AtomicLong ndiCaptured = new AtomicLong(0);
	private final AtomicLong ndiSent     = new AtomicLong(0);
	private final AtomicLong ndiDropped  = new AtomicLong(0);

	// Spout resolution change tracking
	private int spoutLastWidth  = 0;
	private int spoutLastHeight = 0;

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
	 * Legacy single-view setter — kept for API compatibility only.
	 *
	 * @param viewType ignored
	 * @deprecated Use {@link #setViewForOutput(OutputType, zividomelive.ViewType)} per output.
	 */
	@Deprecated
	public void setView(zividomelive.ViewType viewType) {
		// No-op — each output has its own view via setViewForOutput().
	}

	/**
	 * Returns {@code true} if at least one enabled output is configured to receive
	 * the given view type. Used by {@code updateRenderViews()} to ensure a view is
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
	// Per-frame graphics resolution (avoids stale references after resetGraphics)
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
				ndiFrames[i] = new DevolayVideoFrame();
			}
			ndiEnabled = true;
			logger.info("NDI output initialized.");
		} catch (LinkageError | IllegalStateException e) {
			ndiSender = null;
			Arrays.fill(ndiFrames, null);
			ndiEnabled = false;
			logger.log(Level.WARNING, "initNDI failed: NDI unavailable on this platform.", e);
		}
	}

	private void initSpout() {
		try {
			if (spoutSender == null) {
				spoutSender = new Spout(parent.getPApplet());
			}
			spoutEnabled = true;
			logger.info("Spout initialized for Windows.");
		} catch (Exception | LinkageError e) {
			spoutSender = null;
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
			syphonServer = null;
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
				if (!isWindows) {
					logger.warning("Spout toggle ignored: unsupported platform.");
					return;
				}
				if (!spoutEnabled) initSpout(); else shutdownSpout();
				break;
			case "syphon":
				if (!isMacOS) {
					logger.warning("Syphon toggle ignored: unsupported platform.");
					return;
				}
				if (!syphonEnabled) initSyphon(); else shutdownSyphon();
				break;
			default:
				logger.warning("Unknown output method: " + method.trim());
		}
	}

	// -------------------------------------------------------------------------
	// sendOutput — hot path called every draw frame
	// -------------------------------------------------------------------------

	/**
	 * Sends the current rendered frame to all active outputs.
	 *
	 * <p>Must be called from the Processing draw thread after all render views have
	 * been updated. Syphon and Spout are processed first (GPU texture share, minimal
	 * overhead). NDI pixels are captured here and forwarded to a worker thread.
	 */
	public void sendOutput() {
		// Syphon — macOS GPU-to-GPU, remains on draw thread
		if (syphonEnabled && syphonServer != null && isMacOS) {
			try {
				PGraphicsOpenGL pg = resolveGraphics(OutputType.SYPHON);
				if (pg != null) {
					syphonServer.sendImage(pg);
				}
			} catch (Exception e) {
				logger.log(Level.WARNING, "sendOutput: Syphon error.", e);
			}
		}

		// Spout — Windows GPU-to-GPU, remains on draw thread
		if (spoutEnabled && spoutSender != null && isWindows) {
			try {
				PGraphicsOpenGL pg = resolveGraphics(OutputType.SPOUT);
				if (pg != null) {
					if (pg.width != spoutLastWidth || pg.height != spoutLastHeight) {
						spoutLastWidth  = pg.width;
						spoutLastHeight = pg.height;
						logger.info("Spout resolution: " + spoutLastWidth + "×" + spoutLastHeight);
					}
					spoutSender.sendTexture(pg);
				}
			} catch (Exception e) {
				logger.log(Level.WARNING, "sendOutput: Spout error.", e);
			}
		}

		// NDI — pixel capture on draw thread, conversion + send on worker thread
		if (ndiEnabled && ndiSender != null) {
			try {
				PGraphicsOpenGL pg = resolveGraphics(OutputType.NDI);
				if (pg != null && pg.width > 0 && pg.height > 0) {
					pg.loadPixels(); // must stay on draw thread
					int[] pixels = pg.pixels;
					if (pixels != null && pixels.length == pg.width * pg.height) {
						submitNDIFrame(pixels, pg.width, pg.height);
					}
				}
			} catch (Exception e) {
				logger.log(Level.WARNING, "sendOutput: NDI capture error.", e);
			}
		}
	}

	/**
	 * Copies pixel data into the next NDI slot and submits a worker task to send it.
	 * Drops the frame (incrementing the dropped counter) if the previous task is still running.
	 */
	private void submitNDIFrame(int[] pixels, int width, int height) {
		int slot = ndiSlot;
		ndiSlot = (ndiSlot + 1) % NDI_SLOT_COUNT;

		int byteCount = width * height * 4;
		if (ndiBuffers[slot] == null || ndiBuffers[slot].capacity() != byteCount) {
			ndiBuffers[slot] = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.LITTLE_ENDIAN);
		}
		ByteBuffer buf = ndiBuffers[slot];
		buf.clear();
		for (int px : pixels) {
			buf.put((byte) ((px >> 16) & 0xFF)); // R
			buf.put((byte) ((px >> 8)  & 0xFF)); // G
			buf.put((byte)  (px        & 0xFF)); // B
			buf.put((byte) ((px >> 24) & 0xFF)); // A
		}
		buf.flip();

		DevolayVideoFrame frame = ndiFrames[slot];
		if (frame == null) return;
		frame.setResolution(width, height);
		frame.setData(buf);
		frame.setFourCCType(DevolayFrameFourCCType.RGBA);
		frame.setLineStride(width * 4);
		frame.setFormatType(DevolayFrameFormatType.INTERLEAVED);
		frame.setFrameRate(60, 1);

		ndiCaptured.incrementAndGet();

		if (!ndiTaskPending.compareAndSet(false, true)) {
			// Worker busy — drop frame to keep queue bounded
			ndiDropped.incrementAndGet();
			return;
		}

		final int capturedSlot = slot;
		ThreadManager.submitRunnable(() -> {
			try {
				synchronized (OutputManager.this) {
					if (ndiSender != null && ndiEnabled) {
						ndiSender.sendVideoFrame(ndiFrames[capturedSlot]);
						ndiSent.incrementAndGet();
					}
				}
			} catch (Exception e) {
				logger.log(Level.WARNING, "NDI sendVideoFrame error.", e);
			} finally {
				ndiTaskPending.set(false);
			}
		});
	}

	// -------------------------------------------------------------------------
	// NDI metrics
	// -------------------------------------------------------------------------

	/** Returns the total number of NDI frames for which loadPixels() was called. */
	public long getNdiCapturedFrames() { return ndiCaptured.get(); }

	/** Returns the total number of NDI frames successfully transmitted. */
	public long getNdiSentFrames()     { return ndiSent.get(); }

	/** Returns the number of NDI frames dropped due to a busy worker thread. */
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

	private synchronized void shutdownNDI() {
		if (ndiSender != null) {
			ndiSender.close();
			ndiSender = null;
		}
		Arrays.fill(ndiFrames,  null);
		Arrays.fill(ndiBuffers, null);
		ndiEnabled = false;
		ndiTaskPending.set(false);
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

	// -------------------------------------------------------------------------
	// Status
	// -------------------------------------------------------------------------

	/** Returns true if NDI output is enabled. */
	public boolean isNdiEnabled()    { return ndiEnabled;    }

	/** Returns true if Spout output is enabled. */
	public boolean isSpoutEnabled()  { return spoutEnabled;  }

	/** Returns true if Syphon output is enabled. */
	public boolean isSyphonEnabled() { return syphonEnabled; }

	/** Returns true if at least one output is currently active. */
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

	/** Stops all output methods. Alias for {@link #shutdownOutputs()}. */
	public void stopOutput() { shutdownOutputs(); }

	/**
	 * No-op — retained for call-site compatibility with the previous cached-graphics
	 * implementation. Graphics references are now resolved per-frame.
	 */
	public void refreshCachedGraphics() { /* per-frame resolution — no cache to refresh */ }
}
