package Project.SelectionModel.FXGroupSelection;

import Project.SelectionModel.HasSelection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.scene.Group;
import javafx.scene.Node;

import java.util.LinkedList;
import java.util.List;

/**
 * This class has a Group and maintains a selection of the group's contents (-> what Nodes of the group are selected).
 */
public class HasFXGroupSelection implements HasSelection<Node> {

    /**
     * The group.
     */
    private final Group group;

    /**
     * The selection.
     */
    private final ObservableList<Node> selection = FXCollections.observableArrayList();

    /**
     * Initialize by giving a group.
     * @param group
     */
    public HasFXGroupSelection(Group group) {
        this.group = group;
    }


    @Override
    public ObservableList<Node> getSelection() {
        return selection;
    }

    @Override
    public void setSelection(ObservableList<Node> selection) {
        clearSelection();
        this.selection.addAll(selection);
    }

    @Override
    public void select(Node item) {
        this.selection.add(item);
    }

    public void unselect(Node item) {
        this.selection.remove(item);
    }

    public void selectAll() {
        clearSelection();
        for (Node item : getAllItems()) {
            select(item);
        }
    }


    @Override
    public void clearSelection() {
        this.selection.clear();
    }

    @Override
    public List<Node> getAllItems() {
        return new LinkedList<Node>(group.getChildren());
    }

    public ObservableList<Node> getAllItemsObservable() {
        return group.getChildren();
    }
}
