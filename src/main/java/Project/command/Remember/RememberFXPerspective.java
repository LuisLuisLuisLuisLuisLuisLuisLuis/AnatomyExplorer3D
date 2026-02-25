package Project.command.Remember;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.transform.Transform;

import java.util.LinkedList;


public class RememberFXPerspective {

    PerspectiveCamera camera;
    Point3D cameraPosition;

    private final RememberNodeTransform[] rememberNodeTransforms;

    /**
     * Remember the 3D perspective, made up of group transforms and camera position.
     *
     * @param camera the camera
     * @param groups All the groups whose Transforms should be remembered
     */
    public RememberFXPerspective(PerspectiveCamera camera, Group[] groups) {
        this.camera = camera;
        this.cameraPosition = new Point3D(  // save camera positon
                camera.getTranslateX(),
                camera.getTranslateY(),
                camera.getTranslateZ()
        );
        //remember all the group transforms
        this.rememberNodeTransforms = new RememberNodeTransform[groups.length];
        for (int i = 0; i < groups.length; i++) rememberNodeTransforms[i] = new RememberNodeTransform(groups[i]);
    }
    public void restorePerspective() {
        for (RememberNodeTransform rememberNodeTransform : rememberNodeTransforms) rememberNodeTransform.restore();
        camera.setTranslateZ(this.cameraPosition.getZ());
        camera.setTranslateX(this.cameraPosition.getX());
        camera.setTranslateY(this.cameraPosition.getY());
    }
}
