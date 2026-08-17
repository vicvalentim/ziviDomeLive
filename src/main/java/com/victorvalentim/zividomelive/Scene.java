package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.support.LogManager;
import controlP5.*;
import processing.event.*;
import processing.opengl.PGraphicsOpenGL;
import java.util.logging.Logger;

/**
 * Defines the lifecycle and drawing contract for a ziviDomeLive scene.
 *
 * <p>Mutable state that must advance once per Processing frame belongs in
 * {@link #update()}. Drawing belongs in {@link #sceneRender(PGraphicsOpenGL)}.
 * Spherical capture may invoke {@code sceneRender(...)} multiple times during
 * the same Processing frame, so advancing simulation, timelines, counters or
 * mutable random state from the render callback can make spherical directions
 * observe different states.</p>
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
     * Updates mutable scene state for the current Processing frame.
     *
     * <p>Use this callback for simulation, animation counters, timelines, physics,
     * state transitions and mutable randomization that must advance once per frame.
     * Keep those mutations out of {@link #sceneRender(PGraphicsOpenGL)} when all
     * rendered views/faces must observe the same state.</p>
     */
    default void update() {
    }

    /**
     * Draws the current scene state into the supplied OpenGL render target.
     *
     * <p>Spherical capture may call this method multiple times during one Processing
     * frame. Implementations should therefore treat this callback as drawing-only
     * whenever mutable state must advance once per frame.</p>
     *
     * <p>The render target is already inside an active draw frame owned by the library;
     * scene implementations must not call {@code beginDraw()} or {@code endDraw()} here.
     * The target is supplied for this render callback and should not be retained as
     * scene-owned graphics state.</p>
     *
     * @param pg the active {@link PGraphicsOpenGL} target used to draw the scene;
     *           never {@code null} for a library-initiated render callback
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
     * @return the scene name; by default the implementation class simple name
     */
    default String getName() {
        return this.getClass().getSimpleName(); // Return class name as default
    }
}
