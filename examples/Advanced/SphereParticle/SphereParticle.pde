import com.victorvalentim.zividomelive.*;
import controlP5.*;

// Instâncias principais
ziviDomeLive ziviDome;      // Instância da biblioteca ziviDomeLive

void settings() {
  pixelDensity(1);  // Library default policy
  size(1280, 720, P3D);  // Define o tamanho da janela e o modo P3D
}

void setup() {
  // Inicializa a biblioteca ziviDomeLive
  ziviDome = new ziviDomeLive(this);
  ziviDome.setup();

  ziviDome.setScene(new Scene1());

  println("Simulação de partículas usando SceneServices.tasks().");
}

void draw() {
  // ziviDomeLive renderiza pelo hook draw registrado no Processing.
}
