package Project.command.TreeCommands;

import Project.command.Remember.RememberTreeViewSelection;
import Project.model.ANode;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.LinkedList;
import java.util.List;

public class CollapseTreeViewCommand extends TreeCommand {
    private List<TreeItem<ANode>> previouslyExpanded = new LinkedList<>();
    private RememberTreeViewSelection rememberTreeViewSelection;
    public CollapseTreeViewCommand(TreeView<ANode> treeView) {
        super(treeView);
    }

    /**
     * Collapses the TreeView by closing all nodes either completely or based on the selected nodes.
     * If no nodes are selected in the TreeView, all nodes starting from the root are collapsed.
     * Otherwise, only the subtrees of the selected nodes are collapsed.
     */
    @Override
    public void execute() {
        remember(); // remember which items are expanded and/or selected right now

        ObservableList<TreeItem<ANode>> selectedNodes = FXCollections.observableList(new LinkedList<>());
        selectedNodes.addAll(treeView.getSelectionModel().getSelectedItems());

        if (selectedNodes.isEmpty()) {
            collapseAllNodesUptToGivenNode(treeView.getRoot());
        } else {
            // selectedNodes can be emptied mid-loop and cause an out-of-bounds exception. Could be avoided by creating a copy of the selection first. (which i did above so should be fixed, fingerscrosed)
            for (TreeItem<ANode> selectedNode : selectedNodes) {
                collapseAllNodesUptToGivenNode(selectedNode);
            }
        }

        // Clear selection.
        treeView.getSelectionModel().clearSelection();
    }

    @Override
    public void undo() {
        // first collapse all, then expand only those which were expanded before the command was called. (manually expanding individual items is not undoable and thus not considered by this class)
        collapseAllNodesUptToGivenNode(treeView.getRoot());
        for (TreeItem<ANode> treeItem : previouslyExpanded) treeItem.setExpanded(true);
        rememberTreeViewSelection.restoreSelection();
    }

    public void redo() {
        rememberTreeViewSelection.restoreSelection();
        execute();
    }

    @Override
    public String name() {
        return "CollapseTreeViewCommand";
    }

    /**
     * Remembers which items are expanded and / or selected right now.
     */
    private void remember() {
        this.rememberTreeViewSelection = new RememberTreeViewSelection(treeView);   // remember which treeItems are selected. (this is influenced by expanded state)
        previouslyExpanded.clear();
        rememberExpanded(treeView.getRoot());
    }

    private void rememberExpanded(TreeItem<ANode> treeItem) {
        if (treeItem.isExpanded()) previouslyExpanded.add(treeItem);
        for (TreeItem<ANode> child : treeItem.getChildren()) rememberExpanded(child);
    }


    /**
     * Helper function to recursively collapse all nodes below the input node
     * @param item from which all nodes below get collapsed
     */
    private void collapseAllNodesUptToGivenNode(TreeItem<ANode> item) {
        if (item != null) {
            item.setExpanded(false);
            for (TreeItem<ANode> child : item.getChildren()) {
                collapseAllNodesUptToGivenNode(child);
            }
        }
    }
}
