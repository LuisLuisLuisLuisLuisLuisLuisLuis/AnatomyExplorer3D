package Project.command.Remember;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.LinkedList;
import java.util.List;

/**
 * A group to remember the tree selection.
 */
public class RememberTreeViewSelection {
    private TreeView treeView;
    private List<TreeItem> previousSelection = new LinkedList<>();  // save

    public RememberTreeViewSelection(TreeView treeView) {
        previousSelection.addAll(treeView.getSelectionModel().getSelectedItems());
        this.treeView = treeView;
    }

    public void restoreSelection() {
        treeView.getSelectionModel().clearSelection();  // clear and restore
        for (TreeItem treeItem : previousSelection) {
            treeView.getSelectionModel().select(treeItem);
        }
    }
    public List<TreeItem> getPreviousSelection() {
        return previousSelection;
    }
}
