package Project.SelectionModel.FXGroupDraw;

import Project.SelectionModel.HasSelection;
import Project.window.ThreeDPaneHandling.Coloring.FileGroupingScheme;
import Project.window.ThreeDPaneHandling.OBJFile.OpenOBJ;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
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
     * This class resets MeshView.UserData upon selecting it!
     * @param group Group to which MeshViews will be added.
     * @param id ID of this
     */
    public HasFXGroupContents(Group group, String id) {
        this.group = group;
        this.id = id;
        this.fileDirs = new ArrayList<>(2);
        this.resourceLocations = new ArrayList<>(3);
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


    /**
     * Has a mapping of FileIDs (that come in as groupItems here) to anatomical group (String[]).
     * -> one fileID may be mapped to multiple anatomical groups.
     * Is set on Construction by loading resource .json.
     */
    private FileGroupingScheme fileGroupingScheme;
// You will still need this when you implement diff selection visualization modes, specifically color visualization
// (e.g. intense Red for selected) because you will need to be able to go back to default.
// Also, this schema may be needed for loading/saving sessions IF you still allow user to change colors.

    {
        try {
            this.fileGroupingScheme = new ObjectMapper().readValue(this.getClass().getResourceAsStream("/Project/3DSupport/fileGroupingScheme_manual_V6_obj.json"), FileGroupingScheme.class);
            this.anatomicalGroupLevel = 0;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load fileGroupingScheme, e");
        }
    }

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


    /**
     *
     * @param resourceDir directory in resources containing the files. Has to be usable via getClass().getResource(...)
     * @param whiteList Set of FileIDs that can be found in the directory of the given resourceDir
     */
    private void loadOBJsMulti(String resourceDir, Set<String> whiteList) {
        Task<List<MeshView>> task = new Task<>() {

            @Override
            protected List<MeshView> call() throws Exception {
                isLoadingOBJs.set(true);
                updateMessage("Loading OBJ files...");
                ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                List<Future<MeshView>> meshViewF = new ArrayList<>(whiteList.size());
                for (String fileID : whiteList) {
                    if (getMeshViewWithID(fileID) != null) continue;
                    meshViewF.add(executor.submit(() -> createMeshView(getClass().getResourceAsStream(resourceDir + (fileID.endsWith(".obj") ? fileID : fileID + ".obj")), fileID)));
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
            addMeshViews(task.getValue());
            isLoadingOBJs.set(false);
        });

        if (handleLoadingTask != null) handleLoadingTask.apply(task);
        else task.run();
    }


    private void loadOBJsMulti(File dir, Set<String> whiteList) {
        appendFileExtension(whiteList);
        Task<List<MeshView>> task = new Task<>() {

            @Override
            protected List<MeshView> call() throws Exception {
                File[] files = dir.listFiles();

                isLoadingOBJs.set(true);
                updateMessage("Loading OBJ files...");
                ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                List<Future<MeshView>> meshViewF = new ArrayList<>(whiteList.size());
                for (File file : files) {
                    if (!file.isFile()) continue;
                    if (!file.getName().endsWith(".obj")) continue;
                    String name = file.getName();
                    logger.log(Level.CONFIG, name + "!!");
                    if (!whiteList.contains(name)) continue;
                    if (getMeshViewWithID(name) != null) continue;
                    meshViewF.add(executor.submit(() -> createMeshView(file.toURI().toURL().openStream(), name)));
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
            addMeshViews(task.getValue());
            isLoadingOBJs.set(false);
        });

        if (handleLoadingTask != null) handleLoadingTask.apply(task);
        else task.run();
    }


    private void loadOBJs(File file, Set<String> whiteList) {
        loadOBJsMulti(file, whiteList);
    }

    private void loadOBJs(String url, Set<String> whiteList) {
        loadOBJsMulti(url, whiteList);
    }


    public ArrayList<File> getFileDirs() {
        return fileDirs;
    }

    public void addFileDir(File dir, Set<String> whiteList) {
        if (!dir.isDirectory()) throw new IllegalArgumentException("Provided path is not a directory: " + dir.toString());
        fileDirs.add(dir);
        loadOBJs(dir, whiteList);
    }
    public void removeFileDir(File dir) {
        fileDirs.remove(dir);
    }

    // THEORETICALLY the location lists could also be sets. BUT if 2 models share the same resource location, then removing one of them
    // would remove the location for both, possibly breaking resource loading (not actually since OBJs are now created only once at
    // startup but still, it would be wrong). leaving it as a list has the advantage that locations will occur as often as the number of models
    // that use them, circumventing this error.
    // Problem now:
    private final ArrayList<File> fileDirs;
    private final ArrayList<String> resourceLocations;

    public ArrayList<String> getResourceLocationsLocations() {return resourceLocations;}

    /**
     * Add a directory from the resources. Will be used via getClass().getResource()
     * @param resourceDir the path to the directory, like '/path/to/dir/'. slash at the front important.
     * @param whiteList files to be loaded from this directory
     */
    public void addResourceLocation(String resourceDir, Set<String> whiteList) {
        if (!resourceDir.endsWith("/")) resourceDir += "/";
        logger.log(Level.CONFIG, "adding resource location " + resourceDir);
        resourceLocations.add(resourceDir);
        loadOBJs(resourceDir, whiteList);
    }

    public void removeResourceLocation(String location) {resourceLocations.remove(location); //removeOBJs(url);
    }

    /** maybe doesnt work reliably*/
    public static String printNrFaces(List<MeshView> meshViews) {
        int counter = 0;
        for (MeshView meshView : meshViews) {
            if (meshView.getMesh() instanceof TriangleMesh triangleMesh) counter += triangleMesh.getFaces().size();
        }
        return (counter + " faces");
    }

    /**
     *
     * @param inputStream InputStram from which the MeshView will be created.
     * @param id ID that will be given to the MeshView. Will also be treated as FileName to search for corresponding
     *           .png files to apply to the MeshView. If no such image file exists, the meshview will receive a color.
     * @return The MeshView or null if the inputstream is null or if an error occurred when parsing the file.
     */
    public MeshView createMeshView(InputStream inputStream, String id) {

        long startTime = System.nanoTime();
        String anatomicalGroup = findAnatomyGroupForID(id);

        //fileGroupingScheme.groupToColor returns hex values of the color (String) which must be converted to Color using Color.web(string)
        MeshView result = OpenOBJ.objInputStreamToMeshViews(inputStream, id, getColorForGroup(anatomicalGroup));
        long endTime = System.nanoTime();
        long durationMillis = (endTime - startTime) / 1000000;
        logger.log(Level.FINEST, "creating meshview took " + durationMillis);
        return result;
    }

    /**
     * Appends '.obj' to every entry that does not contain '.'.
     * Because every fileID must correspond to a file name and file names have extensions.
     * So this class assumes that if a name without extension is given it refers to an OBJ file with that name.
     */
    public static void appendFileExtension(Set<String> fileIDs) {
        for (String fileID : new HashSet<>(fileIDs)) if (!fileID.contains(".")) {
            fileIDs.remove(fileID);
            fileIDs.add(fileID.trim() + ".obj");
        }
    }

}
