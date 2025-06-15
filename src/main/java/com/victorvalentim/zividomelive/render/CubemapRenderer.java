package com.victorvalentim.zividomelive.render;

import com.victorvalentim.zividomelive.Scene;
import com.victorvalentim.zividomelive.render.camera.CameraManager;
import com.victorvalentim.zividomelive.render.camera.CameraOrientation;
import com.victorvalentim.zividomelive.support.LogManager;
import com.victorvalentim.zividomelive.support.ThreadManager;
import com.victorvalentim.zividomelive.render.Quaternion;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PVector;
import processing.opengl.PGraphicsOpenGL;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * CubemapRenderer class handles the rendering of cubemap faces using Processing's PGraphicsOpenGL.
 * It supports asynchronous calculation of frustum parameters and multi-threaded rendering.
 */
public class CubemapRenderer implements PConstants {
    private static final int NUM_FACES = 6;
    private static final float DEFAULT_NEAR_PLANE = 0.01f;
    private static final float DEFAULT_FAR_PLANE = 10000000.0f;
    private static final Logger LOGGER = LogManager.getLogger();

    // Thread manager can be used to obtain a shared executor
    private final ExecutorService sharedExecutor = ThreadManager.getExecutor();
    private final ExecutorService executor;
    private PGraphicsOpenGL[] cubemapFaces;
    private int resolution;
    private final PApplet parent;

    // Cached frustum parameters
    private volatile float cachedNearPlane;
    private volatile float cachedFarPlane;
    private volatile float cachedFieldOfView;

    // Orientation quaternion used for incremental rotations
    private Quaternion currentOrientation = new Quaternion(0, 0, 0, 1);

    // Futures for asynchronous calculations
    private Future<Float> nearPlaneFuture;
    private Future<Float> farPlaneFuture;
    private Future<Float> fieldOfViewFuture;

    /**
     * Constructs a CubemapRenderer with the specified initial resolution and parent PApplet.
     *
     * @param initialResolution the initial resolution for cubemap faces
     * @param parent the parent PApplet instance
     */
    public CubemapRenderer(int initialResolution, PApplet parent) {
        this.parent = parent;
        this.resolution = initialResolution;
        int numThreads = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(numThreads);
        initializeCubemapFaces();
        calculateFrustumParametersAsync();
    }

    /**
     * Initializes or reinitializes the cubemap faces with the current resolution.
     */
    private void initializeCubemapFaces() {
        if (cubemapFaces == null) {
            cubemapFaces = new PGraphicsOpenGL[NUM_FACES];
        }
        for (int i = 0; i < NUM_FACES; i++) {
            if (cubemapFaces[i] != null) {
                cubemapFaces[i].dispose();
            }
            cubemapFaces[i] = (PGraphicsOpenGL) parent.createGraphics(resolution, resolution, P3D);
        }
    }

    /**
     * Updates the resolution and reinitializes the cubemap faces if needed.
     *
     * @param newResolution the new resolution for cubemap faces
     */
    void updateResolution(int newResolution) {
        if (this.resolution != newResolution) {
            this.resolution = newResolution;
            initializeCubemapFaces();
        }
    }

    /**
     * Configures the camera for each cubemap face using asynchronously calculated frustum parameters.
     * @param pitch rotation around the X axis
     * @param yaw   rotation around the Z axis
     * @param roll  rotation around the Y axis
     */
    private void configureCameraForFace(PGraphicsOpenGL pg, CameraOrientation orientation, float pitch, float yaw, float roll) {
        PVector eye = new PVector(0, 0, 0);
        try {
            // Wait for the asynchronous calculations to complete
            cachedNearPlane = nearPlaneFuture.get();
            cachedFarPlane = farPlaneFuture.get();
            cachedFieldOfView = fieldOfViewFuture.get();
        } catch (Exception e) {
            LOGGER.severe("Error retrieving frustum parameters: " + e.getMessage());
            return;
        }

        pg.camera(eye.x, eye.y, eye.z, orientation.centerX, orientation.centerY, orientation.centerZ,
                  orientation.upX, orientation.upY, orientation.upZ);
        pg.perspective(cachedFieldOfView, 1, cachedNearPlane, cachedFarPlane);

        // The following translations are redundant if they are (0,0,0); remove if not needed
        pg.translate(0, 0, 0);
        // Build rotation using axis-angle quaternions and SLERP for smoother updates
        Quaternion qPitch = Quaternion.fromAxisAngle(1f, 0f, 0f, pitch);
        Quaternion qYaw   = Quaternion.fromAxisAngle(0f, 0f, 1f, yaw);
        Quaternion qRoll  = Quaternion.fromAxisAngle(0f, 1f, 0f, roll);
        Quaternion target = qYaw.multiply(qRoll).multiply(qPitch);
        currentOrientation = currentOrientation.slerp(target, 1f);
        pg.applyMatrix(currentOrientation.toMatrix());
    }

    /**
     * Starts asynchronous calculation of frustum parameters.
     */
    private void calculateFrustumParametersAsync() {
        nearPlaneFuture = executor.submit(this::calculateNearPlane);
        farPlaneFuture  = executor.submit(this::calculateFarPlane);
        fieldOfViewFuture = executor.submit(this::calculateFieldOfView);
    }

    private float calculateNearPlane() {
        return DEFAULT_NEAR_PLANE;
    }

    private float calculateFarPlane() {
        return DEFAULT_FAR_PLANE;
    }

    private float calculateFieldOfView() {
        return PApplet.PI / 2;
    }

    /**
     * Captures the cubemap faces based on the camera orientation.
     *
     * @param pitch rotation around the X axis
     * @param yaw   rotation around the Z axis
     * @param roll  rotation around the Y axis
     * @param cameraManager manager for camera orientations
     * @param currentScene the current scene to render
     */
    public void captureCubemap(float pitch, float yaw, float roll, CameraManager cameraManager, Scene currentScene) {
        if (cubemapFaces == null) {
            initializeCubemapFaces();
        }
        for (int i = 0; i < NUM_FACES; i++) {
            cubemapFaces[i].beginDraw();
            cubemapFaces[i].background(0, 0);
            configureCameraForFace(cubemapFaces[i], cameraManager.getOrientation(i), pitch, yaw, roll);
            if (currentScene != null) {
                currentScene.sceneRender(cubemapFaces[i]);
            }
            cubemapFaces[i].endDraw();
        }
    }

    /**
     * Returns an array of cubemap faces.
     *
     * @return an array containing the current PGraphics cubemap faces
     */
    public PGraphicsOpenGL[] getCubemapFaces() {
        if (cubemapFaces == null) {
            initializeCubemapFaces();
        }
        return cubemapFaces;
    }

    /**
     * Disposes of cubemap faces and shuts down the executor to free up resources.
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
            executor.shutdown();
        }
    }
}