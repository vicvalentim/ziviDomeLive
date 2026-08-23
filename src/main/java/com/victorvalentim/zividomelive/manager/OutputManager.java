package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.ViewType;

/**
 * Artist-facing control, routing, and telemetry for external frame publication.
 *
 * <p>Obtain this interface from
 * {@link com.victorvalentim.zividomelive.ziviDomeLive#getOutputManager()}; applications do not
 * construct output backends. NDI, Spout, and Syphon start disabled. Each transport keeps an
 * independent {@link ViewType} route while global dedicated render modes may temporarily override
 * the effective view.</p>
 *
 * <p>Mutating operations are intended for the Processing frame thread. NDI publication uses a
 * bounded worker queue; disabling NDI requests an asynchronous stop and may temporarily report
 * {@link OutputState#STOPPING}.</p>
 *
 * <p><strong>API stability:</strong> Advanced Stable.</p>
 *
 * @since 2.0.0
 */
public interface OutputManager {

	/** External transports supported by the core output coordinator. */
	enum OutputType {
		/** Network Device Interface video publication. */
		NDI,
		/** Windows GPU texture sharing. */
		SPOUT,
		/** macOS GPU texture sharing. */
		SYPHON
	}

	/** Observable lifecycle state of one output transport. */
	enum OutputState {
		/** Unsupported on this platform or unavailable after initialization failure. */
		UNAVAILABLE,
		/** Supported but not initialized or enabled. */
		AVAILABLE,
		/** Native resources are prepared but publication is disabled. */
		INITIALIZED,
		/** Publication is enabled. */
		ENABLED,
		/** An asynchronous stop is in progress. */
		STOPPING
	}

	/**
	 * Returns the independently configured route for one transport.
	 *
	 * @param outputType transport to inspect
	 * @return configured view, or {@link ViewType#DOMEMASTER} when {@code outputType} is null
	 */
	ViewType getViewForOutput(OutputType outputType);

	/**
	 * Selects the logical view routed to one transport.
	 *
	 * @param outputType transport to configure; a null value is ignored
	 * @param viewType logical view to publish; a null value is ignored
	 */
	void setViewForOutput(OutputType outputType, ViewType viewType);

	/** @param view logical view routed to NDI; {@code null} is ignored */
	void setNdiView(ViewType view);

	/** @param view logical view routed to Spout; {@code null} is ignored */
	void setSpoutView(ViewType view);

	/** @param view logical view routed to Syphon; {@code null} is ignored */
	void setSyphonView(ViewType view);

	/**
	 * Selects the logical view for the platform-local texture-sharing backend.
	 *
	 * @param view view routed to Spout on Windows or Syphon on macOS; {@code null} is ignored
	 */
	void setLocalTextureView(ViewType view);

	/**
	 * @return Spout's configured view on Windows, Syphon's on macOS, or
	 *         {@link ViewType#DOMEMASTER} when no local backend exists
	 */
	ViewType getLocalTextureView();

	/**
	 * Enables or disables one transport.
	 *
	 * @param outputType non-null transport
	 * @param enabled {@code true} to enable publication
	 * @throws IllegalArgumentException when {@code outputType} is null
	 */
	void setOutputEnabled(OutputType outputType, boolean enabled);

	/**
	 * @param outputType non-null transport
	 * @return {@code true} only while that transport is enabled
	 * @throws IllegalArgumentException when {@code outputType} is null
	 */
	boolean isOutputEnabled(OutputType outputType);

	/**
	 * Toggles one transport's enabled state.
	 *
	 * @param outputType non-null transport
	 * @throws IllegalArgumentException when {@code outputType} is null
	 */
	void toggleOutput(OutputType outputType);

	/**
	 * @param outputType transport to inspect
	 * @return current lifecycle state, or {@link OutputState#UNAVAILABLE} for null
	 */
	OutputState getOutputState(OutputType outputType);

	/**
	 * @param outputType transport to inspect
	 * @return latest backend failure reason, or an empty string when none is recorded
	 */
	String getOutputFailureReason(OutputType outputType);

	/** @return whether NDI publication is enabled */
	boolean isNdiEnabled();

	/** @return whether Windows Spout publication is initialized and enabled */
	boolean isSpoutEnabled();

	/** @return whether macOS Syphon publication is initialized and enabled */
	boolean isSyphonEnabled();

	/** @return whether this platform has a usable Spout or Syphon backend */
	boolean isLocalTextureAvailable();

	/** @return whether the platform-local backend has prepared native resources */
	boolean isLocalTextureInitialized();

	/** @return {@code "Spout"}, {@code "Syphon"}, or {@code "None"} */
	String getLocalTextureBackendName();

	/**
	 * Sets rational frame-rate metadata for subsequently captured NDI frames.
	 *
	 * @param numerator positive frame-rate numerator
	 * @param denominator positive frame-rate denominator
	 * @throws IllegalArgumentException if either value is not positive
	 */
	void setNdiFrameRate(int numerator, int denominator);

	/** @return cumulative NDI frames copied into bounded capture slots */
	long getNdiCapturedFrames();

	/** @return cumulative NDI frames sent by the dedicated worker */
	long getNdiSentFrames();

	/** @return cumulative NDI frames discarded by latest-frame backpressure */
	long getNdiDroppedFrames();

	/** @return cumulative NDI frames rejected by capture or sender failures */
	long getNdiFailedFrames();

	/** @return whether at least one external transport is enabled */
	boolean isActive();
}
