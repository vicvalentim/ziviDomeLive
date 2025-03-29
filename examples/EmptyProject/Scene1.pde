// Implementation of Scene1 that uses the Scene interface
class Scene1 implements Scene {
  private zividomelive parent;

  Scene1(zividomelive parent) {
    this.parent = parent;
  }


  public void setupScene() {
    // Specific scene setup, if necessary
  }

 
  public void sceneRender(PGraphicsOpenGL pg) {
    // Scene rendering logic
  }

  public void keyEvent(processing.event.KeyEvent event) {
      if (event.getAction() == processing.event.KeyEvent.PRESS) { // Only handle key press events
          char key = event.getKey();
          println("Key pressed in Scene1: " + key);
      }
  }

  public void mouseEvent(MouseEvent event) {
    
  }

  public void controlEvent(controlP5.ControlEvent theEvent) {
      println("Control event in Scene1: " + theEvent.getName());
  }
  
}
