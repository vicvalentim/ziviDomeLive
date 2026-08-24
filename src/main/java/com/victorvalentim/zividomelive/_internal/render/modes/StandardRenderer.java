package com.victorvalentim.zividomelive;

// Package-private implementation grouped physically under _internal/render/modes.

import com.victorvalentim.zividomelive.Scene;
import com.victorvalentim.zividomelive.performance.PerformanceMetric;
import processing.core.*;
import processing.opengl.PGraphicsOpenGL;

/**
 * Renders a standard free-perspective view into an off-screen {@link PGraphicsOpenGL} buffer.
 *
 * <p>Two operating modes are supported:</p>
 * <ul>
 *   <li><b>Dynamic (preview)</b>: constructed with {@code width=0, height=0}. The buffer is
 *       sized to {@code parent.width × parent.height} and reallocated automatically when the
 *       Processing window changes dimensions, preserving the window's aspect ratio.</li>
 *   <li><b>Fixed (output)</b>: constructed with positive {@code width} and {@code height}. The
 *       buffer is pre-allocated immediately at those dimensions and is never resized
 *       automatically. Use {@code outputResolution × outputResolution} for a square output
 *       buffer consistent with the rest of the high-resolution output pipeline.</li>
 * </ul>
 *
 * <p>A {@link MouseControlledCamera} (quaternion-based orbit) drives the view. Preview and
 * output instances can share the same camera via {@link #setCam(MouseControlledCamera)} so that
 * both always show the same framing at different resolutions.</p>
 *
 * <p>An infinite sky background is painted before each scene render. The sky colour can be
 * customised via {@link #setSkyColor(int, int, int)}.</p>
 *
 * <p>This renderer must only be driven from the Processing draw thread. It calls
 * {@code beginDraw()} and {@code endDraw()} internally; {@link Scene#sceneRender} must
 * <em>not</em> call those methods.</p>
 */
class StandardRenderer {
    private static final int SHAPE_ANTIALIAS_SAMPLES = 4;
    private PGraphicsOpenGL standardView;
    private Scene currentScene;
    private MouseControlledCamera cam;
    private final PApplet parent;
    private final ProcessingGlAdapter glAdapter = ProcessingGlAdapter.getDefault();
    private final EnvironmentBackgroundRenderer environmentBackgroundRenderer;
    private final boolean ownsEnvironmentState;
    private static final float VERTICAL_FOV_RADIANS = PApplet.radians(60.0f);

    /**
     * Fixed buffer width. Zero signals dynamic mode ({@code parent.width} is used each frame).
     */
    private final int fixedWidth;

    /**
     * Fixed buffer height. Zero signals dynamic mode ({@code parent.height} is used each frame).
     */
    private final int fixedHeight;

    /** Sky background colour (R, G, B). Default: dark space blue. */
    private int skyR = 10;
    private int skyG = 10;
    private int skyB = 30;

    /**
     * Near clipping plane multiplier. {@code near = distance * nearFactor}.
     */
    private float nearFactor = 0.001f;

    /**
     * Far clipping plane multiplier. {@code far = distance * farFactor}.
     */
    private float farFactor = 2000f;

    private static final float MIN_NEAR_FACTOR = 0.0001f;
    private static final float DEFAULT_NEAR_FACTOR = 0.001f;
    private static final float MAX_CLIP_FACTOR = 1_000_000f;

    /**
     * Constructs a {@code StandardRenderer}.
     *
     * <p>Pass {@code width=0, height=0} for a <em>dynamic</em> buffer that follows the
     * Processing window dimensions (preview mode). Pass positive values to lock the buffer
     * to exact pixel dimensions (output mode); the FBO is pre-allocated immediately so that
     * Syphon or Spout can obtain a valid texture reference during backend initialisation.</p>
     *
     * @param parent       the parent {@link PApplet} instance; must not be {@code null}
     * @param width        desired buffer width, or {@code 0} to use {@code parent.width}
     *                     dynamically
     * @param height       desired buffer height, or {@code 0} to use {@code parent.height}
     *                     dynamically
     * @param currentScene the initial scene to render; may be {@code null}
     */
    public StandardRenderer(PApplet parent, int width, int height, Scene currentScene) {
        this(parent, width, height, currentScene, new EnvironmentState(), true);
    }

    /**
     * Constructs a Standard renderer consuming a shared logical Environment state.
     *
     * @param parent Processing parent
     * @param width fixed width, or zero for dynamic preview sizing
     * @param height fixed height, or zero for dynamic preview sizing
     * @param currentScene initial scene
     * @param environmentState shared borrowed environment state
     */
    public StandardRenderer(
            PApplet parent,
            int width,
            int height,
            Scene currentScene,
            EnvironmentState environmentState) {
        this(parent, width, height, currentScene, environmentState, false);
    }

    private StandardRenderer(
            PApplet parent,
            int width,
            int height,
            Scene currentScene,
            EnvironmentState environmentState,
            boolean ownsEnvironmentState) {
        this.parent       = parent;
        this.currentScene = currentScene;
        this.fixedWidth   = Math.max(0, width);
        this.fixedHeight  = Math.max(0, height);
        this.environmentBackgroundRenderer =
                new EnvironmentBackgroundRenderer(parent, environmentState);
        this.ownsEnvironmentState = ownsEnvironmentState;
        setCam(new MouseControlledCamera());

        // Pre-allocate when fixed dimensions are provided so that the FBO exists before
        // Syphon or Spout may request a texture reference during backend initialisation.
        if (this.fixedWidth > 0 && this.fixedHeight > 0) {
            initializeStandardView(this.fixedWidth, this.fixedHeight);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Returns the target buffer width for the current frame. */
    private int effectiveWidth() {
        return fixedWidth > 0 ? fixedWidth : parent.width;
    }

    /** Returns the target buffer height for the current frame. */
    private int effectiveHeight() {
        return fixedHeight > 0 ? fixedHeight : parent.height;
    }

    /** Allocates or reallocates the off-screen buffer at the requested dimensions. */
    private void initializeStandardView(int width, int height) {
        if (standardView != null) {
            glAdapter.dispose(standardView);
        }
        standardView = glAdapter.createGraphics(
                parent, width, height, PApplet.P3D, SHAPE_ANTIALIAS_SAMPLES);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Sets the current scene to be rendered.
     *
     * @param newScene the scene to render; may be {@code null}
     */
    public void setCurrentScene(Scene newScene) {
        this.currentScene = newScene;
    }

    /**
     * Sets the sky (infinite background) colour.
     *
     * @param r red component (0–255)
     * @param g green component (0–255)
     * @param b blue component (0–255)
     */
    public void setSkyColor(int r, int g, int b) {
        this.skyR = r;
        this.skyG = g;
        this.skyB = b;
    }

    /**
     * Configures the clipping plane multipliers used to compute near/far each frame.
     *
     * <p>{@code near = distance * nearFactor}, {@code far = distance * farFactor}.
     * Increase {@code farFactor} when distant objects disappear; decrease
     * {@code nearFactor} only if very close geometry clips unexpectedly.</p>
     *
     * @param nearFactor multiplier for near plane (default {@code 0.001})
     * @param farFactor  multiplier for far plane  (default {@code 2000})
     */
    public void setClipFactors(float nearFactor, float farFactor) {
        float effectiveNear = Float.isFinite(nearFactor)
                ? Math.max(MIN_NEAR_FACTOR, Math.min(MAX_CLIP_FACTOR / 10f, nearFactor))
                : DEFAULT_NEAR_FACTOR;
        float minimumFar = effectiveNear * 10f;
        float effectiveFar = Float.isFinite(farFactor)
                ? Math.max(minimumFar, Math.min(MAX_CLIP_FACTOR, farFactor))
                : minimumFar;
        this.nearFactor = effectiveNear;
        this.farFactor = effectiveFar;
    }

    static int shapeAntialiasSamples() {
        return SHAPE_ANTIALIAS_SAMPLES;
    }

    /**
     * Renders the current scene into the off-screen buffer.
     *
     * <p>For <em>dynamic</em> (preview) renderers the buffer is reallocated automatically
     * when the Processing window dimensions change. For <em>fixed</em> (output) renderers
     * the dimensions are permanent.</p>
     *
     * <p>Pipeline per frame:</p>
     * <ol>
     *   <li>Allocate or reallocate the off-screen buffer if dimensions changed.</li>
     *   <li>Update camera position from quaternion + distance.</li>
     *   <li>Fill the buffer with the sky colour (infinite background).</li>
     *   <li>Apply camera transform.</li>
     *   <li>Delegate to {@link Scene#sceneRender(PGraphicsOpenGL)} — the scene must
     *       <em>not</em> call {@code beginDraw()}/{@code endDraw()}.</li>
     * </ol>
     *
     * <p>Must be called from the Processing draw thread.</p>
     */
    public void render() {
        int w = effectiveWidth();
        int h = effectiveHeight();

        // Dynamic renderers: reallocate when the window has been resized.
        if (standardView == null || standardView.width != w || standardView.height != h) {
            initializeStandardView(w, h);
        }

        getCam().update(parent);

        standardView.beginDraw();
        try {
            float dist   = getCam().getDistance();
            float near   = Math.max(0.1f, dist * nearFactor);
            float far    = dist * farFactor;
            float aspect = (float) standardView.width / standardView.height;
            standardView.perspective(VERTICAL_FOV_RADIANS, aspect, near, far);
            standardView.background(skyR, skyG, skyB);
            getCam().apply(standardView);

            if (currentScene != null) {
                PerformanceMonitor monitor = PerformanceMonitor.current();
                boolean profiling = monitor != null && monitor.isEnabled();
                long started = profiling ? monitor.start() : 0L;
                try {
                    currentScene.sceneRender(standardView);
                } finally {
                    if (profiling) monitor.record(PerformanceMetric.SCENE_RENDER, started);
                }
            }

            if (environmentBackgroundRenderer.isVisible()
                    && environmentBackgroundRenderer.hasEquirectangularImage()) {
                standardView.noLights();
                standardView.flush();
                environmentBackgroundRenderer.renderStandard(standardView);
            }
        } finally {
            standardView.endDraw();
        }
    }

    /**
     * Sets the borrowed LDR Environment source for direct renderer integrations.
     * @param image borrowed source, or {@code null}
     */
    public void setEquirectangularBackground(PImage image) {
        environmentBackgroundRenderer.setEquirectangularImage(image);
    }

    /** Clears the borrowed Environment source. */
    public void clearEnvironmentBackground() {
        environmentBackgroundRenderer.clear();
    }

    /**
     * Returns whether an Environment source is configured.
     * @return {@code true} when a source is configured
     */
    public boolean hasEnvironmentBackground() {
        return environmentBackgroundRenderer.hasEquirectangularImage();
    }

    /**
     * Shows or hides the Environment background.
     * @param visible {@code true} to draw the Environment
     */
    public void setEnvironmentBackgroundVisible(boolean visible) {
        environmentBackgroundRenderer.setVisible(visible);
    }

    /**
     * Returns whether the Environment background is visible.
     * @return {@code true} when visible
     */
    public boolean isEnvironmentBackgroundVisible() {
        return environmentBackgroundRenderer.isVisible();
    }

    /**
     * Sets the non-negative visual Environment multiplier.
     * @param intensity visual colour multiplier
     */
    public void setEnvironmentIntensity(float intensity) {
        environmentBackgroundRenderer.setIntensity(intensity);
    }

    /**
     * Returns the visual Environment multiplier.
     * @return visual colour multiplier
     */
    public float getEnvironmentIntensity() {
        return environmentBackgroundRenderer.getIntensity();
    }

    /**
     * Sets the Environment source-longitude offset in radians.
     * @param yawOffset source-longitude offset in radians
     */
    public void setEnvironmentYawOffset(float yawOffset) {
        environmentBackgroundRenderer.setYawOffset(yawOffset);
    }

    /**
     * Returns the Environment source-longitude offset in radians.
     * @return source-longitude offset in radians
     */
    public float getEnvironmentYawOffset() {
        return environmentBackgroundRenderer.getYawOffset();
    }

    /**
     * Returns the shared logical Environment state.
     * @return logical Environment state
     */
    public EnvironmentState getEnvironmentState() {
        return environmentBackgroundRenderer.getEnvironmentState();
    }

    /**
     * Returns the off-screen buffer, allocating or reallocating it on demand when necessary.
     *
     * <p>For dynamic renderers a new buffer is created whenever the Processing window
     * dimensions differ from the current buffer dimensions.</p>
     *
     * @return the {@link PGraphicsOpenGL} buffer; never {@code null} after this call
     */
    public PGraphicsOpenGL getStandardView() {
        int w = effectiveWidth();
        int h = effectiveHeight();
        if (standardView == null || standardView.width != w || standardView.height != h) {
            initializeStandardView(w, h);
        }
        return standardView;
    }

    /**
     * Returns the {@link MouseControlledCamera} driving this renderer.
     *
     * @return the camera; never {@code null}
     */
    public MouseControlledCamera getCam() {
        return cam;
    }

    /**
     * Replaces the camera used by this renderer.
     *
     * <p>Preview and output {@code StandardRenderer} instances can share a single camera so
     * that both always show the same framing at different resolutions:</p>
     * <pre>{@code
     * standardRendererPreview.setCam(standardRenderer.getCam());
     * }</pre>
     *
     * @param cam the new camera; must not be {@code null}
     */
    public void setCam(MouseControlledCamera cam) {
        this.cam = cam;
    }

    /**
     * Releases the graphical resources used by this renderer.
     *
     * <p>After disposal {@link #getStandardView()} will allocate a fresh buffer on demand.</p>
     */
    public void dispose() {
        environmentBackgroundRenderer.dispose();
        if (ownsEnvironmentState) {
            environmentBackgroundRenderer.clear();
        }
        if (standardView != null) {
            glAdapter.dispose(standardView);
            standardView = null;
        }
    }

}
