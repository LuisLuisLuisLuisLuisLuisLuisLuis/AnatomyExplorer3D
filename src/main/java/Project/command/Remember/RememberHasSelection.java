package Project.command.Remember;

import Project.SelectionModel.HasSelection;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Remembers the selection of a HasSelection and provides method to restore it.
 */
public class RememberHasSelection<T> {

    private final HasSelection<T> hasSelection;
    private final ArrayList<T> selection;

    public RememberHasSelection(HasSelection<T> hasSelection) {
        this.hasSelection = hasSelection;
        this.selection = new ArrayList<>(hasSelection.getSelection());
    }
    public void restoreSelection() {
        hasSelection.setSelection(this.selection);
    }
}
