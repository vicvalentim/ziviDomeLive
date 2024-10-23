package com.victorvalentim.zividomelive;

import processing.core.*;

public class CubemapRenderer {
    private PGraphics[] cubemapFaces;
    private int resolution;
    private PApplet parent;

    CubemapRenderer(int initialResolution, PApplet parent) {
        this.parent = parent;
        this.resolution = initialResolution;
        initializeCubemapFaces();
    }

    // Inicializa ou reinicializa as faces do cubemap com a resolução atual
    private void initializeCubemapFaces() {
        cubemapFaces = new PGraphics[6];
        for (int i = 0; i < 6; i++) {
            cubemapFaces[i] = parent.createGraphics(resolution / 2, resolution / 2, PApplet.P3D); // Resolução ajustada
        }
    }

    // Atualiza a resolução do cubemap e reinicializa as faces
    void updateResolution(int newResolution) {
        this.resolution = newResolution;
        initializeCubemapFaces();
    }

    // Configura a câmera para uma face específica do cubemap
    private void configureCameraForFace(PGraphics pg, CameraOrientation orientation, float pitch, float yaw, float roll) {
        PVector eye = new PVector(0, 0, 0);
        pg.camera(eye.x, eye.y, eye.z, orientation.centerX, orientation.centerY, orientation.centerZ, orientation.upX, orientation.upY, orientation.upZ);
        pg.perspective(PApplet.PI / 2, 1, 0.1f, 20000);

        // Aplica pitch, yaw e roll
        pg.translate(pg.width / 2, pg.height / 2, 0);
        pg.rotateX(pitch);
        pg.rotateY(roll);
        pg.rotateZ(yaw);
        pg.translate(-pg.width / 2, -pg.height / 2, 0);
    }

    // Captura o cubemap aplicando as transformações de câmera e renderizando a cena
    void captureCubemap(float pitch, float yaw, float roll, CameraManager cameraManager, Scene currentScene) {
        for (int i = 0; i < 6; i++) {
            cubemapFaces[i].beginDraw();
            configureCameraForFace(cubemapFaces[i], cameraManager.getOrientation(i), pitch, yaw, roll);

            // Chama a renderização da cena diretamente a partir da interface Scene
            if (currentScene != null) {
                currentScene.sceneRender(cubemapFaces[i]);
            }
            cubemapFaces[i].endDraw();
        }
    }

    // Retorna as faces do cubemap para outros renderizadores
    PGraphics[] getCubemapFaces() {
        return cubemapFaces;
    }
}
