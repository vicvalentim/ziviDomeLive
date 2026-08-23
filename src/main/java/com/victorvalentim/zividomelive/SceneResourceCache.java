package com.victorvalentim.zividomelive;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Typed scene cache with explicit borrowed/owned resource semantics.
 */
final class SceneResourceCache<T> {

    /** Defines whether closing the cache invokes a resource disposer. */
    enum Ownership {
        /** Cache clears only its Java reference. */
        BORROWED,
        /** Cache invokes the registered disposer before clearing its reference. */
        OWNED
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<String, Entry<T>> entries = new LinkedHashMap<>();
    private boolean closed;

    /**
     * Returns a cached value, or {@code null} when the key is absent.
     *
     * @param key resource key
     * @return cached value or null
     */
    public synchronized T get(String key) {
        Entry<T> entry = entries.get(requireKey(key));
        return entry != null ? entry.value : null;
    }

    /**
     * Reports whether a key is cached.
     *
     * @param key resource key
     * @return true when present
     */
    public synchronized boolean contains(String key) {
        return entries.containsKey(requireKey(key));
    }

    /**
     * Creates and caches a borrowed value when absent.
     *
     * @param key resource key
     * @param factory non-null value factory
     * @return cached or newly created value
     */
    public synchronized T getOrCreateBorrowed(String key, Supplier<? extends T> factory) {
        return getOrCreate(key, factory, Ownership.BORROWED, null);
    }

    /**
     * Creates and caches an owned value when absent.
     *
     * @param key resource key
     * @param factory non-null value factory
     * @param disposer cleanup invoked for the owned value
     * @return cached or newly created value
     */
    public synchronized T getOrCreateOwned(
            String key,
            Supplier<? extends T> factory,
            Consumer<? super T> disposer) {
        return getOrCreate(key, factory, Ownership.OWNED, disposer);
    }

    /**
     * Stores a borrowed value, disposing any different owned value it replaces.
     *
     * @param key resource key
     * @param value borrowed value
     */
    public synchronized void putBorrowed(String key, T value) {
        put(key, value, Ownership.BORROWED, null);
    }

    /**
     * Stores an owned value, disposing any different owned value it replaces.
     *
     * @param key resource key
     * @param value owned value
     * @param disposer cleanup invoked when ownership ends
     */
    public synchronized void putOwned(String key, T value, Consumer<? super T> disposer) {
        put(key, value, Ownership.OWNED, disposer);
    }

    /**
     * Removes one entry and disposes it if it is owned.
     *
     * @param key resource key
     * @return removed value, or null when absent
     */
    public synchronized T remove(String key) {
        Entry<T> entry = entries.remove(requireKey(key));
        if (entry == null) {
            return null;
        }
        dispose(entry);
        return entry.value;
    }

    /**
     * Removes all entries whose keys begin with the provided prefix.
     *
     * @param prefix key prefix, including the empty prefix for all entries
     * @return number of removed entries
     */
    public synchronized int removeByPrefix(String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        List<String> keys = entries.keySet().stream().filter(key -> key.startsWith(prefix)).toList();
        keys.forEach(this::remove);
        return keys.size();
    }

    /** @return number of cached entries */
    public synchronized int size() {
        return entries.size();
    }

    /** @return whether this cache rejects new values */
    public synchronized boolean isClosed() {
        return closed;
    }

    /** Disposes owned entries in reverse insertion order and clears borrowed references. */
    public synchronized void clear() {
        List<Entry<T>> values = new ArrayList<>(entries.values());
        for (int i = values.size() - 1; i >= 0; i--) {
            dispose(values.get(i));
        }
        entries.clear();
    }

    synchronized void close() {
        if (closed) {
            return;
        }
        clear();
        closed = true;
    }

    private T getOrCreate(
            String key,
            Supplier<? extends T> factory,
            Ownership ownership,
            Consumer<? super T> disposer) {
        ensureOpen();
        String normalized = requireKey(key);
        Entry<T> existing = entries.get(normalized);
        if (existing != null) {
            return existing.value;
        }
        T value = Objects.requireNonNull(Objects.requireNonNull(factory, "factory").get(), "resource");
        put(normalized, value, ownership, disposer);
        return value;
    }

    private void put(String key, T value, Ownership ownership, Consumer<? super T> disposer) {
        ensureOpen();
        String normalized = requireKey(key);
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(ownership, "ownership");
        if (ownership == Ownership.OWNED) {
            Objects.requireNonNull(disposer, "Owned resources require a disposer.");
        }
        Entry<T> previous = entries.put(normalized, new Entry<>(value, ownership, disposer));
        if (previous != null && previous.value != value) {
            dispose(previous);
        }
    }

    private void dispose(Entry<T> entry) {
        if (entry.ownership != Ownership.OWNED || entry.disposer == null) {
            return;
        }
        try {
            entry.disposer.accept(entry.value);
        } catch (RuntimeException error) {
            LOGGER.log(Level.WARNING, "Scene resource disposal failed", error);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Scene resource cache is closed.");
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
        private final Ownership ownership;
        private final Consumer<? super T> disposer;

        private Entry(T value, Ownership ownership, Consumer<? super T> disposer) {
            this.value = value;
            this.ownership = ownership;
            this.disposer = disposer;
        }
    }
}
