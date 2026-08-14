package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.support.LogManager;
import controlP5.*;
import processing.event.*;
import processing.opengl.PGraphicsOpenGL;
import java.util.logging.Logger;

/**
 * The Scene interface defines the structure for a scene in the application.
 * It includes methods for setting up the scene, rendering the scene, and handling events.
 */
public interface Scene {
	/** Logger instance for scene-related logging. */
	Logger LOGGER = LogManager.getLogger();

	/**
	 * Supplies lifecycle-aware API services before this scene is set up.
	 *
	 * <p>The default implementation preserves existing sketches. Service-aware scenes may
	 * retain the provided context until their matching {@link #dispose()} call.</p>
	 *
	 * @param services services owned by this scene activation
	 */
	default void configure(SceneServices services) {
	}

	/**
	 * Sets up the scene when it becomes active.
	 *
	 * <p>A scene may be activated more than once. Each activation after a switch follows a
	 * corresponding {@link #dispose()} call.</p>
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
     * The render target is already inside an active draw frame owned by the library,
     * so scene implementations must NOT call beginDraw()/endDraw() here.
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
	 * Disposes resources used by the scene. This method is called when an active scene is
	 * switched, cleared, replaced, or released by the facade. By default, it logs the transition.
	 * Override this method if a scene requires custom resource management.
	 */
	default void dispose() {
		LOGGER.info("Disposing resources for scene: " + getName());
	}

	/**
	 * Returns the name of the scene. Used for logging and debugging.
	 *
	 * @return the name of the scene
	 */
	default String getName() {
		return this.getClass().getSimpleName(); // Return class name as default
	}
}
