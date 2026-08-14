import com.victorvalentim.zividomelive.*;
import com.victorvalentim.zividomelive.render.Quaternion;
import com.victorvalentim.zividomelive.render.camera.OrbitCamera;
import controlP5.*;
import codeanticode.syphon.*;

// Main instances
ziviDomeLive ziviDome;      // Instance of the ziviDomeLive library

void settings() {
  pixelDensity(1);  // Library default policy
  size(1200, 800, P3D);  // Set the window size and P3D mode
}

void setup() {
  // Initialize the ziviDomeLive library
  ziviDome = new ziviDomeLive(this);
  ziviDome.setup();  // Initial setup of the library

  // setScene configura SceneServices antes de setupScene().
  ziviDome.setScene(new Scene1());
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}
