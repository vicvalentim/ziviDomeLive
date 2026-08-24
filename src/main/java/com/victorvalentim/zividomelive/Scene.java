package com.victorvalentim.zividomelive;

import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;

/**
 * Defines the lifecycle and drawing contract for a ziviDomeLive scene.
 *
 * <p>Mutable state that must advance once per Processing frame belongs in
 * {@link #update()}. Drawing belongs in {@link #sceneRender(PGraphicsOpenGL)}.
 * Spherical capture may invoke {@code sceneRender(...)} multiple times during
 * the same Processing frame, so advancing simulation, timelines, counters or
 * mutable random state from the render callback can make spherical directions
 * observe different states.</p>
 *
 * <p>The library owns the supplied render target's {@code beginDraw()}/{@code endDraw()} pair.
 * A scene must neither call those methods nor retain the target after the callback.</p>
 *
 * <p><strong>API stability:</strong> Stable.</p>
 */
public interface Scene {
    /**
     * Supplies lifecycle-aware API services before this scene is set up.
     *
     * <p>The runtime calls this before the matching {@link #setupScene()} on every activation,
     * including reloads of the same scene instance. The supplied context belongs to that one
     * activation and may be retained only until the matching {@link #dispose()} returns.</p>
     *
     * @param services services owned by this scene activation
     */
    default void configure(SceneServices services) {
    }

    /**
     * Sets up the scene when it becomes active.
     *
     * <p>A scene instance may be activated more than once. Every activation receives fresh
     * services through {@link #configure(SceneServices)} and follows a complete setup/dispose
     * cycle.</p>
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
     *
     * <p>The runtime invokes this on the Processing frame thread after activation callbacks,
     * bounded external input, and the frame clock have been advanced.</p>
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
     * Handles a Processing key event. The default implementation does nothing.
     *
     * <p>Named {@link SceneActionMap} bindings run first; the raw callback remains available for
     * direct Processing-style input.</p>
     *
     * @param event the KeyEvent object containing details of the key event
     */
    default void keyEvent(KeyEvent event) {
    }

    /**
     * Handles a Processing mouse event. The default implementation does nothing.
     *
     * <p>Named {@link SceneActionMap} bindings run first, this raw callback runs next, and
     * library-owned camera navigation is routed afterward.</p>
     *
     * @param event the MouseEvent object containing details of the mouse event
     */
    default void mouseEvent(MouseEvent event) {
    }

    /**
     * Ends one active setup cycle and releases scene-owned domain resources.
     *
     * <p>Before this callback, the runtime stops accepting activation work and cancels
     * activation-owned tasks. After it returns, the runtime releases services and adapters.
     * Scenes must not close runtime-supplied services themselves. The same Java scene object may
     * be configured and activated again later.</p>
     */
    default void dispose() {
    }

    /**
     * Returns the artist-facing scene name used by logging and scene controls.
     *
     * @return the scene name; by default the implementation class simple name
     */
    default String getName() {
        return this.getClass().getSimpleName(); // Return class name as default
    }
}
