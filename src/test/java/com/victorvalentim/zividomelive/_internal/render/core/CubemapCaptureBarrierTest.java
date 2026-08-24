package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubemapCaptureBarrierTest {

    @Test
    void batchIsUnreadableUntilEveryFaceSharesOneCaptureTimestamp() {
        CubemapCaptureBarrier barrier = new CubemapCaptureBarrier();
        barrier.begin(1_000L);

        for (int index = 0; index < CubemapFace.count(); index++) {
            CubemapFace face = CubemapFace.at(index);
            barrier.completeFace(face);
            assertEquals(1_000L, barrier.faceTimestampNanos(face));
            assertFalse(barrier.isReadable());
        }

        barrier.publish(1_350L);

        assertTrue(barrier.isReadable());
        assertFalse(barrier.isCaptureInProgress());
        assertEquals(1_000L, barrier.captureTimestampNanos());
        assertEquals(1_350L, barrier.publicationTimestampNanos());
        assertEquals(1L, barrier.publicationSequence());
    }

    @Test
    void partialCaptureCannotBePublishedOrRead() {
        CubemapCaptureBarrier barrier = new CubemapCaptureBarrier();
        barrier.begin(2_000L);
        barrier.completeFace(CubemapFace.POSITIVE_X);

        assertThrows(IllegalStateException.class, () -> barrier.publish(2_100L));
        assertFalse(barrier.isReadable());

        barrier.abort();
        assertFalse(barrier.isCaptureInProgress());
        assertFalse(barrier.isReadable());
    }

    @Test
    void startingTheNextBatchWithholdsThePreviouslyPublishedMutableTarget() {
        CubemapCaptureBarrier barrier = new CubemapCaptureBarrier();
        barrier.begin(3_000L);
        for (int index = 0; index < CubemapFace.count(); index++) {
            barrier.completeFace(CubemapFace.at(index));
        }
        barrier.publish(3_100L);
        assertTrue(barrier.isReadable());

        barrier.begin(4_000L);

        assertFalse(barrier.isReadable());
        assertTrue(barrier.isCaptureInProgress());
    }
}
