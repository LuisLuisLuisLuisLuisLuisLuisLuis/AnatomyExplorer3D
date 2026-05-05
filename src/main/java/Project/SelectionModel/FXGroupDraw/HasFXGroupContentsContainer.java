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

    /**
     * Has a mapping of FileIDs (that come in as groupItems here) to anatomical group (String[]).
     * -> one fileID may be mapped to multiple anatomical groups.
     * Is set on Construction by loading resource .json.
     */
    private FileGroupingScheme fileGroupingScheme;
// You will still need this when you implement diff selection visualization modes, specifically color visualization
// (e.g. intense Red for selected) because you will need to be able to go back to default.
// Also, this schema may be needed for loading/saving sessions IF you still allow user to change colors.

    /**
     * The index to use when accessing anatomical group array returned by fileGroupingScheme.
     */
    private int anatomicalGroupLevel;
    public int getAnatomicalGroupLevel() {return anatomicalGroupLevel;}

    public static final String DEFAULT_COLOR_VAL = "#789B73";
    public static final Color DEFAULT_COLOR = Color.web(DEFAULT_COLOR_VAL);

    public Color getColorForGroup(String group) {
        return Color.web(this.fileGroupingScheme.groupToColor.getOrDefault(group, DEFAULT_COLOR_VAL));
    }

    public String[] findAnatomyGroupsForID(String id) {
        return this.fileGroupingScheme.fileIDstoGroups.getOrDefault(id, new String[anatomicalGroupLevel]);
    }

    public String findAnatomyGroupForID(String id) {
        String[] anatomicalGroups = findAnatomyGroupsForID(id);
        if (anatomicalGroups.length > anatomicalGroupLevel) {
            return anatomicalGroups[anatomicalGroupLevel];
        } else {
            logger.log(Level.FINER, "could not find a color-group for " + id + ", returning empty.");
            return "";
        }
    }

    /**
     * @param id ID of a MeshView
     * @return The default color for this id at the default anatomical group level
     */
    public Color getDefaultColorForID(String id) {
        return getColorForGroup(findAnatomyGroupForID(id));
    }

    /**
     * @param id ID of a MeshView
     * @param anatomicalGroupLevel anatomical group level
     * @return The default color for this id at that anatomical group level
     */
    public Color getDefaultColorForID(String id, int anatomicalGroupLevel) {
        return getColorForGroup(findAnatomyGroupsForID(id)[anatomicalGroupLevel]);
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
    public Collection<MeshView> transformGroupItemToSelectionItem(String groupItem) {

        MeshView mock = new MeshView();
        mock.setId(groupItem);
        return List.of(mock);

    }

    public MeshView createMeshView(String groupItem) {

        MeshView meshView = this.hasFXGroupContents.getMeshViewWithID(groupItem);
        if (meshView != null) return meshView;


        InputStream inputStream = findTreeFileInResource(groupItem);    // try to find the item first in the resources
        if (inputStream == null) inputStream = findTreeFile(groupItem); // and then in the provided files if unfound.

        if (inputStream != null) {
            long startTime = System.nanoTime();
            String anatomicalGroup = findAnatomyGroupForID(groupItem);
            //fileGroupingScheme.groupToColor returns hex values of the color (String) which must be converted to Color using Color.web(string)
            MeshView result = OpenOBJ.objInputStreamToMeshViews(inputStream, groupItem, getColorForGroup(anatomicalGroup));
            long endTime = System.nanoTime();
            long durationMillis = (endTime - startTime) / 1000000;
            logger.log(Level.FINEST, "creating meshview took " + durationMillis);
            return result;
        }
        else {
            logger.log(Level.WARNING, "inputstream is null");
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
    //shouldnt be used because now that this constructor will acutally call group.addSelectionContainer(this) the group
    //will instantly update the selection of this which will fail because the rest of the constructor has not been
    //executed yet, meaning theres no hasSelection or resource locations here.
    public HasFXGroupContentsContainer(HasFXGroupContents hasSelection, @NotNull SelectionGroup<String> selectionGroup) {
        super(hasSelection, selectionGroup);
        this.hasFXGroupContents = hasSelection;
        this.fileDirs = new ArrayList<>(2);
        this.resourceLocations = new ArrayList<>(3);

        //TODO: NOT VIA FILE! VIA STREAM OR STH.
        try {
            this.fileGroupingScheme = new ObjectMapper().readValue(
                    new File(this.getClass().getResource("/Project/3DSupport/fileGroupingScheme_manual_V5.json").getFile().substring(1)),
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

    public HasFXGroupContentsContainer(HasFXGroupContents hasFXGroupContents) {
        super(hasFXGroupContents);
        this.hasFXGroupContents = hasFXGroupContents;
        this.fileDirs = new ArrayList<>(2);
        this.resourceLocations = new ArrayList<>(3);

        //TODO: NOT VIA FILE! VIA STREAM OR STH.
        try {
            this.fileGroupingScheme = new ObjectMapper().readValue(
                    new File(this.getClass().getResource("/Project/3DSupport/fileGroupingScheme_manual_V5.json").getFile().substring(1)),
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


    private final SimpleBooleanProperty isLoadingOBJs = new SimpleBooleanProperty(false);

    /**
     * @return Are there OBJs being loaded on a different thread right now?
     */
    public SimpleBooleanProperty getIsLoadingOBJs() {return isLoadingOBJs;}

    private Function<Task<List<MeshView>>, Object> handleLoadingTask = null;

    /**
     * Set a function to handle the Task of loading OBJ files. For example to put it on a new Thread or to bind UI to the task.<P>
     *     If set, this class WILL NOT run() the task anymore! This must be done by the provided function.
     * <P>
     *     The task updates progress and has a message.
     * @param handleLoadingTask a function that accepts the task. Set to null to make this class run tasks again itself (not on a new Thread).
     */
    public void setHandleLoadingTask(Function<Task<List<MeshView>>, Object> handleLoadingTask) {
        this.handleLoadingTask = handleLoadingTask;
    }

    private void loadOBJsMulti(File file, Set<String> whiteList) {
        File[] files = file.listFiles();
        if (files == null) return;
        Task<List<MeshView>> task = new Task<>() {

            @Override
            protected List<MeshView> call() throws Exception {
                isLoadingOBJs.set(true);
                updateMessage("Loading OBJ files...");
                ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                List<Future<MeshView>> meshViewF = new ArrayList<>(files.length);
                for (File file1 : files) {
                    if (!file1.isFile()) continue;
                    if (!file1.getName().endsWith(".obj")) continue;
                    String name = file1.getName().substring(0, file1.getName().lastIndexOf("."));
                    if (!whiteList.contains(name)) continue;
                    if (hasFXGroupContents.getMeshViewWithID(name) != null) continue;
                    meshViewF.add(executor.submit(() -> createMeshView(name)));
                }
                executor.shutdown();

                List<MeshView> result = new ArrayList<>(meshViewF.size());

                int total = meshViewF.size();
                int done = 0;

                for (Future<MeshView> f : meshViewF) {
                    result.add(f.get());
                    updateProgress(++done, total);
                }
                return result;
            }
        };

        task.setOnSucceeded(e -> {
            logger.log(Level.CONFIG, "loading OBJs task done");
            hasFXGroupContents.addMeshViews(task.getValue());
            isLoadingOBJs.set(false);
        });

        if (handleLoadingTask != null) handleLoadingTask.apply(task);
        else task.run();
    }

    private void loadOBJs(File file, Set<String> whiteList) {
        loadOBJsMulti(file, whiteList);
    }

    private void loadOBJs(URL url, Set<String> whiteList) {
        loadOBJs(new File(url.getFile()), whiteList);
    }

//    private void removeOBJs(File file) {
//        File[] files = file.listFiles();
//        if (files == null) return;
//        for (File objfile : files) {
//            if (!objfile.getName().endsWith(".obj")) continue;
//            hasFXGroupContents.removeOBJ(objfile.getName().substring(0, objfile.getName().lastIndexOf(".")));
//        }
//    }
//
//    private void removeOBJs(URL url) {
//        removeOBJs(new File(url.getFile()));
//    }


    public ArrayList<File> getFileDirs() {
        return fileDirs;
    }
    public void addFileDir(File dir, Set<String> whiteList) {
        fileDirs.add(dir);
        loadOBJs(dir, whiteList);
    }
    public void removeFileDir(File dir) {
        fileDirs.remove(dir);
//        removeOBJs(dir);
    }

    // THEORETICALLY the location lists could also be sets. BUT if 2 models share the same resource location, then removing one of them
    // would remove the location for both, possibly breaking resource loading (not actually since OBJs are now created only once at
    // startup but still, it would be wrong). leaving it as a list has the advantage that locations will occur as often as the number of models
    // that use them, circumventing this error.
    // Problem now:
    private final ArrayList<File> fileDirs;
    private final ArrayList<URL> resourceLocations;

    public ArrayList<URL> getResourceLocationsLocations() {return resourceLocations;}

    public void addResourceLocation(URL url, Set<String> whiteList) {
//        for (URL url1: resourceLocations) if (url.getFile().equals(url1.getFile())) return;
        resourceLocations.add(url);
        loadOBJs(url, whiteList);
    }

    public void removeResourceLocation(URL url) {resourceLocations.remove(url); //removeOBJs(url);
    }


    public static void printNrFaces(List<MeshView> meshViews, String msg) {
        int counter = 0;
        for (MeshView meshView : meshViews) {
            if (meshView.getMesh() instanceof TriangleMesh triangleMesh) counter += triangleMesh.getFaces().size();
        }
        System.out.println(counter + " faces " + msg);
    }

}
