package com.victorvalentim.zividomelive;

import processing.core.*;
import processing.opengl.*;

/**
 * The EquirectangularRenderer class handles the rendering of equirectangular projections from cubemap faces.
 */
public class EquirectangularRenderer {
    private PGraphics equirectangular;
    private final PShader equirectangularShader;
    private final PApplet parent;
    private final int resolution;

    /**
     * Constructs an EquirectangularRenderer with the specified resolution, shader, and parent PApplet.
     *
     * @param resolution the resolution of the equirectangular projection
     * @param shader the PShader object
     * @param parent the parent PApplet instance
     */
    public EquirectangularRenderer(int resolution, PShader shader, PApplet parent) {
        this.resolution = resolution;
        this.equirectangularShader = shader;
        this.parent = parent;
    }

    /**
     * Initializes or reinitializes the PGraphics object for the equirectangular projection.
     */
    private void initializeEquirectangular() {
        if (equirectangular != null) {
            equirectangular.dispose();
        }
        equirectangular = parent.createGraphics(resolution * 2, resolution, PApplet.P2D);
    }

    /**
     * Renders the equirectangular projection from the given cubemap faces.
     *
     * @param faces an array of PGraphics objects representing the cubemap faces
     */
    public void render(PGraphics[] faces) {
        if (equirectangular == null) {
            initializeEquirectangular();
        }

        equirectangular.beginDraw();
        equirectangular.background(0, 0);
        equirectangularShader.set("posX", faces[0]);
        equirectangularShader.set("negX", faces[1]);
        equirectangularShader.set("posY", faces[2]);
        equirectangularShader.set("negY", faces[3]);
        equirectangularShader.set("posZ", faces[4]);
        equirectangularShader.set("negZ", faces[5]);
        equirectangularShader.set("resolution", new float[]{equirectangular.width, equirectangular.height});
        equirectangular.shader(equirectangularShader);
        equirectangular.rect(0, 0, equirectangular.width, equirectangular.height);
        equirectangular.endDraw();
    }

    /**
     * Returns the PGraphics object for the equirectangular projection.
     *
     * @return the PGraphics object representing the equirectangular projection
     */
    public PGraphics getEquirectangular() {
        if (equirectangular == null) {
            initializeEquirectangular();
        }
        return equirectangular;
    }

    /**
     * Releases the graphical resources used by the equirectangular projection.
     */
    public void dispose() {
        if (equirectangular != null) {
            equirectangular.dispose();
            equirectangular = null;
        }
    }
}