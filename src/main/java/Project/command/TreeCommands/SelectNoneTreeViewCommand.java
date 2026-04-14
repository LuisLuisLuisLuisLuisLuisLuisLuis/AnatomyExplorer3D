package Project.command.TreeCommands;

import Project.command.Remember.RememberTreeViewSelection;
import Project.model.ANode;
import javafx.scene.control.TreeView;

public class SelectNoneTreeViewCommand extends TreeCommand {
    private RememberTreeViewSelection rememberTreeViewSelection;
    public SelectNoneTreeViewCommand(TreeView<ANode> treeView) {
        super(treeView);
    }

    /**
     * Deselects all items of the treeview
     */
    @Override
    public void execute() {
        if (treeView == null || treeView.getRoot() == null) return;
        remember();
        treeView.getSelectionModel().clearSelection();
    }

    @Override
    public void undo() {
        this.rememberTreeViewSelection.restoreSelection();
    }
    @Override
    public void redo() {
        execute();
    }

    @Override
    public String name() {
        return "SelectNoneTreeViewCommand";
    }

    private void remember() {
        this.rememberTreeViewSelection = new RememberTreeViewSelection(treeView);
    }
}
