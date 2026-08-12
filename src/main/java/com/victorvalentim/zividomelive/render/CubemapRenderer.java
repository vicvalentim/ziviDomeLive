package com.victorvalentim.zividomelive.render;

import com.victorvalentim.zividomelive.Scene;
import com.victorvalentim.zividomelive.render.camera.CameraManager;
import com.victorvalentim.zividomelive.render.camera.CameraOrientation;
import com.victorvalentim.zividomelive.render.camera.CubemapFace;
import com.victorvalentim.zividomelive.render.gl.ProcessingGlAdapter;
import com.victorvalentim.zividomelive.support.LogManager;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PVector;
import processing.opengl.PGraphicsOpenGL;

import java.util.logging.Logger;

/**
 * CubemapRenderer class handles the rendering of cubemap faces using Processing's PGraphicsOpenGL.
 * It uses cached frustum parameters for rendering.
 */
public class CubemapRenderer implements PConstants {
    private static final int NUM_FACES = CubemapFace.count();
    private static final float DEFAULT_NEAR_PLANE = 0.01f;
    private static final float DEFAULT_FAR_PLANE = 10000000.0f;
    private static final Logger LOGGER = LogManager.getLogger();

    private PGraphicsOpenGL[] cubemapFaces;
    private int resolution;
    private final PApplet parent;
    private final ProcessingGlAdapter glAdapter = ProcessingGlAdapter.getDefault();

    // Cached frustum parameters
    private volatile float cachedNearPlane;
    private volatile float cachedFarPlane;
    private volatile float cachedFieldOfView;

    private final SphericalOrientation legacyOrientation = new SphericalOrientation();


    /**
     * Constructs a CubemapRenderer with the specified initial resolution and parent PApplet.
     *
     * @param initialResolution the initial resolution for cubemap faces
     * @param parent the parent PApplet instance
     */
    public CubemapRenderer(int initialResolution, PApplet parent) {
        this.parent = parent;
        this.resolution = initialResolution;
        initializeCubemapFaces();
        cachedNearPlane = DEFAULT_NEAR_PLANE;
        cachedFarPlane = DEFAULT_FAR_PLANE;
        cachedFieldOfView = PApplet.PI / 2;
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
                glAdapter.dispose(cubemapFaces[i]);
            }
            cubemapFaces[i] = glAdapter.createGraphics(parent, resolution, resolution, P3D);
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
     * @param sphericalOrientation unit quaternion describing the spherical orientation
     */
    private void configureCameraForFace(
            PGraphicsOpenGL pg,
            CameraOrientation orientation,
            Quaternion sphericalOrientation) {
        PVector eye = new PVector(0, 0, 0);

        pg.camera(eye.x, eye.y, eye.z, orientation.centerX, orientation.centerY, orientation.centerZ,
                  orientation.upX, orientation.upY, orientation.upZ);
        pg.perspective(cachedFieldOfView, 1, cachedNearPlane, cachedFarPlane);

        pg.applyMatrix(sphericalOrientation.toMatrix());
    }

    /**
     * Configures the camera for a canonical cubemap face without requiring a CameraManager.
     * @param face canonical cubemap face in stable layout order
     * @param sphericalOrientation unit quaternion describing the spherical orientation
     */
    private void configureCameraForFace(
            PGraphicsOpenGL pg,
            CubemapFace face,
            Quaternion sphericalOrientation) {
        pg.camera(0f, 0f, 0f, face.centerX(), face.centerY(), face.centerZ(),
                  face.upX(), face.upY(), face.upZ());
        pg.perspective(cachedFieldOfView, 1, cachedNearPlane, cachedFarPlane);

        pg.applyMatrix(sphericalOrientation.toMatrix());
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
        legacyOrientation.setPitch(pitch);
        legacyOrientation.setYaw(yaw);
        legacyOrientation.setRoll(roll);
        captureCubemap(legacyOrientation.getQuaternion(), cameraManager, currentScene);
    }

    /**
     * Captures the cubemap faces based on the canonical face orientations.
     *
     * @param pitch rotation around the X axis
     * @param yaw   rotation around the Z axis
     * @param roll  rotation around the Y axis
     * @param currentScene the current scene to render
     */
    public void captureCubemap(float pitch, float yaw, float roll, Scene currentScene) {
        legacyOrientation.setPitch(pitch);
        legacyOrientation.setYaw(yaw);
        legacyOrientation.setRoll(roll);
        captureCubemap(legacyOrientation.getQuaternion(), currentScene);
    }

    /**
     * Captures the cubemap faces using a quaternion orientation shared by preview and output.
     *
     * @param sphericalOrientation unit quaternion describing the spherical orientation
     * @param cameraManager manager for camera orientations
     * @param currentScene the current scene to render
     */
    public void captureCubemap(
            Quaternion sphericalOrientation,
            CameraManager cameraManager,
            Scene currentScene) {
        if (cubemapFaces == null) {
            initializeCubemapFaces();
        }
        Quaternion effectiveOrientation = sphericalOrientation == null
                ? new Quaternion(0.0f, 0.0f, 0.0f, 1.0f)
                : sphericalOrientation;
        for (int i = 0; i < NUM_FACES; i++) {
            cubemapFaces[i].beginDraw();
            cubemapFaces[i].background(0, 0);
            configureCameraForFace(
                    cubemapFaces[i],
                    cameraManager.getOrientation(i),
                    effectiveOrientation);
            if (currentScene != null) {
                currentScene.sceneRender(cubemapFaces[i]);
            }
            cubemapFaces[i].endDraw();
        }
    }

    /**
     * Captures the cubemap faces using the canonical cubemap-face orientation table.
     *
     * @param sphericalOrientation unit quaternion describing the spherical orientation
     * @param currentScene the current scene to render
     */
    public void captureCubemap(
            Quaternion sphericalOrientation,
            Scene currentScene) {
        if (cubemapFaces == null) {
            initializeCubemapFaces();
        }
        Quaternion effectiveOrientation = sphericalOrientation == null
                ? new Quaternion(0.0f, 0.0f, 0.0f, 1.0f)
                : sphericalOrientation;
        for (int i = 0; i < NUM_FACES; i++) {
            cubemapFaces[i].beginDraw();
            cubemapFaces[i].background(0, 0);
            configureCameraForFace(
                    cubemapFaces[i],
                    CubemapFace.at(i),
                    effectiveOrientation);
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
     * Disposes of cubemap faces to free up resources.
     */
    public void dispose() {
        if (cubemapFaces != null) {
            for (PGraphics face : cubemapFaces) {
                if (face != null) {
                    glAdapter.dispose(face);
                }
            }
            cubemapFaces = null;
        }
    }
}
