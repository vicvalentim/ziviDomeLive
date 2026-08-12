package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.support.LogManager;
import me.walkerknapp.devolay.DevolayFrameFormatType;
import me.walkerknapp.devolay.DevolayFrameFourCCType;
import me.walkerknapp.devolay.DevolaySender;
import me.walkerknapp.devolay.DevolayVideoFrame;
import processing.opengl.PGraphicsOpenGL;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/** Concrete NDI publication backend with bounded latest-frame-wins delivery. */
final class NdiOutputBackend {

	static final long DEFAULT_SHUTDOWN_TIMEOUT_MILLIS = 1_000L;
	static final int DEFAULT_FRAME_RATE_NUMERATOR = 60;
	static final int DEFAULT_FRAME_RATE_DENOMINATOR = 1;
	static final DevolayFrameFourCCType FRAME_FOUR_CC_TYPE = DevolayFrameFourCCType.RGBA;
	static final DevolayFrameFormatType FRAME_FORMAT_TYPE = DevolayFrameFormatType.PROGRESSIVE;

	/** One slot may be sent while the remaining slots are free or queued. */
	private static final int SLOT_COUNT = 3;
	private static final int BYTES_PER_PIXEL = 4;
	private static final String SENDER_NAME = "ziviDomeLive NDI Output";

	private final Logger logger = LogManager.getLogger();
	private final long shutdownTimeoutMillis;
	private final Object lifecycleLock = new Object();

	private volatile DevolaySender ndiSender;
	private volatile boolean ndiEnabled;
	private volatile boolean ndiUnavailable;
	private volatile String ndiFailureReason = "";
	private volatile boolean ndiWorkerRunning;
	private volatile boolean ndiShutdownPending;
	private boolean ndiRestartRequested;
	private volatile Thread ndiWorkerThread;

	private final ArrayBlockingQueue<NdiFrameSlot> ndiFreeSlots =
			new ArrayBlockingQueue<>(SLOT_COUNT);
	private final ArrayBlockingQueue<NdiFrameSlot> ndiReadySlots =
			new ArrayBlockingQueue<>(SLOT_COUNT);
	private final NdiFrameSlot[] ndiSlots = new NdiFrameSlot[SLOT_COUNT];

	private volatile int ndiFrameRateNumerator;
	private volatile int ndiFrameRateDenominator = DEFAULT_FRAME_RATE_DENOMINATOR;

	private final AtomicLong ndiCapturedFrames = new AtomicLong();
	private final AtomicLong ndiSentFrames = new AtomicLong();
	private final AtomicLong ndiDroppedFrames = new AtomicLong();
	private final AtomicLong ndiFailedFrames = new AtomicLong();

	NdiOutputBackend(int frameRateNumerator, long shutdownTimeoutMillis) {
		if (shutdownTimeoutMillis <= 0) {
			throw new IllegalArgumentException("NDI shutdown timeout must be positive");
		}
		this.ndiFrameRateNumerator = frameRateNumerator;
		this.shutdownTimeoutMillis = shutdownTimeoutMillis;
	}

	/** Enables NDI when it is not already active. */
	void enable() {
		initialize();
	}

	/** Initializes NDI and starts its dedicated sender worker. */
	private void initialize() {
		synchronized (lifecycleLock) {
			if (isEnabled()) {
				return;
			}

			Thread worker = ndiWorkerThread;
			if (worker != null && worker.isAlive()) {
				ndiRestartRequested = true;
				ndiUnavailable = false;
				ndiFailureReason = "";
				logger.info("NDI restart scheduled after the current worker finishes stopping.");
				return;
			}

			if (worker != null || ndiSender != null) {
				ndiWorkerThread = null;
				releaseResourcesLocked();
			}

			initializeLocked();
		}
	}

	/** Creates one NDI activation cycle. Must be called while holding the lifecycle lock. */
	private void initializeLocked() {
		ndiUnavailable = false;
		ndiFailureReason = "";
		ndiShutdownPending = false;
		ndiRestartRequested = false;

		try {
			ndiSender = new DevolaySender(SENDER_NAME);
			initializeSlots();

			Thread worker = new Thread(this::workerLoop, "ziviDomeLive-NDI-Sender");
			worker.setDaemon(true);
			ndiWorkerThread = worker;
			ndiWorkerRunning = true;
			ndiEnabled = true;
			worker.start();

			logger.info("NDI output initialized with a dedicated sender worker.");
		} catch (LinkageError | RuntimeException error) {
			ndiEnabled = false;
			ndiWorkerRunning = false;
			ndiWorkerThread = null;
			releaseResourcesLocked();

			ndiUnavailable = true;
			ndiFailureReason = OutputManager.rootCauseMessage(error);

			logger.warning(
					"NDI unavailable on "
							+ System.getProperty("os.name", "unknown")
							+ "/"
							+ System.getProperty("os.arch", "unknown")
							+ ": "
							+ ndiFailureReason
			);
		}
	}

	/** Creates the fixed NDI frame pool after Devolay initializes successfully. */
	private void initializeSlots() {
		ndiFreeSlots.clear();
		ndiReadySlots.clear();

		for (int index = 0; index < SLOT_COUNT; index++) {
			NdiFrameSlot slot = new NdiFrameSlot();
			ndiSlots[index] = slot;
			ndiFreeSlots.offer(slot);
		}
	}

	/** Captures one selected graphics target into a pooled CPU slot on the draw thread. */
	void capture(PGraphicsOpenGL graphics) {
		if (!isEnabled() || graphics == null || graphics.width <= 0 || graphics.height <= 0) {
			return;
		}

		NdiFrameSlot slot = acquireCaptureSlot();
		if (slot == null) {
			ndiDroppedFrames.incrementAndGet();
			return;
		}

		boolean queued = false;
		try {
			graphics.loadPixels();

			int width = graphics.width;
			int height = graphics.height;
			int pixelCount = Math.multiplyExact(width, height);

			if (graphics.pixels == null || graphics.pixels.length < pixelCount) {
				ndiFailedFrames.incrementAndGet();
				logger.warning("NDI frame skipped: Processing pixel buffer is unavailable or incomplete.");
				return;
			}

			slot.ensureCapacity(width, height);
			System.arraycopy(graphics.pixels, 0, slot.argbPixels, 0, pixelCount);
			slot.width = width;
			slot.height = height;
			slot.pixelCount = pixelCount;
			slot.frameRateNumerator = ndiFrameRateNumerator;
			slot.frameRateDenominator = ndiFrameRateDenominator;

			queued = offerLatestFrame(slot);
			if (queued) {
				ndiCapturedFrames.incrementAndGet();
			} else {
				ndiDroppedFrames.incrementAndGet();
			}
		} catch (RuntimeException error) {
			ndiFailedFrames.incrementAndGet();
			logger.warning("NDI frame capture failed: " + OutputManager.rootCauseMessage(error));
		} finally {
			if (!queued) {
				ndiFreeSlots.offer(slot);
			}
		}
	}

	/** Obtains a capture slot, replacing the oldest queued frame when necessary. */
	private NdiFrameSlot acquireCaptureSlot() {
		NdiFrameSlot slot = ndiFreeSlots.poll();
		if (slot != null) {
			return slot;
		}

		slot = ndiReadySlots.poll();
		if (slot != null) {
			ndiDroppedFrames.incrementAndGet();
		}
		return slot;
	}

	/** Queues the newest frame and drops the oldest pending frame when the queue is full. */
	private boolean offerLatestFrame(NdiFrameSlot slot) {
		if (ndiReadySlots.offer(slot)) {
			return true;
		}

		NdiFrameSlot stale = ndiReadySlots.poll();
		if (stale != null) {
			ndiDroppedFrames.incrementAndGet();
			ndiFreeSlots.offer(stale);
		}

		return ndiReadySlots.offer(slot);
	}

	/** Dedicated NDI conversion and sender loop. No OpenGL calls are made here. */
	private void workerLoop() {
		Thread worker = Thread.currentThread();
		try {
			while (ndiWorkerRunning || !ndiReadySlots.isEmpty()) {
				NdiFrameSlot slot = null;

				try {
					slot = ndiReadySlots.poll(100, TimeUnit.MILLISECONDS);
					if (slot == null) {
						continue;
					}

					DevolaySender sender = ndiSender;
					if (!ndiEnabled || sender == null) {
						if (ndiUnavailable) {
							ndiFailedFrames.incrementAndGet();
						}
						continue;
					}

					slot.prepareDevolayFrame();
					sender.sendVideoFrame(slot.frame);
					ndiSentFrames.incrementAndGet();
				} catch (InterruptedException interrupted) {
					if (!ndiWorkerRunning) {
						Thread.currentThread().interrupt();
						break;
					}
				} catch (Exception | LinkageError error) {
					ndiFailedFrames.incrementAndGet();
					markWorkerUnavailable(worker, error);
				} finally {
					if (slot != null) {
						ndiFreeSlots.offer(slot);
					}
				}
			}
		} finally {
			finishWorker(worker);
		}
	}

	/** Records a worker failure without closing native objects still owned by that worker. */
	private void markWorkerUnavailable(Thread worker, Throwable error) {
		String failureReason = OutputManager.rootCauseMessage(error);
		synchronized (lifecycleLock) {
			if (ndiWorkerThread != worker) {
				return;
			}
			ndiEnabled = false;
			ndiWorkerRunning = false;
			ndiUnavailable = true;
			ndiFailureReason = failureReason;
		}
		logger.warning("NDI sender worker failed and was disabled: " + failureReason);
	}

	/** Completes deferred cleanup after the worker can no longer touch native NDI resources. */
	private void finishWorker(Thread worker) {
		synchronized (lifecycleLock) {
			if (ndiWorkerThread != worker) {
				return;
			}

			ndiWorkerThread = null;
			ndiEnabled = false;
			ndiWorkerRunning = false;
			ndiShutdownPending = false;

			boolean restart = ndiRestartRequested;
			ndiRestartRequested = false;
			releaseResourcesLocked();

			if (restart) {
				initializeLocked();
			}
		}
	}

	/** Changes the frame-rate metadata used for subsequently captured frames. */
	void setFrameRate(int numerator, int denominator) {
		if (numerator <= 0 || denominator <= 0) {
			throw new IllegalArgumentException(
					"NDI frame-rate numerator and denominator must be positive."
			);
		}

		ndiFrameRateNumerator = numerator;
		ndiFrameRateDenominator = denominator;
	}

	/** Stops the worker before releasing its native sender and frame resources. */
	void shutdown() {
		Thread worker;
		synchronized (lifecycleLock) {
			ndiEnabled = false;
			ndiWorkerRunning = false;
			ndiRestartRequested = false;

			worker = ndiWorkerThread;
			if (worker == null) {
				ndiShutdownPending = false;
				releaseResourcesLocked();
				return;
			}

			if (!worker.isAlive()) {
				ndiWorkerThread = null;
				ndiShutdownPending = false;
				releaseResourcesLocked();
				return;
			}

			worker.interrupt();
			if (worker == Thread.currentThread() || ndiShutdownPending) {
				ndiShutdownPending = true;
				return;
			}
			ndiShutdownPending = true;
		}

		boolean stopped = waitForWorker(worker, shutdownTimeoutMillis);
		if (!stopped) {
			String reason = "NDI sender worker did not stop within "
					+ shutdownTimeoutMillis
					+ " ms; native cleanup was deferred.";
			synchronized (lifecycleLock) {
				if (ndiWorkerThread == worker && worker.isAlive()) {
					ndiFailureReason = reason;
				}
			}
			logger.warning(reason);
			return;
		}

		synchronized (lifecycleLock) {
			if (ndiWorkerThread == worker) {
				ndiWorkerThread = null;
				ndiShutdownPending = false;
				releaseResourcesLocked();
			}
		}
		logger.info("NDI output shut down.");
	}

	/** Waits a bounded interval for a worker without clearing an interrupt from the caller. */
	static boolean waitForWorker(Thread worker, long timeoutMillis) {
		if (worker == null || !worker.isAlive()) {
			return true;
		}
		try {
			worker.join(timeoutMillis);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			return false;
		}
		return !worker.isAlive();
	}

	/** Releases resources only after no worker can still use them. Lifecycle lock required. */
	private void releaseResourcesLocked() {
		ndiReadySlots.clear();
		ndiFreeSlots.clear();
		closeSlots();
		closeSender();
	}

	private void closeSlots() {
		for (int index = 0; index < ndiSlots.length; index++) {
			NdiFrameSlot slot = ndiSlots[index];
			ndiSlots[index] = null;

			if (slot != null) {
				try {
					slot.close();
				} catch (RuntimeException | LinkageError error) {
					String failureReason = OutputManager.rootCauseMessage(error);
					if (ndiFailureReason.isEmpty()) {
						ndiFailureReason = failureReason;
					}
					logger.warning("Failed to close an NDI frame slot: " + failureReason);
				}
			}
		}
	}

	private void closeSender() {
		DevolaySender sender = ndiSender;
		ndiSender = null;

		if (sender != null) {
			try {
				sender.close();
			} catch (RuntimeException | LinkageError error) {
				String failureReason = OutputManager.rootCauseMessage(error);
				if (ndiFailureReason.isEmpty()) {
					ndiFailureReason = failureReason;
				}
				logger.warning("Failed to close the NDI sender: " + failureReason);
			}
		}
	}

	OutputManager.OutputState state() {
		return state(isEnabled());
	}

	OutputManager.OutputState state(boolean effectivelyEnabled) {
		return OutputManager.resolveOutputState(
				true,
				ndiUnavailable,
				ndiSender != null || ndiWorkerThread != null,
				effectivelyEnabled,
				ndiShutdownPending && ndiWorkerThread != null);
	}

	boolean isEnabled() {
		return ndiEnabled && ndiSender != null && ndiWorkerRunning;
	}

	String failureReason() {
		return ndiFailureReason;
	}

	long capturedFrames() {
		return ndiCapturedFrames.get();
	}

	long sentFrames() {
		return ndiSentFrames.get();
	}

	long droppedFrames() {
		return ndiDroppedFrames.get();
	}

	long failedFrames() {
		return ndiFailedFrames.get();
	}

	int frameRateNumerator() {
		return ndiFrameRateNumerator;
	}

	int frameRateDenominator() {
		return ndiFrameRateDenominator;
	}

	/** Computes the packed RGBA line stride used by Devolay. */
	static int lineStride(int width) {
		if (width <= 0) {
			throw new IllegalArgumentException("NDI frame width must be positive");
		}
		return Math.multiplyExact(width, BYTES_PER_PIXEL);
	}

	/** Writes Processing ARGB pixels as packed RGBA while preserving source row order. */
	static void writeArgbAsRgba(int[] argbPixels, int pixelCount, ByteBuffer rgbaBuffer) {
		if (argbPixels == null || rgbaBuffer == null || pixelCount < 0
				|| pixelCount > argbPixels.length
				|| rgbaBuffer.capacity() < Math.multiplyExact(pixelCount, BYTES_PER_PIXEL)) {
			throw new IllegalArgumentException("Invalid NDI pixel conversion buffers");
		}

		rgbaBuffer.clear();
		for (int index = 0; index < pixelCount; index++) {
			int pixel = argbPixels[index];
			rgbaBuffer.put((byte) ((pixel >>> 16) & 0xFF));
			rgbaBuffer.put((byte) ((pixel >>> 8) & 0xFF));
			rgbaBuffer.put((byte) (pixel & 0xFF));
			rgbaBuffer.put((byte) ((pixel >>> 24) & 0xFF));
		}
		rgbaBuffer.flip();
	}

	/** Reusable NDI frame slot shared only by the draw thread and the NDI worker. */
	private static final class NdiFrameSlot implements AutoCloseable {

		private final DevolayVideoFrame frame = new DevolayVideoFrame();
		private int[] argbPixels;
		private ByteBuffer rgbaBuffer;
		private int width;
		private int height;
		private int pixelCount;
		private int frameRateNumerator;
		private int frameRateDenominator;

		private void ensureCapacity(int requiredWidth, int requiredHeight) {
			int requiredPixels = Math.multiplyExact(requiredWidth, requiredHeight);
			int requiredBytes = Math.multiplyExact(requiredPixels, BYTES_PER_PIXEL);

			if (argbPixels == null || argbPixels.length != requiredPixels) {
				argbPixels = new int[requiredPixels];
			}

			if (rgbaBuffer == null || rgbaBuffer.capacity() != requiredBytes) {
				rgbaBuffer = ByteBuffer
						.allocateDirect(requiredBytes)
						.order(ByteOrder.LITTLE_ENDIAN);
			}
		}

		private void prepareDevolayFrame() {
			writeArgbAsRgba(argbPixels, pixelCount, rgbaBuffer);

			frame.setResolution(width, height);
			frame.setData(rgbaBuffer);
			frame.setFourCCType(FRAME_FOUR_CC_TYPE);
			frame.setLineStride(lineStride(width));
			frame.setFormatType(FRAME_FORMAT_TYPE);
			frame.setFrameRate(frameRateNumerator, frameRateDenominator);
		}

		@Override
		public void close() {
			frame.close();
			argbPixels = null;
			rgbaBuffer = null;
		}
	}
}
