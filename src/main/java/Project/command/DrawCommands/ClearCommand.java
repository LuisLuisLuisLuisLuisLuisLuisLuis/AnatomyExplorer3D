package Project.command.DrawCommands;

import Project.command.GroupCommand;
import Project.command.Remember.RememberFXGroupContents;
import Project.command.Remember.RememberFXPerspective;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;

public class ClearCommand extends GroupCommand {

    private RememberFXGroupContents fxGroupSelection;
    private RememberFXPerspective rememberFxPerspective;
    private Group innerGroup;

    /**
     * Clears the provided group.
     */
    public ClearCommand(Group contentGroup, Group innergroup) {
        super(contentGroup);
        this.innerGroup = innergroup;
    }

    @Override
    public void execute() {
        remember();
        innerGroup.getChildren().clear();
    }

    @Override
    public void undo() {    //restore contents and perspective
        this.fxGroupSelection.restoreSelection();
        this.rememberFxPerspective.restorePerspective();
    }
    @Override
    public void redo() {
        execute();
    }

    @Override
    public String name() {
        return "ClearCommand";
    }

    private void remember() {
        this.rememberFxPerspective = new RememberFXPerspective(new PerspectiveCamera(), group, innerGroup);
        this.fxGroupSelection = new RememberFXGroupContents(innerGroup);
    }
}
