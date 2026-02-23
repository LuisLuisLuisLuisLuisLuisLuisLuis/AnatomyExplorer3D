package Project.command.DrawCommands;

import Project.command.Command;
import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;

/**
 * A class to make scrolling undoable.
 */
public class Mouse3DScrollCommand implements Command {

    private final PerspectiveCamera camera;
    private Point3D camPositionPrev;    // remember.
    private Point3D camPositionPost;

    /**
     * @param camPositionPrev: previous cameraPosition
     * @param camPositionPost: position after zooming
     * @param camera: the camera
     */
    public Mouse3DScrollCommand(Point3D camPositionPrev, Point3D camPositionPost, PerspectiveCamera camera) {
        this.camPositionPrev = camPositionPrev;
        this.camPositionPost = camPositionPost;
        this.camera = camera;
    }

    @Override
    public void undo() {    //set the previous position
        this.camera.setTranslateX(camPositionPrev.getX());
        this.camera.setTranslateY(camPositionPrev.getY());
        this.camera.setTranslateZ(camPositionPrev.getZ());
    }

    @Override
    public void execute() {
        // mock.
        // scrolling is done via mouse, not command. so this is just to undo-redo.
    }

    @Override
    public void redo() {    // redo the position
        this.camera.setTranslateX(camPositionPost.getX());
        this.camera.setTranslateY(camPositionPost.getY());
        this.camera.setTranslateZ(camPositionPost.getZ());
    }

    @Override
    public String name() {
        return "Mouse3DScrollCommand";
    }
}
