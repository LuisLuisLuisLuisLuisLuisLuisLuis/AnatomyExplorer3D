package Project.SelectionModel.FXGroupDraw;

import Project.SelectionModel.HasSelection;
import Project.SelectionModel.SelectionContainer;
import Project.SelectionModel.SelectionGroup;
import Project.window.ThreeDPaneHandling.Coloring.FileGroupingScheme;
import Project.window.ThreeDPaneHandling.OpenOBJ;
import Project.window.WindowPresenter;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class HasFXGroupContentsContainer extends SelectionContainer<Node, String> {

    /**
     * Has a mapping of FileIDs (that come in as groupItems here) to anatomical group (String[]).
     * -> one fileID may be mapped to multiple anatomical groups.
     * Is set on Construction by loading resource .json.
     */
    private FileGroupingScheme fileGroupingScheme;
    /**
     * The index to use when accessing anatomical group array returned by fileGroupingScheme.
     */
    private int anatomicalGroupLevel;

    public Color getColorForGroup(String group) {
        System.out.println("Getting color for Group " + group);
        return Color.web(this.fileGroupingScheme.groupToColor.getOrDefault(group, "#789B73"));
    }


    @Override
    public Set<String> getSelectionFormatted() {
        ObservableList<Node> selectedItems = hasSelection.getSelection();
        Set<String> selectionFormatted = new HashSet<>();
        for (Node node : selectedItems) {
            selectionFormatted.add(node.getId());
        }
        return selectionFormatted;
    }

    @Override
    protected Collection<Node> transformGroupItemToSelectionItem(String groupItem) {

        InputStream inputStream = findTreeFileInResource(groupItem);
        LinkedList<Node> result = new LinkedList<>();

        if (inputStream != null) {
            String[] anatomicalGroups = this.fileGroupingScheme.fileIDstoGroups.getOrDefault(groupItem, null);
            String anatomicalGroup;
            if (anatomicalGroups != null ) {
                anatomicalGroup = anatomicalGroups[anatomicalGroupLevel];
            } else {
                System.out.println("could not find a color-group for " + groupItem + ", returning null.");
                anatomicalGroup = null;
            }
            //fileGroupingScheme.groupToColor returns hex values of the color (String) which must be converted to Color using Color.web(string)
            result.add(OpenOBJ.objInputStreamToMeshViews(inputStream, groupItem, getColorForGroup(anatomicalGroup)));
            return result;
        }
        else {
            Node meshView = new MeshView();
            meshView.setId(groupItem);  //returning this mock meshView with the groupItem as ID so that the downstream methods
            result.add(meshView);      // can still search for the groupitem
            return result;              //TODO: if else branch runs, nothing will be drawn but item will still be added
        }                               // to selection.
    }                                   // When does else-branch run?



    /**
     * @param hasSelection   : an object that has a selection
     * @param selectionGroup : a SelectionGroup for this container to be part of
     */
    public HasFXGroupContentsContainer(HasSelection<Node> hasSelection, SelectionGroup<String> selectionGroup) {
        super(hasSelection, selectionGroup);
        this.fileLocations = new ArrayList<>(2);
        this.resourceLocations = new ArrayList<>(2);
        //in constructor: , String[] fileLocations
        //this.fileLocations.add(fileLocations[0]);
        //this.fileLocations.add(fileLocations[1]);
        try {
            this.fileGroupingScheme = new ObjectMapper().readValue(
                    new File(this.getClass().getResource("/Project/3DSupport/fileGroupingScheme_manual_V4.json").getFile().substring(1)),
                    FileGroupingScheme.class
            );
            this.anatomicalGroupLevel = 0;
//            this.fileIDtoColorMapping = new ObjectMapper().readValue(
//                    new File(this.getClass().getResource("/Project/3DSupport/fileIDtoGroup_perplex_MOD120125.json").getFile().substring(1)),
//                    new TypeReference<HashMap<String, String>>() {
//                    }
//            );
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

    }

    /**
     * Tries to find a .obj resource in one of the resource locations.
     * @param nameWithoutExtension name of the .obj file but without .obj.
     * @return the Inputstream if it exists, else return null.
     */
    public InputStream findTreeFileInResource(String nameWithoutExtension) {
        InputStream inputStream;
        for (String fileLocation : resourceLocations) {
            inputStream = WindowPresenter.class.getResourceAsStream(fileLocation + nameWithoutExtension + ".obj");
            if (inputStream != null) {
                return inputStream;
            }
        }
        return null;
    }

    /**
     * Tries to find a .obj file in either of the file folders of both trees.
     * @param nameWithoutExtension name of the .obj file but without .obj.
     * @return the file if it exists, else return null.
     * *is legacy code right now. might be useful later if I decide to allow user to Open user-provided .obj files.
     */
    public File findTreeFile(String nameWithoutExtension) {
        File file;
        for (String fileLocation : fileLocations) {
            file = new File(fileLocation + File.separator + nameWithoutExtension + ".obj");
            if (file.exists()) return file;
        }
        return null;
    }
    /*
    /**
     * Stores where the two folders with the .obj files are located.
     */
    private ArrayList<String> fileLocations;
    public ArrayList<String> getFileLocations() {
        return fileLocations;
    }
    public void addFileLocation(String location) {
        fileLocations.add(location);
    }


    /**
     * Stores where the two folders with the .obj files are located.
     */
    private ArrayList<String> resourceLocations;
    public ArrayList<String> getResourceLocationsLocations() {
        return resourceLocations;
    }
    public void addResourceLocation(String location) {
        resourceLocations.add(location);
    }

}
