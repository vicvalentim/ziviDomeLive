import com.victorvalentim.zividomelive.*;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;

// Instâncias principais
zividomelive ziviDome;  // Instância da biblioteca zividomelive
Scene currentScene;     // Cena atual que implementa a interface Scene

void settings() {
  size(1280, 720, P3D);  // Define o tamanho da janela e o modo P3D
}

void setup() {
  // Inicializa a biblioteca zividomelive
  ziviDome = new zividomelive(this);
  ziviDome.setup();  // Configuração inicial da biblioteca

  // Inicializa a cena e a define na biblioteca
  currentScene = new Scene1(ziviDome);
  ziviDome.setScene(currentScene);

}

void draw() {
  // Chama o método draw da biblioteca para processar renderizações adicionais
  ziviDome.draw();
}

// Implementação da cena Scene1 que usa a interface Scene
class Scene1 implements Scene {
  zividomelive parent;

  Scene1(zividomelive parent) {
    this.parent = parent;
  }

  @Override
  public void setupScene() {
    // Configurações específicas da cena, se necessário
    println("Setup da Scene1 concluído.");
  }

  @Override
  public void sceneRender(PGraphics pg) {
    pg.pushMatrix();
    pg.background(0, 0, 80, 0);
    float radius = 700; // Distância do centro
    int numPillars = 8;
    float angleStep = TWO_PI / numPillars;
    int[] colors = {#FF0000, #00FF00, #0000FF, #FFFF00, #FF00FF, #00FFFF, #FFFFFF, #FF8000};
    float time = millis() / 2000.0; // Tempo em segundos para animação
    for (int i = 0; i < numPillars; i++) {
      float angle = angleStep * i + time; // Adiciona a animação de rotação
      float x = sin(angle) * radius;
      float y = cos(angle) * radius;
      pg.pushMatrix();
      pg.translate(x, y, 0);
      pg.rotateX(time); // Adiciona a rotação no eixo X
      pg.fill(colors[i]);
      pg.box(200); // Altere os parâmetros conforme a necessidade para ajustar o tamanho
      pg.popMatrix();
    }
    pg.popMatrix();
  }

  @Override
  public void keyPressed(char key) {
    // Implementar lógica de resposta a teclas, se necessário
    println("Tecla pressionada na Scene1: " + key);
  }
}

// Encaminha eventos de teclas para a biblioteca
void keyPressed() {
  ziviDome.keyPressed();
  // Opcional: chamar keyPressed da cena atual
  if (currentScene != null) {
    currentScene.keyPressed(key);
  }
}

// Encaminha eventos de mouse para a biblioteca
void mouseEvent(processing.event.MouseEvent event) {
  ziviDome.mouseEvent(event);
}

// Encaminha eventos de controle para a biblioteca
void controlEvent(controlP5.ControlEvent theEvent) {
  ziviDome.controlEvent(theEvent);
}
