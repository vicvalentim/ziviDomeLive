package com.victorvalentim.zividomelive.core.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Vec3Test {

    @Test
    void zeroIsSharedAndEveryFiniteValueIncludingExtremesIsPreserved() {
        assertSame(Vec3.zero(), Vec3.zero());
        assertEquals(new Vec3(0.0f, 0.0f, 0.0f), Vec3.zero());
        Vec3 extremes = new Vec3(-Float.MAX_VALUE, Float.MIN_VALUE, Float.MAX_VALUE);
        assertEquals(-Float.MAX_VALUE, extremes.x());
        assertEquals(Float.MIN_VALUE, extremes.y());
        assertEquals(Float.MAX_VALUE, extremes.z());
    }

    @Test
    void eachNonFiniteComponentIsRejected() {
        for (float invalid : new float[]{
                Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Vec3(invalid, 0.0f, 0.0f));
            assertThrows(IllegalArgumentException.class,
                    () -> new Vec3(0.0f, invalid, 0.0f));
            assertThrows(IllegalArgumentException.class,
                    () -> new Vec3(0.0f, 0.0f, invalid));
        }
    }
}
