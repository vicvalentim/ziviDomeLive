package com.victorvalentim.zividomelive;

// Package-private implementation grouped physically under _internal/render/core.

import com.victorvalentim.zividomelive.Scene;
import com.victorvalentim.zividomelive.render.Quaternion;
import com.victorvalentim.zividomelive.render.SphericalOrientation;
import com.victorvalentim.zividomelive.performance.PerformanceMetric;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PMatrix3D;
import processing.opengl.FrameBuffer;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

import java.util.logging.Logger;
import java.util.function.Consumer;

/**
 * Captures a scene into a native OpenGL cubemap texture.
 *
 * <p>The renderer preserves the qualified 1.5 camera-orientation contract while using
 * one reusable Processing OpenGL scratch target. Each rendered scratch face is copied
 * GPU-to-GPU into the corresponding {@code GL_TEXTURE_CUBE_MAP} face.</p>
 *
 * <p>This avoids adapting the scene camera itself to native cubemap framebuffer
 * conventions. Any framebuffer-origin conversion is isolated to the blit step.</p>
 */
class CubemapRenderer implements PConstants {
    private static final float DEFAULT_NEAR_PLANE = 1.0f;
    private static final float DEFAULT_FAR_PLANE = 1122000.0f;
    private static final Logger LOGGER = LogManager.getLogger();

    private PGraphicsOpenGL nativeCaptureGraphics;
    private CubemapTarget nativeCubemapTarget;
    private final SphericalEnvironmentNativePass sphericalEnvironmentPass;
    private final EnvironmentState environmentState;
    private final boolean ownsEnvironmentState;
    private int resolution;
    private final PApplet parent;
    private final ProcessingGlAdapter glAdapter = ProcessingGlAdapter.getDefault();
    private boolean nativeCubemapUnavailableWarningLogged;

    // Cached frustum parameters
    private volatile float cachedNearPlane;
    private volatile float cachedFarPlane;
    private volatile float cachedFieldOfView;

    private final SphericalOrientation angleOrientation = new SphericalOrientation();
    private final CameraManager defaultCameraManager = new CameraManager();
    private final PMatrix3D captureOrientationMatrix = new PMatrix3D();
    private final PMatrix3D environmentOrientationMatrix = new PMatrix3D();
    private final Consumer<PGL> resolvedFaceBlit = this::blitResolvedFace;
    private int resolvedSourceFramebufferId;

    /**
     * Constructs a CubemapRenderer with the specified initial resolution and parent PApplet.
     *
     * @param initialResolution the initial resolution for cubemap faces
     * @param parent the parent PApplet instance
     */
    public CubemapRenderer(int initialResolution, PApplet parent) {
        this(initialResolution, parent, new EnvironmentState(), true);
    }

    /**
     * Constructs a cubemap renderer consuming a shared Environment state.
     *
     * @param initialResolution initial face resolution
     * @param parent Processing parent
     * @param environmentState shared borrowed environment state
     */
    public CubemapRenderer(
            int initialResolution,
            PApplet parent,
            EnvironmentState environmentState) {
        this(initialResolution, parent, environmentState, false);
    }

    private CubemapRenderer(
            int initialResolution,
            PApplet parent,
            EnvironmentState environmentState,
            boolean ownsEnvironmentState) {
        this.parent = parent;
        this.resolution = initialResolution;
        this.environmentState = environmentState;
        this.sphericalEnvironmentPass =
                new SphericalEnvironmentNativePass(parent, environmentState);
        this.ownsEnvironmentState = ownsEnvironmentState;
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
     * Configures one Processing scratch render using the qualified 1.5 camera contract.
     *
     * <p>No native cubemap handedness correction is applied to the scene matrix here.
     * The camera orientation comes directly from {@link CameraManager} /
     * {@link CameraOrientation}; framebuffer-origin conversion is handled only during
     * the GPU blit into the native cubemap face.</p>
     */
    private void configureCameraForFace(
            PGraphicsOpenGL pg,
            CameraOrientation orientation,
            PMatrix3D sphericalOrientationMatrix,
            float fieldOfView) {
        pg.camera(
                0f, 0f, 0f,
                orientation.centerX,
                orientation.centerY,
                orientation.centerZ,
                orientation.upX,
                orientation.upY,
                orientation.upZ);

        pg.perspective(
                fieldOfView,
                1.0f,
                cachedNearPlane,
                cachedFarPlane);

        pg.applyMatrix(sphericalOrientationMatrix);
    }

    /**
     * Captures the cubemap faces based on the camera orientation.
     *
     * @param pitch rotation around the X axis
     * @param yaw rotation around the Z axis
     * @param roll rotation around the Y axis
     * @param cameraManager manager for camera orientations
     * @param currentScene the current scene to render
     */
    public void captureCubemap(
            float pitch,
            float yaw,
            float roll,
            CameraManager cameraManager,
            Scene currentScene) {
        angleOrientation.setPitch(pitch);
        angleOrientation.setYaw(yaw);
        angleOrientation.setRoll(roll);
        captureCubemap(
                angleOrientation.getQuaternion(),
                cameraManager,
                currentScene);
    }

    /**
     * Captures the cubemap faces using the default qualified camera orientations.
     *
     * @param pitch rotation around the X axis
     * @param yaw rotation around the Z axis
     * @param roll rotation around the Y axis
     * @param currentScene the current scene to render
     */
    public void captureCubemap(
            float pitch,
            float yaw,
            float roll,
            Scene currentScene) {
        angleOrientation.setPitch(pitch);
        angleOrientation.setYaw(yaw);
        angleOrientation.setRoll(roll);
        captureCubemap(
                angleOrientation.getQuaternion(),
                defaultCameraManager,
                currentScene);
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

        captureNativeCubemap(
                effectiveOrientation,
                cameraManager,
                currentScene);
    }

    /**
     * Captures the cubemap faces using the default qualified camera orientations.
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

        captureNativeCubemap(
                effectiveOrientation,
                defaultCameraManager,
                currentScene);
    }

    /**
     * Renders each canonical 1.5 camera orientation into one reusable Processing scratch
     * target and copies the resolved color framebuffer into the matching native cubemap face.
     */
    private boolean captureNativeCubemap(
            Quaternion effectiveOrientation,
            CameraManager cameraManager,
            Scene currentScene) {
        PerformanceMonitor monitor = PerformanceMonitor.current();
        boolean profiling = monitor != null && monitor.isEnabled();
        long captureStarted = profiling ? monitor.start() : 0L;
        try {
            return captureNativeCubemapFrame(
                    effectiveOrientation,
                    cameraManager,
                    currentScene,
                    monitor,
                    profiling);
        } finally {
            if (profiling) monitor.record(PerformanceMetric.CUBEMAP_TOTAL, captureStarted);
        }
    }

    private boolean captureNativeCubemapFrame(
            Quaternion effectiveOrientation,
            CameraManager cameraManager,
            Scene currentScene,
            PerformanceMonitor monitor,
            boolean profiling) {
        if (nativeCubemapTarget != null
                && !nativeCubemapTarget.isValidInCurrentContext()) {
            LOGGER.warning("OpenGL context changed; recreating native cubemap resources.");
            nativeCubemapTarget.abandonAfterContextLoss();
            nativeCubemapTarget = null;
            disposeNativeCaptureGraphics();
            initializeNativeCubemapTarget();
        }
        if (nativeCubemapTarget == null) {
            if (!nativeCubemapUnavailableWarningLogged) {
                LOGGER.warning(
                        "Native cubemap capture unavailable; spherical frames will be skipped until resources are recreated.");
                nativeCubemapUnavailableWarningLogged = true;
            }
            return false;
        }

        PGraphicsOpenGL captureGraphics = ensureNativeCaptureGraphics();
        if (captureGraphics == null) {
            return false;
        }

        CameraManager effectiveCameraManager =
                cameraManager != null
                        ? cameraManager
                        : defaultCameraManager;

        float captureFieldOfView = cachedFieldOfView;
        effectiveOrientation.toMatrix(captureOrientationMatrix);
        composeEnvironmentOrientation(
                effectiveOrientation,
                environmentState.getSceneCameraOrientation())
                .toMatrix(environmentOrientationMatrix);

        try {
            for (int faceIndex = 0; faceIndex < CubemapFace.count(); faceIndex++) {
                CubemapFace face = CubemapFace.at(faceIndex);
                long faceStarted = profiling ? monitor.start() : 0L;
                try {
                    renderNativeCubemapFace(
                            captureGraphics,
                            face,
                            effectiveCameraManager.getOrientation(face.index()),
                            currentScene,
                            captureFieldOfView,
                            monitor,
                            profiling);
                } finally {
                    if (profiling) monitor.record(faceMetric(face), faceStarted);
                }
            }

            refreshNativeCubemapMipmaps();
            nativeCubemapUnavailableWarningLogged = false;
            return true;
        } catch (RuntimeException error) {
            LOGGER.warning(
                    "Native cubemap capture failed; spherical frame skipped: "
                            + error.getMessage());
            disposeNativeCubemapTarget();
            return false;
        }
    }

    private void renderNativeCubemapFace(
            PGraphicsOpenGL captureGraphics,
            CubemapFace face,
            CameraOrientation orientation,
            Scene currentScene,
            float captureFieldOfView,
            PerformanceMonitor monitor,
            boolean profiling) {
        captureGraphics.beginDraw();
        try {
            captureGraphics.resetMatrix();
            captureGraphics.background(0, 0);
            configureCameraForFace(
                    captureGraphics,
                    orientation,
                    captureOrientationMatrix,
                    captureFieldOfView);

            if (currentScene != null) {
                long sceneStarted = profiling ? monitor.start() : 0L;
                try {
                    currentScene.sceneRender(captureGraphics);
                } finally {
                    if (profiling) monitor.record(PerformanceMetric.SCENE_RENDER, sceneStarted);
                }
            }

            captureGraphics.noLights();
            captureGraphics.flush();
            sphericalEnvironmentPass.renderScratchCubemapFace(
                    captureGraphics,
                    face,
                    environmentOrientationMatrix);

            /* Keep Processing's resolved offscreen color framebuffer current under MSAA. */
            captureGraphics.loadTexture();
            FrameBuffer sourceFramebuffer = captureGraphics.getFrameBuffer(false);
            if (sourceFramebuffer == null || sourceFramebuffer.glFbo == 0) {
                throw new IllegalStateException(
                        "Processing cubemap scratch framebuffer is unavailable.");
            }

            long blitStarted = profiling ? monitor.start() : 0L;
            try {
                resolvedSourceFramebufferId = sourceFramebuffer.glFbo;
                nativeCubemapTarget.renderFace(face, captureGraphics, resolvedFaceBlit);
            } finally {
                resolvedSourceFramebufferId = 0;
                if (profiling) monitor.record(PerformanceMetric.CUBEMAP_BLIT, blitStarted);
            }
        } finally {
            captureGraphics.endDraw();
        }
    }

    private void blitResolvedFace(PGL pgl) {
        /* renderFace() owns DRAW; bind only READ to the Processing scratch framebuffer. */
        pgl.bindFramebuffer(PGL.READ_FRAMEBUFFER, resolvedSourceFramebufferId);
        pgl.readBuffer(PGL.COLOR_ATTACHMENT0);
        /* Flip only framebuffer Y during the GPU-to-GPU transfer. */
        pgl.blitFramebuffer(
                0,
                0,
                resolution,
                resolution,
                0,
                resolution,
                resolution,
                0,
                PGL.COLOR_BUFFER_BIT,
                PGL.NEAREST);
    }

    /**
     * Returns the native cubemap target populated by GPU scratch-to-cubemap copies.
     *
     * @return native cubemap target, or {@code null} when unsupported/unavailable
     */
    public CubemapTarget getNativeCubemapTarget() {
        return nativeCubemapTarget;
    }

    /**
     * Reports whether the renderer currently owns an allocated native cubemap.
     *
     * @return {@code true} when {@code GL_TEXTURE_CUBE_MAP} capture is available
     */
    public boolean hasNativeCubemapTarget() {
        return nativeCubemapTarget != null && nativeCubemapTarget.isAllocated();
    }

    /**
     * Sets the LDR equirectangular environment image rendered behind spherical capture.
     *
     * <p>The library draws the environment at far depth after the scene has populated the
     * reusable scratch target, then transfers the combined colour to the native cubemap face.</p>
     *
     * @param image Processing image, or {@code null} to clear the environment
     */
    public void setEquirectangularBackground(PImage image) {
        environmentState.setLdrEquirectangularSource(image);
    }

    /** Clears the configured environment background. */
    public void clearEnvironmentBackground() {
        environmentState.clearSource();
    }

    /**
     * Reports whether this renderer has an environment image configured.
     *
     * @return {@code true} when an equirectangular background image is available
     */
    public boolean hasEnvironmentBackground() {
        return environmentState.hasSource();
    }

    /**
     * Shows or hides the configured environment background.
     *
     * @param visible {@code true} to draw the background
     */
    public void setEnvironmentBackgroundVisible(boolean visible) {
        environmentState.setVisible(visible);
    }

    /**
     * Reports whether the environment background is visible.
     *
     * @return {@code true} when visible
     */
    public boolean isEnvironmentBackgroundVisible() {
        return environmentState.isVisible();
    }

    /**
     * Sets the background colour multiplier used by the equirectangular environment pass.
     *
     * @param intensity non-negative multiplier
     */
    public void setEnvironmentIntensity(float intensity) {
        environmentState.setIntensity(intensity);
    }

    /**
     * Returns the current environment colour multiplier.
     *
     * @return non-negative multiplier
     */
    public float getEnvironmentIntensity() {
        return environmentState.getIntensity();
    }

    /**
     * Rotates the equirectangular environment around the vertical axis.
     *
     * @param yawOffset rotation offset, in radians, applied to the source longitude lookup
     */
    public void setEnvironmentYawOffset(float yawOffset) {
        environmentState.setYawOffset(yawOffset);
    }

    /**
     * Returns the current environment yaw offset.
     *
     * @return yaw offset in radians
     */
    public float getEnvironmentYawOffset() {
        return environmentState.getYawOffset();
    }

    /**
     * Returns the logical Environment state consumed by this renderer.
     * @return logical Environment state
     */
    public EnvironmentState getEnvironmentState() {
        return environmentState;
    }

    static Quaternion composeEnvironmentOrientation(
            Quaternion sphericalOrientation,
            Quaternion sceneCameraOrientation) {
        Quaternion spherical = sphericalOrientation == null
                ? new Quaternion(0.0f, 0.0f, 0.0f, 1.0f)
                : sphericalOrientation;
        Quaternion sceneCamera = sceneCameraOrientation == null
                ? new Quaternion(0.0f, 0.0f, 0.0f, 1.0f)
                : sceneCameraOrientation;
        return spherical.multiply(sceneCamera).normalized();
    }

    /**
     * Disposes native cubemap capture resources.
     */
    public void dispose() {
        sphericalEnvironmentPass.dispose();
        if (ownsEnvironmentState) {
            environmentState.clearSource();
        }
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
                LOGGER.warning(
                        "Native cubemap target unavailable: "
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
            nativeCaptureGraphics =
                    glAdapter.createGraphics(
                            parent,
                            resolution,
                            resolution,
                            P3D);

            if (LogManager.isDebugEnabled()) {
                LOGGER.fine(
                        "Native cubemap capture graphics allocated: resolution="
                                + resolution
                                + ", renderer="
                                + nativeCaptureGraphics.getClass().getSimpleName());
            }
            return nativeCaptureGraphics;
        } catch (RuntimeException error) {
            LOGGER.warning(
                    "Native cubemap capture graphics unavailable: "
                            + error.getMessage());
            nativeCaptureGraphics = null;
            disposeNativeCubemapTarget();
            return null;
        }
    }

    private void refreshNativeCubemapMipmaps() {
        if (nativeCubemapTarget == null
                || nativeCubemapTarget.hasValidMipmaps()) {
            return;
        }

        PerformanceMonitor monitor = PerformanceMonitor.current();
        boolean profiling = monitor != null && monitor.isEnabled();
        long started = profiling ? monitor.start() : 0L;
        try {
            nativeCubemapTarget.generateMipmaps();
        } catch (RuntimeException error) {
            LOGGER.warning(
                    "Native cubemap mipmap refresh failed; disposing native target: "
                            + error.getMessage());
            disposeNativeCubemapTarget();
        } finally {
            if (profiling) monitor.record(PerformanceMetric.CUBEMAP_MIPMAP, started);
        }
    }

    private static PerformanceMetric faceMetric(CubemapFace face) {
        return switch (face) {
            case POSITIVE_X -> PerformanceMetric.CUBEMAP_POS_X;
            case NEGATIVE_X -> PerformanceMetric.CUBEMAP_NEG_X;
            case POSITIVE_Y -> PerformanceMetric.CUBEMAP_POS_Y;
            case NEGATIVE_Y -> PerformanceMetric.CUBEMAP_NEG_Y;
            case POSITIVE_Z -> PerformanceMetric.CUBEMAP_POS_Z;
            case NEGATIVE_Z -> PerformanceMetric.CUBEMAP_NEG_Z;
        };
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
