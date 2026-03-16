package Project.command.DrawCommands;

import Project.SelectionModel.FXGroupDraw.HasFXGroupContents;
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
    private SelectionGroup selectionGroup;

    /**
     * @param color: Color all nodes with this color.
     * @param nodeIDs: Nodes to color.
     * @param hasFXGroupContents: contains the nodes
     * @param selectionGroup: holds
     */
    public ColorMeshviewsCommand(Color color, Set<String> nodeIDs, HasFXGroupContents hasFXGroupContents, SelectionGroup selectionGroup) {
        this.nodeIDs = nodeIDs;
        this.newColor = color;
        this.hasFXGroupContents = hasFXGroupContents;
        this.selectionGroup = selectionGroup;
    }

    @Override
    public void undo() {
        rememberFxMeshViewColors.restoreSelection();
    }

    @Override
    public void execute() {
        remember();

        for (String nodeID : nodeIDs) { // go thru nodeIDs and get the corresponding nodes
            Node node = hasFXGroupContents.getMeshViewWithID(nodeID);
            if (node instanceof MeshView) {
                if (((MeshView) node).getMaterial() instanceof PhongMaterial) {
                    // saturate or not, depending on selection
                    if (selectionGroup.getSelection().contains(nodeID)) ((PhongMaterial) ((MeshView) node).getMaterial()).setDiffuseColor(newColor);
                    else ((PhongMaterial) ((MeshView) node).getMaterial()).setDiffuseColor(newColor.desaturate().desaturate());
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
        this.rememberFxMeshViewColors = new RememberFXMeshViewColors(nodeIDs, hasFXGroupContents, selectionGroup);
    }

    @Override
    public String name() {return "ColorMeshviewsCommand";}
}
