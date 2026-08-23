package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.ViewType;

/** Artist-facing control and telemetry contract for external frame publication. */
public interface OutputManager {

	/** External transports supported by the core output coordinator. */
	enum OutputType {
		NDI,
		SPOUT,
		SYPHON
	}

	/** Observable lifecycle state of one output transport. */
	enum OutputState {
		UNAVAILABLE,
		AVAILABLE,
		INITIALIZED,
		ENABLED,
		STOPPING
	}

	ViewType getViewForOutput(OutputType outputType);

	void setViewForOutput(OutputType outputType, ViewType viewType);

	void setNdiView(ViewType view);

	void setSpoutView(ViewType view);

	void setSyphonView(ViewType view);

	void setLocalTextureView(ViewType view);

	ViewType getLocalTextureView();

	void setOutputEnabled(OutputType outputType, boolean enabled);

	boolean isOutputEnabled(OutputType outputType);

	void toggleOutput(OutputType outputType);

	OutputState getOutputState(OutputType outputType);

	String getOutputFailureReason(OutputType outputType);

	boolean isNdiEnabled();

	boolean isSpoutEnabled();

	boolean isSyphonEnabled();

	boolean isLocalTextureAvailable();

	boolean isLocalTextureInitialized();

	String getLocalTextureBackendName();

	void setNdiFrameRate(int numerator, int denominator);

	long getNdiCapturedFrames();

	long getNdiSentFrames();

	long getNdiDroppedFrames();

	long getNdiFailedFrames();

	boolean isActive();
}
