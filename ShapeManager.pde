import processing.core.*;
import processing.opengl.*;
import java.util.HashMap;

public class ShapeManager {
  private PApplet pApplet;

  private final HashMap<String, PShape> wireShapes = new HashMap<>();
  private final HashMap<String, PShape> solidShapes = new HashMap<>();
  private final HashMap<String, PShape> texturedShapes = new HashMap<>();

  // Detalhes configuráveis
  private int lowDetail = 32;
  private int mediumDetail = 48;
  private int highDetail = 64;

  // Constantes globais para wireframe (defina esses valores conforme seu projeto)
  private static final float WIREFRAME_STROKE_WEIGHT = 1.5f;
  private static final int WIREFRAME_COLOR = 0xFFCCCCCC; // Exemplo: cinza claro

  public ShapeManager(PApplet pApplet) {
    this.pApplet = pApplet;
  }

  public PShape getShape(String name, int mode, PImage texture) {
    String key = name + ":" + mode;
    int detail = getDetailLevelByName(name);

    switch (mode) {
      case 0:  // Wireframe – use PShape padrão
        return wireShapes.computeIfAbsent(key, k -> 
          createSphereShape(detail, false, true, null, false)
        );
      case 1:  // Solid – usa PShapeOpenGL se possível
        return solidShapes.computeIfAbsent(key, k -> 
          createSphereShape(detail, true, false, null, true)
        );
      case 2:  // Textured – usa PShapeOpenGL se possível
        return texturedShapes.computeIfAbsent(key, k -> 
          createSphereShape(detail, true, false, texture, true)
        );
      default:
        return solidShapes.computeIfAbsent(key, k -> 
          createSphereShape(detail, true, false, null, true)
        );
    }
  }

  public void buildShape(String name, int mode, PImage texture) {
    String key = name + ":" + mode;
    int detail = getDetailLevelByName(name);

    PShape shape = createSphereShape(
      detail,
      mode != 0,            // fill: true para sólido e texturizado
      mode == 0,            // stroke: true apenas para wireframe
      mode == 2 ? texture : null,
      (mode != 0)           // Use OpenGL para modos sólidos e texturizados; wireframe usa o padrão
    );

    switch (mode) {
      case 0: // Wireframe
        wireShapes.put(key, shape); break;
      case 1: // Solid
        solidShapes.put(key, shape); break;
      case 2: // Textured
        texturedShapes.put(key, shape); break;
    }
  }

  /**
   * Cria uma esfera com o nível de detalhe especificado.
   * Se useOpenGL for true e o contexto atual for PGraphicsOpenGL, cria um PShapeOpenGL;
   * caso contrário, utiliza o método padrão (criação do PShape tradicional).
   */
  private PShape createSphereShape(int detail, boolean fill, boolean stroke, PImage tex, boolean useOpenGL) {
    pApplet.sphereDetail(detail);
    
    if (useOpenGL && pApplet.g instanceof PGraphicsOpenGL) {
      PGraphicsOpenGL pgogl = (PGraphicsOpenGL) pApplet.g;
      // Cria um PShapeOpenGL; o valor 1 indica uma esfera unitária
      PShapeOpenGL s = new PShapeOpenGL(pgogl, SPHERE, 1);
      s.setStroke(stroke);
      if (stroke) {
        s.setStrokeWeight(WIREFRAME_STROKE_WEIGHT); // Usa a espessura global do wireframe
        s.setStroke(WIREFRAME_COLOR);               // Usa a cor global do wireframe
      }
      s.setFill(fill);
      if (tex != null) s.setTexture(tex);
      return s;
    } else {
      // Caso não se use OpenGL (modo wireframe ou renderizador diferente)
      PShape s = pApplet.createShape(SPHERE, 1);
      s.setStroke(stroke);
      if (stroke) {
        s.setStrokeWeight(WIREFRAME_STROKE_WEIGHT);
        s.setStroke(WIREFRAME_COLOR);
      }
      s.setFill(fill);
      if (tex != null) s.setTexture(tex);
      return s;
    }
  }

  private int getDetailLevelByName(String name) {
    name = name.toLowerCase();
    if (name.contains("sun") || name.contains("jupiter") || name.contains("saturn")) {
      return highDetail;
    } else if (name.contains("moon") || name.contains("phobos") || name.contains("deimos")) {
      return lowDetail;
    } else {
      return mediumDetail;
    }
  }

  public void clearCache(int mode) {
    switch (mode) {
      case 0: wireShapes.clear(); break;
      case 1: solidShapes.clear(); break;
      case 2: texturedShapes.clear(); break;
    }
  }

  public void clearUnused(boolean keepWire, boolean keepSolid, boolean keepTextured) {
    if (!keepWire) wireShapes.clear();
    if (!keepSolid) solidShapes.clear();
    if (!keepTextured) texturedShapes.clear();
  }

  public void setDetailLevels(int low, int medium, int high) {
    this.lowDetail = low;
    this.mediumDetail = medium;
    this.highDetail = high;
  }
}
