package Project.SelectionModel.FXGroupDraw;

import Project.SelectionModel.HasSelection;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;

import java.util.List;
import java.util.Objects;

public class HasFXGroupContents implements HasSelection<Node> {

    private final Group group;

    public HasFXGroupContents(Group group) {
        this.group = group;
    }

    @Override
    public ObservableList<Node> getSelection() {
        return group.getChildren();
    }

    @Override
    public void setSelection(ObservableList<Node> selection) {
        group.getChildren().clear();
        group.getChildren().addAll(selection);
    }

    @Override
    public void select(Node item) {
        if (getNodeWithID(item.getId()) != null) {
            return;
        }
        group.getChildren().add(item);
    }
    @Override
    public void unselect(Node item) {
        if (!group.getChildren().remove(item)) {
            group.getChildren().remove(getNodeWithID(item.getId()));
        }
    }


    public Node getNodeWithID(String id) {
        for (Node node : group.getChildren()) {
            if (Objects.equals(node.getId(), id)) {
                return node;
            }
        }
        return null;
    }

    @Override
    public void clearSelection() {
        group.getChildren().clear();
    }

    @Override
    public List<Node> getAllItems() {
        return group.getChildren();
    }
}
