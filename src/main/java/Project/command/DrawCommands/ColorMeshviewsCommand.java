package Project.command.DrawCommands;

import Project.SelectionModel.FXGroupDraw.HasFXGroupContents;
import Project.SelectionModel.FXGroupSelection.HasFXGroupSelection;
import Project.SelectionModel.SelectionGroup;
import Project.command.Command;
import Project.command.Remember.RememberFXMeshViewColors;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;

import java.util.Set;

public class ColorMeshviewsCommand implements Command {

    private RememberFXMeshViewColors rememberFxMeshViewColors;
    private HasFXGroupContents hasFXGroupContents;
    private Set<String> nodeIDs;
    private Color newColor;
//    private SelectionGroup selectionGroup;
    private HasFXGroupSelection hasFXGroupSelection;

    /**
     * @param color: Color all nodes with this color.
     * @param nodeIDs: Nodes to color.
     * @param hasFXGroupContents: contains the nodes
     * @param hasFXGroupSelection: holds
     */
    public ColorMeshviewsCommand(Color color, Set<String> nodeIDs, HasFXGroupContents hasFXGroupContents, HasFXGroupSelection hasFXGroupSelection) {
        this.nodeIDs = nodeIDs;
        this.newColor = color;
        this.hasFXGroupContents = hasFXGroupContents;
        this.hasFXGroupSelection = hasFXGroupSelection;
    }

    @Override
    public void undo() {
        rememberFxMeshViewColors.restoreSelection();
    }

    @Override
    public void execute() {
        remember();

        for (String nodeID : nodeIDs) { // go through nodeIDs and get the corresponding nodes
            MeshView meshView = hasFXGroupContents.getMeshViewWithID(nodeID);
            if (meshView != null) {
                if (meshView.getMaterial() instanceof PhongMaterial) {
                    // saturate or not, depending on selection
                    if (hasFXGroupSelection.getSelection().stream().map(MeshView::getId).toList().contains(nodeID)) ((PhongMaterial) meshView.getMaterial()).setDiffuseColor(newColor);
                    else ((PhongMaterial) meshView.getMaterial()).setDiffuseColor(newColor.desaturate().desaturate());
                }
            }
        }
    }

    @Override
    public void redo() {
        execute();
    }

    private void remember() {
        // remember and undo via RememberFxMeshViewColors
        this.rememberFxMeshViewColors = new RememberFXMeshViewColors(nodeIDs, hasFXGroupContents, hasFXGroupSelection);
    }

    @Override
    public String name() {return "ColorMeshviewsCommand";}
}
