package Project.command.Remember;

import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.HashMap;
import java.util.List;

public class RememberMeshViewMeshes {
    private final HashMap<MeshView, TriangleMesh> meshViewToMesh;   //will map MeshView ids to their TriangleMesh before they were undrawn.

    public RememberMeshViewMeshes(List<MeshView> meshViews) {
        this.meshViewToMesh = new HashMap<>(meshViews.size());
        for (MeshView meshView : meshViews) {
            TriangleMesh triangleMesh = (TriangleMesh) meshView.getMesh();
            meshViewToMesh.put(meshView, triangleMesh);
        }
    }

    public void restoreSelection() {

    }
}

