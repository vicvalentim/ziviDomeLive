import com.victorvalentim.zividomelive.*;
import com.victorvalentim.zividomelive.support.ThreadManager;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;

import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;

// Instâncias principais
ziviDomeLive ziviDome;      // Instância da biblioteca ziviDomeLive
SceneManager sceneManager;  // Gerenciador de cenas

ReentrantLock lock = new ReentrantLock();  // Lock para controle de acesso concorrente

void settings() {
  pixelDensity(1);  // Library default policy
  size(1280, 720, P3D);  // Define o tamanho da janela e o modo P3D
}

void setup() {
  // Inicializa a biblioteca ziviDomeLive
  ziviDome = new ziviDomeLive(this);
  ziviDome.setup();

  // Criação e configuração do SceneManager
  sceneManager = new SceneManager();
  sceneManager.registerScene(new Scene1(ziviDome)); // Registra apenas Scene1

  // Vincula o SceneManager à biblioteca ziviDomeLive
  ziviDome.setSceneManager(sceneManager);

  println("Simulação de partículas usando o ThreadManager compartilhado.");
}

void draw() {
  // ziviDomeLive renderiza pelo hook draw registrado no Processing.
}
