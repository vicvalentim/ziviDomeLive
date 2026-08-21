package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.support.LogManager;
import com.victorvalentim.zividomelive.support.ThreadManager;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bounded scene-owned task group backed by the library's shared {@link ThreadManager}.
 */
public final class SceneTaskGroup {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<String, FutureTask<?>> tasks = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private final int maxInFlight;
    private final RenderThreadQueue renderQueue;
    private volatile boolean closed;

    SceneTaskGroup(RenderThreadQueue renderQueue) {
        this(32, renderQueue);
    }

    SceneTaskGroup(int maxInFlight) {
        this(maxInFlight, new RenderThreadQueue());
    }

    SceneTaskGroup(int maxInFlight, RenderThreadQueue renderQueue) {
        if (maxInFlight < 1) {
            throw new IllegalArgumentException("Maximum in-flight task count must be positive.");
        }
        this.maxInFlight = maxInFlight;
        this.renderQueue = Objects.requireNonNull(renderQueue, "renderQueue");
    }

    /**
     * Submits an unkeyed task or throws when the scene budget is full.
     *
     * @param <T> result type
     * @param task work to execute
     * @return future representing the task
     */
    public <T> Future<T> submit(Callable<T> task) {
        String key = "task-" + sequence.incrementAndGet();
        return trySubmit(key, task).orElseThrow(
                () -> new RejectedExecutionException("Scene task budget is full."));
    }

    /**
     * Submits keyed work only when no work with that key is already in flight.
     *
     * @param key stable task key
     * @param task work to execute
     * @return true when submitted
     */
    public boolean submitIfIdle(String key, Runnable task) {
        Objects.requireNonNull(task, "task");
        return trySubmit(key, () -> {
            task.run();
            return null;
        }).isPresent();
    }

    /**
     * Runs keyed work in the shared background pool and publishes its result at the next
     * frame boundary of this activation.
     *
     * @param key stable task key
     * @param task background work that must not call Processing/OpenGL APIs
     * @param onResult result handler executed on the Processing render thread
     * @param <T> result type
     * @return true when submitted
     */
    public <T> boolean submitIfIdle(
            String key,
            Callable<T> task,
            Consumer<? super T> onResult) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(onResult, "onResult");
        return trySubmit(key, () -> {
            T result = task.call();
            publishResult(() -> onResult.accept(result));
            return result;
        }).isPresent();
    }

    /**
     * Attempts to submit keyed work. Empty means that key is busy or the group budget is full.
     *
     * @param <T> result type
     * @param key stable task key
     * @param task work to execute
     * @return future when submitted, otherwise empty
     */
    public synchronized <T> Optional<Future<T>> trySubmit(String key, Callable<T> task) {
        ensureOpen();
        String normalized = requireKey(key);
        Objects.requireNonNull(task, "task");
        if (tasks.size() >= maxInFlight) {
            return Optional.empty();
        }

        TrackedFutureTask<T> future = new TrackedFutureTask<>(normalized, task);
        FutureTask<?> previous = tasks.putIfAbsent(normalized, future);
        if (previous != null) {
            return Optional.empty();
        }

        try {
            ThreadManager.execute(future);
            return Optional.of(future);
        } catch (RuntimeException error) {
            tasks.remove(normalized, future);
            throw error;
        }
    }

    /**
     * Reports whether keyed work is currently in flight.
     *
     * @param key task key
     * @return true while queued or running
     */
    public boolean isBusy(String key) {
        FutureTask<?> task = tasks.get(requireKey(key));
        return task != null && !task.isDone();
    }

    /** @return current number of queued or running tasks */
    public int getInFlightCount() {
        return tasks.size();
    }

    /** @return configured in-flight task budget */
    public int getMaxInFlight() {
        return maxInFlight;
    }

    /** @return whether the group rejects new work */
    boolean isClosed() {
        return closed;
    }

    /** Cancels every scene-owned task and rejects future submissions. */
    synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (FutureTask<?> task : new ArrayList<>(tasks.values())) {
            task.cancel(true);
        }
        tasks.clear();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Scene task group is closed.");
        }
    }

    private void publishResult(Runnable callback) {
        synchronized (this) {
            if (closed) {
                return;
            }
            try {
                renderQueue.enqueue(() -> {
                    if (!closed) {
                        callback.run();
                    }
                });
            } catch (IllegalStateException error) {
                if (!closed) {
                    throw error;
                }
            }
        }
    }

    private static String requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Task key cannot be blank.");
        }
        return key;
    }

    private final class TrackedFutureTask<T> extends FutureTask<T> {
        private final String key;

        private TrackedFutureTask(String key, Callable<T> task) {
            super(task);
            this.key = key;
        }

        @Override
        protected void done() {
            tasks.remove(key, this);
            if (isCancelled()) {
                return;
            }
            try {
                get();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            } catch (Exception error) {
                LOGGER.log(Level.SEVERE, "Scene task failed: " + key, error.getCause());
            }
        }
    }
}
