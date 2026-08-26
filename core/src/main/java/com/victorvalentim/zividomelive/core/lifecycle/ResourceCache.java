package com.victorvalentim.zividomelive.core.lifecycle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Generic keyed cache with explicit borrowed and owned resource semantics. */
public final class ResourceCache<T> implements AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(ResourceCache.class.getName());

    private final Map<String, Entry<T>> entries = new LinkedHashMap<>();
    private boolean closed;

    /** Returns a cached value or null. Reads remain safe after close. */
    public synchronized T get(String key) {
        Entry<T> entry = entries.get(requireKey(key));
        return entry != null ? entry.value : null;
    }

    /** Reports key membership. Reads remain safe after close. */
    public synchronized boolean contains(String key) {
        return entries.containsKey(requireKey(key));
    }

    /** Creates and stores a borrowed non-null value when absent. */
    public synchronized T getOrCreateBorrowed(String key, Supplier<? extends T> factory) {
        return getOrCreate(key, factory, null);
    }

    /** Creates and stores an owned non-null value when absent. */
    public synchronized T getOrCreateOwned(
            String key,
            Supplier<? extends T> factory,
            Consumer<? super T> disposer) {
        return getOrCreate(key, factory, Objects.requireNonNull(disposer, "disposer"));
    }

    /** Stores borrowed state and disposes a different owned value it replaces. */
    public synchronized void putBorrowed(String key, T value) {
        put(key, value, null);
    }

    /** Stores owned state and disposes a different owned value it replaces. */
    public synchronized void putOwned(String key, T value, Consumer<? super T> disposer) {
        put(key, value, Objects.requireNonNull(disposer, "disposer"));
    }

    /** Removes and, when owned, disposes one entry. */
    public synchronized T remove(String key) {
        Entry<T> entry = entries.remove(requireKey(key));
        if (entry == null) {
            return null;
        }
        dispose(entry);
        return entry.value;
    }

    /** Removes every matching prefix and disposes matching owned entries. */
    public synchronized int removeByPrefix(String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        List<String> keys = entries.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .toList();
        keys.forEach(this::remove);
        return keys.size();
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    /** Disposes owned entries in reverse insertion order and drops borrowed references. */
    public synchronized void clear() {
        List<Entry<T>> values = new ArrayList<>(entries.values());
        for (int index = values.size() - 1; index >= 0; index--) {
            dispose(values.get(index));
        }
        entries.clear();
    }

    /** Clears once and permanently rejects new values. */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        clear();
        closed = true;
    }

    private T getOrCreate(
            String key,
            Supplier<? extends T> factory,
            Consumer<? super T> disposer) {
        ensureOpen();
        String normalized = requireKey(key);
        Entry<T> existing = entries.get(normalized);
        if (existing != null) {
            return existing.value;
        }
        T value = Objects.requireNonNull(
                Objects.requireNonNull(factory, "factory").get(), "resource");
        put(normalized, value, disposer);
        return value;
    }

    private void put(String key, T value, Consumer<? super T> disposer) {
        ensureOpen();
        String normalized = requireKey(key);
        Objects.requireNonNull(value, "value");
        Entry<T> previous = entries.put(normalized, new Entry<>(value, disposer));
        if (previous != null && previous.value != value) {
            dispose(previous);
        }
    }

    private void dispose(Entry<T> entry) {
        if (entry.disposer == null) {
            return;
        }
        try {
            entry.disposer.accept(entry.value);
        } catch (RuntimeException error) {
            LOGGER.log(System.Logger.Level.WARNING, "Resource disposal failed", error);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Resource cache is closed.");
        }
    }

    private static String requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Resource key cannot be blank.");
        }
        return key;
    }

    private static final class Entry<T> {
        private final T value;
        private final Consumer<? super T> disposer;

        private Entry(T value, Consumer<? super T> disposer) {
            this.value = value;
            this.disposer = disposer;
        }
    }
}
