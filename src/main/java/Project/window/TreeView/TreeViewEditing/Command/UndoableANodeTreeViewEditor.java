package Project.window.TreeView.TreeViewEditing.Command;


import Project.command.Command;
import Project.model.ANode;
import Project.window.SupportingUI.PopUp.LittlePopUp;
import Project.window.TreeView.TreeAnalysis.TreeAnalysisUtils;
import Project.window.TreeView.TreeRestructuring.TreeRestructuring;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UndoableANodeTreeViewEditor {
    private static final Logger logger = Logger.getLogger(UndoableANodeTreeViewEditor.class.getName());

    /**
     *  @param runnable A runnable to be run on every execute/undo/redo.
     */
    public void addOnEvent(Runnable runnable) {onEventRunnables.add(runnable);}
    private final HashSet<Runnable> onEventRunnables = new HashSet<>();
    private void runOnEvent() {for (Runnable runnable : onEventRunnables) runnable.run();}

    /**
     * @return The ID of the TreeView of the last command on the undostack.
     */
    public String treeViewIDOfLastExecuted() {return undoStack.getLast().getTreeViewID();}

    /**
     *  @param runnable A runnable to be run on every execute.
     */
    public void addOnExecute(Runnable runnable) {onExecuteRunnables.add(runnable);}
    private final HashSet<Runnable> onExecuteRunnables = new HashSet<>();
    private void runOnExecute() {for (Runnable runnable : onExecuteRunnables) runnable.run();}


    private final HashMap<String, TreeView<ANode>> treeViews = new HashMap<>();

    /**
     * Before this class can work on a TreeView, the TreeView must be provided. This class relies on the .getId() of the
     * TreeView. This also means that this class cannot work on two TreeViews whose IDs are the same.
     */
    public void addTreeView(TreeView<ANode> treeView) {if (!treeViews.containsKey(treeView.getId())) this.treeViews.put(treeView.getId(), treeView);}
    public void removeTreeView(TreeView<ANode> treeView) {this.treeViews.remove(treeView.getId());}
    public boolean knowsTreeViewID(String treeViewID) {return treeViews.containsKey(treeViewID);}


    /**
     * This is called after a TreeItem has been inserted.
     * Updates the values of the TreeItems if necessary.
     */
    private void updateValuesOnInsert(TreeItem<ANode> inserted) {
        if (inserted.getParent() != null) inserted.getParent().getValue().getChildren().add(inserted.getValue());
    }

    /**
     * This is called after a TreeItem has been removed.
     * Updates the values of the TreeItems if necessary.
     */
    private void updateValuesOnRemove(TreeItem<ANode> removed, TreeItem<ANode> parentOfRemoved) {
        parentOfRemoved.getValue().getChildren().remove(removed.getValue());
    }
    

    private final ObservableList<TreeViewEditorCommand> undoStack = FXCollections.observableList(new LinkedList<TreeViewEditorCommand>());
    private final ObservableList<TreeViewEditorCommand> redoStack = FXCollections.observableList(new LinkedList<TreeViewEditorCommand>());

    public BooleanBinding canUndo() {return Bindings.isEmpty(undoStack).not();}
    public BooleanBinding canRedo() {return Bindings.isEmpty(redoStack).not();}


    public void undo() {
        logger.log(Level.INFO, "Undo " + undoStack.getLast().name());
        if (!canUndo().get()) return;
        System.out.println("can: " + canUndo().get() + " with size " + undoStack.size());   //at some point undostack.getlast threw out of bounds which shouldve been impossible
        undoStack.getLast().undo();
        redoStack.add(undoStack.removeLast());
        runOnEvent();
    }

    public void redo() {
        if (!canRedo().get()) return;
        logger.log(Level.INFO, "Redo " + redoStack.getLast().name());
        redoStack.getLast().redo();
        undoStack.add(redoStack.removeLast());
        runOnEvent();
    }

    void execute(TreeViewEditorCommand treeViewEditorCommand) {
        logger.log(Level.INFO, "Execute " + treeViewEditorCommand.name());
        treeViewEditorCommand.execute();
        undoStack.add(treeViewEditorCommand);
        redoStack.clear();
        runOnEvent();
        runOnExecute();
    }

    private final TreeItem<ANode> clipboardHolder = new TreeItem<>();

    public TreeItem<ANode> clipBoardContent() {return clipboardHolder.getChildren().getLast();}

    public BooleanBinding canPaste() {
        return Bindings.isEmpty(clipboardHolder.getChildren()).not();
    }
//    public BooleanBinding canCut() {return canPaste().not();} not doing this. cutting while theres stuff on clipboard
    //simply overrides the clipboard as it does anyhwere else. also, else i would need to solve some other hassles.

    public void cut(TreeItem<ANode> mover, String treeViewID) {
        execute(new CutTreeItemCommand(mover, treeViewID));
    }

    
    public void delete(TreeItem<ANode> toDelete, String treeViewID) {
        if (toDelete.getParent() == null) return;
        execute(new DeleteTreeItemCommand(toDelete, treeViewID));
    }

    public void paste(TreeItem<ANode> targetParent, String treeViewID) {
        if (!canPaste().get() || this.clipboardHolder.getChildren().getLast() == targetParent) return;
        else execute(new PasteTreeItemCommand(targetParent, treeViewID));
    }

    public TreeItem<ANode> create(TreeItem<ANode> parent, ANode newNodeValue, String treeViewID) {
        execute(new CreateTreeItemCommand(parent, newNodeValue, treeViewID));
        return parent.getChildren().getLast();
    }

    /**
     * Makes all children of treeItem siblings of treeItem.
     * @throws IllegalArgumentException if the treeItem is a root.
     */
    public void extractChildren(TreeItem<ANode> parent, String treeViewID) throws IllegalArgumentException {
        if (parent.getChildren().isEmpty()) return;
        execute(new ExtractChildrenCommand(parent, treeViewID));
    }

    public void rename(TreeItem<ANode> treeItem, String name, String treeViewID) {
        execute(new RenameTreeItemCommand(treeItem, name, treeViewID));
    }

    public void restructureTree(TreeItem<ANode> subtree, String treeViewID) {
        TreeView<ANode> treeView = treeViews.get(treeViewID);
        if (treeView == null) throw new IllegalArgumentException("UndoableTreeViewEditor does not know about this TreeView (id=" + treeViewID + ").");
        try {
            TreeRestructuring treeRestructuring;
            if (subtree.getParent() == null) treeRestructuring = new TreeRestructuring(subtree);  //assuming it is the root then
            else treeRestructuring = new TreeRestructuring(treeView.getRoot(), subtree);
            treeRestructuring.addOnAcceptedRunnable(() -> {
                try {
                    execute(new RestructureTreeCommand(subtree, treeRestructuring.getResultNow(), treeView.getId()));
                } catch (NullPointerException n) {
                    LittlePopUp.showMsg("Error", """
                Failed to accept. Possible reasons:
                    - The TreeItem you were working on does no longer exist
                    - The TreeView you were working on has been closed""", "OK");
                }
            });
        } catch (IOException ignored) {
        }
    }

    public void addFileID(TreeItem<ANode> treeItem, String fileID, String treeViewID) {
        execute(new AddFileIDCommand(treeItem, fileID, treeViewID));
    }

    public void removeFileID(TreeItem<ANode> treeItem, String fileID, String treeViewID) {
//        if (!treeItem.getValue().fileIds().contains(fileID)) return; not doing this bcuz im neiter checking before adding fileIDs. and i dont want to /can check beforea adding because whether fileIDs is a set or not is implementation-specific to ANode.
        execute(new RemoveFileIDCommand(treeItem, fileID, treeViewID));
    }

    /**
     * Does NOT treeView.setRoot(null)
     */
    public static boolean moveTreeItem(TreeItem<ANode> mover, TreeItem<ANode> targetParent) {
        if (mover.getParent() != null) mover.getParent().getChildren().remove(mover);
        return targetParent.getChildren().add(mover);
    }

    // all commands use this pattern: don't rely on an instance, but know how to get what you need
    // thus, everyone who works with this editor needs to provide the treeview and MUST NOT RENAME IT (bcuz String keys in dicts aren't passed by reference are they?? anyway better be safe).
    // because adding / removing models (and thus also treeviews) means that the instances will no longer be the correct ones.
    private abstract class TreeViewEditorCommand implements Command {

        private final String treeViewID;
        protected TreeView<ANode> getTreeView() {
            return treeViews.get(treeViewID);
        }
        public String getTreeViewID() {return treeViewID;}

        public TreeViewEditorCommand(String treeViewID) {
            if (!treeViews.containsKey(treeViewID)) throw new IllegalArgumentException("This TreeViewEditor does not know about TreeView ID=" + treeViewID);
            this.treeViewID = treeViewID;
        }
    }

    private class RenameTreeItemCommand extends TreeViewEditorCommand {

        private final String oldName;
        private final String nwName;
        private final String itemID;

        public RenameTreeItemCommand(TreeItem<ANode> treeItem, String name, String treeViewID) {
            super(treeViewID);
            this.itemID = treeItem.getValue().conceptId();
            this.oldName = treeItem.getValue().name();
            this.nwName = name;
        }

        @Override
        public void undo() {
            TreeItem<ANode> treeItem = TreeAnalysisUtils.getTreeItemWANodeId(itemID, getTreeView().getRoot());
            ANode value = TreeAnalysisUtils.getTreeItemWANodeId(itemID, getTreeView().getRoot()).getValue();
            value.setName(oldName);
            treeItem.setValue(value);
            getTreeView().refresh();
        }

        @Override
        public void execute() {
            TreeItem<ANode> treeItem = TreeAnalysisUtils.getTreeItemWANodeId(itemID, getTreeView().getRoot());
            ANode value = TreeAnalysisUtils.getTreeItemWANodeId(itemID, getTreeView().getRoot()).getValue();
            value.setName(nwName);
            treeItem.setValue(value);
            getTreeView().refresh();
        }

        @Override
        public void redo() {
            execute();
        }

        @Override
        public String name() {
            return "RenameTreeItemCommand";
        }
    }

    private class CreateTreeItemCommand extends TreeViewEditorCommand {
        
        private final String parentID;
        private final TreeItem<ANode> newNode;
        
        public CreateTreeItemCommand(TreeItem<ANode> parent, ANode value, String treeViewID) {
            super(treeViewID);
            this.parentID = parent.getValue().conceptId();
            this.newNode = new TreeItem<>(value);
        }

        @Override
        public void execute() {
            TreeItem<ANode> parent = TreeAnalysisUtils.getTreeItemWANodeId(parentID, getTreeView().getRoot());
            parent.getChildren().add(newNode);
            parent.setExpanded(true);
            updateValuesOnInsert(newNode);
        }

        @Override
        public void undo() {
            TreeItem<ANode> parent = TreeAnalysisUtils.getTreeItemWANodeId(parentID, getTreeView().getRoot());
            TreeItem<ANode> newNode = TreeAnalysisUtils.getTreeItemWANodeId(this.newNode.getValue().conceptId(), getTreeView().getRoot());  // if treeview's been recreated then so has newNode
            parent.getChildren().remove(newNode);
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

    private class DeleteTreeItemCommand extends TreeViewEditorCommand {

        private TreeItem<ANode> treeItem;
        private final String treeItemID;
        private final String parentID;

        public DeleteTreeItemCommand(@NotNull TreeItem<ANode> treeItem, String treeViewID) {
            super(treeViewID);
            if (treeItem.getParent() == null) throw new IllegalArgumentException("TreeItem must have a parent.");
            this.treeItem = treeItem;
            this.treeItemID = treeItem.getValue().conceptId();
            this.parentID = treeItem.getParent().getValue().conceptId();
        }

        @Override
        public void execute() {
            TreeItem<ANode> treeItem = TreeAnalysisUtils.getTreeItemWANodeId(this.treeItemID, getTreeView().getRoot());
            if (treeItem.getParent() != null) {
                treeItem.getParent().getChildren().remove(treeItem);
                updateValuesOnRemove(treeItem, TreeAnalysisUtils.getTreeItemWANodeId(parentID, getTreeView().getRoot()));
            }
            else {
                updateValuesOnRemove(treeItem, null);
            }
            this.treeItem = treeItem;
        }

        @Override
        public void undo() {
            TreeAnalysisUtils.getTreeItemWANodeId(parentID, getTreeView().getRoot()).getChildren().add(this.treeItem);
            updateValuesOnInsert(this.treeItem);
        }
        @Override
        public void redo() {execute();}

        @Override
        public String name() {
            return "DeleteTreeItemCommand";
        }
    }

    private class PasteTreeItemCommand extends TreeViewEditorCommand {

        private final String targetParentID;
        private final String moverID;

        public PasteTreeItemCommand(TreeItem<ANode> targetParent, String treeViewID) {
            super(treeViewID);
            this.targetParentID = targetParent.getValue().conceptId();
            this.moverID = clipboardHolder.getChildren().getLast().getValue().conceptId();
        }
        @Override
        public void execute() {
            TreeItem<ANode> targetParent = TreeAnalysisUtils.getTreeItemWANodeId(this.targetParentID, getTreeView().getRoot());
            if (moveTreeItem(clipboardHolder.getChildren().getLast(), targetParent)) {
                updateValuesOnInsert(TreeAnalysisUtils.getTreeItemWANodeId(moverID, getTreeView().getRoot()));
                targetParent.setExpanded(true);
            }
        }
        @Override
        public void undo() {
            TreeItem<ANode> mover = TreeAnalysisUtils.getTreeItemWANodeId(this.moverID, getTreeView().getRoot());
            assert mover != null;
            moveTreeItem(mover, clipboardHolder);
            updateValuesOnRemove(mover, Objects.requireNonNull(TreeAnalysisUtils.getTreeItemWANodeId(targetParentID, getTreeView().getRoot())));
        }
        @Override
        public void redo() {execute();}

        @Override
        public String name() {return "PasteTreeItemCommand";}
    }


    private class CutTreeItemCommand extends TreeViewEditorCommand {

        private final String parentID;
        private final String moverID;
        /**
         * The parent of mover must not be null, otherwise an exception will be thrown.
         */
        public CutTreeItemCommand(TreeItem<ANode> mover, String treeViewID) throws IllegalArgumentException{
            super(treeViewID);
            if (mover.getParent() == null) {
                throw new IllegalArgumentException("TreeItem to cut must have a parent.");
            }
            this.moverID = mover.getValue().conceptId();
            this.parentID = mover.getParent() != null ? mover.getParent().getValue().conceptId() : null;
        }


        @Override
        public void execute() {
            TreeItem<ANode> mover = TreeAnalysisUtils.getTreeItemWANodeId(this.moverID, getTreeView().getRoot());
            assert mover != null;
            moveTreeItem(mover, clipboardHolder);
            updateValuesOnRemove(mover, Objects.requireNonNull(TreeAnalysisUtils.getTreeItemWANodeId(parentID, getTreeView().getRoot())));
        }
        @Override
        public void undo() {
            TreeItem<ANode> mover = clipboardHolder.getChildren().getLast();
            moveTreeItem(mover, TreeAnalysisUtils.getTreeItemWANodeId(parentID, getTreeView().getRoot()));
            updateValuesOnInsert(mover);
        }@Override
        public void redo() {execute();}

        @Override
        public String name() {return "CutTreeItemCommand";}
    }

    private class ExtractChildrenCommand extends TreeViewEditorCommand {

        private final String treeItemID;
        private final ArrayList<String> childIDs;

        /**
         * Makes all children of treeItem siblings of treeItem.
         * @throws IllegalArgumentException if the treeItem is a root.
         */
        public ExtractChildrenCommand(TreeItem<ANode> treeItem, String treeViewID) {
            super(treeViewID);
            if (treeItem.getParent() == null) throw new IllegalArgumentException("Cannot extract children from root");
            this.treeItemID = treeItem.getValue().conceptId();
            this.childIDs = new ArrayList<>(treeItem.getChildren().size());
        }

        @Override
        public void execute() {
            TreeItem<ANode> treeItem = TreeAnalysisUtils.getTreeItemWANodeId(this.treeItemID, getTreeView().getRoot());
            for (TreeItem<ANode> child : new LinkedList<>(treeItem.getChildren())) {
                treeItem.getChildren().remove(child);
                updateValuesOnRemove(child, treeItem);
                treeItem.getParent().getChildren().add(child);
                updateValuesOnInsert(child);
                this.childIDs.add(child.getValue().conceptId());
            }
        }

        @Override
        public void undo() {
            for (String childID : this.childIDs) {
                TreeItem<ANode> child = TreeAnalysisUtils.getTreeItemWANodeId(childID, getTreeView().getRoot());
                if (child.getParent() != null) {
                    TreeItem<ANode> parent = child.getParent();
                    parent.getChildren().remove(child);
                    updateValuesOnRemove(child, parent);
                }
                TreeAnalysisUtils.getTreeItemWANodeId(treeItemID, getTreeView().getRoot()).getChildren().add(child);
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

    private class RestructureTreeCommand extends TreeViewEditorCommand {

        private final String beforeID;
        private final TreeItem<ANode> after;

        // only use this to store the before-item after it has been replaced by after-item (is set after each execute).
        private TreeItem<ANode> before = null;

        public RestructureTreeCommand(TreeItem<ANode> before, TreeItem<ANode> after, String treeViewID) {
            super(treeViewID);
            this.beforeID = before.getValue().conceptId();
            this.after = after;
        }

        @Override
        public void undo() {
            TreeItem<ANode> realTreeParent = this.after.getParent();
            if (realTreeParent != null) {
                realTreeParent.getChildren().remove(this.after);
                realTreeParent.getValue().getChildren().remove(this.after.getValue());
                UndoableANodeTreeViewEditor.moveTreeItem(this.before, realTreeParent);
                realTreeParent.getValue().getChildren().add(this.before.getValue());
            } else {
                getTreeView().setRoot(this.before);
            }
        }

        @Override
        public void execute() {
            TreeItem<ANode> before = TreeAnalysisUtils.getTreeItemWANodeId(beforeID, getTreeView().getRoot());
            TreeItem<ANode> realTreeParent = before.getParent();

            if (realTreeParent != null) {
                realTreeParent.getChildren().remove(before);
                realTreeParent.getValue().getChildren().remove(before.getValue());
                moveTreeItem(this.after, realTreeParent);
                realTreeParent.getValue().getChildren().add(this.after.getValue());
            } else {
                getTreeView().setRoot(this.after);
            }
            this.before = before;
        }

        @Override
        public void redo() {execute();}
        @Override
        public String name() {return "RestructureTreeCommand";}
    }

    private class AddFileIDCommand extends TreeViewEditorCommand {

        private final String treeItemID;
        private final String fileID;

        public AddFileIDCommand(TreeItem<ANode> treeItem, String fileID, String treeViewID) {
            super(treeViewID);
            this.treeItemID = treeItem.getValue().conceptId();
            this.fileID = fileID;
        }
        @Override
        public void undo() {
            TreeItem<ANode> treeItem = TreeAnalysisUtils.getTreeItemWANodeId(treeItemID, getTreeView().getRoot());
            if (treeItem != null) treeItem.getValue().fileIds().remove(this.fileID);
        }
        @Override
        public void execute() {
            TreeItem<ANode> treeItem = TreeAnalysisUtils.getTreeItemWANodeId(treeItemID, getTreeView().getRoot());
            if (treeItem != null) treeItem.getValue().fileIds().add(fileID);}
        @Override
        public void redo() {execute();}
        @Override
        public String name() {return "AddFileIDCommand";}
    }

    private class RemoveFileIDCommand extends TreeViewEditorCommand {
        private final String treeItemID;
        private final String fileID;

        public RemoveFileIDCommand(TreeItem<ANode> treeItem, String fileID, String treeViewID) {
            super(treeViewID);
            this.treeItemID = treeItem.getValue().conceptId();
            this.fileID = fileID;
        }
        @Override
        public void undo() {
            TreeItem<ANode> treeItem = TreeAnalysisUtils.getTreeItemWANodeId(treeItemID, getTreeView().getRoot());
            if (treeItem != null) treeItem.getValue().fileIds().add(this.fileID);}
        @Override
        public void execute() {
            TreeItem<ANode> treeItem = TreeAnalysisUtils.getTreeItemWANodeId(treeItemID, getTreeView().getRoot());
            if (treeItem != null) treeItem.getValue().fileIds().remove(fileID);}
        @Override
        public void redo() {execute();}
        @Override
        public String name() {return "AddFileIDCommand";}
    }
}