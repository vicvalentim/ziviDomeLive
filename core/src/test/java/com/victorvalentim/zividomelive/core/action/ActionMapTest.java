package com.victorvalentim.zividomelive.core.action;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionMapTest {

    @Test
    void registerTriggerAndMissingActionAreSynchronous() {
        ActionMap actions = new ActionMap();
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<Thread> thread = new AtomicReference<>();
        actions.register("reload", () -> {
            calls.incrementAndGet();
            thread.set(Thread.currentThread());
        });

        assertTrue(actions.trigger("reload"));
        assertEquals(1, calls.get());
        assertSame(Thread.currentThread(), thread.get());
        assertFalse(actions.trigger("missing"));
    }

    @Test
    void registrationReplacementDoesNotIncreaseSize() {
        ActionMap actions = new ActionMap();
        AtomicInteger result = new AtomicInteger();
        actions.register("mode", () -> result.set(1));
        actions.register("mode", () -> result.set(2));

        assertEquals(1, actions.size());
        actions.trigger("mode");
        assertEquals(2, result.get());
    }

    @Test
    void unregisterAndClearRemoveActions() {
        ActionMap actions = new ActionMap();
        actions.register("one", () -> { });
        actions.register("two", () -> { });
        actions.unregister("one");
        assertFalse(actions.trigger("one"));
        assertEquals(1, actions.size());
        actions.clear();
        assertEquals(0, actions.size());
    }

    @Test
    void namesMustBeNonBlankAndActionsNonNull() {
        ActionMap actions = new ActionMap();
        assertThrows(IllegalArgumentException.class, () -> actions.register(null, () -> { }));
        assertThrows(IllegalArgumentException.class, () -> actions.register(" ", () -> { }));
        assertThrows(IllegalArgumentException.class, () -> actions.trigger(""));
        assertThrows(NullPointerException.class, () -> actions.register("null", null));
    }

    @Test
    void closeIsIdempotentAndGuardsAllRegistryOperations() {
        ActionMap actions = new ActionMap();
        actions.register("before", () -> { });
        actions.close();
        actions.close();

        assertTrue(actions.isClosed());
        assertThrows(IllegalStateException.class, actions::size);
        assertThrows(IllegalStateException.class, actions::clear);
        assertThrows(IllegalStateException.class, () -> actions.trigger("before"));
        assertThrows(IllegalStateException.class, () -> actions.unregister("before"));
        assertThrows(IllegalStateException.class,
                () -> actions.register("after", () -> { }));
    }
}
