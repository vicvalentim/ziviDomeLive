import com.victorvalentim.zividomelive.*;
import controlP5.*;

ziviDomeLive ziviDome;

void settings() {
  pixelDensity(1);  // Library default policy
  size(1280, 720, P3D);  // Set the window size and P3D mode
}

void setup() {
  ziviDome = new ziviDomeLive(this);
  ziviDome.setup();
  ziviDome.setScene(new Scene1(ziviDome));
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}
