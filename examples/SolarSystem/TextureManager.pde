import java.util.HashMap;

class TextureManager {
  private static final String TEXTURE_PATH = "textures/";
  
  private final PApplet pApplet;
  private final HashMap<String, PImage> textures = new HashMap<String, PImage>();

  TextureManager(PApplet pApplet) {
    this.pApplet = pApplet;
  }

  // Normaliza o caminho da textura
  private String resolvePath(String filename) {
    return TEXTURE_PATH + filename;
  }

  // Carrega no Processing thread ou retorna a textura da cache.
  PImage getTexture(String filename) {
    String path = resolvePath(filename);
    if (!textures.containsKey(path)) {
      PImage img = pApplet.loadImage(path);
      if (img != null) {
        textures.put(path, img);
      } else {
        pApplet.println("[TextureManager] Erro ao carregar textura: " + path);
        return null;
      }
    }
    return textures.get(path);
  }

  // Remove as referências; as PImage continuam sob ownership do Processing.
  public void clear() {
    textures.clear();
  }
}
