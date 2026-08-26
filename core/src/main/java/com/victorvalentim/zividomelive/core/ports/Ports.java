package com.victorvalentim.zividomelive.core.ports;

import com.victorvalentim.zividomelive.core.task.FrameThreadQueue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Activation boundary for generic input and output adapters.
 *
 * <p>Input is bounded, drops the oldest event on overflow, and is delivered only by an explicit
 * drain on the bound frame thread. Pause drops stale input and rejects new input until resume.
 * Connected adapters close in reverse registration order.</p>
 */
public final class Ports implements AutoCloseable {

    public static final int DEFAULT_INPUT_CAPACITY = 256;
    public static final int DEFAULT_MAX_EVENTS_PER_FRAME = 32;

    private static final System.Logger LOGGER = System.getLogger(Ports.class.getName());

    private final FrameThreadQueue frameQueue;
    private final int inputCapacity;
    private final int maxEventsPerFrame;
    private final Deque<Runnable> pendingInput = new ArrayDeque<>();
    private final List<AutoCloseable> adapters = new ArrayList<>();
    private final Map<Object, Boolean> connected = new IdentityHashMap<>();
    private long droppedInputCount;
    private boolean accepting = true;
    private boolean paused;
    private boolean closed;

    /** Creates default ports with a frame queue bound to the calling thread. */
    public Ports() {
        this(new FrameThreadQueue());
    }

    /** Creates default ports using the supplied frame-thread authority. */
    public Ports(FrameThreadQueue frameQueue) {
        this(frameQueue, DEFAULT_INPUT_CAPACITY, DEFAULT_MAX_EVENTS_PER_FRAME);
    }

    /** Creates ports with a custom positive input capacity and default per-frame budget. */
    public Ports(FrameThreadQueue frameQueue, int inputCapacity) {
        this(frameQueue, inputCapacity, DEFAULT_MAX_EVENTS_PER_FRAME);
    }

    /** Creates ports with custom positive capacity and per-frame handler budget. */
    public Ports(FrameThreadQueue frameQueue, int inputCapacity, int maxEventsPerFrame) {
        this.frameQueue = Objects.requireNonNull(frameQueue, "frameQueue");
        if (inputCapacity < 1) {
            throw new IllegalArgumentException("Input capacity must be positive.");
        }
        if (maxEventsPerFrame < 1) {
            throw new IllegalArgumentException("Per-frame input budget must be positive.");
        }
        this.inputCapacity = inputCapacity;
        this.maxEventsPerFrame = maxEventsPerFrame;
    }

    /** Connects and owns one input adapter by instance identity. */
    public <T> void connectInput(InputPort<T> port, Consumer<? super T> handler) {
        Objects.requireNonNull(port, "port");
        Objects.requireNonNull(handler, "handler");
        synchronized (this) {
            ensureAccepting();
            register(port);
        }
        try {
            port.start(value -> enqueue(handler, value));
        } catch (RuntimeException | Error error) {
            unregisterFailedConnection(port);
            throw error;
        }
    }

    /**
     * Connects and owns one output adapter, returning a pause/disposal-guarded view.
     */
    public <T> OutputPort<T> connectOutput(OutputPort<T> port) {
        Objects.requireNonNull(port, "port");
        synchronized (this) {
            ensureAccepting();
            register(port);
        }
        return new ManagedOutputPort<>(port);
    }

    public synchronized long getDroppedInputCount() {
        return droppedInputCount;
    }

    public synchronized int getPendingInputCount() {
        return pendingInput.size();
    }

    public synchronized boolean isPaused() {
        return paused;
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    /**
     * Delivers at most the configured finite snapshot on the bound frame thread.
     *
     * @return handlers invoked
     */
    public int drain() {
        frameQueue.requireFrameThread();
        int limit;
        synchronized (this) {
            if (!accepting || paused) {
                return 0;
            }
            limit = Math.min(pendingInput.size(), maxEventsPerFrame);
        }
        int handled = 0;
        while (handled < limit) {
            Runnable event;
            synchronized (this) {
                if (!accepting || paused) {
                    break;
                }
                event = pendingInput.pollFirst();
            }
            if (event == null) {
                break;
            }
            try {
                event.run();
            } catch (RuntimeException error) {
                LOGGER.log(System.Logger.Level.WARNING, "Input port handler failed", error);
            }
            handled++;
        }
        return handled;
    }

    /** Stops admission and drops pending input without closing adapters yet. */
    public synchronized void stopAccepting() {
        if (!accepting) {
            return;
        }
        accepting = false;
        pendingInput.clear();
    }

    /** Drops pending input and rejects input/output work until resume. */
    public synchronized void pause() {
        ensureAccepting();
        paused = true;
        pendingInput.clear();
    }

    /** Resumes admission without replaying input that arrived while paused. */
    public synchronized void resume() {
        ensureAccepting();
        paused = false;
    }

    /** Stops admission and closes connected adapters in reverse registration order. */
    @Override
    public void close() {
        List<AutoCloseable> toClose;
        synchronized (this) {
            if (closed) {
                return;
            }
            stopAccepting();
            closed = true;
            toClose = new ArrayList<>(adapters);
            adapters.clear();
            connected.clear();
        }
        for (int index = toClose.size() - 1; index >= 0; index--) {
            try {
                toClose.get(index).close();
            } catch (Exception | LinkageError error) {
                LOGGER.log(System.Logger.Level.WARNING, "Port cleanup failed", error);
            }
        }
    }

    private <T> void enqueue(Consumer<? super T> handler, T value) {
        synchronized (this) {
            if (!accepting || paused) {
                return;
            }
            if (pendingInput.size() == inputCapacity) {
                pendingInput.removeFirst();
                droppedInputCount++;
            }
            pendingInput.addLast(() -> handler.accept(value));
        }
    }

    private void register(AutoCloseable adapter) {
        if (connected.containsKey(adapter)) {
            throw new IllegalArgumentException("Port is already connected.");
        }
        connected.put(adapter, Boolean.TRUE);
        adapters.add(adapter);
    }

    private void unregisterFailedConnection(AutoCloseable adapter) {
        synchronized (this) {
            connected.remove(adapter);
            for (int index = 0; index < adapters.size(); index++) {
                if (adapters.get(index) == adapter) {
                    adapters.remove(index);
                    break;
                }
            }
        }
        try {
            adapter.close();
        } catch (Exception | LinkageError closeError) {
            LOGGER.log(System.Logger.Level.WARNING, "Failed port could not be closed", closeError);
        }
    }

    private synchronized boolean canOffer() {
        return accepting && !paused && !closed;
    }

    private void ensureAccepting() {
        if (!accepting || closed) {
            throw new IllegalStateException("Ports are closed for this activation.");
        }
    }

    private final class ManagedOutputPort<T> implements OutputPort<T> {
        private final OutputPort<T> delegate;

        private ManagedOutputPort(OutputPort<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean offer(T value) {
            return canOffer() && delegate.offer(value);
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
