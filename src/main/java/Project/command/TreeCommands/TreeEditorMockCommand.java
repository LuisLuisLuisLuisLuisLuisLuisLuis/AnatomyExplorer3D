package Project.command.TreeCommands;

import Project.command.Command;
import Project.window.TreeView.TreeViewEditing.Command.UndoableANodeTreeViewEditor;

public class TreeEditorMockCommand implements Command {
    private final UndoableANodeTreeViewEditor undoableANodeTreeViewEditor;

    private final String treeViewID;
    public String getTreeViewID() {return this.treeViewID;}

    /**
     * Mocks a command from UndoableANodeTreeViewEditor.
     * execute() is without effect.
     * undo() / redo() will call undo() / redo() of the UndoableTreeViewEditor.
     * @param undoableANodeTreeViewEditor An UndoableTreeViewEditor.
     * @param treeViewID ID of the TreeView this Command modifies.
     */
    public TreeEditorMockCommand(UndoableANodeTreeViewEditor undoableANodeTreeViewEditor, String treeViewID) {
        this.undoableANodeTreeViewEditor = undoableANodeTreeViewEditor;
        this.treeViewID = treeViewID;
    }
    public TreeEditorMockCommand(UndoableANodeTreeViewEditor undoableANodeTreeViewEditor) {
        this(undoableANodeTreeViewEditor, null);
    }

    @Override
    public void undo() {this.undoableANodeTreeViewEditor.undo();}

    @Override
    public void execute() {}

    @Override
    public void redo() {this.undoableANodeTreeViewEditor.redo();}

    @Override
    public String name() {return "TreeEditorMockCommand";}
}
