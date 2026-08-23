package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SceneTaskGroupTest {

    @Test
    void keyedSubmissionPreventsDuplicateInFlightWork() throws Exception {
        SceneTaskGroup group = new SceneTaskGroup(2);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        assertTrue(group.submitIfIdle("simulation", () -> {
            started.countDown();
            awaitLatch(release);
        }));

        assertTrue(started.await(2, TimeUnit.SECONDS));
        assertFalse(group.submitIfIdle("simulation", () -> {}));
        assertTrue(group.isBusy("simulation"));

        release.countDown();
        assertTrue(group.submitIfIdle("finished-signal", finished::countDown));
        assertTrue(finished.await(2, TimeUnit.SECONDS));
        awaitNotBusy(group, "simulation");
        assertFalse(group.isBusy("simulation"));
        group.close();
    }

    @Test
    void closeCancelsSceneTasksAndRejectsNewWork() throws Exception {
        SceneTaskGroup group = new SceneTaskGroup(1);
        CountDownLatch started = new CountDownLatch(1);
        assertTrue(group.submitIfIdle("long", () -> {
            started.countDown();
            awaitLatch(new CountDownLatch(1));
        }));
        assertTrue(started.await(2, TimeUnit.SECONDS));

        group.close();

        assertEquals(0, group.getInFlightCount());
        assertThrows(IllegalStateException.class,
                () -> group.submitIfIdle("late", () -> {}));
    }

    @Test
    void budgetRejectsAdditionalDistinctTasks() throws Exception {
        SceneTaskGroup group = new SceneTaskGroup(1);
        CountDownLatch release = new CountDownLatch(1);
        assertTrue(group.submitIfIdle("first", () -> {
            awaitLatch(release);
        }));

        assertFalse(group.submitIfIdle("second", () -> {}));
        release.countDown();
        awaitNotBusy(group, "first");
        group.close();
    }

    @Test
    void completedResultIsPublishedOnlyAtTheSceneFrameBoundary() throws Exception {
        RenderThreadQueue queue = new RenderThreadQueue();
        SceneTaskGroup group = new SceneTaskGroup(1, queue);
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
        assertNull(result.get());
        awaitPending(queue);
        queue.drain();
        assertEquals("ready", result.get());
        assertSame(Thread.currentThread(), callbackThread.get());
        group.close();
        queue.close();
    }

    @Test
    void completedResultFromClosedActivationIsDiscarded() throws Exception {
        RenderThreadQueue queue = new RenderThreadQueue();
        SceneTaskGroup group = new SceneTaskGroup(1, queue);
        AtomicReference<String> result = new AtomicReference<>();
        CountDownLatch workFinished = new CountDownLatch(1);

        assertTrue(group.submitIfIdle("load", () -> {
            workFinished.countDown();
            return "stale";
        }, result::set));

        assertTrue(workFinished.await(2, TimeUnit.SECONDS));
        group.close();
        queue.close();
        assertEquals(0, queue.getPendingCount());
        assertNull(result.get());
    }

    @Test
    void failuresArePublishedOnlyAtTheSceneFrameBoundary() throws Exception {
        RenderThreadQueue queue = new RenderThreadQueue();
        SceneTaskGroup group = new SceneTaskGroup(1, queue);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Thread> callbackThread = new AtomicReference<>();
        CountDownLatch workFinished = new CountDownLatch(1);

        assertTrue(group.submitIfIdle("failure", () -> {
            workFinished.countDown();
            throw new IllegalStateException("boom");
        }, value -> {
            fail("Failed work must not publish a result");
        }, error -> {
            failure.set(error);
            callbackThread.set(Thread.currentThread());
        }));

        assertTrue(workFinished.await(2, TimeUnit.SECONDS));
        assertNull(failure.get());
        awaitPending(queue);
        queue.drain();
        assertEquals("boom", failure.get().getMessage());
        assertSame(Thread.currentThread(), callbackThread.get());
        group.close();
        queue.close();
    }

    @Test
    void sharedWorkersAreOffThreadAndDaemonOwnedByTheRuntime() throws Exception {
        SceneTaskGroup group = new SceneTaskGroup(1);
        AtomicReference<Thread> worker = new AtomicReference<>();
        CountDownLatch finished = new CountDownLatch(1);

        assertTrue(group.submitIfIdle("thread", () -> {
            worker.set(Thread.currentThread());
            finished.countDown();
        }));

        assertTrue(finished.await(2, TimeUnit.SECONDS));
        assertNotSame(Thread.currentThread(), worker.get());
        assertTrue(worker.get().isDaemon());
        assertTrue(worker.get().getName().startsWith("zividomelive-task-"));
        group.close();
    }

    private static void awaitPending(RenderThreadQueue queue) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (queue.getPendingCount() == 0 && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertEquals(1, queue.getPendingCount());
    }

    private static void awaitNotBusy(SceneTaskGroup group, String key) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (group.isBusy(key) && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertFalse(group.isBusy(key));
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }
}
