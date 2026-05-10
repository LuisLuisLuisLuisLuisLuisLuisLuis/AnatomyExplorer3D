package Project.SelectionModel.FXGroupDraw;

import Project.SelectionModel.SelectionContainer;
import Project.SelectionModel.SelectionGroup;
import Project.window.ThreeDPaneHandling.Coloring.FileGroupingScheme;
import Project.window.ThreeDPaneHandling.OBJFile.OpenOBJ;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HasFXGroupContentsContainer extends SelectionContainer<MeshView, String> {

    private static final Logger logger = Logger.getLogger(HasFXGroupContentsContainer.class.getName());


    @Override
    public Set<String> getSelectionFormatted() {
        ObservableList<MeshView> selectedItems = hasSelection.getSelection();
        Set<String> selectionFormatted = new HashSet<>();
        for (Node node : selectedItems) {
            selectionFormatted.add(node.getId());
        }
        return selectionFormatted;
    }

    /**
     * This returns a mock MeshView with the correct id as this is often sufficient for HasFXGroupContents. Only when needed
     * will HasFXGroupContents request the actual MeshView via createMesh().
     * @param groupItem a FileID
     * @return MeshView with the FileID as id.
     */
    @Override
    public Collection<MeshView> transformGroupItemToSelectionItem(String groupItem) {

        MeshView mock = new MeshView();
        mock.setId(groupItem);
        return List.of(mock);

    }

    /**
     * @param hasSelection   : an object that has a selection
     * @param selectionGroup : a SelectionGroup for this container to be part of
     */
    //shouldnt be used because now that this constructor will actually call group.addSelectionContainer(this) the group
    //will instantly update the selection of this which will fail because the rest of the constructor has not been
    //executed yet, meaning theres no hasSelection or resource locations here.
    //leaving the code in as a bad example.
    public HasFXGroupContentsContainer(HasFXGroupContents hasSelection, @NotNull SelectionGroup<String> selectionGroup) {
        super(hasSelection, selectionGroup);

    }

    public HasFXGroupContentsContainer(HasFXGroupContents hasFXGroupContents) {
        super(hasFXGroupContents);
    }

}
