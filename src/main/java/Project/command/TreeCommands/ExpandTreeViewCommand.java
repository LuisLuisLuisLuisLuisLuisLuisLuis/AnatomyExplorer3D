package Project.command.TreeCommands;

import Project.command.Remember.RememberTreeViewSelection;
import Project.model.ANode;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.LinkedList;
import java.util.List;

public class ExpandTreeViewCommand extends TreeCommand {

    private List<TreeItem<ANode>> previouslyCollapsed = new LinkedList<>();
    private RememberTreeViewSelection rememberTreeViewSelection;

    public ExpandTreeViewCommand(TreeView<ANode> treeView) {
        super(treeView);
    }

    /**
     * Expands the TreeView by opening all nodes either completely or based on the selected nodes.
     * If no nodes are selected in the TreeView, all nodes starting from the root are expanded.
     * Otherwise, only the subtrees of the selected nodes are expanded.
     */
    @Override
    public void execute() {
        remember();

        ObservableList<TreeItem<ANode>> selectedNodes = treeView.getSelectionModel().getSelectedItems();    //remember selection

        if (selectedNodes.isEmpty()) {
            expandAllBelowGivenNode(treeView.getRoot());
        } else {
            for (TreeItem<ANode> selectedNode : selectedNodes) {
                expandAllBelowGivenNode(selectedNode);
            }
        }

        // Clear selection
        treeView.getSelectionModel().clearSelection();
    }

    @Override
    public void undo() {
        expandAllBelowGivenNode(treeView.getRoot());
        for (TreeItem<ANode> treeItem : previouslyCollapsed) treeItem.setExpanded(false);
        rememberTreeViewSelection.restoreSelection();
    }

    public void redo() {
        rememberTreeViewSelection.restoreSelection();
        execute();
    }

    @Override
    public String name() {
        return "ExpandTreeViewCommand";
    }


    /**
     * Remembers which items are expanded and / or selected right now.
     */
    private void remember() {
        this.rememberTreeViewSelection = new RememberTreeViewSelection(treeView);
        previouslyCollapsed.clear();
        rememberExpanded(treeView.getRoot());
    }

    private void rememberExpanded(TreeItem<ANode> treeItem) {
        if (!treeItem.isExpanded()) previouslyCollapsed.add(treeItem);
        for (TreeItem<ANode> child : treeItem.getChildren()) rememberExpanded(child);
    }

    /**
     * Expands the given TreeItem along with all of its child items recursively.
     * If the provided TreeItem is null, the method execution is skipped.
     *
     * @param item the TreeItem to be expanded, along with all its descendants
     */
    public static void expandAllBelowGivenNode(TreeItem<ANode> item) {
        if (item != null) {
            item.setExpanded(true);
            for (TreeItem<ANode> child : item.getChildren()) {
                expandAllBelowGivenNode(child);
            }
        }
    }
}
