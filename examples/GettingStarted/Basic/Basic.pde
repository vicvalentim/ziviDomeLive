import com.victorvalentim.zividomelive.*;
import controlP5.*;

ziviDomeLive ziviDome;

void settings() {
  pixelDensity(1);  // Library default policy
  size(1280, 720, P3D);  // Set the window size and P3D mode
}

void setup() {
  // Uncomment while diagnosing a sketch:
  // ziviDomeLive.enableDebugLogging();

  ziviDome = new ziviDomeLive(this);
  ziviDome.setup();
  ziviDome.setRenderMode(RenderMode.FULL);
  ziviDome.setScene(new Scene1(ziviDome));
  ziviDome.registerScene(new Scene2(ziviDome));
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}
