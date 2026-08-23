package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.opengl.PGraphicsOpenGL;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class FrameViewsRoutingTest {

	@Test
	void resolvesCompletedFrameWithoutInspectingFacadeRenderers() {
		ziviDomeLive dome = new ziviDomeLive(new PApplet());
		OutputManagerImpl outputs = new OutputManagerImpl(dome);
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
		OutputManagerImpl outputs = new OutputManagerImpl(dome);
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
		OutputManagerImpl outputs = new OutputManagerImpl(new ziviDomeLive(new PApplet()));

		assertNull(outputs.resolveGraphics(null, ViewType.STANDARD));
		assertNull(outputs.resolveGraphics(
				view -> {
					throw new IllegalStateException("unavailable");
				},
				ViewType.STANDARD));
	}

}
