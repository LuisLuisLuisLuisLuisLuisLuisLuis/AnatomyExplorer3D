package Project.command.DrawCommands;

import Project.SelectionModel.FXGroupDraw.HasFXGroupContents;
import Project.SelectionModel.SelectionGroup;
import Project.command.Command;
import Project.command.Remember.RememberHasFXGroupContents;
import Project.window.Slicing.MeshSlicer_Aware;
import Project.window.Slicing.Plane;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.*;
import javafx.scene.transform.NonInvertibleTransformException;

import java.util.*;
import java.util.stream.Collectors;

public class SliceCommand implements Command {

    private final HashSet<String> meshViewIDs;
    private final Group meshViewGroup;
    private final double[] nxyd;

    private final HashMap<String, TriangleMesh> meshViewIDToOgMesh;
    LinkedList<String> cutTheFirstTime = new LinkedList<>();
    private RememberHasFXGroupContents rememberHasFXGroupContents;  //remembers Meshes that are fully removed by this slice and are not in their original state when this happens

    private final HasFXGroupContents hasFXGroupContents;
    private final SelectionGroup<String> threeDSelectionGroup;

    /**
     * Relies on every MeshView having a unique, consistent ID. Requires that when a MeshView is replaced by a new identical MeshView,
     * that ID is kept.
     * Relies on MeshView.getUserData() == null ->
     * @param meshViews
     * @param meshViewGroup
     * @param box
     */
    public SliceCommand(List<MeshView> meshViews, Group meshViewGroup, Box box, HasFXGroupContents hasInnerGroupContents, SelectionGroup<String> threeDSelectionGroup) {
        this.meshViewGroup = meshViewGroup;
        this.meshViewIDs = new HashSet<>();
        for (MeshView meshView : meshViews) meshViewIDs.add(meshView.getId());

        meshViewIDToOgMesh = new HashMap<>(meshViews.size());

        this.hasFXGroupContents = hasInnerGroupContents;
        this.threeDSelectionGroup = threeDSelectionGroup;

        double[] temp = null;           //needed to satisfy nxyd final constraint
        try {temp = Plane.extractPlaneFromBox(box, meshViewGroup);}
        catch (NonInvertibleTransformException ne) {nxyd = temp; return;}

        nxyd = temp;


    }

    @Override
    public void undo() {

        for (String meshViewid : meshViewIDToOgMesh.keySet()) {
            TriangleMesh triangleMesh = meshViewIDToOgMesh.get(meshViewid);
            MeshView meshView = hasFXGroupContents.getMeshViewWithID(meshViewid);
            if (triangleMesh != null) {
                meshView.setMesh(triangleMesh);
            } else {
                hasFXGroupContents.select(meshView);
            }
        }
        for (String meshViewid : cutTheFirstTime) {
            hasFXGroupContents.resetTriangleMesh(meshViewid);
        }
        if (this.rememberHasFXGroupContents != null ) this.rememberHasFXGroupContents.restoreSelection(); // restore the MeshViews that were fully removed
    }

    @Override
    public void execute() {
        this.rememberHasFXGroupContents = null;
        meshViewIDToOgMesh.clear();
        cutTheFirstTime.clear();
        LinkedList<MeshView> modMeshesToRemoveFully = new LinkedList<>();

        for (Node node : meshViewGroup.getChildren()) {
            if (!(node instanceof MeshView meshView)) continue;
            if (!meshViewIDs.contains(meshView.getId())) continue;

            if (!(meshView.getMesh() instanceof TriangleMesh)) continue;

            SimpleBooleanProperty meshIsCut = new SimpleBooleanProperty();
            TriangleMesh cutMesh = MeshSlicer_Aware.slicePositiveSide((TriangleMesh) meshView.getMesh(), nxyd[0], nxyd[1], nxyd[2], nxyd[3], meshIsCut);
            if (meshIsCut.get()) {
                if (cutMesh.getFaces().size() > 0) {
                    if (meshView.getUserData() != null) meshViewIDToOgMesh.put(meshView.getId(), (TriangleMesh) meshView.getMesh());
                    else cutTheFirstTime.add(meshView.getId());
                    meshView.setMesh(cutMesh);
                    meshView.setCullFace(CullFace.NONE);    // VERY IMPORTANT! because now we can see inside the MeshView, i.e. we can see both sides of the faces -> both sides need to be rendered -> FRONT/BACK wont suffice
                    meshView.setUserData("cut");
                } else {
                    if (meshView.getUserData() != null) modMeshesToRemoveFully.add(meshView);
                    else {
                        meshViewIDToOgMesh.put(meshView.getId(), null);
                        threeDSelectionGroup.changeSelection(Set.of(meshView.getId()), false, true);
                        this.hasFXGroupContents.unselect(meshView);
                    }

                }
            }
        }
        if (!modMeshesToRemoveFully.isEmpty()) {
            this.rememberHasFXGroupContents = new RememberHasFXGroupContents(hasFXGroupContents, modMeshesToRemoveFully);
            threeDSelectionGroup.changeSelection(modMeshesToRemoveFully.stream().map(Node::getId).collect(Collectors.toSet()), false, true);
            for (MeshView meshView : modMeshesToRemoveFully) this.hasFXGroupContents.unselect(meshView);
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
