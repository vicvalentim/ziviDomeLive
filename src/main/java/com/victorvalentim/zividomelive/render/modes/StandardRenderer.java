package com.victorvalentim.zividomelive.render.modes;

import com.victorvalentim.zividomelive.Scene;
import com.victorvalentim.zividomelive.render.camera.MouseControlledCamera;
import processing.core.*;
import processing.opengl.PGraphicsOpenGL;

/**
 * The StandardRenderer class handles the rendering of a standard view using a PGraphics object.
 * It utilizes a {@link MouseControlledCamera} (quaternion-based orbit) for camera control
 * and a {@link Scene} interface for rendering the scene content.
 *
 * <p>An infinite sky background is painted before each scene render. The sky color
 * can be customised via {@link #setSkyColor(int, int, int)}.</p>
 */
public class StandardRenderer {
    private PGraphicsOpenGL standardView;
    private Scene currentScene;
    private MouseControlledCamera cam;
    private final PApplet parent;

    /** Sky background color components (R, G, B). Default: dark space blue. */
    private int skyR = 10;
    private int skyG = 10;
    private int skyB = 30;

    /**
     * Constructs a StandardRenderer with the specified parent PApplet, width, height, and current scene.
     *
     * @param parent       the parent PApplet instance
     * @param width        the width of the standard view (currently unused; lazily sized to window)
     * @param height       the height of the standard view (currently unused; lazily sized to window)
     * @param currentScene the current scene to be rendered
     */
    public StandardRenderer(PApplet parent, int width, int height, Scene currentScene) {
        this.parent = parent;
        this.currentScene = currentScene;
        this.standardView = null;
        setCam(new MouseControlledCamera());
    }

    /**
     * Initializes or reinitializes the PGraphics object for the standard view.
     */
    private void initializeStandardView(int width, int height) {
        if (standardView != null) {
            standardView.dispose();
        }
        standardView = (PGraphicsOpenGL) parent.createGraphics(width, height, PApplet.P3D);
    }

    /**
     * Sets the current scene to be rendered.
     *
     * @param newScene the new scene to be set as the current scene
     */
    public void setCurrentScene(Scene newScene) {
        this.currentScene = newScene;
    }

    /**
     * Sets the sky (infinite background) color for the Standard View.
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
     * Renders the current scene using the standard view PGraphics object.
     *
     * <p>Pipeline per frame:</p>
     * <ol>
     *   <li>Lazy-initialise the off-screen buffer if needed.</li>
     *   <li>Update camera position from quaternion + distance.</li>
     *   <li>Fill the buffer with the sky color (infinite background).</li>
     *   <li>Apply camera transform.</li>
     *   <li>Delegate to {@link Scene#sceneRender(PGraphicsOpenGL)} – scene must NOT call
     *       {@code beginDraw/endDraw}.</li>
     * </ol>
     */
    public void render() {
        if (standardView == null) {
            initializeStandardView(parent.width, parent.height);
        }

        getCam().update(parent);

        standardView.beginDraw();
        // Infinite sky: solid fill so every pixel is covered regardless of scene geometry
        standardView.background(skyR, skyG, skyB);
        getCam().apply(standardView);

        currentScene.sceneRender(standardView);

        standardView.endDraw();
    }

    /**
     * Returns the PGraphics object for the standard view.
     *
     * @return the PGraphics object representing the standard view
     */
    public PGraphicsOpenGL getStandardView() {
        if (standardView == null) {
            initializeStandardView(parent.width, parent.height);
        }
        return standardView;
    }

    /**
     * Returns the instance of the MouseControlledCamera.
     *
     * @return the MouseControlledCamera instance
     */
    public MouseControlledCamera getCam() {
        return cam;
    }

    /**
     * Sets a new instance of the MouseControlledCamera.
     *
     * @param cam the new MouseControlledCamera instance
     */
    public void setCam(MouseControlledCamera cam) {
        this.cam = cam;
    }

    /**
     * Releases the graphical resources used by the standard view.
     */
    public void dispose() {
        if (standardView != null) {
            standardView.dispose();
            standardView = null;
        }
    }
}