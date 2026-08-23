package com.victorvalentim.zividomelive;

// Package-private implementation grouped physically under _internal/render/gl.

import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL3ES3;
import com.victorvalentim.zividomelive.performance.GpuTimerArchitecture;
import com.victorvalentim.zividomelive.performance.GpuTimerBackend;
import com.victorvalentim.zividomelive.performance.GpuTimerPolicy;
import processing.core.PApplet;
import processing.opengl.PGL;
import processing.opengl.PJOGL;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Capability-driven GPU measurement session for Processing/JOGL.
 *
 * <p>Timer-query backends are never trusted merely because a counter exists. Before a timer
 * backend is used for normal profiling, a small number of synchronized probes verifies that the
 * reported GPU duration fits inside the CPU envelope that completely contains the same OpenGL
 * workload. The qualification decision is cached per active GL context and policy so a benchmark
 * warm-up can qualify the backend once and a subsequent statistics reset does not repeat the
 * synchronization inside the measured window.</p>
 *
 * <p>When {@link GpuTimerPolicy#ARCHITECTURE_AWARE} cannot obtain a qualified timer query, the
 * session may fall back to OpenGL fence completion. Fence measurements are intentionally reported
 * as {@link GpuTimerBackend#FENCE_COMPLETION}: they are CPU-observed GPU completion latency, not a
 * timer-query GPU execution duration.</p>
 */
final class ProcessingGpuMeasurementSession implements AutoCloseable {
    private enum Phase {
        UNINITIALIZED,
        QUALIFYING_TIMER,
        TIMER,
        FENCE,
        FAILED,
        CLOSED
    }

    private static final Map<Object, EnumMap<GpuTimerPolicy, CachedDecision>> DECISION_CACHE =
            new WeakHashMap<>();

    private final PApplet parent;
    private final int poolSize;
    private final GpuTimerPolicy policy;
    private final ProcessingGlAdapter glAdapter;
    private final ProcessingGlAdapter.GpuTimerResultConsumer qualificationConsumer =
            this::acceptQualificationResult;
    private final double[] qualificationRatios =
            new double[GpuTimerQualification.REQUIRED_PROBES];

    private Phase phase = Phase.UNINITIALIZED;
    private Discovery discovery;
    private ProcessingGlAdapter.GpuTimerQuerySession timerSession;
    private FenceCompletionSession fenceSession;
    private GpuTimerBackend candidateBackend = GpuTimerBackend.NONE;
    private GpuTimerBackend effectiveBackend = GpuTimerBackend.NONE;
    private GpuTimerArchitecture architecture = GpuTimerArchitecture.OTHER;
    private int qualificationProbeCount;
    private boolean qualificationProbeActive;
    private boolean allQualificationProbesPlausible = true;
    private long qualificationCpuStartNanos;
    private long qualificationGpuNanos;
    private String rejectedCandidates = "";
    private String diagnostic = "GPU measurement discovery has not run yet.";

    /**
     * Creates one lazy measurement session for the active Processing OpenGL context.
     *
     * @param parent Processing sketch owning the active OpenGL context
     * @param poolSize bounded number of asynchronous query/fence slots
     * @param policy GPU measurement selection policy
     */
    public ProcessingGpuMeasurementSession(
            PApplet parent,
            int poolSize,
            GpuTimerPolicy policy) {
        if (parent == null) {
            throw new IllegalArgumentException("Processing parent must not be null.");
        }
        if (poolSize < 2 || poolSize > 64) {
            throw new IllegalArgumentException("GPU measurement pool size must be between 2 and 64.");
        }
        if (policy == null) {
            throw new IllegalArgumentException("GPU timer policy must not be null.");
        }
        this.parent = parent;
        this.poolSize = poolSize;
        this.policy = policy;
        this.glAdapter = ProcessingGlAdapter.getDefault();
    }

    /**
     * Begins either a qualification interval or a normal asynchronous measurement interval.
     *
     * @param frameId absolute frame identifier used to align delayed GPU results
     * @param profilingSessionId profiling session identifier used to reject stale results
     * @param consumer recipient for completed asynchronous measurements
     * @return {@code true} when an interval was opened for this frame
     */
    public boolean begin(
            long frameId,
            long profilingSessionId,
            ProcessingGlAdapter.GpuTimerResultConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        ensureInitialized();
        if (phase == Phase.FAILED) {
            throw new IllegalStateException(diagnostic);
        }
        if (phase == Phase.CLOSED) {
            throw new IllegalStateException("GPU measurement session is closed.");
        }

        if (phase == Phase.QUALIFYING_TIMER) {
            boolean opened = beginQualification(frameId, profilingSessionId);
            if (!opened && phase == Phase.FENCE && fenceSession != null) {
                return fenceSession.begin(frameId, profilingSessionId, consumer);
            }
            if (!opened && phase == Phase.FAILED) {
                throw new IllegalStateException(diagnostic);
            }
            return opened;
        }
        if (phase == Phase.TIMER) {
            try {
                return timerSession.begin(frameId, profilingSessionId, consumer);
            } catch (RuntimeException | LinkageError error) {
                if (fallbackFromRuntimeTimerFailure(error)) {
                    return phase == Phase.FENCE
                            && fenceSession.begin(frameId, profilingSessionId, consumer);
                }
                throw error;
            }
        }
        if (phase == Phase.FENCE) {
            return fenceSession.begin(frameId, profilingSessionId, consumer);
        }
        return false;
    }

    /** Ends the interval opened by {@link #begin}. */
    public void end() {
        if (phase == Phase.QUALIFYING_TIMER) {
            endQualification();
            return;
        }
        if (phase == Phase.TIMER && timerSession != null) {
            try {
                timerSession.end();
            } catch (RuntimeException | LinkageError error) {
                if (!fallbackFromRuntimeTimerFailure(error)) {
                    throw error;
                }
            }
            return;
        }
        if (phase == Phase.FENCE && fenceSession != null) {
            fenceSession.end();
        }
    }

    /**
     * Polls delayed normal results without waiting. Qualification probes are synchronous only.
     *
     * @param consumer recipient for completed asynchronous measurements
     */
    public void collectAvailable(ProcessingGlAdapter.GpuTimerResultConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (phase == Phase.TIMER && timerSession != null) {
            timerSession.collectAvailable(consumer);
        } else if (phase == Phase.FENCE && fenceSession != null) {
            fenceSession.collectAvailable(consumer);
        }
    }

    /** @return normal results currently in flight, excluding qualification probes */
    public int pendingResultCount() {
        if (phase == Phase.TIMER && timerSession != null) {
            return timerSession.pendingResultCount();
        }
        if (phase == Phase.FENCE && fenceSession != null) {
            return fenceSession.pendingResultCount();
        }
        return 0;
    }

    /** @return {@code true} while synchronized timer qualification is still in progress */
    public boolean isQualifying() {
        return phase == Phase.QUALIFYING_TIMER;
    }

    /** @return {@code true} after a timer or fence backend is ready for normal samples */
    public boolean isReady() {
        return phase == Phase.TIMER || phase == Phase.FENCE;
    }

    /** @return effective measurement backend, or NONE until qualification succeeds */
    public GpuTimerBackend getBackend() {
        return effectiveBackend;
    }

    /** @return normalized architecture metadata for the active GL context */
    public GpuTimerArchitecture getArchitecture() {
        return architecture;
    }

    /** @return human-readable discovery/qualification evidence for diagnostics */
    public String getDiagnostic() {
        return diagnostic;
    }

    @Override
    public void close() {
        if (phase == Phase.CLOSED) {
            return;
        }
        closeTimerSession();
        closeFenceSession();
        phase = Phase.CLOSED;
        effectiveBackend = GpuTimerBackend.NONE;
    }

    private void ensureInitialized() {
        if (phase != Phase.UNINITIALIZED) {
            return;
        }
        discovery = discover();
        architecture = discovery.architecture;

        CachedDecision cached = cachedDecision(discovery.contextIdentity, policy);
        if (cached != null) {
            architecture = cached.architecture;
            diagnostic = cached.diagnostic;
            applyCachedDecision(cached);
            return;
        }

        if (policy == GpuTimerPolicy.TIME_ELAPSED_EXCLUSIVE) {
            if (discovery.elapsedAvailable) {
                beginTimerQualification(GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE);
            } else {
                failAndCache("Explicit TIME_ELAPSED_EXCLUSIVE requested, but the active OpenGL "
                        + "context does not expose a usable elapsed timer counter.");
            }
            return;
        }

        if (discovery.timestampAvailable) {
            beginTimerQualification(GpuTimerBackend.TIMESTAMP_PAIR);
            return;
        }

        if (policy.allowsElapsedFallback() && discovery.elapsedAvailable) {
            beginTimerQualification(GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE);
            return;
        }

        if (policy.allowsFenceFallback() && discovery.fenceAvailable) {
            activateFence("No usable timer-query candidate was exposed; using asynchronous GPU "
                    + "fence completion latency.");
            return;
        }

        failAndCache("No GPU measurement backend satisfies policy " + policy + " on "
                + architecture + " (timestampBits=" + discovery.timestampCounterBits
                + ", elapsedBits=" + discovery.elapsedCounterBits
                + ", fenceSync=" + discovery.fenceAvailable + ").");
    }

    private boolean beginQualification(long frameId, long profilingSessionId) {
        try {
            finishGpu();
            qualificationCpuStartNanos = System.nanoTime();
            qualificationGpuNanos = 0L;
            qualificationProbeActive = timerSession.begin(
                    frameId,
                    profilingSessionId,
                    qualificationConsumer);
            if (!qualificationProbeActive) {
                throw new IllegalStateException("No timer-query slot was available for qualification.");
            }
            return true;
        } catch (RuntimeException | LinkageError error) {
            qualificationProbeActive = false;
            rejectCandidate("qualification begin failed: " + describe(error));
            return false;
        }
    }

    private void endQualification() {
        if (!qualificationProbeActive || timerSession == null) {
            return;
        }
        try {
            timerSession.end();
            finishGpu();
            long cpuEnvelopeNanos = Math.max(0L, System.nanoTime() - qualificationCpuStartNanos);
            timerSession.collectAvailable(qualificationConsumer);

            boolean plausible = GpuTimerQualification.isPlausible(
                    qualificationGpuNanos,
                    cpuEnvelopeNanos);
            allQualificationProbesPlausible &= plausible;
            qualificationRatios[qualificationProbeCount] =
                    GpuTimerQualification.ratio(qualificationGpuNanos, cpuEnvelopeNanos);
            qualificationProbeCount++;

            if (qualificationProbeCount >= GpuTimerQualification.REQUIRED_PROBES) {
                double medianRatio = GpuTimerQualification.median(
                        qualificationRatios,
                        qualificationProbeCount);
                if (allQualificationProbesPlausible) {
                    qualifyCandidate(medianRatio);
                } else {
                    rejectCandidate("synchronized-envelope validation failed (median GPU/CPU "
                            + "ratio=" + formatRatio(medianRatio) + ").");
                }
            }
        } catch (RuntimeException | LinkageError error) {
            rejectCandidate("qualification end failed: " + describe(error));
        } finally {
            qualificationProbeActive = false;
        }
    }

    private void acceptQualificationResult(
            long frameId,
            long profilingSessionId,
            long elapsedNanos) {
        qualificationGpuNanos = elapsedNanos;
    }

    private void qualifyCandidate(double medianRatio) {
        effectiveBackend = candidateBackend;
        phase = Phase.TIMER;
        diagnostic = "GPU timer qualification passed for " + candidateBackend + " on "
                + architecture + " using " + qualificationProbeCount
                + " synchronized probes (median GPU/CPU envelope ratio="
                + formatRatio(medianRatio) + ").";
        cacheDecision(discovery.contextIdentity, policy,
                new CachedDecision(effectiveBackend, architecture, diagnostic));
    }

    private void rejectCandidate(String reason) {
        String rejection = candidateBackend + " rejected: " + reason;
        rejectedCandidates = rejectedCandidates.isEmpty()
                ? rejection
                : rejectedCandidates + " " + rejection;
        closeTimerSession();

        if (candidateBackend == GpuTimerBackend.TIMESTAMP_PAIR
                && policy.allowsElapsedFallback()
                && discovery.elapsedAvailable) {
            beginTimerQualification(GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE);
            diagnostic = rejectedCandidates + " Trying TIME_ELAPSED_EXCLUSIVE.";
            return;
        }

        if (policy.allowsFenceFallback() && discovery.fenceAvailable) {
            activateFence(rejectedCandidates + " Falling back to asynchronous GPU fence "
                    + "completion latency; this is not GPU execution time.");
            return;
        }

        failAndCache(rejectedCandidates + " No permitted fallback backend remains.");
    }

    private void beginTimerQualification(GpuTimerBackend backend) {
        closeTimerSession();
        candidateBackend = backend;
        effectiveBackend = GpuTimerBackend.NONE;
        qualificationProbeCount = 0;
        qualificationProbeActive = false;
        allQualificationProbesPlausible = true;
        for (int index = 0; index < qualificationRatios.length; index++) {
            qualificationRatios[index] = Double.NaN;
        }
        GpuTimerPolicy exactPolicy = backend == GpuTimerBackend.TIMESTAMP_PAIR
                ? GpuTimerPolicy.SAFE
                : GpuTimerPolicy.TIME_ELAPSED_EXCLUSIVE;
        timerSession = glAdapter.createGpuTimerQuerySession(parent, poolSize, exactPolicy);
        phase = Phase.QUALIFYING_TIMER;
        diagnostic = "Qualifying GPU timer candidate " + backend + " on " + architecture + ".";
    }

    private boolean fallbackFromRuntimeTimerFailure(Throwable error) {
        String reason = "Qualified timer backend " + effectiveBackend
                + " failed at runtime: " + describe(error) + ".";
        closeTimerSession();
        if (policy.allowsFenceFallback() && discovery != null && discovery.fenceAvailable) {
            activateFence(reason + " Falling back to asynchronous GPU fence completion latency; "
                    + "this is not GPU execution time.");
            return true;
        }
        phase = Phase.FAILED;
        effectiveBackend = GpuTimerBackend.NONE;
        diagnostic = reason;
        if (discovery != null) {
            cacheDecision(discovery.contextIdentity, policy,
                    new CachedDecision(GpuTimerBackend.NONE, architecture, diagnostic));
        }
        return false;
    }

    private void activateFence(String reason) {
        closeTimerSession();
        closeFenceSession();
        fenceSession = new FenceCompletionSession(
                parent,
                glAdapter,
                poolSize,
                discovery.contextIdentity,
                architecture);
        effectiveBackend = GpuTimerBackend.FENCE_COMPLETION;
        phase = Phase.FENCE;
        diagnostic = reason;
        cacheDecision(discovery.contextIdentity, policy,
                new CachedDecision(effectiveBackend, architecture, diagnostic));
    }

    private void failAndCache(String reason) {
        closeTimerSession();
        closeFenceSession();
        effectiveBackend = GpuTimerBackend.NONE;
        phase = Phase.FAILED;
        diagnostic = reason;
        if (discovery != null) {
            cacheDecision(discovery.contextIdentity, policy,
                    new CachedDecision(GpuTimerBackend.NONE, architecture, diagnostic));
        }
    }

    private void applyCachedDecision(CachedDecision cached) {
        effectiveBackend = cached.backend;
        if (cached.backend == GpuTimerBackend.TIMESTAMP_PAIR
                || cached.backend == GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE) {
            GpuTimerPolicy exactPolicy = cached.backend == GpuTimerBackend.TIMESTAMP_PAIR
                    ? GpuTimerPolicy.SAFE
                    : GpuTimerPolicy.TIME_ELAPSED_EXCLUSIVE;
            timerSession = glAdapter.createGpuTimerQuerySession(parent, poolSize, exactPolicy);
            phase = Phase.TIMER;
        } else if (cached.backend == GpuTimerBackend.FENCE_COMPLETION) {
            if (!discovery.fenceAvailable) {
                failAndCache("Cached FENCE_COMPLETION backend is no longer available on the "
                        + "active OpenGL context.");
                return;
            }
            fenceSession = new FenceCompletionSession(
                    parent,
                    glAdapter,
                    poolSize,
                    discovery.contextIdentity,
                    architecture);
            phase = Phase.FENCE;
        } else {
            phase = Phase.FAILED;
        }
    }

    private Discovery discover() {
        return glAdapter.withPgl(parent, pgl -> {
            if (!(pgl instanceof PJOGL pjogl) || pjogl.context == null || pjogl.gl == null) {
                throw new IllegalStateException("Processing PJOGL context is not available.");
            }
            if (!pjogl.gl.isGL2ES2()) {
                throw new IllegalStateException(
                        "The active JOGL profile does not expose GL2ES2 timer-query functions.");
            }

            GL2ES2 gl = pjogl.gl.getGL2ES2();
            GpuTimerArchitecture detectedArchitecture = GpuTimerArchitecture.detect(
                    System.getProperty("os.name"),
                    System.getProperty("os.arch"),
                    pgl.getString(PGL.VENDOR),
                    pgl.getString(PGL.RENDERER));

            boolean commonTimerFunctions =
                    pjogl.gl.isFunctionAvailable("glGetQueryiv")
                            && pjogl.gl.isFunctionAvailable("glGetQueryObjectiv")
                            && pjogl.gl.isFunctionAvailable("glGetQueryObjectui64v");
            boolean timestampCallable = commonTimerFunctions
                    && pjogl.gl.isFunctionAvailable("glQueryCounter");
            boolean elapsedCallable = commonTimerFunctions
                    && pjogl.gl.isFunctionAvailable("glBeginQuery")
                    && pjogl.gl.isFunctionAvailable("glEndQuery");

            int timestampBits = timestampCallable
                    ? queryCounterBits(gl, GL2ES2.GL_TIMESTAMP)
                    : 0;
            int elapsedBits = elapsedCallable
                    ? queryCounterBits(gl, GL2ES2.GL_TIME_ELAPSED)
                    : 0;

            boolean fenceCallable = pjogl.gl.isGL3ES3()
                    && pjogl.gl.isFunctionAvailable("glFenceSync")
                    && pjogl.gl.isFunctionAvailable("glClientWaitSync")
                    && pjogl.gl.isFunctionAvailable("glDeleteSync");

            return new Discovery(
                    pjogl.context,
                    detectedArchitecture,
                    timestampCallable && timestampBits > 0,
                    elapsedCallable && elapsedBits > 0,
                    fenceCallable,
                    timestampBits,
                    elapsedBits);
        });
    }

    private void finishGpu() {
        glAdapter.withPgl(parent, pgl -> {
            if (!(pgl instanceof PJOGL pjogl) || pjogl.gl == null || !pjogl.gl.isGL2ES2()) {
                throw new IllegalStateException("Processing JOGL context cannot synchronize GPU work.");
            }
            pjogl.gl.getGL2ES2().glFinish();
            return null;
        });
    }

    private void closeTimerSession() {
        ProcessingGlAdapter.GpuTimerQuerySession current = timerSession;
        timerSession = null;
        if (current != null) {
            try {
                current.close();
            } catch (RuntimeException | LinkageError ignored) {
                // The owning GL context may already be gone.
            }
        }
    }

    private void closeFenceSession() {
        FenceCompletionSession current = fenceSession;
        fenceSession = null;
        if (current != null) {
            try {
                current.close();
            } catch (RuntimeException | LinkageError ignored) {
                // The owning GL context may already be gone.
            }
        }
    }

    private static int queryCounterBits(GL2ES2 gl, int target) {
        int[] bits = new int[1];
        gl.glGetQueryiv(target, GL2ES2.GL_QUERY_COUNTER_BITS, bits, 0);
        return Math.max(0, bits[0]);
    }

    private static String describe(Throwable error) {
        String detail = error.getMessage();
        return error.getClass().getSimpleName()
                + (detail == null || detail.isBlank() ? "" : ": " + detail);
    }

    private static String formatRatio(double value) {
        return Double.isFinite(value)
                ? String.format(Locale.ROOT, "%.3f", value)
                : "unavailable";
    }

    private static synchronized CachedDecision cachedDecision(
            Object contextIdentity,
            GpuTimerPolicy policy) {
        EnumMap<GpuTimerPolicy, CachedDecision> decisions = DECISION_CACHE.get(contextIdentity);
        return decisions == null ? null : decisions.get(policy);
    }

    private static synchronized void cacheDecision(
            Object contextIdentity,
            GpuTimerPolicy policy,
            CachedDecision decision) {
        EnumMap<GpuTimerPolicy, CachedDecision> decisions = DECISION_CACHE.computeIfAbsent(
                contextIdentity,
                ignored -> new EnumMap<>(GpuTimerPolicy.class));
        decisions.put(policy, decision);
    }

    private record Discovery(
            Object contextIdentity,
            GpuTimerArchitecture architecture,
            boolean timestampAvailable,
            boolean elapsedAvailable,
            boolean fenceAvailable,
            int timestampCounterBits,
            int elapsedCounterBits) {
    }

    private record CachedDecision(
            GpuTimerBackend backend,
            GpuTimerArchitecture architecture,
            String diagnostic) {
    }

    /** Bounded asynchronous fence-completion pool. */
    private static final class FenceCompletionSession implements AutoCloseable {
        private final PApplet parent;
        private final ProcessingGlAdapter glAdapter;
        private final Object contextIdentity;
        private final GpuTimerArchitecture architecture;
        private final long[] syncs;
        private final long[] frameIds;
        private final long[] profilingSessionIds;
        private final long[] cpuStartNanos;
        private int activeSlot = -1;
        private boolean closed;

        private FenceCompletionSession(
                PApplet parent,
                ProcessingGlAdapter glAdapter,
                int poolSize,
                Object contextIdentity,
                GpuTimerArchitecture architecture) {
            this.parent = parent;
            this.glAdapter = glAdapter;
            this.contextIdentity = contextIdentity;
            this.architecture = architecture;
            this.syncs = new long[poolSize];
            this.frameIds = new long[poolSize];
            this.profilingSessionIds = new long[poolSize];
            this.cpuStartNanos = new long[poolSize];
        }

        private boolean begin(
                long frameId,
                long profilingSessionId,
                ProcessingGlAdapter.GpuTimerResultConsumer consumer) {
            if (closed) {
                throw new IllegalStateException("GPU fence completion session is closed.");
            }
            if (activeSlot >= 0) {
                throw new IllegalStateException("A GPU fence completion interval is already active.");
            }
            collectAvailable(consumer);
            int freeSlot = findFreeSlot();
            if (freeSlot < 0) {
                return false;
            }
            frameIds[freeSlot] = frameId;
            profilingSessionIds[freeSlot] = profilingSessionId;
            cpuStartNanos[freeSlot] = System.nanoTime();
            activeSlot = freeSlot;
            return true;
        }

        private void end() {
            if (closed || activeSlot < 0) {
                return;
            }
            int endingSlot = activeSlot;
            activeSlot = -1;
            glAdapter.withPgl(parent, pgl -> {
                GL3ES3 gl = requireGl3Es3(pgl, contextIdentity);
                long sync = gl.glFenceSync(GL3ES3.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
                if (sync == 0L) {
                    throw new IllegalStateException("glFenceSync returned no synchronization object.");
                }
                syncs[endingSlot] = sync;
                gl.glFlush();
                return null;
            });
        }

        private void collectAvailable(ProcessingGlAdapter.GpuTimerResultConsumer consumer) {
            Objects.requireNonNull(consumer, "consumer");
            if (closed || pendingResultCount() == 0) {
                return;
            }
            glAdapter.withPgl(parent, pgl -> {
                GL3ES3 gl = requireGl3Es3(pgl, contextIdentity);
                for (int slot = 0; slot < syncs.length; slot++) {
                    long sync = syncs[slot];
                    if (sync == 0L) {
                        continue;
                    }
                    int result = gl.glClientWaitSync(sync, 0, 0L);
                    if (result == GL3ES3.GL_WAIT_FAILED) {
                        throw new IllegalStateException("glClientWaitSync failed while polling GPU completion.");
                    }
                    if (result != GL3ES3.GL_ALREADY_SIGNALED
                            && result != GL3ES3.GL_CONDITION_SATISFIED) {
                        continue;
                    }
                    long completionNanos = Math.max(1L, System.nanoTime() - cpuStartNanos[slot]);
                    gl.glDeleteSync(sync);
                    syncs[slot] = 0L;
                    consumer.accept(
                            frameIds[slot],
                            profilingSessionIds[slot],
                            completionNanos);
                }
                return null;
            });
        }

        private int pendingResultCount() {
            int count = 0;
            for (long sync : syncs) {
                if (sync != 0L) count++;
            }
            return count;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            try {
                glAdapter.withPgl(parent, pgl -> {
                    if (!(pgl instanceof PJOGL pjogl)
                            || pjogl.context != contextIdentity
                            || pjogl.gl == null
                            || !pjogl.gl.isGL3ES3()) {
                        return null;
                    }
                    GL3ES3 gl = pjogl.gl.getGL3ES3();
                    for (int slot = 0; slot < syncs.length; slot++) {
                        if (syncs[slot] != 0L) {
                            gl.glDeleteSync(syncs[slot]);
                            syncs[slot] = 0L;
                        }
                    }
                    return null;
                });
            } finally {
                activeSlot = -1;
                closed = true;
            }
        }

        private int findFreeSlot() {
            for (int slot = 0; slot < syncs.length; slot++) {
                if (syncs[slot] == 0L && slot != activeSlot) {
                    return slot;
                }
            }
            return -1;
        }

        private static GL3ES3 requireGl3Es3(PGL pgl, Object expectedContextIdentity) {
            if (!(pgl instanceof PJOGL pjogl)
                    || pjogl.context == null
                    || pjogl.context != expectedContextIdentity
                    || pjogl.gl == null
                    || !pjogl.gl.isGL3ES3()) {
                throw new IllegalStateException(
                        "The active JOGL context does not expose the expected GL3ES3 sync API.");
            }
            return pjogl.gl.getGL3ES3();
        }
    }
}
