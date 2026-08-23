package com.victorvalentim.zividomelive;

// Package-private implementation grouped physically under _internal/output.

import com.victorvalentim.zividomelive.manager.OutputManager;
import com.victorvalentim.zividomelive.performance.PerformanceMetric;
import processing.core.PApplet;
import processing.opengl.PGraphicsOpenGL;
import spout.Spout;

import java.util.logging.Logger;

/** Concrete Windows Spout backend that publishes Processing textures directly. */
final class SpoutOutputBackend {

	private static final String SENDER_NAME = "ziviDomeLive Spout";

	private final Logger logger = LogManager.getLogger();
	private final PApplet parent;
	private final boolean supported;

	private Spout sender;
	private volatile boolean enabled;
	private volatile boolean unavailable;
	private volatile String failureReason = "";
	private int width = -1;
	private int height = -1;

	SpoutOutputBackend(PApplet parent, boolean supported) {
		this.parent = parent;
		this.supported = supported;
	}

	/** Creates the native sender without enabling publication. */
	void initialize(PGraphicsOpenGL graphics, int initialResolution) {
		if (!supported || sender != null || unavailable) {
			return;
		}

		Spout newSender = null;
		try {
			int initialWidth;
			int initialHeight;

			if (graphics != null && graphics.width > 0 && graphics.height > 0) {
				initialWidth = graphics.width;
				initialHeight = graphics.height;
			} else {
				initialWidth = initialResolution;
				initialHeight = initialResolution;
			}

			newSender = new Spout(parent);
			boolean created = newSender.createSender(SENDER_NAME, initialWidth, initialHeight);

			if (!created) {
				newSender.dispose();
				throw new IllegalStateException("Spout createSender returned false");
			}

			sender = newSender;
			width = initialWidth;
			height = initialHeight;
			unavailable = false;
			failureReason = "";

			logger.info(
					"Spout backend initialized at " + initialWidth + "x" + initialHeight
							+ "; frame publication remains disabled."
			);
		} catch (Exception | LinkageError error) {
			if (newSender != null && newSender != sender) {
				try {
					newSender.dispose();
				} catch (Exception | LinkageError cleanupError) {
					logger.warning(
							"Failed to release Spout after initialization error: "
									+ OutputManagerImpl.rootCauseMessage(cleanupError)
					);
				}
			}

			disposeSender();
			enabled = false;
			width = -1;
			height = -1;
			markUnavailable(error);
		}
	}

	/** Enables or disables publication without destroying the native sender. */
	void setEnabled(boolean requested, PGraphicsOpenGL graphics, int initialResolution) {
		if (!requested) {
			enabled = false;
			return;
		}
		if (!supported || isEnabled()) {
			return;
		}
		if (sender == null) {
			prepareRetry();
			initialize(graphics, initialResolution);
		}

		if (sender == null) {
			logger.warning(
					"Spout cannot be enabled: "
							+ (failureReason.isEmpty()
							? "backend is not initialized"
							: failureReason)
			);
			return;
		}

		enabled = true;
		logger.info("Spout frame publication enabled.");
	}

	/** Publishes a completed Processing texture with no CPU readback. */
	void send(PGraphicsOpenGL graphics) {
		if (!isEnabled() || graphics == null) {
			return;
		}

		PerformanceMonitor monitor = PerformanceMonitor.current();
		boolean profiling = monitor != null && monitor.isEnabled();
		long started = profiling ? monitor.start() : 0L;
		try {
			if (graphics.width != width || graphics.height != height) {
				sender.updateSender(graphics.width, graphics.height);
				width = graphics.width;
				height = graphics.height;
			}

			sender.sendTexture(graphics);
			failureReason = "";
		} catch (Exception | LinkageError error) {
			enabled = false;
			failureReason = OutputManagerImpl.rootCauseMessage(error);
			logger.warning(
					"Spout frame publication failed and was disabled without destroying the backend: "
							+ failureReason
			);
		} finally {
			if (profiling) monitor.record(PerformanceMetric.SPOUT, started);
		}
	}

	/** Updates native dimensions after a deferred renderer reset. */
	void notifyResolutionChanged(PGraphicsOpenGL graphics) {
		if (sender == null) {
			return;
		}

		if (graphics == null || graphics.width <= 0 || graphics.height <= 0) {
			width = -1;
			height = -1;
			return;
		}

		try {
			sender.updateSender(graphics.width, graphics.height);
			width = graphics.width;
			height = graphics.height;
			failureReason = "";
		} catch (Exception | LinkageError error) {
			enabled = false;
			failureReason = OutputManagerImpl.rootCauseMessage(error);
			logger.warning(
					"Spout resolution update failed; publication was disabled without "
							+ "destroying the sender: "
							+ failureReason
			);
		}
	}

	/** Releases the native sender during terminal output shutdown. */
	void shutdown() {
		enabled = false;
		disposeSender();
		width = -1;
		height = -1;
	}

	private void disposeSender() {
		Spout currentSender = sender;
		sender = null;

		if (currentSender != null) {
			try {
				currentSender.dispose();
				logger.info("Spout backend released.");
			} catch (Exception | LinkageError error) {
				failureReason = OutputManagerImpl.rootCauseMessage(error);
				logger.warning("Failed to dispose the Spout sender: " + failureReason);
			}
		}
	}

	private void markUnavailable(Throwable error) {
		unavailable = true;
		failureReason = OutputManagerImpl.rootCauseMessage(error);
		logger.warning("Spout backend initialization failed: " + failureReason);
	}

	private void prepareRetry() {
		if (!unavailable) {
			return;
		}
		logger.info("Retrying Spout backend initialization.");
		unavailable = false;
		failureReason = "";
	}

	OutputManager.OutputState state() {
		return state(isEnabled());
	}

	OutputManager.OutputState state(boolean effectivelyEnabled) {
		return OutputManagerImpl.resolveOutputState(
				supported, unavailable, sender != null, effectivelyEnabled, false);
	}

	boolean isSupported() {
		return supported;
	}

	boolean isEnabled() {
		return supported && enabled && sender != null;
	}

	boolean isInitialized() {
		return sender != null;
	}

	String failureReason() {
		return failureReason;
	}
}
