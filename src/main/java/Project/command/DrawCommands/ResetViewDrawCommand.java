package Project.command.DrawCommands;

import Project.command.Command;
import Project.command.Remember.RememberFXPerspective;
import Project.window.ThreeDPaneHandling.SetupMouseRotate3D;
import javafx.geometry.Point3D;
import javafx.scene.Camera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Group;
import javafx.scene.transform.Affine;

/**
 * Reset the view of the given group.
 */
public class ResetViewDrawCommand implements Command {

    private final PerspectiveCamera camera;
    private final Group[] groups;


    private static Affine initialTransform;
    private static Point3D initialCameraPosition;

    private RememberFXPerspective rememberFxPerspective;

    private final SetupMouseRotate3D setupMouseRotate3D;

    /**
     * Resets the view of the 3D scene to its initial state by adjusting
     * the camera position and resetting the transformations of the content group.
     *
     * @param groups All groups whose Transforms should be reset.
     * @param camera The camera.
     * @param initialTransform: Transform to which contentGroup will be reset to
     * @param initialCameraPosition: Reset position for the camera
     * @param setupMouseRotate3D:  Class responsible for any rotation animations
     */
    public ResetViewDrawCommand(Group[] groups, PerspectiveCamera camera, Affine initialTransform, Point3D initialCameraPosition, SetupMouseRotate3D setupMouseRotate3D) {
        this.initialTransform = initialTransform;
        this.initialCameraPosition = initialCameraPosition;
        this.setupMouseRotate3D = setupMouseRotate3D;
        this.camera = camera;
        this.groups = groups;
    }


    @Override
    public void execute() {
        remember();
        //reset camera, group transforms, stop rotation animations
        camera.setTranslateZ(initialCameraPosition.getZ());
        camera.setTranslateY(initialCameraPosition.getY());
        camera.setTranslateX(initialCameraPosition.getX());
        for (Group group : groups) group.getTransforms().setAll(initialTransform);
        setupMouseRotate3D.stopContinuousRotation();
    }

    @Override
    public void undo() {
        this.rememberFxPerspective.restorePerspective();
    }

    @Override
    public void redo() {
        execute();
    }

    @Override
    public String name() {
        return "ResetViewDrawCommand";
    }

    private void remember() {
        this.rememberFxPerspective = new RememberFXPerspective(camera, groups);  //remember current perspective
    }
}

