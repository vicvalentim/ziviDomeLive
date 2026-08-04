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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The `OutputManager` class manages the output of frames to various systems such as NDI, Spout, and Syphon.
 * It handles the initialization, configuration, and frame sending for these output methods.
 * Depending on the operating system, it sets up either Spout (Windows) or Syphon (macOS).
 */
public class OutputManager implements PConstants {

	/** Enum representing different output types. */
	public enum OutputType {
		/** NDI output type. */
		NDI,
		/** Spout output type. */
		SPOUT,
		/** Syphon output type. */
		SYPHON
	}

	private final Logger logger = LogManager.getLogger();
	private zividomelive.ViewType currentView;
	private final Map<OutputType, zividomelive.ViewType> outputViews;
	private final zividomelive parent;

	private DevolaySender ndiSender;
	private Spout spoutSender;
	private SyphonServer syphonServer;

	private boolean ndiEnabled = false;
	private boolean spoutEnabled = false;
	private boolean syphonEnabled = false;

	/** Per-type cached graphics reference — refreshed on init/reset/view change, not per frame. */
	private final Map<OutputType, PGraphicsOpenGL> cachedGraphics = new EnumMap<>(OutputType.class);
	private final boolean isMacOS;
	private final boolean isWindows;
	private ByteBuffer ndiBuffer;
	private DevolayVideoFrame reusableFrame; // Reusable NDI video frame

	/**
	 * Constructs the OutputManager, initializing it with the parent application instance.
	 * Determines the OS type to configure either Spout or Syphon.
	 *
	 * @param parent the zividomelive instance representing the main application
	 */
	public OutputManager(zividomelive parent) {
		this.parent = parent;
		this.currentView = zividomelive.ViewType.FISHEYE_DOMEMASTER;
		this.outputViews = new EnumMap<>(OutputType.class);
		
		// Initialize output views with default value
		for (OutputType type : OutputType.values()) {
			outputViews.put(type, zividomelive.ViewType.FISHEYE_DOMEMASTER);
		}

		String osName = System.getProperty("os.name").toLowerCase();
		this.isMacOS = osName.contains("mac");
		this.isWindows = osName.contains("win");
	}

	/**
	 * Gets the view type configured for a specific output type.
	 *
	 * @param outputType the output type to query
	 * @return the ViewType configured for the output, or FISHEYE_DOMEMASTER if not set
	 */
	public zividomelive.ViewType getViewForOutput(OutputType outputType) {
		return outputViews.getOrDefault(outputType, zividomelive.ViewType.FISHEYE_DOMEMASTER);
	}

	/**
	 * Sets the view type for a specific output type and refreshes the cached graphics reference.
	 *
	 * @param outputType the output type to configure
	 * @param viewType the ViewType to set
	 */
	public void setViewForOutput(OutputType outputType, zividomelive.ViewType viewType) {
		if (outputType != null && viewType != null) {
			outputViews.put(outputType, viewType);
			refreshCachedGraphics(outputType);
			logger.info("Set view for " + outputType + " to " + viewType);
		}
	}

	/**
	 * Refreshes the cached PGraphicsOpenGL reference for a single output type.
	 * Call this after changing the view for that output.
	 */
	private void refreshCachedGraphics(OutputType type) {
		zividomelive.ViewType viewType = outputViews.getOrDefault(type, zividomelive.ViewType.FISHEYE_DOMEMASTER);
		PGraphicsOpenGL pg = resolveGraphics(viewType);
		if (pg != null) {
			cachedGraphics.put(type, pg);
		} else {
			cachedGraphics.remove(type);
		}
	}

	/**
	 * Refreshes cached graphics references for all output types.
	 * Must be called from the Processing draw thread after renderers are (re)allocated.
	 */
	public void refreshCachedGraphics() {
		for (OutputType type : OutputType.values()) {
			refreshCachedGraphics(type);
		}
	}

	/**
	 * Resolves the PGraphicsOpenGL for a given view type from the parent renderers.
	 */
	private PGraphicsOpenGL resolveGraphics(zividomelive.ViewType viewType) {
		if (viewType == null) return null;
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
			logger.log(Level.WARNING, "resolveGraphics failed for " + viewType + ".", e);
			return null;
		}
	}

	/**
	 * Initializes NDI output if it is not already enabled.
	 */
	private void initNDI() {
		if (!ndiEnabled && ndiSender == null) {
			try {
				ndiSender = new DevolaySender("ziviDomeLive NDI Output");
				reusableFrame = new DevolayVideoFrame();
				ndiEnabled = true;
				logger.info("NDI output initialized.");
			} catch (ExceptionInInitializerError | UnsatisfiedLinkError | IllegalStateException e) {
				ndiSender = null;
				reusableFrame = null;
				ndiEnabled = false;
				logger.log(Level.WARNING, "initNDI failed: NDI unavailable on this platform.", e);
			}
		}
	}

	/**
	 * Sets up Syphon (for macOS) or Spout (for Windows) based on the OS.
	 * Initializes the corresponding output method if the platform is supported.
	 */
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
	 * Toggles the specified output method (NDI, Spout, or Syphon) on or off.
	 *
	 * @param method the name of the output method to toggle ("ndi", "spout", "syphon")
	 */
	public void toggleOutput(String method) {
		if (method == null || method.trim().isEmpty()) {
			logger.warning("Ignoring output toggle request with empty method.");
			return;
		}
		String normalizedMethod = method.trim().toLowerCase(Locale.ROOT);

		switch (normalizedMethod) {
			case "ndi":
				if (!ndiEnabled) {
					initNDI();
				} else {
					shutdownNDI();
				}
				break;
			case "spout":
				if (!isWindows) {
					logger.warning("Spout toggle ignored: unsupported platform.");
					return;
				}
				if (!spoutEnabled) {
					initSpout();
				} else {
					shutdownSpout();
				}
				break;
			case "syphon":
				if (!isMacOS) {
					logger.warning("Syphon toggle ignored: unsupported platform.");
					return;
				}
				if (!syphonEnabled) {
					initSyphon();
				} else {
					shutdownSyphon();
				}
				break;
			default:
				logger.warning("Unknown output method: " + normalizedMethod);
				break;
		}
	}

	/**
	 * Sets the view type for the output (legacy single-view setter).
	 * Updates all output types to the given view and refreshes the cache.
	 *
	 * @param viewType the desired view type
	 */
	public void setView(zividomelive.ViewType viewType) {
		if (currentView != viewType) {
			currentView = viewType;
			for (OutputType type : OutputType.values()) {
				outputViews.put(type, viewType);
			}
			refreshCachedGraphics();
			logger.info("Current view set to " + currentView);
		}
	}

	/**
	 * Sends the current frame to all enabled output methods.
	 * Syphon and Spout use cached PGraphicsOpenGL references (GPU-to-GPU, no pixel copy).
	 * NDI copies pixels via loadPixels() and sends asynchronously via ThreadManager.
	 */
	public void sendOutput() {
		if (ndiEnabled && ndiSender != null) {
			try {
				PGraphicsOpenGL ndiPg = cachedGraphics.get(OutputType.NDI);
				DevolayVideoFrame ndiFrame = ndiPg == null ? null : createNDIFrame(ndiPg);
				if (ndiFrame != null) {
					ThreadManager.submitRunnable(() -> {
						synchronized (this) {
							if (ndiSender != null && ndiEnabled) {
								ndiSender.sendVideoFrameAsync(ndiFrame);
							}
						}
					});
				}
			} catch (Exception e) {
				logger.log(Level.WARNING, "sendOutput skipped NDI frame due to error.", e);
			}
		}

		if (spoutEnabled && spoutSender != null && isWindows) {
			try {
				PGraphicsOpenGL spoutPg = cachedGraphics.get(OutputType.SPOUT);
				if (spoutPg != null) {
					spoutSender.sendTexture(spoutPg);
				}
			} catch (Exception e) {
				logger.log(Level.WARNING, "sendOutput skipped Spout frame due to error.", e);
			}
		}

		if (syphonEnabled && syphonServer != null && isMacOS) {
			try {
				PGraphicsOpenGL syphonPg = cachedGraphics.get(OutputType.SYPHON);
				if (syphonPg != null) {
					syphonServer.sendImage(syphonPg);
				}
			} catch (Exception e) {
				logger.log(Level.WARNING, "sendOutput skipped Syphon frame due to error.", e);
			}
		}
	}

	/**
	 * Creates an NDI video frame from the provided PGraphics in RGBA format.
	 * The Processing render target is already finalized by the render pipeline, so
	 * this method must not reopen the draw context with beginDraw/endDraw.
	 *
	 * @param pg the PGraphics instance containing the image data
	 * @return the created NDI video frame
	 */
 	private synchronized DevolayVideoFrame createNDIFrame(PGraphicsOpenGL pg) {
		if (pg == null || reusableFrame == null) {
			return null;
		}

		int width = pg.width;
		int height = pg.height;
		if (width <= 0 || height <= 0) {
			return null;
		}

		pg.loadPixels();
		int[] pixels = pg.pixels;
		if (pixels == null || pixels.length != width * height) {
			logger.warning("createNDIFrame skipped: pixel buffer is not available.");
			return null;
		}

		int byteCount = width * height * 4;
		if (ndiBuffer == null || ndiBuffer.capacity() != byteCount) {
			ndiBuffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.LITTLE_ENDIAN);
		}

		ndiBuffer.clear();
		for (int pixel : pixels) {
			ndiBuffer.put((byte) ((pixel >> 16) & 0xFF));
			ndiBuffer.put((byte) ((pixel >> 8) & 0xFF));
			ndiBuffer.put((byte) (pixel & 0xFF));
			ndiBuffer.put((byte) ((pixel >> 24) & 0xFF));
		}
		ndiBuffer.flip();

		reusableFrame.setResolution(width, height);
		reusableFrame.setData(ndiBuffer);
		reusableFrame.setFourCCType(DevolayFrameFourCCType.RGBA);
		reusableFrame.setLineStride(width * 4);
		reusableFrame.setFormatType(DevolayFrameFormatType.INTERLEAVED);
		reusableFrame.setFrameRate(150, 1);

		return reusableFrame;
	}

	/**
	 * Shuts down all output methods (NDI, Spout, Syphon).
	 */
	public void shutdownOutputs() {
		ndiEnabled = false;
		spoutEnabled = false;
		syphonEnabled = false;
		shutdownNDI();
		shutdownSpout();
		shutdownSyphon();
		logger.info("All output services have been shut down.");
	}

	/**
	 * Shuts down NDI output, releasing resources.
	 */
	private synchronized void shutdownNDI() {
		if (ndiSender != null) {
			ndiSender.close();
			ndiSender = null;
			logger.info("NDI output shut down.");
		}

		reusableFrame = null;
		ndiEnabled = false;
	}

	/**
	 * Shuts down Spout output, releasing resources.
	 */
	private void shutdownSpout() {
		if (spoutSender != null) {
			spoutSender.dispose();
			spoutSender = null;
			logger.info("Spout output shut down.");
		}
		spoutEnabled = false;
	}

	/**
	 * Shuts down Syphon output, releasing resources.
	 */
	private void shutdownSyphon() {
		if (syphonServer != null) {
			syphonServer.stop();
			syphonServer = null;
			logger.info("Syphon output shut down.");
		}
		syphonEnabled = false;
	}

	// Getter methods for each output method status

	/**
	 * Checks if NDI output is enabled.
	 *
	 * @return true if NDI output is enabled, false otherwise
	 */
	public boolean isNdiEnabled() {
		return ndiEnabled;
	}

	/**
	 * Checks if Spout output is enabled.
	 *
	 * @return true if Spout output is enabled, false otherwise
	 */
	public boolean isSpoutEnabled() {
		return spoutEnabled;
	}

	/**
	 * Checks if Syphon output is enabled.
	 *
	 * @return true if Syphon output is enabled, false otherwise
	 */
	public boolean isSyphonEnabled() {
		return syphonEnabled;
	}

	/**
	 * Sets the view type for NDI output.
	 *
	 * @param view the desired view type for NDI output
	 */
	public void setNdiView(zividomelive.ViewType view) {
		setViewForOutput(OutputType.NDI, view);
	}

	/**
	 * Sets the view type for Spout output.
	 *
	 * @param view the desired view type for Spout output
	 */
	public void setSpoutView(zividomelive.ViewType view) {
		setViewForOutput(OutputType.SPOUT, view);
	}

	/**
	 * Sets the view type for Syphon output.
	 *
	 * @param view the desired view type for Syphon output
	 */
	public void setSyphonView(zividomelive.ViewType view) {
		setViewForOutput(OutputType.SYPHON, view);
	}

	/**
	 * Checks if any output method (NDI, Spout, or Syphon) is currently active.
	 *
	 * @return true if any output method is enabled, false otherwise
	 */
	public boolean isActive() {
		return (ndiEnabled && ndiSender != null)
				|| (spoutEnabled && spoutSender != null)
				|| (syphonEnabled && syphonServer != null);
	}

	/**
	 * Stops all output methods and shuts down the OutputManager.
	 * This method ensures that all output resources are released.
	 */
	public void stopOutput() {
		shutdownOutputs();
	}
}
