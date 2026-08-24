package com.victorvalentim.zividomelive;

// Package-private implementation grouped physically under _internal/output.

import com.victorvalentim.zividomelive.manager.OutputManager;
import processing.opengl.PGraphicsOpenGL;

/** Fail-soft backend used when an optional Processing output library cannot be linked. */
final class UnavailableLocalTextureOutputBackend implements LocalTextureOutputBackend {

	private final String failureReason;

	UnavailableLocalTextureOutputBackend(String failureReason) {
		this.failureReason = failureReason == null ? "optional backend unavailable" : failureReason;
	}

	@Override
	public void initialize(PGraphicsOpenGL graphics, int initialResolution) {
		// Unavailable by construction.
	}

	@Override
	public void setEnabled(boolean requested, PGraphicsOpenGL graphics, int initialResolution) {
		// Unavailable by construction.
	}

	@Override
	public void send(PGraphicsOpenGL graphics) {
		// Unavailable by construction.
	}

	@Override
	public void notifyResolutionChanged(PGraphicsOpenGL graphics) {
		// Unavailable by construction.
	}

	@Override
	public void shutdown() {
		// No native resource was created.
	}

	@Override
	public OutputManager.OutputState state(boolean effectivelyEnabled) {
		return OutputManager.OutputState.UNAVAILABLE;
	}

	@Override
	public boolean isEnabled() {
		return false;
	}

	@Override
	public boolean isInitialized() {
		return false;
	}

	@Override
	public String failureReason() {
		return failureReason;
	}
}
