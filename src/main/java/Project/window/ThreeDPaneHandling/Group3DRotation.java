package Project.window.ThreeDPaneHandling;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

public class Group3DRotation {

    private final Group contentGroup;


    public Group3DRotation(Group contentGroup) {
        this.contentGroup = contentGroup;
    }

    /**
     * Applies a global rotation transformation to the specified 3D content group.
     * This method rotates the group around the given axis by the specified angle
     * and updates its transformation accordingly.
     *
     * @param contentGroup The group of 3D content to which the rotation transformation
     *                     will be applied.
     * @param axis The axis around which the 3D content group will be rotated, represented
     *             as a {@code Point3D}.
     * @param angle The rotation angle in degrees to apply to the 3D content group.
     */
    public static void applyGlobalRotation(Group contentGroup, Point3D axis, double angle) {
        Transform currentTransform = (contentGroup.getTransforms().isEmpty())? new Translate() : contentGroup.getTransforms().getFirst();
        var rotate = new Rotate(angle, currentTransform.getTx(), currentTransform.getTy(), currentTransform.getTz(), axis);
        currentTransform = rotate.createConcatenation(currentTransform);
        contentGroup.getTransforms().setAll(currentTransform);
    }

    public static void applyGlobalRotation(Group contentGroup, Point3D axis, double angle, Point3D pivot) {
        Transform currentTransform = (contentGroup.getTransforms().isEmpty())? new Translate() : contentGroup.getTransforms().getFirst();
        var rotate = new Rotate(angle, pivot.getX(), pivot.getY(), pivot.getZ(), axis);
        currentTransform = rotate.createConcatenation(currentTransform);
        contentGroup.getTransforms().setAll(currentTransform);
    }

    /*
    ----------ON TRANSFORMS AND ROTATIONS-----------------
    Applying a Translate to a group does not change whatever it stores in group.getTranslateX/Y/Z().
    It can only be found in group.getTransforms().getFirst().getTx/y/z(). (Note: transform.toString() is very useful).
    This is the center of the groups contents I believe.
    A Rotate() needs a pivot point to rotate around. To rotate around its center, use the coords given by
    group.getTransforms().getFirst().getTx/y/z().

    you can play around with:
            System.out.println("Group getXYZ: " + group.getTranslateX() + " " + group.getTranslateY() + " " + group.getTranslateZ()); -> always zeros
            Transform transform = group.getTransforms().getFirst();
            System.out.println(transform);
            System.out.println(transform.getTx() + " " + transform.getTy() + " "+ transform.getTz()); -> pivot point for rotation

     */

    public void applyGlobalRotation(Point3D axis, double angle) {
        var currentTransform = contentGroup.getTransforms().isEmpty() ? new Rotate() : contentGroup.getTransforms().getFirst();
        var rotate = new Rotate(angle, currentTransform.getTx(), currentTransform.getTy(), currentTransform.getTz(), axis);
        currentTransform = rotate.createConcatenation(currentTransform);
        contentGroup.getTransforms().setAll(currentTransform);
    }

    public void applyTranslate(Point3D step) {
        var currentTransform = contentGroup.getTransforms().isEmpty() ? new Translate() : contentGroup.getTransforms().getFirst();
        var translate = new Translate(step.getX(), step.getY(), step.getZ());
        currentTransform = translate.createConcatenation(currentTransform);
        contentGroup.getTransforms().setAll(currentTransform);
    }





}
