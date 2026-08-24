package com.victorvalentim.zividomelive.performance;

/**
 * Named performance samples exposed by the experimental profiler.
 *
 * <p>{@link PerformanceSnapshot#getStatistics(PerformanceMetric)} always returns
 * CPU-observed wall time. GPU elapsed statistics are a separate channel and currently
 * exist only for {@link #RENDER_PIPELINE}.</p>
 *
 * <p><strong>API stability:</strong> Experimental.</p>
 *
 * @since 2.0.0
 */
public enum PerformanceMetric {
	/** Interval between consecutive Processing pre-frame boundaries. */
	FRAME_TOTAL,
	/** Active scene update callback. */
	SCENE_UPDATE,
	/** Aggregate scene rendering across Standard targets and cubemap faces. */
	SCENE_RENDER,
	/** Complete ziviDomeLive render pipeline on the Processing thread. */
	RENDER_PIPELINE,
	/** Deferred output-target recreation. */
	GRAPHICS_RESET,
	/** Aggregate Standard rendering across preview and output targets. */
	STANDARD_RENDER,
	/** Standard preview-target rendering. */
	STANDARD_PREVIEW,
	/** Standard output-target rendering. */
	STANDARD_OUTPUT,
	/** Complete master cubemap capture. */
	CUBEMAP_TOTAL,
	/** Preview-resolution cubemap capture. */
	CUBEMAP_PREVIEW,
	/** Output-resolution cubemap capture. */
	CUBEMAP_OUTPUT,
	/** Positive-X cubemap face. */
	CUBEMAP_POS_X,
	/** Negative-X cubemap face. */
	CUBEMAP_NEG_X,
	/** Positive-Y cubemap face. */
	CUBEMAP_POS_Y,
	/** Negative-Y cubemap face. */
	CUBEMAP_NEG_Y,
	/** Positive-Z cubemap face. */
	CUBEMAP_POS_Z,
	/** Negative-Z cubemap face. */
	CUBEMAP_NEG_Z,
	/** CPU-observed scratch-to-cubemap framebuffer blit submission. */
	CUBEMAP_BLIT,
	/** CPU-observed cubemap mipmap generation submission. */
	CUBEMAP_MIPMAP,
	/** Aggregate domemaster projection work. */
	DOMEMASTER,
	/** Preview domemaster projection work. */
	DOMEMASTER_PREVIEW,
	/** Output domemaster projection work. */
	DOMEMASTER_OUTPUT,
	/** Aggregate equirectangular projection work. */
	EQUIRECTANGULAR,
	/** Preview equirectangular projection work. */
	EQUIRECTANGULAR_PREVIEW,
	/** Output equirectangular projection work. */
	EQUIRECTANGULAR_OUTPUT,
	/** Aggregate skybox-layout projection work. */
	SKYBOX,
	/** Preview skybox-layout projection work. */
	SKYBOX_PREVIEW,
	/** Output skybox-layout projection work. */
	SKYBOX_OUTPUT,
	/** Rendering and submission of all enabled external outputs. */
	OUTPUT_PIPELINE,
	/** Production of preview-resolution representations. */
	PREVIEW_PIPELINE,
	/** GPU copy from an existing output projection into a preview target. */
	PREVIEW_COPY,
	/** Composition of the selected preview target into the Processing window. */
	PREVIEW_COMPOSITE,
	/** Composition of the floating domemaster thumbnail. */
	FLOATING_PREVIEW,
	/** Built-in ControlP5 panel update and drawing. */
	CONTROL_PANEL,
	/** NDI readback, CPU copy, and enqueue work on the Processing thread. */
	NDI_CAPTURE,
	/** ARGB-to-RGBA conversion on the NDI worker. */
	NDI_CONVERSION,
	/** Non-blocking NDI free/ready queue operations. */
	NDI_QUEUE,
	/** Native Devolay sender-call duration on the NDI worker. */
	NDI_SEND,
	/** Syphon publication-call duration. */
	SYPHON,
	/** Spout publication-call duration. */
	SPOUT
}
