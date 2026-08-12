package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.manager.OutputManager;
import com.victorvalentim.zividomelive.support.LogManager;
import processing.opengl.PGraphicsOpenGL;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Coordinates one frame using the runtime's existing 1.5 renderer backend.
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
	 *   <li>Render and submit active external outputs.</li>
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

		runtime.clearBackground();
		runtime.handleGraphicsReset();
		runtime.ensurePreviewRenderers();
		runtime.syncCurrentSceneToRenderers();

		RenderRequirementsPolicy.Requirements preview = runtime.computePreviewRequirements();
		RenderRequirementsPolicy.Requirements output = runtime.computeOutputRequirements();
		PGraphicsOpenGL[] masterFaces = runtime.captureMasterCubemap(preview, output);

		OutputManager outputManager = runtime.getOutputManager();
		if (outputManager != null && outputManager.isActive()) {
			runtime.renderOutputPipeline(output, masterFaces);
			outputManager.sendOutput(finalFrameViews);
		}

		runtime.renderPreviewPipeline(preview, output, masterFaces);
		runtime.displayPreviewCurrentView();

		if (runtime.isShowPreview()) {
			runtime.drawFloatingPreview();
		}
		runtime.drawControlPanel();
	}
}
