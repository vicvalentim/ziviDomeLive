package com.victorvalentim.zividomelive.core.ports;

import com.victorvalentim.zividomelive.core.task.FrameThreadQueue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortsTest {

    @Test
    void defaultsAreTheQualifiedBounds() {
        assertEquals(256, Ports.DEFAULT_INPUT_CAPACITY);
        assertEquals(32, Ports.DEFAULT_MAX_EVENTS_PER_FRAME);
    }

    @Test
    void arbitraryThreadProducerIsDeliveredInOrderOnFrameThread() throws Exception {
        Ports ports = new Ports(new FrameThreadQueue(), 8);
        FakeInputPort<Integer> input = new FakeInputPort<>();
        List<Integer> received = new ArrayList<>();
        AtomicReference<Thread> handlerThread = new AtomicReference<>();
        ports.connectInput(input, value -> {
            received.add(value);
            handlerThread.set(Thread.currentThread());
        });
        Thread producer = new Thread(() -> {
            input.emit(1);
            input.emit(2);
            input.emit(3);
        });
        producer.start();
        producer.join();

        assertTrue(received.isEmpty());
        assertEquals(3, ports.getPendingInputCount());
        assertEquals(3, ports.drain());
        assertEquals(List.of(1, 2, 3), received);
        assertSame(Thread.currentThread(), handlerThread.get());
        ports.close();
    }

    @Test
    void overflowDropsOldestAndReportsTelemetry() {
        Ports ports = new Ports(new FrameThreadQueue(), 2);
        FakeInputPort<Integer> input = new FakeInputPort<>();
        List<Integer> received = new ArrayList<>();
        ports.connectInput(input, received::add);
        input.emit(1);
        input.emit(2);
        input.emit(3);

        assertEquals(1L, ports.getDroppedInputCount());
        assertEquals(2, ports.getPendingInputCount());
        assertEquals(2, ports.drain());
        assertEquals(List.of(2, 3), received);
        ports.close();
    }

    @Test
    void perFrameBudgetLeavesPendingEventsForLaterDrains() {
        Ports ports = new Ports(new FrameThreadQueue(), 16, 4);
        FakeInputPort<Integer> input = new FakeInputPort<>();
        List<Integer> received = new ArrayList<>();
        ports.connectInput(input, received::add);
        for (int value = 0; value < 10; value++) {
            input.emit(value);
        }

        assertEquals(4, ports.drain());
        assertEquals(List.of(0, 1, 2, 3), received);
        assertEquals(6, ports.getPendingInputCount());
        assertEquals(4, ports.drain());
        assertEquals(2, ports.getPendingInputCount());
        ports.close();
    }

    @Test
    void pauseClearsAndRejectsStaleInputAndOutputUntilResume() {
        Ports ports = new Ports(new FrameThreadQueue(), 4);
        FakeInputPort<Integer> input = new FakeInputPort<>();
        FakeOutputPort<Integer> output = new FakeOutputPort<>();
        List<Integer> received = new ArrayList<>();
        ports.connectInput(input, received::add);
        OutputPort<Integer> managed = ports.connectOutput(output);
        input.emit(1);
        ports.pause();
        input.emit(2);

        assertTrue(ports.isPaused());
        assertEquals(0, ports.getPendingInputCount());
        assertEquals(0, ports.drain());
        assertFalse(managed.offer(2));
        ports.resume();
        input.emit(3);
        assertEquals(1, ports.drain());
        assertEquals(List.of(3), received);
        assertTrue(managed.offer(4));
        assertEquals(1, output.offerCount.get());
        ports.close();
    }

    @Test
    void stopAcceptingGuardsManagedOutputBeforeReverseAdapterClose() {
        Ports ports = new Ports(new FrameThreadQueue(), 4);
        FakeInputPort<Integer> input = new FakeInputPort<>();
        FakeOutputPort<Integer> output = new FakeOutputPort<>();
        ports.connectInput(input, value -> { });
        OutputPort<Integer> managed = ports.connectOutput(output);
        ports.stopAccepting();
        input.emit(1);

        assertFalse(managed.offer(1));
        assertEquals(0, ports.getPendingInputCount());
        assertEquals(0, input.closeCount.get());
        ports.close();
        assertEquals(1, input.closeCount.get());
        assertEquals(1, output.closeCount.get());
    }

    @Test
    void adaptersCloseInReverseOrderAndFailuresDoNotStopCleanup() {
        Ports ports = new Ports(new FrameThreadQueue(), 4);
        List<String> order = new ArrayList<>();
        ports.connectOutput(new RecordingOutput("first", order, false));
        ports.connectOutput(new RecordingOutput("second", order, true));
        ports.connectOutput(new RecordingOutput("third", order, false));

        assertDoesNotThrow(ports::close);
        assertDoesNotThrow(ports::close);
        assertEquals(List.of("third", "second", "first"), order);
        assertTrue(ports.isClosed());
    }

    @Test
    void duplicateAdapterIdentityIsRejectedEvenWhenEqualsCouldDiffer() {
        Ports ports = new Ports(new FrameThreadQueue(), 4);
        FakeInputPort<Integer> input = new FakeInputPort<>();
        ports.connectInput(input, value -> { });
        assertThrows(IllegalArgumentException.class,
                () -> ports.connectInput(input, value -> { }));
        ports.close();
        assertEquals(1, input.closeCount.get());
    }

    @Test
    void failedInputStartIsUnregisteredAndClosed() {
        Ports ports = new Ports(new FrameThreadQueue(), 4);
        AtomicInteger closes = new AtomicInteger();
        InputPort<Integer> failing = new InputPort<>() {
            @Override
            public void start(Consumer<? super Integer> receiver) {
                throw new IllegalStateException("start failed");
            }

            @Override
            public void close() {
                closes.incrementAndGet();
            }
        };

        assertThrows(IllegalStateException.class,
                () -> ports.connectInput(failing, value -> { }));
        assertEquals(1, closes.get());
        assertDoesNotThrow(ports::close);
        assertEquals(1, closes.get());
    }

    @Test
    void drainRequiresBoundFrameThreadAndBoundsMustBePositive() throws Exception {
        Ports ports = new Ports(new FrameThreadQueue(), 4);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                ports.drain();
            } catch (Throwable error) {
                failure.set(error);
            }
        });
        worker.start();
        worker.join();
        assertTrue(failure.get() instanceof IllegalStateException);
        ports.close();

        assertThrows(IllegalArgumentException.class,
                () -> new Ports(new FrameThreadQueue(), 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Ports(new FrameThreadQueue(), 1, 0));
    }

    private static final class FakeInputPort<T> implements InputPort<T> {
        private Consumer<? super T> receiver;
        private final AtomicInteger closeCount = new AtomicInteger();

        @Override
        public void start(Consumer<? super T> receiver) {
            this.receiver = receiver;
        }

        private void emit(T value) {
            receiver.accept(value);
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }
    }

    private static final class FakeOutputPort<T> implements OutputPort<T> {
        private final AtomicInteger offerCount = new AtomicInteger();
        private final AtomicInteger closeCount = new AtomicInteger();

        @Override
        public boolean offer(T value) {
            offerCount.incrementAndGet();
            return true;
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }
    }

    private record RecordingOutput(
            String name, List<String> order, boolean fail) implements OutputPort<Integer> {
        @Override
        public boolean offer(Integer value) {
            return true;
        }

        @Override
        public void close() {
            order.add(name);
            if (fail) {
                throw new IllegalStateException("close failed");
            }
        }
    }
}
