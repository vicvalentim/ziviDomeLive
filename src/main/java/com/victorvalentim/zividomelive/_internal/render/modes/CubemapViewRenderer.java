package com.victorvalentim.zividomelive;

// Package-private implementation grouped physically under _internal/render/modes.

import processing.core.*;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;
import java.util.logging.Logger;

/**
 * The CubemapViewRenderer class handles the creation and rendering of cubemap views.
 */
class CubemapViewRenderer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int CUBEMAP_TEXTURE_UNIT = 1;
    /**
     * Original CubemapView cross order, expressed with the canonical CubemapFace indices:
     * top, left, center, right, far-right, bottom.
     */
    private static final int[] CUBEMAP_LAYOUT_FACE_ORDER = {3, 1, 4, 0, 5, 2};
    private static final int[] FACE_ROTATIONS = {2, 2, 2, 2, 2, 2};
    private static final int[] FACE_INVERSIONS = {1, 1, 1, 1, 1, 1};
    private int resolution;
    private PGraphicsOpenGL cubemap;
    private final PShader samplerCubeShader;
    private final PApplet parent;
    private final ProcessingGlAdapter glAdapter = ProcessingGlAdapter.getDefault();
    private final ProcessingGlAdapter.CubemapBindingState cubemapBindingState =
            new ProcessingGlAdapter.CubemapBindingState();
    private boolean unavailableWarningLogged;
    private boolean renderFailureWarningLogged;

    /**
     * Constructs a CubemapViewRenderer with the specified parent PApplet and resolution.
     *
     * @param parent the parent PApplet instance
	 * @param resolution the resolution of the cubemap
     */
	public CubemapViewRenderer(PApplet parent, int resolution) {
        this(parent, resolution, null, null);
    }

    /**
     * Constructs a CubemapViewRenderer with optional native samplerCube shader files.
     *
     * @param parent the parent PApplet instance
     * @param resolution the resolution of the cubemap
     * @param samplerCubeFragmentShaderPath the samplerCube fragment shader file (.frag)
     * @param samplerCubeVertexShaderPath the samplerCube vertex shader file (.vert)
     */
	public CubemapViewRenderer(
            PApplet parent,
            int resolution,
            String samplerCubeFragmentShaderPath,
            String samplerCubeVertexShaderPath) {
        this.parent = parent;
        this.resolution = resolution;
        this.samplerCubeShader = samplerCubeFragmentShaderPath != null && samplerCubeVertexShaderPath != null
                ? parent.loadShader(samplerCubeFragmentShaderPath, samplerCubeVertexShaderPath)
                : null;
    }

    /**
     * Initializes or reinitializes the PGraphics object for the cubemap.
     */
    private void initializeCubemap() {
        if (cubemap != null) {
            glAdapter.dispose(cubemap);
        }
        cubemap = glAdapter.createGraphics(parent, resolution * 2, resolution * 3 / 2, PApplet.P2D);
    }

    /**
     * Updates the resolution of the cubemap.
     *
     * @param newResolution the new resolution to be set
     */
    void updateResolution(int newResolution) {
        if (this.resolution != newResolution) {
            this.resolution = newResolution;
            initializeCubemap();
        }
    }

    /**
     * Returns the PGraphics object for the cubemap.
     *
     * @return the PGraphics object representing the cubemap
     */
    public PGraphicsOpenGL getCubemap() {
        if (cubemap == null) {
            initializeCubemap();
        }
        return (PGraphicsOpenGL) cubemap;
    }

    /**
     * Draws the cubemap layout from a native samplerCube cubemap.
     *
     * @param nativeCubemap native cubemap populated by {@code CubemapRenderer}
     */
    public void drawCubemapToGraphics(CubemapTarget nativeCubemap) {
        if (nativeCubemap == null || !nativeCubemap.isAllocated() || samplerCubeShader == null) {
            if (!unavailableWarningLogged) {
                LOGGER.warning("Native cubemap or cubemap layout samplerCube shader unavailable; skipping render.");
                unavailableWarningLogged = true;
            }
            return;
        }
        try {
            drawSamplerCubeToGraphics(nativeCubemap);
            unavailableWarningLogged = false;
            renderFailureWarningLogged = false;
        } catch (RuntimeException error) {
            if (!renderFailureWarningLogged) {
                LOGGER.warning("Native cubemap layout samplerCube render failed: "
                        + error.getMessage());
                renderFailureWarningLogged = true;
            }
        }
    }

    private void drawSamplerCubeToGraphics(CubemapTarget nativeCubemap) {
        if (cubemap == null) {
            initializeCubemap();
        }

        cubemap.beginDraw();
        boolean cubemapBound = false;
        try {
            cubemap.background(0, 0);
            samplerCubeShader.set("resolution", cubemap.width, cubemap.height);
            samplerCubeShader.set("cubemap", CUBEMAP_TEXTURE_UNIT);
            samplerCubeShader.set("layoutFaces", CUBEMAP_LAYOUT_FACE_ORDER);
            samplerCubeShader.set("faceRotations", FACE_ROTATIONS);
            samplerCubeShader.set("faceInversions", FACE_INVERSIONS);
            cubemap.shader(samplerCubeShader);
            glAdapter.bindCubemapTextureScoped(
                    cubemap, nativeCubemap, CUBEMAP_TEXTURE_UNIT, cubemapBindingState);
            cubemapBound = true;
            cubemap.rect(0, 0, cubemap.width, cubemap.height);
        } finally {
            try {
                if (cubemapBound) {
                    glAdapter.restoreCubemapTexture(cubemap, cubemapBindingState);
                }
            } finally {
                cubemap.endDraw();
            }
        }
    }

    /**
     * Releases the graphical resources used by the cubemap.
     */
    public void dispose() {
        if (cubemap != null) {
            glAdapter.dispose(cubemap);
            cubemap = null;
        }
    }

    static int[] cubemapLayoutFaceOrder() {
        return CUBEMAP_LAYOUT_FACE_ORDER.clone();
    }

    static int[] faceRotations() {
        return FACE_ROTATIONS.clone();
    }

    static int[] faceInversions() {
        return FACE_INVERSIONS.clone();
    }
}
