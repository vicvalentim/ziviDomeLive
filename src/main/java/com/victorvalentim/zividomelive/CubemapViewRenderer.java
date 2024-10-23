package com.victorvalentim.zividomelive;

import processing.core.*;

public class CubemapViewRenderer {
    private int resolution;
    private PGraphics cubemap;
    private int[] faceRotations = {2, 2, 2, 2, 2, 2}; // Rotação em 90 graus: 0, 1, 2, 3 para 0, 90, 180, 270 graus
    private boolean[] faceInversions = {true, true, true, true, true, true}; // Inversão horizontal
    private PApplet parent;

    // Construtor
    CubemapViewRenderer(PApplet parent, int resolution) {
        this.parent = parent;
        this.resolution = resolution;
        initializeCubemap();
    }

    // Inicializa ou reinicializa o PGraphics do cubemap
    private void initializeCubemap() {
        cubemap = parent.createGraphics(resolution * 2, resolution * 3 / 2, PApplet.P2D); // Ajusta para um layout em "cruz" ou cubemap
    }

    // Atualiza a resolução do cubemap
    void updateResolution(int newResolution) {
        this.resolution = newResolution;
        initializeCubemap();
    }

    // Retorna o PGraphics do cubemap
    PGraphics getCubemap() {
        return cubemap;
    }

    // Método para desenhar as faces do cubemap no gráfico
    void drawCubemapToGraphics(PGraphics[] cubemapFaces) {
        if (cubemapFaces == null || cubemapFaces.length != 6) {
            System.out.println("Erro: cubemapFaces inválido.");
            return;
        }

        cubemap.beginDraw();
        cubemap.background(0,0); // Fundo do cubemap

        // Desenhar as faces do cubemap em cruz horizontal (4:3)
        applyTransformations(cubemap, cubemapFaces[3], resolution / 2, 0, resolution / 2, resolution / 2, faceRotations[3], faceInversions[3]);   // Top
        applyTransformations(cubemap, cubemapFaces[1], 0, resolution / 2, resolution / 2, resolution / 2, faceRotations[0], faceInversions[0]);   // Right
        applyTransformations(cubemap, cubemapFaces[4], resolution / 2, resolution / 2, resolution / 2, resolution / 2, faceRotations[4], faceInversions[4]); // Front
        applyTransformations(cubemap, cubemapFaces[0], resolution, resolution / 2, resolution / 2, resolution / 2, faceRotations[1], faceInversions[1]); // Left
        applyTransformations(cubemap, cubemapFaces[5], resolution * 3 / 2, resolution / 2, resolution / 2, resolution / 2, faceRotations[5], faceInversions[5]); // Back
        applyTransformations(cubemap, cubemapFaces[2], resolution / 2, resolution, resolution / 2, resolution / 2, faceRotations[2], faceInversions[2]); // Bottom
        cubemap.endDraw();
    }

    // Aplica as transformações de rotação e inversão às faces do cubemap
    void applyTransformations(PGraphics target, PGraphics face, float x, float y, float w, float h, int rotation, boolean invert) {
    	
    	target.pushMatrix();
    	target.translate(x + w / 2, y + h / 2);

    	for (int i = 0; i < rotation; i++) {
    	    target.rotate(PApplet.HALF_PI);
    	}
    	
    	// Aplica a inversão se necessário
    	if (invert) {
    		target.scale(-1, 1);
    	}

    	target.imageMode(PApplet.CENTER);
    	target.image(face, 0, 0, w, h);
    	target.popMatrix();
    	  
    }
}
