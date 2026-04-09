package Project.command.DrawCommands.DrawAnatomyCommands;

import Project.SelectionModel.FXGroupDraw.HasFXGroupContents;
import Project.SelectionModel.FXGroupDraw.HasFXGroupContentsContainer;
import Project.SelectionModel.FXGroupSelection.FXGroupSelectionContainer;
import Project.SelectionModel.FXGroupSelection.HasFXGroupSelection;
import Project.SelectionModel.SelectionGroup;
import Project.command.Command;
import javafx.scene.shape.MeshView;

import java.util.Set;

/**
 * Command for drawing items.
 */
public class DrawItemIn3DCommand implements Command {

    private SelectionGroup<String> threeDContentGroup;
    private SelectionGroup<String> threeDSelectionGroup;
    private Set<String> itemsToDraw;

    private Set<MeshView> meshViewsToDraw;
    private HasFXGroupContents hasFXGroupContents;
    private HasFXGroupSelection hasFXGroupSelection;

    /**
     * @param itemsToDraw: List of fileIDs
     * @param threeDContentGroup: SelectionGroup controlling which items are drawn.
     * @param threeDSelectionGroup: SelectionGroup holding the 3D selection.
     */
    public DrawItemIn3DCommand(Set<String> itemsToDraw, SelectionGroup<String> threeDContentGroup, SelectionGroup threeDSelectionGroup) {
        this.threeDContentGroup = threeDContentGroup;
        this.threeDSelectionGroup = threeDSelectionGroup;
        this.itemsToDraw = itemsToDraw;
    }   // i dont use the old constructor anymore because i think the command should also work without selection group.


    public DrawItemIn3DCommand(Set<MeshView> itemsToDraw, HasFXGroupContents hasFXGroupContents, HasFXGroupSelection hasFXGroupSelection) {
        this.hasFXGroupContents = hasFXGroupContents;
        this.hasFXGroupSelection = hasFXGroupSelection;
        this.meshViewsToDraw = itemsToDraw;
    }

    @Override
    public void undo() {
        //was buggy when these two commands change order
//        threeDSelectionGroup.changeSelection(itemsToDraw, false, true); //unselect them
//        threeDContentGroup.changeSelection(itemsToDraw, false, true);   //remove the items again
        for (MeshView meshView: meshViewsToDraw) {
            hasFXGroupContents.unselect(meshView);
            hasFXGroupSelection.unselect(meshView);
        }
    }

    @Override
    public void execute() {
//        hasFXGroupContentsContainer.changeSelection(itemsToDraw, false, false);
//        threeDContentGroup.changeSelection(itemsToDraw, false, false);  //add the items
        for (MeshView meshView: meshViewsToDraw) {
            hasFXGroupContents.select(meshView);
        }
    }

    @Override
    public void redo() {
        execute();
    }


    @Override
    public String name() {
        return "DrawItemIn3DCommand";
    }
}
