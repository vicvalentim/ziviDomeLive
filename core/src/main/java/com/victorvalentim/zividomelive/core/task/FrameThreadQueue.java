package com.victorvalentim.zividomelive.core.task;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Host-neutral queue for work that must execute at an authoritative frame-thread boundary.
 */
public final class FrameThreadQueue implements AutoCloseable {

    private final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();
    private volatile Thread frameThread;
    private volatile boolean closed;

    /** Binds the queue to the constructing thread. */
    public FrameThreadQueue() {
        this(Thread.currentThread());
    }

    /** Binds the queue to an explicit initial owner thread. */
    public FrameThreadQueue(Thread frameThread) {
        this.frameThread = Objects.requireNonNull(frameThread, "frameThread");
    }

    /** Rebinds ownership to the calling thread. */
    public void bindToCurrentThread() {
        ensureOpen();
        frameThread = Thread.currentThread();
    }

    /** Enqueues work for a future drain boundary. */
    public void enqueue(Runnable work) {
        Objects.requireNonNull(work, "work");
        ensureOpen();
        pending.add(work);
    }

    /** Runs immediately on the owner thread and otherwise enqueues the work. */
    public void executeOrEnqueue(Runnable work) {
        Objects.requireNonNull(work, "work");
        ensureOpen();
        if (isFrameThread()) {
            work.run();
        } else {
            pending.add(work);
        }
    }

    /**
     * Runs a finite snapshot of currently pending work. Items added during the drain wait until
     * the next call.
     *
     * @return number of items executed
     */
    public int drain() {
        requireFrameThread();
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

    /** Throws unless the calling thread is the current owner. */
    public void requireFrameThread() {
        if (!isFrameThread()) {
            throw new IllegalStateException("This operation must run on the bound frame thread.");
        }
    }

    /** @return whether the caller is the current frame thread */
    public boolean isFrameThread() {
        return Thread.currentThread() == frameThread;
    }

    /** @return approximate pending item count */
    public int getPendingCount() {
        return pending.size();
    }

    /** @return whether new work is rejected */
    public boolean isClosed() {
        return closed;
    }

    /** Rejects new work and drops every pending item. */
    @Override
    public void close() {
        closed = true;
        pending.clear();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Frame-thread queue is closed.");
        }
    }
}
