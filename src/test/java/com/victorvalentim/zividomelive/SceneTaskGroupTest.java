package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SceneTaskGroupTest {

    @Test
    void keyedSubmissionPreventsDuplicateInFlightWork() throws Exception {
        SceneTaskGroup group = new SceneTaskGroup(2);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Optional<Future<Void>> first = group.trySubmit("simulation", () -> {
            started.countDown();
            release.await();
            return null;
        });

        assertTrue(first.isPresent());
        assertTrue(started.await(2, TimeUnit.SECONDS));
        assertFalse(group.submitIfIdle("simulation", () -> {}));
        assertTrue(group.isBusy("simulation"));

        release.countDown();
        first.orElseThrow().get(2, TimeUnit.SECONDS);
        assertFalse(group.isBusy("simulation"));
        group.close();
    }

    @Test
    void closeCancelsSceneTasksAndRejectsNewWork() throws Exception {
        SceneTaskGroup group = new SceneTaskGroup(1);
        CountDownLatch started = new CountDownLatch(1);
        Future<Void> future = group.<Void>trySubmit("long", () -> {
            started.countDown();
            Thread.sleep(10_000);
            return null;
        }).orElseThrow();
        assertTrue(started.await(2, TimeUnit.SECONDS));

        group.close();

        assertTrue(future.isCancelled());
        assertEquals(0, group.getInFlightCount());
        assertThrows(IllegalStateException.class,
                () -> group.submitIfIdle("late", () -> {}));
    }

    @Test
    void budgetRejectsAdditionalDistinctTasks() throws Exception {
        SceneTaskGroup group = new SceneTaskGroup(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<Void> first = group.<Void>trySubmit("first", () -> {
            release.await();
            return null;
        }).orElseThrow();

        assertTrue(group.trySubmit("second", () -> null).isEmpty());
        release.countDown();
        first.get(2, TimeUnit.SECONDS);
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

    private static void awaitPending(RenderThreadQueue queue) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (queue.getPendingCount() == 0 && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertEquals(1, queue.getPendingCount());
    }
}
