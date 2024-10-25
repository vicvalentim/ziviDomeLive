package com.victorvalentim.zividomelive;

import processing.core.PGraphics;
import processing.event.MouseEvent;

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
     * @param event the MouseEvent object containing details of the mouse event
     */
    void mouseEvent(MouseEvent event);
}