package Project.command.Remember;

import Project.SelectionModel.FXGroupDraw.HasFXGroupContents;
import Project.SelectionModel.FXGroupSelection.HasFXGroupSelection;
import Project.SelectionModel.SelectionGroup;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;

import java.util.*;

/**
 * A class to remember colorings of MeshViews.
 */
public class RememberFXMeshViewColors {

    private final Map<String, Color> oldColors = new HashMap<>(); //remembers the colors as fileID : color. remembers them in their unselected/desaturated state
    private final Set<String> nodeIDs;
    private final HasFXGroupContents hasFXGroupContents;
//    private final SelectionGroup<String> selectionGroup;
    private final HasFXGroupSelection hasFXGroupSelection;
    /**
     * @param nodeIDs: Nodes whose color is to be remembered
     * @param hasFXGroupContents: holds the nodes
     * @param hasFXGroupSelection: holds a user selection of the 3D view
     */
    public RememberFXMeshViewColors(Set<String> nodeIDs, HasFXGroupContents hasFXGroupContents, HasFXGroupSelection hasFXGroupSelection) {
        this.nodeIDs = nodeIDs;
        this.hasFXGroupContents = hasFXGroupContents;
        this.hasFXGroupSelection = hasFXGroupSelection;
        remember();
    }

    public void restoreSelection() {
        // two-step restoration: first restore the colors, then saturate them if the nodes are selected.
        for (String nodeID : nodeIDs) {
            MeshView node = hasFXGroupContents.getMeshViewWithID(nodeID);
            if (node != null && node.getMaterial() instanceof  PhongMaterial) {
                ((PhongMaterial) node.getMaterial()).setDiffuseColor(oldColors.get(nodeID));
            }
        }

        for (String selectedNodeID : hasFXGroupSelection.getSelection().stream().map(MeshView::getId).toList()) {
            MeshView node = hasFXGroupContents.getMeshViewWithID(selectedNodeID);
            if (node != null && node.getMaterial() instanceof PhongMaterial) {
                ((PhongMaterial) node.getMaterial()).setDiffuseColor(((PhongMaterial) node.getMaterial()).getDiffuseColor().saturate().saturate());
            }
        }

    }

    private void remember() {
        for (String nodeID : nodeIDs) { // go thru the IDs and get the corresponding node
            MeshView node = hasFXGroupContents.getMeshViewWithID(nodeID);
            if (node != null && node.getMaterial() instanceof PhongMaterial) { // cast to meshView
                // if the nodeID is contained in the user selection, desaturate the color before saving
                Color oldColor = hasFXGroupSelection.getSelection().stream().map(MeshView::getId).toList().contains(nodeID) ? ((PhongMaterial) node.getMaterial()).getDiffuseColor().desaturate().desaturate() : ((PhongMaterial) node.getMaterial()).getDiffuseColor();
                oldColors.put(nodeID, oldColor); // save
            }
        }
    }

}
