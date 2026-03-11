package Project.command.DrawCommands;

import Project.command.Command;
import Project.window.Slicing.MeshSlicer_Aware;
import Project.window.Slicing.Plane;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.*;
import javafx.scene.transform.NonInvertibleTransformException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class SliceCommand implements Command {

    private final HashSet<String> meshViewIDs;
    private final Group meshViewGroup;
    private final double[] nxyd;

    private final HashMap<String, TriangleMesh> idToOgMesh;

    /**
     * Relies on every MeshView having a unique, consistent ID. Requires that when a MeshView is replaced by a new identical MeshView,
     * that ID is kept.
     * @param meshViews
     * @param meshViewGroup
     * @param box
     */
    public SliceCommand(List<MeshView> meshViews, Group meshViewGroup, Box box) {
        this.meshViewGroup = meshViewGroup;
        this.meshViewIDs = new HashSet<>();
        for (MeshView meshView : meshViews) {
            meshViewIDs.add(meshView.getId());
        }
        idToOgMesh = new HashMap<>(meshViews.size());

        double[] temp = null;   //needed to satisfy nxyd final constraint
        try {
             temp = Plane.extractPlaneFromBox(box, meshViewGroup);
        } catch (NonInvertibleTransformException ne) {
            nxyd = temp;
            return;
        }
        nxyd = temp;
    }

    @Override
    public void undo() {
        for (Node node : meshViewGroup.getChildren()) {
            if (!(node instanceof MeshView meshView)) continue;
            if (idToOgMesh.containsKey(meshView.getId())) {
                meshView.setMesh(idToOgMesh.get(meshView.getId()));
            }
        }
    }

    @Override
    public void execute() {
        idToOgMesh.clear();

        for (Node node : meshViewGroup.getChildren()) {
            if (!(node instanceof MeshView meshView)) continue;
            if (!meshViewIDs.contains(meshView.getId())) continue;

            if (!(meshView.getMesh() instanceof TriangleMesh)) continue;
            SimpleBooleanProperty meshIsCut = new SimpleBooleanProperty(false);
            TriangleMesh cutMesh = MeshSlicer_Aware.slicePositiveSide((TriangleMesh) meshView.getMesh(), nxyd[0], nxyd[1], nxyd[2], nxyd[3], meshIsCut);
            if (meshIsCut.get()) {
                idToOgMesh.put(meshView.getId(), (TriangleMesh) meshView.getMesh());
                meshView.setMesh(cutMesh);
                meshView.setCullFace(CullFace.NONE);    // VERY IMPORTANT! because now we can see inside the mesh, i.e. we can see both sides of the faces -> both sides need to be rendered -> FRONT/BACK wont suffice
            }
        }
    }

    @Override
    public void redo() {
        execute();
    }

    @Override
    public String name() {
        return "SliceCommand";
    }
}
