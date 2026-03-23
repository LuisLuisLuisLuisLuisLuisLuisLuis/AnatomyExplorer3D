package Project.command.Remember;

import Project.AnatomyExplorer;
import Project.command.TreeCommands.SelectAllTreeViewCommand;
import Project.model.ANode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * A group to remember the tree selection.
 */
public class RememberTreeViewSelection {
    private final TreeView<ANode> treeView;
    private final HashSet<TreeItem<ANode>> previousSelection;  // save

    public RememberTreeViewSelection(TreeView<ANode> treeView) {
        this.treeView = treeView;
        this.previousSelection = new HashSet<>(treeView.getSelectionModel().getSelectedItems().size());
        previousSelection.addAll(treeView.getSelectionModel().getSelectedItems());
    }

    public void restoreSelection() {
        if (previousSelection.isEmpty()) return;
        treeView.getSelectionModel().clearSelection();  // clear and restore
        SelectAllTreeViewCommand.bulkSelect(previousSelection, treeView);
    }
}
