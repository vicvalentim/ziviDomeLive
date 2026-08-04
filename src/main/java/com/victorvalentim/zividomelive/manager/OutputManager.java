package com.victorvalentim.zividomelive.manager;

import codeanticode.syphon.SyphonServer;
import com.victorvalentim.zividomelive.support.LogManager;
import com.victorvalentim.zividomelive.zividomelive;
import me.walkerknapp.devolay.DevolayFrameFormatType;
import me.walkerknapp.devolay.DevolayFrameFourCCType;
import me.walkerknapp.devolay.DevolaySender;
import me.walkerknapp.devolay.DevolayVideoFrame;
import processing.core.PConstants;
import processing.opengl.PGraphicsOpenGL;
import spout.Spout;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages ziviDomeLive video outputs.
 *
 * <p>There are two independent output domains:</p>
 * <ul>
 *     <li>a platform-local GPU texture output: Syphon on macOS or Spout on Windows;</li>
 *     <li>an NDI network output, captured on the Processing draw thread and sent by a dedicated worker.</li>
 * </ul>
 *
 * <p>Syphon and Spout are mutually exclusive. Their calls remain on the Processing/OpenGL thread,
 * because both libraries access the current OpenGL context directly. NDI performs only the required
 * {@code loadPixels()} capture on that thread; pixel conversion and transmission run independently.</p>
 */
public class OutputManager implements PConstants {

	/** Public output identifiers retained for compatibility with the current ControlManager API. */
	public enum OutputType {
        /**
         * Network Device Interface (NDI) output. Requires the NDI runtime to be installed on the host.
         */
		NDI,
        /**
         * Spout output. Available on Windows platforms.
         */
		SPOUT,
        /**
         * Syphon output. Available on macOS platforms.
         */
		SYPHON
	}

	/** The single local texture-sharing backend available in the current process. */
	private enum LocalTextureBackend {
		SYPHON,
		SPOUT,
		NONE
	}

	private static final String NDI_SENDER_NAME = "ziviDomeLive NDI Output";
	private static final String SPOUT_SENDER_NAME = "ziviDomeLive Spout";
	private static final String SYPHON_SERVER_NAME = "ziviDomeLive Syphon";

	/** Triple buffering: one slot may be sending while two remain available or pending. */
	private static final int NDI_SLOT_COUNT = 3;

	/** Retains the frame-rate value used by the previous OutputManager implementation. */
	private static final int DEFAULT_NDI_FRAME_RATE_N = 150;
	private static final int DEFAULT_NDI_FRAME_RATE_D = 1;

	private final Logger logger = LogManager.getLogger();
	private final zividomelive parent;
	private final boolean isMacOS;
	private final boolean isWindows;
	private final LocalTextureBackend localTextureBackend;

	/* Independent view selections. Preview/viewer state is deliberately not stored here. */
	private volatile zividomelive.ViewType ndiView = zividomelive.ViewType.FISHEYE_DOMEMASTER;
	private volatile zividomelive.ViewType spoutView = zividomelive.ViewType.FISHEYE_DOMEMASTER;
	private volatile zividomelive.ViewType syphonView = zividomelive.ViewType.FISHEYE_DOMEMASTER;

	/* NDI lifecycle and worker state. */
	private final Object ndiLifecycleLock = new Object();
	private volatile DevolaySender ndiSender;
	private volatile boolean ndiEnabled;
	private volatile boolean ndiUnavailable;
	private volatile String ndiUnavailableReason = "";
	private volatile boolean ndiWorkerRunning;
	private Thread ndiWorkerThread;

	private final ArrayBlockingQueue<NdiFrameSlot> ndiFreeSlots =
			new ArrayBlockingQueue<>(NDI_SLOT_COUNT);
	private final ArrayBlockingQueue<NdiFrameSlot> ndiReadySlots =
			new ArrayBlockingQueue<>(NDI_SLOT_COUNT);
	private final NdiFrameSlot[] ndiSlots = new NdiFrameSlot[NDI_SLOT_COUNT];

	private volatile int ndiFrameRateNumerator = DEFAULT_NDI_FRAME_RATE_N;
	private volatile int ndiFrameRateDenominator = DEFAULT_NDI_FRAME_RATE_D;

	private final AtomicLong ndiCapturedFrames = new AtomicLong();
	private final AtomicLong ndiSentFrames = new AtomicLong();
	private final AtomicLong ndiDroppedFrames = new AtomicLong();

	/* Platform-local texture output. Only one of these can be active in a process. */
	private Spout spoutSender;
	private SyphonServer syphonServer;
	/* Initialization state: backend resources have been created. */
	private boolean spoutInitialized;
	private boolean syphonInitialized;
	/* Enable state: user has toggled sending on/off. */
	private volatile boolean spoutEnabled;
	private volatile boolean syphonEnabled;
	private int spoutWidth = -1;
	private int spoutHeight = -1;

	/** Prevents repeated warnings from the deprecated single-view setter. */
	private boolean legacySetViewWarningLogged;

	/**
	 * Creates an output manager and selects the only valid local texture backend for the platform.
	 *
	 * @param parent main ziviDomeLive application
	 */
	public OutputManager(zividomelive parent) {
		if (parent == null) {
			throw new IllegalArgumentException("parent cannot be null");
		}

		this.parent = parent;

		String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		this.isMacOS = osName.contains("mac");
		this.isWindows = osName.contains("win");

		if (isMacOS) {
			this.localTextureBackend = LocalTextureBackend.SYPHON;
		} else if (isWindows) {
			this.localTextureBackend = LocalTextureBackend.SPOUT;
		} else {
			this.localTextureBackend = LocalTextureBackend.NONE;
		}
	}

	/**
	 * Returns the independently configured view for an output.
	 *
	 * @param outputType output whose configured view should be returned
	 * @return configured view, or {@link zividomelive.ViewType#FISHEYE_DOMEMASTER}
	 *         when {@code outputType} is {@code null} or unsupported
	 */
	public zividomelive.ViewType getViewForOutput(OutputType outputType) {
		if (outputType == null) {
			return zividomelive.ViewType.FISHEYE_DOMEMASTER;
		}

		switch (outputType) {
			case NDI:
				return ndiView;
			case SPOUT:
				return spoutView;
			case SYPHON:
				return syphonView;
			default:
				return zividomelive.ViewType.FISHEYE_DOMEMASTER;
		}
	}

	/**
	 * Changes only the selected output view. It does not modify the application preview/viewer.
	 *
	 * @param outputType output whose view should be changed
	 * @param viewType view to route to the selected output
	 */
	public void setViewForOutput(OutputType outputType, zividomelive.ViewType viewType) {
		if (outputType == null || viewType == null) {
			return;
		}

		switch (outputType) {
			case NDI:
				ndiView = viewType;
				break;
			case SPOUT:
				spoutView = viewType;
				break;
			case SYPHON:
				syphonView = viewType;
				break;
			default:
				return;
		}

		logger.info("Set view for " + outputType + " to " + viewType + ".");
	}

	/**
	 * Compatibility method retained for callers that previously refreshed cached PGraphics references.
	 * References are now resolved on every frame, so this method intentionally does nothing.
	 */
	public void refreshCachedGraphics() {
		// No-op by design: prevents stale PGraphicsOpenGL references after renderer reallocation.
	}

	/**
	 * Resolves the current PGraphicsOpenGL directly from the active renderer.
	 */
	private PGraphicsOpenGL resolveGraphics(zividomelive.ViewType viewType) {
		if (viewType == null) {
			return null;
		}

		try {
			switch (viewType) {
				case FISHEYE_DOMEMASTER:
					return parent.getFisheyeDomemaster() != null
							? parent.getFisheyeDomemaster().getDomemasterGraphics()
							: null;
				case EQUIRECTANGULAR:
					return parent.getEquirectangularRenderer() != null
							? parent.getEquirectangularRenderer().getEquirectangular()
							: null;
				case CUBEMAP:
					return parent.getCubemapViewRenderer() != null
							? parent.getCubemapViewRenderer().getCubemap()
							: null;
				case STANDARD:
					return parent.getStandardRenderer() != null
							? parent.getStandardRenderer().getStandardView()
							: null;
				default:
					return null;
			}
		} catch (RuntimeException error) {
			logger.log(Level.WARNING, "resolveGraphics failed for " + viewType + ".", error);
			return null;
		}
	}

	/**
	 * Initializes NDI and starts its dedicated sender worker.
	 */
	private void initNDI() {
		synchronized (ndiLifecycleLock) {
			if (ndiEnabled) {
				return;
			}

			if (ndiUnavailable) {
				logger.warning("NDI initialization ignored: " + ndiUnavailableReason);
				return;
			}

			/* Clean up a previous worker failure before retrying. */
			if (ndiSender != null || ndiWorkerThread != null) {
				shutdownNDILocked();
			}

			try {
				ndiSender = new DevolaySender(NDI_SENDER_NAME);
				initializeNdiSlots();

				ndiWorkerRunning = true;
				ndiEnabled = true;
				ndiWorkerThread = new Thread(this::ndiWorkerLoop, "ziviDomeLive-NDI-Sender");
				ndiWorkerThread.setDaemon(true);
				ndiWorkerThread.start();

				logger.info("NDI output initialized with a dedicated sender worker.");
			} catch (LinkageError | RuntimeException error) {
				ndiEnabled = false;
				ndiWorkerRunning = false;
				closeNdiSlots();
				closeNdiSender();

				ndiUnavailable = true;
				ndiUnavailableReason = rootCauseMessage(error);

				logger.log(
						Level.WARNING,
						"initNDI failed: NDI unavailable on this platform ("
								+ System.getProperty("os.name", "unknown") + "/"
								+ System.getProperty("os.arch", "unknown") + "): "
								+ ndiUnavailableReason,
						error
				);
			}
		}
	}

	/**
	 * Creates the fixed NDI frame pool after the native Devolay library has initialized successfully.
	 */
	private void initializeNdiSlots() {
		ndiFreeSlots.clear();
		ndiReadySlots.clear();

		for (int index = 0; index < NDI_SLOT_COUNT; index++) {
			NdiFrameSlot slot = new NdiFrameSlot();
			ndiSlots[index] = slot;
			ndiFreeSlots.offer(slot);
		}
	}

	/**
	 * Initializes the Windows Spout sender without enabling it.
	 *
	 * <p>Initialization is performed once; it creates the native Spout sender and registers it.
	 * The sender remains idle until {@link #toggleOutput(String)} enables transmission.
	 * Dimensions are resolved from the currently active output graphics when available;
	 * if no renderer has been allocated yet, the configured output resolution is used.</p>
	 */
	private void initializeSpoutOnce() {
		if (spoutInitialized) {
			return;
		}

		if (localTextureBackend != LocalTextureBackend.SPOUT) {
			return;
		}

		try {
			PGraphicsOpenGL graphics = resolveGraphics(spoutView);
			int width;
			int height;
			if (graphics != null && graphics.width > 0 && graphics.height > 0) {
				width  = graphics.width;
				height = graphics.height;
			} else {
				int res = parent.getOutputResolution();
				width  = res;
				height = res;
				logger.info("Spout initializing with configured output resolution (" + res + "px).");
			}

			Spout sender = new Spout(parent.getPApplet());
			boolean created = sender.createSender(SPOUT_SENDER_NAME, width, height);

			if (!created) {
				sender.dispose();
				logger.warning("Spout sender creation failed.");
				return;
			}

			spoutSender = sender;
			spoutWidth  = width;
			spoutHeight = height;
			spoutInitialized = true;
			/* Do NOT set spoutEnabled. User must toggle output to enable transmission. */

			logger.info("Spout initialized at " + spoutWidth + "x" + spoutHeight +
					"; transmission disabled until toggled on.");
		} catch (Exception | LinkageError error) {
			spoutInitialized = false;
			disposeSpoutSender();
			logger.log(Level.WARNING, "initializeSpoutOnce failed: Spout unavailable on this platform.", error);
		}
	}

	/**
	 * Initializes the macOS Syphon server without enabling it.
	 *
	 * <p>Initialization is performed once; it creates the Syphon server wrapper and forces
	 * the native JSyphonServer to be created by calling {@code hasClients()}.
	 * The server remains idle until {@link #toggleOutput(String)} enables transmission.
	 * Resolution-agnostic: the server reads texture dimensions on each sent frame.</p>
	 */
	private void initializeSyphonOnce() {
		if (syphonInitialized) {
			return;
		}

		if (localTextureBackend != LocalTextureBackend.SYPHON) {
			return;
		}

		try {
			syphonServer = new SyphonServer(parent.getPApplet(), SYPHON_SERVER_NAME);
			/* Force native JSyphonServer to be created now, not on the first sendImage(). */
			syphonServer.hasClients();
			syphonInitialized = true;
			/* Do NOT set syphonEnabled. User must toggle output to enable transmission. */
			logger.info("Syphon server initialized for macOS; transmission disabled until toggled on.");
		} catch (Exception | LinkageError error) {
			syphonInitialized = false;
			stopSyphonServer();
			logger.log(Level.WARNING, "initializeSyphonOnce failed: Syphon unavailable on this platform.", error);
		}
	}

	/**
	 * Initializes the platform-local texture backend (Syphon on macOS, Spout on Windows).
	 *
	 * <p>This method must be called once by the library after renderer managers are initialized
	 * and the OpenGL context is fully active. It performs all resource allocation and native
	 * server registration, but does NOT enable transmission. The output remains dormant until
	 * {@link #toggleOutput(String)} is called by the user.</p>
	 *
	 * <p>On unsupported platforms, this is a no-op.</p>
	 */
	public void initializeLocalTextureOutput() {
		switch (localTextureBackend) {
			case SYPHON:
				initializeSyphonOnce();
				break;
			case SPOUT:
				initializeSpoutOnce();
				break;
			case NONE:
			default:
				break;
		}
	}

	/**
	 * Toggles NDI, Spout, or Syphon transmission on/off.
	 *
	 * <p>For NDI: creates or destroys the worker thread (expensive, deferred resource cleanup).</p>
	 *
	 * <p>For Syphon and Spout: only toggles a flag. The backend remains initialized and ready;
	 * transmission is simply disabled or re-enabled on the next call to {@link #sendOutput()}.
	 * No resource destruction or recreation occurs. This is a fast O(1) operation.</p>
	 *
	 * @param method output identifier: {@code "ndi"}, {@code "spout"}, or {@code "syphon"}
	 */
	public void toggleOutput(String method) {
		if (method == null || method.trim().isEmpty()) {
			logger.warning("Ignoring output toggle request with empty method.");
			return;
		}

		String normalizedMethod = method.trim().toLowerCase(Locale.ROOT);

		switch (normalizedMethod) {
			case "ndi":
				if (ndiEnabled) {
					shutdownNDI();
				} else {
					initNDI();
				}
				break;

			case "spout":
				if (!isWindows) {
					logger.warning("Spout toggle ignored: unsupported platform.");
					return;
				}
				if (!spoutInitialized) {
					logger.warning("Spout not initialized; cannot toggle.");
					return;
				}
				spoutEnabled = !spoutEnabled;
				logger.info("Spout transmission " + (spoutEnabled ? "enabled" : "disabled") + ".");
				break;

			case "syphon":
				if (!isMacOS) {
					logger.warning("Syphon toggle ignored: unsupported platform.");
					return;
				}
				if (!syphonInitialized) {
					logger.warning("Syphon not initialized; cannot toggle.");
					return;
				}
				syphonEnabled = !syphonEnabled;
				logger.info("Syphon transmission " + (syphonEnabled ? "enabled" : "disabled") + ".");
				break;

			default:
				logger.warning("Unknown output method: " + normalizedMethod);
				break;
		}
	}

	/**
	 * Legacy method retained only for source compatibility.
	 *
	 * <p>It no longer changes NDI, Syphon, or Spout, because coupling the application preview mode
	 * to every external output prevents independent routing. Use {@link #setNdiView(zividomelive.ViewType)},
	 * {@link #setSpoutView(zividomelive.ViewType)}, or {@link #setSyphonView(zividomelive.ViewType)}.</p>
	 *
	 * @param viewType ignored; retained only for source compatibility
	 * @deprecated configure each output independently with the dedicated view setter
	 */
	@Deprecated
	public void setView(zividomelive.ViewType viewType) {
		if (!legacySetViewWarningLogged) {
			legacySetViewWarningLogged = true;
			logger.warning(
					"OutputManager.setView() is deprecated and no longer changes external outputs. "
							+ "Configure NDI and the platform-local texture output independently."
			);
		}
	}

	/**
	 * Sends one frame to every enabled output.
	 *
	 * <p>This method must be called once per Processing draw cycle, after all relevant PGraphics
	 * instances have completed {@code endDraw()}.</p>
	 *
	 * <p>The platform-local GPU texture is published first. NDI capture occurs afterwards, so its
	 * required GPU-to-CPU readback cannot delay the current Syphon/Spout publication.</p>
	 */
	public void sendOutput() {
		sendLocalTextureFrame();
		captureNdiFrame();
	}

	/** Publishes the single platform-local texture output directly on the Processing/OpenGL thread. */
	private void sendLocalTextureFrame() {
		switch (localTextureBackend) {
			case SPOUT:
				sendSpoutFrame();
				break;
			case SYPHON:
				sendSyphonFrame();
				break;
			case NONE:
			default:
				break;
		}
	}

	/** Direct GPU-to-GPU Spout publication. */
	private void sendSpoutFrame() {
		if (!spoutEnabled || spoutSender == null || !isWindows) {
			return;
		}

		try {
			PGraphicsOpenGL graphics = resolveGraphics(spoutView);
			if (graphics == null) {
				return;
			}

			if (graphics.width != spoutWidth || graphics.height != spoutHeight) {
				spoutSender.updateSender(graphics.width, graphics.height);
				spoutWidth = graphics.width;
				spoutHeight = graphics.height;
			}

			spoutSender.sendTexture(graphics);
		} catch (Exception | LinkageError error) {
			logger.log(Level.WARNING, "Spout frame publication failed; Spout has been disabled.", error);
			shutdownSpout();
		}
	}

	/** Direct GPU-to-GPU Syphon publication. */
	private void sendSyphonFrame() {
		if (!syphonEnabled || syphonServer == null || !isMacOS) {
			return;
		}

		try {
			PGraphicsOpenGL graphics = resolveGraphics(syphonView);
			if (graphics != null) {
				syphonServer.sendImage(graphics);
			}
		} catch (Exception | LinkageError error) {
			logger.log(Level.WARNING, "Syphon frame publication failed; Syphon has been disabled.", error);
			shutdownSyphon();
		}
	}

	/**
	 * Captures the selected NDI PGraphics into a pooled CPU slot.
	 *
	 * <p>{@code loadPixels()} must remain on the Processing/OpenGL thread. The expensive ARGB-to-RGBA
	 * conversion and synchronous NDI send are performed by the dedicated worker. A bounded latest-frame
	 * policy prevents an overloaded NDI receiver from accumulating latency or blocking Syphon/Spout.</p>
	 */
	private void captureNdiFrame() {
		if (!ndiEnabled || ndiSender == null || !ndiWorkerRunning) {
			return;
		}

		PGraphicsOpenGL graphics = resolveGraphics(ndiView);
		if (graphics == null || graphics.width <= 0 || graphics.height <= 0) {
			return;
		}

		NdiFrameSlot slot = acquireNdiCaptureSlot();
		if (slot == null) {
			ndiDroppedFrames.incrementAndGet();
			return;
		}

		boolean queued = false;
		try {
			graphics.loadPixels();

			int width = graphics.width;
			int height = graphics.height;
			int pixelCount = Math.multiplyExact(width, height);

			if (graphics.pixels == null || graphics.pixels.length < pixelCount) {
				logger.warning("NDI frame skipped: Processing pixel buffer is unavailable or incomplete.");
				return;
			}

			slot.ensureCapacity(width, height);
			System.arraycopy(graphics.pixels, 0, slot.argbPixels, 0, pixelCount);
			slot.width = width;
			slot.height = height;
			slot.pixelCount = pixelCount;
			slot.frameRateNumerator = ndiFrameRateNumerator;
			slot.frameRateDenominator = ndiFrameRateDenominator;

			queued = offerLatestNdiFrame(slot);
			if (queued) {
				ndiCapturedFrames.incrementAndGet();
			}
		} catch (RuntimeException error) {
			logger.log(Level.WARNING, "NDI frame capture failed.", error);
		} finally {
			if (!queued) {
				ndiFreeSlots.offer(slot);
			}
		}
	}

	/**
	 * Obtains a free slot. If all slots are occupied, the oldest frame still waiting in the ready
	 * queue is replaced, preserving low latency without touching the slot currently used by the worker.
	 */
	private NdiFrameSlot acquireNdiCaptureSlot() {
		NdiFrameSlot slot = ndiFreeSlots.poll();
		if (slot != null) {
			return slot;
		}

		slot = ndiReadySlots.poll();
		if (slot != null) {
			ndiDroppedFrames.incrementAndGet();
		}
		return slot;
	}

	/** Queues the newest NDI frame, replacing the oldest pending frame if required. */
	private boolean offerLatestNdiFrame(NdiFrameSlot slot) {
		if (ndiReadySlots.offer(slot)) {
			return true;
		}

		NdiFrameSlot stale = ndiReadySlots.poll();
		if (stale != null) {
			ndiDroppedFrames.incrementAndGet();
			ndiFreeSlots.offer(stale);
		}

		return ndiReadySlots.offer(slot);
	}

	/** Dedicated NDI conversion and sender loop. No OpenGL calls are made here. */
	private void ndiWorkerLoop() {
		while (ndiWorkerRunning || !ndiReadySlots.isEmpty()) {
			NdiFrameSlot slot = null;

			try {
				slot = ndiReadySlots.poll(100, TimeUnit.MILLISECONDS);
				if (slot == null) {
					continue;
				}

				DevolaySender sender = ndiSender;
				if (!ndiEnabled || sender == null) {
					continue;
				}

				slot.prepareDevolayFrame();

				/*
				 * Synchronous send is intentional here: it runs only on the dedicated worker and makes
				 * ownership of each pooled frame/buffer explicit. The render thread remains independent.
				 */
				sender.sendVideoFrame(slot.frame);
				ndiSentFrames.incrementAndGet();
			} catch (InterruptedException interrupted) {
				if (!ndiWorkerRunning) {
					Thread.currentThread().interrupt();
					break;
				}
			} catch (Exception | LinkageError error) {
				ndiEnabled = false;
				ndiWorkerRunning = false;
				logger.log(Level.WARNING, "NDI sender worker failed; NDI has been disabled.", error);
			} finally {
				if (slot != null) {
					ndiFreeSlots.offer(slot);
				}
			}
		}
	}

	/**
	 * Changes the NDI frame-rate metadata used for subsequently captured frames.
	 *
	 * @param numerator positive frame-rate numerator
	 * @param denominator positive frame-rate denominator
	 * @throws IllegalArgumentException if either value is zero or negative
	 */
	public void setNdiFrameRate(int numerator, int denominator) {
		if (numerator <= 0 || denominator <= 0) {
			throw new IllegalArgumentException("NDI frame-rate numerator and denominator must be positive.");
		}
		ndiFrameRateNumerator = numerator;
		ndiFrameRateDenominator = denominator;
	}

	/** Shuts down all output methods. */
	public void shutdownOutputs() {
		shutdownNDI();
		shutdownSpout();
		shutdownSyphon();
		logger.info("All output services have been shut down.");
	}

	/** Stops the NDI worker before releasing its native sender and frame resources. */
	private void shutdownNDI() {
		synchronized (ndiLifecycleLock) {
			shutdownNDILocked();
		}
	}

	/** Must be called while holding {@link #ndiLifecycleLock}. */
	private void shutdownNDILocked() {
		ndiEnabled = false;
		ndiWorkerRunning = false;

		Thread worker = ndiWorkerThread;
		ndiWorkerThread = null;

		if (worker != null && worker != Thread.currentThread()) {
			worker.interrupt();
			try {
				worker.join();
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				logger.warning("Interrupted while waiting for the NDI sender worker to stop.");
			}
		}

		ndiReadySlots.clear();
		ndiFreeSlots.clear();
		closeNdiSlots();
		closeNdiSender();

		logger.info("NDI output shut down.");
	}

	/** Closes every pooled DevolayVideoFrame. */
	private void closeNdiSlots() {
		for (int index = 0; index < ndiSlots.length; index++) {
			NdiFrameSlot slot = ndiSlots[index];
			ndiSlots[index] = null;

			if (slot != null) {
				try {
					slot.close();
				} catch (RuntimeException | LinkageError error) {
					logger.log(Level.FINE, "Failed to close an NDI frame slot cleanly.", error);
				}
			}
		}
	}

	/** Closes the native NDI sender. */
	private void closeNdiSender() {
		DevolaySender sender = ndiSender;
		ndiSender = null;

		if (sender != null) {
			try {
				sender.close();
			} catch (RuntimeException | LinkageError error) {
				logger.log(Level.FINE, "Failed to close the NDI sender cleanly.", error);
			}
		}
	}

	/** Releases the Windows Spout sender. */
	private void shutdownSpout() {
		spoutEnabled = false;
		disposeSpoutSender();
		spoutWidth = -1;
		spoutHeight = -1;
	}

	private void disposeSpoutSender() {
		Spout sender = spoutSender;
		spoutSender = null;

		if (sender != null) {
			try {
				sender.dispose();
				logger.info("Spout output shut down.");
			} catch (Exception | LinkageError error) {
				logger.log(Level.FINE, "Failed to dispose the Spout sender cleanly.", error);
			}
		}
	}

	/** Stops the macOS Syphon server. */
	private void shutdownSyphon() {
		syphonEnabled = false;
		stopSyphonServer();
	}

	private void stopSyphonServer() {
		SyphonServer server = syphonServer;
		syphonServer = null;

		if (server != null) {
			try {
				server.stop();
				logger.info("Syphon output shut down.");
			} catch (Exception | LinkageError error) {
				logger.log(Level.FINE, "Failed to stop the Syphon server cleanly.", error);
			}
		}
	}

	/**
	 * Reports whether the NDI sender and its worker are active.
	 *
	 * @return {@code true} when NDI is enabled and ready to send frames
	 */
	public boolean isNdiEnabled() {
		return ndiEnabled && ndiSender != null && ndiWorkerRunning;
	}

	/**
	 * Reports whether the Windows Spout sender is active.
	 *
	 * @return {@code true} when running on Windows with Spout enabled and initialized
	 */
	public boolean isSpoutEnabled() {
		return isWindows && spoutEnabled && spoutSender != null;
	}

	/**
	 * Reports whether the macOS Syphon server is active.
	 *
	 * @return {@code true} when running on macOS with Syphon enabled and initialized
	 */
	public boolean isSyphonEnabled() {
		return isMacOS && syphonEnabled && syphonServer != null;
	}

	/**
	 * Selects the view sent through NDI.
	 *
	 * @param view view to route to the NDI output
	 */
	public void setNdiView(zividomelive.ViewType view) {
		setViewForOutput(OutputType.NDI, view);
	}

	/**
	 * Selects the view sent through Spout.
	 *
	 * @param view view to route to the Spout output
	 */
	public void setSpoutView(zividomelive.ViewType view) {
		setViewForOutput(OutputType.SPOUT, view);
	}

	/**
	 * Selects the view sent through Syphon.
	 *
	 * @param view view to route to the Syphon output
	 */
	public void setSyphonView(zividomelive.ViewType view) {
		setViewForOutput(OutputType.SYPHON, view);
	}

	/**
	 * Sets the view of whichever local texture backend exists on this platform.
	 *
	 * @param view view to route to Syphon on macOS or Spout on Windows
	 */
	public void setLocalTextureView(zividomelive.ViewType view) {
		if (view == null) {
			return;
		}

		switch (localTextureBackend) {
			case SPOUT:
				setSpoutView(view);
				break;
			case SYPHON:
				setSyphonView(view);
				break;
			case NONE:
			default:
				logger.warning("Local texture view ignored: no Syphon/Spout backend on this platform.");
				break;
		}
	}

	/**
	 * Returns the view of the platform-local texture backend.
	 *
	 * @return Spout view on Windows, Syphon view on macOS, or
	 *         {@link zividomelive.ViewType#FISHEYE_DOMEMASTER} when no local backend exists
	 */
	public zividomelive.ViewType getLocalTextureView() {
		switch (localTextureBackend) {
			case SPOUT:
				return spoutView;
			case SYPHON:
				return syphonView;
			case NONE:
			default:
				return zividomelive.ViewType.FISHEYE_DOMEMASTER;
		}
	}

	/**
	 * Returns a stable backend name for UI and diagnostics.
	 *
	 * @return {@code "Spout"}, {@code "Syphon"}, or {@code "None"}
	 */
	public String getLocalTextureBackendName() {
		switch (localTextureBackend) {
			case SPOUT:
				return "Spout";
			case SYPHON:
				return "Syphon";
			case NONE:
			default:
				return "None";
		}
	}

	/**
	 * Returns the number of NDI frames copied from Processing into an available slot.
	 *
	 * @return total captured NDI frames for the lifetime of this manager
	 */
	public long getNdiCapturedFrames() {
		return ndiCapturedFrames.get();
	}

	/**
	 * Returns the number of NDI frames successfully submitted by the sender worker.
	 *
	 * @return total transmitted NDI frames for the lifetime of this manager
	 */
	public long getNdiSentFrames() {
		return ndiSentFrames.get();
	}

	/**
	 * Returns the number of NDI frames discarded because no free slot was available.
	 *
	 * @return total dropped NDI frames for the lifetime of this manager
	 */
	public long getNdiDroppedFrames() {
		return ndiDroppedFrames.get();
	}

	/**
	 * Returns whether an enabled external output currently requires the specified high-resolution view.
	 * The render pipeline can use this to update Cubemap and Standard targets independently of preview mode.
	 *
	 * @param view view whose external-output requirement should be checked
	 * @return {@code true} if an enabled NDI, Spout, or Syphon output is configured for {@code view}
	 */
	public boolean requiresView(zividomelive.ViewType view) {
		if (view == null) {
			return false;
		}

		if (isNdiEnabled() && ndiView == view) {
			return true;
		}
		if (isSpoutEnabled() && spoutView == view) {
			return true;
		}
		return isSyphonEnabled() && syphonView == view;
	}

	/**
	 * Checks whether NDI or the single valid local texture backend is active.
	 *
	 * @return {@code true} when at least one output is enabled and initialized
	 */
	public boolean isActive() {
		return isNdiEnabled() || isSpoutEnabled() || isSyphonEnabled();
	}

	/** Stops and releases every output owned by this manager. */
	public void stopOutput() {
		shutdownOutputs();
	}

	/** Finds the most specific error message in a nested initialization failure. */
	private static String rootCauseMessage(Throwable error) {
		Throwable root = error;
		while (root.getCause() != null && root.getCause() != root) {
			root = root.getCause();
		}

		String message = root.getMessage();
		if (message == null || message.trim().isEmpty()) {
			return root.getClass().getSimpleName();
		}
		return root.getClass().getSimpleName() + ": " + message;
	}

	/**
	 * A reusable NDI frame slot. The Processing ARGB copy is filled on the draw thread; conversion and
	 * Devolay submission happen exclusively on the NDI worker.
	 */
	private static final class NdiFrameSlot implements AutoCloseable {

		private final DevolayVideoFrame frame = new DevolayVideoFrame();
		private int[] argbPixels;
		private ByteBuffer rgbaBuffer;
		private int width;
		private int height;
		private int pixelCount;
		private int frameRateNumerator;
		private int frameRateDenominator;

		private void ensureCapacity(int requiredWidth, int requiredHeight) {
			int requiredPixels = Math.multiplyExact(requiredWidth, requiredHeight);
			int requiredBytes = Math.multiplyExact(requiredPixels, 4);

			if (argbPixels == null || argbPixels.length != requiredPixels) {
				argbPixels = new int[requiredPixels];
			}

			if (rgbaBuffer == null || rgbaBuffer.capacity() != requiredBytes) {
				rgbaBuffer = ByteBuffer
						.allocateDirect(requiredBytes)
						.order(ByteOrder.LITTLE_ENDIAN);
			}
		}

		private void prepareDevolayFrame() {
			rgbaBuffer.clear();

			for (int index = 0; index < pixelCount; index++) {
				int pixel = argbPixels[index];
				rgbaBuffer.put((byte) ((pixel >>> 16) & 0xFF));
				rgbaBuffer.put((byte) ((pixel >>> 8) & 0xFF));
				rgbaBuffer.put((byte) (pixel & 0xFF));
				rgbaBuffer.put((byte) ((pixel >>> 24) & 0xFF));
			}

			rgbaBuffer.flip();

			frame.setResolution(width, height);
			frame.setData(rgbaBuffer);
			frame.setFourCCType(DevolayFrameFourCCType.RGBA);
			frame.setLineStride(width * 4);
			frame.setFormatType(DevolayFrameFormatType.INTERLEAVED);
			frame.setFrameRate(frameRateNumerator, frameRateDenominator);
		}

		/** {@inheritDoc} */
		@Override
		public void close() {
			frame.close();
			argbPixels = null;
			rgbaBuffer = null;
		}
	}
}
