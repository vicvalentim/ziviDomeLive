package com.victorvalentim.zividomelive;

import processing.core.*;

public class StandardRenderer {
    private PGraphics standardView;
    private Scene currentScene; // Substituímos DrawScene por Scene
    private MouseControlledCamera cam;
    private PApplet parent;

    // Construtor da classe StandardRenderer
    StandardRenderer(PApplet parent, int width, int height, Scene currentScene) {
        this.parent = parent;
        this.currentScene = currentScene; // Agora recebemos a cena diretamente
        standardView = parent.createGraphics(width, height, PApplet.P3D); // Criação do PGraphics para a visualização padrão
        setCam(new MouseControlledCamera()); // Inicializa a câmera
    }

    // Método de renderização
    void render() {
        getCam().update(parent); // Atualiza a câmera antes de desenhar

        standardView.beginDraw();  // Inicia o desenho no PGraphics
        standardView.background(0, 0);  // Define o fundo da visualização

        // Aplica configurações da MouseControlledCamera ao PGraphics
        getCam().apply(standardView);

        // Chama a função de renderização da cena usando a interface Scene
        currentScene.sceneRender(standardView);

        standardView.endDraw();  // Finaliza o desenho no PGraphics
    }

    // Retorna o PGraphics da visualização padrão
    PGraphics getStandardView() {
        return standardView;
    }

    // Retorna a instância da câmera controlada pelo mouse
    public MouseControlledCamera getCam() {
        return cam;
    }

    // Define uma nova instância da câmera controlada pelo mouse
    public void setCam(MouseControlledCamera cam) {
        this.cam = cam;
    }
}
