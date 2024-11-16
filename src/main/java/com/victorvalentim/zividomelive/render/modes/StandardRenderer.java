package com.victorvalentim.zividomelive.render.modes;

import com.victorvalentim.zividomelive.Scene;
import com.victorvalentim.zividomelive.render.camera.MouseControlledCamera;
import processing.core.*;

/**
 * The StandardRenderer class handles the rendering of a standard view using a PGraphics object.
 * It utilizes a MouseControlledCamera for camera control and a Scene interface for rendering the scene.
 */
public class StandardRenderer {
    private PGraphics standardView;
    private Scene currentScene;
    private MouseControlledCamera cam;
    private final PApplet parent;

    /**
     * Constructs a StandardRenderer with the specified parent PApplet, width, height, and current scene.
     *
     * @param parent the parent PApplet instance
     * @param width the width of the standard view
     * @param height the height of the standard view
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
        standardView = parent.createGraphics(width, height, PApplet.P3D);
    }

    /**
     * Sets the current scene to be rendered.
     * @param newScene the new scene to be set as the current scene
     */
    public void setCurrentScene(Scene newScene) {
        this.currentScene = newScene;
    }

    /**
     * Renders the current scene using the standard view PGraphics object.
     * Updates the camera and applies its settings before rendering the scene.
     */
	public void render() {
        if (standardView == null) {
            initializeStandardView(parent.width, parent.height);
        }

        getCam().update(parent);

        standardView.beginDraw();
        standardView.background(0, 0);

        getCam().apply(standardView);

        currentScene.sceneRender(standardView);

        standardView.endDraw();
    }

    /**
     * Returns the PGraphics object for the standard view.
     *
     * @return the PGraphics object representing the standard view
     */
	public PGraphics getStandardView() {
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