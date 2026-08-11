package com.victorvalentim.zividomelive.manager;

import codeanticode.syphon.SyphonServer;
import com.victorvalentim.zividomelive.RenderMode;
import com.victorvalentim.zividomelive.support.LogManager;
import com.victorvalentim.zividomelive.ziviDomeLive;
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
import java.util.logging.Logger;

/**
 * Manages all external video outputs produced by ziviDomeLive.
 *
 * <p>The manager keeps two independent output domains:</p>
 * <ul>
 *     <li>a platform-local GPU texture output: Syphon on macOS or Spout on Windows;</li>
 *     <li>an NDI network output backed by a dedicated CPU worker.</li>
 * </ul>
 *
 * <p>Syphon and Spout are mutually exclusive because only one of them is valid for the
 * current operating system. Their native backend is prepared once, after the Processing
 * OpenGL context and renderer targets exist. UI toggles only enable or disable frame
 * publication; they never create, destroy, or recreate the native backend.</p>
 *
 * <p>Syphon and Spout receive the selected {@link PGraphicsOpenGL} directly. They do not
 * use {@code loadPixels()}, CPU copies, intermediate graphics targets, or worker threads.
 * NDI is isolated from that path: the Processing draw thread only performs the required
 * pixel readback and a bounded copy, while conversion and network transmission run on a
 * dedicated worker.</p>
 */
public class OutputManager implements PConstants {

	/** Public output identifiers retained for ControlManager compatibility. */
	public enum OutputType {
		/** Network Device Interface output. */
		NDI,
		/** Windows Spout texture output. */
		SPOUT,
		/** macOS Syphon texture output. */
		SYPHON
	}

	/**
	 * Observable lifecycle state of one output backend.
	 *
	 * <p>Availability, native initialization, publication, and render requirements are
	 * deliberately separate concerns. {@link #STOPPING} is specific to bounded NDI shutdown.</p>
	 *
	 * @since 1.5.0
	 */
	public enum OutputState {
		/** The backend is unsupported or its last initialization attempt failed. */
		UNAVAILABLE,
		/** The backend is eligible for initialization but owns no native resources. */
		AVAILABLE,
		/** Native resources exist while frame publication is disabled. */
		INITIALIZED,
		/** Native resources exist and frame publication is enabled. */
		ENABLED,
		/** NDI publication is disabled while its worker completes deferred cleanup. */
		STOPPING
	}

	/** The single platform-local texture-sharing implementation available in this process. */
	private enum LocalTextureBackend {
		SYPHON,
		SPOUT,
		NONE
	}

	private static final String NDI_SENDER_NAME = "ziviDomeLive NDI Output";
	private static final String SPOUT_SENDER_NAME = "ziviDomeLive Spout";
	private static final String SYPHON_SERVER_NAME = "ziviDomeLive Syphon";

	/** One slot may be sent while the remaining slots are free or queued. */
	private static final int NDI_SLOT_COUNT = 3;
	private static final long DEFAULT_NDI_SHUTDOWN_TIMEOUT_MILLIS = 1_000L;
	private static final int NDI_BYTES_PER_PIXEL = 4;

	/** Default metadata follows the facade's default Processing frame rate. */
	static final int DEFAULT_NDI_FRAME_RATE_NUMERATOR = 60;
	static final int DEFAULT_NDI_FRAME_RATE_DENOMINATOR = 1;
	static final DevolayFrameFourCCType NDI_FRAME_FOUR_CC_TYPE = DevolayFrameFourCCType.RGBA;
	static final DevolayFrameFormatType NDI_FRAME_FORMAT_TYPE = DevolayFrameFormatType.PROGRESSIVE;

	private final Logger logger = LogManager.getLogger();
	private final ziviDomeLive parent;
	private final boolean isMacOS;
	private final boolean isWindows;
	private final LocalTextureBackend localTextureBackend;
	private final long ndiShutdownTimeoutMillis;

	/* Independent output routing. Preview/viewer state is intentionally not stored here. */
	private volatile ziviDomeLive.ViewType ndiView = ziviDomeLive.ViewType.FISHEYE_DOMEMASTER;
	private volatile ziviDomeLive.ViewType spoutView = ziviDomeLive.ViewType.FISHEYE_DOMEMASTER;
	private volatile ziviDomeLive.ViewType syphonView = ziviDomeLive.ViewType.FISHEYE_DOMEMASTER;

	/* Platform-local texture output. Only one backend can exist in a process. */
	private Spout spoutSender;
	private SyphonServer syphonServer;
	private volatile boolean spoutEnabled;
	private volatile boolean syphonEnabled;
	private volatile boolean localTextureInitialized;
	private volatile boolean localTextureUnavailable;
	private volatile String localTextureFailureReason = "";
	private int spoutWidth = -1;
	private int spoutHeight = -1;

	/* NDI lifecycle and worker state. */
	private final Object ndiLifecycleLock = new Object();
	private volatile DevolaySender ndiSender;
	private volatile boolean ndiEnabled;
	private volatile boolean ndiUnavailable;
	private volatile String ndiFailureReason = "";
	private volatile boolean ndiWorkerRunning;
	private volatile boolean ndiShutdownPending;
	private boolean ndiRestartRequested;
	private volatile Thread ndiWorkerThread;

	private final ArrayBlockingQueue<NdiFrameSlot> ndiFreeSlots =
			new ArrayBlockingQueue<>(NDI_SLOT_COUNT);
	private final ArrayBlockingQueue<NdiFrameSlot> ndiReadySlots =
			new ArrayBlockingQueue<>(NDI_SLOT_COUNT);
	private final NdiFrameSlot[] ndiSlots = new NdiFrameSlot[NDI_SLOT_COUNT];

	private volatile int ndiFrameRateNumerator = DEFAULT_NDI_FRAME_RATE_NUMERATOR;
	private volatile int ndiFrameRateDenominator = DEFAULT_NDI_FRAME_RATE_DENOMINATOR;

	private final AtomicLong ndiCapturedFrames = new AtomicLong();
	private final AtomicLong ndiSentFrames = new AtomicLong();
	private final AtomicLong ndiDroppedFrames = new AtomicLong();
	private final AtomicLong ndiFailedFrames = new AtomicLong();

	/** Prevents repeated warnings from the deprecated single-view setter. */
	private boolean legacySetViewWarningLogged;

	/**
	 * Creates an output manager and selects the valid local texture backend for the host platform.
	 *
	 * @param parent main ziviDomeLive application; must not be {@code null}
	 * @throws IllegalArgumentException if {@code parent} is {@code null}
	 */
	public OutputManager(ziviDomeLive parent) {
		this(parent, DEFAULT_NDI_SHUTDOWN_TIMEOUT_MILLIS);
	}

	/** Package-private constructor for deterministic worker-shutdown tests. */
	OutputManager(ziviDomeLive parent, long ndiShutdownTimeoutMillis) {
		if (parent == null) {
			throw new IllegalArgumentException("parent cannot be null");
		}
		if (ndiShutdownTimeoutMillis <= 0) {
			throw new IllegalArgumentException("NDI shutdown timeout must be positive");
		}

		this.parent = parent;
		this.ndiShutdownTimeoutMillis = ndiShutdownTimeoutMillis;
		this.ndiFrameRateNumerator = parent.getTargetFrameRate();
		this.ndiFrameRateDenominator = DEFAULT_NDI_FRAME_RATE_DENOMINATOR;

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
	 * @return configured view, or {@link ziviDomeLive.ViewType#FISHEYE_DOMEMASTER}
	 *         when {@code outputType} is {@code null}
	 */
	public ziviDomeLive.ViewType getViewForOutput(OutputType outputType) {
		if (outputType == null) {
			return ziviDomeLive.ViewType.FISHEYE_DOMEMASTER;
		}

		switch (outputType) {
			case NDI:
				return ndiView;
			case SPOUT:
				return spoutView;
			case SYPHON:
				return syphonView;
			default:
				return ziviDomeLive.ViewType.FISHEYE_DOMEMASTER;
		}
	}

	/**
	 * Changes only the selected external-output view.
	 *
	 * <p>This method does not alter the application preview or any other output.</p>
	 *
	 * @param outputType output whose view should be changed
	 * @param viewType view to route to the selected output
	 */
	public void setViewForOutput(OutputType outputType, ziviDomeLive.ViewType viewType) {
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
	 * Compatibility method retained for callers that previously refreshed cached graphics references.
	 *
	 * <p>Graphics references are resolved on every frame, so this method intentionally does nothing.</p>
	 */
	public void refreshCachedGraphics() {
		// No-op by design: resolving per frame prevents stale references after renderer reallocation.
	}

	/**
	 * Resolves the current graphics target directly from the corresponding renderer.
	 *
	 * @param viewType view whose graphics target should be returned
	 * @return current graphics target, or {@code null} when unavailable
	 */
	private PGraphicsOpenGL resolveGraphics(ziviDomeLive.ViewType viewType) {
		ziviDomeLive.ViewType effectiveView = resolveOutputView(viewType);
		if (effectiveView == null) {
			return null;
		}

		try {
			switch (effectiveView) {
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
			logger.warning("resolveGraphics failed for " + effectiveView + ": " + rootCauseMessage(error));
			return null;
		}
	}

	/** Resolves a configured output route under the facade's global render mode. */
	ziviDomeLive.ViewType resolveOutputView(ziviDomeLive.ViewType configuredView) {
		RenderMode renderMode = parent.getRenderMode();
		if (renderMode == null || renderMode == RenderMode.FULL) {
			return configuredView;
		}

		switch (renderMode) {
			case STANDARD:
				return ziviDomeLive.ViewType.STANDARD;
			case DOMEMASTER:
				return ziviDomeLive.ViewType.FISHEYE_DOMEMASTER;
			case EQUIRECTANGULAR:
				return ziviDomeLive.ViewType.EQUIRECTANGULAR;
			case SKYBOX:
				return ziviDomeLive.ViewType.CUBEMAP;
			case FULL:
			default:
				return configuredView;
		}
	}

	/**
	 * Prepares the platform-local texture-sharing backend without enabling frame publication.
	 *
	 * <p>This method must be called on the Processing/OpenGL thread after renderer resources have
	 * been created. It is idempotent: repeated calls never recreate an existing backend.</p>
	 *
	 * <p>On macOS, the method creates and warms up the Syphon server. On Windows, it creates the
	 * Spout sender. On unsupported platforms, it performs no operation.</p>
	 */
	public void initializeLocalTextureOutput() {
		if (localTextureInitialized || localTextureUnavailable) {
			return;
		}

		switch (localTextureBackend) {
			case SYPHON:
				initializeSyphonBackend();
				break;
			case SPOUT:
				initializeSpoutBackend();
				break;
			case NONE:
			default:
				break;
		}
	}

	/**
	 * Creates and warms up the macOS Syphon server without enabling publication.
	 */
	private void initializeSyphonBackend() {
		if (localTextureBackend != LocalTextureBackend.SYPHON || syphonServer != null) {
			return;
		}

		SyphonServer server = null;
		try {
			server = new SyphonServer(parent.getPApplet(), SYPHON_SERVER_NAME);

			/*
			 * Processing-Syphon initializes its native server lazily. hasClients() forces that
			 * initialization now, during library startup, rather than on the first enabled frame.
			 */
			server.hasClients();

			syphonServer = server;
			localTextureInitialized = true;
			localTextureUnavailable = false;
			localTextureFailureReason = "";
			logger.info("Syphon backend initialized and ready; frame publication remains disabled.");
		} catch (Exception | LinkageError error) {
			if (server != null) {
				try {
					server.stop();
				} catch (Exception | LinkageError cleanupError) {
					logger.warning(
							"Failed to release Syphon after initialization error: "
									+ rootCauseMessage(cleanupError)
					);
				}
			}

			syphonServer = null;
			syphonEnabled = false;
			markLocalTextureUnavailable("Syphon", error);
		}
	}

	/**
	 * Creates the Windows Spout sender without enabling publication.
	 */
	private void initializeSpoutBackend() {
		if (localTextureBackend != LocalTextureBackend.SPOUT || spoutSender != null) {
			return;
		}

		Spout sender = null;
		try {
			PGraphicsOpenGL graphics = resolveGraphics(spoutView);

			int width;
			int height;

			if (graphics != null && graphics.width > 0 && graphics.height > 0) {
				width = graphics.width;
				height = graphics.height;
			} else {
				/*
				 * Renderer allocation should already be complete, but this fallback keeps startup
				 * deterministic. sendSpoutFrame() will update the sender to the live dimensions.
				 */
				int outputResolution = parent.getOutputResolution();
				width = outputResolution;
				height = outputResolution;
			}

			sender = new Spout(parent.getPApplet());
			boolean created = sender.createSender(SPOUT_SENDER_NAME, width, height);

			if (!created) {
				sender.dispose();
				throw new IllegalStateException("Spout createSender returned false");
			}

			spoutSender = sender;
			spoutWidth = width;
			spoutHeight = height;
			localTextureInitialized = true;
			localTextureUnavailable = false;
			localTextureFailureReason = "";

			logger.info(
					"Spout backend initialized at " + width + "x" + height
							+ "; frame publication remains disabled."
			);
		} catch (Exception | LinkageError error) {
			if (sender != null && sender != spoutSender) {
				try {
					sender.dispose();
				} catch (Exception | LinkageError cleanupError) {
					logger.warning(
							"Failed to release Spout after initialization error: "
									+ rootCauseMessage(cleanupError)
					);
				}
			}

			disposeSpoutSender();
			spoutEnabled = false;
			spoutWidth = -1;
			spoutHeight = -1;
			markLocalTextureUnavailable("Spout", error);
		}
	}

	/**
	 * Marks the platform-local backend unavailable after an initialization failure.
	 *
	 * @param backendName human-readable backend name
	 * @param error initialization failure
	 */
	private void markLocalTextureUnavailable(String backendName, Throwable error) {
		localTextureInitialized = false;
		localTextureUnavailable = true;
		localTextureFailureReason = rootCauseMessage(error);
		logger.warning(
				backendName + " backend initialization failed: " + localTextureFailureReason
		);
	}

	/** Clears a failed local initialization only in response to an explicit publication toggle. */
	private void prepareLocalTextureRetry() {
		if (!localTextureUnavailable) {
			return;
		}
		logger.info("Retrying " + getLocalTextureBackendName() + " backend initialization.");
		localTextureUnavailable = false;
		localTextureFailureReason = "";
	}

	/**
	 * Initializes NDI and starts its dedicated sender worker.
	 */
	private void initNDI() {
		synchronized (ndiLifecycleLock) {
			if (isNdiEnabled()) {
				return;
			}

			Thread worker = ndiWorkerThread;
			if (worker != null && worker.isAlive()) {
				ndiRestartRequested = true;
				ndiUnavailable = false;
				ndiFailureReason = "";
				logger.info("NDI restart scheduled after the current worker finishes stopping.");
				return;
			}

			if (worker != null || ndiSender != null) {
				ndiWorkerThread = null;
				releaseNdiResourcesLocked();
			}

			initializeNdiLocked();
		}
	}

	/** Creates one NDI activation cycle. Must be called while holding the lifecycle lock. */
	private void initializeNdiLocked() {
		ndiUnavailable = false;
		ndiFailureReason = "";
		ndiShutdownPending = false;
		ndiRestartRequested = false;

		try {
			ndiSender = new DevolaySender(NDI_SENDER_NAME);
			initializeNdiSlots();

			Thread worker = new Thread(this::ndiWorkerLoop, "ziviDomeLive-NDI-Sender");
			worker.setDaemon(true);
			ndiWorkerThread = worker;
			ndiWorkerRunning = true;
			ndiEnabled = true;
			worker.start();

			logger.info("NDI output initialized with a dedicated sender worker.");
		} catch (LinkageError | RuntimeException error) {
			ndiEnabled = false;
			ndiWorkerRunning = false;
			ndiWorkerThread = null;
			releaseNdiResourcesLocked();

			ndiUnavailable = true;
			ndiFailureReason = rootCauseMessage(error);

			logger.warning(
					"NDI unavailable on "
							+ System.getProperty("os.name", "unknown")
							+ "/"
							+ System.getProperty("os.arch", "unknown")
							+ ": "
							+ ndiFailureReason
			);
		}
	}

	/**
	 * Creates the fixed NDI frame pool after Devolay initializes successfully.
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
	 * Toggles an output without coupling it to preview selection or another output.
	 *
	 * <p>NDI owns a dynamic worker lifecycle. Syphon and Spout do not: their native backend is
	 * prepared once and the toggle changes only the publication boolean.</p>
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
				if (isNdiEnabled()) {
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
				toggleSpoutPublication();
				break;

			case "syphon":
				if (!isMacOS) {
					logger.warning("Syphon toggle ignored: unsupported platform.");
					return;
				}
				toggleSyphonPublication();
				break;

			default:
				logger.warning("Unknown output method: " + normalizedMethod);
				break;
		}
	}

	/** Toggles Windows Spout publication without destroying or recreating its native sender. */
	private void toggleSpoutPublication() {
		if (!localTextureInitialized) {
			prepareLocalTextureRetry();
			initializeLocalTextureOutput();
		}

		if (spoutSender == null) {
			logger.warning(
					"Spout cannot be enabled: "
							+ (localTextureFailureReason.isEmpty()
							? "backend is not initialized"
							: localTextureFailureReason)
			);
			return;
		}

		spoutEnabled = !spoutEnabled;
		logger.info("Spout frame publication " + (spoutEnabled ? "enabled." : "disabled."));
	}

	/** Toggles macOS Syphon publication without destroying or recreating its native server. */
	private void toggleSyphonPublication() {
		if (!localTextureInitialized) {
			prepareLocalTextureRetry();
			initializeLocalTextureOutput();
		}

		if (syphonServer == null) {
			logger.warning(
					"Syphon cannot be enabled: "
							+ (localTextureFailureReason.isEmpty()
							? "backend is not initialized"
							: localTextureFailureReason)
			);
			return;
		}

		syphonEnabled = !syphonEnabled;
		logger.info("Syphon frame publication " + (syphonEnabled ? "enabled." : "disabled."));
	}

	/**
	 * Legacy method retained only for source compatibility.
	 *
	 * @param viewType ignored; configure each output with its dedicated setter
	 * @deprecated use {@link #setNdiView(ziviDomeLive.ViewType)},
	 *             {@link #setSpoutView(ziviDomeLive.ViewType)}, or
	 *             {@link #setSyphonView(ziviDomeLive.ViewType)}
	 */
	@Deprecated
	public void setView(ziviDomeLive.ViewType viewType) {
		if (!legacySetViewWarningLogged) {
			legacySetViewWarningLogged = true;
			logger.warning(
					"OutputManager.setView() is deprecated and no longer changes external outputs."
			);
		}
	}

	/**
	 * Sends one frame to every enabled output.
	 *
	 * <p>This method must run once per Processing draw cycle, after all relevant graphics targets
	 * have completed {@code endDraw()}. The local GPU output is published before NDI readback.</p>
	 */
	public void sendOutput() {
		sendLocalTextureFrame();
		captureNdiFrame();
	}

	/** Publishes the single platform-local texture output on the Processing/OpenGL thread. */
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

	/**
	 * Publishes the selected graphics target directly through Spout.
	 *
	 * <p>No pixel readback or intermediate graphics target is created.</p>
	 */
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
			localTextureFailureReason = "";
		} catch (Exception | LinkageError error) {
			spoutEnabled = false;
			localTextureFailureReason = rootCauseMessage(error);
			logger.warning(
					"Spout frame publication failed and was disabled without destroying the backend: "
							+ localTextureFailureReason
			);
		}
	}

	/**
	 * Publishes the selected graphics target directly through Syphon.
	 *
	 * <p>No pixel readback or intermediate graphics target is created.</p>
	 */
	private void sendSyphonFrame() {
		if (!syphonEnabled || syphonServer == null || !isMacOS) {
			return;
		}

		try {
			PGraphicsOpenGL graphics = resolveGraphics(syphonView);
			if (graphics != null) {
				syphonServer.sendImage(graphics);
				localTextureFailureReason = "";
			}
		} catch (Exception | LinkageError error) {
			syphonEnabled = false;
			localTextureFailureReason = rootCauseMessage(error);
			logger.warning(
					"Syphon frame publication failed and was disabled without destroying the backend: "
							+ localTextureFailureReason
			);
		}
	}

	/**
	 * Notifies the manager that renderer dimensions changed.
	 *
	 * <p>Syphon requires no action because each frame carries its texture dimensions. Spout uses
	 * {@code updateSender()} on the next published frame; its sender is never destroyed or recreated.</p>
	 *
	 * @param newResolution new configured output resolution in pixels
	 */
	public void notifyResolutionChanged(int newResolution) {
		if (newResolution <= 0) {
			logger.warning("Ignoring invalid output resolution: " + newResolution);
			return;
		}

		if (localTextureBackend == LocalTextureBackend.SPOUT && spoutSender != null) {
			PGraphicsOpenGL graphics = resolveGraphics(spoutView);
			if (graphics != null && graphics.width > 0 && graphics.height > 0) {
				try {
					spoutSender.updateSender(graphics.width, graphics.height);
					spoutWidth = graphics.width;
					spoutHeight = graphics.height;
					localTextureFailureReason = "";
				} catch (Exception | LinkageError error) {
					spoutEnabled = false;
					localTextureFailureReason = rootCauseMessage(error);
					logger.warning(
							"Spout resolution update failed; publication was disabled without "
									+ "destroying the sender: "
									+ localTextureFailureReason
					);
				}
			}
		}
	}

	/**
	 * Captures the selected NDI graphics target into a pooled CPU slot.
	 *
	 * <p>{@code loadPixels()} must remain on the Processing/OpenGL thread. Conversion and synchronous
	 * NDI sending are performed by the dedicated worker. The latest-frame policy keeps latency bounded.</p>
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
				ndiFailedFrames.incrementAndGet();
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
			} else {
				ndiDroppedFrames.incrementAndGet();
			}
		} catch (RuntimeException error) {
			ndiFailedFrames.incrementAndGet();
			logger.warning("NDI frame capture failed: " + rootCauseMessage(error));
		} finally {
			if (!queued) {
				ndiFreeSlots.offer(slot);
			}
		}
	}

	/**
	 * Obtains a slot for the next NDI capture.
	 *
	 * <p>If no slot is free, the oldest frame still waiting in the ready queue is replaced. The slot
	 * currently owned by the worker is never touched.</p>
	 *
	 * @return slot available for capture, or {@code null} when all slots are unavailable
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

	/**
	 * Queues the newest NDI frame, replacing the oldest pending frame when required.
	 *
	 * @param slot prepared NDI slot
	 * @return {@code true} if the slot was queued
	 */
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
		Thread worker = Thread.currentThread();
		try {
			while (ndiWorkerRunning || !ndiReadySlots.isEmpty()) {
				NdiFrameSlot slot = null;

				try {
					slot = ndiReadySlots.poll(100, TimeUnit.MILLISECONDS);
					if (slot == null) {
						continue;
					}

					DevolaySender sender = ndiSender;
					if (!ndiEnabled || sender == null) {
						if (ndiUnavailable) {
							ndiFailedFrames.incrementAndGet();
						}
						continue;
					}

					slot.prepareDevolayFrame();
					sender.sendVideoFrame(slot.frame);
					ndiSentFrames.incrementAndGet();
				} catch (InterruptedException interrupted) {
					if (!ndiWorkerRunning) {
						Thread.currentThread().interrupt();
						break;
					}
				} catch (Exception | LinkageError error) {
					ndiFailedFrames.incrementAndGet();
					markNdiWorkerUnavailable(worker, error);
				} finally {
					if (slot != null) {
						ndiFreeSlots.offer(slot);
					}
				}
			}
		} finally {
			finishNdiWorker(worker);
		}
	}

	/** Records a worker failure without closing native objects still owned by that worker. */
	private void markNdiWorkerUnavailable(Thread worker, Throwable error) {
		String failureReason = rootCauseMessage(error);
		synchronized (ndiLifecycleLock) {
			if (ndiWorkerThread != worker) {
				return;
			}
			ndiEnabled = false;
			ndiWorkerRunning = false;
			ndiUnavailable = true;
			ndiFailureReason = failureReason;
		}
		logger.warning("NDI sender worker failed and was disabled: " + failureReason);
	}

	/** Completes deferred cleanup after the worker can no longer touch native NDI resources. */
	private void finishNdiWorker(Thread worker) {
		synchronized (ndiLifecycleLock) {
			if (ndiWorkerThread != worker) {
				return;
			}

			ndiWorkerThread = null;
			ndiEnabled = false;
			ndiWorkerRunning = false;
			ndiShutdownPending = false;

			boolean restart = ndiRestartRequested;
			ndiRestartRequested = false;
			releaseNdiResourcesLocked();

			if (restart) {
				initializeNdiLocked();
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
			throw new IllegalArgumentException(
					"NDI frame-rate numerator and denominator must be positive."
			);
		}

		ndiFrameRateNumerator = numerator;
		ndiFrameRateDenominator = denominator;
	}

	/**
	 * Shuts down every output and releases native resources.
	 *
	 * <p>This is the only normal lifecycle path that destroys Syphon or Spout.</p>
	 */
	public void shutdownOutputs() {
		shutdownNDI();
		releaseLocalTextureBackend();
		logger.info("All output services have been shut down.");
	}

	/** Stops the NDI worker before releasing its native sender and frame resources. */
	private void shutdownNDI() {
		Thread worker;
		synchronized (ndiLifecycleLock) {
			ndiEnabled = false;
			ndiWorkerRunning = false;
			ndiRestartRequested = false;

			worker = ndiWorkerThread;
			if (worker == null) {
				ndiShutdownPending = false;
				releaseNdiResourcesLocked();
				return;
			}

			if (!worker.isAlive()) {
				ndiWorkerThread = null;
				ndiShutdownPending = false;
				releaseNdiResourcesLocked();
				return;
			}

			worker.interrupt();
			if (worker == Thread.currentThread() || ndiShutdownPending) {
				ndiShutdownPending = true;
				return;
			}
			ndiShutdownPending = true;
		}

		boolean stopped = waitForWorker(worker, ndiShutdownTimeoutMillis);
		if (!stopped) {
			String reason = "NDI sender worker did not stop within "
					+ ndiShutdownTimeoutMillis
					+ " ms; native cleanup was deferred.";
			synchronized (ndiLifecycleLock) {
				if (ndiWorkerThread == worker && worker.isAlive()) {
					ndiFailureReason = reason;
				}
			}
			logger.warning(reason);
			return;
		}

		synchronized (ndiLifecycleLock) {
			if (ndiWorkerThread == worker) {
				ndiWorkerThread = null;
				ndiShutdownPending = false;
				releaseNdiResourcesLocked();
			}
		}
		logger.info("NDI output shut down.");
	}

	/** Waits a bounded interval for a worker without clearing an interrupt from the caller. */
	static boolean waitForWorker(Thread worker, long timeoutMillis) {
		if (worker == null || !worker.isAlive()) {
			return true;
		}
		try {
			worker.join(timeoutMillis);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			return false;
		}
		return !worker.isAlive();
	}

	/** Releases NDI resources only after no worker can still use them. Lifecycle lock required. */
	private void releaseNdiResourcesLocked() {
		ndiReadySlots.clear();
		ndiFreeSlots.clear();
		closeNdiSlots();
		closeNdiSender();
	}

	/** Releases the platform-local backend during final application shutdown. */
	private void releaseLocalTextureBackend() {
		spoutEnabled = false;
		syphonEnabled = false;

		disposeSpoutSender();
		stopSyphonServer();

		spoutWidth = -1;
		spoutHeight = -1;
		localTextureInitialized = false;
	}

	/** Releases the native Windows Spout sender, if present. */
	private void disposeSpoutSender() {
		Spout sender = spoutSender;
		spoutSender = null;

		if (sender != null) {
			try {
				sender.dispose();
				logger.info("Spout backend released.");
			} catch (Exception | LinkageError error) {
				localTextureFailureReason = rootCauseMessage(error);
				logger.warning("Failed to dispose the Spout sender: " + localTextureFailureReason);
			}
		}
	}

	/** Releases the native macOS Syphon server, if present. */
	private void stopSyphonServer() {
		SyphonServer server = syphonServer;
		syphonServer = null;

		if (server != null) {
			try {
				server.stop();
				logger.info("Syphon backend released.");
			} catch (Exception | LinkageError error) {
				localTextureFailureReason = rootCauseMessage(error);
				logger.warning("Failed to stop the Syphon server: " + localTextureFailureReason);
			}
		}
	}

	/** Closes every pooled Devolay frame. */
	private void closeNdiSlots() {
		for (int index = 0; index < ndiSlots.length; index++) {
			NdiFrameSlot slot = ndiSlots[index];
			ndiSlots[index] = null;

			if (slot != null) {
				try {
					slot.close();
				} catch (RuntimeException | LinkageError error) {
					String failureReason = rootCauseMessage(error);
					if (ndiFailureReason.isEmpty()) {
						ndiFailureReason = failureReason;
					}
					logger.warning("Failed to close an NDI frame slot: " + failureReason);
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
				String failureReason = rootCauseMessage(error);
				if (ndiFailureReason.isEmpty()) {
					ndiFailureReason = failureReason;
				}
				logger.warning("Failed to close the NDI sender: " + failureReason);
			}
		}
	}

	/**
	 * Returns the lifecycle state of one output without treating it as a render requirement.
	 *
	 * @param outputType output to inspect
	 * @return current lifecycle state; {@link OutputState#UNAVAILABLE} for {@code null}
	 * @since 1.5.0
	 */
	public OutputState getOutputState(OutputType outputType) {
		if (outputType == null) {
			return OutputState.UNAVAILABLE;
		}

		switch (outputType) {
			case NDI:
				return resolveOutputState(
						true,
						ndiUnavailable,
						ndiSender != null || ndiWorkerThread != null,
						isNdiEnabled(),
						ndiShutdownPending && ndiWorkerThread != null);
			case SPOUT:
				return resolveOutputState(
						isWindows,
						localTextureUnavailable,
						spoutSender != null,
						isSpoutEnabled(),
						false);
			case SYPHON:
				return resolveOutputState(
						isMacOS,
						localTextureUnavailable,
						syphonServer != null,
						isSyphonEnabled(),
						false);
			default:
				return OutputState.UNAVAILABLE;
		}
	}

	/**
	 * Returns the latest backend failure reason without changing lifecycle state.
	 *
	 * @param outputType output to inspect
	 * @return diagnostic text, or an empty string when no failure has been recorded
	 * @since 1.5.0
	 */
	public String getOutputFailureReason(OutputType outputType) {
		if (outputType == OutputType.NDI) {
			return ndiFailureReason;
		}
		if (outputType != null && outputType == localOutputType()) {
			return localTextureFailureReason;
		}
		return "";
	}

	/** Pure state reducer shared by all backend implementations. */
	static OutputState resolveOutputState(
			boolean supported,
			boolean unavailable,
			boolean initialized,
			boolean enabled,
			boolean stopping) {
		if (!supported || unavailable) {
			return OutputState.UNAVAILABLE;
		}
		if (stopping) {
			return OutputState.STOPPING;
		}
		if (enabled) {
			return OutputState.ENABLED;
		}
		if (initialized) {
			return OutputState.INITIALIZED;
		}
		return OutputState.AVAILABLE;
	}

	/**
	 * Reports whether NDI is enabled and ready to send frames.
	 *
	 * @return {@code true} when NDI is active
	 */
	public boolean isNdiEnabled() {
		return ndiEnabled && ndiSender != null && ndiWorkerRunning;
	}

	/**
	 * Reports whether Spout publication is enabled.
	 *
	 * @return {@code true} on Windows when Spout is initialized and enabled
	 */
	public boolean isSpoutEnabled() {
		return isWindows && spoutEnabled && spoutSender != null;
	}

	/**
	 * Reports whether Syphon publication is enabled.
	 *
	 * @return {@code true} on macOS when Syphon is initialized and enabled
	 */
	public boolean isSyphonEnabled() {
		return isMacOS && syphonEnabled && syphonServer != null;
	}

	/**
	 * Reports whether the platform-local backend has been prepared.
	 *
	 * @return {@code true} when Syphon or Spout initialization completed successfully
	 */
	public boolean isLocalTextureInitialized() {
		OutputState state = getOutputState(localOutputType());
		return state == OutputState.INITIALIZED || state == OutputState.ENABLED;
	}

	/**
	 * Reports whether platform-local texture sharing is available.
	 *
	 * @return {@code true} when a supported backend exists and has not failed initialization
	 */
	public boolean isLocalTextureAvailable() {
		return getOutputState(localOutputType()) != OutputState.UNAVAILABLE;
	}

	/** Returns the public output identifier for the selected local backend. */
	private OutputType localOutputType() {
		switch (localTextureBackend) {
			case SPOUT:
				return OutputType.SPOUT;
			case SYPHON:
				return OutputType.SYPHON;
			case NONE:
			default:
				return null;
		}
	}

	/**
	 * Selects the view sent through NDI.
	 *
	 * @param view view to route to NDI
	 */
	public void setNdiView(ziviDomeLive.ViewType view) {
		setViewForOutput(OutputType.NDI, view);
	}

	/**
	 * Selects the view sent through Spout.
	 *
	 * @param view view to route to Spout
	 */
	public void setSpoutView(ziviDomeLive.ViewType view) {
		setViewForOutput(OutputType.SPOUT, view);
	}

	/**
	 * Selects the view sent through Syphon.
	 *
	 * @param view view to route to Syphon
	 */
	public void setSyphonView(ziviDomeLive.ViewType view) {
		setViewForOutput(OutputType.SYPHON, view);
	}

	/**
	 * Sets the view of the valid platform-local texture backend.
	 *
	 * @param view view to route to Syphon on macOS or Spout on Windows
	 */
	public void setLocalTextureView(ziviDomeLive.ViewType view) {
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
	 * Returns the view routed to the platform-local texture backend.
	 *
	 * @return Spout view on Windows, Syphon view on macOS, or fisheye when unsupported
	 */
	public ziviDomeLive.ViewType getLocalTextureView() {
		switch (localTextureBackend) {
			case SPOUT:
				return spoutView;
			case SYPHON:
				return syphonView;
			case NONE:
			default:
				return ziviDomeLive.ViewType.FISHEYE_DOMEMASTER;
		}
	}

	/**
	 * Returns a stable local-backend name for UI and diagnostics.
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
	 * Returns the number of NDI frames copied into capture slots.
	 *
	 * @return total captured NDI frames
	 */
	public long getNdiCapturedFrames() {
		return ndiCapturedFrames.get();
	}

	/**
	 * Returns the number of NDI frames sent by the worker.
	 *
	 * @return total sent NDI frames
	 */
	public long getNdiSentFrames() {
		return ndiSentFrames.get();
	}

	/**
	 * Returns the number of NDI frames discarded by the bounded latest-frame policy.
	 *
	 * @return total dropped NDI frames
	 */
	public long getNdiDroppedFrames() {
		return ndiDroppedFrames.get();
	}

	/**
	 * Returns the number of NDI frames rejected by capture or sender failures.
	 *
	 * <p>This counter is separate from {@link #getNdiDroppedFrames()}, which remains reserved
	 * for bounded latest-frame backpressure.</p>
	 *
	 * @return total failed NDI frames
	 * @since 1.5.0
	 */
	public long getNdiFailedFrames() {
		return ndiFailedFrames.get();
	}

	/**
	 * Reports whether an enabled external output effectively requires a view.
	 *
	 * <p>A backend that is merely initialized does not request rendering. Only an enabled output
	 * does. Dedicated {@link RenderMode} values override configured output routes without erasing
	 * them; {@link RenderMode#FULL} restores independent routing.</p>
	 *
	 * @param view view whose external-output requirement should be checked
	 * @return {@code true} when an enabled output effectively resolves to {@code view}
	 */
	public boolean requiresView(ziviDomeLive.ViewType view) {
		if (view == null) {
			return false;
		}

		if (isNdiEnabled() && resolveOutputView(ndiView) == view) {
			return true;
		}
		if (isSpoutEnabled() && resolveOutputView(spoutView) == view) {
			return true;
		}
		return isSyphonEnabled() && resolveOutputView(syphonView) == view;
	}

	/**
	 * Checks whether at least one external output is active.
	 *
	 * @return {@code true} when NDI or the valid local texture output is enabled
	 */
	public boolean isActive() {
		return isNdiEnabled() || isSpoutEnabled() || isSyphonEnabled();
	}

	/** Stops and releases every output owned by this manager. */
	public void stopOutput() {
		shutdownOutputs();
	}

	/**
	 * Finds the most specific message in a nested failure.
	 *
	 * @param error failure whose root cause should be inspected
	 * @return root-cause class and message
	 */
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

	/** Computes the packed RGBA line stride used by Devolay. */
	static int ndiLineStride(int width) {
		if (width <= 0) {
			throw new IllegalArgumentException("NDI frame width must be positive");
		}
		return Math.multiplyExact(width, NDI_BYTES_PER_PIXEL);
	}

	/** Writes Processing ARGB pixels as packed RGBA while preserving source row order. */
	static void writeArgbAsRgba(int[] argbPixels, int pixelCount, ByteBuffer rgbaBuffer) {
		if (argbPixels == null || rgbaBuffer == null || pixelCount < 0
				|| pixelCount > argbPixels.length
				|| rgbaBuffer.capacity() < Math.multiplyExact(pixelCount, NDI_BYTES_PER_PIXEL)) {
			throw new IllegalArgumentException("Invalid NDI pixel conversion buffers");
		}

		rgbaBuffer.clear();
		for (int index = 0; index < pixelCount; index++) {
			int pixel = argbPixels[index];
			rgbaBuffer.put((byte) ((pixel >>> 16) & 0xFF));
			rgbaBuffer.put((byte) ((pixel >>> 8) & 0xFF));
			rgbaBuffer.put((byte) (pixel & 0xFF));
			rgbaBuffer.put((byte) ((pixel >>> 24) & 0xFF));
		}
		rgbaBuffer.flip();
	}

	/**
	 * Reusable NDI frame slot.
	 *
	 * <p>The Processing ARGB copy is written on the draw thread. RGBA conversion and Devolay
	 * submission occur exclusively on the NDI worker.</p>
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

		/** Ensures the slot buffers exactly match the requested frame dimensions. */
		private void ensureCapacity(int requiredWidth, int requiredHeight) {
			int requiredPixels = Math.multiplyExact(requiredWidth, requiredHeight);
			int requiredBytes = Math.multiplyExact(requiredPixels, NDI_BYTES_PER_PIXEL);

			if (argbPixels == null || argbPixels.length != requiredPixels) {
				argbPixels = new int[requiredPixels];
			}

			if (rgbaBuffer == null || rgbaBuffer.capacity() != requiredBytes) {
				rgbaBuffer = ByteBuffer
						.allocateDirect(requiredBytes)
						.order(ByteOrder.LITTLE_ENDIAN);
			}
		}

		/** Converts the stored ARGB frame to RGBA and configures the reusable Devolay frame. */
		private void prepareDevolayFrame() {
			writeArgbAsRgba(argbPixels, pixelCount, rgbaBuffer);

			frame.setResolution(width, height);
			frame.setData(rgbaBuffer);
			frame.setFourCCType(NDI_FRAME_FOUR_CC_TYPE);
			frame.setLineStride(ndiLineStride(width));
			frame.setFormatType(NDI_FRAME_FORMAT_TYPE);
			frame.setFrameRate(frameRateNumerator, frameRateDenominator);
		}

		/** Releases the native Devolay frame and clears Java buffer references. */
		@Override
		public void close() {
			frame.close();
			argbPixels = null;
			rgbaBuffer = null;
		}
	}
}
