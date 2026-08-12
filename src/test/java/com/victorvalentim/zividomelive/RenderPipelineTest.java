package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.manager.OutputManager;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.opengl.PGraphicsOpenGL;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RenderPipelineTest {

	@Test
	void rejectsMissingRuntime() {
		assertThrows(NullPointerException.class, () -> new RenderPipeline(null));
	}

	@Test
	void skipsEveryFrameStepWhenBackendIsNotReady() {
		RecordingRuntime runtime = new RecordingRuntime(false);

		new RenderPipeline(runtime).renderFrame();

		assertEquals(List.of("ready"), runtime.calls);
	}

	@Test
	void facadeDrawPreservesFrameOrderAndSendsCompletedOutputBeforePreview() throws Exception {
		RecordingRuntime runtime = new RecordingRuntime(true);
		setInitialized(runtime);

		runtime.draw();

		assertEquals(List.of(
				"ready",
				"clear",
				"reset",
				"ensure-preview",
				"sync-scene",
				"preview-requirements",
				"output-requirements",
				"capture",
				"output-passes",
				"send-output",
				"preview-passes",
				"display-preview",
				"floating-preview",
				"controls"
		), runtime.calls);
	}

	private static void setInitialized(ziviDomeLive runtime) throws Exception {
		Field field = ziviDomeLive.class.getDeclaredField("initState");
		field.setAccessible(true);
		field.set(runtime, ziviDomeLive.InitState.MANAGERS_READY);
	}

	private static final class RecordingRuntime extends ziviDomeLive {
		private final List<String> calls = new ArrayList<>();
		private final boolean ready;
		private final OutputManager outputManager;

		private RecordingRuntime(boolean ready) {
			super(new PApplet());
			this.ready = ready;
			this.outputManager = new RecordingOutputManager(this, calls);
		}

		@Override
		boolean isRenderContentReady() {
			calls.add("ready");
			return ready;
		}

		@Override
		void clearBackground() {
			calls.add("clear");
		}

		@Override
		void handleGraphicsReset() {
			calls.add("reset");
		}

		@Override
		void ensurePreviewRenderers() {
			calls.add("ensure-preview");
		}

		@Override
		void syncCurrentSceneToRenderers() {
			calls.add("sync-scene");
		}

		@Override
		RenderRequirementsPolicy.Requirements computePreviewRequirements() {
			calls.add("preview-requirements");
			return RenderRequirementsPolicy.forPreview(
					RenderMode.FULL, ViewType.STANDARD, true);
		}

		@Override
		RenderRequirementsPolicy.Requirements computeOutputRequirements() {
			calls.add("output-requirements");
			return RenderRequirementsPolicy.forOutputs(
					true, true, true, true, true);
		}

		@Override
		PGraphicsOpenGL[] captureMasterCubemap(
				RenderRequirementsPolicy.Requirements preview,
				RenderRequirementsPolicy.Requirements output) {
			calls.add("capture");
			return null;
		}

		@Override
		void renderOutputPipeline(
				RenderRequirementsPolicy.Requirements output,
				PGraphicsOpenGL[] masterFaces) {
			calls.add("output-passes");
		}

		@Override
		void renderPreviewPipeline(
				RenderRequirementsPolicy.Requirements preview,
				RenderRequirementsPolicy.Requirements output,
				PGraphicsOpenGL[] masterFaces) {
			calls.add("preview-passes");
		}

		@Override
		void displayPreviewCurrentView() {
			calls.add("display-preview");
		}

		@Override
		public boolean isShowPreview() {
			return true;
		}

		@Override
		public void drawFloatingPreview() {
			calls.add("floating-preview");
		}

		@Override
		void drawControlPanel() {
			calls.add("controls");
		}

		@Override
		public OutputManager getOutputManager() {
			return outputManager;
		}
	}

	private static final class RecordingOutputManager extends OutputManager {
		private final List<String> calls;

		private RecordingOutputManager(ziviDomeLive parent, List<String> calls) {
			super(parent);
			this.calls = calls;
		}

		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public void sendOutput() {
			calls.add("send-output");
		}
	}
}
