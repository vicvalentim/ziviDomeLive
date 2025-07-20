import java.util.HashMap;
import java.util.HashSet;

class TextureManager {
  private static final String TEXTURE_PATH = "textures/";
  
  private PApplet pApplet;
  private final HashMap<String, PImage> textures = new HashMap<String, PImage>();
  private final HashMap<String, String> textureAliases = new HashMap<String, String>();
  private final Object lock = new Object();

  TextureManager(PApplet pApplet) {
    this.pApplet = pApplet;
  }

  // Normaliza o caminho da textura
  private String resolvePath(String filename) {
    return TEXTURE_PATH + filename;
  }

  // Carregamento assíncrono com segurança
  void loadTextureAsync(final String filename) {
    new Thread(() -> {
      String path = resolvePath(filename);
      synchronized(lock) {
        if (!textures.containsKey(path)) {
          PImage img = pApplet.loadImage(path);
          if (img != null) {
            textures.put(path, img);
          } else {
            pApplet.println("[TextureManager] Erro ao carregar textura: " + path);
          }
        }
      }
    }).start();
  }

  // Pré-carrega uma lista de texturas
  void preloadTextures(final String[] filenames) {
    new Thread(() -> {
      for (String filename : filenames) {
        loadTextureAsync(filename);
      }
    }).start();
  }

  // Carrega ou retorna a textura da cache
  PImage getTexture(String filename) {
    String path = resolvePath(filename);
    synchronized(lock) {
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
  }

  // Registra o alias para um planeta ou lua
  void registerAlias(String objectName, String filename) {
    textureAliases.put(objectName.toLowerCase(), filename);
  }

  // Acessa a textura associada a um planeta ou lua por nome
  PImage getAliasTexture(String objectName) {
    String alias = textureAliases.get(objectName.toLowerCase());
    if (alias != null) {
      return getTexture(alias);
    }
    return null;
  }

  // Remove texturas que não estão mais em uso
  void clearUnused(HashSet<String> keepFilenames) {
    synchronized(lock) {
      HashSet<String> keepPaths = new HashSet<>();
      for (String name : keepFilenames) {
        keepPaths.add(resolvePath(name));
      }
      textures.keySet().removeIf(key -> !keepPaths.contains(key));
    }
  }

  // Libera toda a memória associada
  public void clear() {
    synchronized(lock) {
      textures.clear();
      textureAliases.clear();
    }
  }
}
