// Scene2 implementation (Example for additional scene)
class Scene2 implements Scene {
  private zividomelive parent;

  Scene2(zividomelive parent) {
      this.parent = parent;
  }

  public void setupScene() {
      println("Scene2 setup completed.");
  }

  public void update() {
      // Optional update logic
  }

  public void sceneRender(PGraphicsOpenGL pg) {
      pg.background(50, 50, 150); // Set a different background
      pg.fill(255);
      pg.textSize(36);
      pg.textAlign(CENTER, CENTER);
      pg.text("Welcome to Scene2", pg.width / 2f, pg.height / 2f);
  }

  public void keyEvent(processing.event.KeyEvent event) {
      println("Key pressed in Scene2.");
  }

  public void mouseEvent(MouseEvent event) {
      println("Mouse event in Scene2.");
  }

  public void controlEvent(controlP5.ControlEvent theEvent) {
      println("Control event in Scene2: " + theEvent.getName());
  }

  public String getName() {
      return "Scene2";
  }
}
