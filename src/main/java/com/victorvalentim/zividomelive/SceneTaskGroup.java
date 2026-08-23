package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.support.LogManager;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bounded scene-owned task group backed by the library's shared worker pool.
 */
public final class SceneTaskGroup {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<String, FutureTask<?>> tasks = new ConcurrentHashMap<>();
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
     * Submits keyed work only when no work with that key is already in flight.
     *
     * @param key stable task key
     * @param task work to execute
     * @return true when submitted
     */
    public boolean submitIfIdle(String key, Runnable task) {
        Objects.requireNonNull(task, "task");
        return submitTracked(key, () -> {
            task.run();
            return null;
        }, null, null);
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
        Objects.requireNonNull(onResult, "onResult");
        return submitTracked(key, task, onResult, null);
    }

    /**
     * Runs keyed work in the shared background pool and publishes either its result or failure
     * at the next frame boundary of this activation.
     *
     * @param key stable task key
     * @param task background work that must not call Processing/OpenGL APIs
     * @param onResult result handler executed on the Processing render thread
     * @param onError failure handler executed on the Processing render thread
     * @param <T> result type
     * @return true when submitted
     */
    public <T> boolean submitIfIdle(
            String key,
            Callable<T> task,
            Consumer<? super T> onResult,
            Consumer<? super Throwable> onError) {
        Objects.requireNonNull(onResult, "onResult");
        Objects.requireNonNull(onError, "onError");
        return submitTracked(key, task, onResult, onError);
    }

    private synchronized <T> boolean submitTracked(
            String key,
            Callable<T> task,
            Consumer<? super T> onResult,
            Consumer<? super Throwable> onError) {
        ensureOpen();
        String normalized = requireKey(key);
        Objects.requireNonNull(task, "task");
        if (tasks.size() >= maxInFlight) {
            return false;
        }

        TrackedFutureTask<T> future = new TrackedFutureTask<>(
                normalized, task, onResult, onError);
        FutureTask<?> previous = tasks.putIfAbsent(normalized, future);
        if (previous != null) {
            return false;
        }

        try {
            SharedTaskExecutor.execute(future);
            return true;
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
        private final Consumer<? super T> onResult;
        private final Consumer<? super Throwable> onError;

        private TrackedFutureTask(
                String key,
                Callable<T> task,
                Consumer<? super T> onResult,
                Consumer<? super Throwable> onError) {
            super(task);
            this.key = key;
            this.onResult = onResult;
            this.onError = onError;
        }

        @Override
        protected void done() {
            tasks.remove(key, this);
            if (isCancelled()) {
                return;
            }
            try {
                T result = get();
                if (onResult != null) {
                    publishResult(() -> onResult.accept(result));
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            } catch (CancellationException ignored) {
                // Activation disposal intentionally discards cancelled work.
            } catch (ExecutionException error) {
                Throwable cause = error.getCause();
                if (onError != null) {
                    publishResult(() -> onError.accept(cause));
                } else {
                    LOGGER.log(Level.SEVERE, "Scene task failed: " + key, cause);
                }
            }
        }
    }
}
