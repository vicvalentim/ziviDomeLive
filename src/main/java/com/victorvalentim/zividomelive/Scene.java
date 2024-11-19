package com.victorvalentim.zividomelive;

import controlP5.*;
import processing.event.*;
import processing.opengl.PGraphicsOpenGL;

/**
 * The Scene interface defines the structure for a scene in the application.
 * It includes methods for setting up the scene, rendering the scene, and handling events.
 */
public interface Scene {

	/**
	 * Sets up the scene. This method is called once when the scene is initialized.
	 */
	default void setupScene() {

	}

	/**
	 * Updates the scene. This method is called to update the scene's state.
	 */
	default void update() {

	}

	/**
     * Renders the scene using the provided PGraphics object.
     *
     * @param pg the PGraphics object used for rendering the scene
     */
    void sceneRender(PGraphicsOpenGL pg);

    /**
     * Handles key press events. Default implementation does nothing.
     *
     * @param event the KeyEvent object containing details of the key event
     */
    default void keyEvent(KeyEvent event) {
    }

    /**
     * Handles mouse events. Default implementation does nothing.
     *
     * @param event the MouseEvent object containing details of the mouse event
     */
    default void mouseEvent(MouseEvent event) {
    }

    /**
     * Handles control events. Default implementation does nothing.
     *
     * @param theEvent the ControlEvent object containing details of the control event
     */
    default void controlEvent(ControlEvent theEvent) {
    }

	/**
	 * Disposes resources used by the scene. This method is called when a scene is
	 * switched or no longer needed. By default, it ensures common cleanup actions.
	 * Override this method if a scene requires custom resource management.
	 */
	default void dispose() {
		System.out.println("Disposing resources for scene: " + getName());
	}

	/**
	 * Returns the name of the scene. Used for logging and debugging.
	 *
	 * @return the name of the scene
	 */
	default String getName() {
		return this.getClass().getSimpleName(); // Retorna o nome da classe como padrão
	}
}
