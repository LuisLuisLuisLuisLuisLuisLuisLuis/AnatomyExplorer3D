package Project.SelectionModel.Tree;

import Project.SelectionModel.HasSelection;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;


/**
 * Wrapper class for a TreeView with the purpose of enabling access to its selection by SelectionContainer.
 * @param <T>: value type of TreeItems of the TreeView
 */
public class HasTreeView<T> implements HasSelection<TreeItem<T>> {

    /**
     * The treeview.
     */
    private final TreeView<T> treeView;

    private final String id;

    /**
     * @return The ID of the treeview, i.e. treeView.getId().
     */
    @Override
    public String getId() {return id;}

    /**
     * Initialize by giving a treeview.
     */
    public HasTreeView(TreeView<T> treeView) {
        this.treeView = treeView;
        this.id = treeView.getId();
    }

    /**
     * @return the selectedItems()
     */
    @Override
    public ObservableList<TreeItem<T>> getSelection() {
        return treeView.getSelectionModel().getSelectedItems();
    }

    /**
     * @param selection: set the selection of the treeView by giving the full list of selected items.
     */
    @Override
    public void setSelection(List<TreeItem<T>> selection) {
        treeView.getSelectionModel().getSelectedItems().clear();
        treeView.getSelectionModel().getSelectedItems().addAll(selection);
    }

    /**
     * @param item: select a TreeItem (add to selection)
     */
    @Override
    public void select(TreeItem<T> item) {
        System.out.println(getId() + " HasTreeView selecting " + item.toString() + " " + (item.getParent() != null ? item.getParent().toString() : ""));
        treeView.getSelectionModel().select(item);
        item.setExpanded(true);
        System.out.println(treeView.getRow(item));
    }

    @Override
    public void unselect(TreeItem<T> item) {
        int index = treeView.getRow(item);
        if (index >= 0) treeView.getSelectionModel().clearSelection(index);
    }

    public void selectAll() {
        treeView.getSelectionModel().selectAll();
    }

    /**
     * Unselects everything.
     */
    @Override
    public void clearSelection() {
        treeView.getSelectionModel().clearSelection();
    }

    /**
     * @return the root of the tree.
     */
    public TreeItem<T> getRoot() {
        return treeView.getRoot();
    }

    /**
     * @return all TreeItems of this treeView.
     */
    @Override
    public List<TreeItem<T>> getAllItems() {
        return collectItemsRec(treeView.getRoot());
    }

    /**
     * Recursive helper to collect all nodes in the tree below 'node'.
     * @param node
     * @return all nodes below node as List.
     */
    private List<TreeItem<T>> collectItemsRec(TreeItem<T> node) {
        List<TreeItem<T>> nodesBelow = new LinkedList<>();
        for (TreeItem<T> child : node.getChildren()) {
            nodesBelow.addAll(collectItemsRec(child));
        }
        nodesBelow.add(node);
        return nodesBelow;
    }
}
