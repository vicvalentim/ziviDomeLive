package com.victorvalentim.zividomelive;

// Package-private implementation grouped physically under _internal/render/core.

import java.util.Arrays;

/**
 * Nanosecond-resolution publication barrier for one logical six-face cubemap capture.
 *
 * <p>OpenGL renders the faces sequentially on one context. This barrier gives every face in the
 * batch one monotonic capture timestamp and withholds the mutable native target from consumers
 * until all six faces and the final mipmap pass have completed. It deliberately never sleeps or
 * spins on the render thread.</p>
 */
final class CubemapCaptureBarrier {
    private static final int FACE_COUNT = 6;
    private static final int COMPLETE_FACE_MASK = (1 << FACE_COUNT) - 1;

    private final long[] faceTimestampsNanos = new long[FACE_COUNT];
    private int completedFaceMask;
    private long captureTimestampNanos;
    private long publicationTimestampNanos;
    private long publicationSequence;
    private boolean captureInProgress;
    private boolean published;

    void begin(long timestampNanos) {
        if (captureInProgress) {
            throw new IllegalStateException("A cubemap capture batch is already active.");
        }
        captureTimestampNanos = timestampNanos;
        publicationTimestampNanos = 0L;
        completedFaceMask = 0;
        Arrays.fill(faceTimestampsNanos, 0L);
        captureInProgress = true;
    }

    void completeFace(CubemapFace face) {
        if (!captureInProgress) {
            throw new IllegalStateException("No cubemap capture batch is active.");
        }
        int faceIndex = face.index();
        faceTimestampsNanos[faceIndex] = captureTimestampNanos;
        completedFaceMask |= 1 << faceIndex;
    }

    void publish(long timestampNanos) {
        if (!captureInProgress) {
            throw new IllegalStateException("No cubemap capture batch is active.");
        }
        if (completedFaceMask != COMPLETE_FACE_MASK) {
            throw new IllegalStateException(
                    "Cubemap capture cannot be published before all six faces complete.");
        }
        publicationTimestampNanos = Math.max(timestampNanos, captureTimestampNanos);
        publicationSequence++;
        captureInProgress = false;
        published = true;
    }

    void abort() {
        captureInProgress = false;
        published = false;
        completedFaceMask = 0;
        publicationTimestampNanos = 0L;
        Arrays.fill(faceTimestampsNanos, 0L);
    }

    void reset() {
        abort();
        captureTimestampNanos = 0L;
        publicationSequence = 0L;
    }

    boolean isReadable() {
        return published && !captureInProgress;
    }

    boolean isCaptureInProgress() {
        return captureInProgress;
    }

    long captureTimestampNanos() {
        return captureTimestampNanos;
    }

    long faceTimestampNanos(CubemapFace face) {
        return faceTimestampsNanos[face.index()];
    }

    long publicationTimestampNanos() {
        return publicationTimestampNanos;
    }

    long publicationSequence() {
        return publicationSequence;
    }
}
