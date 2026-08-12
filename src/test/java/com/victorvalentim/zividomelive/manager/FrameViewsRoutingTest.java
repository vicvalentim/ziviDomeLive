package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.FrameViews;
import com.victorvalentim.zividomelive.RenderMode;
import com.victorvalentim.zividomelive.ViewType;
import com.victorvalentim.zividomelive.ziviDomeLive;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.opengl.PGraphicsOpenGL;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class FrameViewsRoutingTest {

	@Test
	void resolvesCompletedFrameWithoutInspectingFacadeRenderers() {
		ziviDomeLive dome = new ziviDomeLive(new PApplet());
		OutputManager outputs = new OutputManager(dome);
		PGraphicsOpenGL expected = new PGraphicsOpenGL();
		AtomicReference<ViewType> requestedView = new AtomicReference<>();
		FrameViews frameViews = view -> {
			requestedView.set(view);
			return expected;
		};

		PGraphicsOpenGL resolved = outputs.resolveGraphics(
				frameViews, ViewType.EQUIRECTANGULAR);

		assertSame(expected, resolved);
		assertSame(ViewType.EQUIRECTANGULAR, requestedView.get());
	}

	@Test
	void dedicatedRenderModeSelectsTheEffectiveFinalView() {
		ziviDomeLive dome = new ziviDomeLive(new PApplet());
		dome.setRenderMode(RenderMode.SKYBOX);
		OutputManager outputs = new OutputManager(dome);
		AtomicReference<ViewType> requestedView = new AtomicReference<>();
		FrameViews frameViews = view -> {
			requestedView.set(view);
			return null;
		};

		outputs.resolveGraphics(frameViews, ViewType.DOMEMASTER);

		assertSame(ViewType.SKYBOX, requestedView.get());
	}

	@Test
	void missingOrFailingFrameContractIsSafelyIgnored() {
		OutputManager outputs = new OutputManager(new ziviDomeLive(new PApplet()));

		assertNull(outputs.resolveGraphics(null, ViewType.STANDARD));
		assertNull(outputs.resolveGraphics(
				view -> {
					throw new IllegalStateException("unavailable");
				},
				ViewType.STANDARD));
	}

	@Test
	void frameAwareSendPreservesLegacyNoArgumentOverride() {
		LegacySendOutputManager outputs = new LegacySendOutputManager(
				new ziviDomeLive(new PApplet()));

		outputs.sendOutput(view -> null);

		assertEquals(1, outputs.sendCalls);
	}

	private static final class LegacySendOutputManager extends OutputManager {
		private int sendCalls;

		private LegacySendOutputManager(ziviDomeLive parent) {
			super(parent);
		}

		@Override
		public void sendOutput() {
			sendCalls++;
		}
	}
}
