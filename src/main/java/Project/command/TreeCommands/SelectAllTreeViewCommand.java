package Project.command.TreeCommands;

import Project.command.Remember.RememberTreeViewSelection;
import Project.model.ANode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class SelectAllTreeViewCommand extends TreeCommand {
    private RememberTreeViewSelection rememberTreeViewSelection;
    private List<TreeItem<ANode>> previouslyCollapsed = new LinkedList<>();
    public SelectAllTreeViewCommand(TreeView<ANode> treeView) {
        super(treeView);
    }
    private HashSet<TreeItem<ANode>> itemsToSelect = new HashSet<>();   // using a Set so that the same item is not added twice
                                                                        // and thus also not selected twice. HUGE time saving.

    /**
     * select all children of selected nodes or all nodes in the tree if none are selected.
     */

    @Override
    public void execute() {
        remember();

        if (treeView.getSelectionModel().getSelectedItems().isEmpty()) {
            collectAllRecursive(treeView.getRoot());
        } else {
            for (TreeItem<ANode> treeItem : treeView.getSelectionModel().getSelectedItems()) {
                collectAllRecursive(treeItem);
            }
        }
        bulkSelect(this.itemsToSelect, this.treeView);
    }


    @Override
    public void undo() {
        rememberTreeViewSelection.restoreSelection();
        for (TreeItem<ANode> treeItem : previouslyCollapsed) treeItem.setExpanded(false);
    }
    @Override
    public void redo() {
        rememberTreeViewSelection.restoreSelection();
        execute();
    }

    @Override
    public String name() {
        return "SelectAllTreeViewCommand";
    }

    private void remember() {
        previouslyCollapsed.clear();
        rememberExpanded(treeView.getRoot());
        this.rememberTreeViewSelection = new RememberTreeViewSelection(treeView);
    }

    private void rememberExpanded(TreeItem<ANode> treeItem) {
        if (!treeItem.isExpanded()) previouslyCollapsed.add(treeItem);
        for (TreeItem<ANode> child : treeItem.getChildren()) rememberExpanded(child);
    }


    /**
     * Recursively adds the subtree of item to itemsToSelect.
     */
    private void collectAllRecursive(TreeItem<ANode> item) {
        itemsToSelect.add(item);
        for (TreeItem<ANode> child : item.getChildren()) {
            collectAllRecursive(child);
        }
    }

    public static void bulkSelect(HashSet<TreeItem<ANode>> itemsToSelect, TreeView<ANode> treeView) {
        if (itemsToSelect.isEmpty()) return;
        for (TreeItem<ANode> treeItem : itemsToSelect) treeItem.setExpanded(true);
        if (itemsToSelect.size() == 1) {
            treeView.getSelectionModel().select(treeView.getRow(itemsToSelect.iterator().next()));
            return;
        }
        int[] indicesToSelect = new int[itemsToSelect.size()];
        int i = 0;
        for (TreeItem<ANode> treeItem : itemsToSelect) {
            indicesToSelect[i] = treeView.getRow(treeItem);
            i++;
        }
        treeView.getSelectionModel().selectIndices(indicesToSelect[0], indicesToSelect);
    }

}
