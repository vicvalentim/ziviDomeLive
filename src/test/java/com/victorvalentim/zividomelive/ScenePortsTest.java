package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class ScenePortsTest {

    @Test
    void externalInputIsDeliveredInOrderAtTheFrameBoundary() throws Exception {
        RenderThreadQueue renderQueue = new RenderThreadQueue();
        ScenePorts ports = new ScenePorts(renderQueue, 4);
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
        }, "test-midi-input");
        producer.start();
        producer.join();

        assertTrue(received.isEmpty());
        assertEquals(3, ports.drain());
        assertEquals(List.of(1, 2, 3), received);
        assertSame(Thread.currentThread(), handlerThread.get());
    }

    @Test
    void boundedInputDropsOldestMessagesAndReportsTheLoss() {
        RenderThreadQueue renderQueue = new RenderThreadQueue();
        ScenePorts ports = new ScenePorts(renderQueue, 2);
        FakeInputPort<Integer> input = new FakeInputPort<>();
        List<Integer> received = new ArrayList<>();
        ports.connectInput(input, received::add);

        input.emit(1);
        input.emit(2);
        input.emit(3);

        assertEquals(1, ports.getDroppedInputCount());
        assertEquals(2, ports.drain());
        assertEquals(List.of(2, 3), received);
    }

    @Test
    void stopRejectsOldInputAndOutputBeforeAdaptersAreClosed() {
        RenderThreadQueue renderQueue = new RenderThreadQueue();
        ScenePorts ports = new ScenePorts(renderQueue, 4);
        FakeInputPort<Integer> input = new FakeInputPort<>();
        FakeOutputPort<Integer> output = new FakeOutputPort<>();
        AtomicInteger received = new AtomicInteger();
        ports.connectInput(input, received::set);
        SceneOutputPort<Integer> managedOutput = ports.connectOutput(output);

        ports.stopAccepting();
        input.emit(7);

        assertEquals(0, ports.drain());
        assertEquals(0, received.get());
        assertFalse(managedOutput.offer(8));
        assertEquals(0, output.offers.get());
        assertEquals(0, input.closeCount.get());
        assertEquals(0, output.closeCount.get());

        ports.close();
        assertEquals(1, input.closeCount.get());
        assertEquals(1, output.closeCount.get());
    }

    @Test
    void pauseDropsPendingMessagesUntilTheActivationResumes() {
        ScenePorts ports = new ScenePorts(new RenderThreadQueue(), 4);
        FakeInputPort<Integer> input = new FakeInputPort<>();
        List<Integer> received = new ArrayList<>();
        SceneOutputPort<Integer> output = ports.connectOutput(new FakeOutputPort<>());
        ports.connectInput(input, received::add);

        input.emit(1);
        ports.pause();
        input.emit(2);

        assertEquals(0, ports.drain());
        assertFalse(output.offer(2));

        ports.resume();
        input.emit(3);
        assertEquals(1, ports.drain());
        assertEquals(List.of(3), received);
        ports.close();
    }

    @Test
    void closeUsesReverseOrderAndContinuesAfterAdapterFailure() {
        RenderThreadQueue renderQueue = new RenderThreadQueue();
        ScenePorts ports = new ScenePorts(renderQueue, 4);
        List<String> closed = new ArrayList<>();
        ports.connectOutput(new RecordingOutputPort("first", closed, false));
        ports.connectOutput(new RecordingOutputPort("second", closed, true));
        ports.connectOutput(new RecordingOutputPort("third", closed, false));

        assertDoesNotThrow(ports::close);
        assertEquals(List.of("third", "second", "first"), closed);
        assertDoesNotThrow(ports::close);
    }

    @Test
    void duplicatePortConnectionIsRejected() {
        ScenePorts ports = new ScenePorts(new RenderThreadQueue(), 4);
        FakeInputPort<Integer> input = new FakeInputPort<>();
        ports.connectInput(input, value -> {});

        assertThrows(IllegalArgumentException.class,
                () -> ports.connectInput(input, value -> {}));
        ports.close();
        assertEquals(1, input.closeCount.get());
    }

    private static final class FakeInputPort<T> implements SceneInputPort<T> {
        private Consumer<? super T> receiver;
        private final AtomicInteger closeCount = new AtomicInteger();

        @Override
        public void start(Consumer<? super T> receiver) {
            this.receiver = receiver;
        }

        void emit(T value) {
            receiver.accept(value);
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }
    }

    private static final class FakeOutputPort<T> implements SceneOutputPort<T> {
        private final AtomicInteger offers = new AtomicInteger();
        private final AtomicInteger closeCount = new AtomicInteger();

        @Override
        public boolean offer(T value) {
            offers.incrementAndGet();
            return true;
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }
    }

    private static final class RecordingOutputPort implements SceneOutputPort<Integer> {
        private final String name;
        private final List<String> closed;
        private final boolean fail;

        private RecordingOutputPort(String name, List<String> closed, boolean fail) {
            this.name = name;
            this.closed = closed;
            this.fail = fail;
        }

        @Override
        public boolean offer(Integer value) {
            return true;
        }

        @Override
        public void close() {
            closed.add(name);
            if (fail) {
                throw new IllegalStateException("close failure");
            }
        }
    }
}
