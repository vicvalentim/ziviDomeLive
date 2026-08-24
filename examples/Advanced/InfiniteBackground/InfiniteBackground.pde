import com.victorvalentim.zividomelive.*;
import controlP5.*;

ziviDomeLive ziviDome;

void settings() {
  pixelDensity(1);
  fullScreen(P3D, 1);
  //size(1280, 720, P3D);
}

void setup() {
  ziviDome = new ziviDomeLive(this);
  ziviDome.setup();
  ziviDome.setRenderMode(RenderMode.FULL);
  ziviDome.setScene(new InfiniteBackgroundScene(ziviDome));
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}
