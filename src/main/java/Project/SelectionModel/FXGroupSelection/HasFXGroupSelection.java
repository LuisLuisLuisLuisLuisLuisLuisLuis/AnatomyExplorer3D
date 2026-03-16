package Project.SelectionModel.FXGroupSelection;

import Project.SelectionModel.FXGroupDraw.HasFXGroupContents;
import Project.SelectionModel.HasSelection;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;

import java.util.LinkedList;
import java.util.List;

/**
 * This class has a Group and maintains a selection of the group's contents (-> what Nodes of the group are selected).
 */
public class HasFXGroupSelection implements HasSelection<MeshView> {

    private final String id;

    @Override
    public String getId() {
        return id;
    }

    /**
     * The group.
     */
    private final HasFXGroupContents hasFXGroupContents;

    /**
     * The selection.
     */
    private final ObservableList<MeshView> selection = FXCollections.observableArrayList();

    /**
     * @param hasFXGroupContents HasFXGroupContents whose items may be selected by this.
     */
    public HasFXGroupSelection(HasFXGroupContents hasFXGroupContents, String id) {
        this.id = id;
        this.hasFXGroupContents = hasFXGroupContents;
        this.hasFXGroupContents.getSelection().addListener(new ListChangeListener<MeshView>() {
            @Override
            public void onChanged(Change<? extends MeshView> c) {
                while (c.next()) {
                    for (MeshView meshView : c.getRemoved()) {
                        unselect(meshView);
                    }
                }
            }
        });
    }


    @Override
    public ObservableList<MeshView> getSelection() {
        return selection;
    }

    @Override
    public void setSelection(List<MeshView> selection) {
        clearSelection();   //important so that things arent duplicated
        this.selection.addAll(selection);
    }

    @Override
    public void select(MeshView item) {
        this.selection.add(item);
    }

    public void unselect(MeshView item) {
        this.selection.remove(item);
    }

    public void selectAll() {
        clearSelection();
        for (MeshView item : this.getAllItems()) {
            select(item);
        }
    }


    @Override
    public void clearSelection() {
        this.selection.clear();
    }

    /**
     * @return All items that this can possibly select. Meaning all items that are currently drawn by hasFXGroupContents.
     */
    @Override
    public List<MeshView> getAllItems() {
        return new LinkedList<MeshView>(hasFXGroupContents.getSelection());
    }

    public ObservableList<MeshView> getAllItemsObservable() {
        return hasFXGroupContents.getSelection();
    }

}
