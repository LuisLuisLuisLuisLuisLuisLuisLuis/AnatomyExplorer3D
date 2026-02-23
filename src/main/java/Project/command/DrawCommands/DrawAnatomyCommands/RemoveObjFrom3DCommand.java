package Project.command.DrawCommands.DrawAnatomyCommands;

import Project.SelectionModel.FXGroupDraw.HasFXGroupContents;
import Project.SelectionModel.SelectionGroup;
import Project.command.Command;
import Project.command.Remember.RememberFXMeshViewColors;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Command for un-drawing items.
 */
public class RemoveObjFrom3DCommand implements Command {

    private Set<String> removedNodes;
    private SelectionGroup threeDContentGroup;
    private SelectionGroup threeDSelectionGroup;
    private HasFXGroupContents hasFXGroupContents;
    private RememberFXMeshViewColors rememberFXMeshViewColors;

    /**
     *
     * @param nodesToRemove: set of fileIDs
     * @param threeDContentGroup: SelectionGroup controlling which items are drawn.
     * @param threeDSelectionGroup: SelectionGroup holding the 3D selection.
     * @param hasFXGroupContents: holding the 3D selection.
     */
    public RemoveObjFrom3DCommand(Set<String> nodesToRemove, SelectionGroup threeDContentGroup, HasFXGroupContents hasFXGroupContents, SelectionGroup threeDSelectionGroup) {
        this.removedNodes = nodesToRemove;
        this.threeDContentGroup = threeDContentGroup;
        this.threeDSelectionGroup = threeDSelectionGroup;

        this.hasFXGroupContents = hasFXGroupContents;   //required to remember the coloring.
    }

    @Override
    public void undo() {
        threeDContentGroup.changeSelection(removedNodes, false, false); // re-draw

        rememberFXMeshViewColors.restoreSelection();    // re-color
        threeDSelectionGroup.changeSelection(removedNodes, false, false);   //also reselect all removed nodes
    }

    @Override
    public void execute() {
        remember();
        //was buggy when these two commands change order
        threeDSelectionGroup.changeSelection(removedNodes, false, true);
        threeDContentGroup.changeSelection(removedNodes, false, true);  // undraw and then unselect

    }

    @Override
    public void redo() {
        execute();
    }

    private void remember() {
        this.rememberFXMeshViewColors = new RememberFXMeshViewColors(removedNodes, hasFXGroupContents, threeDSelectionGroup); //remember coloring
    }

    @Override
    public String name() {
        return "RemoveItemFrom3DCommand";
    }
}
