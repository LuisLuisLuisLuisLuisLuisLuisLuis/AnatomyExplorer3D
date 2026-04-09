package Project.command.DrawCommands;

import Project.SelectionModel.FXGroupDraw.HasFXGroupContents;
import Project.command.GroupCommand;
import Project.command.Remember.RememberHasFXGroupContents;
import Project.command.Remember.RememberHasSelection;
import Project.command.Remember.RememberFXPerspective;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.shape.MeshView;

public class ClearCommand<T> extends GroupCommand {

    private RememberHasFXGroupContents rememberFXGroupContents;
    private RememberFXPerspective rememberFxPerspective;
    private final HasFXGroupContents hasFXGroupContents;


    /**
     * Clears view and resets perspective.
     * @param hasSelection to be cleared
     * @param contentGroup Group whose perspective is to be reset
     */
    public ClearCommand(Group contentGroup, HasFXGroupContents hasSelection) {
        super(contentGroup);
        this.hasFXGroupContents = hasSelection;
    }

    @Override
    public void execute() {
        remember();
        hasFXGroupContents.clearSelection();
    }

    @Override
    public void undo() {    //restore contents and perspective
        this.rememberFXGroupContents.restoreSelection();
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
        this.rememberFxPerspective = new RememberFXPerspective(new PerspectiveCamera(), new Group[]{group});
        this.rememberFXGroupContents = new RememberHasFXGroupContents(hasFXGroupContents);
    }
}
