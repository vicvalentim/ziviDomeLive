package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.manager.*;
import com.victorvalentim.zividomelive.render.*;
import com.victorvalentim.zividomelive.render.camera.*;
import com.victorvalentim.zividomelive.performance.*;
import processing.core.*;
import processing.event.*;
import processing.opengl.*;
import controlP5.*;
import java.util.LinkedHashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static processing.core.PConstants.*;

/**
 * Main artist-facing facade for live fulldome, spherical, and Standard rendering in Processing.
 *
 * <p>Create one instance in the sketch's {@code setup()}, call {@link #setup()} after the P3D
 * surface exists, and provide content through {@link Scene}. The facade owns scene activation,
 * renderers, Processing hooks, camera synchronization, the built-in control panel, outputs, and
 * terminal cleanup.</p>
 *
 * <p>Methods documented as <em>Processing Callback</em> are Java-public only because the
 * constructor registers them with Processing. Sketches must not call those callbacks directly.
 * Performance methods are explicitly Experimental; the remaining artist-facing facade is Stable
 * for the 2.x series.</p>
 *
 * <p><strong>API stability:</strong> Stable, except methods explicitly marked Experimental or
 * Processing Callback.</p>
 */
public class ziviDomeLive {

	/**
	 * Enum representing the initialization state of the library.
	 */
	enum InitState {
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
	private final RenderPipeline renderPipeline;
	private final PerformanceMonitor performanceMonitor = new PerformanceMonitor();
	private final GpuPerformanceTimer gpuPerformanceTimer;
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
	private static final String EQUIRECT_SAMPLERCUBE_VERT = "data/shaders/samplercube/equirectangular.vert";
	private static final String EQUIRECT_SAMPLERCUBE_FRAG = "data/shaders/samplercube/equirectangular.frag";
	private static final String DOME_SAMPLERCUBE_VERT = "data/shaders/samplercube/fisheye.vert";
	private static final String DOME_SAMPLERCUBE_FRAG = "data/shaders/samplercube/fisheye.frag";
	private static final String SKYBOX_SAMPLERCUBE_VERT = "data/shaders/samplercube/skybox.vert";
	private static final String SKYBOX_SAMPLERCUBE_FRAG = "data/shaders/samplercube/skybox.frag";

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
	private final EnvironmentState environmentState = new EnvironmentState();
	private OutputManagerImpl outputManager;
	private boolean resumeNdiOutput;
	private boolean resumeSpoutOutput;
	private boolean resumeSyphonOutput;
	private SplashScreen splash;
	private SceneManager sceneManager;
	private Scene bootstrapScene;
	private final Map<Scene, SceneServices> sceneServices = new IdentityHashMap<>();
	private Thread renderThread;
	private final SceneManager.LifecycleListener sceneLifecycle = new SceneManager.LifecycleListener() {
		@Override
		public void beforeSetup(Scene scene) {
			prepareSceneServices(scene);
		}

		@Override
		public void beforeDispose(Scene scene) {
			prepareSceneServicesForDisposal(scene);
		}

		@Override
		public void afterDispose(Scene scene) {
			releaseSceneServices(scene);
		}
	};

	/**
	 * Aspect policy used to compute Standard output dimensions.
	 *
	 * <p><strong>API stability:</strong> Stable.</p>
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

	private ViewType currentView = ViewType.DOMEMASTER;
	private ViewType performanceOutputDemand;
	private RenderMode renderMode = RenderMode.FULL;
	private StandardOutputAspectMode standardOutputAspectMode = StandardOutputAspectMode.AUTO;
	private boolean sphericalCaptureActive = false;

	private boolean pendingOutputReset = false;
	private int pendingOutputResolution = outputResolution;
	private static final Logger LOGGER = LogManager.getLogger();


	/**
	 * Creates a facade owned by the supplied Processing sketch and registers runtime callbacks.
	 *
	 * <p>Call {@link #setup()} from the sketch's {@code setup()} after creating its P3D surface.</p>
	 *
	 * @param p non-null Processing sketch used for rendering
	 * @throws IllegalArgumentException if the PApplet instance is null
	 */
	public ziviDomeLive(PApplet p) {
		if (p == null) {
			throw new IllegalArgumentException("PApplet instance cannot be null.");
		}
		this.p = p;
		this.gpuPerformanceTimer = new GpuPerformanceTimer(p, performanceMonitor);
		this.renderPipeline = new RenderPipeline(this);
		this.sceneManager = new SceneManager();
		this.sceneManager.setLifecycleListener(sceneLifecycle);
		this.renderThread = Thread.currentThread();

		welcome();
		registerEventHandlers();
	}

	/**
	 * Sets the global logging mode used by the library.
	 * Call this before creating a ziviDomeLive instance so startup follows the selected profile.
	 *
	 * @param mode desired logging mode
	 */
	public static void setLogMode(LogMode mode) {
		LogManager.setMode(mode);
	}

	/** Enables verbose DEBUG logs to the console and library log file. */
	public static void enableDebugLogging() {
		LogManager.setMode(LogMode.DEBUG);
	}

	/** Enables RELEASE mode, which disables library-managed log output. */
	public static void enableReleaseLogging() {
		LogManager.setMode(LogMode.RELEASE);
	}

	/**
	 * Returns the currently active logging mode.
	 *
	 * @return active logging mode
	 */
	public static LogMode getLogMode() {
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
	 * Selects and activates a scene through the facade-owned manager.
	 *
	 * <p>An unregistered instance is registered automatically. Switching performs the complete
	 * activation order: stop old activation work, dispose the previous scene, create fresh
	 * services, configure the new scene, then set it up.</p>
	 *
	 * @param scene scene instance to activate; {@code null} is ignored
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
		if (bootstrapScene != null
				&& scene != bootstrapScene
				&& sceneManager.containsScene(bootstrapScene)) {
			sceneManager.clearScenes();
			bootstrapScene = null;
		}
		sceneManager.activateScene(scene);
		syncCurrentSceneToRenderers();
	}

	/**
	 * Registers a scene with the facade-owned manager without activating it when another user
	 * scene is already active. The first registered user scene replaces the bootstrap fallback.
	 *
	 * <p>The facade lifecycle guarantees that {@link Scene#configure(SceneServices)} runs
	 * before the first setup.</p>
	 *
	 * @param scene scene to register
	 */
	public void registerScene(Scene scene) {
		if (disposed) {
			LOGGER.warning("Cannot register a scene after disposal.");
			return;
		}
		if (scene == null) {
			LOGGER.warning("Cannot register a null scene.");
			return;
		}
		if (bootstrapScene != null && sceneManager.containsScene(bootstrapScene)) {
			sceneManager.clearScenes();
			bootstrapScene = null;
		}
		sceneManager.registerScene(scene);
		syncCurrentSceneToRenderers();
	}

	/**
	 * Sets up the rendering environment, including frame rate, OpenGL info, texture hints,
	 * output coordination, and the built-in interface.
	 *
	 * <p>Call once from the sketch's {@code setup()} after its P3D surface has been created. The
	 * operation is idempotent; renderer allocation completes lazily in the registered
	 * {@link #post()} callback when the OpenGL context is active.</p>
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
		renderThread = Thread.currentThread();

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
			LOGGER.info("Renderer hints configured.");
		} catch (Exception e) {
			LOGGER.severe("Error configuring texture hints: " + e.getMessage());
		}

		try {
			outputManager = new OutputManagerImpl(this);
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
				bootstrapScene = new DefaultScene(p);
				sceneManager.registerScene(bootstrapScene);
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
	void printOpenGLInfo(PApplet p) {
		ProcessingGlCapabilities capabilities = ProcessingGlAdapter.getDefault().queryCapabilities(p);
		if (capabilities.isOpenGlRenderer()) {
			LOGGER.info("OpenGL Version: " + capabilities.version());
			LOGGER.info("OpenGL Vendor: " + capabilities.vendor());
			LOGGER.info("OpenGL Renderer: " + capabilities.renderer());
			LOGGER.info("Runtime Architecture: " + System.getProperty("os.arch"));
			LOGGER.info("JOGL Profile: " + capabilities.joglProfile());
			LOGGER.info("Hardware Rasterizer Known: " + capabilities.isHardwareRasterizerKnown());
			LOGGER.info("Hardware Rasterizer: " + capabilities.isHardwareRasterizer());
			LOGGER.info("OpenGL Texture Support: " + capabilities.supportsTexture());
			LOGGER.info("OpenGL FBO Support: " + capabilities.supportsFramebuffer());
			LOGGER.info("OpenGL Cubemap Support: " + capabilities.supportsCubemap());
			LOGGER.info("OpenGL Seamless Cubemap Support: " + capabilities.supportsSeamlessCubemap());
			LOGGER.info("OpenGL Anisotropic Filtering Support: " + capabilities.supportsAnisotropicFiltering());
			LOGGER.info("OpenGL PBO Support: " + capabilities.supportsPixelBufferObject());
			LOGGER.info("OpenGL Fence Support: " + capabilities.supportsSyncFence());
		} else {
			LOGGER.severe("The current renderer is not OpenGL.");
		}
	}

	/**
	 * Configures renderer hints for the rendering environment.
	 */
	void setupHints() {
		p.hint(DISABLE_OPENGL_ERRORS);
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
	void initializeManagers() {
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
				outputManager.initializeLocalTextureOutput(renderPipeline.finalFrameViews());

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
			outputManager.shutdownOutputsTerminal();
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
		cubemapRenderer = new CubemapRenderer(outputResolution, p, environmentState);
		LOGGER.info("CubemapRenderer (output) initialized at " + outputResolution + "px.");
		equirectangularRenderer = new EquirectangularRenderer(
				outputResolution,
				EQUIRECT_SAMPLERCUBE_FRAG,
				EQUIRECT_SAMPLERCUBE_VERT,
				p);
		LOGGER.info("EquirectangularRenderer (output) initialized.");
		fisheyeDomemaster = new FisheyeDomemaster(
				outputResolution,
				DOME_SAMPLERCUBE_FRAG,
				DOME_SAMPLERCUBE_VERT,
				p);
		fisheyeDomemaster.setSizePercentage(fishSize);
		LOGGER.info("FisheyeDomemaster (output) initialized.");
		cubemapViewRenderer = new CubemapViewRenderer(
				p,
				outputResolution,
				SKYBOX_SAMPLERCUBE_FRAG,
				SKYBOX_SAMPLERCUBE_VERT);
		LOGGER.info("CubemapViewRenderer (output) initialized.");
		int[] standardOutputDimensions = computeStandardOutputDimensions();
		standardRenderer = new StandardRenderer(
				p,
				standardOutputDimensions[0],
				standardOutputDimensions[1],
				getCurrentScene(),
				environmentState
		);
		LOGGER.info("StandardRenderer (output) initialized at "
				+ standardOutputDimensions[0] + "×" + standardOutputDimensions[1] + "px.");
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

		previewCubemapRenderer = new CubemapRenderer(previewResolution, p, environmentState);
		previewEquirectangularRenderer = new EquirectangularRenderer(
				previewResolution,
				EQUIRECT_SAMPLERCUBE_FRAG,
				EQUIRECT_SAMPLERCUBE_VERT,
				p);
		previewFisheyeDomemaster = new FisheyeDomemaster(
				previewResolution,
				DOME_SAMPLERCUBE_FRAG,
				DOME_SAMPLERCUBE_VERT,
				p);
		previewFisheyeDomemaster.setSizePercentage(fishSize);
		previewCubemapViewRenderer = new CubemapViewRenderer(
				p,
				previewResolution,
				SKYBOX_SAMPLERCUBE_FRAG,
				SKYBOX_SAMPLERCUBE_VERT);

		// Dynamic dimensions (0, 0) → renderer uses parent.width/parent.height each frame,
		// preserving the window aspect ratio and handling window resize automatically.
		standardRendererPreview = new StandardRenderer(
				p, 0, 0, getCurrentScene(), environmentState);

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
	void ensurePreviewRenderers() {
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
			boolean profiling = performanceMonitor.isEnabled();
			long started = profiling ? performanceMonitor.start() : 0L;
			sphericalCaptureActive = true;
			try {
				previewCubemapRenderer.captureCubemap(
						sphericalOrientation.getQuaternion(), getCurrentScene());
			} finally {
				sphericalCaptureActive = false;
				if (profiling) performanceMonitor.record(PerformanceMetric.CUBEMAP_PREVIEW, started);
			}
		}
	}

	private CubemapTarget resolveMasterNativeCubemap(RenderRequirementsPolicy.Requirements output) {
		if (output != null && output.needsCubemapSource()) {
			return cubemapRenderer != null ? cubemapRenderer.getNativeCubemapTarget() : null;
		}
		return previewCubemapRenderer != null ? previewCubemapRenderer.getNativeCubemapTarget() : null;
	}

	private boolean hasMasterNativeCubemap(RenderRequirementsPolicy.Requirements output) {
		CubemapTarget nativeCubemap = resolveMasterNativeCubemap(output);
		return nativeCubemap != null && nativeCubemap.isAllocated();
	}

	/**
	 * Computes the window-preview requirements for the current frame.
	 *
	 * <p>{@code showPreview=true} forces the fisheye chain so the floating thumbnail can be
	 * composed even when the main preview view is Standard.</p>
	 *
	 * @return cached requirements for the current preview state
	 */
	RenderRequirementsPolicy.Requirements computePreviewRequirements() {
		return RenderRequirementsPolicy.forPreview(renderMode, getCurrentView(), showPreview);
	}

	/**
	 * Computes output-resolution requirements for the current frame.
	 *
	 * <p>Enabled external routes and the optional performance-qualification demand are merged into
	 * one minimal dependency set. The qualification demand never enables an external transport.</p>
	 *
	 * @return cached requirements for active output-resolution consumers
	 */
	RenderRequirementsPolicy.Requirements computeOutputRequirements() {
		boolean outputsActive = outputManager != null && outputManager.isActive();
		ViewType qualificationView = performanceMonitor.isEnabled()
				? performanceOutputDemand
				: null;
		boolean qualificationActive = qualificationView != null;
		return RenderRequirementsPolicy.forOutputs(
				outputsActive || qualificationActive,
				(outputsActive && outputManager.requiresView(ViewType.DOMEMASTER))
						|| qualificationView == ViewType.DOMEMASTER,
				(outputsActive && outputManager.requiresView(ViewType.EQUIRECTANGULAR))
						|| qualificationView == ViewType.EQUIRECTANGULAR,
				(outputsActive && outputManager.requiresView(ViewType.SKYBOX))
						|| qualificationView == ViewType.SKYBOX,
				(outputsActive && outputManager.requiresView(ViewType.STANDARD))
						|| qualificationView == ViewType.STANDARD
		);
	}

	boolean hasOutputRenderDemand() {
		OutputManagerImpl manager = outputManager;
		return (manager != null && manager.isActive())
				|| (performanceMonitor.isEnabled() && performanceOutputDemand != null);
	}

	/**
	 * Captures at most one cubemap for the current frame.
	 *
	 * <p>If an output-resolution consumer requires cubemap data, the output-resolution cubemap
	 * becomes the master source for both output and preview projections. Otherwise, the preview-resolution
	 * cubemap is captured only when the window preview or floating fisheye thumbnail needs it.</p>
	 *
	 * @param preview preview requirements for the current frame
	 * @param output output requirements for the current frame
	 */
	void captureMasterCubemap(
			RenderRequirementsPolicy.Requirements preview,
			RenderRequirementsPolicy.Requirements output) {
		if (output.needsCubemapSource()) {
			captureCubemap();
			return;
		}

		if (preview.needsCubemapSource()) {
			capturePreviewCubemap();
		}
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
	void renderPreviewPipeline(
			RenderRequirementsPolicy.Requirements preview,
			RenderRequirementsPolicy.Requirements output) {
		if (preview.needsStandard()) {
			renderStandardProfiled(standardRendererPreview, PerformanceMetric.STANDARD_PREVIEW);
		}

		if (preview.needsEquirectangular()) {
			if (output.needsEquirectangular() && output.needsCubemapSource()) {
				copyToPreview(equirectangularRenderer.getEquirectangular(), previewEquirectangularRenderer.getEquirectangular());
			} else {
				long started = performanceMonitor.isEnabled() ? performanceMonitor.start() : 0L;
				previewEquirectangularRenderer.render(resolveMasterNativeCubemap(output));
				recordProjection(
						PerformanceMetric.EQUIRECTANGULAR,
						PerformanceMetric.EQUIRECTANGULAR_PREVIEW,
						started);
			}
		}

		if (preview.needsFisheye()) {
			if (output.needsFisheye()) {
				copyToPreview(fisheyeDomemaster.getDomemasterGraphics(), previewFisheyeDomemaster.getDomemasterGraphics());
			} else {
				long started = performanceMonitor.isEnabled() ? performanceMonitor.start() : 0L;
				previewFisheyeDomemaster.applyShader(
						resolveMasterNativeCubemap(output),
						getFov());
				recordProjection(
						PerformanceMetric.DOMEMASTER,
						PerformanceMetric.DOMEMASTER_PREVIEW,
						started);
			}
		}

		if (preview.needsCubemapLayout()) {
			if (output.needsCubemapLayout() && output.needsCubemapSource()) {
				copyToPreview(cubemapViewRenderer.getCubemap(), previewCubemapViewRenderer.getCubemap());
			} else {
				long started = performanceMonitor.isEnabled() ? performanceMonitor.start() : 0L;
				previewCubemapViewRenderer.drawCubemapToGraphics(resolveMasterNativeCubemap(output));
				recordProjection(
						PerformanceMetric.SKYBOX,
						PerformanceMetric.SKYBOX_PREVIEW,
						started);
			}
		}
	}

	/**
	 * Renders the high-resolution passes requested by the resolved output requirements.
	 *
	 * <p>Requirements may originate from enabled external outputs or from the experimental
	 * performance-qualification demand. All FBOs produced here belong to the output pipeline ({@code outputResolution})
	 * and remain offscreen. They are never composited onto the Processing window. After this
	 * method returns, all relevant {@code endDraw()} calls have completed and the FBOs are
	 * ready for
	 * {@link com.victorvalentim.zividomelive.manager.OutputManager#sendOutput(FrameViews)}.</p>
	 *
	 * <p>The set of passes is derived from
	 * {@link com.victorvalentim.zividomelive.manager.OutputManager#requiresView(ViewType)}:
	 * only the minimal dependency chain is executed. A single cubemap capture supplies all
	 * output passes that need it.</p>
	 *
	 * <p>Example: NDI requests equirectangular, Syphon requests fisheye →
	 * one cubemap capture → one equirectangular samplerCube pass and one fisheye
	 * samplerCube pass.</p>
	 *
	 * <p>Must be called from the Processing draw thread after requirements have been resolved.</p>
	 */
	void renderOutputPipeline(RenderRequirementsPolicy.Requirements output) {
		if (output.needsCubemapSource() && !hasMasterNativeCubemap(output)) {
			return;
		}

		if (output.needsEquirectangular()) {
			long started = performanceMonitor.isEnabled() ? performanceMonitor.start() : 0L;
			equirectangularRenderer.render(
					cubemapRenderer != null ? cubemapRenderer.getNativeCubemapTarget() : null);
			recordProjection(
					PerformanceMetric.EQUIRECTANGULAR,
					PerformanceMetric.EQUIRECTANGULAR_OUTPUT,
					started);
		}

		if (output.needsFisheye()) {
			long started = performanceMonitor.isEnabled() ? performanceMonitor.start() : 0L;
			fisheyeDomemaster.applyShader(
					cubemapRenderer != null ? cubemapRenderer.getNativeCubemapTarget() : null,
					getFov());
			recordProjection(
					PerformanceMetric.DOMEMASTER,
					PerformanceMetric.DOMEMASTER_OUTPUT,
					started);
		}

		if (output.needsCubemapLayout()) {
			long started = performanceMonitor.isEnabled() ? performanceMonitor.start() : 0L;
			cubemapViewRenderer.drawCubemapToGraphics(
					cubemapRenderer != null ? cubemapRenderer.getNativeCubemapTarget() : null);
			recordProjection(
					PerformanceMetric.SKYBOX,
					PerformanceMetric.SKYBOX_OUTPUT,
					started);
		}

		if (output.needsStandard()) {
			renderStandardProfiled(standardRenderer, PerformanceMetric.STANDARD_OUTPUT);
		}
	}

	private void renderStandardProfiled(StandardRenderer renderer, PerformanceMetric domainMetric) {
		boolean profiling = performanceMonitor.isEnabled();
		long started = profiling ? performanceMonitor.start() : 0L;
		renderer.render();
		if (profiling) {
			long duration = Math.max(0L, System.nanoTime() - started);
			performanceMonitor.recordDuration(PerformanceMetric.STANDARD_RENDER, duration);
			performanceMonitor.recordDuration(domainMetric, duration);
		}
	}

	private void recordProjection(
			PerformanceMetric aggregate,
			PerformanceMetric domainMetric,
			long started) {
		if (!performanceMonitor.isEnabled() || started == 0L) {
			return;
		}
		long duration = Math.max(0L, System.nanoTime() - started);
		performanceMonitor.recordDuration(aggregate, duration);
		performanceMonitor.recordDuration(domainMetric, duration);
	}

	/**
	 * Resolves a completed high-resolution output target without exposing its producer.
	 *
	 * <p>The returned target is owned by the existing renderer backend and remains valid only
	 * until that backend is reallocated or disposed.</p>
	 */
	PGraphicsOpenGL resolveFinalFrame(ViewType view) {
		if (view == null) {
			return null;
		}

		switch (view) {
			case DOMEMASTER:
				return fisheyeDomemaster != null
						? fisheyeDomemaster.getDomemasterGraphics()
						: null;
			case EQUIRECTANGULAR:
				return equirectangularRenderer != null
						? equirectangularRenderer.getEquirectangular()
						: null;
			case SKYBOX:
				return cubemapViewRenderer != null
						? cubemapViewRenderer.getCubemap()
						: null;
			case STANDARD:
				return standardRenderer != null
						? standardRenderer.getStandardView()
						: null;
			default:
				return null;
		}
	}

	/**
	 * Composites the current preview FBO onto the Processing window.
	 *
	 * <p>Only preview-resolution FBOs are passed to
	 * {@link processing.core.PApplet#image(processing.core.PImage, float, float, float, float)}
	 * here. Output FBOs are never drawn onto the main window by this method.</p>
	 */
	void displayPreviewCurrentView() {
		ViewType effectiveView = RenderRequirementsPolicy.resolveView(renderMode, getCurrentView());
		switch (effectiveView) {
			case SKYBOX:
				displayView(previewCubemapViewRenderer.getCubemap());
				break;
			case EQUIRECTANGULAR:
				displayView(previewEquirectangularRenderer.getEquirectangular());
				break;
			case DOMEMASTER:
				displayView(previewFisheyeDomemaster.getDomemasterGraphics());
				break;
			case STANDARD:
				displayView(standardRendererPreview.getStandardView());
				break;
		}
	}

	/**
	 * Processing Callback that renders the already-updated frame and built-in interface.
	 *
	 * <p>The constructor registers this method automatically. Sketches must not call it directly or
	 * call a second ziviDomeLive render method from their own {@code draw()}.</p>
	 */
	public void draw() {
		if (disposed || paused || initState != InitState.MANAGERS_READY) {
			p.background(0, 0);
			return;
		}

		// Renderiza o conteúdo principal em segundo plano
		renderPipeline.renderFrame();

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

	/** Returns whether the current renderer backend and active scene can produce a frame. */
	boolean isRenderContentReady() {
		return standardRendererPreview != null
				&& standardRenderer != null
				&& getCurrentScene() != null;
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
	void handleGraphicsReset() {
		if (pendingOutputReset) {
			boolean profiling = performanceMonitor.isEnabled();
			long started = profiling ? performanceMonitor.start() : 0L;
			LOGGER.info("Applying output resolution change: " + pendingOutputResolution + "px.");
			try {
				MouseControlledCamera preservedStandardCamera = standardCameraForGraphicsReset();
				releaseOutputGraphicsResources();
				outputResolution = pendingOutputResolution;
				initializeOutputRenderers();
				restoreStandardCameraAfterGraphicsReset(preservedStandardCamera);
				if (outputManager != null) {
					outputManager.notifyResolutionChanged(outputResolution);
				}
				pendingOutputReset = false;
				LOGGER.info("Output graphics reset completed at " + outputResolution + "px.");
			} finally {
				if (profiling) performanceMonitor.record(PerformanceMetric.GRAPHICS_RESET, started);
			}
		}
	}

	MouseControlledCamera standardCameraForGraphicsReset() {
		if (standardRenderer != null) {
			return standardRenderer.getCam();
		}
		return standardRendererPreview != null ? standardRendererPreview.getCam() : null;
	}

	void restoreStandardCameraAfterGraphicsReset(MouseControlledCamera preservedCamera) {
		if (preservedCamera == null || standardRenderer == null) {
			return;
		}
		standardRenderer.setCam(preservedCamera);
		if (standardRendererPreview != null) {
			standardRendererPreview.setCam(preservedCamera);
		}
	}

	/**
	 * Captures the cubemap for the current scene.
	 */
	private void captureCubemap() {
		if (cubemapRenderer != null) {
			boolean profiling = performanceMonitor.isEnabled();
			long started = profiling ? performanceMonitor.start() : 0L;
			sphericalCaptureActive = true;
			try {
				cubemapRenderer.captureCubemap(
						sphericalOrientation.getQuaternion(), getCurrentScene());
			} finally {
				sphericalCaptureActive = false;
				if (profiling) performanceMonitor.record(PerformanceMetric.CUBEMAP_OUTPUT, started);
			}
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
	 * Draws the control panel if it is set to be shown.
	 */
	void drawControlPanel() {
		controlManager.syncPanelVisibility(showControlPanel);
		if (!showControlPanel) {
			return;
		}
		p.hint(DISABLE_DEPTH_TEST);
		controlManager.updateFpsLabel(p.frameRate);
		p.hint(ENABLE_DEPTH_TEST);
	}

	/**
	 * Draws a floating 200×200 fisheye domemaster thumbnail in the bottom-right corner.
	 *
	 * <p>Uses exclusively the preview fisheye FBO ({@code previewFisheyeDomemaster}).
	 * If the FBO is not yet available the method returns safely without drawing anything.
	 * The output fisheye FBO is never substituted for the preview target.</p>
	 *
	 * <p>{@code renderPreviewPipeline()} guarantees the fisheye FBO is populated before
	 * this method is called whenever {@code showPreview} is {@code true}.</p>
	 */
	void drawFloatingPreview() {
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

		boolean profiling = performanceMonitor.isEnabled();
		long started = profiling ? performanceMonitor.start() : 0L;
		boolean drawing = false;
		try {
			destination.beginDraw();
			drawing = true;
			destination.clear();
			destination.image(source, 0, 0, destination.width, destination.height);
		} finally {
			if (drawing) {
				destination.endDraw();
			}
			if (profiling) performanceMonitor.record(PerformanceMetric.PREVIEW_COPY, started);
		}
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
	 * Schedules output-target recreation at a safe draw boundary.
	 * Preview resolution remains derived from the Processing window.
	 *
	 * @param newResolution positive output resolution in pixels
	 * @throws IllegalArgumentException when {@code newResolution} is not positive
	 */
	public void resetGraphics(int newResolution) {
		if (newResolution <= 0) {
			throw new IllegalArgumentException("Output resolution must be positive.");
		}
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
	 * Alias for {@link #setScene(Scene)} retained as part of the 2.x scene API.
	 *
	 * @param newScene scene instance to activate; {@code null} is ignored
	 */
	public void setCurrentScene(Scene newScene) {
		setScene(newScene);
	}

	/** Returns the scene selected by the authoritative SceneManager. */
	private Scene getCurrentScene() {
		return sceneManager != null ? sceneManager.getCurrentScene() : null;
	}

	/**
	 * Returns lifecycle-aware services for a scene owned by this facade.
	 *
	 * <p>Normally scenes retain the value supplied through
	 * {@link Scene#configure(SceneServices)}. This accessor is also convenient for
	 * Processing sketches that already keep a facade reference.</p>
	 *
	 * @param scene scene whose current activation owns the services
	 * @return services for the scene's current activation, or {@code null} when inactive
	 */
	synchronized SceneServices getSceneServices(Scene scene) {
		if (disposed) {
			throw new IllegalStateException("Cannot access scene services after facade disposal.");
		}
		if (scene == null) {
			throw new IllegalArgumentException("Scene cannot be null.");
		}
		return sceneServices.get(scene);
	}

	private synchronized void prepareSceneServices(Scene scene) {
		SceneServices services = getOrCreateSceneServices(scene);
		scene.configure(services);
	}

	private synchronized SceneServices getOrCreateSceneServices(Scene scene) {
		return sceneServices.computeIfAbsent(
				scene,
				ignored -> new SceneServices(this, scene, resolveRenderThread()));
	}

	private synchronized void releaseSceneServices(Scene scene) {
		SceneServices services = sceneServices.remove(scene);
		if (services != null) {
			services.close();
		}
	}

	private synchronized void prepareSceneServicesForDisposal(Scene scene) {
		SceneServices services = sceneServices.get(scene);
		if (services != null) {
			services.prepareForDispose();
		}
	}

	private synchronized void releaseAllSceneServices() {
		for (SceneServices services : sceneServices.values().toArray(new SceneServices[0])) {
			services.close();
		}
		sceneServices.clear();
	}

	private Thread resolveRenderThread() {
		return renderThread != null ? renderThread : Thread.currentThread();
	}

	/** Keeps stateful Standard renderers aligned with the authoritative SceneManager. */
	void syncCurrentSceneToRenderers() {
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
	 * Processing Callback that routes key events to built-ins, named scene actions, and the active
	 * scene's raw callback.
	 *
	 * <p>The constructor registers this method automatically; sketches must not call or register it
	 * themselves.</p>
	 *
	 * @param event the KeyEvent object containing details of the key event
	 */
	public void keyEvent(processing.event.KeyEvent event) {
		if (disposed || paused) {
			return;
		}
		if (controlManager != null) {
			controlManager.keyEvent(event);
		}
		if (controlManager == null) {
			Scene activeScene = getCurrentScene();
			if (activeScene != null) {
				dispatchSceneAction(activeScene, event);
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
			dispatchSceneAction(activeScene, event);
			activeScene.keyEvent(event);
		}
	}


	/**
	 * Processing Callback that routes mouse events to named actions, the active scene, the built-in
	 * interface, and exactly one navigation camera.
	 *
	 * <p>The constructor registers this method automatically; sketches must not call or register it
	 * themselves. A visible control under the pointer owns its gesture and suppresses navigation.</p>
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
			dispatchSceneAction(activeScene, event);
			activeScene.mouseEvent(event);
		}

		// UI gestures remain UI-only; clear any prior drag anchor before returning.
		if (controlManager != null
				&& showControlPanel
				&& controlManager.isMouseOverControls()) {
			resetCameraInputState();
			return;
		}

		// Route navigation to exactly one camera to avoid double orbit/zoom in Standard view.
		if (sceneCameraInputEnabled) {
			sceneCamera.mouseEvent(event);
		} else if (standardRenderer != null) {
			standardRenderer.getCam().mouseEvent(event);
		}
	}

	private synchronized void dispatchSceneAction(Scene scene, KeyEvent event) {
		SceneServices services = sceneServices.get(scene);
		if (services != null && !services.isClosed()) {
			services.actions().dispatch(event);
		}
	}

	private synchronized void dispatchSceneAction(Scene scene, MouseEvent event) {
		SceneServices services = sceneServices.get(scene);
		if (services != null && !services.isClosed()) {
			services.actions().dispatch(event);
		}
	}

	/**
	 * Processing Callback used by the built-in ControlP5 interface.
	 *
	 * <p>This method is public for callback integration only. It is not a scene input command and
	 * sketches must not call it directly.</p>
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

	}

	/**
	 * Returns artist-facing output control and telemetry.
	 *
	 * @return output manager, or {@code null} before {@link #setup()} completes
	 */
	public OutputManager getOutputManager() {
		return outputManager;
	}

	OutputManagerImpl outputManagerInternal() {
		return outputManager;
	}

	/**
	 * Returns the domemaster circle size.
	 *
	 * @return configured size as a percentage of the square target
	 */
	public float getFishSize() {
		return fishSize;
	}

	/**
	 * Sets the domemaster circle size.
	 *
	 * @param fishSize size percentage; values outside 0..100 are constrained
	 */
	public void setFishSize(float fishSize) {
		if (!Float.isFinite(fishSize)) {
			LOGGER.warning("Ignoring non-finite domemaster size: " + fishSize);
			return;
		}
		this.fishSize = PApplet.constrain(fishSize, 0.0f, 100.0f);
		if (fisheyeDomemaster != null) {
			fisheyeDomemaster.setSizePercentage(this.fishSize);
		}
		if (previewFisheyeDomemaster != null) {
			previewFisheyeDomemaster.setSizePercentage(fishSize);
		}
	}

	/**
	 * Returns the domemaster angular field of view.
	 *
	 * @return field of view in degrees
	 */
	public float getFov() {
		return fov;
	}

	/**
	 * Sets the domemaster angular field of view.
	 *
	 * @param fov field of view in degrees; finite values are constrained to 0..360 and non-finite
	 *            values are ignored
	 */
	public void setFov(float fov) {
		if (!Float.isFinite(fov)) {
			LOGGER.warning("Ignoring non-finite domemaster field of view: " + fov);
			return;
		}
		this.fov = PApplet.constrain(fov, 0.0f, 360.0f);
	}

	/**
	 * Returns the spherical pitch control accumulator.
	 *
	 * @return pitch in radians
	 */
	public float getPitch() {
		return sphericalOrientation.getPitch();
	}

	/**
	 * Sets spherical pitch by composing the shortest delta around the local X axis.
	 *
	 * @param pitch control value in radians; non-finite values are ignored
	 */
	public void setPitch(float pitch) {
		sphericalOrientation.setPitch(pitch);
	}

	/**
	 * Returns the spherical yaw control accumulator.
	 *
	 * @return yaw in radians
	 */
	public float getYaw() {
		return sphericalOrientation.getYaw();
	}

	/**
	 * Sets spherical yaw by composing the shortest delta around the local Z axis.
	 *
	 * @param yaw control value in radians; non-finite values are ignored
	 */
	public void setYaw(float yaw) {
		sphericalOrientation.setYaw(yaw);
	}

	/**
	 * Returns the spherical roll control accumulator.
	 *
	 * @return roll in radians
	 */
	public float getRoll() {
		return sphericalOrientation.getRoll();
	}

	/**
	 * Sets spherical roll by composing the shortest delta around the local Y axis.
	 *
	 * @param roll control value in radians; non-finite values are ignored
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
	 * Gets the configured preview view.
	 *
	 * <p>In a dedicated {@link RenderMode}, the effective representation is controlled by that
	 * mode while this value is preserved for a later return to {@link RenderMode#FULL}.</p>
	 *
	 * @return configured preview view
	 */
	public ViewType getCurrentView() {
		return currentView;
	}

	/**
	 * Sets the configured preview view.
	 *
	 * <p>The selection takes effect immediately in {@link RenderMode#FULL}. Dedicated modes keep
	 * it as the preview selection to restore when FULL is selected again.</p>
	 *
	 * @param currentView new preview view; {@code null} is ignored
	 */
	public void setCurrentView(ViewType currentView) {
		if (currentView == null) {
			LOGGER.warning("Ignoring null preview view.");
			return;
		}
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
		if (controlManager != null) {
			controlManager.refreshVisibility();
		}
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
	 * Configures an offscreen output-resolution view for performance qualification.
	 *
	 * <p>This demand is honored only while performance profiling is enabled. It renders the
	 * requested high-resolution output dependency chain without enabling, disabling, or sending
	 * NDI, Syphon, or Spout. Passing {@code null} clears the qualification demand.</p>
	 *
	 * <p>This is an experimental benchmarking hook; normal applications should route output through
	 * {@link OutputManager}.</p>
	 *
	 * <p><strong>API stability:</strong> Experimental.</p>
	 *
	 * @param view output-resolution view to render during profiling, or {@code null} to clear
	 * @since 2.0.0
	 */
	public void setPerformanceOutputDemand(ViewType view) {
		performanceOutputDemand = view;
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
		if (controlManager != null) {
			controlManager.refreshVisibility();
		}
	}

	/**
	 * Resets the built-in controls to their default state when the interface is initialized.
	 */
	public void resetControls() {
		if (controlManager != null) {
			controlManager.resetControls();
		}
	}

	/**
	 * Sets the FisheyeDomemaster instance.
	 *
	 * @param fisheyeDomemaster the new FisheyeDomemaster instance
	 */
	void setFisheyeDomemaster(FisheyeDomemaster fisheyeDomemaster) {
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
	FisheyeDomemaster getFisheyeDomemaster() {
		return fisheyeDomemaster;
	}

	/**
	 * Returns the instance of the EquirectangularRenderer.
	 * @return the EquirectangularRenderer instance.
	 */
	EquirectangularRenderer getEquirectangularRenderer() {
		return equirectangularRenderer;
	}

	/**
	 * Returns the instance of the CubemapViewRenderer.
	 * @return the CubemapViewRenderer instance.
	 */
	CubemapViewRenderer getCubemapViewRenderer() {
		return cubemapViewRenderer;
	}

	/**
	 * Returns the instance of the StandardRenderer.
	 * @return the StandardRenderer instance.
	 */
	StandardRenderer getStandardRenderer() {
		return standardRenderer;
	}

	/**
	 * Returns the native scene-space orbit camera service.
	 *
	 * <p>Scenes normally obtain this same camera from {@link SceneServices#camera()} and apply it
	 * inside {@code sceneRender} between {@code pushMatrix}/{@code popMatrix}. The
	 * camera transforms the scene modelview directly, so it works across every
	 * projection without touching the dome parameters (yaw/pitch/roll/fov). Its current
	 * rotational quaternion is synchronized with the shared Environment every frame;
	 * target and distance remain scene-only so the background stays infinite.</p>
	 *
	 * @return the shared {@link OrbitCamera} instance
	 */
	public OrbitCamera getSceneCamera() {
		return sceneCamera;
	}

	/**
	 * Reports whether the current {@link Scene#sceneRender(PGraphicsOpenGL)} call is being
	 * executed by a spherical cubemap capture pass.
	 *
	 * <p>Scenes can use this to skip viewport-only background geometry, HUDs, or helper
	 * elements that should not be baked into domemaster, equirectangular, or skybox outputs.
	 * The method does not change the {@code sceneRender(PGraphicsOpenGL)} contract; it only
	 * exposes the current renderer phase.</p>
	 *
	 * @return {@code true} while a cubemap capture pass is rendering the scene
	 * @since 2.0.0
	 */
	public boolean isSphericalCaptureActive() {
		return sphericalCaptureActive;
	}

	/**
	 * Sets an LDR equirectangular environment background for all render modes.
	 *
	 * <p>The image is rendered by the library as an infinite far-depth background after the
	 * active {@link Scene} is drawn. This keeps backgrounds out of scene geometry, survives
	 * scene-owned {@code background()} calls, and makes the same environment available to
	 * domemaster, equirectangular, skybox, and Standard projections. In Standard view the
	 * environment follows camera rotation but remains invariant under camera translation.
	 * Passing {@code null} clears the environment.</p>
	 *
	 * @param image equirectangular Processing image, or {@code null} to clear
	 */
	public void setEquirectangularBackground(PImage image) {
		environmentState.setLdrEquirectangularSource(image);
	}

	/**
	 * Loads and sets an LDR equirectangular environment background from the sketch data path.
	 *
	 * <p>This is a synchronous Processing asset load. Use it during sketch setup, not inside a
	 * recurring frame callback or background task.</p>
	 *
	 * @param imagePath Processing data path or absolute image path; null or blank clears the source
	 */
	public void setEquirectangularBackground(String imagePath) {
		if (imagePath == null || imagePath.isBlank()) {
			clearEnvironmentBackground();
			return;
		}
		PImage image = p.loadImage(imagePath);
		if (image == null) {
			LOGGER.warning("Could not load equirectangular environment background: " + imagePath);
			return;
		}
		setEquirectangularBackground(image);
	}

	/** Clears the configured environment background. */
	public void clearEnvironmentBackground() {
		setEquirectangularBackground((PImage) null);
	}

	/** Returns the borrowed source for scene-scoped ownership bookkeeping. */
	PImage getEnvironmentBackgroundSource() {
		return environmentState.getLdrEquirectangularSource();
	}

	/**
	 * Reports whether an environment background image is currently configured.
	 *
	 * @return {@code true} when an image is configured
	 */
	public boolean hasEnvironmentBackground() {
		return environmentState.hasSource();
	}

	/**
	 * Shows or hides the configured environment background without releasing the image.
	 *
	 * @param visible {@code true} to draw the environment background
	 */
	public void setEnvironmentBackgroundVisible(boolean visible) {
		environmentState.setVisible(visible);
	}

	/**
	 * Reports whether the configured environment background is visible.
	 *
	 * @return {@code true} when visible
	 */
	public boolean isEnvironmentBackgroundVisible() {
		return environmentState.isVisible();
	}

	/**
	 * Sets the colour multiplier applied to the LDR environment background.
	 *
	 * @param intensity colour multiplier; finite negative values are clamped to zero and non-finite
	 *                  values are ignored
	 */
	public void setEnvironmentBackgroundIntensity(float intensity) {
		environmentState.setIntensity(intensity);
	}

	/**
	 * Returns the current environment background colour multiplier.
	 *
	 * @return non-negative colour multiplier
	 */
	public float getEnvironmentBackgroundIntensity() {
		return environmentState.getIntensity();
	}

	/**
	 * Rotates the equirectangular environment lookup around the vertical axis.
	 *
	 * @param yawOffset radians added to the source longitude lookup; non-finite values are ignored
	 */
	public void setEnvironmentBackgroundYawOffset(float yawOffset) {
		environmentState.setYawOffset(yawOffset);
	}

	/**
	 * Returns the current equirectangular environment yaw offset.
	 *
	 * @return yaw offset in radians
	 */
	public float getEnvironmentBackgroundYawOffset() {
		return environmentState.getYawOffset();
	}

	/**
	 * Enables or disables built-in mouse handling for the scene camera.
	 * When enabled, the library forwards mouse drag/wheel events to
	 * {@link #getSceneCamera()} automatically and suspends the independent Standard-view
	 * camera input, preventing the same gesture from rotating two cameras at once.
	 *
	 * @param enabled true to let the library drive the scene camera from mouse input
	 */
	public void setSceneCameraInputEnabled(boolean enabled) {
		if (sceneCameraInputEnabled != enabled) {
			resetCameraInputState();
		}
		this.sceneCameraInputEnabled = enabled;
	}

	/** Clears transient drag anchors for whichever camera owns navigation input. */
	private void resetCameraInputState() {
		sceneCamera.resetInputState();
		if (standardRenderer != null) {
			standardRenderer.getCam().resetInputState();
		}
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
	 * Returns the Processing sketch that owns this facade.
	 *
	 * <p>Scenes should prefer {@link SceneServices#applet()} so ownership remains explicit for the
	 * current activation.</p>
	 *
	 * @return non-null owning Processing sketch
	 */
	public PApplet getPApplet() {

		return p;
	}

	EnvironmentState getEnvironmentState() {
		return environmentState;
	}

	/**
	 * Reports whether setup and lazy OpenGL manager initialization completed successfully.
	 *
	 * @return {@code true} when the facade can render frames
	 */
	public boolean isInitialized() {
		return initState == InitState.MANAGERS_READY;
	}

	/**
	 * Enables experimental performance collection using the default ring-buffer capacity.
	 *
	 * <p>{@link PerformanceMode#CPU_GPU} adds one capability-gated asynchronous GPU timestamp
	 * interval around the complete render pipeline. Unsupported contexts fall back to CPU, and
	 * no synchronous query or {@code glFinish()} is introduced.</p>
	 *
	 * <p><strong>API stability:</strong> Experimental.</p>
	 *
	 * @param mode non-null requested profiling mode
	 * @throws IllegalArgumentException when {@code mode} is null
	 * @since 2.0.0
	 */
	public void enablePerformanceProfiling(PerformanceMode mode) {
		enablePerformanceProfiling(mode, PerformanceMonitor.DEFAULT_CAPACITY);
	}

	/**
	 * Enables experimental performance collection with preallocated sample storage.
	 * Calling this method resets previously collected samples.
	 *
	 * <p><strong>API stability:</strong> Experimental.</p>
	 *
	 * @param mode non-null requested profiling mode
	 * @param sampleCapacity number of completed frames retained in the ring buffer, from 2 to 100000
	 * @throws IllegalArgumentException when an argument is invalid
	 * @since 2.0.0
	 */
	public void enablePerformanceProfiling(PerformanceMode mode, int sampleCapacity) {
		enablePerformanceProfiling(mode, sampleCapacity, GpuTimerPolicy.SAFE);
	}

	/**
	 * Enables profiling with an explicit GPU timer ownership/fallback policy.
	 *
	 * <p>{@link GpuTimerPolicy#ARCHITECTURE_AWARE} is intended for controlled benchmark
	 * scenes. On Apple Silicon it may own {@code GL_TIME_ELAPSED} for the complete pipeline,
	 * so scene code must not start another elapsed timer query during the interval.</p>
	 *
	 * <p><strong>API stability:</strong> Experimental.</p>
	 *
	 * @param mode non-null requested profiling mode
	 * @param sampleCapacity number of completed frames retained in the ring buffer, from 2 to 100000
	 * @param timerPolicy non-null GPU timer selection policy
	 * @throws IllegalArgumentException when an argument is invalid
	 * @since 2.0.0
	 */
	public void enablePerformanceProfiling(
			PerformanceMode mode,
			int sampleCapacity,
			GpuTimerPolicy timerPolicy) {
		performanceMonitor.enable(mode, sampleCapacity, timerPolicy);
		gpuPerformanceTimer.stop(isOnRenderThread());
		if (mode == PerformanceMode.CPU_GPU) {
			LOGGER.info("CPU_GPU profiling requested with " + timerPolicy
					+ "; GPU support will be checked on the render thread.");
		}
	}

	/**
	 * Disables collection without discarding completed samples.
	 *
	 * <p><strong>API stability:</strong> Experimental.</p>
	 *
	 * @since 2.0.0
	 */
	public void disablePerformanceProfiling() {
		gpuPerformanceTimer.stop(isOnRenderThread());
		performanceMonitor.disable();
		performanceOutputDemand = null;
	}

	/**
	 * Clears all performance samples and invariant counters.
	 *
	 * <p><strong>API stability:</strong> Experimental.</p>
	 *
	 * @since 2.0.0
	 */
	public void resetPerformanceStatistics() {
		gpuPerformanceTimer.stop(isOnRenderThread());
		performanceMonitor.reset();
	}

	/**
	 * Creates an immutable performance snapshot. Call outside a measured interval because
	 * snapshot aggregation intentionally allocates and sorts copies of retained samples.
	 *
	 * <p><strong>API stability:</strong> Experimental.</p>
	 *
	 * @return immutable snapshot of completed frames
	 * @since 2.0.0
	 */
	public PerformanceSnapshot getPerformanceSnapshot() {
		return performanceMonitor.snapshot();
	}

	/**
	 * Returns a read-only diagnostic report for the active Processing graphics renderer.
	 *
	 * <p>Call on the Processing/OpenGL thread after the renderer is initialized.</p>
	 *
	 * <p><strong>API stability:</strong> Experimental.</p>
	 *
	 * @return current graphics capability report
	 * @since 2.0.0
	 */
	public GraphicsCapabilities getGraphicsCapabilities() {
		return ProcessingGlAdapter.getDefault().queryCapabilities(p);
	}

	PerformanceMonitor performanceMonitor() {
		return performanceMonitor;
	}

	boolean beginGpuPerformanceInterval() {
		return gpuPerformanceTimer.begin();
	}

	void endGpuPerformanceInterval() {
		gpuPerformanceTimer.end();
	}

	private boolean isOnRenderThread() {
		return renderThread != null && Thread.currentThread() == renderThread;
	}

	/**
	 * Returns the current initialization state.
	 *
	 * @return the current InitState
	 */
	InitState getInitState() {
		return initState;
	}

	/**
	 * Sets the SceneManager instance for managing multiple scenes.
	 *
	 * <p>A detached manager selects, but does not set up, its first registered scene. Attaching it
	 * here begins that activation with facade-owned services. Replacing the manager disposes the
	 * previously owned activation first.</p>
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

		if (this.sceneManager != null) {
			this.sceneManager.clearScenes();
		}
		this.sceneManager = sceneManager;
		this.sceneManager.setLifecycleListener(sceneLifecycle);
		bootstrapScene = null;
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
	 * Processing Callback that establishes the authoritative frame boundary.
	 *
	 * <p>It drains activation queues and bounded external input, ticks the frame clock, processes a
	 * deferred reload or calls {@link Scene#update()}, refreshes tracked camera targets, and advances
	 * camera interpolation once. The constructor registers it automatically; sketches must not call
	 * it directly.</p>
	 */
	public void pre() {
		if (disposed || paused) {
			return;
		}
		if (initState != InitState.MANAGERS_READY) {
			LOGGER.warning("Render skipped: System not ready. State: " + initState);
			return;
		}
		performanceMonitor.beginFrame();

		// Processing can execute setup() and JOGL frame callbacks on different threads.
		// pre() is the authoritative Processing/OpenGL frame boundary.
		renderThread = Thread.currentThread();
		gpuPerformanceTimer.maintain();
		syncCurrentSceneToRenderers();
		Scene activeScene = getCurrentScene();
		if (activeScene != null && sceneManager.isCurrentSceneActive()) {
			SceneServices services = getOrCreateSceneServices(activeScene);
			boolean activationStillCurrent = services.beginFrame()
					&& activeScene == getCurrentScene()
					&& sceneManager.isCurrentSceneActive();
			if (!activationStillCurrent) {
				syncCurrentSceneToRenderers();
			} else if (services.consumeReloadRequest()) {
				sceneManager.reloadCurrentScene();
				syncCurrentSceneToRenderers();
			} else {
				boolean profiling = performanceMonitor.isEnabled();
				long sceneStarted = profiling ? performanceMonitor.start() : 0L;
				try {
					activeScene.update();
				} finally {
					if (profiling) performanceMonitor.record(PerformanceMetric.SCENE_UPDATE, sceneStarted);
				}
				services.endFrame();
			}
		}

		// Advance the native scene camera smoothing once per frame.
		sceneCamera.update();
		environmentState.setSceneCameraOrientation(sceneCamera.getOrientation());
	}

	/**
	 * Processing Callback that performs lazy renderer initialization after {@link #setup()}.
	 *
	 * <p>The constructor registers it automatically and the facade unregisters it after successful
	 * initialization. Sketches must not call it directly.</p>
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
	 * Processing Callback that stops the instance permanently and releases all owned resources.
	 *
	 * <p>This terminal operation delegates to {@link #dispose()} and is idempotent. Processing calls
	 * it automatically; applications normally do not.</p>
	 */
	public void stop() {
		dispose();
	}

	/**
	 * Processing Callback that resumes activation input and outputs remembered by {@link #pause()}.
	 *
	 * <p>The constructor registers it automatically; sketches must not call it directly.</p>
	 */
	public void resume() {
		if (disposed || !paused) {
			return;
		}
		LOGGER.info("Resuming processes...");
		if (outputManager != null) {
			if (resumeNdiOutput && !outputManager.isNdiEnabled()) {
				outputManager.setOutputEnabled(OutputManager.OutputType.NDI, true);
			}
			if (resumeSpoutOutput && !outputManager.isSpoutEnabled()) {
				outputManager.setOutputEnabled(OutputManager.OutputType.SPOUT, true);
			}
			if (resumeSyphonOutput && !outputManager.isSyphonEnabled()) {
				outputManager.setOutputEnabled(OutputManager.OutputType.SYPHON, true);
			}
		}
		Scene activeScene = getCurrentScene();
		SceneServices services = activeScene != null ? getSceneServices(activeScene) : null;
		if (services != null && !services.isClosed()) {
			services.resume();
		}
		clearPausedOutputState();
		LOGGER.info("Processes resumed.");
	}

	/**
	 * Processing Callback that pauses frame work, drops queued external input, clears pointer
	 * anchors, and disables outputs while remembering their enabled state.
	 *
	 * <p>The constructor registers it automatically; sketches must not call it directly.</p>
	 */
	public void pause() {
		if (disposed || paused) {
			return;
		}
		LOGGER.info("Pausing processes...");
		resetCameraInputState();
		Scene activeScene = getCurrentScene();
		SceneServices services = activeScene != null ? getSceneServices(activeScene) : null;
		if (services != null && !services.isClosed()) {
			services.pause();
		}
		paused = true;
		if (outputManager != null) {
			resumeNdiOutput = outputManager.isNdiEnabled();
			resumeSpoutOutput = outputManager.isSpoutEnabled();
			resumeSyphonOutput = outputManager.isSyphonEnabled();
			outputManager.disableAll();
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
	 * Processing Callback that performs terminal, idempotent cleanup.
	 *
	 * <p>Processing calls this method automatically. It may also be invoked once by an application
	 * that intentionally ends the facade early; no facade operation may reactivate it afterward.</p>
	 */
	public void dispose() {
		if (disposed) {
			unregisterEventHandlers();
			return;
		}
		disposed = true;
		LOGGER.info("Disposing resources...");
		resetCameraInputState();
		clearPausedOutputState();
		gpuPerformanceTimer.dispose(isOnRenderThread());

		if (outputManager != null) {
			try {
				outputManager.shutdownOutputsTerminal();
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
		releaseAllSceneServices();
		bootstrapScene = null;
		try {
			releaseGraphicsResources();
		} catch (RuntimeException | LinkageError error) {
			LOGGER.warning("Renderer disposal failed: " + error.getMessage());
		}
		environmentState.clearSource();
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
