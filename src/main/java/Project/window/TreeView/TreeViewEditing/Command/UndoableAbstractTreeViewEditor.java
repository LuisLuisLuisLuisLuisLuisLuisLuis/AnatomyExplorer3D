package Project.window.TreeView.TreeViewEditing.Command;


import Project.command.Command;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;


import java.util.ArrayList;
import java.util.LinkedList;


/**
 * Wanted to make this abstract but then I realized that this class cannot handle a situation where the treeview
 * is completely removed and replaced because then all treeItems, even if they correspond
 * to the same concept, will be new instances. still not deleting this class in case it comes in handy at some point.
 */
public abstract class UndoableAbstractTreeViewEditor<T> {

    /**
     * This is called after a TreeItem has been inserted.
     * Updates the values of the TreeItems if necessary.
     */
    abstract void updateValuesOnInsert(TreeItem<T> inserted);

    /**
     * This is called after a TreeItem has been removed.
     * Updates the values of the TreeItems if necessary.
     */
    abstract void updateValuesOnRemove(TreeItem<T> removed, TreeItem<T> parentOfRemoved);


    private final ObservableList<Command> undoStack = FXCollections.observableList(new LinkedList<Command>());
    private final ObservableList<Command> redoStack = FXCollections.observableList(new LinkedList<Command>());

    public BooleanBinding canUndo() {return Bindings.isEmpty(undoStack).not();}
    public BooleanBinding canRedo() {return Bindings.isEmpty(redoStack).not();}


    public void undo() {
        if (!canUndo().get()) return;
        Command treeViewEditorCommand = undoStack.removeLast();
        treeViewEditorCommand.undo();
        redoStack.add(treeViewEditorCommand);
    }

    public void redo() {
        if (!canRedo().get()) return;
        Command treeViewEditorCommand = redoStack.removeLast();
        treeViewEditorCommand.redo();
        undoStack.add(treeViewEditorCommand);
    }

    void execute(Command treeViewEditorCommand) {
        treeViewEditorCommand.execute();
        undoStack.add(treeViewEditorCommand);
    }

    private final TreeItem<T> clipboardHolder = new TreeItem<>();

    public TreeItem<T> clipBoardContent() {return clipboardHolder.getChildren().getFirst();}

    public BooleanBinding canPaste() {
        return Bindings.isEmpty(clipboardHolder.getChildren()).not();
    }
    public BooleanBinding canCut() {return canPaste().not();}

    public void cut(TreeItem<T> mover) {
        if (!canCut().get()) return;
        execute(new CutTreeItemCommand(mover));
    }

    
    public void delete(TreeItem<T> toDelete) {
        if (toDelete.getParent() == null) return;
        execute(new DeleteTreeItemCommand(toDelete));
    }

    public void paste(TreeItem<T> targetParent) {
        if (!canPaste().get() || this.clipboardHolder.getChildren().getFirst() == targetParent) return;
        else execute(new PasteTreeItemCommand(targetParent));
    }

    public TreeItem<T> create(TreeItem<T> parent, T newNodeValue) {
        execute(new CreateTreeItemCommand(parent, newNodeValue));
        return parent.getChildren().getLast();
    }

    /**
     * Makes all children of treeItem siblings of treeItem.
     * @throws IllegalArgumentException if the treeItem is a root.
     */
    public void extractChildren(TreeItem<T> parent) throws IllegalArgumentException {
        execute(new ExtractChildrenCommand(parent));
    }

    /**
     * Does NOT treeView.setRoot(null)
     */
    public static <T> boolean moveTreeItem(TreeItem<T> mover, TreeItem<T> targetParent) {
        if (mover.getParent() != null) mover.getParent().getChildren().remove(mover);
        return targetParent.getChildren().add(mover);
    }

    private class CreateTreeItemCommand implements Command {
        
        private final TreeItem<T> parent;
        private final TreeItem<T> newNode;
        
        public CreateTreeItemCommand(TreeItem<T> parent, T value) {
            this.parent = parent;
            this.newNode = new TreeItem<>(value);
        }

        @Override
        public void execute() {
            parent.getChildren().add(newNode);
            parent.setExpanded(true);
            updateValuesOnInsert(newNode);
        }

        @Override
        public void undo() {
            this.parent.getChildren().remove(newNode);
            updateValuesOnRemove(newNode, parent);
        }

        @Override
        public void redo() {
            execute();
        }

        @Override
        public String name() {
            return "CreateTreeItemCommand";
        }
    }

    private class DeleteTreeItemCommand implements Command {
        private final TreeItem<T> treeItem;
        private final TreeItem<T> parent;

        public DeleteTreeItemCommand(TreeItem<T> treeItem) {
            if (treeItem.getParent() == null) throw new IllegalArgumentException("TreeItem must have a parent.");
            this.treeItem = treeItem;
            this.parent = treeItem.getParent();
        }

        @Override
        public void execute() {
            if (treeItem.getParent() != null) {
                treeItem.getParent().getChildren().remove(treeItem);
                updateValuesOnRemove(treeItem, parent);
            }
            else {
                updateValuesOnRemove(treeItem, null);
            }
        }

        @Override
        public void undo() {
            this.parent.getChildren().add(treeItem);
            updateValuesOnInsert(treeItem);
        }
        @Override
        public void redo() {execute();}

        @Override
        public String name() {
            return "DeleteTreeItemCommand";
        }
    }

    private class PasteTreeItemCommand implements Command{
        private final TreeItem<T> targetParent;
        private final TreeItem<T> mover;

        public PasteTreeItemCommand(TreeItem<T> targetParent) {
            this.targetParent = targetParent;
            this.mover = clipboardHolder.getChildren().getFirst();
        }
        @Override
        public void execute() {
            if (moveTreeItem(clipboardHolder.getChildren().getFirst(), targetParent)) {
                updateValuesOnInsert(mover);
                targetParent.setExpanded(true);
            }
        }
        @Override
        public void undo() {
            moveTreeItem(mover, clipboardHolder);
            updateValuesOnRemove(mover, targetParent);
        }@Override
        public void redo() {execute();}

        @Override
        public String name() {return "PasteTreeItemCommand";}
    }


    private class CutTreeItemCommand implements Command{
        private final TreeItem<T> parent;
        private final TreeItem<T> mover;

        /**
         * The parent of mover must not be null, otherwise an exception will be thrown.
         */
        public CutTreeItemCommand(TreeItem<T> mover) throws IllegalArgumentException{
            if (mover.getParent() == null) {
                throw new IllegalArgumentException("TreeItem to cut must have a parent.");
            }
            this.mover = mover;
            this.parent = mover.getParent() != null ? mover.getParent() : null;
        }


        @Override
        public void execute() {
            clipboardHolder.getChildren().clear();
            moveTreeItem(mover, clipboardHolder);
            updateValuesOnRemove(mover, parent);
        }
        @Override
        public void undo() {
            moveTreeItem(mover, parent);
            updateValuesOnInsert(mover);
        }@Override
        public void redo() {execute();}

        @Override
        public String name() {return "CutTreeItemCommand";}
    }

    private class ExtractChildrenCommand implements Command {

        private final TreeItem<T> treeItem;
        private final ArrayList<TreeItem<T>> children;

        /**
         * Makes all children of treeItem siblings of treeItem.
         * @throws IllegalArgumentException if the treeItem is a root.
         */
        public ExtractChildrenCommand(TreeItem<T> treeItem) {
            if (treeItem.getParent() == null) throw new IllegalArgumentException("Cannot extract children from root");
            this.treeItem = treeItem;
            this.children = new ArrayList<>(treeItem.getChildren().size());
        }

        @Override
        public void execute() {
            for (TreeItem<T> child : treeItem.getChildren()) {
                treeItem.getChildren().remove(child);
                updateValuesOnRemove(child, treeItem);
                treeItem.getParent().getChildren().add(child);
                updateValuesOnInsert(child);
                this.children.add(child);
            }
        }

        @Override
        public void undo() {
            for (TreeItem<T> child : this.children) {
                TreeItem<T> parent = child.getParent();
                parent.getChildren().remove(child);
                updateValuesOnRemove(child, parent);
                treeItem.getChildren().add(child);
                updateValuesOnInsert(child);
            }
        }

        @Override
        public void redo() {
            execute();
        }
        @Override
        public String name() {return "ExtractChildrenCommand";}
    }
}

