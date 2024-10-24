package com.victorvalentim.zividomelive;

import processing.core.PGraphics;

/**
 * The Scene interface defines the structure for a scene in the application.
 * It includes methods for setting up the scene, rendering the scene, and handling key press events.
 */
public interface Scene {

    /**
     * Sets up the scene. This method is called once when the scene is initialized.
     */
    void setupScene();

    /**
     * Renders the scene using the provided PGraphics object.
     *
     * @param pg the PGraphics object used for rendering the scene
     */
    void sceneRender(PGraphics pg);

    /**
     * Handles key press events.
     *
     * @param key the character representing the key that was pressed
     */
    void keyPressed(char key);

    /**
     * Handles mouse events.
     *
     * @param mouseX the x-coordinate of the mouse
     * @param mouseY the y-coordinate of the mouse
     * @param button the mouse button that was pressed
     */
    void mouseEvent(int mouseX, int mouseY, int button);
}