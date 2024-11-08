package com.victorvalentim.zividomelive;

import processing.core.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The CubemapRenderer class handles the creation and rendering of cubemap faces with dynamic frustum adjustments.
 */
public class CubemapRenderer {
    private static final Logger LOGGER = Logger.getLogger(CubemapRenderer.class.getName()); // Logger para a classe

    private final ExecutorService executor; // Pool de threads dinâmico para cálculos
    private PGraphics[] cubemapFaces;
    private int resolution;
    private final PApplet parent;

    // Valores padrão para os planos do frustum (podem ser ajustados conforme necessário)
    final float defaultNearPlane = 0.01f;
    final float defaultFarPlane = 22000.0f;

    // Variáveis para armazenar os resultados dos cálculos de frustum
    private volatile float cachedNearPlane;
    private volatile float cachedFarPlane;
    private volatile float cachedFieldOfView;

    // Futuros para cálculos assíncronos
    private Future<Float> nearPlaneFuture;
    private Future<Float> farPlaneFuture;
    private Future<Float> fieldOfViewFuture;

    /**
     * Constructs a CubemapRenderer with the specified initial resolution and parent PApplet.
     *
     * @param initialResolution the initial resolution of the cubemap faces
     * @param parent the parent PApplet instance
     */
    CubemapRenderer(int initialResolution, PApplet parent) {
        this.parent = parent;
        this.resolution = initialResolution;
        int numThreads = Runtime.getRuntime().availableProcessors(); // Detecta o número de núcleos
        this.executor = Executors.newFixedThreadPool(numThreads); // Cria o pool de threads dinâmico
        initializeCubemapFaces();
        calculateFrustumParametersAsync(); // Cálculo inicial
    }

    /**
     * Initializes or reinitializes the cubemap faces with the current resolution.
     */
    private void initializeCubemapFaces() {
        if (cubemapFaces == null) {
            cubemapFaces = new PGraphics[6];
        }
        for (int i = 0; i < 6; i++) {
            if (cubemapFaces[i] != null) {
                cubemapFaces[i].dispose();
            }
            cubemapFaces[i] = parent.createGraphics(resolution, resolution, PApplet.P3D);
        }
    }

    void updateResolution(int newResolution) {
        if (this.resolution != newResolution) {
            this.resolution = newResolution;
            initializeCubemapFaces();
        }
    }

    /**
     * Configura a câmera para cada face usando parâmetros de frustum calculados.
     */
    private void configureCameraForFace(PGraphics pg, CameraOrientation orientation, float pitch, float yaw, float roll) {
        PVector eye = new PVector(0, 0, 0);

        // Certifique-se de que os parâmetros foram calculados antes de configurar a câmera
        if (nearPlaneFuture.isDone() && farPlaneFuture.isDone() && fieldOfViewFuture.isDone()) {
            try {
                cachedNearPlane = nearPlaneFuture.get();
                cachedFarPlane = farPlaneFuture.get();
                cachedFieldOfView = fieldOfViewFuture.get();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erro ao obter valores de frustum para a configuração da câmera", e);
            }
        }

        // Configura a câmera e perspectiva com os valores calculados
        pg.camera(eye.x, eye.y, eye.z, orientation.centerX, orientation.centerY, orientation.centerZ, orientation.upX, orientation.upY, orientation.upZ);
        pg.perspective(cachedFieldOfView, 1, cachedNearPlane, cachedFarPlane);

        pg.translate((float) pg.width / 2, (float) pg.height / 2, 0);
        pg.rotateX(pitch);
        pg.rotateY(roll);
        pg.rotateZ(yaw);
        pg.translate((float) -pg.width / 2, (float) -pg.height / 2, 0);
    }

    /**
     * Inicia o cálculo assíncrono dos parâmetros do frustum.
     */
    private void calculateFrustumParametersAsync() {
        // Cálculo assíncrono dos parâmetros para evitar recalcular a cada face
        nearPlaneFuture = executor.submit(this::calculateNearPlaneForFace);
        farPlaneFuture = executor.submit(this::calculateFarPlaneForFace);
        fieldOfViewFuture = executor.submit(this::calculateFieldOfViewForFace);
    }

    /**
     * Métodos de cálculo paralelizados para parâmetros de frustum.
     */
    private float calculateNearPlaneForFace() {
        return defaultNearPlane;
    }

    private float calculateFarPlaneForFace() {
        return defaultFarPlane;
    }

    private float calculateFieldOfViewForFace() {
        return PApplet.PI / 2;
    }

    /**
     * Captures each face of the cubemap using calculated camera parameters.
     */
    void captureCubemap(float pitch, float yaw, float roll, CameraManager cameraManager, Scene currentScene) {
        if (cubemapFaces == null) {
            initializeCubemapFaces();
        }
        for (int i = 0; i < 6; i++) {
            cubemapFaces[i].beginDraw();
            cubemapFaces[i].background(0, 0);
            configureCameraForFace(cubemapFaces[i], cameraManager.getOrientation(i), pitch, yaw, roll);
            if (currentScene != null) {
                currentScene.sceneRender(cubemapFaces[i]);
            }
            cubemapFaces[i].endDraw();
        }
    }

    PGraphics[] getCubemapFaces() {
        if (cubemapFaces == null) {
            initializeCubemapFaces();
        }
        return cubemapFaces;
    }

    /**
     * Disposes of the cubemap faces and shuts down the thread pool to free up resources.
     */
    public void dispose() {
        if (cubemapFaces != null) {
            for (PGraphics face : cubemapFaces) {
                if (face != null) {
                    face.dispose();
                }
            }
            cubemapFaces = null;
        }
        if (executor != null) {
            executor.shutdown(); // Encerra o pool de threads
        }
    }
}
