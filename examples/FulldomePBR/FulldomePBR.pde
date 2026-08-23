import com.victorvalentim.zividomelive.*;
import com.victorvalentim.zividomelive.render.Quaternion;
// Processing adds contributed libraries to the runtime classpath through imports.
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;
import processing.opengl.PGraphicsOpenGL;

ziviDomeLive ziviDome;

void settings() {
  pixelDensity(1);
  size(1280, 720, P3D);
}

void setup() {
  surface.setTitle("ziviDomeLive - Fulldome PBR Example");

  // Uncomment while diagnosing a sketch: ziviDomeLive.enableDebugLogging();

  ziviDome = new ziviDomeLive(this);
  ziviDome.setup();
  ziviDome.setRenderMode(RenderMode.FULL);
  ziviDome.setScene(new FulldomePbrScene());
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}
