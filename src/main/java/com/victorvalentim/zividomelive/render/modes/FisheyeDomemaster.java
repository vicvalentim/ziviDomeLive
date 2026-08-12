package com.victorvalentim.zividomelive.render.modes;


import com.victorvalentim.zividomelive.render.gl.CubemapTarget;
import com.victorvalentim.zividomelive.render.gl.ProcessingGlAdapter;
import com.victorvalentim.zividomelive.support.LogManager;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;
import java.util.logging.Logger;

/**
 * The FisheyeDomemaster class handles the rendering of fisheye domemaster projections from equirectangular maps.
 */
public class FisheyeDomemaster {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int CUBEMAP_TEXTURE_UNIT = 1;
    private PGraphics domemaster;
    private PGraphics domemasterSize;
    private final PShader domemasterShader;
    private final PShader samplerCubeShader;
    private final int resolution;
    private float sizePercentage;
    private final PApplet parent;
    private final ProcessingGlAdapter glAdapter = ProcessingGlAdapter.getDefault();

    /**
     * Constructs a FisheyeDomemaster with the specified resolution, shader files, and parent PApplet.
     *
     * @param resolution the resolution of the domemaster projection
     * @param fragmentShaderPath the path to the fragment shader file (.frag)
     * @param vertexShaderPath the path to the vertex shader file (.vert)
     * @param parent the parent PApplet instance
     */
    public FisheyeDomemaster(int resolution,String fragmentShaderPath, String vertexShaderPath, PApplet parent) {
        this(resolution, fragmentShaderPath, vertexShaderPath, null, null, parent);
    }

    /**
     * Constructs a FisheyeDomemaster with legacy and native samplerCube shader files.
     *
     * @param resolution the resolution of the domemaster projection
     * @param fragmentShaderPath the legacy fragment shader file (.frag)
     * @param vertexShaderPath the legacy vertex shader file (.vert)
     * @param samplerCubeFragmentShaderPath the samplerCube fragment shader file (.frag)
     * @param samplerCubeVertexShaderPath the samplerCube vertex shader file (.vert)
     * @param parent the parent PApplet instance
     */
    public FisheyeDomemaster(
            int resolution,
            String fragmentShaderPath,
            String vertexShaderPath,
            String samplerCubeFragmentShaderPath,
            String samplerCubeVertexShaderPath,
            PApplet parent) {
        this.resolution = resolution;
        this.sizePercentage = 100.0f;
        this.parent = parent;
        this.domemasterShader = parent.loadShader(fragmentShaderPath, vertexShaderPath);
        this.samplerCubeShader = samplerCubeFragmentShaderPath != null && samplerCubeVertexShaderPath != null
                ? parent.loadShader(samplerCubeFragmentShaderPath, samplerCubeVertexShaderPath)
                : null;
    }

    /**
     * Initializes or reinitializes the PGraphics object for the domemaster projection.
     */
    private void initializeDomemaster() {
        if (domemaster != null) {
            glAdapter.dispose(domemaster);
        }
        domemaster = glAdapter.createGraphics(parent, resolution, resolution, PApplet.P2D);
    }

    /**
     * Initializes or reinitializes the PGraphics object for the domemaster size.
     */
    private void initializeDomemasterSize() {
        if (domemasterSize != null) {
            glAdapter.dispose(domemasterSize);
        }
        domemasterSize = glAdapter.createGraphics(parent, resolution, resolution, PApplet.P2D);
    }

    /**
     * Sets the field of view (FOV) for the domemaster shader.
     *
     * @param fov the field of view to set
     */
    void setFOV(float fov) {
        if (domemasterShader == null) {
            LOGGER.warning("Domemaster shader not initialized; skipping FOV update.");
            return;
        }
        domemasterShader.set("fov", fov);
    }

    /**
     * Sets the size percentage for the domemaster projection.
     *
     * @param percentage the size percentage to set, constrained between 0 and 100
     */
    public void setSizePercentage(float percentage) {
        sizePercentage = PApplet.constrain(percentage, 0, 100);
    }

    /**
     * Returns the current size percentage of the domemaster projection.
     *
     * @return the size percentage, between 0 and 100
     */
    public float getSizePercentage() {
        return sizePercentage;
    }

    /**
     * Applies the shader to the equirectangular map and renders the domemaster projection.
     *
     * @param equirectangular the PGraphics object representing the equirectangular map
     * @param fov the field of view to use for the shader
     */
    public void applyShader(PGraphicsOpenGL equirectangular, float fov) {
        if (equirectangular == null) {
            LOGGER.warning("Equirectangular PGraphics is null.");
            return;
        }
        if (domemasterShader == null) {
            LOGGER.warning("Domemaster shader not initialized; skipping shader pass.");
            return;
        }

        if (domemaster == null) {
            initializeDomemaster();
        }
        if (domemasterSize == null) {
            initializeDomemasterSize();
        }

        setFOV(fov);

        domemaster.beginDraw();
        domemaster.background(0, 0); // Set transparent background
        domemasterShader.set("equirectangularMap", equirectangular);
        domemasterShader.set("resolution", new float[]{domemaster.width, domemaster.height});
        domemaster.shader(domemasterShader);
        domemaster.rect(0, 0, domemaster.width, domemaster.height);
        domemaster.endDraw();

        applySizePass();
    }

    /**
     * Applies the domemaster shader directly to a native cubemap when available.
     *
     * <p>The legacy equirectangular input remains the fallback so the public 2.0 rendering
     * contract can stay stable while native samplerCube paths are enabled incrementally.</p>
     *
     * @param nativeCubemap native cubemap populated by {@code CubemapRenderer}
     * @param fallbackEquirectangular legacy equirectangular map used when native sampling is unavailable
     * @param fov the field of view to use for the shader
     */
    public void applyShader(
            CubemapTarget nativeCubemap,
            PGraphicsOpenGL fallbackEquirectangular,
            float fov) {
        if (nativeCubemap == null || !nativeCubemap.isAllocated() || samplerCubeShader == null) {
            applyShader(fallbackEquirectangular, fov);
            return;
        }
        try {
            applySamplerCubeShader(nativeCubemap, fov);
        } catch (RuntimeException error) {
            LOGGER.warning("Native fisheye samplerCube render failed; falling back to equirectangular map: "
                    + error.getMessage());
            applyShader(fallbackEquirectangular, fov);
        }
    }

    private void applySamplerCubeShader(CubemapTarget nativeCubemap, float fov) {
        if (domemaster == null) {
            initializeDomemaster();
        }
        if (domemasterSize == null) {
            initializeDomemasterSize();
        }

        PGraphicsOpenGL target = (PGraphicsOpenGL) domemaster;
        target.beginDraw();
        boolean cubemapBound = false;
        try {
            target.background(0, 0); // Set transparent background
            samplerCubeShader.set("fov", fov);
            samplerCubeShader.set("resolution", new float[]{target.width, target.height});
            samplerCubeShader.set("cubemap", CUBEMAP_TEXTURE_UNIT);
            target.shader(samplerCubeShader);
            glAdapter.bindCubemapTexture(target, nativeCubemap, CUBEMAP_TEXTURE_UNIT);
            cubemapBound = true;
            target.rect(0, 0, target.width, target.height);
        } finally {
            try {
                if (cubemapBound) {
                    glAdapter.unbindCubemapTexture(target, CUBEMAP_TEXTURE_UNIT);
                }
            } finally {
                target.endDraw();
            }
        }

        applySizePass();
    }

    private void applySizePass() {
        float adjustedSize = resolution * (sizePercentage / 100.0f);
        domemasterSize.beginDraw();
        domemasterSize.background(0, 0); // Set transparent background
        domemasterSize.image(domemaster, (domemasterSize.width - adjustedSize) / 2, (domemasterSize.height - adjustedSize) / 2, adjustedSize, adjustedSize);
        domemasterSize.endDraw();
    }

    /**
     * Returns the PGraphics object for the domemaster projection.
     *
     * @return the PGraphics object representing the domemaster projection
     */
    public PGraphicsOpenGL getDomemasterGraphics() {
        if (domemasterSize == null) {
            initializeDomemasterSize();
        }
        return (PGraphicsOpenGL) domemasterSize;
    }

    /**
     * Releases the graphical resources used by the domemaster projection.
     */
    public void dispose() {
        if (domemaster != null) {
            glAdapter.dispose(domemaster);
            domemaster = null;
        }
        if (domemasterSize != null) {
            glAdapter.dispose(domemasterSize);
            domemasterSize = null;
        }
    }
}
