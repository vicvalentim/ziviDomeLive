package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.performance.PerformanceMetric;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Coordinates one frame using the runtime renderer backend.
 *
 * <p>The Processing window always displays preview-resolution FBOs. High-resolution output
 * FBOs remain offscreen and are submitted only to enabled backends after all relevant
 * {@code endDraw()} calls have completed.</p>
 */
final class RenderPipeline {

	private static final Logger LOGGER = LogManager.getLogger();

	private final ziviDomeLive runtime;
	private final FrameViews finalFrameViews;

	RenderPipeline(ziviDomeLive runtime) {
		this.runtime = Objects.requireNonNull(runtime, "runtime cannot be null");
		this.finalFrameViews = runtime::resolveFinalFrame;
	}

	/** Returns the stable, allocation-free final-frame boundary owned by this pipeline. */
	FrameViews finalFrameViews() {
		return finalFrameViews;
	}

	/**
	 * Executes one frame on the Processing render thread.
	 *
	 * <p>Frame order:</p>
	 * <ol>
	 *   <li>Clear the window background.</li>
	 *   <li>Apply any pending output-resolution change.</li>
	 *   <li>Ensure preview FBOs are valid for the current window size.</li>
	 *   <li>Resolve requirements and capture at most one master cubemap.</li>
	 *   <li>Render required output-resolution passes and submit only active external outputs.</li>
	 *   <li>Render preview passes, reusing completed output projections when available.</li>
	 *   <li>Composite the preview FBO onto the window.</li>
	 *   <li>Optionally draw the floating fisheye thumbnail.</li>
	 *   <li>Draw the control panel.</li>
	 * </ol>
	 */
	void renderFrame() {
		if (!runtime.isRenderContentReady()) {
			LOGGER.severe("Cannot render content: renderer or scene not initialized.");
			return;
		}

		PerformanceMonitor monitor = runtime.performanceMonitor();
		boolean profiling = monitor.isEnabled();
		PerformanceMonitor previous = profiling ? PerformanceMonitor.attach(monitor) : null;
		long pipelineStarted = profiling ? monitor.start() : 0L;
		boolean gpuTiming = profiling && runtime.beginGpuPerformanceInterval();
		try {
			runtime.clearBackground();
			runtime.handleGraphicsReset();
			runtime.ensurePreviewRenderers();
			runtime.syncCurrentSceneToRenderers();

			RenderRequirementsPolicy.Requirements preview = runtime.computePreviewRequirements();
			RenderRequirementsPolicy.Requirements output = runtime.computeOutputRequirements();
			if (profiling) {
				monitor.setExpectedPassCounts(
						(preview.needsStandard() ? 1 : 0) + (output.needsStandard() ? 1 : 0),
						preview.needsCubemapSource() || output.needsCubemapSource() ? 1 : 0,
						output.needsFisheye() || preview.needsFisheye() ? 1 : 0,
						output.needsEquirectangular() || preview.needsEquirectangular() ? 1 : 0,
						output.needsCubemapLayout() || preview.needsCubemapLayout() ? 1 : 0,
						matchingPreviewCopies(preview, output));
			}
			runtime.captureMasterCubemap(preview, output);

			OutputManagerImpl outputManager = runtime.outputManagerInternal();
			if (runtime.hasOutputRenderDemand()) {
				long outputStarted = profiling ? monitor.start() : 0L;
				try {
					runtime.renderOutputPipeline(output);
					if (outputManager != null && outputManager.isActive()) {
						outputManager.sendOutput(finalFrameViews);
					}
				} finally {
					if (profiling) monitor.record(PerformanceMetric.OUTPUT_PIPELINE, outputStarted);
				}
			}

			long previewStarted = profiling ? monitor.start() : 0L;
			try {
				runtime.renderPreviewPipeline(preview, output);
			} finally {
				if (profiling) monitor.record(PerformanceMetric.PREVIEW_PIPELINE, previewStarted);
			}

			long compositeStarted = profiling ? monitor.start() : 0L;
			runtime.displayPreviewCurrentView();
			if (profiling) monitor.record(PerformanceMetric.PREVIEW_COMPOSITE, compositeStarted);

			if (runtime.isShowPreview()) {
				long floatingStarted = profiling ? monitor.start() : 0L;
				runtime.drawFloatingPreview();
				if (profiling) monitor.record(PerformanceMetric.FLOATING_PREVIEW, floatingStarted);
			}
			long controlsStarted = profiling ? monitor.start() : 0L;
			runtime.drawControlPanel();
			if (profiling) monitor.record(PerformanceMetric.CONTROL_PANEL, controlsStarted);
		} finally {
			if (profiling) {
				if (gpuTiming) runtime.endGpuPerformanceInterval();
				monitor.record(PerformanceMetric.RENDER_PIPELINE, pipelineStarted);
				PerformanceMonitor.restore(previous);
			}
		}
	}

	private static int matchingPreviewCopies(
			RenderRequirementsPolicy.Requirements preview,
			RenderRequirementsPolicy.Requirements output) {
		int copies = 0;
		if (preview.needsFisheye() && output.needsFisheye()) copies++;
		if (preview.needsEquirectangular() && output.needsEquirectangular()) copies++;
		if (preview.needsCubemapLayout() && output.needsCubemapLayout()) copies++;
		return copies;
	}
}
