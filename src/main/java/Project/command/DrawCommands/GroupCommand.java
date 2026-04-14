package Project.command.DrawCommands;


import Project.command.Command;
import javafx.scene.Group;

/**
 * A Command that has a Group.
 */
public abstract class GroupCommand implements Command {

    protected Group group;
    public GroupCommand(Group group) {
        this.group = group;
    }
}
