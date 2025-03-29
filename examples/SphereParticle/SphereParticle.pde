import com.victorvalentim.zividomelive.*;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;

// Instâncias principais
zividomelive ziviDome;      // Instância da biblioteca zividomelive
SceneManager sceneManager;  // Gerenciador de cenas

// Gerenciamento de threads
ExecutorService particleProcessors;  // ExecutorService para processamento paralelo
ReentrantLock lock = new ReentrantLock();  // Lock para controle de acesso concorrente

void settings() {
  size(1280, 720, P3D);  // Define o tamanho da janela e o modo P3D
}

void setup() {
  // Inicializa a biblioteca zividomelive
  ziviDome = new zividomelive(this);
  ziviDome.setup();

  // Criação e configuração do SceneManager
  sceneManager = new SceneManager();
  sceneManager.registerScene(new Scene1(ziviDome)); // Registra apenas Scene1

  // Vincula o SceneManager à biblioteca zividomelive
  ziviDome.setSceneManager(sceneManager);

  // Define a cena inicial na biblioteca
  ziviDome.setScene(sceneManager.getCurrentScene());

  // Inicializa o ExecutorService para processamento de partículas
  int numThreads = Runtime.getRuntime().availableProcessors();
  println("Usando " + numThreads + " threads para processamento de partículas.");
  particleProcessors = Executors.newFixedThreadPool(numThreads);
}

void draw() {
  // Chama o método draw da biblioteca zividomelive
  ziviDome.draw();
}
