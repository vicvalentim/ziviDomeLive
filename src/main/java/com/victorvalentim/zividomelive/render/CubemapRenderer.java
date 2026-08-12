package com.victorvalentim.zividomelive.render;

import com.victorvalentim.zividomelive.Scene;
import com.victorvalentim.zividomelive.render.camera.CameraManager;
import com.victorvalentim.zividomelive.render.camera.CubemapFace;
import com.victorvalentim.zividomelive.render.gl.CubemapTarget;
import com.victorvalentim.zividomelive.render.gl.ProcessingGlAdapter;
import com.victorvalentim.zividomelive.support.LogManager;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.opengl.PGraphicsOpenGL;

import java.util.logging.Logger;

/**
 * Captures a scene into a native OpenGL cubemap texture.
 *
 * <p>The renderer uses one Processing OpenGL command target as a bridge for the public
 * {@code sceneRender(PGraphicsOpenGL)} contract, while the actual color target is a
 * {@code GL_TEXTURE_CUBE_MAP} face attached to a reusable native framebuffer. No legacy
 * {@code PGraphicsOpenGL[6]} face targets are allocated.</p>
 */
public class CubemapRenderer implements PConstants {
    private static final float DEFAULT_NEAR_PLANE = 1.0f;
    private static final float DEFAULT_FAR_PLANE = 22000.0f;
    private static final Logger LOGGER = LogManager.getLogger();

    private PGraphicsOpenGL nativeCaptureGraphics;
    private CubemapTarget nativeCubemapTarget;
    private int resolution;
    private final PApplet parent;
    private final ProcessingGlAdapter glAdapter = ProcessingGlAdapter.getDefault();
    private boolean nativeCubemapUnavailableLogged;

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
        initializeNativeCubemapTarget();
        cachedNearPlane = DEFAULT_NEAR_PLANE;
        cachedFarPlane = DEFAULT_FAR_PLANE;
        cachedFieldOfView = PApplet.PI / 2;
    }

    /**
     * Updates the native cubemap resolution when needed.
     *
     * @param newResolution the new cubemap face resolution
     */
    void updateResolution(int newResolution) {
        if (this.resolution != newResolution) {
            this.resolution = newResolution;
            disposeNativeCaptureGraphics();
            initializeNativeCubemapTarget();
        }
    }

    /**
     * Configures the parent renderer while a native cubemap face is attached to the active FBO.
     *
     * <p>This follows the Processing PGL sampleCube reference: face cameras are aligned to the
     * OpenGL cubemap target order, with Processing's Y-up/Y-down convention corrected by
     * {@code scale(-1, 1, -1)} before the scene is rendered.</p>
     */
    private void configureNativeCameraForFace(
            PGraphicsOpenGL pg,
            CubemapFace face,
            Quaternion sphericalOrientation,
            float fieldOfView) {
        switch (face) {
            case POSITIVE_Y -> pg.camera(
                    0f, 0f, 0f,
                    0f, -1f, 0f,
                    0f, 0f, -1f);
            case NEGATIVE_Y -> pg.camera(
                    0f, 0f, 0f,
                    0f, 1f, 0f,
                    0f, 0f, 1f);
            default -> pg.camera(
                    0f, 0f, 0f,
                    face.centerX(), face.centerY(), face.centerZ(),
                    face.upX(), face.upY(), face.upZ());
        }
        pg.perspective(fieldOfView, 1.0f, cachedNearPlane, cachedFarPlane);
        pg.scale(-1f, 1f, -1f);
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
        if (cameraManager != null && LogManager.isDebugEnabled()) {
            LOGGER.fine("CameraManager argument ignored by native cubemap capture; sampleCube face table is authoritative.");
        }
        Quaternion effectiveOrientation = sphericalOrientation == null
                ? new Quaternion(0.0f, 0.0f, 0.0f, 1.0f)
                : sphericalOrientation;
        captureNativeCubemap(effectiveOrientation, cameraManager, currentScene);
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
        Quaternion effectiveOrientation = sphericalOrientation == null
                ? new Quaternion(0.0f, 0.0f, 0.0f, 1.0f)
                : sphericalOrientation;
        captureNativeCubemap(effectiveOrientation, null, currentScene);
    }

    private boolean captureNativeCubemap(
            Quaternion effectiveOrientation,
            CameraManager cameraManager,
            Scene currentScene) {
        if (nativeCubemapTarget == null) {
            LOGGER.warning("Native cubemap capture skipped: native CubemapTarget is unavailable.");
            return false;
        }

        PGraphicsOpenGL captureGraphics = ensureNativeCaptureGraphics();
        if (captureGraphics == null) {
            return false;
        }

        float captureFieldOfView = cachedFieldOfView;
        if (LogManager.isDebugEnabled()) {
            LOGGER.fine("Native cubemap capture begin: resolution=" + resolution
                    + ", fovDeg=" + PApplet.degrees(captureFieldOfView)
                    + ", near=" + cachedNearPlane
                    + ", far=" + cachedFarPlane
                    + ", scene=" + (currentScene != null ? currentScene.getName() : "<none>")
                    + ", cameraSource=sampleCube"
                    + (cameraManager != null ? ", legacyCameraManagerFallback=true" : ""));
        }

        try {
            for (CubemapFace face : CubemapFace.values()) {
                if (LogManager.isDebugEnabled()) {
                    LOGGER.finer("Native cubemap capture face begin: " + face
                            + " -> glTarget=0x" + Integer.toHexString(CubemapTarget.glTargetFor(face)));
                }
                captureGraphics.beginDraw();
                try {
                    nativeCubemapTarget.renderFace(face, captureGraphics, () -> {
                        captureGraphics.resetMatrix();
                        captureGraphics.background(0, 0);
                        configureNativeCameraForFace(
                                captureGraphics,
                                face,
                                effectiveOrientation,
                                captureFieldOfView);
                        if (currentScene != null) {
                            currentScene.sceneRender(captureGraphics);
                        }
                        captureGraphics.noLights();
                        captureGraphics.flush();
                    });
                } finally {
                    captureGraphics.endDraw();
                }
            }
            refreshNativeCubemapMipmaps();
            if (LogManager.isDebugEnabled()) {
                LOGGER.fine("Native cubemap capture completed.");
            }
            return true;
        } catch (RuntimeException error) {
            LOGGER.warning("Native cubemap capture failed; spherical frame skipped: "
                    + error.getMessage());
            disposeNativeCubemapTarget();
            return false;
        }
    }

    /**
     * Returns the native cubemap target populated by direct face-FBO capture.
     *
     * @return native cubemap target, or {@code null} when unsupported/unavailable
     */
    public CubemapTarget getNativeCubemapTarget() {
        return nativeCubemapTarget;
    }

    /**
     * Reports whether the renderer currently owns an allocated native cubemap.
     *
     * @return {@code true} when direct {@code GL_TEXTURE_CUBE_MAP} capture is available
     */
    public boolean hasNativeCubemapTarget() {
        return nativeCubemapTarget != null && nativeCubemapTarget.isAllocated();
    }

    /**
     * Disposes native cubemap capture resources.
     */
    public void dispose() {
        disposeNativeCaptureGraphics();
        disposeNativeCubemapTarget();
    }

    private void initializeNativeCubemapTarget() {
        disposeNativeCubemapTarget();
        try {
            nativeCubemapTarget = CubemapTarget.create(parent, resolution);
            ensureNativeCaptureGraphics();
            nativeCubemapUnavailableLogged = false;
        } catch (IllegalStateException error) {
            nativeCubemapTarget = null;
            if (!nativeCubemapUnavailableLogged) {
                LOGGER.warning("Native cubemap target unavailable: "
                        + error.getMessage());
                nativeCubemapUnavailableLogged = true;
            }
        }
    }

    private PGraphicsOpenGL ensureNativeCaptureGraphics() {
        if (nativeCaptureGraphics != null
                && nativeCaptureGraphics.width == resolution
                && nativeCaptureGraphics.height == resolution) {
            return nativeCaptureGraphics;
        }

        disposeNativeCaptureGraphics();
        try {
            nativeCaptureGraphics = glAdapter.createGraphics(parent, resolution, resolution, P3D);
            if (LogManager.isDebugEnabled()) {
                LOGGER.fine("Native cubemap capture graphics allocated: resolution=" + resolution
                        + ", renderer=" + nativeCaptureGraphics.getClass().getSimpleName());
            }
            return nativeCaptureGraphics;
        } catch (RuntimeException error) {
            LOGGER.warning("Native cubemap capture graphics unavailable: " + error.getMessage());
            nativeCaptureGraphics = null;
            disposeNativeCubemapTarget();
            return null;
        }
    }

    private void refreshNativeCubemapMipmaps() {
        if (nativeCubemapTarget == null || nativeCubemapTarget.hasValidMipmaps()) {
            return;
        }
        try {
            nativeCubemapTarget.generateMipmaps();
        } catch (RuntimeException error) {
            LOGGER.warning("Native cubemap mipmap refresh failed; disabling native cubemap bridge: "
                    + error.getMessage());
            disposeNativeCubemapTarget();
        }
    }

    private void disposeNativeCubemapTarget() {
        if (nativeCubemapTarget != null) {
            nativeCubemapTarget.dispose();
            nativeCubemapTarget = null;
        }
    }

    private void disposeNativeCaptureGraphics() {
        if (nativeCaptureGraphics != null) {
            glAdapter.dispose(nativeCaptureGraphics);
            nativeCaptureGraphics = null;
        }
    }
}
