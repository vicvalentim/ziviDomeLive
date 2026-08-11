import com.victorvalentim.zividomelive.*;
// Processing adds contributed libraries to the runtime classpath through imports.
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;
import processing.event.KeyEvent;
import processing.opengl.PGraphicsOpenGL;

zividomelive ziviDome;

void settings() {
  size(1280, 720, P3D);
  pixelDensity(1);
}

void setup() {
  surface.setTitle("ziviDomeLive - Empty Project");

  ziviDome = new zividomelive(this);
  ziviDome.setup();
  ziviDome.setScene(new Scene1(ziviDome));
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}
