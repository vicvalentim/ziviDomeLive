public class SimulatedClock {
  private float simulatedDays;
  private float timeScale;
  private long lastUpdateNanos;
  private boolean paused = false;

  public SimulatedClock(float initialTimeInDays, float timeScale) {
    this.simulatedDays = initialTimeInDays;
    this.timeScale = timeScale;
    this.lastUpdateNanos = System.nanoTime();
  }

  public float update() {
    long now = System.nanoTime();
    float deltaRealSeconds = (now - lastUpdateNanos) / 1_000_000_000.0f;
    lastUpdateNanos = now;

    if (paused) return 0.0f;

    float deltaSimulatedDays = deltaRealSeconds * timeScale;
    simulatedDays += deltaSimulatedDays;
    return deltaSimulatedDays;
  }

  public float getCurrentTimeInDays() {
    return simulatedDays;
  }

  public void setTimeInDays(float days) {
    this.simulatedDays = days;
  }

  public void setTimeScale(float timeScale) {
    this.timeScale = timeScale;
  }

  public float getTimeScale() {
    return timeScale;
  }

  public void pause() {
    this.paused = true;
  }

  public void resume() {
    this.paused = false;
    this.lastUpdateNanos = System.nanoTime();
  }

  public boolean isPaused() {
    return paused;
  }

  public void jumpDays(float daysToJump) {
    this.simulatedDays += daysToJump;
  }

  public void reset(float startDays) {
    this.simulatedDays = startDays;
    this.lastUpdateNanos = System.nanoTime();
  }
}
