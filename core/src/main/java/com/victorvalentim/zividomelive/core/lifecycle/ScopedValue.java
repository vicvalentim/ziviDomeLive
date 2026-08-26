package com.victorvalentim.zividomelive.core.lifecycle;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Ownership-safe scoped override for a host value.
 *
 * <p>The first write captures the previous value. Close restores it only when the host still
 * contains the last value applied by this scope, so a later owner is never overwritten.</p>
 */
public final class ScopedValue<T> implements AutoCloseable {

    private final Supplier<? extends T> reader;
    private final Consumer<? super T> writer;
    private final BiPredicate<? super T, ? super T> equality;
    private T previous;
    private T applied;
    private boolean touched;
    private boolean closed;

    /** Creates a scope using {@link Objects#equals(Object, Object)} ownership comparison. */
    public ScopedValue(Supplier<? extends T> reader, Consumer<? super T> writer) {
        this(reader, writer, Objects::equals);
    }

    /** Creates a scope with an explicit ownership comparison strategy. */
    public ScopedValue(
            Supplier<? extends T> reader,
            Consumer<? super T> writer,
            BiPredicate<? super T, ? super T> equality) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.writer = Objects.requireNonNull(writer, "writer");
        this.equality = Objects.requireNonNull(equality, "equality");
    }

    /** Captures the previous value once, applies the new value, and records actual host state. */
    public synchronized void set(T value) {
        ensureOpen();
        if (!touched) {
            previous = reader.get();
            touched = true;
        }
        writer.accept(value);
        applied = reader.get();
    }

    /** @return current host value */
    public synchronized T get() {
        ensureOpen();
        return reader.get();
    }

    public synchronized boolean isTouched() {
        return touched;
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    /** Restores the captured value only while this scope still owns the current value. */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        if (touched && equality.test(reader.get(), applied)) {
            writer.accept(previous);
        }
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Scoped value is closed.");
        }
    }
}
