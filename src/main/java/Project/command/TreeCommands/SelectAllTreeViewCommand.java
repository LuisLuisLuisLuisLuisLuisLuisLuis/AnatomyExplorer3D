package Project.command.TreeCommands;

import Project.command.Remember.RememberTreeViewSelection;
import Project.command.TreeCommand;
import Project.model.ANode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.LinkedList;
import java.util.List;

public class SelectAllTreeViewCommand extends TreeCommand {
    private RememberTreeViewSelection rememberTreeViewSelection;
    private List<TreeItem<ANode>> previouslyCollapsed = new LinkedList<>();
    public SelectAllTreeViewCommand(TreeView<ANode> treeView) {
        super(treeView);
    }
    private LinkedList<TreeItem<ANode>> itemsToSelect = new LinkedList<>();

    /**
     * select all children of selected nodes or all nodes in the tree if none are selected.
     */

    @Override
    public void execute() {
        remember();

        if (treeView.getSelectionModel().getSelectedItems().isEmpty()) {
            selectAllRecursive(treeView.getRoot());
        } else {
            for (TreeItem<ANode> treeItem : treeView.getSelectionModel().getSelectedItems()) {
                selectAllRecursive(treeItem);
            }
        }
        bulkSelect();
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
     * Recursively selects a given TreeItem and all its children in the provided selection model.
     *
     * @param item the TreeItem to start the selection process from, including its sub-items
     */
    private void selectAllRecursive(TreeItem<ANode> item) {
        //item.setExpanded(true);
        itemsToSelect.add(item);
        for (TreeItem<ANode> child : item.getChildren()) {
            selectAllRecursive(child);
        }
    }
    private void bulkSelect() {
        for (TreeItem<ANode> treeItem : itemsToSelect) treeItem.setExpanded(true);
        for (TreeItem<ANode> treeItem : itemsToSelect) treeView.getSelectionModel().select(treeItem);
    }

}
