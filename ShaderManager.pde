import processing.core.*;
import processing.opengl.*;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ShaderManager {
  private final PApplet pApplet;
  private final HashMap<String, PShader> shaders = new HashMap<>();
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final ConcurrentLinkedQueue<Runnable> renderQueue = new ConcurrentLinkedQueue<>();

  public ShaderManager(PApplet pApplet) {
    this.pApplet = pApplet;
  }

  // Carrega shader separado: vertex + fragment
  public void loadShader(String name, String fragPath, String vertPath) {
    lock.writeLock().lock();
    try {
      PShader shader = pApplet.loadShader(fragPath, vertPath);
      shaders.put(name, shader);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // Suporte legado para shader unificado (.glsl)
  public void loadUnifiedShader(String name, String unifiedPath) {
    lock.writeLock().lock();
    try {
      PShader shader = pApplet.loadShader(unifiedPath);
      shaders.put(name, shader);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // Enfileira carregamento unificado de forma segura
  public void queueUnifiedShaderLoad(String name, String unifiedPath) {
    renderQueue.add(() -> loadUnifiedShader(name, unifiedPath));
  }

  // Executa shaders pendentes para carregamento
  public void executePending() {
    Runnable task;
    while ((task = renderQueue.poll()) != null) {
      task.run();
    }
  }

  // Retorna o shader pelo nome
  public PShader getShader(String name) {
    lock.readLock().lock();
    try {
      return shaders.get(name);
    } finally {
      lock.readLock().unlock();
    }
  }

  // Aplica shader ao contexto gráfico
  public void applyShader(PGraphicsOpenGL pg, String name) {
    PShader shader = getShader(name);
    if (shader != null) {
      pg.shader(shader);
    }
  }

  // Remove o shader atual do contexto
  public void resetShader(PGraphicsOpenGL pg) {
    pg.resetShader();
  }

  // Uniformes auxiliares
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

  // Libera memória dos shaders
  public void dispose() {
    lock.writeLock().lock();
    try {
      shaders.clear();
    } finally {
      lock.writeLock().unlock();
    }
  }
}
