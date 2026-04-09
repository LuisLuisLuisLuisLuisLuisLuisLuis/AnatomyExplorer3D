package Project.window.ThreeDPaneHandling.Movement;

import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;

/**
 * Has a camera and moves it.
 */
public class CamMover {
    private final PerspectiveCamera camera;
    public CamMover(PerspectiveCamera camera) {
        this.camera = camera;
    }
    public void moveX(double X) {camera.setTranslateX(camera.getTranslateX() + X);}
    public void moveY(double Y) {camera.setTranslateY(camera.getTranslateY() + Y);}
    public void moveZ(double Z) {camera.setTranslateZ(camera.getTranslateZ() + Z);}
    public Point3D getCamLocation() {return new Point3D(camera.getTranslateX(), camera.getTranslateY(), camera.getTranslateZ());}
}
