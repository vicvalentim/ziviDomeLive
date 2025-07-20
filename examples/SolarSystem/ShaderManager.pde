import processing.core.*;
import processing.opengl.*;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ShaderManager {
  private static final String SHADER_PATH = "data/shader/";
  private static final String DEFAULT_TEXTURE_UNIFORM = "texSampler";

  private final PApplet pApplet;
  private final HashMap<String, PShader> shaders = new HashMap<>();
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final ConcurrentLinkedQueue<Runnable> renderQueue = new ConcurrentLinkedQueue<>();

  public ShaderManager(PApplet pApplet) {
    this.pApplet = pApplet;
  }

  public void loadShader(String name, String fragFile, String vertFile) {
    lock.writeLock().lock();
    try {
      String fragPath = SHADER_PATH + fragFile;
      String vertPath = SHADER_PATH + vertFile;
      PShader shader = pApplet.loadShader(fragPath, vertPath);
      shaders.put(name, shader);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void loadUnifiedShader(String name, String unifiedFile) {
    lock.writeLock().lock();
    try {
      String path = SHADER_PATH + unifiedFile;
      PShader shader = pApplet.loadShader(path);
      shaders.put(name, shader);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void queueUnifiedShaderLoad(String name, String unifiedPath) {
    renderQueue.add(() -> loadUnifiedShader(name, unifiedPath));
  }

  public void executePending() {
    Runnable task;
    while ((task = renderQueue.poll()) != null) {
      task.run();
    }
  }

  public PShader getShader(String name) {
    lock.readLock().lock();
    try {
      return shaders.get(name);
    } finally {
      lock.readLock().unlock();
    }
  }

  public void applyShader(PGraphicsOpenGL pg, String name) {
    PShader shader = getShader(name);
    if (shader != null) {
      pg.shader(shader);
    }
  }

  public void setTexture(String shaderName, PImage texture) {
    PShader shader = getShader(shaderName);
    if (shader != null && texture != null) {
      shader.set(DEFAULT_TEXTURE_UNIFORM, texture);
    }
  }

  public void resetShader(PGraphicsOpenGL pg) {
    pg.resetShader();
  }

  public void setUniform(String shaderName, String uniformName, float value) {
    PShader shader = getShader(shaderName);
    if (shader != null) {
      shader.set(uniformName, value);
    }
  }

  public void setUniform(String shaderName, String uniformName, PVector vec) {
    PShader shader = getShader(shaderName);
    if (shader != null) {
      shader.set(uniformName, vec.x, vec.y, vec.z);
    }
  }

  public void dispose() {
    lock.writeLock().lock();
    try {
      shaders.clear();
    } finally {
      lock.writeLock().unlock();
    }
  }
}
