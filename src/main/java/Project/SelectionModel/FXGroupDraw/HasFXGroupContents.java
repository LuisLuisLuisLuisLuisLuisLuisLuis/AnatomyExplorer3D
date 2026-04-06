package Project.SelectionModel.FXGroupDraw;

import Project.SelectionModel.HasSelection;
import Project.command.Remember.RememberHasFXGroupContents;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.*;
import javafx.scene.Group;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HasFXGroupContents implements HasSelection<MeshView> {

    private static final Logger logger = Logger.getLogger(HasFXGroupContents.class.getName());

    private final String id;
    @Override
    public String getId() {return id;}

    private final Group group;

    private final ObservableList<MeshView> selection = FXCollections.observableList(new ArrayList<>());

    private HasFXGroupContentsContainer hasFXGroupContentsContainer = null;

    public void setHasFXGroupContentsContainer(HasFXGroupContentsContainer hasFXGroupContentsContainer) {this.hasFXGroupContentsContainer = hasFXGroupContentsContainer;}

    /**
     * This class resets all MeshViews UserData upon select()!!
     * @param group
     * @param id
     */
    public HasFXGroupContents(Group group, String id) {
        this.group = group;
        this.id = id;
        meshViewToOGMesh.addListener((MapChangeListener<MeshView, TriangleMesh>) change -> {
            if (change.wasRemoved() && !meshViewToOGMesh.containsKey(change.getKey())) {
                availableMeshViews.remove(change.getKey());
            }
            if (change.wasAdded()) {
                availableMeshViews.add(change.getKey());
            }
        });
        if (logger.getLevel() == Level.FINER) {
            this.availableMeshViews.addListener((ListChangeListener<MeshView>) c -> {
                while (c.next()) {
                    if (c.wasAdded()) logger.log(Level.INFO, c.getAddedSubList().toString() + " added to availableMeshViews");
                    else if (c.wasRemoved()) logger.log(Level.INFO, c.getRemoved().toString() + " removed from availableMeshViews");
                }

            });

        }
    }

    private final ObservableMap<MeshView, TriangleMesh> meshViewToOGMesh = FXCollections.observableHashMap();
    private final ObservableList<MeshView> availableMeshViews = FXCollections.observableArrayList();

    public void addMeshViews(List<MeshView> meshViews) {
        LinkedList<MeshView> newOnes = new LinkedList<>();
        for (MeshView meshView : meshViews) {
            if (meshViewToOGMesh.containsKey(meshView) || meshView == null || getMeshViewWithID(meshView.getId()) != null) {continue;}
            newOnes.add(meshView);
            meshViewToOGMesh.put(meshView, (TriangleMesh) meshView.getMesh());
            meshView.setMesh(null);
        }
        this.group.getChildren().addAll(newOnes);
    }

    public void addOBJ(String fileName) {
        if (getMeshViewWithID(fileName) != null) return;
        MeshView meshView = (MeshView) hasFXGroupContentsContainer.createMeshView(fileName);
        TriangleMesh triangleMesh = (TriangleMesh) meshView.getMesh();
        meshViewToOGMesh.put(meshView, triangleMesh);
        meshView.setMesh(null);
        this.group.getChildren().add(meshView);
    }

    public void removeOBJ(String id) {
        MeshView meshView = getMeshViewWithID(id);
        meshViewToOGMesh.remove(meshView);
        this.group.getChildren().remove(meshView);
        this.selection.remove(meshView);
    }

    @Override
    public ObservableList<MeshView> getSelection() {return this.selection;}
    
    @Override
    public void setSelection(List<MeshView> selection) {
        this.clearSelection();
        for (MeshView meshView : selection) this.select(meshView);
    }
    public void setSelection(List<MeshView> selection, List<TriangleMesh> meshes) {
        this.clearSelection();
        for (int m = 0; m < selection.size(); m++) this.select(selection.get(m), meshes.get(m));
    }

    /**
     * Draws a MeshView with the same ID as mockItem. Does NOT set CullFace.BACK. Adds the MeshView to the Selection of this.
     * @param mockItem
     * @param triangleMesh
     */
    public void select(MeshView mockItem, TriangleMesh triangleMesh) {
        logger.log(Level.FINE, "trying to select "+ mockItem.getId());
        MeshView target = getMeshViewWithID(mockItem.getId());
        logger.log(Level.FINER, mockItem.getId() + " is " + (target==null ? "" : "not ") + "null");
        if (target == null || !(target.getMesh() == null)) return;  //dont draw things that are not drawable by this OR that are already drawn
        target.setMesh(triangleMesh);
        this.selection.add(target);
    }

    /**
     * Draws a MeshView with the same ID as mockItem. Sets CullFace.BACK. Adds the MeshView to the Selection of this.
     * @param mockItem This MeshView will not be drawn. Instead, a MeshView which this class already has that has the same
     *                 ID as mockItem will be drawn.
     */
    @Override
    public void select(MeshView mockItem) {
        logger.log(Level.FINE, "trying to select "+ mockItem.getId());
        MeshView target = getMeshViewWithID(mockItem.getId());
        logger.log(Level.FINER, mockItem.getId() + " is " + (target==null ? "" : "not ") + "null");
        if (target == null || !(target.getMesh() == null)) return;  //dont draw things that are not drawable by this OR that are already drawn
        target.setCullFace(CullFace.BACK);  //
        target.setMesh(meshViewToOGMesh.get(target));
        this.selection.add(target);
        target.setUserData(null);   //
    }

    @Override
    public void unselect(MeshView item) {
        MeshView target = getMeshViewWithID(item.getId());
        if (target == null) return;
//        TriangleMesh lastDrawnMesh = (TriangleMesh) target.getMesh();
//        meshViewToMesh.put(target, lastDrawnMesh);
        target.setMesh(null);
        this.selection.remove(target);
    }

    /**
     * Looks up the MeshView with the given ID out of all MeshViews (selected or unselected) of this; i.e. out of getAllItems().
     * @param id
     * @return
     */
    public MeshView getMeshViewWithID(String id) {
        for (MeshView meshView : meshViewToOGMesh.keySet()) if (meshView.getId().equals(id)) return meshView;
        return null;
    }

    /**
     * Sets the default TriangleMesh for the MeshView with the given ID and sets CullFace.BACK.
     * ONLY allowed for MeshViews that are currently drawn, i.e. who's Mesh is not null, i.e. who are selected by this.
     */
    public void resetTriangleMesh(String id) {
        MeshView meshView = getMeshViewWithID(id);
        if (meshView == null) return;
        if (meshView.getMesh() == null) throw new IllegalArgumentException("resetTriangleMesh is only allowed for MeshViews that are already drawn.");
        meshView.setMesh(meshViewToOGMesh.get(meshView));
        meshView.setCullFace(CullFace.BACK);
        meshView.setUserData(null);
    }


    @Override
    public void clearSelection() {
        for (MeshView meshView : new LinkedList<>(this.selection)) this.unselect(meshView);
    }

    /**
     * @return A list of all items that this class is able to select, whether they be selected right now or not.
     */
    @Override
    public ObservableList<MeshView> getAllItems() {
        return availableMeshViews;
    }

}
