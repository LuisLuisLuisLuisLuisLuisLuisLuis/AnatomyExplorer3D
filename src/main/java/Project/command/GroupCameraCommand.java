package Project.command;

import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;

/**
 * A command that has a Group and a Camera.
 */
public abstract class GroupCameraCommand extends GroupCommand{
    protected PerspectiveCamera camera;
    public GroupCameraCommand(Group group, PerspectiveCamera camera) {
        super(group);
        this.camera = camera;
    }
}
