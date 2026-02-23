package Project.window.SupportingUI.TextSearch;

import Project.model.ANode;
import javafx.scene.control.TreeItem;

/**
 * Used by SearchTree class as element of Undo and Redo Stacks.
 * @param treeItem
 * @param counter
 */
public record FoundText(TreeItem<ANode> treeItem, int counter) {
}
