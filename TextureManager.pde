class TextureManager {
  PApplet pApplet;
  HashMap<String, PImage> textures;
  Object lock = new Object();  // Alternativa ao uso de ReentrantReadWriteLock no Processing

  TextureManager(PApplet pApplet) {
    this.pApplet = pApplet;
    textures = new HashMap<String, PImage>();
  }

  // Método para carregar uma textura de forma assíncrona
  void loadTextureAsync(final String filename) {
    new Thread(new Runnable() {
      public void run() {
        synchronized(lock) {
          if (!textures.containsKey(filename)) {
            PImage img = pApplet.loadImage(filename);
            textures.put(filename, img);
          }
        }
      }
    }).start();
  }

  // Método síncrono que retorna a textura (usa cache)
  PImage getTexture(String filename) {
    synchronized(lock) {
      if (!textures.containsKey(filename)) {
        // Se a textura não estiver carregada, carrega-a de forma síncrona
        PImage img = pApplet.loadImage(filename);
        textures.put(filename, img);
      }
      return textures.get(filename);
    }
  }

  // Método para carregar várias texturas previamente (em uma thread separada)
  void preloadTextures(final String[] filenames) {
    new Thread(new Runnable() {
      public void run() {
        for (String filename : filenames) {
          loadTextureAsync(filename);
        }
      }
    }).start();
  }

  public void clear() {
    synchronized(lock) {
      textures.clear();
    }
  }
}