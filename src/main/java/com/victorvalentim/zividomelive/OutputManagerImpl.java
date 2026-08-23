package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.manager.OutputManager;
import com.victorvalentim.zividomelive.support.LogManager;
import me.walkerknapp.devolay.DevolayFrameFormatType;
import me.walkerknapp.devolay.DevolayFrameFourCCType;
import processing.opengl.PGraphicsOpenGL;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Coordinates external-output routing and delegates publication to concrete backends.
 *
 * <p>NDI, Syphon, and Spout own their native resources and lifecycle independently.
 * This manager retains the public routing API, resolves logical {@link ViewType} selections,
 * and supplies completed {@link FrameViews} targets without implementing backend details.</p>
 */
class OutputManagerImpl implements OutputManager {

	/** The single platform-local texture-sharing implementation available in this process. */
	private enum LocalTextureBackend {
		SYPHON,
		SPOUT,
		NONE
	}

	private static final long DEFAULT_NDI_SHUTDOWN_TIMEOUT_MILLIS =
			NdiOutputBackend.DEFAULT_SHUTDOWN_TIMEOUT_MILLIS;

	/** Default NDI metadata retained for compatibility and qualification. */
	static final int DEFAULT_NDI_FRAME_RATE_NUMERATOR =
			NdiOutputBackend.DEFAULT_FRAME_RATE_NUMERATOR;
	static final int DEFAULT_NDI_FRAME_RATE_DENOMINATOR =
			NdiOutputBackend.DEFAULT_FRAME_RATE_DENOMINATOR;
	static final DevolayFrameFourCCType NDI_FRAME_FOUR_CC_TYPE =
			NdiOutputBackend.FRAME_FOUR_CC_TYPE;
	static final DevolayFrameFormatType NDI_FRAME_FORMAT_TYPE =
			NdiOutputBackend.FRAME_FORMAT_TYPE;

	private final Logger logger = LogManager.getLogger();
	private final ziviDomeLive parent;
	private final LocalTextureBackend localTextureBackend;
	private final NdiOutputBackend ndiBackend;
	private final SpoutOutputBackend spoutBackend;
	private final SyphonOutputBackend syphonBackend;
	private FrameViews latestFrameViews;

	/* Independent output routing. Preview/viewer state is intentionally not stored here. */
	private volatile ViewType ndiView = ViewType.DOMEMASTER;
	private volatile ViewType spoutView = ViewType.DOMEMASTER;
	private volatile ViewType syphonView = ViewType.DOMEMASTER;

	/** Prevents repeated warnings from the deprecated single-view setter. */
	/**
	 * Creates an output manager and the three concrete backend services.
	 *
	 * @param parent main ziviDomeLive application; must not be {@code null}
	 * @throws IllegalArgumentException if {@code parent} is {@code null}
	 */
	OutputManagerImpl(ziviDomeLive parent) {
		this(parent, DEFAULT_NDI_SHUTDOWN_TIMEOUT_MILLIS);
	}

	/** Package-private constructor for deterministic worker-shutdown tests. */
	OutputManagerImpl(ziviDomeLive parent, long ndiShutdownTimeoutMillis) {
		if (parent == null) {
			throw new IllegalArgumentException("parent cannot be null");
		}
		if (ndiShutdownTimeoutMillis <= 0) {
			throw new IllegalArgumentException("NDI shutdown timeout must be positive");
		}

		this.parent = parent;

		String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		boolean isMacOS = osName.contains("mac");
		boolean isWindows = osName.contains("win");

		if (isMacOS) {
			localTextureBackend = LocalTextureBackend.SYPHON;
		} else if (isWindows) {
			localTextureBackend = LocalTextureBackend.SPOUT;
		} else {
			localTextureBackend = LocalTextureBackend.NONE;
		}

		ndiBackend = new NdiOutputBackend(parent.getTargetFrameRate(), ndiShutdownTimeoutMillis);
		spoutBackend = new SpoutOutputBackend(parent.getPApplet(), isWindows);
		syphonBackend = new SyphonOutputBackend(parent.getPApplet(), isMacOS);
	}

	/**
	 * Returns the independently configured view for an output.
	 *
	 * @param outputType output whose configured view should be returned
	 * @return configured view, or {@link ViewType#DOMEMASTER} for {@code null}
	 */
	public ViewType getViewForOutput(OutputType outputType) {
		if (outputType == null) {
			return ViewType.DOMEMASTER;
		}

		switch (outputType) {
			case NDI:
				return ndiView;
			case SPOUT:
				return spoutView;
			case SYPHON:
				return syphonView;
			default:
				return ViewType.DOMEMASTER;
		}
	}

	/**
	 * Changes only the selected external-output view.
	 *
	 * @param outputType output whose route should change
	 * @param viewType logical view to publish
	 */
	public void setViewForOutput(OutputType outputType, ViewType viewType) {
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

	/** Resolves a completed graphics target under the effective global render mode. */
	PGraphicsOpenGL resolveGraphics(FrameViews frameViews, ViewType viewType) {
		ViewType effectiveView = resolveOutputView(viewType);
		if (frameViews == null || effectiveView == null) {
			return null;
		}

		try {
			return frameViews.getFrame(effectiveView);
		} catch (RuntimeException error) {
			logger.warning("resolveGraphics failed for " + effectiveView + ": "
					+ rootCauseMessage(error));
			return null;
		}
	}

	/** Resolves a configured output route under the facade's global render mode. */
	ViewType resolveOutputView(ViewType configuredView) {
		RenderMode renderMode = parent.getRenderMode();
		if (renderMode == null || renderMode == RenderMode.FULL) {
			return configuredView;
		}

		switch (renderMode) {
			case STANDARD:
				return ViewType.STANDARD;
			case DOMEMASTER:
				return ViewType.DOMEMASTER;
			case EQUIRECTANGULAR:
				return ViewType.EQUIRECTANGULAR;
			case SKYBOX:
				return ViewType.SKYBOX;
			case FULL:
			default:
				return configuredView;
		}
	}

	/** Prepares the supported local GPU backend without enabling publication. */
	void initializeLocalTextureOutput() {
		initializeLocalTextureBackend(latestFrameViews);
	}

	/**
	 * Prepares the supported local GPU backend using final views for initial dimensions.
	 *
	 * @param frameViews final-frame contract supplied by the render pipeline
	 * @since 2.0.0
	 */
	void initializeLocalTextureOutput(FrameViews frameViews) {
		if (frameViews != null) {
			latestFrameViews = frameViews;
		}
		initializeLocalTextureOutput();
	}

	private void initializeLocalTextureBackend(FrameViews frameViews) {
		switch (localTextureBackend) {
			case SYPHON:
				syphonBackend.initialize();
				break;
			case SPOUT:
				spoutBackend.initialize(
						resolveGraphics(frameViews, spoutView), parent.getOutputResolution());
				break;
			case NONE:
			default:
				break;
		}
	}

	@Override
	public void setOutputEnabled(OutputType outputType, boolean enabled) {
		requireOutputType(outputType);
		switch (outputType) {
			case NDI:
				if (enabled) {
					ndiBackend.enable();
				} else {
					ndiBackend.requestStop();
				}
				break;
			case SPOUT:
				spoutBackend.setEnabled(
						enabled,
						resolveGraphics(latestFrameViews, spoutView),
						parent.getOutputResolution());
				break;
			case SYPHON:
				syphonBackend.setEnabled(enabled);
				break;
			default:
				throw new IllegalArgumentException("Unsupported output type: " + outputType);
		}
	}

	@Override
	public boolean isOutputEnabled(OutputType outputType) {
		requireOutputType(outputType);
		return switch (outputType) {
			case NDI -> isNdiEnabled();
			case SPOUT -> isSpoutEnabled();
			case SYPHON -> isSyphonEnabled();
		};
	}

	@Override
	public void toggleOutput(OutputType outputType) {
		setOutputEnabled(outputType, !isOutputEnabled(outputType));
	}

	/**
	 * Supplies the current final-frame boundary and publishes through enabled backends.
	 *
	 * @param frameViews completed final views supplied by the render pipeline
	 * @since 2.0.0
	 */
	void sendOutput(FrameViews frameViews) {
		if (frameViews == null) {
			return;
		}
		latestFrameViews = frameViews;
		sendOutput();
	}

	/** Republishes the most recently supplied final-frame contract. */
	void sendOutput() {
		FrameViews frameViews = latestFrameViews;
		if (frameViews == null) {
			return;
		}

		if (spoutBackend.isEnabled()) {
			spoutBackend.send(resolveGraphics(frameViews, spoutView));
		} else if (syphonBackend.isEnabled()) {
			syphonBackend.send(resolveGraphics(frameViews, syphonView));
		}

		if (ndiBackend.isEnabled()) {
			ndiBackend.capture(resolveGraphics(frameViews, ndiView));
		}
	}

	/**
	 * Notifies Spout that deferred output target dimensions changed.
	 *
	 * @param newResolution new configured output resolution in pixels
	 */
	void notifyResolutionChanged(int newResolution) {
		if (newResolution <= 0) {
			logger.warning("Ignoring invalid output resolution: " + newResolution);
			return;
		}

		if (localTextureBackend == LocalTextureBackend.SPOUT && spoutBackend.isInitialized()) {
			spoutBackend.notifyResolutionChanged(
					resolveGraphics(latestFrameViews, spoutView));
		}
	}

	/**
	 * Changes NDI frame-rate metadata for subsequently captured frames.
	 *
	 * @param numerator positive frame-rate numerator
	 * @param denominator positive frame-rate denominator
	 * @throws IllegalArgumentException if either value is zero or negative
	 */
	public void setNdiFrameRate(int numerator, int denominator) {
		ndiBackend.setFrameRate(numerator, denominator);
	}

	/** Shuts down every output and releases all native resources during terminal disposal. */
	void shutdownOutputsTerminal() {
		ndiBackend.shutdownTerminal();
		spoutBackend.shutdown();
		syphonBackend.shutdown();
		logger.info("All output services have been shut down.");
	}

	/**
	 * Returns the lifecycle state of one output backend.
	 *
	 * @param outputType output to inspect
	 * @return current state, or {@link OutputState#UNAVAILABLE} for {@code null}
	 */
	public OutputState getOutputState(OutputType outputType) {
		if (outputType == null) {
			return OutputState.UNAVAILABLE;
		}

		switch (outputType) {
			case NDI:
				return ndiBackend.state(isNdiEnabled());
			case SPOUT:
				return spoutBackend.state(isSpoutEnabled());
			case SYPHON:
				return syphonBackend.state(isSyphonEnabled());
			default:
				return OutputState.UNAVAILABLE;
		}
	}

	/**
	 * Returns the latest failure reason recorded by one backend.
	 *
	 * @param outputType output to inspect
	 * @return diagnostic text, or an empty string when no failure was recorded
	 */
	public String getOutputFailureReason(OutputType outputType) {
		if (outputType == OutputType.NDI) {
			return ndiBackend.failureReason();
		}
		if (outputType == OutputType.SPOUT && localTextureBackend == LocalTextureBackend.SPOUT) {
			return spoutBackend.failureReason();
		}
		if (outputType == OutputType.SYPHON && localTextureBackend == LocalTextureBackend.SYPHON) {
			return syphonBackend.failureReason();
		}
		return "";
	}

	/** Pure lifecycle-state reducer shared by the concrete backends. */
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
		return ndiBackend.isEnabled();
	}

	/**
	 * Reports whether Spout publication is enabled.
	 *
	 * @return {@code true} on Windows when Spout is initialized and enabled
	 */
	public boolean isSpoutEnabled() {
		return spoutBackend.isEnabled();
	}

	/**
	 * Reports whether Syphon publication is enabled.
	 *
	 * @return {@code true} on macOS when Syphon is initialized and enabled
	 */
	public boolean isSyphonEnabled() {
		return syphonBackend.isEnabled();
	}

	/**
	 * Reports whether the supported local texture backend has been prepared.
	 *
	 * @return {@code true} when Syphon or Spout initialization completed
	 */
	public boolean isLocalTextureInitialized() {
		OutputState state = getOutputState(localOutputType());
		return state == OutputState.INITIALIZED || state == OutputState.ENABLED;
	}

	/**
	 * Reports whether platform-local texture sharing is currently available.
	 *
	 * @return {@code true} when the supported backend has not failed initialization
	 */
	public boolean isLocalTextureAvailable() {
		return getOutputState(localOutputType()) != OutputState.UNAVAILABLE;
	}

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
	 * @param view logical view to route to NDI
	 */
	public void setNdiView(ViewType view) {
		setViewForOutput(OutputType.NDI, view);
	}

	/**
	 * Selects the view sent through Spout.
	 *
	 * @param view logical view to route to Spout
	 */
	public void setSpoutView(ViewType view) {
		setViewForOutput(OutputType.SPOUT, view);
	}

	/**
	 * Selects the view sent through Syphon.
	 *
	 * @param view logical view to route to Syphon
	 */
	public void setSyphonView(ViewType view) {
		setViewForOutput(OutputType.SYPHON, view);
	}

	/**
	 * Sets the view routed to the supported platform-local texture backend.
	 *
	 * @param view logical view to route to Syphon or Spout
	 */
	public void setLocalTextureView(ViewType view) {
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
	 * Returns the configured view for the supported local texture backend.
	 *
	 * @return Spout view on Windows, Syphon view on macOS, or domemaster otherwise
	 */
	public ViewType getLocalTextureView() {
		switch (localTextureBackend) {
			case SPOUT:
				return spoutView;
			case SYPHON:
				return syphonView;
			case NONE:
			default:
				return ViewType.DOMEMASTER;
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
		return ndiBackend.capturedFrames();
	}

	/**
	 * Returns the number of NDI frames sent by the worker.
	 *
	 * @return total sent NDI frames
	 */
	public long getNdiSentFrames() {
		return ndiBackend.sentFrames();
	}

	/**
	 * Returns the number of NDI frames discarded by latest-frame backpressure.
	 *
	 * @return total dropped NDI frames
	 */
	public long getNdiDroppedFrames() {
		return ndiBackend.droppedFrames();
	}

	/**
	 * Returns the number of NDI frames rejected by capture or sender failures.
	 *
	 * @return total failed NDI frames
	 */
	public long getNdiFailedFrames() {
		return ndiBackend.failedFrames();
	}

	/**
	 * Reports whether an enabled output effectively requires a logical view.
	 *
	 * @param view logical view whose requirement should be checked
	 * @return {@code true} when an enabled output resolves to {@code view}
	 */
	boolean requiresView(ViewType view) {
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
	 * @return {@code true} when NDI, Spout, or Syphon is enabled
	 */
	public boolean isActive() {
		return isNdiEnabled() || isSpoutEnabled() || isSyphonEnabled();
	}

	/** Disables publication promptly without releasing local native backends. */
	void disableAll() {
		for (OutputType outputType : OutputType.values()) {
			setOutputEnabled(outputType, false);
		}
	}

	/** Compatibility wrapper for the bounded NDI worker wait. */
	static boolean waitForWorker(Thread worker, long timeoutMillis) {
		return NdiOutputBackend.waitForWorker(worker, timeoutMillis);
	}

	/** Compatibility wrapper for packed NDI line-stride calculation. */
	static int ndiLineStride(int width) {
		return NdiOutputBackend.lineStride(width);
	}

	/** Compatibility wrapper for Processing ARGB to packed RGBA conversion. */
	static void writeArgbAsRgba(int[] argbPixels, int pixelCount, ByteBuffer rgbaBuffer) {
		NdiOutputBackend.writeArgbAsRgba(argbPixels, pixelCount, rgbaBuffer);
	}

	/** Finds the most specific message in a nested backend failure. */
	static String rootCauseMessage(Throwable error) {
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

	int ndiFrameRateNumerator() {
		return ndiBackend.frameRateNumerator();
	}

	int ndiFrameRateDenominator() {
		return ndiBackend.frameRateDenominator();
	}

	private static OutputType requireOutputType(OutputType outputType) {
		if (outputType == null) {
			throw new IllegalArgumentException("Output type cannot be null.");
		}
		return outputType;
	}
}
