package com.victorvalentim.zividomelive;

import processing.core.*;
import processing.opengl.*;

public class FisheyeDomemaster {
    PGraphics domemaster, domemasterSize;
    PShader domemasterShader;
    int resolution;
    float sizePercentage; // Variável para armazenar o tamanho em porcentagem
    PApplet parent; // Reference to PApplet for shader use

    // Constructor with PApplet parent passed
    FisheyeDomemaster(int resolution, String shaderPath, PApplet parent) {
        this.resolution = resolution;
        this.sizePercentage = 100.0f; // Inicializar com 100%
        this.parent = parent; // Assign the parent PApplet

        domemaster = parent.createGraphics(resolution, resolution, PApplet.P2D);
        domemaster.smooth(4);
        domemasterSize = parent.createGraphics(resolution, resolution, PApplet.P2D);
        domemasterSize.smooth(4);
        domemasterShader = parent.loadShader(shaderPath); // Use parent to load shader
    }

    // Method to set the field of view (fov) from the main class
    void setFOV(float fov) {
        domemasterShader.set("fov", fov);
    }

    // Ajusta o percentual de tamanho
    public void setSizePercentage(float percentage) {
        sizePercentage = PApplet.constrain(percentage, 0, 100); // Garante que a porcentagem esteja entre 0 e 100
    }

    // Apply shader with the equirectangular map
    void applyShader(PGraphics equirectangular, float fov) {
        // Certifique-se de que o equirectangular não é nulo
        if (equirectangular == null) {
            System.out.println("Equirectangular PGraphics é nulo.");
            return;
        }

        // Atualiza o fov
        setFOV(fov);

        // Renderiza o domemaster com o shader aplicado
        domemaster.beginDraw();
        domemaster.background(0, 0); // Fundo transparente
        domemasterShader.set("equirectangularMap", equirectangular);
        domemasterShader.set("resolution", new float[]{domemaster.width, domemaster.height});
        domemaster.shader(domemasterShader);
        domemaster.rect(0, 0, domemaster.width, domemaster.height);
        domemaster.endDraw();

        // Ajusta o tamanho do domemaster para o percentual configurado
        float adjustedSize = resolution * (sizePercentage / 100.0f);
        domemasterSize.beginDraw();
        domemasterSize.background(0, 0); // Fundo transparente
        domemasterSize.image(domemaster, (domemasterSize.width - adjustedSize) / 2, (domemasterSize.height - adjustedSize) / 2, adjustedSize, adjustedSize);
        domemasterSize.endDraw();
    }

    // Getter for domemaster graphics
    public PGraphics getDomemasterGraphics() {
        return domemasterSize;
    }
}
