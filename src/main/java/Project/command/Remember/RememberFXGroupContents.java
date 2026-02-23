package Project.command.Remember;

import javafx.scene.Group;
import javafx.scene.Node;

import java.util.LinkedList;

/**
 * Remembers the contents of a group and provides method to restore that state.
 */
public class RememberFXGroupContents {
    private Group group;
    private LinkedList<Node> selection;
    public RememberFXGroupContents(Group group) {
        this.group = group;
        this.selection = new LinkedList<>(group.getChildren());
    }


    public void restoreSelection() {
        group.getChildren().clear();
        group.getChildren().addAll(this.selection);
    }
}
