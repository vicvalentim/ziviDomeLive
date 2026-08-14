package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SceneResourceCacheTest {

    @Test
    void factoryRunsOncePerKey() {
        SceneResourceCache<Object> cache = new SceneResourceCache<>();
        AtomicInteger creations = new AtomicInteger();

        Object first = cache.getOrCreateBorrowed("image", () -> {
            creations.incrementAndGet();
            return new Object();
        });
        Object second = cache.getOrCreateBorrowed("image", () -> new Object());

        assertSame(first, second);
        assertEquals(1, creations.get());
    }

    @Test
    void onlyOwnedResourcesAreDisposedExactlyOnce() {
        SceneResourceCache<String> cache = new SceneResourceCache<>();
        AtomicInteger disposals = new AtomicInteger();
        cache.putBorrowed("borrowed", "a");
        cache.putOwned("owned", "b", ignored -> disposals.incrementAndGet());

        cache.close();
        cache.close();

        assertEquals(1, disposals.get());
        assertThrows(IllegalStateException.class, () -> cache.putBorrowed("late", "c"));
    }

    @Test
    void prefixInvalidationDisposesMatchingOwnedEntries() {
        SceneResourceCache<String> cache = new SceneResourceCache<>();
        AtomicInteger disposals = new AtomicInteger();
        cache.putOwned("shape:wire:a", "a", ignored -> disposals.incrementAndGet());
        cache.putOwned("shape:wire:b", "b", ignored -> disposals.incrementAndGet());
        cache.putBorrowed("shape:solid:a", "c");

        assertEquals(2, cache.removeByPrefix("shape:wire:"));
        assertEquals(2, disposals.get());
        assertEquals(1, cache.size());
    }
}
