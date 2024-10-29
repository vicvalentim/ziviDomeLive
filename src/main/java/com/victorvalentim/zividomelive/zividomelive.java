package com.victorvalentim.zividomelive;

import processing.core.*;
import processing.event.*;
import processing.opengl.*;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;
import java.util.concurrent.*;


/**
 * The `zividomelive` class is responsible for managing the rendering and control of a live dome visualization.
 * It integrates with Processing, Syphon, and Spout to provide a comprehensive solution for dome rendering.
 *
 * <p>This class handles the setup, initialization, and rendering of various views including fisheye domemaster,
 * equirectangular, cubemap, and standard views. It also manages the control panel and mouse events for interaction.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * PApplet p = new PApplet();
 * zividomelive domeLive = new zividomelive(p);
 * domeLive.setup();
 * domeLive.draw();
 * }
 * </pre>
 *
 * <p>Note: Ensure that the PApplet instance is properly configured before initializing this class.</p>
 *
 * @see PApplet
 * @see SyphonServer
 * @see Spout
 */
public class zividomelive {

	private final PApplet p;
	private boolean initialized = false;
	private Scene currentScene;

	private float pitch = 0.0f, yaw = 0.0f, roll = 0.0f, fov = 210.0f, fishSize = 100.0f;
	private int resolution = 1024;
	private boolean showControlPanel = true;
	private boolean showPreview = false;
	private boolean enableOutput = false;
	private boolean controlPanelShownOnce = false;

	private ControlManager controlManager;
	private CubemapRenderer cubemapRenderer;
	private EquirectangularRenderer equirectangularRenderer;
	private StandardRenderer standardRenderer;
	private FisheyeDomemaster fisheyeDomemaster;
	private CameraManager cameraManager;
	private CubemapViewRenderer cubemapViewRenderer;
	private SplashScreen splash;

	private SyphonServer syphonServer;
	private Spout spout;

	private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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
	}

	/**
	 * Prints a welcome message indicating that the library has been initialized.
	 */
	private void welcome() {
		System.out.println("[ziviDomeLive] Library initialized.");
	}

	/**
	 * Sets the current scene to be rendered.
	 *
	 * @param scene the Scene instance to be set
	 */
	public void setScene(Scene scene) {
		this.currentScene = scene;
		currentScene.setupScene();
	}

	/**
	 * Sets up the rendering environment, including frame rate, OpenGL info, texture hints,
	 * Syphon/Spout setup, and mouse event registration.
	 *
	 * @throws IllegalStateException if the PApplet instance is not properly configured
	 */
	public void setup() {
		if (p == null) {
			throw new IllegalStateException("PApplet instance is not properly configured.");
		}

		System.out.println("Starting setup...");

		try {
			p.frameRate(64);
			System.out.println("Frame rate set to 64.");
		} catch (Exception e) {
			System.out.println("Error setting frame rate: " + e.getMessage());
		}

		try {
			printlnOpenGLInfo();
		} catch (Exception e) {
			System.out.println("Error printing OpenGL info: " + e.getMessage());
		}

		try {
			setupHints();
			System.out.println("Texture hints configured.");
		} catch (Exception e) {
			System.out.println("Error configuring texture hints: " + e.getMessage());
		}

		p.registerMethod("post", this);

		try {
			setupSyphonOrSpout();
			System.out.println("Syphon/Spout setup completed.");
		} catch (Exception e) {
			System.out.println("Error setting up Syphon/Spout: " + e.getMessage());
		}

		try {
			registerMouseEvents();
		} catch (Exception e) {
			System.out.println("Error registering mouse events: " + e.getMessage());
		}

		splash = new SplashScreen(p);
		splash.start();

		System.out.println("Setup completed.");
	}

	/**
	 * Prints OpenGL information including version, vendor, and renderer.
	 */
	void printlnOpenGLInfo() {
		PApplet.println(PGraphicsOpenGL.OPENGL_VERSION);
		PApplet.println(PGraphicsOpenGL.OPENGL_VENDOR);
		PApplet.println(PGraphicsOpenGL.OPENGL_RENDERER);
	}

	/**
	 * Configures texture hints for the rendering environment.
	 */
	void setupHints() {
		p.textureMode(PConstants.NORMAL);
		p.textureWrap(PConstants.REPEAT);
		p.hint(PConstants.ENABLE_TEXTURE_MIPMAPS);
		p.hint(PConstants.ENABLE_DEPTH_TEST);
		p.hint(PConstants.ENABLE_OPTIMIZED_STROKE);  // Otimiza a renderização de contornos
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
	 * Initializes various managers required for rendering and control.
	 */
	public void initializeManagers() {
		try {
			System.out.println("Initializing managers...");

			CompletableFuture<Void> cameraManagerFuture = CompletableFuture.runAsync(() -> {
				cameraManager = new CameraManager();
				System.out.println("CameraManager initialized.");
			});

			CompletableFuture<Void> renderersFuture = CompletableFuture.runAsync(this::initializeRenderers);

			CompletableFuture<Void> controlManagerFuture = CompletableFuture.runAsync(() -> {
				controlManager = new ControlManager(p, this, resolution);
				System.out.println("ControlManager initialized.");
			});

			CompletableFuture.allOf(cameraManagerFuture, renderersFuture, controlManagerFuture).join();
			System.out.println("Managers initialized successfully.");
		} catch (Exception e) {
			System.out.println("Error initializing managers: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Initializes various renderers required for different views.
	 */
	void initializeRenderers() {
		try {
			System.out.println("Initializing renderers...");

			// Paths to shader files
			String equirectangularVertexShaderPath = "data/shaders/equirectangular.vert";
			String equirectangularFragmentShaderPath = "data/shaders/equirectangular.frag";
			String domemasterVertexShaderPath = "data/shaders/domemaster.vert";
			String domemasterFragmentShaderPath = "data/shaders/domemaster.frag";

			// Load shaders
			PShader equirectangularShader = p.loadShader(equirectangularFragmentShaderPath, equirectangularVertexShaderPath);
			PShader domemasterShader = p.loadShader(domemasterFragmentShaderPath, domemasterVertexShaderPath);

			// Initialize renderers asynchronously
			CompletableFuture<Void> cubemapRendererFuture = CompletableFuture.runAsync(() -> {
				cubemapRenderer = new CubemapRenderer(resolution, p);
				System.out.println("CubemapRenderer initialized: " + true);
			});

			CompletableFuture<Void> equirectangularRendererFuture = CompletableFuture.runAsync(() -> {
				equirectangularRenderer = new EquirectangularRenderer(resolution, equirectangularFragmentShaderPath, equirectangularVertexShaderPath, p);
				System.out.println("EquirectangularRenderer initialized: " + true);
			});

			CompletableFuture<Void> standardRendererFuture = CompletableFuture.runAsync(() -> {
				standardRenderer = new StandardRenderer(p, p.width, p.height, currentScene);
				System.out.println("StandardRenderer initialized: " + true);
			});

			CompletableFuture<Void> fisheyeDomemasterFuture = CompletableFuture.runAsync(() -> {
				fisheyeDomemaster = new FisheyeDomemaster(resolution, domemasterFragmentShaderPath, domemasterVertexShaderPath, p);
				System.out.println("FisheyeDomemaster initialized: " + true);
			});

			CompletableFuture<Void> cubemapViewRendererFuture = CompletableFuture.runAsync(() -> {
				cubemapViewRenderer = new CubemapViewRenderer(p, resolution);
				System.out.println("CubemapViewRenderer initialized: " + true);
			});

			// Wait for all renderers to be initialized
			CompletableFuture.allOf(
					cubemapRendererFuture,
					equirectangularRendererFuture,
					standardRendererFuture,
					fisheyeDomemasterFuture,
					cubemapViewRendererFuture
			).join();

			System.out.println("Renderers initialized successfully.");
		} catch (Exception e) {
			System.out.println("Error initializing renderers: " + e.getMessage());
			e.printStackTrace();
		}
	}


	/**
	 * Sets up Syphon or Spout based on the operating system.
	 */
	void setupSyphonOrSpout() {
		try {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("mac")) {
				syphonServer = new SyphonServer(p, "ziviDomeLive Syphon");
				System.out.println("SyphonServer initialized for macOS.");
			} else if (os.contains("win")) {
				spout = new Spout(p);
				System.out.println("Spout initialized for Windows.");
			}
		} catch (Exception e) {
			System.out.println("Error setting up Syphon/Spout: " + e.getMessage());
		}
	}

	/**
	 * Registers mouse events for interaction.
	 */
	void registerMouseEvents() {
		try {
			p.registerMethod("mouseEvent", this);
			System.out.println("Mouse events registered.");
		} catch (Exception e) {
			System.out.println("Error registering mouse events: " + e.getMessage());
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
			System.out.println("Error: Renderer or scene not initialized.");
			return;
		}

		clearBackground();         // Limpa o fundo
		handleGraphicsReset();     // Garante que o reset gráfico seja realizado, se necessário
		captureCubemap();          // Captura o cubemap para a cena atual
		renderView();              // Renderiza a visualização principal

		if (showPreview) {
			drawFloatingPreview(); // Desenha uma visualização flutuante, se ativada
		}
		sendOutput();              // Envia a saída para Syphon/Spout, se habilitado
		drawControlPanel();        // Exibe o painel de controle
	}

	void individualRenderer() {
		if (!initialized) {
			System.out.println("Error: System not fully initialized.");
			return;
		}

		clearBackground();
		handleGraphicsReset(); // Ensure graphics reset is handled
		captureCubemap();
		sendOutput();
		drawControlPanel();
	}

	void clearBackground() {
		p.background(0, 0, 0, 0);
	}

	/**
	 * Handles resetting the graphics if a reset is pending.
	 */
	private void handleGraphicsReset() {
		if (pendingReset) {
			System.out.println("Pending reset detected. Changing resolution to: " + pendingResolution);
			releaseGraphicsResources(); // Libera os recursos gráficos antigos
			resolution = pendingResolution;
			initializeRenderers(); // Inicializa novos recursos gráficos
			pendingReset = false;
			System.out.println("Graphics reset completed.");
		}
	}

	/**
	 * Captures the cubemap for the current scene.
	 */
	private void captureCubemap() {
		if (cubemapRenderer != null) {
			cubemapRenderer.captureCubemap(getPitch(), getYaw(), getRoll(), cameraManager, currentScene);
		} else {
			System.out.println("Error: CubemapRenderer not initialized.");
		}
	}

	/**
	 * Displays the given PGraphics object on the screen.
	 *
	 * @param pg the PGraphics object to be displayed
	 */
	private void displayView(PGraphics pg) {
		float aspectRatio = pg.width / (float) pg.height;
		float displayWidth = p.width;
		float displayHeight = p.width / aspectRatio;

		if (displayHeight > p.height) {
			displayHeight = p.height;
			displayWidth = p.height * aspectRatio;
		}

		p.image(pg, (p.width - displayWidth) / 2, (p.height - displayHeight) / 2, displayWidth, displayHeight);
	}

	/**
	 * Updates the render views based on the current view type.
	 */
	private void updateRenderViews() {
		equirectangularRenderer.render(cubemapRenderer.getCubemapFaces());
		fisheyeDomemaster.applyShader(equirectangularRenderer.getEquirectangular(), getFov());

		switch (getCurrentView()) {
			case CUBEMAP:
				cubemapViewRenderer.drawCubemapToGraphics(cubemapRenderer.getCubemapFaces());
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
	 * Sends the output to Syphon or Spout if enabled.
	 */
	private void sendOutput() {
		if (isEnableOutput()) {
			if (syphonServer != null) {
				syphonServer.sendImage(fisheyeDomemaster.getDomemasterGraphics());
			} else if (spout != null) {
				spout.sendTexture(fisheyeDomemaster.getDomemasterGraphics());
			}
		}
	}

	/**
	 * Renders the fisheye domemaster view by applying the shader and displaying the view.
	 * If the FisheyeDomemaster is not initialized, an error message is printed.
	 */
	public void renderFisheyeDomemaster() {
		if (fisheyeDomemaster != null) {
			individualRenderer();
			equirectangularRenderer.render(cubemapRenderer.getCubemapFaces());
			fisheyeDomemaster.applyShader(equirectangularRenderer.getEquirectangular(), getFov());
			displayView(fisheyeDomemaster.getDomemasterGraphics());
		} else {
			System.out.println("Error: FisheyeDomemaster not initialized.");
		}
	}
	
	/**
	 * Renders the equirectangular view by invoking the EquirectangularRenderer.
	 * If the EquirectangularRenderer is not initialized, an error message is printed.
	 */
	public void renderEquirectangular() {
		if (equirectangularRenderer != null) {
			individualRenderer();
			equirectangularRenderer.render(cubemapRenderer.getCubemapFaces());
			displayView(equirectangularRenderer.getEquirectangular());
		} else {
			System.out.println("Error: EquirectangularRenderer not initialized.");
		}
	}

	/**
	 * Renders the cubemap view by drawing the cubemap faces to the graphics and displaying the view.
	 * If the CubemapViewRenderer is not initialized, an error message is printed.
	 */
	public void renderCubemap() {
		if (cubemapViewRenderer != null) {
			individualRenderer();
			cubemapViewRenderer.drawCubemapToGraphics(cubemapRenderer.getCubemapFaces());
			displayView(cubemapViewRenderer.getCubemap());
		} else {
			System.out.println("Error: CubemapViewRenderer not initialized.");
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
			System.out.println("Error: StandardRenderer not initialized.");
		}
	}

	/**
	 * Draws the control panel if it is set to be shown.
	 */
	private void drawControlPanel() {
		p.hint(PConstants.DISABLE_DEPTH_TEST);
		controlManager.updateFpsLabel(p.frameRate);

		if (showControlPanel) {
			controlManager.show();
		} else {
			controlManager.hide();
		}
		p.hint(PConstants.ENABLE_DEPTH_TEST);
	}

	/**
	 * Draws a floating preview of the fisheye domemaster view.
	 */
	public void drawFloatingPreview() {
		float previewWidth = 200f;
		float previewHeight = 200f;
		float x = p.width - previewWidth;
		float y = p.height - previewHeight;

		PGraphics previewGraphics = fisheyeDomemaster.getDomemasterGraphics();
		p.image(previewGraphics, x, y, previewWidth, previewHeight);
	}

	/**
	 * Handles mouse events for interaction.
	 *
	 * @param event the MouseEvent object representing the mouse event
	 */
	public void mouseEvent(MouseEvent event) {
		if (splash != null && event.getAction() == MouseEvent.PRESS) {
			splash.mousePressed(); // Inicia o fade-out da splash quando o mouse é pressionado
		}

		// Permanece com os eventos de mouse originais para interação de cena
		if (event.getAction() == MouseEvent.WHEEL) {
			standardRenderer.getCam().mouseWheel(event);
		}
		if (currentScene != null) {
			currentScene.mouseEvent(event);
		}
	}

	/**
	 * Handles key press events for interaction.
	 */
	public void keyPressed() {
		if (!controlManager.isNumberboxActive()) {
			if (p.key == 'h') {
				showControlPanel = !showControlPanel;
				System.out.println("Toggling control panel visibility: " + showControlPanel);
			}
			if (p.key == 'm') {
				setCurrentView(ViewType.values()[(getCurrentView().ordinal() + 1) % ViewType.values().length]);
				System.out.println("Switching view to: " + getCurrentView());
			}
		}
	}

	/**
	 * Handles control events from the control panel.
	 *
	 * @param theEvent the ControlEvent object representing the control event
	 */
	public void controlEvent(ControlEvent theEvent) {
		if (controlManager != null) {
			controlManager.handleEvent(theEvent);
		}
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
		return enableOutput;
	}

	/**
	 * Sets whether output is enabled.
	 *
	 * @param enableOutput true to enable output, false to disable
	 */
	public void setEnableOutput(boolean enableOutput) {
		this.enableOutput = enableOutput;
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
		System.out.println("Changing resolution to: " + newResolution); // Imprime a nova resolução no console
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
	 * Gets the current FisheyeDomemaster instance.
	 *
	 * @return the current FisheyeDomemaster instance
	 */
	public FisheyeDomemaster getFisheyeDomemaster() {
		return fisheyeDomemaster;
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
}