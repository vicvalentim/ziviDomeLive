package com.victorvalentim.zividomelive.render.modes;

import com.victorvalentim.zividomelive.Scene;
import com.victorvalentim.zividomelive.render.camera.MouseControlledCamera;
import com.victorvalentim.zividomelive.render.gl.ProcessingGlAdapter;
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
public class StandardRenderer {
    private PGraphicsOpenGL standardView;
    private Scene currentScene;
    private MouseControlledCamera cam;
    private final PApplet parent;
    private final ProcessingGlAdapter glAdapter = ProcessingGlAdapter.getDefault();

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
        this.parent       = parent;
        this.currentScene = currentScene;
        this.fixedWidth   = Math.max(0, width);
        this.fixedHeight  = Math.max(0, height);
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
        standardView = glAdapter.createGraphics(parent, width, height, PApplet.P3D);
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
        this.nearFactor = Math.max(0.0001f, nearFactor);
        this.farFactor  = Math.max(nearFactor * 10f, farFactor);
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
        float dist   = getCam().getDistance();
        float near   = Math.max(0.1f, dist * nearFactor);
        float far    = dist * farFactor;
        float aspect = (float) standardView.width / standardView.height;
        standardView.perspective(PApplet.radians(60), aspect, near, far);
        standardView.background(skyR, skyG, skyB);
        getCam().apply(standardView);

        if (currentScene != null) {
            currentScene.sceneRender(standardView);
        }

        standardView.endDraw();
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
        if (standardView != null) {
            glAdapter.dispose(standardView);
            standardView = null;
        }
    }
}
