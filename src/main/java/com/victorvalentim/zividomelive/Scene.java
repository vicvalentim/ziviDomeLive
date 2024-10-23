package com.victorvalentim.zividomelive;

import processing.core.PGraphics;

public interface Scene {
    void setupScene();  // Método para configurar a cena
    void sceneRender(PGraphics pg);  // Método para desenhar a cena
    void keyPressed(char key);  // Método para tratar eventos de teclado
}
