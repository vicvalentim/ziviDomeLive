package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.ViewType;
import com.victorvalentim.zividomelive.ziviDomeLive;
import me.walkerknapp.devolay.DevolayFrameFormatType;
import me.walkerknapp.devolay.DevolayFrameFourCCType;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputManagerHardeningTest {

	@Test
	void lifecycleStateKeepsAvailabilityInitializationAndPublicationDistinct() {
		assertAll(
				() -> assertEquals(OutputManager.OutputState.UNAVAILABLE,
						OutputManager.resolveOutputState(false, false, false, false, false)),
				() -> assertEquals(OutputManager.OutputState.UNAVAILABLE,
						OutputManager.resolveOutputState(true, true, true, false, false)),
				() -> assertEquals(OutputManager.OutputState.AVAILABLE,
						OutputManager.resolveOutputState(true, false, false, false, false)),
				() -> assertEquals(OutputManager.OutputState.INITIALIZED,
						OutputManager.resolveOutputState(true, false, true, false, false)),
				() -> assertEquals(OutputManager.OutputState.ENABLED,
						OutputManager.resolveOutputState(true, false, true, true, false)),
				() -> assertEquals(OutputManager.OutputState.STOPPING,
						OutputManager.resolveOutputState(true, false, true, false, true)));
	}

	@Test
	void defaultStatesReflectPlatformWithoutEnablingPublication() {
		OutputManager manager = new OutputManager(new ziviDomeLive(new PApplet()));

		assertEquals(OutputManager.OutputState.AVAILABLE,
				manager.getOutputState(OutputManager.OutputType.NDI));
		assertEquals("", manager.getOutputFailureReason(OutputManager.OutputType.NDI));
		assertEquals(0, manager.getNdiFailedFrames());

		switch (manager.getLocalTextureBackendName()) {
			case "Spout":
				assertEquals(OutputManager.OutputState.AVAILABLE,
						manager.getOutputState(OutputManager.OutputType.SPOUT));
				assertEquals(OutputManager.OutputState.UNAVAILABLE,
						manager.getOutputState(OutputManager.OutputType.SYPHON));
				break;
			case "Syphon":
				assertEquals(OutputManager.OutputState.AVAILABLE,
						manager.getOutputState(OutputManager.OutputType.SYPHON));
				assertEquals(OutputManager.OutputState.UNAVAILABLE,
						manager.getOutputState(OutputManager.OutputType.SPOUT));
				break;
			default:
				assertEquals(OutputManager.OutputState.UNAVAILABLE,
						manager.getOutputState(OutputManager.OutputType.SPOUT));
				assertEquals(OutputManager.OutputState.UNAVAILABLE,
						manager.getOutputState(OutputManager.OutputType.SYPHON));
				break;
		}

		assertFalse(manager.isActive());
		assertFalse(manager.requiresView(ViewType.DOMEMASTER));
	}

	@Test
	void ndiEncodingPreservesPixelAndRowOrder() {
		int[] twoByTwoArgb = {
				0x11223344, 0x55667788,
				0x99AABBCC, 0xDDEEFF00
		};
		ByteBuffer rgba = ByteBuffer.allocateDirect(16);

		OutputManager.writeArgbAsRgba(twoByTwoArgb, twoByTwoArgb.length, rgba);

		byte[] encoded = new byte[rgba.remaining()];
		rgba.get(encoded);
		assertArrayEquals(new byte[]{
				0x22, 0x33, 0x44, 0x11,
				0x66, 0x77, (byte) 0x88, 0x55,
				(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0x99,
				(byte) 0xEE, (byte) 0xFF, 0x00, (byte) 0xDD
		}, encoded);
	}

	@Test
	void ndiMetadataUsesPackedProgressiveRgba() {
		assertAll(
				() -> assertEquals(DevolayFrameFourCCType.RGBA,
						OutputManager.NDI_FRAME_FOUR_CC_TYPE),
				() -> assertEquals(DevolayFrameFormatType.PROGRESSIVE,
						OutputManager.NDI_FRAME_FORMAT_TYPE),
				() -> assertEquals(4, OutputManager.ndiLineStride(1)),
				() -> assertEquals(7680, OutputManager.ndiLineStride(1920)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> OutputManager.ndiLineStride(0)));
	}

	@Test
	void ndiPipelineKeepsThreeBoundedFrameSlots() throws Exception {
		OutputManager manager = new OutputManager(new ziviDomeLive(new PApplet()));
		Object[] slots = (Object[]) readField(manager, "ndiSlots");
		BlockingQueue<?> freeSlots = (BlockingQueue<?>) readField(manager, "ndiFreeSlots");
		BlockingQueue<?> readySlots = (BlockingQueue<?>) readField(manager, "ndiReadySlots");

		assertAll(
				() -> assertEquals(3, slots.length),
				() -> assertEquals(3, freeSlots.remainingCapacity()),
				() -> assertEquals(3, readySlots.remainingCapacity()));
	}

	@Test
	void ndiShutdownIsBoundedAndDefersCleanupUntilWorkerStops() throws Exception {
		OutputManager manager = new OutputManager(new ziviDomeLive(new PApplet()), 25);
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		Thread worker = new Thread(() -> {
			started.countDown();
			while (release.getCount() > 0) {
				try {
					release.await();
				} catch (InterruptedException ignored) {
					// Simulates a native send that does not honor Java interruption.
				}
			}
		}, "blocked-ndi-test-worker");
		worker.setDaemon(true);
		worker.start();
		assertTrue(started.await(1, TimeUnit.SECONDS));

		setField(manager, "ndiWorkerThread", worker);
		setField(manager, "ndiWorkerRunning", true);
		setField(manager, "ndiEnabled", true);

		try {
			long startedAt = System.nanoTime();
			manager.shutdownOutputs();
			Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

			assertTrue(elapsed.compareTo(Duration.ofMillis(500)) < 0, elapsed.toString());
			assertEquals(OutputManager.OutputState.STOPPING,
					manager.getOutputState(OutputManager.OutputType.NDI));
			assertTrue(manager.getOutputFailureReason(OutputManager.OutputType.NDI)
					.contains("native cleanup was deferred"));

			long repeatedAt = System.nanoTime();
			manager.shutdownOutputs();
			Duration repeated = Duration.ofNanos(System.nanoTime() - repeatedAt);
			assertTrue(repeated.compareTo(Duration.ofMillis(100)) < 0, repeated.toString());
		} finally {
			release.countDown();
			worker.join(1_000);
		}

		manager.shutdownOutputs();
		assertEquals(OutputManager.OutputState.AVAILABLE,
				manager.getOutputState(OutputManager.OutputType.NDI));
	}

	@Test
	void constructorRejectsInvalidShutdownTimeout() {
		ziviDomeLive dome = new ziviDomeLive(new PApplet());
		assertThrows(IllegalArgumentException.class, () -> new OutputManager(dome, 0));
	}

	private static void setField(OutputManager manager, String fieldName, Object value) throws Exception {
		Field field = OutputManager.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(manager, value);
	}

	private static Object readField(OutputManager manager, String fieldName) throws Exception {
		Field field = OutputManager.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(manager);
	}
}
