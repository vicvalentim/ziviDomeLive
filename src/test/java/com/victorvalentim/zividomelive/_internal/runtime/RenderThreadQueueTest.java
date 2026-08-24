package com.victorvalentim.zividomelive;

// Package-private implementation test grouped physically under _internal/runtime.

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RenderThreadQueueTest {

    @Test
    void sameThreadWorkCanRunImmediately() {
        RenderThreadQueue queue = new RenderThreadQueue();
        AtomicInteger calls = new AtomicInteger();

        queue.executeOrEnqueue(calls::incrementAndGet);

        assertEquals(1, calls.get());
        assertEquals(0, queue.getPendingCount());
    }

    @Test
    void workerWorkWaitsForRenderThreadDrain() throws Exception {
        RenderThreadQueue queue = new RenderThreadQueue();
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch submitted = new CountDownLatch(1);
        Thread worker = new Thread(() -> {
            queue.executeOrEnqueue(calls::incrementAndGet);
            submitted.countDown();
        });
        worker.start();
        assertTrue(submitted.await(2, TimeUnit.SECONDS));
        worker.join();

        assertEquals(0, calls.get());
        assertEquals(1, queue.drain());
        assertEquals(1, calls.get());
    }

    @Test
    void drainRejectsWrongThreadAndCloseDropsPendingWork() throws Exception {
        RenderThreadQueue queue = new RenderThreadQueue();
        AtomicInteger failures = new AtomicInteger();
        Thread worker = new Thread(() -> {
            try {
                queue.drain();
            } catch (IllegalStateException expected) {
                failures.incrementAndGet();
            }
            queue.enqueue(() -> {});
        });
        worker.start();
        worker.join();

        assertEquals(1, failures.get());
        queue.close();
        assertEquals(0, queue.getPendingCount());
        assertThrows(IllegalStateException.class, () -> queue.enqueue(() -> {}));
    }

    @Test
    void sceneFrameBoundaryCanRebindFromSetupThreadToAnimatorThread() throws Exception {
        RenderThreadQueue queue = new RenderThreadQueue();
        AtomicInteger calls = new AtomicInteger();
        queue.enqueue(calls::incrementAndGet);

        Thread animator = new Thread(() -> {
            queue.bindToCurrentThread();
            assertEquals(1, queue.drain());
        }, "test-processing-animator");
        animator.start();
        animator.join();

        assertEquals(1, calls.get());
        assertThrows(IllegalStateException.class, queue::requireRenderThread,
                "the old setup thread must no longer be treated as the render thread");
    }
}
