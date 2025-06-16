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
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * The `zividomelive` class manages rendering and control of a live dome visualization.
 * It integrates with Processing and `OutputManager` for dome rendering.
 *
 * <p>This class handles setup, initialization, and rendering of fisheye domemaster,
 * equirectangular, cubemap, and standard views. It also manages the control panel and mouse events for interaction.</p>
 *
 * <p>It provides methods to set up the rendering environment, initialize various managers,
 * handle mouse and key events, and render different views. The class also supports toggling
 * output methods (NDI, Spout, Syphon) and managing the current scene and view type.</p>
 */
public class zividomelive implements PConstants {

	private final PApplet p;
	private boolean initialized = false;
	private Scene currentScene;

	private float pitch = 0.0f, yaw = 0.0f, roll = 0.0f, fov = 210.0f, fishSize = 100.0f;
	private int resolution = 1024;
	private boolean showControlPanel = true;
	private boolean showPreview = false;
	private final boolean enableOutput = false;
	private boolean controlPanelShownOnce = false;

	private ControlManager controlManager;
        private PGLCubemapRenderer cubemapRenderer;
	private EquirectangularRenderer equirectangularRenderer;
	private StandardRenderer standardRenderer;
	private FisheyeDomemaster fisheyeDomemaster;
	private CameraManager cameraManager;
	private CubemapViewRenderer cubemapViewRenderer;
	private OutputManager outputManager;
	private SplashScreen splash;
	private SceneManager sceneManager;
	private final ExecutorService executorService = ThreadManager.getExecutor();

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

	private ViewType currentView = ViewType.FISHEYE_DOMEMASTER;
	private boolean pendingReset = false;
	private int pendingResolution = resolution;
	private static final Logger LOGGER = LogManager.getLogger();


	/**
	 * Constructs a new `zividomelive` instance with the specified PApplet.
	 *
	 * @param p the PApplet instance used for rendering
	 * @throws IllegalArgumentException if the PApplet instance is null
	 */
	public zividomelive(PApplet p) {
		if (p == null) {
			throw new IllegalArgumentException("PApplet instance cannot be null.");
		}
		this.p = p;

		welcome();
		registerEventHandlers();
		this.sceneManager = new SceneManager();
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
	 * @param scene the Scene instance to be set
	 */
	public void setScene(Scene scene) {
		if (scene == null) {
			LOGGER.warning("Cannot set a null scene.");
			return;
		}
		sceneManager.registerScene(scene);
		currentScene = sceneManager.getCurrentScene();
		if (currentScene != null) {
			currentScene.setupScene();
		}
	}

	/**
	 * Sets up the rendering environment, including frame rate, OpenGL info, texture hints,
	 * output configuration, and mouse event registration.
	 *
	 * @throws IllegalStateException if the PApplet instance is not properly configured
	 */
	public void setup() {
		if (p == null) {
			throw new IllegalStateException("PApplet instance is not properly configured.");
		}

		LOGGER.info("Starting setup...");

		try {
			p.frameRate(70);
			LOGGER.info("Frame rate set to 70.");
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

		// Load DefaultScene if no scene is set
		if (currentScene == null) {
			try {
				currentScene = new com.victorvalentim.zividomelive.scene.DefaultScene(p);
				currentScene.setupScene();
				LOGGER.info("DefaultScene loaded successfully as the initial scene.");
			} catch (Exception e) {
				LOGGER.severe("Error initializing DefaultScene: " + e.getMessage());
			}
		}
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

			System.out.println("OpenGL Version: " + pglContext.getString(PGL.VERSION));
			System.out.println("OpenGL Vendor: " + pglContext.getString(PGL.VENDOR));
			System.out.println("OpenGL Renderer: " + pglContext.getString(PGL.RENDERER));

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
	 * Initializes various managers required for rendering and control.
	 */
	public void initializeManagers() {
		try {
			LOGGER.info("Initializing managers...");

			CompletableFuture<Void> cameraManagerFuture = CompletableFuture.runAsync(() -> {
				cameraManager = new CameraManager();
				LOGGER.info("CameraManager initialized.");
			}, ThreadManager.getExecutor());

			CompletableFuture<Void> renderersFuture = CompletableFuture.runAsync(this::initializeRenderers, ThreadManager.getExecutor());

			CompletableFuture<Void> controlManagerFuture = CompletableFuture.runAsync(() -> {
				controlManager = new ControlManager(p, this, resolution);
				LOGGER.info("ControlManager initialized.");
			}, ThreadManager.getExecutor());

			CompletableFuture.allOf(cameraManagerFuture, renderersFuture, controlManagerFuture).join();
			LOGGER.info("Managers initialized successfully.");
		} catch (Exception e) {
			LOGGER.severe("Error initializing managers");
		}
	}

	/**
	 * Initializes various renderers required for different views.
	 */
	void initializeRenderers() {
		try {
			LOGGER.info("Initializing renderers...");

			// Paths to shader files
			String equirectangularVertexShaderPath = "data/shaders/equirectangular.vert";
                        String equirectangularFragmentShaderPath = "data/shaders/equirectangular_cube.frag";
			String domemasterVertexShaderPath = "data/shaders/domemaster.vert";
			String domemasterFragmentShaderPath = "data/shaders/domemaster.frag";

			// Load shaders asynchronously
			CompletableFuture<PShader> equirectangularShaderFuture = CompletableFuture.supplyAsync(() -> p.loadShader(equirectangularFragmentShaderPath, equirectangularVertexShaderPath), ThreadManager.getExecutor());
			CompletableFuture<PShader> domemasterShaderFuture = CompletableFuture.supplyAsync(() -> p.loadShader(domemasterFragmentShaderPath, domemasterVertexShaderPath), ThreadManager.getExecutor());

			// Initialize renderers asynchronously
                        CompletableFuture<Void> cubemapRendererFuture = CompletableFuture.runAsync(() -> {
                                cubemapRenderer = new PGLCubemapRenderer(p, resolution);
                                LOGGER.info("CubemapRenderer initialized.");
                        });

			CompletableFuture<Void> equirectangularRendererFuture = CompletableFuture.runAsync(() -> {
				equirectangularRenderer = new EquirectangularRenderer(resolution, equirectangularFragmentShaderPath, equirectangularVertexShaderPath, p);
				LOGGER.info("EquirectangularRenderer initialized.");
			});

			CompletableFuture<Void> standardRendererFuture = CompletableFuture.runAsync(() -> {
				standardRenderer = new StandardRenderer(p, p.width, p.height, currentScene);
				LOGGER.info("StandardRenderer initialized.");
			});

			CompletableFuture<Void> fisheyeDomemasterFuture = CompletableFuture.runAsync(() -> {
				fisheyeDomemaster = new FisheyeDomemaster(resolution, domemasterFragmentShaderPath, domemasterVertexShaderPath, p);
				LOGGER.info("FisheyeDomemaster initialized.");
			});

			CompletableFuture<Void> cubemapViewRendererFuture = CompletableFuture.runAsync(() -> {
				cubemapViewRenderer = new CubemapViewRenderer(p, resolution);
				LOGGER.info("CubemapViewRenderer initialized.");
			});

			// Wait for all renderers to be initialized
			CompletableFuture.allOf(
					cubemapRendererFuture,
					equirectangularRendererFuture,
					standardRendererFuture,
					fisheyeDomemasterFuture,
					cubemapViewRendererFuture
			).join();

			LOGGER.info("Renderers initialized successfully.");
		} catch (Exception e) {
			LOGGER.severe("Error initializing renderers");
		}
	}

	/**
	 * Main draw method that handles rendering and updating the view.
	 */
	public void draw() {
		if (!initialized) {
			p.background(0);
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
			splash = null; // Libera a splash após o fade-out

			// Exibe o painel de controle apenas uma vez
			if (!controlPanelShownOnce) {
				showControlPanel = true;
				controlPanelShownOnce = true; // Define a flag para evitar reaparecimento
			}
		}
	}

	void renderContent() {
		// Verifica se os renderizadores e a cena foram inicializados
		if (cubemapRenderer == null || equirectangularRenderer == null || fisheyeDomemaster == null || standardRenderer == null || currentScene == null) {
			LOGGER.severe("Error: Renderer or scene not initialized.");
			return;
		}

		clearBackground();         // Limpa o fundo
		handleGraphicsReset();     // Garante que o reset gráfico seja realizado, se necessário
		captureCubemap();          // Captura o cubemap para a cena atual
		renderView();              // Renderiza a visualização principal
		outputManager.sendOutput(); // Envia a saída para os métodos de saída ativados

		if (showPreview) {
			drawFloatingPreview(); // Desenha uma visualização flutuante, se ativada
		}
		drawControlPanel();        // Exibe o painel de controle
	}

	void individualRenderer() {
		if (!initialized) {
			LOGGER.severe("Error: System not fully initialized.");
			return;
		}

		clearBackground();
		handleGraphicsReset(); // Garante que o reset gráfico seja realizado
		captureCubemap();
		outputManager.sendOutput(); // Envia a saída para os métodos de saída ativados
		drawControlPanel();         // Exibe o painel de controle
	}

	void clearBackground() {
		p.background(0, 0, 0, 0);
	}

	/**
	 * Handles resetting the graphics if a reset is pending.
	 */
	private void handleGraphicsReset() {
		if (pendingReset) {
			LOGGER.info("Pending reset detected. Changing resolution to: " + pendingResolution);
			releaseGraphicsResources(); // Libera os recursos gráficos antigos
			resolution = pendingResolution;
			initializeRenderers(); // Inicializa novos recursos gráficos
			pendingReset = false;
			LOGGER.info("Graphics reset completed.");
		}
	}

	/**
	 * Captures the cubemap for the current scene.
	 */
	private void captureCubemap() {
		if (cubemapRenderer != null) {
			cubemapRenderer.captureCubemap(getPitch(), getYaw(), getRoll(), cameraManager, currentScene);
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
	 * Updates the render views based on the current view type.
	 */
	private void updateRenderViews() {
                equirectangularRenderer.render(cubemapRenderer.getCubemapTexture());
		fisheyeDomemaster.applyShader(equirectangularRenderer.getEquirectangular(), getFov());

		switch (getCurrentView()) {
			case CUBEMAP:
                                cubemapViewRenderer.drawCubemapToGraphics(cubemapRenderer.getCubemapTexture());
				break;
			case STANDARD:
				standardRenderer.render();
				break;
		}
	}

	/**
	 * Displays the current view based on the view type.
	 */
	private void displayCurrentView() {
		switch (getCurrentView()) {
			case CUBEMAP:
				displayView(cubemapViewRenderer.getCubemap());
				break;
			case EQUIRECTANGULAR:
				displayView(equirectangularRenderer.getEquirectangular());
				break;
			case FISHEYE_DOMEMASTER:
				displayView(fisheyeDomemaster.getDomemasterGraphics());
				break;
			case STANDARD:
				displayView(standardRenderer.getStandardView());
				break;
		}
	}

	/**
	 * Renders the view by updating and displaying the current view.
	 */
	private void renderView() {
		updateRenderViews();
		displayCurrentView();
	}

	/**
	 * Renders the fisheye domemaster view by applying the shader and displaying the view.
	 * If the FisheyeDomemaster is not initialized, an error message is printed.
	 */
	public void renderFisheyeDomemaster() {
		if (fisheyeDomemaster != null) {
			individualRenderer();
                        equirectangularRenderer.render(cubemapRenderer.getCubemapTexture());
			fisheyeDomemaster.applyShader(equirectangularRenderer.getEquirectangular(), getFov());
			displayView(fisheyeDomemaster.getDomemasterGraphics());
		} else {
			LOGGER.severe("Error: FisheyeDomemaster not initialized.");
		}
	}

	/**
	 * Renders the equirectangular view by invoking the EquirectangularRenderer.
	 * If the EquirectangularRenderer is not initialized, an error message is printed.
	 */
	public void renderEquirectangular() {
		if (equirectangularRenderer != null) {
			individualRenderer();
                        equirectangularRenderer.render(cubemapRenderer.getCubemapTexture());
			displayView(equirectangularRenderer.getEquirectangular());
		} else {
			LOGGER.severe("Error: EquirectangularRenderer not initialized.");
		}
	}

	/**
	 * Renders the cubemap view by drawing the cubemap faces to the graphics and displaying the view.
	 * If the CubemapViewRenderer is not initialized, an error message is printed.
	 */
	public void renderCubemap() {
		if (cubemapViewRenderer != null) {
			individualRenderer();
                        cubemapViewRenderer.drawCubemapToGraphics(cubemapRenderer.getCubemapTexture());
			displayView(cubemapViewRenderer.getCubemap());
		} else {
			LOGGER.severe("Error: CubemapViewRenderer not initialized.");
		}
	}

	/**
	 * Renders the standard view by invoking the StandardRenderer.
	 * If the StandardRenderer is not initialized, an error message is printed.
	 */
	public void renderStandard() {
		if (standardRenderer != null) {
			individualRenderer();
			standardRenderer.render();
			displayView(standardRenderer.getStandardView());
		} else {
			LOGGER.severe("Error: StandardRenderer not initialized.");
		}
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
	 * Draws a floating preview of the fisheye domemaster view.
	 */
	public void drawFloatingPreview() {
		float previewWidth = 200f;
		float previewHeight = 200f;
		float x = p.width - previewWidth;
		float y = p.height - previewHeight;

		PGraphicsOpenGL previewGraphics = fisheyeDomemaster.getDomemasterGraphics();
		p.image(previewGraphics, x, y, previewWidth, previewHeight);
	}

	/**
	 * Disposes of the resources used by the instance.
	 */
	private void releaseGraphicsResources() {
		if (cubemapRenderer != null) {
			cubemapRenderer.dispose();
			cubemapRenderer = null;
		}
		if (equirectangularRenderer != null) {
			equirectangularRenderer.dispose();
			equirectangularRenderer = null;
		}
		if (standardRenderer != null) {
			standardRenderer.dispose();
			standardRenderer = null;
		}
		if (fisheyeDomemaster != null) {
			fisheyeDomemaster.dispose();
			fisheyeDomemaster = null;
		}
		if (cubemapViewRenderer != null) {
			cubemapViewRenderer.dispose();
			cubemapViewRenderer = null;
		}
	}

	/**
	 * Resets the graphics with a new resolution.
	 *
	 * @param newResolution the new resolution to be set
	 */
	public void resetGraphics(int newResolution) {
		pendingReset = true;
		pendingResolution = newResolution;
		LOGGER.info("Changing resolution to: " + newResolution); // Log the new resolution
		ThreadManager.getExecutor().submit(() -> {
			try {
				// Simulate some delay or processing if needed
				Thread.sleep(100);
				LOGGER.info("Resolution change processed.");
			} catch (InterruptedException e) {
				LOGGER.severe("Error during resolution change processing");
				Thread.currentThread().interrupt();
			}
		});
	}

	/**
	 * Sets the current scene to be rendered and updates all relevant components.
	 * @param newScene the new scene to be set as the current scene
	 */
	public void setCurrentScene(Scene newScene) {
		this.currentScene = newScene;
		this.setScene(newScene); // Update the scene in the parent PApplet
		standardRenderer.setCurrentScene(newScene); // Update the scene in StandardRenderer
	}

	/**
	 * Registers event handlers with the Processing sketch and verifies success for each method.
	 */
	private void registerEventHandlers() {
		boolean allSuccess = true;

		if (registerMethod("pre")) allSuccess = false;
		if (registerMethod("draw")) allSuccess = false;
		if (registerMethod("post")) allSuccess = false;
		if (registerMethod("mouseEvent")) allSuccess = false;
		if (registerMethod("keyEvent")) allSuccess = false;
		if (registerMethod("stop")) allSuccess = false;
		if (registerMethod("resume")) allSuccess = false;
		if (registerMethod("pause")) allSuccess = false;
		if (registerMethod("dispose")) allSuccess = false;

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
			LOGGER.info("Successfully registered method: " + methodName);
			return false;
		} catch (Exception e) {
			LOGGER.severe("Failed to register method: " + methodName + ". Error: " + e.getMessage());
			return true;
		}
	}

	/**
	 * Handles key events for interaction.
	 * This method must be registered using p.registerMethod("keyEvent", this).
	 *
	 * @param event the KeyEvent object containing details of the key event
	 */
	public void keyEvent(processing.event.KeyEvent event) {
		if (event.getAction() == KeyEvent.PRESS) { // Apenas trata eventos de tecla pressionada
			if (!controlManager.isNumberboxActive()) {
				// Primeiro, processa as teclas padrão
				switch (event.getKey()) {
					case 'h':
						showControlPanel = !showControlPanel;
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
							currentScene = sceneManager.getCurrentScene();
							if (currentScene != null) {
								currentScene.setupScene();
								LOGGER.info("Switched to the previous scene: " + currentScene.getName());
							} else {
								LOGGER.warning("No previous scene available.");
							}
						}
						break;

					case PConstants.RIGHT:
						if (sceneManager != null) {
							sceneManager.nextScene();
							currentScene = sceneManager.getCurrentScene();
							if (currentScene != null) {
								currentScene.setupScene();
								LOGGER.info("Switched to the next scene: " + currentScene.getName());
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
		if (currentScene != null) {
			currentScene.keyEvent(event);
		}
	}


	/**
	 * Handles mouse events for interaction.
	 * This method must be registered using p.registerMethod("mouseEvent", this).
	 *
	 * @param event the MouseEvent object containing details of the mouse event
	 */
	public void mouseEvent(MouseEvent event) {
		int x = event.getX();
		int y = event.getY();

		if (splash != null && event.getAction() == MouseEvent.PRESS) {
			splash.mousePressed();
		}

		if (currentScene != null) {
			currentScene.mouseEvent(event);
		}

		if (standardRenderer != null) {
			standardRenderer.getCam().mouseEvent(event);
		}
	}

	/**
	 * Handles control events from the ControlP5 library.
	 * This method must be registered using p.registerMethod("controlEvent", this).
	 *
	 * @param theEvent the ControlEvent object containing details of the control event
	 */
	public void controlEvent(ControlEvent theEvent) {
		if (controlManager != null) {
			controlManager.handleEvent(theEvent);
		}

		// Forward the event to the current scene, if one exists
		if (currentScene != null) {
			currentScene.controlEvent(theEvent);
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
		return pitch;
	}

	/**
	 * Sets the pitch.
	 *
	 * @param pitch the new pitch
	 */
	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	/**
	 * Gets the current yaw.
	 *
	 * @return the current yaw
	 */
	public float getYaw() {
		return yaw;
	}

	/**
	 * Sets the yaw.
	 *
	 * @param yaw the new yaw
	 */
	public void setYaw(float yaw) {
		this.yaw = yaw;
	}

	/**
	 * Gets the current roll.
	 *
	 * @return the current roll
	 */
	public float getRoll() {
		return roll;
	}

	/**
	 * Sets the roll.
	 *
	 * @param roll the new roll
	 */
	public void setRoll(float roll) {
		this.roll = roll;
	}

	/**
	 * Gets the current view type.
	 *
	 * @return the current view type
	 */
	public ViewType getCurrentView() {
		return currentView;
	}

	/**
	 * Sets the current view type.
	 *
	 * @param currentView the new view type
	 */
	public void setCurrentView(ViewType currentView) {
		this.currentView = currentView;
	}

	/**
	 * Checks if output is enabled.
	 *
	 * @return true if output is enabled, false otherwise
	 */
	public boolean isEnableOutput() {
		return outputManager.isNdiEnabled() || outputManager.isSpoutEnabled() || outputManager.isSyphonEnabled();
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
	 * Checks if the instance is initialized.
	 *
	 * @return true if the instance is initialized, false otherwise
	 */
	public boolean isInitialized() {

		return initialized;
	}

	/**
	 * Sets the SceneManager instance for managing multiple scenes.
	 * Initializes the first scene as the current scene.
	 *
	 * @param sceneManager the SceneManager instance to manage scenes
	 */
	public void setSceneManager(SceneManager sceneManager) {
		if (sceneManager == null || sceneManager.getSceneCount() == 0) {
			LOGGER.severe("SceneManager is null or contains no scenes.");
			return;
		}
		this.sceneManager = sceneManager;
		setScene(sceneManager.getCurrentScene());
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
		// Garantir que os managers estejam inicializados antes de renderizar
		if (!initialized) {
			LOGGER.warning("Render skipped: Managers not initialized.");
			return;
		}

		// Atualizar qualquer estado global ou cenas antes do desenho
		if (currentScene != null) {
			currentScene.update(); // Atualiza o estado da cena atual
		}
	}

	/**
	 * Post-initialization method to set up managers after the initial setup.
	 */
	public void post() {
		if (!initialized) {
			initializeManagers();
			initialized = true;
			p.unregisterMethod("post", this);
		}
	}

	/**
	 * Stops all processes and releases resources.
	 */
	public void stop() {
		LOGGER.info("Stopping all processes...");
		if (outputManager != null) {
			outputManager.shutdownOutputs();
		}
		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdownNow();
		}
		LOGGER.info("All processes stopped.");
	}

	/**
	 * Resumes processes after a pause.
	 */
	public void resume() {
		LOGGER.info("Resuming processes...");
		if (outputManager != null && !outputManager.isActive()) {
			outputManager.sendOutput();
		}
		LOGGER.info("Processes resumed.");
	}

	/**
	 * Pauses all processes.
	 */
	public void pause() {
		LOGGER.info("Pausing processes...");
		if (outputManager != null) {
			outputManager.stopOutput();
		}
		LOGGER.info("Processes paused.");
	}

	/**
	 * Releases resources and cleans up before the application exits.
	 */
	public void dispose() {
		LOGGER.info("Disposing resources...");

		releaseGraphicsResources();

		// Libera managers
		if (outputManager != null) {
			outputManager.shutdownOutputs();
			outputManager = null;
		}
		if (controlManager != null) {
			controlManager.dispose();
			controlManager = null;
		}
		if (cameraManager != null) {
			cameraManager.dispose();
			cameraManager = null;
		}

		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdownNow();
		}
		LOGGER.info("Resources disposed successfully.");
	}
}
