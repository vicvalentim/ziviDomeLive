package com.victorvalentim.zividomelive;

// Package-private implementation contract grouped physically under _internal/output.

import com.victorvalentim.zividomelive.manager.OutputManager;
import processing.opengl.PGraphicsOpenGL;

/** Optional platform-local texture output isolated from its Processing library. */
interface LocalTextureOutputBackend {

	void initialize(PGraphicsOpenGL graphics, int initialResolution);

	void setEnabled(boolean requested, PGraphicsOpenGL graphics, int initialResolution);

	void send(PGraphicsOpenGL graphics);

	void notifyResolutionChanged(PGraphicsOpenGL graphics);

	void shutdown();

	OutputManager.OutputState state(boolean effectivelyEnabled);

	boolean isEnabled();

	boolean isInitialized();

	String failureReason();
}
