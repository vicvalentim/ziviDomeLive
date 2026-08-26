package com.victorvalentim.zividomelive.core.lifecycle;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScopedValueTest {

    @Test
    void untouchedScopeDoesNotRewriteHostState() {
        AtomicReference<String> host = new AtomicReference<>("original");
        ScopedValue<String> scope = new ScopedValue<>(host::get, host::set);
        scope.close();
        assertEquals("original", host.get());
        assertFalse(scope.isTouched());
    }

    @Test
    void firstPreviousValueIsRestoredAfterMultipleOwnedWrites() {
        AtomicReference<String> host = new AtomicReference<>("original");
        ScopedValue<String> scope = new ScopedValue<>(host::get, host::set);
        scope.set("first");
        scope.set("second");
        assertEquals("second", scope.get());
        scope.close();
        assertEquals("original", host.get());
    }

    @Test
    void laterOwnerIsNeverOverwritten() {
        AtomicReference<String> host = new AtomicReference<>("original");
        ScopedValue<String> scope = new ScopedValue<>(host::get, host::set);
        scope.set("temporary");
        host.set("later-owner");
        scope.close();
        assertEquals("later-owner", host.get());
    }

    @Test
    void customBitwiseFloatEqualitySupportsOwnershipSafeRestoration() {
        AtomicReference<Float> host = new AtomicReference<>(1.0f);
        ScopedValue<Float> scope = new ScopedValue<>(host::get, host::set,
                (left, right) -> Float.floatToIntBits(left) == Float.floatToIntBits(right));
        scope.set(-0.0f);
        assertTrue(scope.isTouched());
        scope.close();
        assertEquals(1.0f, host.get());
    }

    @Test
    void closeIsIdempotentAndGuardsAccess() {
        AtomicReference<String> host = new AtomicReference<>("original");
        ScopedValue<String> scope = new ScopedValue<>(host::get, host::set);
        scope.set("temporary");
        scope.close();
        scope.close();
        assertTrue(scope.isClosed());
        assertThrows(IllegalStateException.class, () -> scope.set("late"));
        assertThrows(IllegalStateException.class, scope::get);
    }
}
