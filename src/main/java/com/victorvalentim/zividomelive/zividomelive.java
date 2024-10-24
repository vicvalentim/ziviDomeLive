package com.victorvalentim.zividomelive;

import processing.core.*;
import processing.event.*;
import processing.opengl.*;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;

import java.net.URL;

public class zividomelive {

    private PApplet p;  // Referência para a instância PApplet
    private boolean initialized = false; // Flag para verificar a inicialização

    private Scene currentScene;  // Interface para a cena personalizada

    // Variáveis globais principais
    private float pitch = 0.0f, yaw = 0.0f, roll = 0.0f, fov = 210.0f, fishSize = 100.0f;
    private int resolution = 1024;
    private boolean showControlPanel = true;
    private boolean showPreview = false;
    private boolean enableOutput = false;

    // Gerenciadores e renderizadores
    private ControlManager controlManager;
    private CubemapRenderer cubemapRenderer;
    private EquirectangularRenderer equirectangularRenderer;
    private StandardRenderer standardRenderer;
    private FisheyeDomemaster fisheyeDomemaster;
    private CameraManager cameraManager;
    private CubemapViewRenderer cubemapViewRenderer;

    private SyphonServer syphonServer;
    private Spout spout;

    // Enum para tipos de visualização
    public enum ViewType {
        FISHEYE_DOMEMASTER, EQUIRECTANGULAR, CUBEMAP, STANDARD
    }

    private ViewType currentView = ViewType.FISHEYE_DOMEMASTER;
    private boolean pendingReset = false;
    private int pendingResolution = resolution;

    // Construtor
    public zividomelive(PApplet p) {
        if (p == null) {
            throw new IllegalArgumentException("A instância PApplet não pode ser nula.");
        }
        this.p = p;
        welcome();
    }

    private void welcome() {
        System.out.println("Biblioteca ziviDomeLive inicializada.");
    }

    // Método para definir a cena atual
    public void setScene(Scene scene) {
        this.currentScene = scene;  // Define a cena atual
        currentScene.setupScene();  // Chama o setup da cena ao definir
    }


    public void setup() {
        if (p == null) {
            throw new IllegalStateException("A instância PApplet não está configurada corretamente.");
        }

        System.out.println("Iniciando setup...");

        try {
            p.frameRate(64);
            System.out.println("Taxa de quadros definida para 64.");
        } catch (Exception e) {
            System.out.println("Erro ao definir a taxa de quadros: " + e.getMessage());
        }

        try {
            printlnOpenGLInfo();
        } catch (Exception e) {
            System.out.println("Erro ao imprimir informações do OpenGL: " + e.getMessage());
        }

        try {
            setupHints();
            System.out.println("Dicas de textura configuradas.");
        } catch (Exception e) {
            System.out.println("Erro ao configurar dicas de textura: " + e.getMessage());
        }

        p.registerMethod("post", this);

        try {
            setupSyphonOrSpout();
            System.out.println("Configuração do Syphon/Spout concluída.");
        } catch (Exception e) {
            System.out.println("Erro ao configurar Syphon/Spout: " + e.getMessage());
        }

        try {
            registerMouseEvents();
            System.out.println("Eventos de mouse registrados.");
        } catch (Exception e) {
            System.out.println("Erro ao registrar eventos de mouse: " + e.getMessage());
        }

        System.out.println("Setup concluído.");
    }

    void printlnOpenGLInfo() {
        PApplet.println(PGraphicsOpenGL.OPENGL_VERSION);
        PApplet.println(PGraphicsOpenGL.OPENGL_VENDOR);
        PApplet.println(PGraphicsOpenGL.OPENGL_RENDERER);
    }

    void setupHints() {
        p.textureMode(PConstants.NORMAL);
        p.textureWrap(PConstants.REPEAT);
        p.hint(PConstants.DISABLE_OPENGL_ERRORS);
        p.hint(PConstants.ENABLE_TEXTURE_MIPMAPS);
    }

    public void post() {
        if (!initialized) {
            initializeManagers();
            initialized = true;
            p.unregisterMethod("post", this);
        }
    }

    void initializeManagers() {
        try {
            System.out.println("Inicializando gerenciadores...");

            cameraManager = new CameraManager();
            System.out.println("CameraManager inicializado.");

            initializeRenderers();
            controlManager = new ControlManager(p, this, resolution);
            System.out.println("ControlManager inicializado.");
            System.out.println("Gerenciadores inicializados com sucesso.");
        } catch (Exception e) {
            System.out.println("Erro ao inicializar gerenciadores: " + e.getMessage());
            e.printStackTrace();
        }
    }

    void initializeRenderers() {
        try {
            System.out.println("Inicializando renderizadores...");

            cubemapRenderer = new CubemapRenderer(resolution, p);
            System.out.println("CubemapRenderer inicializado: " + (cubemapRenderer != null));

            equirectangularRenderer = new EquirectangularRenderer(resolution, "equirectangular.glsl", p);
            System.out.println("EquirectangularRenderer inicializado: " + (equirectangularRenderer != null));

            standardRenderer = new StandardRenderer(p, p.width, p.height, currentScene);
            System.out.println("StandardRenderer inicializado: " + (standardRenderer != null));

            fisheyeDomemaster = new FisheyeDomemaster(resolution, "domemaster.glsl", p);
            System.out.println("FisheyeDomemaster inicializado: " + (fisheyeDomemaster != null));

            cubemapViewRenderer = new CubemapViewRenderer(p, resolution);
            System.out.println("CubemapViewRenderer inicializado: " + (cubemapViewRenderer != null));

            System.out.println("Renderizadores inicializados com sucesso.");
        } catch (Exception e) {
            System.out.println("Erro ao inicializar renderizadores: " + e.getMessage());
            e.printStackTrace();
        }
    }

    void setupSyphonOrSpout() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("mac")) {
                syphonServer = new SyphonServer(p, "ziviDomeLive Syphon");
                System.out.println("SyphonServer inicializado para macOS.");
            } else if (os.contains("win")) {
                spout = new Spout(p);
                System.out.println("Spout inicializado para Windows.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao configurar Syphon/Spout: " + e.getMessage());
        }
    }

    void registerMouseEvents() {
        try {
            p.registerMethod("mouseEvent", this);
            System.out.println("Eventos de mouse registrados.");
        } catch (Exception e) {
            System.out.println("Erro ao registrar eventos de mouse: " + e.getMessage());
        }
    }

    // Função principal de desenho
    public void draw() {
        // Verificação dos renderizadores e cena antes de iniciar a renderização
        if (cubemapRenderer == null) {
            System.out.println("Erro: CubemapRenderer não inicializado.");
            return;
        }

        if (equirectangularRenderer == null) {
            System.out.println("Erro: EquirectangularRenderer não inicializado.");
            return;
        }

        if (fisheyeDomemaster == null) {
            System.out.println("Erro: FisheyeDomemaster não inicializado.");
            return;
        }

        if (standardRenderer == null) {
            System.out.println("Erro: StandardRenderer não inicializado.");
            return;
        }

        if (currentScene == null) {
            System.out.println("Erro: currentScene não inicializado.");
            return;
        }

        // Depuração da etapa de reset gráfico
        //System.out.println("Iniciando renderização...");

        clearBackground();  // Limpar e configurar o fundo
        handleGraphicsReset();  // Verificar se há necessidade de redefinir gráficos

        // Renderiza o cubemap
        captureCubemap();  // Função para capturar cubemap

        // Depuração de qual visualização está sendo renderizada
        //System.out.println("Modo de visualização atual: " + getCurrentView());

        renderView();
       
        // Desenhar o preview flutuante se estiver ativo
        if (showPreview) {
          drawFloatingPreview();
        }

        // Enviar saída para Syphon/Spout se habilitado
        sendOutput();  // Função para enviar imagem para Syphon/Spout

        // Desenha painel de controle e visualizações, se necessário
        drawControlPanel();  // Função para desenhar controles
    }

    // Função para limpar o fundo da tela
    private void clearBackground() {
        //System.out.println("Limpando o fundo...");
        p.background(0, 0, 0, 0);  // Define o fundo como preto com transparência
    }

    // Função para verificar e aplicar resets gráficos
    private void handleGraphicsReset() {
        if (pendingReset) {
            //System.out.println("Redefinindo gráficos com nova resolução: " + pendingResolution);
            resolution = pendingResolution;
            initializeRenderers();  // Reinicializa os renderizadores com a nova resolução
            pendingReset = false;
        }
    }

    // Função para capturar o cubemap
    private void captureCubemap() {
        //System.out.println("Capturando cubemap...");
        if (cubemapRenderer != null) {
            cubemapRenderer.captureCubemap(getPitch(), getYaw(), getRoll(), cameraManager, currentScene);
        } else {
            System.out.println("Erro: CubemapRenderer não inicializado.");
        }
    }
    
    void displayView(PGraphics pg) {
        // Desenha a visualização na tela principal, ajustando o tamanho e centralizando
        float aspectRatio = pg.width / (float) pg.height;
        float displayWidth = p.width;
        float displayHeight = p.width / aspectRatio;

        if (displayHeight > p.height) {
            displayHeight = p.height;
            displayWidth = p.height * aspectRatio;
        }

        p.image(pg, (p.width - displayWidth) / 2, (p.height - displayHeight) / 2, displayWidth, displayHeight);
    }
    
    private void updateRenderViews() {
        // Renderiza sempre o equirectangular primeiro para garantir que esteja pronto para o fisheye
        equirectangularRenderer.render(cubemapRenderer.getCubemapFaces());
        
        // Aplica o shader Fisheye usando o equirectangular já renderizado
        fisheyeDomemaster.applyShader(equirectangularRenderer.getEquirectangular(), getFov());

        // Renderiza as outras vistas condicionalmente
        switch (getCurrentView()) {
            case CUBEMAP:
                cubemapViewRenderer.drawCubemapToGraphics(cubemapRenderer.getCubemapFaces());
                break;
            case STANDARD:
                standardRenderer.render();
                break;
        }
    }

    private void displayCurrentView() {
        // Exibir a visualização com base no modo atual
        switch (getCurrentView()) {
            case CUBEMAP:
                displayView(cubemapViewRenderer.getCubemap());
                break;
            case EQUIRECTANGULAR:
                displayView(equirectangularRenderer.getEquirectangular());
                break;
            case FISHEYE_DOMEMASTER:
                displayView(fisheyeDomemaster.getDomemasterGraphics());
                break;
            case STANDARD:
                displayView(standardRenderer.getStandardView());
                break;
        }
    }

    private void renderView() {
        updateRenderViews();  // Atualiza todas as renderizações necessárias
        displayCurrentView(); // Exibe a vista conforme o modo selecionado
    }

    // Função para enviar a saída via Syphon ou Spout
    private void sendOutput() {
        if (isEnableOutput()) {
            if (syphonServer != null) {
                syphonServer.sendImage(fisheyeDomemaster.getDomemasterGraphics());
                //System.out.println("Imagem enviada para Syphon.");
            } else if (spout != null) {
                spout.sendTexture(fisheyeDomemaster.getDomemasterGraphics());
                //System.out.println("Textura enviada para Spout.");
            }
        }
    }

    // Função para desenhar o painel de controle
    private void drawControlPanel() {
        //System.out.println("Desenhando painel de controle...");
        p.hint(PConstants.DISABLE_DEPTH_TEST);
     // Atualizar informações de controle e FPS
        controlManager.updateFpsLabel(p.frameRate);
        
        if (showControlPanel) {
            controlManager.show();  // Mostra os controles
        } else {
            controlManager.hide();  // Esconde os controles
        }
        p.hint(PConstants.ENABLE_DEPTH_TEST);
    }
    
    public void drawFloatingPreview() {
        // Definir o tamanho da pré-visualização flutuante
        float previewWidth = 200f;
        float previewHeight = 200f;
        float x = p.width - previewWidth; // Usar 'p.width' se 'width' não for reconhecido diretamente
        float y = p.height - previewHeight; // Usar 'p.height' se 'height' não for reconhecido diretamente

        // Obter o gráfico a ser mostrado, neste caso, o domemaster do fisheye
        PGraphics previewGraphics = fisheyeDomemaster.getDomemasterGraphics();

        // Desenhar a pré-visualização flutuante
        p.image(previewGraphics, x, y, previewWidth, previewHeight);
    }

    public void mouseEvent(MouseEvent event) {
        if (event.getAction() == MouseEvent.WHEEL) {
            standardRenderer.getCam().mouseWheel(event);
        }
    }

    public void keyPressed() {
        if (!controlManager.isNumberboxActive()) {
            if (p.key == 'h') {
                showControlPanel = !showControlPanel;
                System.out.println("Alternando visibilidade do painel de controle: " + showControlPanel);
            }
            if (p.key == 'm') {
                setCurrentView(ViewType.values()[(getCurrentView().ordinal() + 1) % ViewType.values().length]);
                System.out.println("Alternando visualização para: " + getCurrentView());
            }
        }
    }

    public void controlEvent(ControlEvent theEvent) {
        if (controlManager != null) {
            controlManager.handleEvent(theEvent);
        }
    }

    // Getters e setters para variáveis globais

    public float getFishSize() {
        return fishSize;
    }

    public void setFishSize(float fishSize) {
        this.fishSize = fishSize;
    }

    public float getFov() {
        return fov;
    }

    public void setFov(float fov) {
        this.fov = fov;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getRoll() {
        return roll;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public ViewType getCurrentView() {
        return currentView;
    }

    public void setCurrentView(ViewType currentView) {
        this.currentView = currentView;
    }

    public boolean isEnableOutput() {
        return enableOutput;
    }

    public void setEnableOutput(boolean enableOutput) {
        this.enableOutput = enableOutput;
    }

    public boolean isShowPreview() {
        return showPreview;
    }

    public void setShowPreview(boolean showPreview) {
        this.showPreview = showPreview;
    }

    public void resetControls() {
        controlManager.resetControls();
    }

    public void resetGraphics(int newResolution) {
        pendingReset = true;
        pendingResolution = newResolution;
    }

    public FisheyeDomemaster getFisheyeDomemaster() {
        return fisheyeDomemaster;
    }

    public void setFisheyeDomemaster(FisheyeDomemaster fisheyeDomemaster) {
        this.fisheyeDomemaster = fisheyeDomemaster;
    }

    public PApplet getPApplet() {
        return p;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
