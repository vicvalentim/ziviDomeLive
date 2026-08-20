import com.victorvalentim.zividomelive.*;
import com.victorvalentim.zividomelive.manager.OutputManager;
import com.victorvalentim.zividomelive.performance.*;
import com.victorvalentim.zividomelive.render.gl.ProcessingGlAdapter;
import com.victorvalentim.zividomelive.render.gl.ProcessingGlCapabilities;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;
import processing.core.PApplet;
import processing.core.PShape;
import processing.opengl.PGraphicsOpenGL;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

ziviDomeLive ziviDome;
SceneManager sceneManager;
Scene[] benchmarkScenes;
BenchmarkController benchmarkController;

void settings() {
  size(1600, 900, P3D);
  pixelDensity(1);
}

void setup() {
  surface.setTitle("ziviDomeLive - Benchmark Tool");

  ziviDome = new ziviDomeLive(this);
  ziviDome.setup();

  benchmarkScenes = createBenchmarkScenes(ziviDome);
  sceneManager = new SceneManager();
  for (Scene scene : benchmarkScenes) {
    sceneManager.registerScene(scene);
  }
  ziviDome.setSceneManager(sceneManager);

  benchmarkController = new BenchmarkController(
      this,
      ziviDome,
      sceneManager,
      benchmarkScenes);
}

void draw() {
  if (benchmarkController != null) {
    benchmarkController.update();
  }
}

void controlEvent(ControlEvent event) {
  if (benchmarkController != null) {
    benchmarkController.controlEvent(event);
  }
}

void keyPressed() {
  if (benchmarkController == null) return;
  if (key == 'x' || key == 'X') benchmarkController.stopRun();
  if (key == 'e' || key == 'E') benchmarkController.exportLastRun();
}
