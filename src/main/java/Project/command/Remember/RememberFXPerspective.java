package Project.command.Remember;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.transform.Transform;

import java.util.LinkedList;


public class RememberFXPerspective {
    Group group;
    Group innerGroup;
    Point3D axis;
    double value;
    PerspectiveCamera camera;
    LinkedList<Transform> transforms = new LinkedList<>();
    Point3D cameraPosition;

    /**
     *
     * Remember the 3D perspective, made up of group transforms and camera position.
     *
     * @param camera an camera
     * @param group Holds innerGroup.
     * @param innerGroup Holds meshViews
     */
    public RememberFXPerspective(PerspectiveCamera camera, Group group, Group innerGroup) {
        this.cameraPosition = new Point3D(  // save camera positon
                camera.getTranslateX(),
                camera.getTranslateY(),
                camera.getTranslateZ()
        );
        // save everything else
        this.transforms.addAll(group.getTransforms());
        this.group = group;
        this.innerGroup = innerGroup;
        this.axis = innerGroup.getRotationAxis();
        this.value = group.getRotate();
        this.camera = camera;
    }
    public void restorePerspective() {
//        group.getTransforms().clear();
//        group.getTransforms().addAll(this.transforms);
        group.getTransforms().setAll(this.transforms);
        camera.setTranslateZ(this.cameraPosition.getZ());
        camera.setTranslateX(this.cameraPosition.getX());
        camera.setTranslateY(this.cameraPosition.getY());
//        innerGroup.setRotationAxis(axis);
//        innerGroup.setRotate(value);
    }
}
