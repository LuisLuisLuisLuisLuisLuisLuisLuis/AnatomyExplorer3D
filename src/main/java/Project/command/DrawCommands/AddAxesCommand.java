package Project.command.DrawCommands;

import Project.window.ThreeDPaneHandling.Objects.Axes;
import javafx.scene.Group;

/**
 * Use this to undoably add Axes to the specified group.
 * Not using this for now because the styling doesnt suit the anatomy.
 */
public class AddAxesCommand extends GroupCommand {
    /**
     * Adds an Axes group to the given group.
     */
    public AddAxesCommand(Group innergroup) {
        super(innergroup);
    }

    @Override
    public void execute() {
        group.getChildren().add(new Axes(2000));    // add an axes object
    }

    @Override
    public void undo() {
        group.getChildren().removeIf(node -> node instanceof Axes); // remove all axes. there can only ever be one.
    }
    @Override
    public void redo() {
        execute();
    }

    @Override
    public String name() {
        return "AddAxesCommand";
    }
}
