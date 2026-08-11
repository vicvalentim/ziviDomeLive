import com.victorvalentim.zividomelive.*;
// Processing adds contributed libraries to the runtime classpath through imports.
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;
import java.util.ArrayList;

zividomelive ziviDome;

void settings() {
  size(1280, 720, P3D);
  pixelDensity(1);
}

void setup() {
  surface.setTitle("ziviDomeLive - Sphere Particle");

  ziviDome = new zividomelive(this);
  ziviDome.setup();
  ziviDome.setScene(new ParticleFieldScene(ziviDome));
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}
