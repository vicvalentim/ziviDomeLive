package com.victorvalentim.zividomelive;

import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Activation-scoped named actions and Processing input bindings.
 *
 * <p>Key and mouse bindings run on the Processing frame thread before the matching raw
 * {@link Scene} callback. Programmatic {@link #trigger(String)} calls run synchronously on the
 * caller's thread, so scene code should trigger actions from Processing callbacks.</p>
 *
 * <p>The runtime clears all bindings when the activation ends.</p>
 *
 * <p><strong>API stability:</strong> Advanced Stable.</p>
 *
 * @since 2.0.0
 */
public final class SceneActionMap {

    private final Map<String, Runnable> actions = new LinkedHashMap<>();
    private final Map<Character, String> pressedKeys = new LinkedHashMap<>();
    private final Map<Integer, String> pressedKeyCodes = new LinkedHashMap<>();
    private final Map<Integer, MouseBinding> mouseActions = new LinkedHashMap<>();
    private boolean closed;

    SceneActionMap() {
    }

    /**
     * Registers or replaces a named action.
     *
     * @param name stable non-blank action name
     * @param action callback to run
     */
    public synchronized void register(String name, Runnable action) {
        ensureOpen();
        actions.put(requireName(name), Objects.requireNonNull(action, "action"));
    }

    /**
     * Registers an action and binds it to a key-press character.
     *
     * @param name stable non-blank action name
     * @param key Processing key character
     * @param action callback to run
     */
    public synchronized void bindKeyPressed(String name, char key, Runnable action) {
        register(name, action);
        pressedKeys.put(key, name);
    }

    /**
     * Registers an action and binds it to a Processing key code.
     *
     * @param name stable non-blank action name
     * @param keyCode Processing key code
     * @param action callback to run
     */
    public synchronized void bindKeyCodePressed(String name, int keyCode, Runnable action) {
        register(name, action);
        pressedKeyCodes.put(keyCode, name);
    }

    /**
     * Binds a named mouse handler such as {@link MouseEvent#PRESS} or {@link MouseEvent#DRAG}.

     * <p>Only one binding is retained for each Processing mouse action constant.</p>
     *
     * @param name stable non-blank action name
     * @param mouseAction Processing mouse action constant
     * @param handler event consumer to run
     */
    public synchronized void bindMouse(
            String name,
            int mouseAction,
            Consumer<MouseEvent> handler) {
        ensureOpen();
        mouseActions.put(mouseAction, new MouseBinding(requireName(name),
                Objects.requireNonNull(handler, "handler")));
    }

    /**
     * Triggers a named action programmatically.
     *
     * @param name action name
     * @return true when a registered action ran
     */
    public synchronized boolean trigger(String name) {
        ensureOpen();
        Runnable action = actions.get(requireName(name));
        if (action == null) {
            return false;
        }
        action.run();
        return true;
    }

    /**
     * Dispatches one Processing key event and reports whether an action ran.
     *
     * @param event Processing key event
     * @return true when a binding ran
     */
    synchronized boolean dispatch(KeyEvent event) {
        Objects.requireNonNull(event, "event");
        if (closed || event.getAction() != KeyEvent.PRESS) {
            return false;
        }
        String name = pressedKeys.get(event.getKey());
        if (name == null) {
            name = pressedKeyCodes.get(event.getKeyCode());
        }
        return name != null && trigger(name);
    }

    /**
     * Dispatches one Processing mouse event and reports whether a binding ran.
     *
     * @param event Processing mouse event
     * @return true when a binding ran
     */
    synchronized boolean dispatch(MouseEvent event) {
        Objects.requireNonNull(event, "event");
        if (closed) {
            return false;
        }
        MouseBinding binding = mouseActions.get(event.getAction());
        if (binding == null) {
            return false;
        }
        binding.handler.accept(event);
        return true;
    }

    /**
     * Removes an action and all of its input bindings.
     *
     * @param name action name to remove
     */
    public synchronized void unregister(String name) {
        ensureOpen();
        String normalized = requireName(name);
        actions.remove(normalized);
        pressedKeys.values().removeIf(normalized::equals);
        pressedKeyCodes.values().removeIf(normalized::equals);
        mouseActions.values().removeIf(binding -> normalized.equals(binding.name));
    }

    /** @return number of named runnable actions plus separately registered mouse bindings */
    public synchronized int size() {
        ensureOpen();
        return actions.size() + mouseActions.size();
    }

    /** Removes every action and binding. */
    public synchronized void clear() {
        ensureOpen();
        clearState();
    }

    private void clearState() {
        actions.clear();
        pressedKeys.clear();
        pressedKeyCodes.clear();
        mouseActions.clear();
    }

    synchronized void close() {
        if (closed) {
            return;
        }
        clearState();
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Scene action map is closed.");
        }
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Action name cannot be blank.");
        }
        return name;
    }

    private static final class MouseBinding {
        private final String name;
        private final Consumer<MouseEvent> handler;

        private MouseBinding(String name, Consumer<MouseEvent> handler) {
            this.name = name;
            this.handler = handler;
        }
    }
}
