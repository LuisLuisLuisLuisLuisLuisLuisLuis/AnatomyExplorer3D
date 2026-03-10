package Project.SelectionModel.FXGroupDraw;

import Project.SelectionModel.HasSelection;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class HasFXGroupContents implements HasSelection<Node> {

    private final String id;

    @Override
    public String getId() {return id;}

    private final Group group;

    private HasFXGroupContentsContainer hasFXGroupContentsContainer = null;
    public void setHasFXGroupContentsContainer(HasFXGroupContentsContainer hasFXGroupContentsContainer) {this.hasFXGroupContentsContainer = hasFXGroupContentsContainer;}

    public HasFXGroupContents(Group group, String id) {
        this.group = group;
        this.id = id;
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
    public void select(Node mockItem) {
        if (getNodeWithID(mockItem.getId()) != null) {
            return;
        }
        Node item = hasFXGroupContentsContainer != null ? hasFXGroupContentsContainer.createMeshView(mockItem.getId()) : mockItem;
        if (item != null) group.getChildren().add(item);
    }
    @Override
    public void unselect(Node item) {
        if (!group.getChildren().remove(item)) {
            group.getChildren().remove(getNodeWithID(item.getId()));
        } else System.out.println("HasFXGroupContents removed a Mesh without having to use the ID"); //i supsect this is never possible and thus very inefficient
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
