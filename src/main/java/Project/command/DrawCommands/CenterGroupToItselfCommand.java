package Project.command.DrawCommands;

import Project.command.GroupCommand;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

import java.util.LinkedList;

/**
 *
 */
public class CenterGroupToItselfCommand extends GroupCommand {

    /**
     * Centers the specified 3D group to its own local bounding box.
     * This method calculates the center point of the group's local bounds
     * and applies a translation to shift the group so that its center aligns with the origin.
     *
     * @param group The 3D group to be centered relative to its local coordinate system.
     */
    public CenterGroupToItselfCommand(Group group) {
        super(group);
    }

    private LinkedList<Transform> transforms = new LinkedList<>();

    @Override
    public void execute() {
        remember();

        Bounds bounds = group.getBoundsInLocal();
        double X = (bounds.getMinX() + bounds.getMaxX()) / 2;
        double Y = (bounds.getMinY() + bounds.getMaxY()) / 2;
        double Z = (bounds.getMinZ() + bounds.getMaxZ()) / 2;
        group.getTransforms().setAll(new Translate(-X, -Y, -Z));
    }

    @Override
    public void undo() {
        group.getTransforms().clear();
        group.getTransforms().addAll(this.transforms);
    }
    @Override
    public void redo() {
        execute();
    }

    @Override
    public String name() {
        return "CenterGroupToItselfCommand";
    }

    private void remember() {
        this.transforms.clear();
        this.transforms.addAll(group.getTransforms());
    }
}
