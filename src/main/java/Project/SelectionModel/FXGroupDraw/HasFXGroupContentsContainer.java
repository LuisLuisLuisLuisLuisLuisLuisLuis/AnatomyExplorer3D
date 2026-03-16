package Project.SelectionModel.FXGroupDraw;

import Project.SelectionModel.SelectionContainer;
import Project.SelectionModel.SelectionGroup;
import Project.window.ThreeDPaneHandling.Coloring.FileGroupingScheme;
import Project.window.ThreeDPaneHandling.OpenOBJ;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

public class HasFXGroupContentsContainer extends SelectionContainer<MeshView, String> {

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
        return Color.web(this.fileGroupingScheme.groupToColor.getOrDefault(group, "#789B73"));
    }


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
    protected Collection<MeshView> transformGroupItemToSelectionItem(String groupItem) {

        MeshView mock = new MeshView();
        mock.setId(groupItem);
        return List.of(mock);

    }

    public Node createMeshView(String groupItem) {

        Node node = this.hasFXGroupContents.getMeshViewWithID(groupItem);
        if (node != null) return node;


        InputStream inputStream = findTreeFileInResource(groupItem);    // try to find the item first in the resources
        if (inputStream == null) inputStream = findTreeFile(groupItem); // and then in the provided files if unfound.

        if (inputStream != null) {
            long startTime = System.nanoTime();
            String[] anatomicalGroups = this.fileGroupingScheme.fileIDstoGroups.getOrDefault(groupItem, null);
            String anatomicalGroup;
            if (anatomicalGroups != null ) {
                anatomicalGroup = anatomicalGroups[anatomicalGroupLevel];
            } else {
                System.out.println("could not find a color-group for " + groupItem + ", returning null.");
                anatomicalGroup = null;
            }
            //fileGroupingScheme.groupToColor returns hex values of the color (String) which must be converted to Color using Color.web(string)
            Node result = OpenOBJ.objInputStreamToMeshViews(inputStream, groupItem, getColorForGroup(anatomicalGroup));
            long endTime = System.nanoTime();
            long durationMillis = (endTime - startTime) / 1000000;
            System.out.println("transformGroupItemToSelecitonItem took " + durationMillis);
            return result;
        }
        else {
            System.out.println("HasFXGroupContentsContainer: inputstream is null");
            return null;
        }
    }

    /**
    This is a hack so I can access getNodeWithID().. shouldn't be used for other purposes
     */
    private final HasFXGroupContents hasFXGroupContents;

    /**
     * @param hasSelection   : an object that has a selection
     * @param selectionGroup : a SelectionGroup for this container to be part of
     */
    public HasFXGroupContentsContainer(HasFXGroupContents hasSelection, SelectionGroup<String> selectionGroup) {
        super(hasSelection, selectionGroup);
        this.hasFXGroupContents = hasSelection;
        this.fileDirs = new ArrayList<>(2);
        this.resourceLocations = new ArrayList<>(3);

        //TODO: NOT VIA FILE! VIA STREAM OR STH.
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
        for (URL fileLocation : resourceLocations) {

            try {inputStream = new URL(fileLocation, nameWithoutExtension + ".obj").openStream();
            } catch (IOException i) {continue;}

            if (inputStream != null) return inputStream;
        }
        return null;
    }


    /**
     * Tries to find a .obj file in either of the file folders of both trees.
     * @param nameWithoutExtension name of the .obj file but without .obj.
     * @return the file if it exists, else return null.
     * *is legacy code right now. might be useful later if I decide to allow user to Open user-provided .obj files.
     */
    public InputStream findTreeFile(String nameWithoutExtension) {

        for (File dir : fileDirs) {
            try {
                Path directoryPath = dir.toPath();
                Path modelPath = directoryPath.resolve(nameWithoutExtension + ".obj");
                URL fileURL = modelPath.toUri().toURL();
                return fileURL.openStream();
            }
            catch (IOException ignored) {continue;}
        }
        return null;
    }

    private void loadOBJs(File file) {
        File[] files = file.listFiles();
        if (files == null) return; //TODO: UI notification?
        for (File objfile : files) hasFXGroupContents.addOBJ(objfile.getName().substring(0, objfile.getName().lastIndexOf(".")));
    }
    private void loadOBJs(URL url) {
        loadOBJs(new File(url.getFile()));
    }

    private void removeOBJs(File file) {
        File[] files = file.listFiles();
        if (files == null) return;
        for (File objfile : files) hasFXGroupContents.removeOBJ(objfile.getName().substring(0, objfile.getName().lastIndexOf(".")));
    }
    private void removeOBJs(URL url) {
        removeOBJs(new File(url.getFile()));
    }

    private final ArrayList<File> fileDirs;
    public ArrayList<File> getFileDirs() {
        return fileDirs;
    }
    public void addFileDir(File dir) {
        fileDirs.add(dir); loadOBJs(dir);
    }
    public void removeFileDir(File dir) {
        fileDirs.remove(dir); removeOBJs(dir);}

    private final ArrayList<URL> resourceLocations;
    public ArrayList<URL> getResourceLocationsLocations() {
        return resourceLocations;
    }
    public void addResourceLocation(URL url) {
        for (URL url1: resourceLocations) if (url.getFile().equals(url1.getFile())) return;
        resourceLocations.add(url); loadOBJs(url);
    }
    public void removeResourceLocation(URL url) {
        resourceLocations.remove(url); removeOBJs(url);}

}
