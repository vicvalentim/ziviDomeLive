package com.victorvalentim.zividomelive.core.task;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameThreadQueueTest {

    @Test
    void ownerExecutesImmediatelyAndExplicitEnqueueWaits() {
        FrameThreadQueue queue = new FrameThreadQueue();
        AtomicInteger calls = new AtomicInteger();
        queue.executeOrEnqueue(calls::incrementAndGet);
        queue.enqueue(calls::incrementAndGet);

        assertEquals(1, calls.get());
        assertEquals(1, queue.getPendingCount());
        assertEquals(1, queue.drain());
        assertEquals(2, calls.get());
    }

    @Test
    void workerSubmissionRunsOnlyAtOwnerDrain() throws Exception {
        FrameThreadQueue queue = new FrameThreadQueue();
        AtomicInteger calls = new AtomicInteger();
        Thread worker = new Thread(() -> queue.executeOrEnqueue(calls::incrementAndGet));
        worker.start();
        worker.join();

        assertEquals(0, calls.get());
        assertEquals(1, queue.drain());
        assertEquals(1, calls.get());
    }

    @Test
    void workEnqueuedDuringDrainWaitsForNextSnapshot() {
        FrameThreadQueue queue = new FrameThreadQueue();
        List<Integer> order = new ArrayList<>();
        queue.enqueue(() -> {
            order.add(1);
            queue.enqueue(() -> order.add(3));
        });
        queue.enqueue(() -> order.add(2));

        assertEquals(2, queue.drain());
        assertEquals(List.of(1, 2), order);
        assertEquals(1, queue.getPendingCount());
        assertEquals(1, queue.drain());
        assertEquals(List.of(1, 2, 3), order);
    }

    @Test
    void ownershipCanRebindAndOldOwnerIsRejected() throws Exception {
        FrameThreadQueue queue = new FrameThreadQueue();
        CountDownLatch rebound = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread newOwner = new Thread(() -> {
            queue.bindToCurrentThread();
            rebound.countDown();
            await(release);
            assertTrue(queue.isFrameThread());
        });
        newOwner.start();
        assertTrue(rebound.await(2, TimeUnit.SECONDS));

        assertFalse(queue.isFrameThread());
        assertThrows(IllegalStateException.class, queue::requireFrameThread);
        assertThrows(IllegalStateException.class, queue::drain);
        release.countDown();
        newOwner.join();
    }

    @Test
    void closeDropsPendingAndGuardsEveryMutation() {
        FrameThreadQueue queue = new FrameThreadQueue();
        queue.enqueue(() -> { });
        queue.close();
        queue.close();

        assertTrue(queue.isClosed());
        assertEquals(0, queue.getPendingCount());
        assertThrows(IllegalStateException.class, () -> queue.enqueue(() -> { }));
        assertThrows(IllegalStateException.class, () -> queue.executeOrEnqueue(() -> { }));
        assertThrows(IllegalStateException.class, queue::bindToCurrentThread);
        assertThrows(IllegalStateException.class, queue::drain);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }
}
