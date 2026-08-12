package com.victorvalentim.zividomelive.render;

import com.victorvalentim.zividomelive.Scene;
import com.victorvalentim.zividomelive.render.camera.CameraManager;
import com.victorvalentim.zividomelive.render.camera.CubemapFace;
import com.victorvalentim.zividomelive.render.gl.CubemapTarget;
import com.victorvalentim.zividomelive.render.gl.ProcessingGlAdapter;
import com.victorvalentim.zividomelive.support.LogManager;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.opengl.PGraphicsOpenGL;

import java.util.logging.Logger;

/**
 * Captures a scene into a native OpenGL cubemap texture.
 *
 * <p>The renderer uses one Processing OpenGL command target as a bridge for the public
 * {@code sceneRender(PGraphicsOpenGL)} contract, while the actual color target is a
 * {@code GL_TEXTURE_CUBE_MAP} face attached to a reusable native framebuffer. The renderer
 * does not allocate six independent Processing face targets.</p>
 */
public class CubemapRenderer implements PConstants {
    private static final float DEFAULT_NEAR_PLANE = 1.0f;
    private static final float DEFAULT_FAR_PLANE = 1122000.0f;
    private static final Logger LOGGER = LogManager.getLogger();

    private PGraphicsOpenGL nativeCaptureGraphics;
    private CubemapTarget nativeCubemapTarget;
    private final EnvironmentBackgroundRenderer environmentBackgroundRenderer;
    private int resolution;
    private final PApplet parent;
    private final ProcessingGlAdapter glAdapter = ProcessingGlAdapter.getDefault();
    private boolean nativeCubemapUnavailableWarningLogged;

    // Cached frustum parameters
    private volatile float cachedNearPlane;
    private volatile float cachedFarPlane;
    private volatile float cachedFieldOfView;

    private final SphericalOrientation angleOrientation = new SphericalOrientation();


    /**
     * Constructs a CubemapRenderer with the specified initial resolution and parent PApplet.
     *
     * @param initialResolution the initial resolution for cubemap faces
     * @param parent the parent PApplet instance
     */
    public CubemapRenderer(int initialResolution, PApplet parent) {
        this.parent = parent;
        this.resolution = initialResolution;
        this.environmentBackgroundRenderer = new EnvironmentBackgroundRenderer(parent);
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
        angleOrientation.setPitch(pitch);
        angleOrientation.setYaw(yaw);
        angleOrientation.setRoll(roll);
        captureCubemap(angleOrientation.getQuaternion(), cameraManager, currentScene);
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
        angleOrientation.setPitch(pitch);
        angleOrientation.setYaw(yaw);
        angleOrientation.setRoll(roll);
        captureCubemap(angleOrientation.getQuaternion(), currentScene);
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
        Quaternion effectiveOrientation = sphericalOrientation == null
                ? new Quaternion(0.0f, 0.0f, 0.0f, 1.0f)
                : sphericalOrientation;
        captureNativeCubemap(effectiveOrientation, currentScene);
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
        captureNativeCubemap(effectiveOrientation, currentScene);
    }

    private boolean captureNativeCubemap(
            Quaternion effectiveOrientation,
            Scene currentScene) {
        if (nativeCubemapTarget == null) {
            if (!nativeCubemapUnavailableWarningLogged) {
                LOGGER.warning("Native cubemap capture unavailable; spherical frames will be skipped until resources are recreated.");
                nativeCubemapUnavailableWarningLogged = true;
            }
            return false;
        }

        PGraphicsOpenGL captureGraphics = ensureNativeCaptureGraphics();
        if (captureGraphics == null) {
            return false;
        }

        float captureFieldOfView = cachedFieldOfView;

        try {
            for (CubemapFace face : CubemapFace.values()) {
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
                        environmentBackgroundRenderer.renderCubemapFace(
                                captureGraphics,
                                face,
                                effectiveOrientation);
                        captureGraphics.noLights();
                        captureGraphics.flush();
                    });
                } finally {
                    captureGraphics.endDraw();
                }
            }
            refreshNativeCubemapMipmaps();
            nativeCubemapUnavailableWarningLogged = false;
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
     * Sets the LDR equirectangular environment image rendered behind spherical capture.
     *
     * @param image Processing image, or {@code null} to clear the environment
     */
    public void setEquirectangularBackground(PImage image) {
        environmentBackgroundRenderer.setEquirectangularImage(image);
    }

    /** Clears the configured environment background. */
    public void clearEnvironmentBackground() {
        environmentBackgroundRenderer.clear();
    }

    /**
     * Reports whether this renderer has an environment image configured.
     *
     * @return {@code true} when an equirectangular background image is available
     */
    public boolean hasEnvironmentBackground() {
        return environmentBackgroundRenderer.hasEquirectangularImage();
    }

    /**
     * Shows or hides the configured environment background.
     *
     * @param visible {@code true} to draw the background
     */
    public void setEnvironmentBackgroundVisible(boolean visible) {
        environmentBackgroundRenderer.setVisible(visible);
    }

    /**
     * Reports whether the environment background is visible.
     *
     * @return {@code true} when visible
     */
    public boolean isEnvironmentBackgroundVisible() {
        return environmentBackgroundRenderer.isVisible();
    }

    /**
     * Sets the background colour multiplier used by the equirectangular environment pass.
     *
     * @param intensity non-negative multiplier
     */
    public void setEnvironmentIntensity(float intensity) {
        environmentBackgroundRenderer.setIntensity(intensity);
    }

    /**
     * Returns the current environment colour multiplier.
     *
     * @return non-negative multiplier
     */
    public float getEnvironmentIntensity() {
        return environmentBackgroundRenderer.getIntensity();
    }

    /**
     * Rotates the equirectangular environment around the vertical axis.
     *
     * @param yawOffset radians added to the source longitude lookup
     */
    public void setEnvironmentYawOffset(float yawOffset) {
        environmentBackgroundRenderer.setYawOffset(yawOffset);
    }

    /**
     * Returns the current environment yaw offset.
     *
     * @return yaw offset in radians
     */
    public float getEnvironmentYawOffset() {
        return environmentBackgroundRenderer.getYawOffset();
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
            nativeCubemapUnavailableWarningLogged = false;
        } catch (IllegalStateException error) {
            nativeCubemapTarget = null;
            if (!nativeCubemapUnavailableWarningLogged) {
                LOGGER.warning("Native cubemap target unavailable: "
                        + error.getMessage());
                nativeCubemapUnavailableWarningLogged = true;
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
            LOGGER.warning("Native cubemap mipmap refresh failed; disposing native target: "
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
