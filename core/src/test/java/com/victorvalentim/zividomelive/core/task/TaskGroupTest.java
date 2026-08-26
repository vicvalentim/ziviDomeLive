package com.victorvalentim.zividomelive.core.task;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TaskGroupTest {

    @Test
    void qualifiedDefaultsAndSharedExecutorRemainBounded() {
        TaskGroup group = new TaskGroup();
        assertEquals(32, group.getMaxInFlight());
        assertEquals(256, CoreTaskExecutor.queueCapacity());
        assertEquals(Math.max(1, Runtime.getRuntime().availableProcessors()),
                CoreTaskExecutor.workerCount());
        group.close();
    }

    @Test
    void publicApiNeverExposesAFuture() {
        assertTrue(Arrays.stream(TaskGroup.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::getReturnType)
                .noneMatch(java.util.concurrent.Future.class::isAssignableFrom));
    }

    @Test
    void duplicateKeyAndCapacityAreRejectedWhileWorkIsInFlight() throws Exception {
        TaskGroup group = new TaskGroup(1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        assertTrue(group.submitIfIdle("first", () -> {
            started.countDown();
            await(release);
        }));
        assertTrue(started.await(2, TimeUnit.SECONDS));

        assertFalse(group.submitIfIdle("first", () -> { }));
        assertFalse(group.submitIfIdle("second", () -> { }));
        assertTrue(group.isBusy("first"));
        assertEquals(1, group.getInFlightCount());
        release.countDown();
        awaitIdle(group, "first");
        group.close();
    }

    @Test
    void resultCallbackRunsOnlyOnFrameDrain() throws Exception {
        FrameThreadQueue queue = new FrameThreadQueue();
        TaskGroup group = new TaskGroup(1, queue);
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Thread> callbackThread = new AtomicReference<>();
        CountDownLatch workFinished = new CountDownLatch(1);
        assertTrue(group.submitIfIdle("load", () -> {
            workFinished.countDown();
            return "ready";
        }, value -> {
            result.set(value);
            callbackThread.set(Thread.currentThread());
        }));

        assertTrue(workFinished.await(2, TimeUnit.SECONDS));
        awaitPending(queue);
        assertNull(result.get());
        queue.drain();
        assertEquals("ready", result.get());
        assertSame(Thread.currentThread(), callbackThread.get());
        group.close();
        queue.close();
    }

    @Test
    void errorCallbackRunsOnlyOnFrameDrain() throws Exception {
        FrameThreadQueue queue = new FrameThreadQueue();
        TaskGroup group = new TaskGroup(1, queue);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch workFinished = new CountDownLatch(1);
        assertTrue(group.submitIfIdle("failure", () -> {
            workFinished.countDown();
            throw new IllegalStateException("boom");
        }, value -> fail("failure must not publish a result"), failure::set));

        assertTrue(workFinished.await(2, TimeUnit.SECONDS));
        awaitPending(queue);
        assertNull(failure.get());
        queue.drain();
        assertEquals("boom", failure.get().getMessage());
        group.close();
        queue.close();
    }

    @Test
    void closeCancelsWorkRejectsSubmissionsAndSuppressesStaleCallbacks() throws Exception {
        FrameThreadQueue queue = new FrameThreadQueue();
        TaskGroup group = new TaskGroup(1, queue);
        AtomicReference<String> result = new AtomicReference<>();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        assertTrue(group.submitIfIdle("slow", () -> {
            started.countDown();
            await(release);
            return "stale";
        }, result::set));
        assertTrue(started.await(2, TimeUnit.SECONDS));

        group.close();
        group.close();
        release.countDown();

        assertTrue(group.isClosed());
        assertEquals(0, group.getInFlightCount());
        assertNull(result.get());
        assertThrows(IllegalStateException.class,
                () -> group.submitIfIdle("late", () -> { }));
        queue.close();
    }

    @Test
    void executorCapacityRejectionReturnsFalseAndReleasesKey() {
        TaskGroup group = new TaskGroup(1, new FrameThreadQueue(), task -> {
            throw new RejectedExecutionException("full");
        });

        assertFalse(group.submitIfIdle("work", () -> { }));
        assertFalse(group.isBusy("work"));
        assertEquals(0, group.getInFlightCount());
        group.close();
    }

    @Test
    void controlledExecutorProvesAdmissionRemovalAndCallbackOrdering() {
        FrameThreadQueue queue = new FrameThreadQueue();
        ManualExecutor executor = new ManualExecutor();
        TaskGroup group = new TaskGroup(2, queue, executor);
        AtomicReference<String> firstResult = new AtomicReference<>();
        AtomicReference<Throwable> secondError = new AtomicReference<>();

        assertTrue(group.submitIfIdle("first", () -> "ready", firstResult::set));
        assertFalse(group.submitIfIdle("first", () -> "duplicate", firstResult::set));
        assertTrue(group.submitIfIdle("second", () -> {
            throw new IllegalArgumentException("broken");
        }, ignored -> fail("failure cannot publish a result"), secondError::set));
        assertFalse(group.submitIfIdle("third", () -> { }));
        assertEquals(2, group.getInFlightCount());
        assertTrue(group.isBusy("first"));

        executor.runNext();
        assertFalse(group.isBusy("first"),
                "The key is removed before its frame callback is delivered");
        assertEquals(1, group.getInFlightCount());
        assertEquals(1, queue.getPendingCount());
        assertNull(firstResult.get());
        assertEquals(1, queue.drain());
        assertEquals("ready", firstResult.get());

        executor.runNext();
        assertFalse(group.isBusy("second"));
        assertEquals(0, group.getInFlightCount());
        assertNull(secondError.get());
        assertEquals(1, queue.drain());
        assertEquals("broken", secondError.get().getMessage());
        group.close();
        queue.close();
    }

    @Test
    void controlledCancellationSuppressesWorkAndCallbacksWithoutTiming() {
        FrameThreadQueue queue = new FrameThreadQueue();
        ManualExecutor executor = new ManualExecutor();
        TaskGroup group = new TaskGroup(1, queue, executor);
        AtomicReference<String> result = new AtomicReference<>();
        assertTrue(group.submitIfIdle("stale", () -> "old", result::set));

        group.close();
        executor.runNext();

        assertTrue(group.isClosed());
        assertEquals(0, group.getInFlightCount());
        assertEquals(0, queue.getPendingCount());
        assertNull(result.get());
        queue.close();
    }

    @Test
    void unexpectedExecutorFailureReleasesKeyAndPropagates() {
        TaskGroup group = new TaskGroup(1, new FrameThreadQueue(), task -> {
            throw new IllegalStateException("executor failed");
        });

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> group.submitIfIdle("work", () -> { }));
        assertEquals("executor failed", error.getMessage());
        assertFalse(group.isBusy("work"));
        assertEquals(0, group.getInFlightCount());
        group.close();
    }

    @Test
    void invalidKeysCallbacksAndCapacityAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TaskGroup(0));
        TaskGroup group = new TaskGroup(1);
        assertThrows(IllegalArgumentException.class,
                () -> group.submitIfIdle(" ", () -> { }));
        assertThrows(NullPointerException.class,
                () -> group.submitIfIdle("null", (Runnable) null));
        assertThrows(NullPointerException.class,
                () -> group.submitIfIdle("null-result", () -> "x", null));
        group.close();
    }

    @Test
    void sharedWorkersAreDaemonOffThreadWorkers() throws Exception {
        TaskGroup group = new TaskGroup(1);
        AtomicReference<Thread> worker = new AtomicReference<>();
        CountDownLatch finished = new CountDownLatch(1);
        assertTrue(group.submitIfIdle("thread", () -> {
            worker.set(Thread.currentThread());
            finished.countDown();
        }));
        assertTrue(finished.await(2, TimeUnit.SECONDS));

        assertFalse(worker.get() == Thread.currentThread());
        assertTrue(worker.get().isDaemon());
        assertTrue(worker.get().getName().startsWith("zividomelive-core-task-"));
        group.close();
    }

    private static void awaitPending(FrameThreadQueue queue) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (queue.getPendingCount() == 0 && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertEquals(1, queue.getPendingCount());
    }

    private static void awaitIdle(TaskGroup group, String key) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (group.isBusy(key) && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertFalse(group.isBusy(key));
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class ManualExecutor implements Executor {
        private final Deque<Runnable> pending = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            pending.addLast(command);
        }

        private void runNext() {
            Runnable command = pending.removeFirst();
            command.run();
        }
    }
}
