import com.victorvalentim.zividomelive.*;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;

ziviDomeLive ziviDome;
SceneManager sceneManager;
PImage realEnvironment;
PImage calibrationEnvironment;

void settings() {
  pixelDensity(1);
  size(1280, 720, P3D);
}

void setup() {
  ziviDome = new ziviDomeLive(this);
  ziviDome.setup();
  ziviDome.setRenderMode(RenderMode.EQUIRECTANGULAR);

  String environmentPath = sketchPath("../SolarSystem/data/textures/8k_stars_milky_way.jpg");
  realEnvironment = loadImage(environmentPath);
  calibrationEnvironment = createCalibrationEnvironment(2048, 1024);
  if (realEnvironment == null) {
    println("[InfiniteBackground] Could not load environment: " + environmentPath);
  }
  ziviDome.setEquirectangularBackground(calibrationEnvironment);
  ziviDome.setEnvironmentBackgroundIntensity(1.0);
  println("[InfiniteBackground] Synthetic calibration Environment active.");

  sceneManager = new SceneManager();
  sceneManager.registerScene(new InfiniteBackgroundScene(
    ziviDome, realEnvironment, calibrationEnvironment));
  ziviDome.setSceneManager(sceneManager);
}

void draw() {
  // ziviDomeLive renders through its registered Processing draw hook.
}

// Generated once during setup: this is a small diagnostic source, never a frame-loop readback.
PImage createCalibrationEnvironment(int width, int height) {
  PImage image = createImage(width, height, ARGB);
  image.loadPixels();
  for (int y = 0; y < height; y++) {
    float v = y / float(height - 1);
    float latitudeShade = 0.45 + 0.55 * sin(v * PI);
    for (int x = 0; x < width; x++) {
      float u = x / float(width);
      int sector = floor(u * 8.0) % 8;
      int baseR = int((35 + sector * 17) * latitudeShade);
      int baseG = int((45 + ((sector + 3) % 8) * 14) * latitudeShade);
      int baseB = int((60 + ((sector + 5) % 8) * 16) * latitudeShade);
      int pixelColor = color(baseR, baseG, baseB);

      boolean longitudeGrid = x % (width / 24) < 2;
      boolean latitudeGrid = y % (height / 12) < 2;
      if (longitudeGrid || latitudeGrid) {
        pixelColor = color(220, 220, 220);
      }

      float markerWidth = 0.006;
      if (circularDistance(u, 0.00) < markerWidth) pixelColor = color(255, 0, 255); // -Z Back / seam
      if (circularDistance(u, 0.25) < markerWidth) pixelColor = color(0, 255, 255); // -X Left
      if (circularDistance(u, 0.50) < markerWidth) pixelColor = color(255, 220, 0); // +Z Front
      if (circularDistance(u, 0.75) < markerWidth) pixelColor = color(255, 50, 50); // +X Right
      if (v < 0.012) pixelColor = color(50, 255, 80);                              // +Y Top
      if (v > 0.988) pixelColor = color(120, 60, 255);                             // -Y Bottom

      image.pixels[y * width + x] = pixelColor;
    }
  }
  image.updatePixels();
  return image;
}

float circularDistance(float a, float b) {
  float direct = abs(a - b);
  return min(direct, 1.0 - direct);
}
