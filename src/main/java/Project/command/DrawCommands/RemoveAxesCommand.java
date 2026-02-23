package Project.command.DrawCommands;

import Project.command.GroupCommand;
import Project.window.ThreeDPaneHandling.Axes;
import javafx.scene.Group;

public class RemoveAxesCommand extends GroupCommand {
    /**
     * Removes Axes group(s) from the given group if present.
     * In this state, undo always adds axes without checking if any were present when command was executed,
     * meaning that in this state, this command must only be called if there really is an Axis to remove.
     */
    public RemoveAxesCommand(Group innergroup) {
        super(innergroup);
    }

    @Override
    public void execute() {
        group.getChildren().removeIf(node -> node instanceof Axes);
    }

    @Override
    public void undo() {
        group.getChildren().add(new Axes(20));
    }
    @Override
    public void redo() {
        execute();
    }

    @Override
    public String name() {
        return "RemoveAxesCommand";
    }
}
