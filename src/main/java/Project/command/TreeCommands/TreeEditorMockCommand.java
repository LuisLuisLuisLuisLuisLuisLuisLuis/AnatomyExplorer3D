package Project.command.TreeCommands;

import Project.command.Command;
import Project.window.TreeView.TreeViewEditing.Command.UndoableANodeTreeViewEditor;

public class TreeEditorMockCommand<T> implements Command {
    private final UndoableANodeTreeViewEditor undoableANodeTreeViewEditor;

    /**
     * Mocks a command from UndoableTreeViewEditor.
     * execute() is without effect.
     * undo() / redo() will call undo() / redo() of the UndoableTreeViewEditor.
     * @param undoableANodeTreeViewEditor An UndoableTreeViewEditor.
     */
    public TreeEditorMockCommand(UndoableANodeTreeViewEditor undoableANodeTreeViewEditor) {
        this.undoableANodeTreeViewEditor = undoableANodeTreeViewEditor;
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
