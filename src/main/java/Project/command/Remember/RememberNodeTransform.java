package Project.command.Remember;

import javafx.scene.Node;
import javafx.scene.transform.Transform;

import java.util.LinkedList;
import java.util.List;

public class RememberNodeTransform {

    private final Node node;
    private final List<Transform> transforms;

    /**
     * Remembers the transforms of the Node upon instantiation of this class and allows to restore them.
     * @param node Node whose transforms should be remembered.
     */
    public RememberNodeTransform(Node node) {
        this.node = node;
        this.transforms = new LinkedList<>(node.getTransforms());
    }

    public void restore() {
        this.node.getTransforms().setAll(transforms);
    }


}
