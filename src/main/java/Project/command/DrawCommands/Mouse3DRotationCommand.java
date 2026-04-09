package Project.command.DrawCommands;

import Project.command.Command;
import Project.window.ThreeDPaneHandling.Movement.Group3DRotation;
import javafx.geometry.Point3D;
import javafx.scene.Group;

/**
 * A class to make mouse rotation undoable
 */
public class Mouse3DRotationCommand implements Command {

    private double deltaX;
    private double deltaY;
    private Group group;

    public Mouse3DRotationCommand(double deltaX, double deltaY, Group group) {
        this.group = group;
        this.deltaX = deltaX;   // remember.
        this.deltaY = deltaY;
    }

    @Override
    public void undo() {
        double dy = -deltaY;
        double dx = -deltaX;
        var axis = new Point3D(dy, -dx, 0).normalize();
        var angle = Math.sqrt(dx * dx + dy * dy) * 0.5; // based on the distance of the mouse movement
        Group3DRotation.applyGlobalRotation(group, axis, angle);
    }

    @Override
    public void execute() {
        // mock.
        // mouse rotation is done via mouse, not command. so this is just to undo and redo.
    }

    @Override
    public void redo() {

        double dy = deltaY;
        double dx = deltaX;
        var axis = new Point3D(dy, -dx, 0).normalize();
        var angle = Math.sqrt(dx * dx + dy * dy) * 0.5; // based on the distance of the mouse movement
        Group3DRotation.applyGlobalRotation(group, axis, angle);
    }

    @Override
    public String name() {
        return "Mouse3DRotationCommand";
    }

}
