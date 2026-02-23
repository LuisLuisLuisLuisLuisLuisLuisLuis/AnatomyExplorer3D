package Project.command.DrawCommands.DrawAnatomyCommands;

import Project.SelectionModel.SelectionGroup;
import Project.command.Command;

import java.util.Set;

/**
 * Command for drawing items.
 */
public class DrawItemIn3DCommand implements Command {

    private SelectionGroup<String> threeDContentGroup;
    private SelectionGroup<String> threeDSelectionGroup;
    private Set<String> itemsToDraw;

    /**
     * @param itemsToDraw: List of fileIDs
     * @param threeDContentGroup: SelectionGroup controlling which items are drawn.
     * @param threeDSelectionGroup: SelectionGroup holding the 3D selection.
     */
    public DrawItemIn3DCommand(Set<String> itemsToDraw, SelectionGroup<String> threeDContentGroup, SelectionGroup threeDSelectionGroup) {
        this.threeDContentGroup = threeDContentGroup;
        this.threeDSelectionGroup = threeDSelectionGroup;
        this.itemsToDraw = itemsToDraw;
    }

    @Override
    public void undo() {
        //was buggy when these two commands change order
        threeDSelectionGroup.changeSelection(itemsToDraw, false, true); //unselect them
        threeDContentGroup.changeSelection(itemsToDraw, false, true);   //remove the items again

    }

    @Override
    public void execute() {
        remember();
        threeDContentGroup.changeSelection(itemsToDraw, false, false);  //add the items
    }

    @Override
    public void redo() {
        execute();
    }

    private void remember() {
    }

    @Override
    public String name() {
        return "DrawItemIn3DCommand";
    }
}
