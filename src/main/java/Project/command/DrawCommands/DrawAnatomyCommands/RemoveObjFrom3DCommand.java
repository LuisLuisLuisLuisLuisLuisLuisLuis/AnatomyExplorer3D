package Project.command.DrawCommands.DrawAnatomyCommands;

import Project.SelectionModel.FXGroupDraw.HasFXGroupContents;
import Project.SelectionModel.SelectionGroup;
import Project.command.Command;
import Project.command.Remember.RememberFXMeshViewColors;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Command for un-drawing items.
 */
public class RemoveObjFrom3DCommand implements Command {

    private final Set<String> removedNodes;
    private final SelectionGroup threeDContentGroup;
    private final SelectionGroup threeDSelectionGroup;
    private final HasFXGroupContents hasFXGroupContents;

    private final HashMap<String, TriangleMesh> idToMesh;   //will map MeshView ids to their TriangleMesh before they were undrawn.

    /**
     *
     * @param nodesToRemove: set of fileIDs
     * @param threeDContentGroup: SelectionGroup controlling which items are drawn.
     * @param threeDSelectionGroup: SelectionGroup holding the 3D selection.
     * @param hasFXGroupContents: holding the 3D selection.
     */
    public RemoveObjFrom3DCommand(Set<String> nodesToRemove, SelectionGroup threeDContentGroup, HasFXGroupContents hasFXGroupContents, SelectionGroup threeDSelectionGroup) {
        this.removedNodes = nodesToRemove;
        this.idToMesh = new HashMap<>(nodesToRemove.size());
        this.threeDContentGroup = threeDContentGroup;
        this.threeDSelectionGroup = threeDSelectionGroup;

        this.hasFXGroupContents = hasFXGroupContents;   //required to remember the coloring.

    }

    @Override
    public void undo() {
//        threeDContentGroup.changeSelection(removedNodes, false, false); // re-draw
        for (String id : removedNodes) {
            MeshView meshView = hasFXGroupContents.getMeshViewWithID(id);
            hasFXGroupContents.select(meshView, idToMesh.get(id));
        }
//        rememberFXMeshViewColors.restoreSelection();  not necessary anymore since MeshViews keep existing and only their Mesh changes  // re-color
//        threeDSelectionGroup.changeSelection(removedNodes, false, false); removed because it seems unintuitive //also reselect all removed nodes
    }

    @Override
    public void execute() {
        idToMesh.clear();
        //was buggy when undrawn happened before unselect. should not matter anymore tho since undraw should automatically unselect
        threeDSelectionGroup.changeSelection(removedNodes, false, true);
        for (String id : removedNodes) {
            MeshView meshView = hasFXGroupContents.getMeshViewWithID(id);
            TriangleMesh triangleMesh = (TriangleMesh) meshView.getMesh();
            idToMesh.put(id, triangleMesh);
            hasFXGroupContents.unselect(meshView);
        }
//        threeDContentGroup.changeSelection(removedNodes, false, true);  // undraw and then unselect

    }

    @Override
    public void redo() {
        execute();
    }

    @Override
    public String name() {
        return "RemoveItemFrom3DCommand";
    }
}
