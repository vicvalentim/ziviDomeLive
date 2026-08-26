package com.victorvalentim.zividomelive.core.lifecycle;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceCacheTest {

    @Test
    void factoryRunsOncePerKey() {
        ResourceCache<Object> cache = new ResourceCache<>();
        AtomicInteger creations = new AtomicInteger();
        Object first = cache.getOrCreateBorrowed("asset", () -> {
            creations.incrementAndGet();
            return new Object();
        });
        Object second = cache.getOrCreateBorrowed("asset", Object::new);
        assertSame(first, second);
        assertEquals(1, creations.get());
        assertTrue(cache.contains("asset"));
        assertEquals(1, cache.size());
    }

    @Test
    void ownedReplacementAndRemovalDisposeExactlyOnce() {
        ResourceCache<String> cache = new ResourceCache<>();
        List<String> disposed = new ArrayList<>();
        cache.putOwned("value", "first", disposed::add);
        cache.putOwned("value", "second", disposed::add);
        assertEquals(List.of("first"), disposed);
        assertEquals("second", cache.remove("value"));
        assertEquals(List.of("first", "second"), disposed);
        assertNull(cache.remove("value"));
    }

    @Test
    void borrowedResourcesAreNeverDisposed() {
        ResourceCache<String> cache = new ResourceCache<>();
        AtomicInteger disposals = new AtomicInteger();
        cache.putBorrowed("borrowed", "value");
        cache.close();
        assertEquals(0, disposals.get());
    }

    @Test
    void prefixRemovalDisposesOnlyMatchingOwnedEntries() {
        ResourceCache<String> cache = new ResourceCache<>();
        List<String> disposed = new ArrayList<>();
        cache.putOwned("shape:wire:a", "a", disposed::add);
        cache.putOwned("shape:wire:b", "b", disposed::add);
        cache.putBorrowed("shape:solid:a", "c");
        assertEquals(2, cache.removeByPrefix("shape:wire:"));
        assertEquals(List.of("a", "b"), disposed);
        assertEquals(1, cache.size());
    }

    @Test
    void closeDisposesOwnedResourcesInReverseInsertionOrder() {
        ResourceCache<String> cache = new ResourceCache<>();
        List<String> disposed = new ArrayList<>();
        cache.putOwned("first", "first", disposed::add);
        cache.putOwned("second", "second", disposed::add);
        cache.putOwned("third", "third", disposed::add);
        cache.close();
        cache.close();
        assertEquals(List.of("third", "second", "first"), disposed);
        assertTrue(cache.isClosed());
        assertFalse(cache.contains("first"));
    }

    @Test
    void disposalFailureDoesNotPreventRemainingCleanup() {
        ResourceCache<String> cache = new ResourceCache<>();
        AtomicInteger disposed = new AtomicInteger();
        cache.putOwned("first", "first", value -> disposed.incrementAndGet());
        cache.putOwned("failing", "failing", value -> {
            throw new IllegalStateException("failure");
        });
        assertDoesNotThrow(cache::close);
        assertEquals(1, disposed.get());
    }

    @Test
    void invalidKeysNullResourcesAndClosedMutationAreRejected() {
        ResourceCache<Object> cache = new ResourceCache<>();
        assertThrows(IllegalArgumentException.class, () -> cache.get(" "));
        assertThrows(NullPointerException.class, () -> cache.putBorrowed("null", null));
        assertThrows(NullPointerException.class,
                () -> cache.getOrCreateBorrowed("null-factory", () -> null));
        assertThrows(NullPointerException.class,
                () -> cache.putOwned("owned", new Object(), null));
        cache.close();
        assertThrows(IllegalStateException.class,
                () -> cache.putBorrowed("late", new Object()));
    }
}
