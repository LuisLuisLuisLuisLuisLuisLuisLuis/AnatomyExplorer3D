package Project.command.Remember;

import Project.SelectionModel.FXGroupDraw.HasFXGroupContents;
import Project.window.WindowPresenter;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class RememberHasFXGroupContents {

    private static final Logger logger = Logger.getLogger(RememberHasFXGroupContents.class.getName());

    private final boolean selective;

    private final ArrayList<MeshView> meshViews;
    private final ArrayList<TriangleMesh> triangleMeshes;

    private final HasFXGroupContents hasFXGroupContents;

    /**
     * Remember the state (TriangleMesh and whether is drawn or not) of all MeshViews held by HasFXGroupContents.
     */
    public RememberHasFXGroupContents(HasFXGroupContents hasFXGroupContents) {
        selective = false;
        this.hasFXGroupContents = hasFXGroupContents;
        this.meshViews = new ArrayList<>(hasFXGroupContents.getSelection().size());
        this.triangleMeshes = new ArrayList<>(hasFXGroupContents.getSelection().size());

        for (MeshView meshView : new LinkedList<>(hasFXGroupContents.getSelection())) {
            TriangleMesh triangleMesh = (TriangleMesh) meshView.getMesh();
            this.meshViews.add(meshView);
            this.triangleMeshes.add(triangleMesh);
        }
        logger.log(Level.FINER, "Remembering these MeshViews: " + this.meshViews.stream().map(MeshView::getId).toList());
    }
    /**
     * Only remember the state of these MeshViews.
     * @param hasFXGroupContents Holds the MeshViews.
     * @param meshViews List of MeshViews to remember.
     */
    public RememberHasFXGroupContents(HasFXGroupContents hasFXGroupContents, List<MeshView> meshViews) {
        selective = true;
        this.hasFXGroupContents = hasFXGroupContents;
        this.meshViews = new ArrayList<>(meshViews.size());
        this.triangleMeshes = new ArrayList<>(meshViews.size());

        for (MeshView meshView : meshViews) {
            TriangleMesh triangleMesh = (TriangleMesh) meshView.getMesh();
            this.meshViews.add(meshView);
            this.triangleMeshes.add(triangleMesh);
        }
    }

    public void restoreSelection() {
        logger.log(Level.FINE, "restoreselection: " + meshViews.stream().map(MeshView::getId).toList());
        if (!selective) {
            hasFXGroupContents.setSelection(meshViews, triangleMeshes);
        }
        else {
            for (int m = 0; m < meshViews.size(); m++) hasFXGroupContents.select(meshViews.get(m), triangleMeshes.get(m));
        }
        return;
    }
}
