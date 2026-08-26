package com.victorvalentim.zividomelive.core.action;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Synchronous named action registry with deterministic replacement and close semantics. */
public final class ActionMap implements AutoCloseable {

    private final Map<String, Runnable> actions = new LinkedHashMap<>();
    private boolean closed;

    /** Registers or replaces a non-blank named action. */
    public synchronized void register(String name, Runnable action) {
        ensureOpen();
        actions.put(requireName(name), Objects.requireNonNull(action, "action"));
    }

    /**
     * Triggers an action synchronously on the calling thread.
     *
     * @return true when an action was registered and ran
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

    /** Removes a named action if present. */
    public synchronized void unregister(String name) {
        ensureOpen();
        actions.remove(requireName(name));
    }

    /** @return number of registered names */
    public synchronized int size() {
        ensureOpen();
        return actions.size();
    }

    /** Removes every registered action. */
    public synchronized void clear() {
        ensureOpen();
        actions.clear();
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    /** Clears the registry and permanently rejects subsequent operations. */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        actions.clear();
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Action map is closed.");
        }
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Action name cannot be blank.");
        }
        return name;
    }
}
