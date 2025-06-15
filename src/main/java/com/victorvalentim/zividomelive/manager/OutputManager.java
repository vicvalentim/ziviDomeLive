package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.support.LogManager;
import com.victorvalentim.zividomelive.support.ThreadManager;
import com.victorvalentim.zividomelive.zividomelive;
import me.walkerknapp.devolay.*;
import processing.core.PConstants;
import codeanticode.syphon.SyphonServer;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;
import spout.Spout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.lang.reflect.Method;

import java.util.logging.Logger;

/**
 * The `OutputManager` class manages the output of frames to various systems such as NDI, Spout, and Syphon.
 * It handles the initialization, configuration, and frame sending for these output methods.
 * Depending on the operating system, it sets up either Spout (Windows) or Syphon (macOS).
 */
public class OutputManager implements PConstants {

	private final Logger logger = LogManager.getLogger();
	private zividomelive.ViewType currentView;
	private zividomelive.ViewType ndiView;
	private zividomelive.ViewType spoutView;
	private zividomelive.ViewType syphonView;
	private final zividomelive parent;

	private DevolaySender ndiSender;
	private Spout spoutSender;
	private SyphonServer syphonServer;

	private boolean ndiEnabled = false;
	private boolean spoutEnabled = false;
	private boolean syphonEnabled = false;

        private PGraphicsOpenGL outputGraphics;
        private final boolean isMacOS;
        private final boolean isWindows;
        private int lastWidth = 0;
        private int lastHeight = 0;
        // Persistent resources for NDI frame creation
        private int pboId = -1;
        private ByteBuffer ndiBuffer;
        private Method sendVideoFrameGLMethod;
        private boolean glTransferAvailable = false;
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
		this.ndiView = currentView;
		this.spoutView = currentView;
		this.syphonView = currentView;

		String osName = System.getProperty("os.name").toLowerCase();
		this.isMacOS = osName.contains("mac");
		this.isWindows = osName.contains("win");

		setupSyphonOrSpout(); // Initializes Syphon or Spout based on the OS
	}

	/**
	 * Initializes NDI output if it is not already enabled.
	 */
        private void initNDI() {
                if (!ndiEnabled && ndiSender == null) {
                        ndiSender = new DevolaySender("ziviDomeLive NDI Output");
                        reusableFrame = new DevolayVideoFrame(); // Initialize reusable frame
                        // Check for optional GL transfer support
                        try {
                                for (Method m : DevolaySender.class.getMethods()) {
                                        if (m.getName().equals("sendVideoFrameGL")) {
                                                sendVideoFrameGLMethod = m;
                                                glTransferAvailable = true;
                                                logger.info("sendVideoFrameGL available, using GPU transfer.");
                                                break;
                                        }
                                }
                        } catch (Exception e) {
                                logger.warning("Failed to resolve sendVideoFrameGL: " + e.getMessage());
                        }

                        ndiEnabled = true;
                        logger.info("NDI output initialized.");
                }
        }

	/**
	 * Sets up Syphon (for macOS) or Spout (for Windows) based on the OS.
	 * Initializes the corresponding output method if the platform is supported.
	 */
	private void setupSyphonOrSpout() {
		try {
			if (isMacOS && syphonServer == null) {
				syphonServer = new SyphonServer(parent.getPApplet(), "ziviDomeLive Syphon");
				syphonEnabled = true;
				logger.info("SyphonServer initialized for macOS.");
			} else if (isWindows && spoutSender == null) {
				spoutSender = new Spout(parent.getPApplet());
				spoutEnabled = true;
				logger.info("Spout initialized for Windows.");
			}
		} catch (Exception e) {
			logger.severe("Error setting up Syphon/Spout: " + e.getMessage());
		}
	}

	/**
	 * Toggles the specified output method (NDI, Spout, or Syphon) on or off.
	 *
	 * @param method the name of the output method to toggle ("ndi", "spout", "syphon")
	 */
	public void toggleOutput(String method) {
		switch (method.toLowerCase()) {
			case "ndi":
				if (!ndiEnabled) {
					initNDI();
				} else {
					shutdownNDI();
					ndiEnabled = false;
				}
				break;
			case "spout":
				spoutEnabled = !spoutEnabled;
				if (spoutEnabled) setupSyphonOrSpout();
				else shutdownSpout();
				break;
			case "syphon":
				syphonEnabled = !syphonEnabled;
				if (syphonEnabled) setupSyphonOrSpout();
				else shutdownSyphon();
				break;
		}
	}

	/**
	 * Sets the view type for the output.
	 *
	 * @param viewType the desired view type
	 */
	public void setView(zividomelive.ViewType viewType) {
		if (currentView != viewType) {
			currentView = viewType;
			prepareOutput(currentView);
			logger.info("Current view set to " + currentView);
		}
	}

	/**
	 * Prepares the output view based on the specified view type.
	 *
	 * @param viewType the view type to prepare
	 */
	private void prepareOutput(zividomelive.ViewType viewType) {
		switch (viewType) {
			case FISHEYE_DOMEMASTER:
				outputGraphics = parent.getFisheyeDomemaster().getDomemasterGraphics();
				break;
			case EQUIRECTANGULAR:
				outputGraphics = parent.getEquirectangularRenderer().getEquirectangular();
				break;
			case CUBEMAP:
				outputGraphics = parent.getCubemapViewRenderer().getCubemap();
				break;
			case STANDARD:
				outputGraphics = parent.getStandardRenderer().getStandardView();
				break;
		}
	}

	/**
	 * Sends the prepared output to the enabled output methods (NDI, Spout, or Syphon).
	 */
        public void sendOutput() {
                if (ndiEnabled && ndiSender != null) {
                        prepareOutput(ndiView);
                        DevolayVideoFrame ndiFrame = createNDIFrame(outputGraphics);

                        if (ndiFrame != null) {
                                ThreadManager.submitRunnable(() -> {
                                        synchronized (this) {
                                                if (ndiSender != null) {
                                                        ndiSender.sendVideoFrameAsync(ndiFrame);
                                                }
                                        }
                                });
                        }
                }

		if (spoutEnabled && spoutSender != null && isWindows) {
			prepareOutput(spoutView);
			spoutSender.sendTexture(outputGraphics);
		}

		if (syphonEnabled && syphonServer != null && isMacOS) {
			prepareOutput(syphonView);
			syphonServer.sendImage(outputGraphics);
		}
	}

	/**
	 * Creates an NDI video frame from the provided PGraphics in RGBA format, using the OpenGL texture directly.
	 *
	 * @param pg the PGraphics instance containing the image data
	 * @return the created NDI video frame
	 */
        private synchronized DevolayVideoFrame createNDIFrame(PGraphicsOpenGL pg) {
                int width = pg.width;
                int height = pg.height;

                // Get the OpenGL texture ID
                int textureID = pg.getTexture().glName;

                // If Devolay supports sending GL textures directly, use it
                if (glTransferAvailable && sendVideoFrameGLMethod != null) {
                        try {
                                sendVideoFrameGLMethod.invoke(ndiSender, textureID, width, height);
                                return null; // Frame was sent directly
                        } catch (Exception e) {
                                logger.warning("sendVideoFrameGL failed, falling back to CPU copy: " + e.getMessage());
                                glTransferAvailable = false;
                        }
                }

                // Lazy creation of persistent PBO
                PGL pgl = pg.beginPGL();
                if (pboId == -1) {
                        IntBuffer ids = IntBuffer.allocate(1);
                        pgl.genBuffers(1, ids);
                        pboId = ids.get(0);
                }

                int requiredSize = width * height * 4;

                if (ndiBuffer == null || ndiBuffer.capacity() != requiredSize || width != lastWidth || height != lastHeight) {
                        pgl.bindBuffer(PGL.PIXEL_PACK_BUFFER, pboId);
                        pgl.bufferData(PGL.PIXEL_PACK_BUFFER, requiredSize, null, PGL.STREAM_READ);
                        pgl.bindBuffer(PGL.PIXEL_PACK_BUFFER, 0);

                        ndiBuffer = ByteBuffer.allocateDirect(requiredSize);
                        ndiBuffer.order(ByteOrder.LITTLE_ENDIAN);
                        lastWidth = width;
                        lastHeight = height;
                }

                // Read pixels into PBO
                pgl.bindBuffer(PGL.PIXEL_PACK_BUFFER, pboId);
                pgl.bindTexture(PGL.TEXTURE_2D, textureID);
                pgl.readPixels(0, 0, width, height, PGL.RGBA, PGL.UNSIGNED_BYTE, 0);
                ByteBuffer mapped = pgl.mapBuffer(PGL.PIXEL_PACK_BUFFER, PGL.READ_ONLY);
                ndiBuffer.clear();
                ndiBuffer.put(mapped);
                ndiBuffer.flip();

                pgl.unmapBuffer(PGL.PIXEL_PACK_BUFFER);
                pgl.bindBuffer(PGL.PIXEL_PACK_BUFFER, 0);
                pg.endPGL();

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
		shutdownNDI();
		shutdownSpout();
		shutdownSyphon();
		ThreadManager.shutdown(); // Ensures executor is shut down
		logger.info("All output services and thread pool have been shut down.");
	}

	/**
	 * Shuts down NDI output, releasing resources.
	 */
	private synchronized void shutdownNDI() {
		if (ndiSender != null) {
			ndiSender.close();
			ndiSender = null;
			reusableFrame = null; // Clears reusable frame
			logger.info("NDI output shut down.");
		}
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
		this.ndiView = view;
		logger.info("NDI view set to " + view);
	}

	/**
	 * Sets the view type for Spout output.
	 *
	 * @param view the desired view type for Spout output
	 */
	public void setSpoutView(zividomelive.ViewType view) {
		this.spoutView = view;
		logger.info("Spout view set to " + view);
	}

	/**
	 * Sets the view type for Syphon output.
	 *
	 * @param view the desired view type for Syphon output
	 */
	public void setSyphonView(zividomelive.ViewType view) {
		this.syphonView = view;
		logger.info("Syphon view set to " + view);
	}

	/**
	 * Checks if any output method (NDI, Spout, or Syphon) is currently active.
	 *
	 * @return true if any output method is enabled, false otherwise
	 */
	public boolean isActive() {
		return ndiEnabled || spoutEnabled || syphonEnabled;
	}

	/**
	 * Stops all output methods and shuts down the OutputManager.
	 * This method ensures that all resources are released and
	 * the thread pool is properly shut down.
	 */
	public void stopOutput() {
		shutdownOutputs();
	}
}
