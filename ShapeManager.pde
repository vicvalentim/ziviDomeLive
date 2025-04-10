public class ShapeManager {
  private PApplet pApplet;

  private final HashMap<String, PShape> wireShapes = new HashMap<>();
  private final HashMap<String, PShape> solidShapes = new HashMap<>();
  private final HashMap<String, PShape> texturedShapes = new HashMap<>();

  // Detalhes configuráveis
  private int lowDetail = 32;
  private int mediumDetail = 48;
  private int highDetail = 64;

  public ShapeManager(PApplet pApplet) {
    this.pApplet = pApplet;
  }

  public PShape getShape(String name, int mode, PImage texture) {
    String key = name + ":" + mode;
    int detail = getDetailLevelByName(name);

    switch (mode) {
      case 0:  // Wireframe
        return wireShapes.computeIfAbsent(key, k -> createSphereShape(detail, false, true, null));
      case 1:  // Solid
        return solidShapes.computeIfAbsent(key, k -> createSphereShape(detail, true, false, null));
      case 2:  // Textured
        return texturedShapes.computeIfAbsent(key, k -> createSphereShape(detail, true, false, texture));
      default:
        return solidShapes.computeIfAbsent(key, k -> createSphereShape(detail, true, false, null));
    }
  }

  public void buildShape(String name, int mode, PImage texture) {
    String key = name + ":" + mode;
    int detail = getDetailLevelByName(name);

    PShape shape = createSphereShape(
      detail,
      mode != 0, // fill: true para sólido e texturizado
      mode == 0, // stroke: true apenas para wireframe
      mode == 2 ? texture : null
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

  private PShape createSphereShape(int detail, boolean fill, boolean stroke, PImage tex) {
    pApplet.sphereDetail(detail);
    PShape s = pApplet.createShape(PConstants.SPHERE, 1);
    s.setStroke(stroke);
    if (stroke) {
      s.setStrokeWeight(WIREFRAME_STROKE_WEIGHT);  // Usa a espessura global do wireframe
      s.setStroke(WIREFRAME_COLOR); // Usa a cor global do wireframe
    }
    s.setFill(fill);
    if (tex != null) s.setTexture(tex);
    return s;
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
