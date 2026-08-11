package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.manager.*;
import com.victorvalentim.zividomelive.render.*;
import com.victorvalentim.zividomelive.render.camera.*;
import com.victorvalentim.zividomelive.render.modes.*;
import com.victorvalentim.zividomelive.support.*;
import processing.core.*;
import processing.event.*;
import processing.opengl.*;
import controlP5.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The `ziviDomeLive` class manages rendering and control of a live dome visualization.
 * It integrates with Processing and `OutputManager` for dome rendering.
 *
 * <p>This class handles setup, initialization, and rendering of fisheye domemaster,
 * equirectangular, cubemap, and standard views. It also manages the control panel and mouse events for interaction.</p>
 *
 * <p>It provides methods to set up the rendering environment, initialize various managers,
 * handle mouse and key events, and render different views. The class also supports toggling
 * output methods (NDI, Spout, Syphon) and managing the current scene and view type.</p>
 */
public class ziviDomeLive implements PConstants {

	/**
	 * Enum representing the initialization state of the library.
	 */
	public enum InitState {
		/** Instance created but setup has not started yet. */
		NOT_INITIALIZED,
		/** setup() completed, waiting for managers initialization in post(). */
		SETUP_COMPLETE,
		/** Managers and renderers are ready for rendering. */
		MANAGERS_READY,
		/** Reserved fully-ready state for future lifecycle expansion. */
		READY
	}

	private final PApplet p;
	private InitState initState = InitState.NOT_INITIALIZED;
	private boolean paused;
	private boolean disposed;
	private final Set<String> registeredEventHandlers = new LinkedHashSet<>();

	private final SphericalOrientation sphericalOrientation = new SphericalOrientation();
	private float fov = 210.0f, fishSize = 100.0f;
	// Output resolution is dedicated to offscreen render targets used by external outputs.
	private int outputResolution = 1024;
	private boolean showControlPanel = true;
	private boolean showPreview = false;
	private boolean controlPanelShownOnce = false;
	private int targetFrameRate = 60;

	// Shader resource paths (packaged under data/shaders by the build).
	private static final String EQUIRECT_VERT = "data/shaders/equirectangular.vert";
	private static final String EQUIRECT_FRAG = "data/shaders/equirectangular.frag";
	private static final String DOME_VERT = "data/shaders/domemaster.vert";
	private static final String DOME_FRAG = "data/shaders/domemaster.frag";

	private ControlManager controlManager;
	// Output pipeline (high resolution)
	private CubemapRenderer cubemapRenderer;
	private EquirectangularRenderer equirectangularRenderer;
	private FisheyeDomemaster fisheyeDomemaster;
	private CubemapViewRenderer cubemapViewRenderer;
	// Preview pipeline (window-sized)
	private CubemapRenderer previewCubemapRenderer;
	private EquirectangularRenderer previewEquirectangularRenderer;
	private FisheyeDomemaster previewFisheyeDomemaster;
	private CubemapViewRenderer previewCubemapViewRenderer;
	/**
	 * Preview Standard renderer. Uses dynamic {@code parent.width × parent.height} so that
	 * the window aspect ratio is preserved and resize is handled automatically.
	 * This instance is independent from the output {@code standardRenderer}.
	 */
	private StandardRenderer standardRendererPreview;
	private int previewResolution = 1024;
	private StandardRenderer standardRenderer;
	private CameraManager cameraManager;
	// Native scene-space orbit camera service (see OrbitCamera).
	private final OrbitCamera sceneCamera = new OrbitCamera();
	private boolean sceneCameraInputEnabled = false;
	private OutputManager outputManager;
	private boolean resumeNdiOutput;
	private boolean resumeSpoutOutput;
	private boolean resumeSyphonOutput;
	private SplashScreen splash;
	private SceneManager sceneManager;
	private Scene fallbackScene;

	/**
	 * Enum representing the different types of views available.
	 */
	public enum ViewType {
		/** Fisheye domemaster view. */
		FISHEYE_DOMEMASTER,
		/** Equirectangular view. */
		EQUIRECTANGULAR,
		/** Cubemap view. */
		CUBEMAP,
		/** Standard view. */
		STANDARD
	}

	/**
	 * Aspect policy used to compute Standard output dimensions.
	 */
	public enum StandardOutputAspectMode {
		/** Snap to the closest supported family based on the current logical window ratio. */
		AUTO,
		/** Force 16:9 output dimensions. */
		ASPECT_16_9,
		/** Force 16:10 output dimensions. */
		ASPECT_16_10,
		/** Force 4:3 output dimensions. */
		ASPECT_4_3,
		/** Force 1:1 output dimensions. */
		ASPECT_1_1
	}

	private ViewType currentView = ViewType.FISHEYE_DOMEMASTER;
	private RenderMode renderMode = RenderMode.FULL;
	private StandardOutputAspectMode standardOutputAspectMode = StandardOutputAspectMode.AUTO;

	private boolean pendingOutputReset = false;
	private int pendingOutputResolution = outputResolution;
	private static final Logger LOGGER = LogManager.getLogger();


	/**
	 * Constructs a new `ziviDomeLive` instance with the specified PApplet.
	 *
	 * @param p the PApplet instance used for rendering
	 * @throws IllegalArgumentException if the PApplet instance is null
	 */
	public ziviDomeLive(PApplet p) {
		if (p == null) {
			throw new IllegalArgumentException("PApplet instance cannot be null.");
		}
		this.p = p;
		this.sceneManager = new SceneManager();

		welcome();
		registerEventHandlers();
	}

	/**
	 * Sets the global logging mode used by the library.
	 * Call this before creating a ziviDomeLive instance.
	 *
	 * @param mode desired logging mode
	 */
	public static void setLogMode(LogManager.Mode mode) {
		LogManager.setMode(mode);
	}

	/** Enables verbose DEBUG logs (console + file). */
	public static void enableDebugLogging() {
		LogManager.setMode(LogManager.Mode.DEBUG);
	}

	/** Enables RELEASE logging mode (LogManager output disabled). */
	public static void enableReleaseLogging() {
		LogManager.setMode(LogManager.Mode.RELEASE);
	}

	/**
	 * Returns the currently active logging mode.
	 *
	 * @return active logging mode
	 */
	public static LogManager.Mode getLogMode() {
		return LogManager.getMode();
	}

	/**
	 * Prints a welcome message indicating that the library has been initialized.
	 */
	private void welcome() {
		String libraryName = LibraryMetadata.get("name");
		String libraryVersion = LibraryMetadata.get("prettyVersion");
		String authors = LibraryMetadata.get("authors");
		System.out.printf("[%s] %s by %s%n", libraryName, libraryVersion, authors);
	}

	/**
	 * Sets the current scene to be rendered.
	 *
	 * <p>Both the output and preview Standard renderers are updated so that both pipelines
	 * render the new scene content.</p>
	 *
	 * @param scene the Scene instance to be set
	 */
	public void setScene(Scene scene) {
		if (disposed) {
			LOGGER.warning("Cannot set a scene after disposal.");
			return;
		}
		if (scene == null) {
			LOGGER.warning("Cannot set a null scene.");
			return;
		}
		if (fallbackScene != null
				&& scene != fallbackScene
				&& sceneManager.containsScene(fallbackScene)) {
			sceneManager.clearScenes();
			fallbackScene = null;
		}
		sceneManager.activateScene(scene);
		syncCurrentSceneToRenderers();
	}

	/**
	 * Sets up the rendering environment, including frame rate, OpenGL info, texture hints,
	 * output configuration, and mouse event registration.
	 *
	 * @throws IllegalStateException if the PApplet instance is not properly configured
	 */
	public void setup() {
		if (disposed) {
			LOGGER.warning("Setup ignored: this ziviDomeLive instance has been disposed.");
			return;
		}
		if (initState != InitState.NOT_INITIALIZED) {
			LOGGER.info("Setup already completed; duplicate call ignored.");
			return;
		}
		if (p == null) {
			throw new IllegalStateException("PApplet instance is not properly configured.");
		}

		LOGGER.info("Starting setup...");

		try {
			p.frameRate(targetFrameRate);
			LOGGER.info("Frame rate set to " + targetFrameRate + ".");
		} catch (Exception e) {
			LOGGER.severe("Error setting frame rate: " + e.getMessage());
		}
		try {
			printOpenGLInfo(p);
		} catch (Exception e) {
			LOGGER.severe("Error printing OpenGL info: " + e.getMessage());
		}

		try {
			setupHints();
			LOGGER.info("Texture hints configured.");
		} catch (Exception e) {
			LOGGER.severe("Error configuring texture hints: " + e.getMessage());
		}

		try {
			outputManager = new OutputManager(this);
			LOGGER.info("OutputManager initialized without active outputs.");
		} catch (Exception e) {
			LOGGER.severe("Error initializing OutputManager: " + e.getMessage());
		}

		try {
			splash = new SplashScreen(p);
			splash.start();
			LOGGER.info("SplashScreen initialized and started successfully.");
		} catch (Exception e) {
			LOGGER.severe("Error initializing or starting SplashScreen: " + e.getMessage());
		}

		// Load DefaultScene through SceneManager when no user scene was selected before setup.
		if (getCurrentScene() == null) {
			try {
				fallbackScene = new com.victorvalentim.zividomelive.scene.DefaultScene(p);
				sceneManager.registerScene(fallbackScene);
				LOGGER.info("DefaultScene loaded successfully as the initial scene.");
			} catch (Exception e) {
				LOGGER.severe("Error initializing DefaultScene: " + e.getMessage());
			}
		}

		initState = InitState.SETUP_COMPLETE;
		LOGGER.info("Setup completed.");
	}



	/**
	 * Prints OpenGL information including version, vendor, and renderer.
	 *
	 * @param p the PApplet instance used for rendering
	 */
	public void printOpenGLInfo(PApplet p) {
		if (p.g instanceof PGraphicsOpenGL pgl) {
			PGL pglContext = pgl.beginPGL();

			LOGGER.info("OpenGL Version: " + pglContext.getString(PGL.VERSION));
			LOGGER.info("OpenGL Vendor: " + pglContext.getString(PGL.VENDOR));
			LOGGER.info("OpenGL Renderer: " + pglContext.getString(PGL.RENDERER));

			pgl.endPGL();
		} else {
			LOGGER.severe("The current renderer is not OpenGL.");
		}
	}

	/**
	 * Configures texture hints for the rendering environment.
	 */
	void setupHints() {
		p.textureMode(NORMAL);
		p.textureWrap(REPEAT);
		p.hint(ENABLE_TEXTURE_MIPMAPS);
		p.hint(ENABLE_DEPTH_TEST);
		p.hint(ENABLE_OPTIMIZED_STROKE);
	}

	/**
	 * Initializes the application managers after Processing setup has completed.
	 *
	 * <p>Renderer resources and the platform-local texture-sharing backend are prepared on the
	 * Processing thread, after the OpenGL context is active. Preparing Syphon or Spout does not
	 * enable frame publication; the corresponding UI toggle controls only the enabled state.</p>
	 */
	public void initializeManagers() {
		if (disposed) {
			LOGGER.warning("Manager initialization ignored after disposal.");
			return;
		}
		if (paused) {
			LOGGER.info("Manager initialization deferred while paused.");
			return;
		}
		if (initState != InitState.SETUP_COMPLETE) {
			LOGGER.severe(
					"Cannot initialize managers: Setup not complete. Current state: "
							+ initState
			);
			return;
		}

		try {
			LOGGER.info("Initializing managers...");

			cameraManager = new CameraManager();
			LOGGER.info("CameraManager initialized.");

			/*
			 * Renderer targets must exist before Syphon or Spout is prepared.
			 * This method is expected to execute on the Processing/OpenGL thread.
			 */
			initializeRenderers();
			LOGGER.info("Renderers initialized.");

			/*
			 * Prepare the single valid local texture backend for this platform:
			 *
			 * - macOS: create and warm up Syphon;
			 * - Windows: create the Spout sender;
			 * - other systems: no operation.
			 *
			 * This call does not enable frame publication. It only absorbs native
			 * initialization during library startup so the later UI toggle is immediate.
			 */
			if (outputManager != null) {
				outputManager.initializeLocalTextureOutput();

				if (outputManager.isLocalTextureInitialized()) {
					LOGGER.info(
							"Local texture output backend prepared: "
									+ outputManager.getLocalTextureBackendName()
									+ "."
					);
				} else if (outputManager.isLocalTextureAvailable()) {
					LOGGER.warning(
							"Local texture output backend was not prepared during startup: "
									+ outputManager.getLocalTextureBackendName()
									+ "."
					);
				} else {
					LOGGER.info("No platform-local Syphon/Spout backend is available.");
				}
			}

			controlManager = new ControlManager(
					p,
					this,
					outputResolution
			);
			LOGGER.info("ControlManager initialized.");

			initState = InitState.MANAGERS_READY;
			LOGGER.info("Managers initialized successfully.");

		} catch (Exception | LinkageError error) {
			LOGGER.severe(
					"Error initializing managers: "
						+ error.getClass().getSimpleName()
						+ ": "
						+ error.getMessage()
			);

			try {
				rollbackManagerInitialization();
			} catch (Exception | LinkageError cleanupError) {
				LOGGER.warning("Manager initialization rollback failed: " + cleanupError.getMessage());
			} finally {
				initState = InitState.SETUP_COMPLETE;
			}
		}
	}

	private void rollbackManagerInitialization() {
		if (controlManager != null) {
			controlManager.dispose();
			controlManager = null;
		}
		if (cameraManager != null) {
			cameraManager.dispose();
			cameraManager = null;
		}
		releaseGraphicsResources();
		if (outputManager != null) {
			outputManager.shutdownOutputs();
		}
	}

	/**
	 * Initialises all render targets for both the preview and output pipelines.
	 *
	 * <p>Output FBOs are allocated at {@code outputResolution} even when no external output
	 * is active; this avoids a first-frame stall when the user enables Syphon or Spout.
	 * Preview FBOs are allocated at the resolution derived from the current window size via
	 * {@link #computePreviewResolution()}.</p>
	 *
	 * <p>Must be called from the Processing/OpenGL thread.</p>
	 */
	void initializeRenderers() {
		LOGGER.info("Initializing renderers...");

		initializeOutputRenderers();
		LOGGER.info("Output renderers initialized.");

		initializePreviewRenderers();
		LOGGER.info("Preview renderers initialized.");

		LOGGER.info("Renderers initialized successfully.");
	}

	/**
	 * Allocates the high-resolution output render targets if they are not yet available.
	 *
	 * <p>All output FBOs use {@code outputResolution} as their base high-resolution budget.
	 * Cubemap, equirectangular, and fisheye targets remain square or aspect-fixed as before.
	 * Standard output is aspect-aware: it uses the logical window aspect ratio while keeping
	 * the longest edge aligned to the selected {@code outputResolution} bucket (1k/2k/3k/4k).
	 * Passing positive dimensions pre-allocates the FBO immediately so that Syphon or Spout
	 * can obtain a valid texture reference during backend initialisation.</p>
	 *
	 * <p>This method is idempotent: it returns immediately when all targets already exist.
	 * Must be called from the Processing draw thread.</p>
	 */
	private void initializeOutputRenderers() {
		if (cubemapRenderer != null && equirectangularRenderer != null
				&& fisheyeDomemaster != null && cubemapViewRenderer != null
				&& standardRenderer != null) {
			return;
		}
		cubemapRenderer = new CubemapRenderer(outputResolution, p);
		LOGGER.info("CubemapRenderer (output) initialized at " + outputResolution + "px.");
		equirectangularRenderer = new EquirectangularRenderer(outputResolution, EQUIRECT_FRAG, EQUIRECT_VERT, p);
		LOGGER.info("EquirectangularRenderer (output) initialized.");
		fisheyeDomemaster = new FisheyeDomemaster(outputResolution, DOME_FRAG, DOME_VERT, p);
		fisheyeDomemaster.setSizePercentage(fishSize);
		LOGGER.info("FisheyeDomemaster (output) initialized.");
		cubemapViewRenderer = new CubemapViewRenderer(p, outputResolution);
		LOGGER.info("CubemapViewRenderer (output) initialized.");
		int[] standardOutputDimensions = computeStandardOutputDimensions();
		standardRenderer = new StandardRenderer(
				p,
				standardOutputDimensions[0],
				standardOutputDimensions[1],
				getCurrentScene()
		);
		LOGGER.info("StandardRenderer (output) initialized at "
				+ standardOutputDimensions[0] + "×" + standardOutputDimensions[1] + "px.");
		// Refresh OutputManager cache so Syphon/Spout have valid PGraphics references.
		if (outputManager != null) {
			outputManager.refreshCachedGraphics();
		}
	}

	private int computePreviewResolution() {
		// Keep preview constrained by window size and capped for stable FPS.
		int minDim = Math.max(256, Math.min(p.width, p.height));
		return Math.min(1024, minDim);
	}

	private static final int BUCKET_1K = 1024;
	private static final int BUCKET_2K = 2048;
	private static final int BUCKET_3K = 3072;
	private static final int BUCKET_4K = 4096;

	/**
	 * Computes Standard-output dimensions using explicit 1k/2k/3k/4k presets aligned to the
	 * current logical window aspect ratio.
	 *
	 * <p>To avoid arbitrary non-standard sizes, this method snaps the window aspect ratio to one
	 * of the common buckets ({@code 16:9}, {@code 16:10}, {@code 4:3}, {@code 1:1}), then selects
	 * a pre-defined output pair for the configured resolution bucket ({@code 1024/2048/3072/4096}).
	 * If the selected resolution is outside those buckets, it falls back to proportional scaling
	 * of the closest aspect bucket while preserving orientation.</p>
	 *
	 * @return a two-element array containing {@code [width, height]}
	 */
	private int[] computeStandardOutputDimensions() {
		int resolutionBucket = outputResolution;
		float aspect = (p.width > 0 && p.height > 0)
				? (p.width / (float) p.height)
				: 1.0f;

		boolean landscape = aspect >= 1.0f;
		float normalizedAspect = landscape ? aspect : (1.0f / Math.max(0.0001f, aspect));

		// Snap to a common aspect family for stable, predictable output sizes.
		float a169 = 16.0f / 9.0f;
		float a1610 = 16.0f / 10.0f;
		float a43 = 4.0f / 3.0f;
		float a11 = 1.0f;

		float d169 = Math.abs(normalizedAspect - a169);
		float d1610 = Math.abs(normalizedAspect - a1610);
		float d43 = Math.abs(normalizedAspect - a43);
		float d11 = Math.abs(normalizedAspect - a11);

		int aspectFamily;
		switch (standardOutputAspectMode) {
			case ASPECT_16_9:
				aspectFamily = 169;
				break;
			case ASPECT_16_10:
				aspectFamily = 1610;
				break;
			case ASPECT_4_3:
				aspectFamily = 43;
				break;
			case ASPECT_1_1:
				aspectFamily = 11;
				break;
			case AUTO:
			default:
				if (d169 <= d1610 && d169 <= d43 && d169 <= d11) {
					aspectFamily = 169;
				} else if (d1610 <= d43 && d1610 <= d11) {
					aspectFamily = 1610;
				} else if (d43 <= d11) {
					aspectFamily = 43;
				} else {
					aspectFamily = 11;
				}
				break;
		}

		int width;
		int height;
		switch (resolutionBucket) {
			case BUCKET_1K:
				int[] dims1k = dimensionsForFamily(aspectFamily, BUCKET_1K);
				width = dims1k[0];
				height = dims1k[1];
				break;
			case BUCKET_2K:
				int[] dims2k = dimensionsForFamily(aspectFamily, BUCKET_2K);
				width = dims2k[0];
				height = dims2k[1];
				break;
			case BUCKET_3K:
				int[] dims3k = dimensionsForFamily(aspectFamily, BUCKET_3K);
				width = dims3k[0];
				height = dims3k[1];
				break;
			case BUCKET_4K:
				int[] dims4k = dimensionsForFamily(aspectFamily, BUCKET_4K);
				width = dims4k[0];
				height = dims4k[1];
				break;
			default:
				// Fallback: scale from the 1k preset for the snapped aspect family.
				int[] base = dimensionsForFamily(aspectFamily, BUCKET_1K);
				float scale = Math.max(1, resolutionBucket) / (float) BUCKET_1K;
				width = Math.max(1, Math.round(base[0] * scale));
				height = Math.max(1, Math.round(base[1] * scale));
				break;
		}

		if (!landscape) {
			int tmp = width;
			width = height;
			height = tmp;
		}

		return new int[]{width, height};
	}

	/**
	 * Returns explicit Standard-output dimensions for a given aspect family and bucket.
	 */
	private int[] dimensionsForFamily(int family, int bucket) {
		switch (family) {
			case 169:
				switch (bucket) {
					case BUCKET_1K: return new int[]{1024, 576};
					case BUCKET_2K: return new int[]{2048, 1152};
					case BUCKET_3K: return new int[]{3072, 1728};
					default: return new int[]{4096, 2304};
				}
			case 1610:
				switch (bucket) {
					case BUCKET_1K: return new int[]{1024, 640};
					case BUCKET_2K: return new int[]{2048, 1280};
					case BUCKET_3K: return new int[]{3072, 1920};
					default: return new int[]{4096, 2560};
				}
			case 43:
				switch (bucket) {
					case BUCKET_1K: return new int[]{1024, 768};
					case BUCKET_2K: return new int[]{2048, 1536};
					case BUCKET_3K: return new int[]{3072, 2304};
					default: return new int[]{4096, 3072};
				}
			default:
				switch (bucket) {
					case BUCKET_1K: return new int[]{1024, 1024};
					case BUCKET_2K: return new int[]{2048, 2048};
					case BUCKET_3K: return new int[]{3072, 3072};
					default: return new int[]{4096, 4096};
				}
		}
	}

	/**
	 * Sets the aspect policy used to compute Standard output dimensions.
	 *
	 * <p>Changing this setting schedules an output-render-target rebuild on the next frame so the
	 * Standard output FBO dimensions are updated without recreating preview targets.</p>
	 *
	 * @param mode desired aspect mode, ignored when {@code null}
	 */
	public void setStandardOutputAspectMode(StandardOutputAspectMode mode) {
		if (mode == null || mode == this.standardOutputAspectMode) {
			return;
		}
		this.standardOutputAspectMode = mode;
		pendingOutputReset = true;
		pendingOutputResolution = outputResolution;
		LOGGER.info("Standard output aspect mode set to: " + mode);
	}

	/**
	 * Returns the aspect policy currently used to compute Standard output dimensions.
	 *
	 * @return current Standard output aspect mode
	 */
	public StandardOutputAspectMode getStandardOutputAspectMode() {
		return standardOutputAspectMode;
	}

	/**
	 * Allocates the window-resolution preview render targets.
	 *
	 * <p>Fisheye, equirectangular, and cubemap preview FBOs use a square resolution derived
	 * from the window's smaller dimension via {@link #computePreviewResolution()}. The Standard
	 * preview renderer uses {@code p.width × p.height} (dynamic, window aspect ratio preserved)
	 * by passing {@code 0, 0} to the constructor.</p>
	 *
	 * <p>The Standard preview renderer shares the same
	 * {@link com.victorvalentim.zividomelive.render.camera.MouseControlledCamera} as the output
	 * Standard renderer so that both always show the same framing.</p>
	 *
	 * <p>Must be called from the Processing draw thread.</p>
	 */
	private void initializePreviewRenderers() {
		previewResolution = computePreviewResolution();

		previewCubemapRenderer = new CubemapRenderer(previewResolution, p);
		previewEquirectangularRenderer = new EquirectangularRenderer(previewResolution, EQUIRECT_FRAG, EQUIRECT_VERT, p);
		previewFisheyeDomemaster = new FisheyeDomemaster(previewResolution, DOME_FRAG, DOME_VERT, p);
		previewFisheyeDomemaster.setSizePercentage(fishSize);
		previewCubemapViewRenderer = new CubemapViewRenderer(p, previewResolution);

		// Dynamic dimensions (0, 0) → renderer uses parent.width/parent.height each frame,
		// preserving the window aspect ratio and handling window resize automatically.
		standardRendererPreview = new StandardRenderer(p, 0, 0, getCurrentScene());

		// Share camera so preview and output Standard views are always framing the same scene.
		if (standardRenderer != null) {
			standardRendererPreview.setCam(standardRenderer.getCam());
		}
	}

	/**
	 * Recreates preview render targets when the computed preview resolution has changed.
	 *
	 * <p>Called at the beginning of each draw cycle. The Standard preview renderer is included
	 * in the check because it belongs to the preview pipeline.</p>
	 */
	private void ensurePreviewRenderers() {
		int expected = computePreviewResolution();
		if (previewCubemapRenderer == null
				|| previewEquirectangularRenderer == null
				|| previewFisheyeDomemaster == null
				|| previewCubemapViewRenderer == null
				|| standardRendererPreview == null
				|| previewResolution != expected) {
			releasePreviewGraphicsResources();
			initializePreviewRenderers();
		}
	}

	private void capturePreviewCubemap() {
		if (previewCubemapRenderer != null) {
			previewCubemapRenderer.captureCubemap(
					sphericalOrientation.getQuaternion(), cameraManager, getCurrentScene());
		}
	}

	/**
	 * Computes the window-preview requirements for the current frame.
	 *
	 * <p>{@code showPreview=true} forces the fisheye chain so the floating thumbnail can be
	 * composed even when the main preview view is Standard.</p>
	 *
	 * @return cached requirements for the current preview state
	 */
	private RenderRequirementsPolicy.Requirements computePreviewRequirements() {
		return RenderRequirementsPolicy.forPreview(renderMode, getCurrentView(), showPreview);
	}

	/**
	 * Computes the external-output requirements for the current frame.
	 *
	 * @return cached requirements for all enabled external-output routes
	 */
	private RenderRequirementsPolicy.Requirements computeOutputRequirements() {
		boolean outputsActive = outputManager != null && outputManager.isActive();
		return RenderRequirementsPolicy.forOutputs(
				outputsActive,
				outputsActive && outputManager.requiresView(ViewType.FISHEYE_DOMEMASTER),
				outputsActive && outputManager.requiresView(ViewType.EQUIRECTANGULAR),
				outputsActive && outputManager.requiresView(ViewType.CUBEMAP),
				outputsActive && outputManager.requiresView(ViewType.STANDARD)
		);
	}

	/**
	 * Captures at most one cubemap for the current frame.
	 *
	 * <p>If an active output requires cubemap data, the output-resolution cubemap becomes the
	 * master source for both output and preview projections. Otherwise, the preview-resolution
	 * cubemap is captured only when the window preview or floating fisheye thumbnail needs it.</p>
	 *
	 * @param preview preview requirements for the current frame
	 * @param output output requirements for the current frame
	 * @return master cubemap faces, or {@code null} when no cubemap is required
	 */
	private PGraphicsOpenGL[] captureMasterCubemap(
			RenderRequirementsPolicy.Requirements preview,
			RenderRequirementsPolicy.Requirements output) {
		if (output.needsCubemapSource()) {
			captureCubemap();
			return cubemapRenderer != null ? cubemapRenderer.getCubemapFaces() : null;
		}

		if (preview.needsCubemapSource()) {
			capturePreviewCubemap();
			return previewCubemapRenderer != null ? previewCubemapRenderer.getCubemapFaces() : null;
		}

		return null;
	}

	/**
	 * Renders only the passes required by the application preview window.
	 *
	 * <p>All FBOs used here belong to the preview pipeline (window resolution) and are
	 * independent from the high-resolution output FBOs. This method is always called, even
	 * when external outputs are active, ensuring the window always shows preview content.</p>
	 *
	 * <p>Pass counts per frame (0 or 1 of each):</p>
	 * <ul>
	 *   <li>Cubemap capture: skipped when the current view is {@link ViewType#STANDARD} and
	 *       {@code showPreview} is {@code false}.</li>
	 *   <li>Fisheye: rendered whenever the current view is fisheye <em>or</em>
	 *       {@code showPreview} is {@code true} (the floating thumbnail always shows fisheye
	 *       regardless of the main view).</li>
	 * </ul>
	 *
	 * <p>Must be called from the Processing draw thread after {@link #ensurePreviewRenderers()}.</p>
	 */
	private void renderPreviewPipeline(
			RenderRequirementsPolicy.Requirements preview,
			RenderRequirementsPolicy.Requirements output,
			PGraphicsOpenGL[] masterFaces) {
		if (preview.needsStandard()) {
			standardRendererPreview.render();
		}

		if (preview.needsEquirectangular()) {
			if (output.needsEquirectangular() && output.needsCubemapSource()) {
				copyToPreview(equirectangularRenderer.getEquirectangular(), previewEquirectangularRenderer.getEquirectangular());
			} else {
				previewEquirectangularRenderer.render(masterFaces);
			}
		}

		if (preview.needsFisheye()) {
			if (output.needsFisheye()) {
				copyToPreview(fisheyeDomemaster.getDomemasterGraphics(), previewFisheyeDomemaster.getDomemasterGraphics());
			} else {
				previewFisheyeDomemaster.applyShader(previewEquirectangularRenderer.getEquirectangular(), getFov());
			}
		}

		if (preview.needsCubemapLayout()) {
			if (output.needsCubemapLayout() && output.needsCubemapSource()) {
				copyToPreview(cubemapViewRenderer.getCubemap(), previewCubemapViewRenderer.getCubemap());
			} else {
				previewCubemapViewRenderer.drawCubemapToGraphics(masterFaces);
			}
		}
	}

	/**
	 * Renders only the high-resolution passes required by enabled external outputs.
	 *
	 * <p>All FBOs produced here belong to the output pipeline ({@code outputResolution})
	 * and remain offscreen. They are never composited onto the Processing window. After this
	 * method returns, all relevant {@code endDraw()} calls have completed and the FBOs are
	 * ready for
	 * {@link com.victorvalentim.zividomelive.manager.OutputManager#sendOutput()}.</p>
	 *
	 * <p>The set of passes is derived from
	 * {@link com.victorvalentim.zividomelive.manager.OutputManager#requiresView(ViewType)}:
	 * only the minimal dependency chain is executed. A single cubemap capture supplies all
	 * output passes that need it.</p>
	 *
	 * <p>Example: NDI requests equirectangular, Syphon requests fisheye →
	 * one cubemap capture → one equirectangular pass → one fisheye pass.</p>
	 *
	 * <p>Returns immediately when {@code outputManager} is {@code null} or inactive.
	 * Must be called from the Processing draw thread.</p>
	 */
	private void renderOutputPipeline(
			RenderRequirementsPolicy.Requirements output,
			PGraphicsOpenGL[] masterFaces) {
		if (output.needsCubemapSource() && masterFaces == null) {
			return;
		}

		if (output.needsEquirectangular()) {
			equirectangularRenderer.render(masterFaces);
		}

		if (output.needsFisheye()) {
			fisheyeDomemaster.applyShader(
					equirectangularRenderer.getEquirectangular(), getFov());
		}

		if (output.needsCubemapLayout()) {
			cubemapViewRenderer.drawCubemapToGraphics(masterFaces);
		}

		if (output.needsStandard()) {
			standardRenderer.render();
		}
	}

	/**
	 * Composites the current preview FBO onto the Processing window.
	 *
	 * <p>Only preview-resolution FBOs are passed to
	 * {@link processing.core.PApplet#image(processing.core.PImage, float, float, float, float)}
	 * here. Output FBOs are never drawn onto the main window by this method.</p>
	 */
	private void displayPreviewCurrentView() {
		ViewType effectiveView = RenderRequirementsPolicy.resolveView(renderMode, getCurrentView());
		switch (effectiveView) {
			case CUBEMAP:
				displayView(previewCubemapViewRenderer.getCubemap());
				break;
			case EQUIRECTANGULAR:
				displayView(previewEquirectangularRenderer.getEquirectangular());
				break;
			case FISHEYE_DOMEMASTER:
				displayView(previewFisheyeDomemaster.getDomemasterGraphics());
				break;
			case STANDARD:
				displayView(standardRendererPreview.getStandardView());
				break;
		}
	}

	/**
	 * Main draw method that handles rendering and updating the view.
	 */
	public void draw() {
		if (disposed || paused || initState != InitState.MANAGERS_READY) {
			p.background(0, 0);
			return;
		}

		// Renderiza o conteúdo principal em segundo plano
		renderContent();

		// Atualiza e renderiza a splash screen enquanto ativa
		if (splash != null && splash.showSplash) {
			clearBackground(); // Limpa o fundo antes de renderizar a splash
			splash.update();
			splash.render();
			showControlPanel = false; // Oculta o painel enquanto a splash está ativa
			controlPanelShownOnce = false; // Reseta a flag durante a splash
		} else if (splash != null) {
			releaseSplash();

			// Exibe o painel de controle apenas uma vez
			if (!controlPanelShownOnce) {
				showControlPanel = true;
				controlPanelShownOnce = true; // Define a flag para evitar reaparecimento
			}
		}
	}

	/**
	 * Executes the independent preview and external-output rendering pipelines for one frame.
	 *
	 * <p>The Processing window always displays preview-resolution FBOs. High-resolution output
	 * FBOs remain offscreen and are submitted only to enabled backends after all relevant
	 * {@code endDraw()} calls have completed.</p>
	 *
	 * <p>Frame order:</p>
	 * <ol>
	 *   <li>Clear the window background.</li>
	 *   <li>Apply any pending output-resolution change (output FBOs only, preview unaffected).</li>
	 *   <li>Ensure preview FBOs are valid for the current window size.</li>
	 *   <li>Resolve preview and output requirements, then capture at most one master cubemap.</li>
	 *   <li>When at least one output is active, run its minimal projection passes and submit
	 *       completed targets to the enabled backends.</li>
	 *   <li>Run the preview passes, reusing completed output projections when available.</li>
	 *   <li>Composite the preview FBO onto the window.</li>
	 *   <li>Optionally draw the floating fisheye thumbnail (preview FBO only).</li>
	 *   <li>Draw the control panel.</li>
	 * </ol>
	 */
	void renderContent() {
		if (standardRendererPreview == null || standardRenderer == null || getCurrentScene() == null) {
			LOGGER.severe("Cannot render content: renderer or scene not initialized.");
			return;
		}

		clearBackground();
		handleGraphicsReset();
		ensurePreviewRenderers();
		syncCurrentSceneToRenderers();

		RenderRequirementsPolicy.Requirements preview = computePreviewRequirements();
		RenderRequirementsPolicy.Requirements output = computeOutputRequirements();
		PGraphicsOpenGL[] masterFaces = captureMasterCubemap(preview, output);

		if (outputManager != null && outputManager.isActive()) {
			renderOutputPipeline(output, masterFaces);
			outputManager.sendOutput();
		}

		renderPreviewPipeline(preview, output, masterFaces);

		// Only preview FBOs are composited onto the main window.
		displayPreviewCurrentView();

		if (showPreview) {
			drawFloatingPreview();
		}
		drawControlPanel();
	}

	void clearBackground() {
		p.background(0, 0, 0, 0);
	}

	/**
	 * Applies a pending output-resolution change by recreating only the output render targets.
	 *
	 * <p>Preview FBOs are not affected. Syphon is not recreated; Spout will resize its sender
	 * on the next published frame via
	 * {@link com.victorvalentim.zividomelive.manager.OutputManager#notifyResolutionChanged(int)}.</p>
	 */
	private void handleGraphicsReset() {
		if (pendingOutputReset) {
			LOGGER.info("Applying output resolution change: " + pendingOutputResolution + "px.");
			releaseOutputGraphicsResources();
			outputResolution = pendingOutputResolution;
			initializeOutputRenderers();
			// Restore camera sharing after the new output StandardRenderer is created.
			if (standardRendererPreview != null && standardRenderer != null) {
				standardRendererPreview.setCam(standardRenderer.getCam());
			}
			if (outputManager != null) {
				outputManager.notifyResolutionChanged(outputResolution);
			}
			pendingOutputReset = false;
			LOGGER.info("Output graphics reset completed at " + outputResolution + "px.");
		}
	}

	/**
	 * Captures the cubemap for the current scene.
	 */
	private void captureCubemap() {
		if (cubemapRenderer != null) {
			cubemapRenderer.captureCubemap(
					sphericalOrientation.getQuaternion(), cameraManager, getCurrentScene());
		} else {
			LOGGER.severe("Error: CubemapRenderer not initialized.");
		}
	}

	/**
	 * Displays the given PGraphics object on the screen at a proportionate size.
	 * Reduces the rendering scale based on screen size to improve FPS.
	 *
	 * @param pg the PGraphics object to be displayed
	 */
	private void displayView(PGraphicsOpenGL pg) {
		float aspectRatio = pg.width / (float) pg.height;
		float displayWidth = p.width;
		float displayHeight = displayWidth / aspectRatio;

		if (displayHeight > p.height) {
			displayHeight = p.height;
			displayWidth = displayHeight * aspectRatio;
		}

		p.image(pg, (p.width - displayWidth) / 2, (p.height - displayHeight) / 2, displayWidth, displayHeight);
	}

	/**
	 * Sets the current view to {@link ViewType#FISHEYE_DOMEMASTER}.
	 *
	 * @deprecated The library's internal draw loop ({@code draw()} → {@code renderContent()})
	 *             renders every frame automatically. Call {@link #setCurrentView(ViewType)}
	 *             directly and let the pipeline handle the rest. This method is retained for
	 *             source compatibility only.
	 */
	@Deprecated
	public void renderFisheyeDomemaster() {
		setCurrentView(ViewType.FISHEYE_DOMEMASTER);
	}

	/**
	 * Sets the current view to {@link ViewType#EQUIRECTANGULAR}.
	 *
	 * @deprecated See {@link #renderFisheyeDomemaster()} for migration guidance.
	 */
	@Deprecated
	public void renderEquirectangular() {
		setCurrentView(ViewType.EQUIRECTANGULAR);
	}

	/**
	 * Sets the current view to {@link ViewType#CUBEMAP}.
	 *
	 * @deprecated See {@link #renderFisheyeDomemaster()} for migration guidance.
	 */
	@Deprecated
	public void renderCubemap() {
		setCurrentView(ViewType.CUBEMAP);
	}

	/**
	 * Sets the current view to {@link ViewType#STANDARD}.
	 *
	 * @deprecated See {@link #renderFisheyeDomemaster()} for migration guidance.
	 */
	@Deprecated
	public void renderStandard() {
		setCurrentView(ViewType.STANDARD);
	}

	/**
	 * Draws the control panel if it is set to be shown.
	 */
	private void drawControlPanel() {
		p.hint(DISABLE_DEPTH_TEST);
		controlManager.updateFpsLabel(p.frameRate);

		if (showControlPanel) {
			controlManager.show();
		} else {
			controlManager.hide();
		}
		p.hint(ENABLE_DEPTH_TEST);
	}

	/**
	 * Draws a floating 200×200 fisheye domemaster thumbnail in the bottom-right corner.
	 *
	 * <p>Uses exclusively the preview fisheye FBO ({@code previewFisheyeDomemaster}).
	 * If the FBO is not yet available the method returns safely without drawing anything.
	 * The output fisheye FBO is never used as a fallback.</p>
	 *
	 * <p>{@code renderPreviewPipeline()} guarantees the fisheye FBO is populated before
	 * this method is called whenever {@code showPreview} is {@code true}.</p>
	 */
	public void drawFloatingPreview() {
		if (previewFisheyeDomemaster == null) {
			return;
		}
		PGraphicsOpenGL previewGraphics = previewFisheyeDomemaster.getDomemasterGraphics();
		if (previewGraphics == null) {
			return;
		}
		float previewWidth  = 200f;
		float previewHeight = 200f;
		float x = p.width  - previewWidth;
		float y = p.height - previewHeight;
		p.image(previewGraphics, x, y, previewWidth, previewHeight);
	}

	/**
	 * Copies a rendered texture into a preview-resolution FBO.
	 *
	 * <p>This operation remains entirely on the GPU. It does not use CPU readback or
	 * intermediate {@link PImage} objects.</p>
	 *
	 * @param source rendered source texture
	 * @param destination preview destination FBO
	 */
	private void copyToPreview(PGraphicsOpenGL source, PGraphicsOpenGL destination) {
		if (source == null || destination == null || source == destination) {
			return;
		}

		destination.beginDraw();
		destination.clear();
		destination.image(source, 0, 0, destination.width, destination.height);
		destination.endDraw();
	}

	/**
	 * Releases all high-resolution output render targets.
	 *
	 * <p>Preview FBOs and external output backends (Syphon, Spout, NDI) are not affected.</p>
	 */
	private void releaseOutputGraphicsResources() {
		if (cubemapRenderer != null) {
			cubemapRenderer.dispose();
			cubemapRenderer = null;
		}
		if (equirectangularRenderer != null) {
			equirectangularRenderer.dispose();
			equirectangularRenderer = null;
		}
		if (fisheyeDomemaster != null) {
			fisheyeDomemaster.dispose();
			fisheyeDomemaster = null;
		}
		if (cubemapViewRenderer != null) {
			cubemapViewRenderer.dispose();
			cubemapViewRenderer = null;
		}
		if (standardRenderer != null) {
			standardRenderer.dispose();
			standardRenderer = null;
		}
	}

	/**
	 * Releases all window-resolution preview render targets.
	 *
	 * <p>Output FBOs and external output backends are not affected.</p>
	 */
	private void releasePreviewGraphicsResources() {
		if (previewCubemapRenderer != null) {
			previewCubemapRenderer.dispose();
			previewCubemapRenderer = null;
		}
		if (previewEquirectangularRenderer != null) {
			previewEquirectangularRenderer.dispose();
			previewEquirectangularRenderer = null;
		}
		if (previewFisheyeDomemaster != null) {
			previewFisheyeDomemaster.dispose();
			previewFisheyeDomemaster = null;
		}
		if (previewCubemapViewRenderer != null) {
			previewCubemapViewRenderer.dispose();
			previewCubemapViewRenderer = null;
		}
		if (standardRendererPreview != null) {
			standardRendererPreview.dispose();
			standardRendererPreview = null;
		}
	}

	/**
	 * Releases all render targets — both output and preview.
	 *
	 * <p>Used during full teardown ({@link #dispose()}).</p>
	 */
	private void releaseGraphicsResources() {
		releaseOutputGraphicsResources();
		releasePreviewGraphicsResources();
	}

	/** Releases the splash-screen graphics layers, if they still exist. */
	private void releaseSplash() {
		if (splash == null) {
			return;
		}
		try {
			splash.dispose();
		} catch (RuntimeException | LinkageError error) {
			LOGGER.warning("SplashScreen disposal failed: " + error.getMessage());
		} finally {
			splash = null;
		}
	}

	/**
	 * Resets output graphics with a new output resolution.
	 * Preview stays constrained to the Processing window size.
	 *
	 * @param newResolution the new output resolution to be set
	 */
	public void resetGraphics(int newResolution) {
		pendingOutputReset = true;
		pendingOutputResolution = newResolution;
		LOGGER.info("Changing output resolution to: " + newResolution);
	}

	/**
	 * Returns the current output resolution used by offscreen render targets.
	 *
	 * @return current output resolution
	 */
	public int getOutputResolution() {
		return outputResolution;
	}

	/**
	 * Sets the current scene to be rendered and updates all relevant components.
	 *
	 * @param newScene the new scene to be set as the current scene
	 */
	public void setCurrentScene(Scene newScene) {
		setScene(newScene);
	}

	/** Returns the scene selected by the authoritative SceneManager. */
	private Scene getCurrentScene() {
		return sceneManager != null ? sceneManager.getCurrentScene() : null;
	}

	/** Keeps stateful Standard renderers aligned with the authoritative SceneManager. */
	private void syncCurrentSceneToRenderers() {
		Scene activeScene = getCurrentScene();
		if (standardRenderer != null) {
			standardRenderer.setCurrentScene(activeScene);
		}
		if (standardRendererPreview != null) {
			standardRendererPreview.setCurrentScene(activeScene);
		}
	}

	/**
	 * Registers event handlers with the Processing sketch and verifies success for each method.
	 */
	private void registerEventHandlers() {
		String[] methodNames = {
				"pre", "draw", "post", "mouseEvent", "keyEvent",
				"stop", "resume", "pause", "dispose"
		};
		boolean allSuccess = true;
		for (String methodName : methodNames) {
			if (!registerMethod(methodName)) {
				allSuccess = false;
			}
		}

		if (allSuccess) {
			LOGGER.info("All event handlers registered successfully.");
		} else {
			LOGGER.warning("One or more event handlers failed to register. Check logs for details.");
		}
	}

	/**
	 * Helper method to register a specific method and log its success or failure.
	 *
	 * @param methodName the name of the method to register
	 * @return true if registration is successful, false otherwise
	 */
	private boolean registerMethod(String methodName) {
		try {
			p.registerMethod(methodName, this);
			registeredEventHandlers.add(methodName);
			LOGGER.info("Successfully registered method: " + methodName);
			return true;
		} catch (Exception e) {
			LOGGER.severe("Failed to register method: " + methodName + ". Error: " + e.getMessage());
			return false;
		}
	}

	/** Removes a registered Processing hook and forgets its ownership. */
	private void unregisterMethod(String methodName) {
		if (!registeredEventHandlers.contains(methodName)) {
			return;
		}
		try {
			p.unregisterMethod(methodName, this);
			registeredEventHandlers.remove(methodName);
		} catch (RuntimeException | LinkageError error) {
			LOGGER.warning("Failed to unregister method " + methodName + ": " + error.getMessage());
		}
	}

	/** Removes every Processing callback still owned by this instance. */
	private void unregisterEventHandlers() {
		for (String methodName : registeredEventHandlers.toArray(new String[0])) {
			unregisterMethod(methodName);
		}
	}

	/**
	 * Handles key events for interaction.
	 * This method must be registered using p.registerMethod("keyEvent", this).
	 *
	 * @param event the KeyEvent object containing details of the key event
	 */
	public void keyEvent(processing.event.KeyEvent event) {
		if (disposed || paused) {
			return;
		}
		if (controlManager == null) {
			Scene activeScene = getCurrentScene();
			if (activeScene != null) {
				activeScene.keyEvent(event);
			}
			return;
		}

		if (event.getAction() == KeyEvent.PRESS) { // Apenas trata eventos de tecla pressionada
			if (!controlManager.isNumberboxActive()) {
				// Primeiro, processa as teclas padrão
				switch (event.getKey()) {
					case 'h':
						showControlPanel = !showControlPanel;
						controlManager.syncPanelVisibility(showControlPanel);
						LOGGER.info("Toggling control panel visibility: " + showControlPanel);
						break;

					case 'm':
						setCurrentView(ViewType.values()[(getCurrentView().ordinal() + 1) % ViewType.values().length]);
						LOGGER.info("Switching view to: " + getCurrentView());
						break;
				}

				// Em seguida, processa as teclas de navegação (LEFT e RIGHT)
				switch (event.getKeyCode()) {
					case PConstants.LEFT:
						if (sceneManager != null) {
							sceneManager.previousScene();
							syncCurrentSceneToRenderers();
							Scene activeScene = getCurrentScene();
							if (activeScene != null) {
								LOGGER.info("Switched to the previous scene: " + activeScene.getName());
							} else {
								LOGGER.warning("No previous scene available.");
							}
						}
						break;

					case PConstants.RIGHT:
						if (sceneManager != null) {
							sceneManager.nextScene();
							syncCurrentSceneToRenderers();
							Scene activeScene = getCurrentScene();
							if (activeScene != null) {
								LOGGER.info("Switched to the next scene: " + activeScene.getName());
							} else {
								LOGGER.warning("No next scene available.");
							}
						}
						break;

					default:
						break;
				}
			}
		}

		// Encaminha o evento para a cena atual
		Scene activeScene = getCurrentScene();
		if (activeScene != null) {
			activeScene.keyEvent(event);
		}
	}


	/**
	 * Handles mouse events for interaction.
	 * This method must be registered using p.registerMethod("mouseEvent", this).
	 *
	 * @param event the MouseEvent object containing details of the mouse event
	 */
	public void mouseEvent(MouseEvent event) {
		if (disposed || paused) {
			return;
		}
		if (splash != null && event.getAction() == MouseEvent.PRESS) {
			splash.mousePressed();
		}

		Scene activeScene = getCurrentScene();
		if (activeScene != null) {
			activeScene.mouseEvent(event);
		}

		// Forward to the native scene-space orbit camera when enabled.
		if (sceneCameraInputEnabled) {
			sceneCamera.mouseEvent(event);
		}

		if (standardRenderer != null) {
			standardRenderer.getCam().mouseEvent(event);
		}
	}

	/**
	 * Handles control events from the ControlP5 library.
	 * The built-in ControlManager registers this callback as a ControlP5 listener.
	 *
	 * @param theEvent the ControlEvent object containing details of the control event
	 */
	public void controlEvent(ControlEvent theEvent) {
		if (disposed || paused) {
			return;
		}
		if (controlManager != null) {
			controlManager.handleEvent(theEvent);
		}

		// Forward the event to the current scene, if one exists
		Scene activeScene = getCurrentScene();
		if (activeScene != null) {
			activeScene.controlEvent(theEvent);
		}
	}

	/**
	 * Returns the instance of the OutputManager.
	 *
	 * @return the OutputManager instance
	 */
	public OutputManager getOutputManager() {
		return outputManager;
	}

	/**
	 * Gets the current fish size.
	 *
	 * @return the current fish size
	 */
	public float getFishSize() {
		return fishSize;
	}

	/**
	 * Sets the fish size.
	 *
	 * @param fishSize the new fish size
	 */
	public void setFishSize(float fishSize) {
		this.fishSize = fishSize;
		if (fisheyeDomemaster != null) {
			fisheyeDomemaster.setSizePercentage(fishSize);
		}
		if (previewFisheyeDomemaster != null) {
			previewFisheyeDomemaster.setSizePercentage(fishSize);
		}
	}

	/**
	 * Gets the current field of view (FOV).
	 *
	 * @return the current FOV
	 */
	public float getFov() {
		return fov;
	}

	/**
	 * Sets the field of view (FOV).
	 *
	 * @param fov the new FOV
	 */
	public void setFov(float fov) {
		this.fov = fov;
	}

	/**
	 * Gets the current pitch.
	 *
	 * @return the current pitch
	 */
	public float getPitch() {
		return sphericalOrientation.getPitch();
	}

	/**
	 * Sets the pitch.
	 *
	 * @param pitch the new pitch
	 */
	public void setPitch(float pitch) {
		sphericalOrientation.setPitch(pitch);
	}

	/**
	 * Gets the current yaw.
	 *
	 * @return the current yaw
	 */
	public float getYaw() {
		return sphericalOrientation.getYaw();
	}

	/**
	 * Sets the yaw.
	 *
	 * @param yaw the new yaw
	 */
	public void setYaw(float yaw) {
		sphericalOrientation.setYaw(yaw);
	}

	/**
	 * Gets the current roll.
	 *
	 * @return the current roll
	 */
	public float getRoll() {
		return sphericalOrientation.getRoll();
	}

	/**
	 * Sets the roll.
	 *
	 * @param roll the new roll
	 */
	public void setRoll(float roll) {
		sphericalOrientation.setRoll(roll);
	}

	/**
	 * Restores the spherical orientation to the identity quaternion.
	 */
	public void resetOrientation() {
		sphericalOrientation.reset();
	}

	/**
	 * Gets the configured legacy preview view.
	 *
	 * <p>In a dedicated {@link RenderMode}, the effective representation is controlled by that
	 * mode while this value is preserved for a later return to {@link RenderMode#FULL}.</p>
	 *
	 * @return configured legacy preview view
	 */
	public ViewType getCurrentView() {
		return currentView;
	}

	/**
	 * Sets the configured legacy preview view.
	 *
	 * <p>The selection takes effect immediately in {@link RenderMode#FULL}. Dedicated modes keep
	 * it as the preview selection to restore when FULL is selected again.</p>
	 *
	 * @param currentView new legacy preview view
	 */
	public void setCurrentView(ViewType currentView) {
		this.currentView = currentView;
	}

	/**
	 * Returns the active global render mode.
	 *
	 * @return active mode, defaulting to {@link RenderMode#FULL}
	 * @since 1.5.0
	 */
	public RenderMode getRenderMode() {
		return renderMode;
	}

	/**
	 * Selects the global rendering behavior.
	 *
	 * <p>FULL preserves independent preview and external-output routing. A dedicated mode
	 * overrides their effective representation without mutating the configured
	 * {@link ViewType} values.</p>
	 *
	 * @param renderMode new global mode; {@code null} is ignored
	 * @since 1.5.0
	 */
	public void setRenderMode(RenderMode renderMode) {
		if (renderMode == null) {
			LOGGER.warning("Ignoring null render mode.");
			return;
		}
		if (this.renderMode == renderMode) {
			return;
		}
		this.renderMode = renderMode;
		LOGGER.info("Render mode set to: " + renderMode);
	}

	/**
	 * Sets the target frame rate applied during setup. Defaults to 60.
	 * Call before setup() to take effect at startup; calling afterwards applies immediately and
	 * updates the default NDI frame-rate metadata. Reapplying the configured value does not
	 * restart Processing's animator.
	 *
	 * @param fps desired frame rate, must be positive
	 */
	public void setTargetFrameRate(int fps) {
		if (fps <= 0) {
			LOGGER.warning("Ignoring invalid target frame rate: " + fps);
			return;
		}
		boolean changed = this.targetFrameRate != fps;
		this.targetFrameRate = fps;
		if (outputManager != null) {
			outputManager.setNdiFrameRate(fps, 1);
		}
		if (changed && initState != InitState.NOT_INITIALIZED) {
			p.frameRate(fps);
		}
	}

	/**
	 * Returns the configured target frame rate.
	 *
	 * @return target frame rate in frames per second
	 */
	public int getTargetFrameRate() {
		return targetFrameRate;
	}

	/**
	 * Checks if output is enabled.
	 *
	 * @return true if output is enabled, false otherwise
	 */
	public boolean isEnableOutput() {
		return outputManager != null
				&& (outputManager.isNdiEnabled()
				|| outputManager.isSpoutEnabled()
				|| outputManager.isSyphonEnabled());
	}

	/**
	 * Checks if the preview is shown.
	 *
	 * @return true if the preview is shown, false otherwise
	 */
	public boolean isShowPreview() {
		return showPreview;
	}

	/**
	 * Sets whether the preview is shown.
	 *
	 * @param showPreview true to show the preview, false to hide
	 */
	public void setShowPreview(boolean showPreview) {
		this.showPreview = showPreview;
	}

	/**
	 * Resets the controls to their default state.
	 */
	public void resetControls() {
		controlManager.resetControls();
	}

	/**
	 * Sets the FisheyeDomemaster instance.
	 *
	 * @param fisheyeDomemaster the new FisheyeDomemaster instance
	 */
	public void setFisheyeDomemaster(FisheyeDomemaster fisheyeDomemaster) {
		this.fisheyeDomemaster = fisheyeDomemaster;
		if (this.fisheyeDomemaster != null) {
			this.fisheyeDomemaster.setSizePercentage(fishSize);
		}
	}

	/**
	 * Gets the current FisheyeDomemaster instance.
	 *
	 * @return the current FisheyeDomemaster instance
	 */
	public FisheyeDomemaster getFisheyeDomemaster() {
		return fisheyeDomemaster;
	}

	/**
	 * Returns the instance of the EquirectangularRenderer.
	 * @return the EquirectangularRenderer instance.
	 */
	public EquirectangularRenderer getEquirectangularRenderer() {
		return equirectangularRenderer;
	}

	/**
	 * Returns the instance of the CubemapViewRenderer.
	 * @return the CubemapViewRenderer instance.
	 */
	public CubemapViewRenderer getCubemapViewRenderer() {
		return cubemapViewRenderer;
	}

	/**
	 * Returns the instance of the StandardRenderer.
	 * @return the StandardRenderer instance.
	 */
	public StandardRenderer getStandardRenderer() {
		return standardRenderer;
	}

	/**
	 * Returns the native scene-space orbit camera service.
	 *
	 * <p>Scenes drive space navigation by calling {@code getSceneCamera().apply(pg)}
	 * inside {@code sceneRender} (between {@code pushMatrix}/{@code popMatrix}). The
	 * camera transforms the scene modelview directly, so it works across every
	 * projection without touching the dome parameters (yaw/pitch/roll/fov).</p>
	 *
	 * @return the shared {@link OrbitCamera} instance
	 */
	public OrbitCamera getSceneCamera() {
		return sceneCamera;
	}

	/**
	 * Enables or disables built-in mouse handling for the scene camera.
	 * When enabled, the library forwards mouse drag/wheel events to
	 * {@link #getSceneCamera()} automatically.
	 *
	 * @param enabled true to let the library drive the scene camera from mouse input
	 */
	public void setSceneCameraInputEnabled(boolean enabled) {
		this.sceneCameraInputEnabled = enabled;
	}

	/**
	 * Returns whether built-in mouse handling for the scene camera is enabled.
	 *
	 * @return true if the library forwards mouse input to the scene camera
	 */
	public boolean isSceneCameraInputEnabled() {
		return sceneCameraInputEnabled;
	}

	/**
	 * Gets the current PApplet instance.
	 *
	 * @return the current PApplet instance
	 */
	public PApplet getPApplet() {

		return p;
	}

	/**
	 * Gets the width of the PApplet window.
	 *
	 * @return the width of the PApplet window
	 */
	public int getWidth() {
		return p.width;
	}

	/**
	 * Gets the height of the PApplet window.
	 *
	 * @return the height of the PApplet window
	 */
	public int getHeight() {
		return p.height;
	}

	/**
	 * Checks if the instance is initialized and ready to render.
	 *
	 * @return true if the instance is initialized and managers are ready, false otherwise
	 */
	public boolean isInitialized() {
		return initState == InitState.MANAGERS_READY;
	}

	/**
	 * Returns the current initialization state.
	 *
	 * @return the current InitState
	 */
	public InitState getInitState() {
		return initState;
	}

	/**
	 * Sets the SceneManager instance for managing multiple scenes.
	 *
	 * <p>The incoming manager is already responsible for activating its first registered scene.
	 * This facade adopts lifecycle ownership: replacing the manager or disposing the facade clears
	 * its registrations and disposes its active scene. Configured Standard renderers are synchronized
	 * immediately; all other render paths query the manager directly.</p>
	 *
	 * @param sceneManager the SceneManager instance to manage scenes
	 */
	public void setSceneManager(SceneManager sceneManager) {
		if (disposed) {
			LOGGER.warning("Cannot replace SceneManager after disposal.");
			return;
		}
		if (sceneManager == null || sceneManager.getSceneCount() == 0) {
			LOGGER.severe("SceneManager is null or contains no scenes.");
			return;
		}
		if (this.sceneManager == sceneManager) {
			syncCurrentSceneToRenderers();
			return;
		}

		Scene previousScene = getCurrentScene();
		Scene nextScene = sceneManager.getCurrentScene();
		if (this.sceneManager != null) {
			if (previousScene == nextScene) {
				this.sceneManager.detachScenes();
			} else {
				this.sceneManager.clearScenes();
			}
		}
		this.sceneManager = sceneManager;
		fallbackScene = null;
		syncCurrentSceneToRenderers();
	}

	/**
	 * Returns the current SceneManager instance.
	 *
	 * @return the current SceneManager
	 */
	public SceneManager getSceneManager() {
		return sceneManager;
	}

	/**
	 * Method called before each draw call.
	 */
	public void pre() {
		if (disposed || paused) {
			return;
		}
		if (initState != InitState.MANAGERS_READY) {
			LOGGER.warning("Render skipped: System not ready. State: " + initState);
			return;
		}

		syncCurrentSceneToRenderers();
		Scene activeScene = getCurrentScene();
		if (activeScene != null) {
			activeScene.update();
		}

		// Advance the native scene camera smoothing once per frame.
		sceneCamera.update();
	}

	/**
	 * Post-initialization method to set up managers after the initial setup.
	 */
	public void post() {
		if (!disposed && !paused && initState == InitState.SETUP_COMPLETE) {
			try {
				initializeManagers();
				if (initState == InitState.MANAGERS_READY) {
					unregisterMethod("post");
					LOGGER.info("Post-initialization completed successfully.");
				} else {
					LOGGER.warning("Post-initialization did not complete; the next post hook will retry.");
				}
			} catch (Exception e) {
				LOGGER.severe("Error during post-initialization: " + e.getMessage());
			}
		}
	}

	/**
	 * Stops the instance permanently and releases all owned resources.
	 *
	 * <p>This terminal operation delegates to {@link #dispose()} and is idempotent.</p>
	 */
	public void stop() {
		dispose();
	}

	/**
	 * Resumes processes after a pause.
	 */
	public void resume() {
		if (disposed || !paused) {
			return;
		}
		LOGGER.info("Resuming processes...");
		if (outputManager != null) {
			if (resumeNdiOutput && !outputManager.isNdiEnabled()) {
				outputManager.toggleOutput("ndi");
			}
			if (resumeSpoutOutput && !outputManager.isSpoutEnabled()) {
				outputManager.toggleOutput("spout");
			}
			if (resumeSyphonOutput && !outputManager.isSyphonEnabled()) {
				outputManager.toggleOutput("syphon");
			}
		}
		clearPausedOutputState();
		LOGGER.info("Processes resumed.");
	}

	/**
	 * Pauses all processes.
	 */
	public void pause() {
		if (disposed || paused) {
			return;
		}
		LOGGER.info("Pausing processes...");
		paused = true;
		if (outputManager != null) {
			resumeNdiOutput = outputManager.isNdiEnabled();
			resumeSpoutOutput = outputManager.isSpoutEnabled();
			resumeSyphonOutput = outputManager.isSyphonEnabled();
			outputManager.stopOutput();
		}
		LOGGER.info("Processes paused.");
	}

	private void clearPausedOutputState() {
		paused = false;
		resumeNdiOutput = false;
		resumeSpoutOutput = false;
		resumeSyphonOutput = false;
	}

	/**
	 * Releases resources and cleans up before the application exits.
	 */
	public void dispose() {
		if (disposed) {
			unregisterEventHandlers();
			return;
		}
		disposed = true;
		LOGGER.info("Disposing resources...");
		clearPausedOutputState();

		if (outputManager != null) {
			try {
				outputManager.shutdownOutputs();
			} catch (Exception | LinkageError error) {
				LOGGER.warning("OutputManager disposal failed: " + error.getMessage());
			}
			outputManager = null;
		}
		if (controlManager != null) {
			try {
				controlManager.dispose();
			} catch (RuntimeException | LinkageError error) {
				LOGGER.warning("ControlManager disposal failed: " + error.getMessage());
			}
			controlManager = null;
		}
		if (sceneManager != null) {
			try {
				sceneManager.clearScenes();
			} catch (RuntimeException | LinkageError error) {
				LOGGER.warning("SceneManager disposal failed: " + error.getMessage());
			}
		}
		fallbackScene = null;
		try {
			releaseGraphicsResources();
		} catch (RuntimeException | LinkageError error) {
			LOGGER.warning("Renderer disposal failed: " + error.getMessage());
		}
		if (cameraManager != null) {
			try {
				cameraManager.dispose();
			} catch (RuntimeException | LinkageError error) {
				LOGGER.warning("CameraManager disposal failed: " + error.getMessage());
			}
			cameraManager = null;
		}
		releaseSplash();
		initState = InitState.NOT_INITIALIZED;
		unregisterEventHandlers();
		LOGGER.info("Resources disposed successfully.");
	}
}
