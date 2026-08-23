package com.victorvalentim.zividomelive;

// Package-private implementation grouped physically under _internal/runtime.

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Scene-scoped queue for work that must execute on the Processing/OpenGL thread.
 */
final class RenderThreadQueue {

    private final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();
    private volatile Thread renderThread;
    private volatile boolean closed;

    /** Binds a standalone queue to the thread that constructs it. */
    RenderThreadQueue() {
        this(Thread.currentThread());
    }

    RenderThreadQueue(Thread renderThread) {
        this.renderThread = Objects.requireNonNull(renderThread, "renderThread");
    }

    /**
     * Rebinds scene-owned work to the thread executing the Processing frame boundary.
     * Processing may run {@code setup()} and its JOGL animator callbacks on different threads.
     */
    void bindToCurrentThread() {
        ensureOpen();
        renderThread = Thread.currentThread();
    }

    /**
     * Enqueues work for the next scene-frame boundary.
     *
     * @param work work to execute on the bound render thread
     */
    void enqueue(Runnable work) {
        Objects.requireNonNull(work, "work");
        ensureOpen();
        pending.add(work);
    }

    /**
     * Runs immediately on the render thread, otherwise enqueues the work.
     *
     * @param work work to execute or enqueue
     */
    void executeOrEnqueue(Runnable work) {
        Objects.requireNonNull(work, "work");
        ensureOpen();
        if (isRenderThread()) {
            work.run();
        } else {
            pending.add(work);
        }
    }

    /**
     * Executes all work currently queued. New concurrently queued work waits for the next drain.
     *
     * @return number of work items executed
     */
    int drain() {
        requireRenderThread();
        ensureOpen();
        int limit = pending.size();
        int executed = 0;
        while (executed < limit) {
            Runnable work = pending.poll();
            if (work == null) {
                break;
            }
            work.run();
            executed++;
        }
        return executed;
    }

    /** Throws when called outside the Processing/OpenGL thread. */
    void requireRenderThread() {
        if (!isRenderThread()) {
            throw new IllegalStateException("This operation must run on the Processing render thread.");
        }
    }

    /** @return whether the caller is the currently bound render thread */
    boolean isRenderThread() {
        return Thread.currentThread() == renderThread;
    }

    /** @return approximate number of queued work items */
    int getPendingCount() {
        return pending.size();
    }

    /** @return whether the queue rejects new work */
    boolean isClosed() {
        return closed;
    }

    void close() {
        closed = true;
        pending.clear();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Render-thread queue is closed.");
        }
    }
}
