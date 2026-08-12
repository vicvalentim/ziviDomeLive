package com.victorvalentim.zividomelive.render.modes;

import com.victorvalentim.zividomelive.render.gl.CubemapTarget;
import com.victorvalentim.zividomelive.render.gl.ProcessingGlAdapter;
import com.victorvalentim.zividomelive.support.LogManager;
import processing.core.*;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;
import java.util.logging.Logger;

/**
 * The CubemapViewRenderer class handles the creation and rendering of cubemap views.
 */
public class CubemapViewRenderer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int CUBEMAP_TEXTURE_UNIT = 1;
    private int resolution;
    private PGraphicsOpenGL cubemap;
    private final PShader samplerCubeShader;
    private final int[] faceRotations = {2, 2, 2, 2, 2, 2};
    private final boolean[] faceInversions = {true, true, true, true, true, true};
    private final int[] faceInversionsAsUniform = {1, 1, 1, 1, 1, 1};
    private final PApplet parent;
    private final ProcessingGlAdapter glAdapter = ProcessingGlAdapter.getDefault();

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
     * Draws the cubemap faces onto the PGraphics object.
     *
     * @param cubemapFaces an array of PGraphics objects representing the cubemap faces
     */
    public void drawCubemapToGraphics(PGraphicsOpenGL[] cubemapFaces) {
        if (cubemapFaces == null || cubemapFaces.length != 6) {
            LOGGER.warning("Invalid cubemapFaces: expected 6 faces.");
            return;
        }

        if (cubemap == null) {
            initializeCubemap();
        }

        cubemap.beginDraw();
        cubemap.background(0, 0);
        applyTransformations(cubemap, cubemapFaces[3], (float) resolution / 2, 0, (float) resolution / 2, (float) resolution / 2, faceRotations[3], faceInversions[3]);
        applyTransformations(cubemap, cubemapFaces[1], 0, (float) resolution / 2, (float) resolution / 2, (float) resolution / 2, faceRotations[0], faceInversions[0]);
        applyTransformations(cubemap, cubemapFaces[4], (float) resolution / 2, (float) resolution / 2, (float) resolution / 2, (float) resolution / 2, faceRotations[4], faceInversions[4]);
        applyTransformations(cubemap, cubemapFaces[0], resolution, (float) resolution / 2, (float) resolution / 2, (float) resolution / 2, faceRotations[1], faceInversions[1]);
        applyTransformations(cubemap, cubemapFaces[5], (float) (resolution * 3) / 2, (float) resolution / 2, (float) resolution / 2, (float) resolution / 2, faceRotations[5], faceInversions[5]);
        applyTransformations(cubemap, cubemapFaces[2], (float) resolution / 2, resolution, (float) resolution / 2, (float) resolution / 2, faceRotations[2], faceInversions[2]);
        cubemap.endDraw();
    }

    /**
     * Draws the cubemap layout from a native cubemap when available.
     *
     * <p>The Processing face-array renderer remains the fallback so the view still works
     * on OpenGL contexts where the native cubemap path is unavailable.</p>
     *
     * @param nativeCubemap native cubemap populated by {@code CubemapRenderer}
     * @param fallbackFaces legacy Processing face targets used when native sampling is unavailable
     */
    public void drawCubemapToGraphics(CubemapTarget nativeCubemap, PGraphicsOpenGL[] fallbackFaces) {
        if (nativeCubemap == null || !nativeCubemap.isAllocated() || samplerCubeShader == null) {
            drawCubemapToGraphics(fallbackFaces);
            return;
        }
        try {
            drawSamplerCubeToGraphics(nativeCubemap);
        } catch (RuntimeException error) {
            LOGGER.warning("Native cubemap layout samplerCube render failed; falling back to Processing faces: "
                    + error.getMessage());
            drawCubemapToGraphics(fallbackFaces);
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
            samplerCubeShader.set("faceRotations", faceRotations);
            samplerCubeShader.set("faceInversions", faceInversionsAsUniform);
            cubemap.shader(samplerCubeShader);
            glAdapter.bindCubemapTexture(cubemap, nativeCubemap, CUBEMAP_TEXTURE_UNIT);
            cubemapBound = true;
            cubemap.rect(0, 0, cubemap.width, cubemap.height);
        } finally {
            try {
                if (cubemapBound) {
                    glAdapter.unbindCubemapTexture(cubemap, CUBEMAP_TEXTURE_UNIT);
                }
            } finally {
                cubemap.endDraw();
            }
        }
    }

    /**
     * Applies transformations of rotation and inversion to the cubemap faces.
     *
     * @param target the target PGraphics object
     * @param face the PGraphics object representing the cubemap face
     * @param x the x-coordinate for the transformation
     * @param y the y-coordinate for the transformation
     * @param w the width of the face
     * @param h the height of the face
     * @param rotation the number of 90-degree rotations to apply
     * @param invert whether to apply horizontal inversion
     */
    void applyTransformations(PGraphicsOpenGL target, PGraphicsOpenGL face, float x, float y, float w, float h, int rotation, boolean invert) {
        target.pushMatrix();
        target.translate(x + w / 2, y + h / 2);

        for (int i = 0; i < rotation; i++) {
            target.rotate(PApplet.HALF_PI);
        }

        if (invert) {
            target.scale(-1, 1);
        }

        target.imageMode(PApplet.CENTER);
        target.image(face, 0, 0, w, h);
        target.popMatrix();
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
}
