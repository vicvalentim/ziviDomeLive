package com.victorvalentim.zividomelive.core.task;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

/**
 * Bounded activation-owned keyed background work with callbacks returned to a frame thread.
 *
 * <p>The public API deliberately exposes no {@code Future}. Closing cancels work and suppresses
 * callbacks that belong to the old activation.</p>
 */
public final class TaskGroup implements AutoCloseable {

    /** Qualified default maximum distinct tasks in flight. */
    public static final int DEFAULT_MAX_IN_FLIGHT = 32;

    private static final System.Logger LOGGER = System.getLogger(TaskGroup.class.getName());

    private final Map<String, FutureTask<?>> tasks = new ConcurrentHashMap<>();
    private final int maxInFlight;
    private final FrameThreadQueue frameQueue;
    private final Executor executor;
    private volatile boolean closed;

    /** Creates a default-size group and a frame queue bound to the calling thread. */
    public TaskGroup() {
        this(DEFAULT_MAX_IN_FLIGHT, new FrameThreadQueue());
    }

    /** Creates a group with the qualified default capacity and supplied callback queue. */
    public TaskGroup(FrameThreadQueue frameQueue) {
        this(DEFAULT_MAX_IN_FLIGHT, frameQueue);
    }

    /** Creates a group with a custom positive capacity and a new current-thread callback queue. */
    public TaskGroup(int maxInFlight) {
        this(maxInFlight, new FrameThreadQueue());
    }

    /** Creates a group with custom capacity and supplied callback queue. */
    public TaskGroup(int maxInFlight, FrameThreadQueue frameQueue) {
        this(maxInFlight, frameQueue, CoreTaskExecutor::execute);
    }

    TaskGroup(int maxInFlight, FrameThreadQueue frameQueue, Executor executor) {
        if (maxInFlight < 1) {
            throw new IllegalArgumentException("Maximum in-flight task count must be positive.");
        }
        this.maxInFlight = maxInFlight;
        this.frameQueue = Objects.requireNonNull(frameQueue, "frameQueue");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /** Submits runnable work only when its key is idle and group capacity is available. */
    public boolean submitIfIdle(String key, Runnable task) {
        Objects.requireNonNull(task, "task");
        return submitTracked(key, () -> {
            task.run();
            return null;
        }, null, null);
    }

    /** Submits callable work and returns its result at a later frame-queue drain. */
    public <T> boolean submitIfIdle(
            String key,
            Callable<T> task,
            Consumer<? super T> onResult) {
        Objects.requireNonNull(onResult, "onResult");
        return submitTracked(key, task, onResult, null);
    }

    /** Submits callable work and returns its result or failure at a later frame-queue drain. */
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
            executor.execute(future);
            return true;
        } catch (RejectedExecutionException error) {
            tasks.remove(normalized, future);
            return false;
        } catch (RuntimeException error) {
            tasks.remove(normalized, future);
            throw error;
        }
    }

    /** Reports whether a keyed task is queued or running. */
    public boolean isBusy(String key) {
        FutureTask<?> task = tasks.get(requireKey(key));
        return task != null && !task.isDone();
    }

    public int getInFlightCount() {
        return tasks.size();
    }

    public int getMaxInFlight() {
        return maxInFlight;
    }

    public boolean isClosed() {
        return closed;
    }

    /** Cancels all owned work, rejects new work, and makes queued callbacks stale. */
    @Override
    public synchronized void close() {
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
            throw new IllegalStateException("Task group is closed.");
        }
    }

    private void publishResult(Runnable callback) {
        synchronized (this) {
            if (closed) {
                return;
            }
            try {
                frameQueue.enqueue(() -> {
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
                    LOGGER.log(System.Logger.Level.ERROR, "Core task failed: " + key, cause);
                }
            }
        }
    }
}
