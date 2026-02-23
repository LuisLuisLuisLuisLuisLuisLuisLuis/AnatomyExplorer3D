package Project.window.TreeView.TreeAnalysis.TreeViewEditing;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.TreeItem;

import java.util.LinkedList;

public class TreeViewEditor<T> {

    private TreeItem<T> trashParent;                    //when a treeItem is deleted, this becomes its former parent

    private TreeItem<T> trashCan = new TreeItem<>();    //when deleted, a treeitem becomes the only child of this

    public BooleanBinding canRestore() {return Bindings.isEmpty(trashCan.getChildren()).not();}

    private TreeItem<T> clipboardHolder = new TreeItem<>();

    public TreeItem<T> clipBoardContent() {return clipboardHolder.getChildren().getFirst();}

    public BooleanBinding canPaste() {
        return Bindings.isEmpty(clipboardHolder.getChildren()).not();
    }
    public BooleanBinding canCut() {return canPaste().not();}

    /**
     * Makes all children of parent siblings of parent. Does not work if parent has no parent.
     * @param parent Non-root treeItem with children.
     * @return False if parent is root, else true.
     */
    public boolean extractChildren(TreeItem<T> parent) {
        if (parent.getParent() == null) return false;
        for (TreeItem<T> child : new LinkedList<>(parent.getChildren())) moveTreeItem(child, parent.getParent());
        return true;
    }

    public boolean cut(TreeItem<T> mover) {
        if (!canCut().get()) return false;
        clipboardHolder.getChildren().clear();
        moveTreeItem(mover, clipboardHolder);
        return true;
    }

    public boolean delete(TreeItem<T> toDelete) {
        if (toDelete.getParent() == null) return false;
        trashCan.getChildren().clear();
        this.trashParent = toDelete.getParent();
        toDelete.getParent().getChildren().remove(toDelete);
        trashCan.getChildren().add(toDelete);
        System.out.println("trashparent: " + trashParent.getValue().toString());
        return true;
    }

    //undo the last delete action if possible. only the last delete action can be undone. deleting twice makes the first delete unrestorable.
    public TreeItem<T> restore() {
        if (!canRestore().get()) return null;
        TreeItem<T> result = trashCan.getChildren().getFirst();
        trashCan.getChildren().clear();
        this.trashParent.getChildren().add(result);
        return trashParent.getChildren().getLast();
    }

    public boolean paste(TreeItem<T> targetParent) {
        if (!canPaste().get() || this.clipboardHolder.getChildren().getFirst() == targetParent) return false;
        else {
            if (moveTreeItem(clipboardHolder.getChildren().getFirst(), targetParent)) {
                targetParent.setExpanded(true);
                return true;
            } else return false;
        }
    }

    public TreeItem<T> create(TreeItem<T> parent, T newNodeValue) {
        TreeItem<T> newTreeItem = new TreeItem<>(newNodeValue);
        parent.getChildren().add(newTreeItem);
        parent.setExpanded(true);
        return newTreeItem;
    }

    public static <T> boolean moveTreeItem(TreeItem<T> mover, TreeItem<T> targetParent) {
        if (mover.getParent() != null) mover.getParent().getChildren().remove(mover);
        return targetParent.getChildren().add(mover);
    }
}
