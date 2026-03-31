package Project.model;

import javafx.scene.control.TreeItem;

public class EnableDisableBP3DV3Parts {



    /**
     * Takes a mapping of node id to fileID and adds those fileIDs to the matching ANodes in the tree.
     * @param root root of tree
     */
    public static void addV3FilesToTree(TreeItem<ANode> root) {
        if (root.getValue().name().endsWith("BP3D 3.0)")) {
            root.getValue().fileIds().add(root.getValue().conceptId());
        }
        for (TreeItem<ANode> child : root.getChildren()) addV3FilesToTree(child);
    }

    /**
     * Takes a mapping of node id to fileID and removes those fileIDs from the matching ANodes in the tree.
     * @param root root of tree
     */
    public static void removeV3FilesFromTree(TreeItem<ANode> root) {
        if (root.getValue().name().endsWith("BP3D 3.0)")) {
            root.getValue().fileIds().remove(root.getValue().conceptId());
        }
        for (TreeItem<ANode> child : root.getChildren()) removeV3FilesFromTree(child);
    }

}
