package Project.window.TreeView.TreeAnalysis.TreeViewEditing.Command;


import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.util.LinkedList;

public class UndoAbleTreeViewEditor<T> {

    /**
     * TODO: NOT TESTED
     */
    public UndoAbleTreeViewEditor() {
        undoStack.addListener((ListChangeListener<? super TreeViewEditorCommand>) il -> {if (undoStack.size() > 400) undoStack.removeFirst();});
        redoStack.addListener((ListChangeListener<? super TreeViewEditorCommand>) il -> {if (redoStack.size() > 400) redoStack.removeFirst();});
    }

    private final ObservableList<TreeViewEditorCommand> undoStack = FXCollections.observableList(new LinkedList<TreeViewEditorCommand>());
    private final ObservableList<TreeViewEditorCommand> redoStack = FXCollections.observableList(new LinkedList<TreeViewEditorCommand>());

    public BooleanBinding canUndo() {return Bindings.isEmpty(undoStack).not();}
    public BooleanBinding canRedo() {return Bindings.isEmpty(redoStack).not();}

    public boolean undo() {
        if (!canUndo().get()) return false;
        TreeViewEditorCommand treeViewEditorCommand = undoStack.removeLast();
        if (treeViewEditorCommand.undo()) {
            redoStack.add(treeViewEditorCommand);
            return true;
        } return false;
    }

    public boolean redo() {
        if (!canRedo().get()) return false;
        TreeViewEditorCommand treeViewEditorCommand = redoStack.removeLast();
        if (treeViewEditorCommand.redo()) {
            undoStack.add(treeViewEditorCommand);
            return true;
        }return false;
    }

    private boolean execute(TreeViewEditorCommand treeViewEditorCommand) {
        treeViewEditorCommand.execute();
        return undoStack.add(treeViewEditorCommand);
    }

    private final TreeItem<T> clipboardHolder = new TreeItem<>();

    public TreeItem<T> clipBoardContent() {return clipboardHolder.getChildren().getFirst();}

    public BooleanBinding canPaste() {
        return Bindings.isEmpty(clipboardHolder.getChildren()).not();
    }
    public BooleanBinding canCut() {return canPaste().not();}

    public boolean cut(TreeItem<T> mover) {
        if (!canCut().get()) return false;
        return execute(new CutCommand(mover));
    }


    //basically cut without saving anywhere
    public boolean delete(TreeItem<T> toDelete) {
        if (toDelete.getParent() == null) return false;
        return execute(new DeleteCommand(toDelete));
    }

    public boolean paste(TreeItem<T> targetParent) {
        if (!canPaste().get() || this.clipboardHolder.getChildren().getFirst() == targetParent) return false;
        else return execute(new PasteCommand(targetParent));
    }


    public static <T> boolean moveTreeItem(TreeItem<T> mover, TreeItem<T> targetParent) {
        if (mover.getParent() != null) mover.getParent().getChildren().remove(mover);
        return targetParent.getChildren().add(mover);
    }

    private interface TreeViewEditorCommand {
        boolean execute();
        boolean undo();
        boolean redo();
    }

    private class DeleteCommand implements TreeViewEditorCommand {
        private final TreeItem<T> treeItem;
        private TreeItem<T> parent;

        public DeleteCommand(TreeItem<T> treeItem) {
            this.treeItem = treeItem;
        }
        public boolean execute() {
            remember();
            return treeItem.getParent().getChildren().remove(treeItem);
        }
        public boolean undo() {
            return this.parent.getChildren().add(treeItem);
        }
        private void remember() {
            this.parent = treeItem.getParent();
        }
        public boolean redo() {return execute();}
    }

    private class PasteCommand implements TreeViewEditorCommand{
        private final TreeItem<T> targetParent;
        private TreeItem<T> mover;

        public PasteCommand(TreeItem<T> targetParent) {
            this.targetParent = targetParent;
        }

        private void remember() {
            this.mover = clipboardHolder.getChildren().getFirst();
        }

        public boolean execute() {
            remember();
            if (moveTreeItem(clipboardHolder.getChildren().getFirst(), targetParent)) {
                targetParent.setExpanded(true);
                return true;
            } else return false;
        }

        public boolean undo() {
            return cut(mover);
        }
        public boolean redo() {return execute();}
    }


    private class CutCommand implements TreeViewEditorCommand{
        private TreeItem<T> parent;
        protected TreeItem<T> mover;;

        public CutCommand(TreeItem<T> mover) {
            this.mover = mover;
        }
        private void remember() {
            this.parent = mover.getParent();
        }

        public boolean execute() {
            remember();
            clipboardHolder.getChildren().clear();
            moveTreeItem(mover, clipboardHolder);
            return true;
        }

        public boolean undo() {
            if (this.parent == null) return false;
            else return paste(this.parent);
        }
        public boolean redo() {return execute();}
    }
}

