package com.victorvalentim.zividomelive.core.lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Small host-neutral activation gate for pause, coalesced reload, and terminal admission state.
 *
 * <p>This is not a scene abstraction and does not invoke lifecycle callbacks. A host retains the
 * authoritative configure/setup/update/render/dispose ordering.</p>
 */
public final class ActivationState implements AutoCloseable {

    private final AtomicBoolean reloadRequested = new AtomicBoolean();
    private volatile boolean accepting = true;
    private volatile boolean paused;
    private volatile boolean closed;

    /** Requests a reload; multiple requests coalesce until consumed. */
    public void requestReload() {
        ensureAccepting();
        reloadRequested.set(true);
    }

    /** @return true once for one or more requests since the previous consumption */
    public boolean consumeReloadRequest() {
        if (!accepting || closed) {
            return false;
        }
        return reloadRequested.getAndSet(false);
    }

    /** Pauses host activation work without closing admission permanently. */
    public synchronized void pause() {
        ensureAccepting();
        paused = true;
    }

    /** Resumes host activation work. */
    public synchronized void resume() {
        ensureAccepting();
        paused = false;
    }

    /** Stops accepting activation work before domain disposal begins. */
    public synchronized void beginStopping() {
        if (!accepting) {
            return;
        }
        accepting = false;
        paused = false;
        reloadRequested.set(false);
    }

    public boolean isAccepting() {
        return accepting && !closed;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isClosed() {
        return closed;
    }

    /** Permanently closes the activation state. */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        beginStopping();
        closed = true;
    }

    private void ensureAccepting() {
        if (!accepting || closed) {
            throw new IllegalStateException("Activation is no longer accepting work.");
        }
    }
}
