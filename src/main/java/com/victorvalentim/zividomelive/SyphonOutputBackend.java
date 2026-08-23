package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.manager.OutputManager;
import codeanticode.syphon.SyphonServer;
import com.victorvalentim.zividomelive.performance.PerformanceMetric;
import processing.core.PApplet;
import processing.opengl.PGraphicsOpenGL;

import java.util.logging.Logger;

/** Concrete macOS Syphon backend that publishes Processing textures directly. */
final class SyphonOutputBackend {

	private static final String SERVER_NAME = "ziviDomeLive Syphon";

	private final Logger logger = LogManager.getLogger();
	private final PApplet parent;
	private final boolean supported;

	private SyphonServer server;
	private volatile boolean enabled;
	private volatile boolean unavailable;
	private volatile String failureReason = "";

	SyphonOutputBackend(PApplet parent, boolean supported) {
		this.parent = parent;
		this.supported = supported;
	}

	/** Creates and warms up the native server without enabling publication. */
	void initialize() {
		if (!supported || server != null || unavailable) {
			return;
		}

		SyphonServer newServer = null;
		try {
			newServer = new SyphonServer(parent, SERVER_NAME);

			/* Force Processing-Syphon's lazy native initialization during startup. */
			newServer.hasClients();

			server = newServer;
			unavailable = false;
			failureReason = "";
			logger.info("Syphon backend initialized and ready; frame publication remains disabled.");
		} catch (Exception | LinkageError error) {
			if (newServer != null) {
				try {
					newServer.stop();
				} catch (Exception | LinkageError cleanupError) {
					logger.warning(
							"Failed to release Syphon after initialization error: "
									+ OutputManagerImpl.rootCauseMessage(cleanupError)
					);
				}
			}

			server = null;
			enabled = false;
			markUnavailable(error);
		}
	}

	/** Enables or disables publication without destroying the native server. */
	void setEnabled(boolean requested) {
		if (!requested) {
			enabled = false;
			return;
		}
		if (!supported || isEnabled()) {
			return;
		}
		if (server == null) {
			prepareRetry();
			initialize();
		}

		if (server == null) {
			logger.warning(
					"Syphon cannot be enabled: "
							+ (failureReason.isEmpty()
							? "backend is not initialized"
							: failureReason)
			);
			return;
		}

		enabled = true;
		logger.info("Syphon frame publication enabled.");
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
			server.sendImage(graphics);
			failureReason = "";
		} catch (Exception | LinkageError error) {
			enabled = false;
			failureReason = OutputManagerImpl.rootCauseMessage(error);
			logger.warning(
					"Syphon frame publication failed and was disabled without destroying the backend: "
							+ failureReason
			);
		} finally {
			if (profiling) monitor.record(PerformanceMetric.SYPHON, started);
		}
	}

	/** Releases the native server during terminal output shutdown. */
	void shutdown() {
		enabled = false;
		SyphonServer currentServer = server;
		server = null;

		if (currentServer != null) {
			try {
				currentServer.stop();
				logger.info("Syphon backend released.");
			} catch (Exception | LinkageError error) {
				failureReason = OutputManagerImpl.rootCauseMessage(error);
				logger.warning("Failed to stop the Syphon server: " + failureReason);
			}
		}
	}

	private void markUnavailable(Throwable error) {
		unavailable = true;
		failureReason = OutputManagerImpl.rootCauseMessage(error);
		logger.warning("Syphon backend initialization failed: " + failureReason);
	}

	private void prepareRetry() {
		if (!unavailable) {
			return;
		}
		logger.info("Retrying Syphon backend initialization.");
		unavailable = false;
		failureReason = "";
	}

	OutputManager.OutputState state() {
		return state(isEnabled());
	}

	OutputManager.OutputState state(boolean effectivelyEnabled) {
		return OutputManagerImpl.resolveOutputState(
				supported, unavailable, server != null, effectivelyEnabled, false);
	}

	boolean isSupported() {
		return supported;
	}

	boolean isEnabled() {
		return supported && enabled && server != null;
	}

	boolean isInitialized() {
		return server != null;
	}

	String failureReason() {
		return failureReason;
	}
}
