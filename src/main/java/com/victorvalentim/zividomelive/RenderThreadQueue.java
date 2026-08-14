package com.victorvalentim.zividomelive;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Scene-scoped queue for work that must execute on the Processing/OpenGL thread.
 */
public final class RenderThreadQueue implements AutoCloseable {

    private final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();
    private final Thread renderThread;
    private volatile boolean closed;

    /** Binds a standalone queue to the thread that constructs it. */
    public RenderThreadQueue() {
        this(Thread.currentThread());
    }

    RenderThreadQueue(Thread renderThread) {
        this.renderThread = Objects.requireNonNull(renderThread, "renderThread");
    }

    /**
     * Enqueues work for the next scene-frame boundary.
     *
     * @param work work to execute on the bound render thread
     */
    public void enqueue(Runnable work) {
        Objects.requireNonNull(work, "work");
        ensureOpen();
        pending.add(work);
    }

    /**
     * Runs immediately on the render thread, otherwise enqueues the work.
     *
     * @param work work to execute or enqueue
     */
    public void executeOrEnqueue(Runnable work) {
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
    public int drain() {
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
    public void requireRenderThread() {
        if (!isRenderThread()) {
            throw new IllegalStateException("This operation must run on the Processing render thread.");
        }
    }

    /** @return whether the caller is the thread bound at construction */
    public boolean isRenderThread() {
        return Thread.currentThread() == renderThread;
    }

    /** @return approximate number of queued work items */
    public int getPendingCount() {
        return pending.size();
    }

    /** @return whether the queue rejects new work */
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
        pending.clear();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Render-thread queue is closed.");
        }
    }
}
