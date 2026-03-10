package Project.window;

import Project.AI.AIService;
import Project.AI.AISystemPrompt;
import Project.AI.Format.OpenAIMessage;
import Project.AI.QnA;
import Project.SelectionModel.FXGroupDraw.HasFXGroupContents;
import Project.SelectionModel.FXGroupDraw.HasFXGroupContentsContainer;
import Project.SelectionModel.FXGroupSelection.FXGroupSelectionContainer;
import Project.SelectionModel.FXGroupSelection.HasFXGroupSelection;
import Project.SelectionModel.HasGroup.HasGroup;
import Project.SelectionModel.HasGroup.HasTreeSelectionGroupContainer;
import Project.SelectionModel.Mediator.SelectionMediator_Tree_3D;
import Project.SelectionModel.Tree.HasTreeView;
import Project.SelectionModel.Tree.TreeViewSelectionContainer;
import Project.command.Command;
import Project.command.DrawCommands.*;
import Project.command.DrawCommands.DrawAnatomyCommands.DrawItemIn3DCommand;
import Project.command.DrawCommands.DrawAnatomyCommands.RemoveObjFrom3DCommand;
import Project.command.Remember.RememberFXGroupContents;
import Project.command.Remember.RememberFXMeshViewColors;
import Project.command.TreeCommands.CollapseTreeViewCommand;
import Project.command.TreeCommands.ExpandTreeViewCommand;
import Project.command.TreeCommands.SelectAllTreeViewCommand;
import Project.command.TreeCommands.SelectNoneTreeViewCommand;
import Project.model.ANode;
import Project.model.Model;
import Project.model.TreeExport;
import Project.window.PopUp.About;
import Project.window.PopUp.Help;
import Project.window.PopUp.InfoChart;
import Project.window.PopUp.LittlePopUp;
import Project.window.Slicing.Plane;
import Project.window.SupportingUI.FileExplorerInteraction;
import Project.window.SupportingUI.TextSearch.SearchTree;
import Project.window.ThreeDPaneHandling.*;
import Project.SelectionModel.*;

import Project.window.TreeView.TreeAnalysis.TreeAnalysisUtils;
import Project.window.TreeView.TreeViewSetup;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import javafx.scene.text.Text;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static Project.window.TreeView.TreeAnalysis.TreeAnalysisUtils.replaceANodeByCopy;


/**
 * Connects UI to classes.
 */
public class WindowPresenter {

    private final MainWindowController controller; //holds all components of the UI (buttons etc)
    private final ObservableList<Command> undoList = FXCollections.observableList(new LinkedList<>()); //undo-redo
    private final ObservableList<Command> redoList = FXCollections.observableList(new LinkedList<>());


    private static final PerspectiveCamera camera = new PerspectiveCamera(true); //camera in 3D view
    private static final Point3D initialCameraPosition = new Point3D(0,-10,-1500);

    private static final Affine initialTransform = new Affine( //initial transform of the group holding the 3D contents. serves as reset position
            1.0, 0.0, 0.0, 0.0,
            0.0, 0.0, -1.0, 0.0,
            0.0, 1.0, 0.0, 0.0
    );

    // these two doubles decide how large each step of zoom (via scrolling) or rotation is
    private static final double zoomStep = 4;
    private static final double rotationStep = 2;

    private final Group root3d;         //root group of the 3d view holding the groups below.
    private final Group slicePlaneGroup;//will hold plane for slicing meshes
    private final Group contentGroup;   //holds the innerGroup
    private final Group innerGroup;     //holds the meshViews

    private final CamMover camMover = new CamMover(camera); // controls camera movement
    private final Group3DRotation contentGroupRotator;      // implements rotation of the 3D view via rotation of a group
    private final Group3DRotation slicePlaneGroupRotator;   // rotates the slicing plane
    private final SetupMouseRotate3D setupMouseRotate3D;    // uses groupRotater to rotate a group using mouse drag
    private boolean isRotating = false; //is the user rotating the 3D view via mouse drag right now? if so, disable click-selecting meshViews


    private final ArrayList<TreeView<ANode>> treeViews;
    private final ArrayList<SearchTree> searchTrees;
    private final SelectionGroup<ANode> treeViewSelectionGroup;
    private final ArrayList<TreeViewSelectionContainer> treeViewSelectionContainers;

    private final HasFXGroupContents hasInnerGroupContents;
    private final SelectionGroup<String> threeDContentGroup;
    private final HasFXGroupContentsContainer hasTheeDContentsContainer;
    private final SelectionGroup<String> threeDSelectionGroup;

    private final ArrayList<SelectionMediator_Tree_3D> selectionMediatorTree3DS;
    private final SelectionMediator_Tree_3D selectionMediatorTree3D_Selection;
    private final SelectionMediator_Tree_3D selectionMediatorTree_3D_Content;

    private final TreeViewSetup treeViewSetup = new TreeViewSetup();

    private final ArrayList<Tab> tabs;

    private final ArrayList<Model> models;

    private final AIService aiService;  //implements AI support

    /**
     *
     * @param controller Holds all UI FXML components used by this presenter.
     */
    public WindowPresenter(MainWindowController controller) {

        this.controller = controller;

        //initialize the contentGroup via Setup3DSubPane
        LinkedList<Group> outerGroups = Setup3DSubPane.setup3DSubPane(controller.getThreeDPane(), camera, initialCameraPosition);
        root3d = outerGroups.get(0);    // root3d holds contentGroup.
                                        // If you want to add more content that is independet from the meshes (i.e. independent from innerGroup),
                                        // add a new group to root3d and put it in there. like for example the slice pane.
                                        // It will not be affected by clearing the view or any rotation, mouse drag etc unless you add that group to
                                        // specific methods (like SetupMouseRotate3D).
        slicePlaneGroup = new Group();
        root3d.getChildren().add(this.slicePlaneGroup);
        contentGroup = outerGroups.get(1);
        this.innerGroup = new Group();


        // Rotation
        this.contentGroupRotator = new Group3DRotation(contentGroup);
        this.slicePlaneGroupRotator = new Group3DRotation(slicePlaneGroup);
        setupMouseRotate3D = new SetupMouseRotate3D(controller.getThreeDPane(), new Group[]{contentGroup, slicePlaneGroup});

        //mouse scrolling functionality
        MouseScrolling3D mouseScrolling3D = new MouseScrolling3D(new Group3DRotation[] {contentGroupRotator, slicePlaneGroupRotator},controller.getThreeDPane());

        innerGroup.getChildren().addListener((InvalidationListener) e -> centerGroupToItself(innerGroup));

        contentGroup.getChildren().add(innerGroup);
        contentGroup.getTransforms().setAll(initialTransform);


        //little bug-avoidance in case i add some objects at some point and forget to give them an ID. null ID will throw exceptions.
        //im not sure if this is ever called (it shouldnt be)
        innerGroup.getChildren().addListener(new ListChangeListener<Node>() {
            @Override
            public void onChanged(Change<? extends Node> c) {
                while (c.next()) {
                    if (c.wasAdded()) {
                        for (Node node : c.getAddedSubList()) {
                            if (node.getId() == null) node.setId("null");
                        }
                    }
                }
            }
        });

        //----------------create the models----------------

        this.models = new ArrayList<>();

        Model mainModel = new Model(
                getClass().getResourceAsStream("/Project/anatomy/partof_inclusion_relation_list.txt"),
                getClass().getResourceAsStream("/Project/anatomy/partof_element_parts.txt"),
                "anatomy",
                WindowPresenter.class.getResource("/Project/anatomy/BP3D_4.0_obj_99/"));

        Model partof_v3 = new Model(
                getClass().getResourceAsStream("/Project/anatomy/bp3d_v3_conventional_partof.txt"),
                getClass().getResourceAsStream("/Project/anatomy/bp3d_v3_parts_v4_format_cleaned.txt"),
                "BP3D 3.0 part-of",
                getClass().getResource("/Project/anatomy/BodyParts3D_3.0_obj_99")
                );
        Model isa_v3 = new Model(
                getClass().getResourceAsStream("/Project/anatomy/bp3d_v3_composite_isa.txt"),
                getClass().getResourceAsStream("/Project/anatomy/bp3d_v3_parts_v4_format_cleaned.txt"),
                "BP3D 3.0 is-a",
                getClass().getResource("/Project/anatomy/BodyParts3D_3.0_obj_99")
                );

        this.models.add(mainModel);
        this.models.add(partof_v3);
        this.models.add(isa_v3);
        //--------initialize tree selection model----------
        this.treeViewSelectionGroup = new SelectionGroup<>("treeViewSelectionGroup");
        this.treeViewSelectionContainers = new ArrayList<>(this.models.size());
        this.searchTrees = new ArrayList<>(this.models.size());
        this.selectionMediatorTree3DS = new ArrayList<>(3);
        this.treeViews = new ArrayList<>(this.models.size());
        this.tabs = new ArrayList<>(3);
        //-----------------------------


        //bind undo redo buttons
        controller.getUndoButton().setOnAction(e -> undoCommand());
        controller.getUndoButton().disableProperty().bind(Bindings.isEmpty(undoList));
        controller.getRedoButton().setOnAction(e -> redoCommand());
        controller.getRedoButton().disableProperty().bind(Bindings.isEmpty(redoList));

        //select all, select none button
        controller.getTreeSelectAllButton().setOnAction(e -> executeCommand(new SelectAllTreeViewCommand(getSelectedTreeView())));
        controller.getTreeSelectNoneButton().setOnAction(e -> executeCommand(new SelectNoneTreeViewCommand(getSelectedTreeView())));

        //menu: File
        MenuItem searchForDups = new MenuItem("Search for Dups");
        searchForDups.setOnAction(e -> {searchForDupIDs( treeViews.get(controller.getTreeTabPane().getSelectionModel().getSelectedIndex()).getRoot());});
        controller.getTopMenuBar().getMenus().getFirst().getItems().add(searchForDups);
        controller.getMenuClose().setOnAction(e -> Platform.exit());
//        controller.getMenuSaveTree().setOnAction(e -> TreeExport.writeToFiles(getSelectedTreeView(), new File(partOfFilesLocation.get()).getParent()));
        controller.getMenuSaveTree().setOnAction(e -> {
            TreeExport.saveTrees(treeViews.stream().toList(), models.stream().map(Model::getName).toList());
        });

        controller.getLoadBP3DV3checkMenu().setUserData(false); //a flag to prevent this listener from actioning
        controller.getLoadBP3DV3checkMenu().selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if ((boolean) controller.getLoadBP3DV3checkMenu().getUserData()) return;
                if (newValue) {
                    executeCommand(new AddModelCommand(List.of(partof_v3, isa_v3)));
                }
                else {
                    executeCommand(new RemoveModelCommand(List.of(partof_v3, isa_v3)));
                }
            }
        });
        controller.getTreeTabPane().getTabs().addListener(new InvalidationListener() {  //if BP3D 3.0 is added/removed via undo/redo the menu must still show the correct check.
            @Override
            public void invalidated(Observable observable) {
                if (tabs.stream().filter(tab -> tab.getText().equals("BP3D 3.0 part-of") || tab.getText().equals("BP3D 3.0 is-a")).toList().size() == 2) {
                    controller.getLoadBP3DV3checkMenu().setUserData(true);
                    controller.getLoadBP3DV3checkMenu().setSelected(true);
                } else {
                    controller.getLoadBP3DV3checkMenu().setUserData(true);
                    controller.getLoadBP3DV3checkMenu().setSelected(false);
                }
                controller.getLoadBP3DV3checkMenu().setUserData(false);
            }
        });

        controller.getEnableBP3DPartsCheckmenu().selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

            }
        });

        //removed axes for now because it doesn't look good with the anatomy parts
        //controller.getMenuAddAxes().setOnAction(e -> executeCommand(new AddAxesCommand(innerGroup)));
        //controller.getMenuRmAxes().setOnAction(e -> executeCommand(new RemoveAxesCommand(innerGroup)));

        //3D related buttons

        controller.getMoveDownButton().setOnAction(e -> rotationControl(KeyCode.DOWN, false));
        controller.getMoveUpButton().setOnAction(e -> rotationControl(KeyCode.UP, false));
        controller.getMoveRightButton().setOnAction(e -> rotationControl(KeyCode.RIGHT, false));
        controller.getMoveLeftButton().setOnAction(e -> rotationControl(KeyCode.LEFT, false));
        controller.getZoomInButton().setOnAction(e -> rotationControl(KeyCode.PLUS, false));
        controller.getZoomOutButton().setOnAction(e -> rotationControl(KeyCode.MINUS, false));

        controller.getRotDownButton().setOnAction(e -> rotationControl(KeyCode.DOWN, true));
        controller.getRotUpButton().setOnAction(e -> rotationControl(KeyCode.UP, true));
        controller.getRotRightButton().setOnAction(e -> rotationControl(KeyCode.RIGHT, true));
        controller.getRotLeftButton().setOnAction(e -> rotationControl(KeyCode.LEFT, true));
        controller.getTiltAntiClButton().setOnAction(e -> rotationControl(KeyCode.MINUS, true));
        controller.getTiltClockwButton().setOnAction(e -> rotationControl(KeyCode.PLUS, true));


        //this implements undoable scrolling. but i choose to take it out because its imperfect.
        /*
        mouseScrolling3D.setListener(() -> {
            //System.out.println("Listening");
            executeCommand(new Mouse3DScrollCommand(mouseScrolling3D.getCamPositionPrev(), mouseScrolling3D.getCamPositionPost(), camera));
        });
         */


        //---------------initialize 3D selection model-----------
        threeDSelectionGroup = new SelectionGroup<>("3dSelectionGroup");
        HasFXGroupSelection hasInnerGroupSelectedItems = new HasFXGroupSelection(innerGroup, "hasInnerGroupSelectedItems");
        FXGroupSelectionContainer threeDSelectionContainer = new FXGroupSelectionContainer(
                hasInnerGroupSelectedItems, threeDSelectionGroup);
        threeDSelectionGroup.addSelectionContainer(threeDSelectionContainer);
        //-----------------------------------------------

        //-------------initialize selection model for 3D contents (so not 3D selection but what meshViews are drawn)
        this.hasInnerGroupContents = new HasFXGroupContents(innerGroup, "hasinnergroupMeshes");
        this.threeDContentGroup = new SelectionGroup<String>("3dContentGroup");
        this.hasTheeDContentsContainer = new HasFXGroupContentsContainer(hasInnerGroupContents, threeDContentGroup); //, new String[]{isAFilesLocation.getValue().getAbsolutePath(), partOfFilesLocation.getValue().getAbsolutePath()}
        this.hasInnerGroupContents.setHasFXGroupContentsContainer(hasTheeDContentsContainer);
        threeDContentGroup.addSelectionContainer(hasTheeDContentsContainer);
        hasTheeDContentsContainer.addResourceLocations(models.stream().map(Model::getFilesDirURL).collect(Collectors.toList()));
        //---------------------------

        //mediator between tree selection and 3D content
        this.selectionMediatorTree_3D_Content = new SelectionMediator_Tree_3D(treeViewSelectionGroup, threeDContentGroup);

        //mediator between tree selection and 3D selection
        this.selectionMediatorTree3D_Selection = new SelectionMediator_Tree_3D(treeViewSelectionGroup, threeDSelectionGroup);

        //------------load main model------------
        loadModel(mainModel);
        makeModelVisible(mainModel);
        //---------------------------------------


        //listen to TreeViewSetup for cut, paste and delete events.
        //these necessitate that the mediator reloads his mapping of FileIDs to ANodes.
        treeViewSetup.setOnEvent(new Runnable() {
            @Override
            public void run() {
                selectionMediatorTree3D_Selection.reloadDicts();
            }
        });

        //---------enable the mediator------------------
        controller.getSelectIn3DButton().setOnAction(e -> {
//            selectionMediatorTree3D_Selection.pushAtoB();
            TreeViewSelectionContainer treeViewSelectionContainer = null;
            for (TreeViewSelectionContainer treeViewSelectionContainer1 : this.treeViewSelectionContainers) if (treeViewSelectionContainer1.getId().equals(getSelectedTreeView().getId())) treeViewSelectionContainer = treeViewSelectionContainer1;
            if (treeViewSelectionContainer == null) {return;}
            threeDSelectionGroup.changeSelection(selectionMediatorTree3D_Selection.transformAselectionToBSelection(treeViewSelectionContainer.getSelectionFormatted()), true, false);
        });
        controller.getShowInTreeButton().setOnAction(e -> {
//            selectionMediatorTree3D_Selection.pushBtoA(); unfortunately not correct if there are ANodes with same id but different files in multiple trees.
            TreeViewSelectionContainer treeViewSelectionContainer = null;
            for (TreeViewSelectionContainer treeViewSelectionContainer1 : this.treeViewSelectionContainers) if (treeViewSelectionContainer1.getId().equals(getSelectedTreeView().getId())) treeViewSelectionContainer = treeViewSelectionContainer1;
            if (treeViewSelectionContainer == null) {return;}
            SelectionMediator_Tree_3D selectionMediatorTree3D = getSelectionMediatorTree3D(treeViewSelectionContainer.getId());
            selectionMediatorTree3D = selectionMediatorTree3D != null ? selectionMediatorTree3D : selectionMediatorTree3D_Selection;
            treeViewSelectionContainer.changeSelection(selectionMediatorTree3D.transformBSelectionToASelection(threeDSelectionGroup.getSelection()), true, false);
            treeViewSelectionContainer.updateGroup();
        });

        controller.getSelectIn3DButton().disableProperty().bind(controller.getSelectionSynchCheck().selectedProperty());
        controller.getShowInTreeButton().disableProperty().bind(controller.getSelectionSynchCheck().selectedProperty());
        //--------------------------------


        //---------------implement 3D mouse selection------------------
        innerGroup.setOnMouseReleased(event -> {
            if (isRotating) {
                isRotating = false; //dont de-/select when you were just mouse dragging for rotating
                return;
            }
            PickResult result = event.getPickResult();
            Node pickedNode = result.getIntersectedNode();
            if (pickedNode instanceof MeshView) {
                MeshView mesh = (MeshView) pickedNode;
                if (!hasInnerGroupSelectedItems.getSelection().contains(mesh)) {
                    hasInnerGroupSelectedItems.select(mesh);
                } else {
                    hasInnerGroupSelectedItems.unselect(mesh);
                }
            }
        });
        innerGroup.setOnMouseDragged(event -> isRotating = true); //user is currently rotating ;)
        //---------------------------------------------


        //------------Call change of appearance of MeshViews upon un-/selecting them------------
        hasInnerGroupSelectedItems.getSelection().addListener(new ListChangeListener<Node>() {
            @Override
            public void onChanged(Change<? extends Node> c) {
                while (c.next()) {
                    if (c.wasAdded()) {
                        for (Node node : c.getAddedSubList()) {
                            if (node instanceof MeshView) {
                                meshViewSelectionEffect((MeshView) node, true);

                            }
                        }
                    }
                    if (c.wasRemoved()) {
                        for (Node node : c.getRemoved()) {
                            if (node instanceof MeshView) {
                                meshViewSelectionEffect((MeshView) node, false);
                            }
                        }
                    }
                }
            }
        });
        //---------------------------------------------------

        //----------menu File: 3D
        controller.getMenuResetView().setOnAction(e -> executeCommand(new ResetViewDrawCommand(new Group[]{contentGroup, slicePlaneGroup}, camera, initialTransform, initialCameraPosition, setupMouseRotate3D)));
        controller.getMenuClearView().setOnAction(e -> {
            executeCommand(new ClearCommand(contentGroup, innerGroup));
            threeDSelectionGroup.changeSelection(new HashSet<>(), true, false);


        });
        controller.getResetViewButton().setOnAction(e -> executeCommand(new ResetViewDrawCommand(new Group[]{contentGroup,slicePlaneGroup}, camera, initialTransform, initialCameraPosition, setupMouseRotate3D)));
        controller.getClearViewButton().setOnAction(e -> {
            executeCommand(new ClearCommand(contentGroup, innerGroup));
            threeDSelectionGroup.changeSelection(new HashSet<>(), true, false);
        });


        //------expand and collapse button for the tree view-----------------
        controller.getTreeExpandButton().setOnAction(e -> {
            treeViewSelectionGroup.setNoUpdating(true);
            executeCommand(new ExpandTreeViewCommand(getSelectedTreeView()));
            treeViewSelectionGroup.setNoUpdating(false);
            treeViewSelectionGroup.changeSelection(new HashSet<>(), true, false);
        });
        controller.getTreeCollapseButton().setOnAction(e -> {
            treeViewSelectionGroup.setNoUpdating(true);
            executeCommand(new CollapseTreeViewCommand(getSelectedTreeView()));
            treeViewSelectionGroup.setNoUpdating(false);
            treeViewSelectionGroup.changeSelection(new HashSet<>(), true, false);
        });
        //----------------------------------

        //--------3D buttons for selection--------------
        controller.getSelectAll3DButton().setOnAction(e -> hasInnerGroupSelectedItems.selectAll());
        controller.getUnselectAll3DButton().setOnAction(e -> {
            //hasInnerGroupSelectedItems.clearSelection();
            threeDSelectionGroup.changeSelection(new HashSet<>(), true, false);
        });
        //-------------------------------------------------

        //-----------------buttons to draw / undraw------------------------------
        controller.getRemoveObjButton().setOnAction(e -> {
            executeCommand(new RemoveObjFrom3DCommand(new HashSet<>(threeDSelectionGroup.getSelection()), threeDContentGroup, hasInnerGroupContents, threeDSelectionGroup));
        });
        controller.getRemoveObjButton().disableProperty().bind(Bindings.isEmpty(hasInnerGroupContents.getSelection()));

        controller.getDrawIn3DButton().setOnAction(e -> {
            TreeViewSelectionContainer treeViewSelectionContainer = null;
            for (TreeViewSelectionContainer treeViewSelectionContainer1 : this.treeViewSelectionContainers) if (treeViewSelectionContainer1.getId().equals(getSelectedTreeView().getId())) treeViewSelectionContainer = treeViewSelectionContainer1;
            if (treeViewSelectionContainer == null) {System.out.println("drawin3dButton: treeViewSelectionContainer is null!"); return;}
            // commented out because if you switch to another treetab and there is stuff selected (because tabs are synced) then the button would still draw the selection of the tree where the selection was last changed (not the one youre looking at).
//            HashSet<String> toDraw = new HashSet<String>(selectionMediatorTree_3D_Content.transformAselectionToBSelection(treeViewSelectionGroup.getSelection()));
//            for (String item : new HashSet<String>(selectionMediatorTree_3D_Content.transformAselectionToBSelection(treeViewSelectionGroup.getSelection()))) {  //need to create this again to not concurrently modify doDraw
            HashSet<String> toDraw = new HashSet<String>(selectionMediatorTree_3D_Content.transformAselectionToBSelection(treeViewSelectionContainer.getSelectionFormatted()));
            for (String item : new HashSet<String>(selectionMediatorTree_3D_Content.transformAselectionToBSelection(treeViewSelectionContainer.getSelectionFormatted()))) {  //need to create this again to not concurrently modify doDraw

                if (threeDContentGroup.getSelection().contains(item)) {
                    toDraw.remove(item);    //remove already present items
                }
            }
            if (!toDraw.isEmpty()) {
                executeCommand(new DrawItemIn3DCommand(toDraw, threeDContentGroup, threeDSelectionGroup));
                if (controller.getSelectionSynchCheck().isSelected()) {
                    treeViewSelectionGroup.setNoUpdating(true);
                    threeDSelectionGroup.changeSelection(toDraw, false, false);
                    treeViewSelectionGroup.setNoUpdating(false);
                }
            }
        });

        controller.getRemoveFrom3DButton().setOnAction(e -> {
            TreeViewSelectionContainer treeViewSelectionContainer = null;
            for (TreeViewSelectionContainer treeViewSelectionContainer1 : this.treeViewSelectionContainers) if (treeViewSelectionContainer1.getId().equals(getSelectedTreeView().getId())) treeViewSelectionContainer = treeViewSelectionContainer1;
            if (treeViewSelectionContainer == null) {System.out.println("drawin3dButton: treeViewSelectionContainer is null!"); return;}
            HashSet<String> toRemove = new HashSet<>(selectionMediatorTree_3D_Content.transformAselectionToBSelection(treeViewSelectionContainer.getSelectionFormatted()));
            for (String item : new HashSet<String>(selectionMediatorTree_3D_Content.transformAselectionToBSelection(treeViewSelectionContainer.getSelectionFormatted()))) {  //need to create this again to not concurrently modify doDraw
                if (!threeDContentGroup.getSelection().contains(item)) {
                    toRemove.remove(item);    //remove items that aren't even drawn
                }
            }
            if (!toRemove.isEmpty()) {
                executeCommand(new RemoveObjFrom3DCommand(toRemove, threeDContentGroup, hasInnerGroupContents, threeDSelectionGroup));
                if (controller.getSelectionSynchCheck().isSelected()) {
                    treeViewSelectionGroup.setNoUpdating(true);
                    threeDSelectionGroup.changeSelection(toRemove, false, false);
                    treeViewSelectionGroup.setNoUpdating(false);
                }
            }
//            executeCommand(new RemoveObjFrom3DCommand(selectionMediatorTree_3D_Content.transformAselectionToBSelection(treeViewSelectionGroup.getSelection()), threeDContentGroup, hasInnerGroupContents, threeDSelectionGroup));
        });
        controller.getRemoveFrom3DButton().disableProperty().bind(Bindings.isEmpty(hasInnerGroupContents.getSelection()));
        //-----------------------------------------------------------


        //-----------------enable loading of custom models---------------------
        controller.getLoadHierarchyMenu().setOnAction(e -> {

            if (!LittlePopUp.showLoadHierInfo()) return;

            Function<String, InputStream> findFileAndMakeStream = new Function<String, InputStream>() {
                @Override
                public InputStream apply(String prompt) {
                    File file = FileExplorerInteraction.findTxt(prompt);
                    InputStream relationsStream = null;
                    do {
                        if (file == null) return null;
                        try {
                            relationsStream = file.toURI().toURL().openStream();
                            break;
                        } catch (IOException ex) {
                            if (!LittlePopUp.showMsg("Error", "Failed to read file\n("+ex.getMessage()+")", "Retry", "Cancel")) break;
                        }
                    } while (true);

                    return relationsStream;
                }
            };

            InputStream relationsStream = findFileAndMakeStream.apply("Edge list");
            if (relationsStream == null) return;

            InputStream filesListStream = null;
            File filesDir = null;
            if (LittlePopUp.showMsg("", "Add OBJ files?", "Yes", "No")) {
                filesListStream = findFileAndMakeStream.apply("File List");
                if (filesListStream != null) {    //only ask for the directory if the files list is provided
                    do {
                        filesDir = FileExplorerInteraction.findDir("Folder containing .obj files");
                        if (filesDir == null || !filesDir.isDirectory() || filesDir.list().length == 0) {
                            if (!LittlePopUp.showMsg("Error", "No valid directory", "Retry", "Cancel")) {
                                filesListStream = null;
                                break;
                            }
                        } else break;
                    } while (true);
                }
            }

            String name;
            while (true) {    //try to
                name = LittlePopUp.askForString("Set a name", "");
                if (name == null || name.isBlank()) return;
                String finalName = name;
                if (!models.stream().filter(model -> model.getName().equals(finalName)).toList().isEmpty() && !name.equals("BP3D 3.0 part-of") && !name.equals("BP3D 3.0 is-a")) {
                    if (!LittlePopUp.showMsg("Error", "Name already exists, choose a different name.", "OK", "Cancel")) return;
                } else break; // success
            }


            Model model = filesListStream == null ? new Model(relationsStream, name) : new Model(relationsStream, filesListStream, name, filesDir);
            ContextMenu contextMenu = new ContextMenu();
            MenuItem remove = new MenuItem("Remove");
            remove.setOnAction(actionEvent -> executeCommand(new RemoveModelCommand(model)));
            contextMenu.getItems().add(remove);
            executeCommand(new AddModelCommand(model, contextMenu));
        });
        //-----------------------------------------


        // ----------------set up text search in trees----

        controller.getTreeSearchField().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !controller.getTreeSearchField().getText().isEmpty()) {
                getSelectedSearchTree().find(controller.getTreeSearchField().getText(), false, controller.getRegexCheck().isSelected());
            }
        });



        // TODO: it seems this only works because of the selectionGroups. when i try in AITreeIntegration without selectiongroups it only selects one item.
        // TODO: consider fixing so that it works without selectiongroups too.
        controller.getTreeSearchAllButton().setOnAction(e -> {
            if (!controller.getTreeSearchField().getText().isEmpty()) {
                treeViewSelectionGroup.setNoUpdating(true);
                getSelectedSearchTree().find(controller.getTreeSearchField().getText(), true, controller.getRegexCheck().isSelected());
                treeViewSelectionGroup.setNoUpdating(false);
//                treeViewSelectionGroup.changeSelection(partOfTreeSelectionContainer.getSelectionFormatted(getSelectedTreeView().getSelectionModel().getSelectedItems()), true, false);
                //TODO: is this the cause? maybe replace partOf (now getFirst() ) with getSelected()?
                treeViewSelectionGroup.changeSelection(treeViewSelectionContainers.getFirst().getSelectionFormatted(getSelectedTreeView().getSelectionModel().getSelectedItems()), true, false);
            }
        });

        // setup dynamic resizing of the search field to be as large as possible while leaving enough space for the buttons
        controller.getSearchToolBar().widthProperty().addListener((obs, oldVal, newVal) -> {
            controller.getTreeSearchField().setPrefWidth(Math.max(100, newVal.doubleValue()-250));

            /* //this was my first idea but for whatever reason it will not compute the width correctly. so i resort to hardcoding above.
            double availableSpace = newVal.doubleValue() - 20;
            for (Node node : controller.getSearchToolBar().getItems()) {
                if (node instanceof Button || node instanceof CheckBox || node instanceof Separator) {
                    availableSpace -= ((Control) node).getWidth();
                }
            }controller.getTreeSearchField().setPrefWidth(Math.max(100, availableSpace));
            */
        });
        //-------------------------------------------------------

        //----------- bot label-----------
        InvalidationListener treeBotLabelUpdater = new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                updateTreeBotLabel();
            }

        };

//        for (TreeView<ANode> treeView : treeViews) treeView.getSelectionModel().getSelectedItems().addListener((InvalidationListener) i -> updateTreeBotLabel());

        hasInnerGroupSelectedItems.getSelection().addListener((InvalidationListener) i -> {
            update3DBotLabel(hasInnerGroupSelectedItems.getSelection().size() + " items selected.");
            StringBuilder stringBuilder = new StringBuilder();
            for (Node node : hasInnerGroupSelectedItems.getSelection()) stringBuilder.append(node.getId() + ", ");
            String text = stringBuilder.toString();
            updateFileIDBotTextField(text.substring(0, text.length() > 2 ? text.length()-2 : text.length()));
        });
        //------------------------------


        // ------------------------
        // bind things that depend on which tree is selected. i need to do it once in the beginning and then again every time the tree tab switches
        bindTreeDependantThings(treeBotLabelUpdater);
        controller.getTreeTabPane().getSelectionModel().selectedItemProperty().addListener((InvalidationListener) e -> {
            bindTreeDependantThings(treeBotLabelUpdater);  // stuff in here needs to be rebound every time the tree tab switches because getSelectedTree..() doesn't dynamically react to tab-switching
            updateTreeBotLabel(); // update the bottom label
        });
        // -------------------


        //make 3D mouse rotation undoable ------ //I leave it out tho because its imperfect.
        /*setupMouseRotate3D.setListener(() -> {
            executeCommand(new Mouse3DRotationCommand(setupMouseRotate3D.deltaX(), setupMouseRotate3D.deltaY(), contentGroup));
        });
        //----------------------------------------
         */


        //-----------------setup color picking----------------------
        controller.getApplyColorButton().setOnAction(e -> {
            if (!hasInnerGroupSelectedItems.getSelection().isEmpty()) executeCommand(new ColorMeshviewsCommand(controller.getColorPicker().getValue(), new HashSet<>(threeDSelectionGroup.getSelection()), hasInnerGroupContents, threeDSelectionGroup));
        });
        controller.getApplyColorButton().disableProperty().bind(Bindings.isEmpty(hasInnerGroupSelectedItems.getSelection()));

        controller.getColorPicker().getCustomColors().add(0, Color.CRIMSON); // default color
        //---------------------------------------------


        //debug printer
        //info: i think this printer always prints everything in the Set (also everything that was added in previous changes)
        /*
        threeDSelectionGroup.getSelection().addListener(new SetChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> change) {
                if (change.wasRemoved()) System.out.println(change.getElementRemoved() + " was removed from 3D selection");
                else if (change.wasAdded()) System.out.println(change.getElementAdded() + " was added to 3D selection");
            }
        });
         */

        //------- explode button-------------
        Explode explode = new Explode();
        controller.getExplodeButton().setOnAction(e -> {
            LinkedList<Node> nodesToExplode = new LinkedList<>();
            Set<String> nodesToExplodeByID = threeDSelectionGroup.getSelection().size() < 2 ? threeDContentGroup.getSelection() : threeDSelectionGroup.getSelection();
            for (String nodeID : nodesToExplodeByID) {
                nodesToExplode.add(hasInnerGroupContents.getNodeWithID(nodeID));
            }
            explode.explode(nodesToExplode, innerGroup, initialTransform);
        });

        controller.getExplodeButton().disableProperty().bind(Bindings.isEmpty(threeDContentGroup.getSelection()).or(explode.isExploding()));
        //------------------------------------


        //-------------AI support-------------------------------
        //---------create table for chat history
        String apiKey = System.getenv().get("OPENAPI_API_KEY");
        this.aiService = new AIService(apiKey);
        ObservableList<QnA> aiInteractions = FXCollections.observableArrayList();
        controller.getAiHistoryTable().setItems(aiInteractions);

        TableColumn<QnA, String> aiPromptCol = new TableColumn<>("Prompt");
        TableColumn<QnA, String> aiAnswerCol = new TableColumn<>("Answer");

        aiPromptCol.setCellValueFactory(cellData -> cellData.getValue().qProperty());
        aiAnswerCol.setCellValueFactory(cellData -> cellData.getValue().aProperty());
        controller.getAiHistoryTable().getColumns().addAll(aiPromptCol, aiAnswerCol);

        //setup correct UI for column cells:
        // 1) make them contain javafx text, not just text because else
        // they will just abbreviate long text with "..." without being scrollable
        // 2) ctrl+click -> copy to clipboard
        aiPromptCol.setCellFactory(tc -> {
            TableCell<QnA, String> cell = new TableCell<>() {
                private final Text text = new Text();
                { //need to put this stuff in a function to execute it
                    setGraphic(text);
                    text.wrappingWidthProperty().bind(tc.widthProperty());
                    text.textProperty().bind(itemProperty());
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? "" : item);
                }
            };

            cell.setOnMouseClicked(event -> {
                if (!cell.isEmpty() && event.isControlDown()) {
                    String content = cell.getItem();
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    ClipboardContent clipboardContent = new ClipboardContent();
                    clipboardContent.putString(content);
                    clipboard.setContent(clipboardContent);
                }
            });
            return cell;
        });
        //same for the other column
        aiAnswerCol.setCellFactory(tc -> {
            TableCell<QnA, String> cell = new TableCell<>() {
                private final Text text = new Text();

                {
                    setGraphic(text);
                    text.wrappingWidthProperty().bind(tc.widthProperty());
                    text.textProperty().bind(itemProperty());
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? "" : item);
                }
            };

            cell.setOnMouseClicked(event -> {
                if (!cell.isEmpty()) {
                    String content = cell.getItem();
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    ClipboardContent clipboardContent = new ClipboardContent();
                    clipboardContent.putString(content);
                    clipboard.setContent(clipboardContent);
                }
            });

            return cell;
        });

        //--------setup correct UI behaviour (visibility, width)
        if (apiKey == null) controller.getShowAICheck().disableProperty().set(true);
        controller.getAiButtonBox().visibleProperty().bind(controller.getShowAICheck().selectedProperty());
        controller.getAiButtonBox().managedProperty().bind(controller.getShowAICheck().selectedProperty());
        controller.getAiPromptTextArea().visibleProperty().bind(controller.getShowAICheck().selectedProperty());
        controller.getAiPromptTextArea().managedProperty().bind(controller.getShowAICheck().selectedProperty());
        controller.getShowAICheck().setOnAction(e -> {
            controller.getAiHistoryTable().setVisible(false);
            controller.getAiHistoryTable().setManaged(false);
        });
        controller.getAiHistoryButton().setOnAction(e -> {
            controller.getAiHistoryTable().setVisible(!controller.getAiHistoryTable().isVisible());
            controller.getAiHistoryTable().setManaged(!controller.getAiHistoryTable().isManaged());
        });
        aiPromptCol.prefWidthProperty().bind(controller.getAiHistoryTable().widthProperty().subtract(2).multiply(0.5));
        aiAnswerCol.prefWidthProperty().bind(controller.getAiHistoryTable().widthProperty().subtract(2).multiply(0.5));

        aiPromptCol.setSortable(false);
        aiAnswerCol.setSortable(false);

        //--------AI system prompt
        aiService.initializeChat(AISystemPrompt.prompt2);

        //-------------Ask AI button functionality
        controller.getAskAIButton().disableProperty().bind(aiService.runningProperty()); //disable if AIService is currently running
        controller.getAskAIButton().setOnAction(e -> {
            aiService.addMessage(new OpenAIMessage("user", controller.getAiPromptTextArea().getText()));
            aiService.restart();
        });
        // on success:
        aiService.setOnSucceeded(e -> {
            String answer = aiService.getValue().replace('$', ' ');
            QnA result = new QnA(aiService.getLastMessage().content, answer);
            aiService.addMessage(new OpenAIMessage("assistant", result.getA()));
            aiInteractions.add(result);

            getSelectedSearchTree().find(result.getA(), true, true); // search the regex answer of the AI
        });
        //------------end of AI


        //-------------info chart--------------
        controller.getInfoButton().setOnAction(e -> LittlePopUp.showPopup(InfoChart.makePie(getSelectedTreeView()), "Leaf Info", 1000, 500));
        //--------------------

        //------------Menu: Help----------
        controller.getMenuGuide().setOnAction(e -> LittlePopUp.showPopup(Help.getHelp(), "Help", 1000, 500));
        controller.getMenuAbout().setOnAction(e -> LittlePopUp.showChartPopup(About.getAbout(), "About", 400, 100));

        //------------------optional sync tree + 3D selection
        HasGroup<ANode> hasTreeSelectionGroup = new HasGroup<>(treeViewSelectionGroup);
        HasTreeSelectionGroupContainer hasTreeSelectionGroupContainer = new HasTreeSelectionGroupContainer(hasTreeSelectionGroup, selectionMediatorTree3D_Selection);

        controller.getSelectionSynchCheck().setOnAction(e -> {
            if (controller.getSelectionSynchCheck().isSelected()) {
                threeDSelectionGroup.addSelectionContainer(hasTreeSelectionGroupContainer);
            } else threeDSelectionGroup.removeSelectionContainer(hasTreeSelectionGroupContainer);
        });
        //-----------------------------



        //------------------cutting-------------------------
        controller.getCutButt().setUserData(false);
        InvalidationListener setupSlicePlaneListener = new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                setupSlicePlane();
            }
        };

        controller.getCutButt().setOnAction(e -> {
            boolean isCutting = (boolean) controller.getCutButt().getUserData();
            if (isCutting) {
                //TODO: cancel
                controller.getCutButt().setUserData(false);
                controller.getCutButt().setText("Slice");
                controller.getCutSelectionButton().setDisable(true);
                slicePlaneGroup.getChildren().clear();
                contentGroupRotator.clearIsTransformForbidden();
                innerGroup.getChildren().removeListener(setupSlicePlaneListener);
            } else {
                controller.getCutSelectionButton().setDisable(false);
                controller.getCutButt().setText("Cancel");
                controller.getCutButt().setUserData(true);      //size of the plane will be the size of the drawn 3D objects * 2
                setupSlicePlane();
                innerGroup.getChildren().addListener(setupSlicePlaneListener);
            }
        });

        //cutSelectionButton will store the last clicked MenuItem under UserData.
        //Thats what fires when the button is clicked. 'All' is the default behaviour.
        controller.getCutSelectionButton().setUserData(controller.getCutSelectionButton().getItems().getFirst());
        controller.getCutSelectionButton().setOnAction(e -> {
            ((MenuItem)controller.getCutSelectionButton().getUserData()).fire();    //fire the menuItem stored in UserData
        });

        // Set the default action and text after the user cut something
        Function<MenuItem, Boolean> postCutEvent = new Function<MenuItem, Boolean>() {
            @Override
            public Boolean apply(MenuItem menuItem) {
                controller.getCutSelectionButton().setUserData(menuItem);
                controller.getCutSelectionButton().setText(menuItem.getText());
                controller.getCutSelectionButton().setDisable(true);
                controller.getCutButt().setUserData(false);
                controller.getCutButt().setText("Cut");
                slicePlaneGroup.getChildren().clear();
                return true;
            }
        };

        //actions for the menuitems
        controller.getCutAllMenuItem().setOnAction(e -> {
            LinkedList<MeshView> meshViews = new LinkedList<>();
            for (Node node : innerGroup.getChildren()) if (node instanceof MeshView) meshViews.add((MeshView) node);
            executeCommand(new SliceCommand(meshViews, innerGroup, (Box) slicePlaneGroup.getChildren().getFirst()));
            postCutEvent.apply(controller.getCutAllMenuItem());
        });
        controller.getCutSelectedMenuItem().setOnAction(e -> {
            System.out.println("cut selected");
            LinkedList<MeshView> meshViews = new LinkedList<>();
            for (Node node : hasInnerGroupSelectedItems.getSelection()) if (node instanceof MeshView) meshViews.add((MeshView) node);
            executeCommand(new SliceCommand(meshViews, innerGroup, (Box) slicePlaneGroup.getChildren().getFirst()));
            postCutEvent.apply(controller.getCutSelectedMenuItem());
        });


        //---------------------------end cutting-------------------------


        innerGroup.getChildren().addListener((ListChangeListener<? super Node>) i -> controller.getBotLabelDrawCount().setText(innerGroup.getChildren().size() + " items drawn."));





//-----------------end of constructor----------------
    }


    //updates the label in the bottom left
    private void updateTreeBotLabel() {
        controller.getBotLabel_Tree().setText(getSelectedTreeView().getSelectionModel().getSelectedItems().size() + " items selected");
    }

    private void update3DBotLabel(String text) {
        controller.getBotLabel_3D().setText(text);
    }
    private void updateFileIDBotTextField(String text) {controller.getFileIDTextfield().setText(text);}

    /**
     * Set up the bindings of the tree text search buttons which depend on which tree is shown
     * @param treeBotLabelUpdater a listener that is to be added to only the selected TreeView / Tab and removed from the others.
     */
    private void bindTreeDependantThings(InvalidationListener treeBotLabelUpdater) {
        controller.getTreeSearchNextButton().disableProperty().unbind();
        controller.getTreeSearchPrevButton().disableProperty().unbind();

        controller.getTreeSearchNextButton().setOnAction(e -> getSelectedSearchTree().next());
        controller.getTreeSearchNextButton().disableProperty().bind(getSelectedSearchTree().getCanNextObservable().not());
        controller.getTreeSearchPrevButton().setOnAction(e -> getSelectedSearchTree().previous());
        controller.getTreeSearchPrevButton().disableProperty().bind(getSelectedSearchTree().getHasPreviousObservable());

        for (TreeView<ANode> treeView : this.treeViews) treeView.getSelectionModel().getSelectedItems().removeListener(treeBotLabelUpdater);
        getSelectedTreeView().getSelectionModel().getSelectedItems().addListener(treeBotLabelUpdater);

    }


    /**
     * Implements the 3D selection effect.
     * @param meshView to select.
     * @param select: to select or to unselect (decides which effect is applied).
     */
    private void meshViewSelectionEffect(MeshView meshView, boolean select) {
        if (!(meshView.getMaterial() instanceof PhongMaterial)) meshView.setMaterial(new PhongMaterial(Color.WHITE));
        PhongMaterial currentMaterial = (PhongMaterial) meshView.getMaterial();
        //things ive tried:
        //currentMaterial.setSpecularColor(Color.WHITE); //or other colors
        //currentMaterial.setSpecularPower(1200);
        //currentMaterial.getDiffuseColor().desaturate().desaturate().desaturate();

        if (select) {
            currentMaterial.setDiffuseColor(currentMaterial.getDiffuseColor().saturate().saturate());
        } else {
            currentMaterial.setDiffuseColor(currentMaterial.getDiffuseColor().desaturate().desaturate());
        }  //works!
    }

    //only for debug purposes
    private void printTreeSelection(TreeView<ANode> treeView) {
        if (controller.getTreeTabPane().getSelectionModel().getSelectedIndex() == 0) System.out.println("getPartOfTreeView selection");
        else System.out.println("getIsATreeView selection");
        if (treeView.getSelectionModel().getSelectedItems().isEmpty()) System.out.println("EMPTY");

        for (TreeItem<ANode> treeItem : treeView.getSelectionModel().getSelectedItems()) {
            System.out.println(treeItem.getValue().name());
        }
    }
    //debug purposes
    private void print3DSelection(SelectionGroup<String> selectionGroup) {
        System.out.println("selection of group:");
        for (String s : selectionGroup.getSelection()) {
            System.out.println(s);
        }
    }

    /**
     * Enables control of the 3D objects via arrow keys
     * by adding EventFilter to the given Scene.
     * Works by calling rotationControl() with the pressed keys.
     */
    public void setKeyControls(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            boolean alt = event.isAltDown();
            boolean shift = event.isShiftDown();
            KeyCode keyCode = event.getCode();
            if (event.isControlDown() || alt) {
                if (List.of(KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN, KeyCode.PLUS, KeyCode.MINUS).contains(keyCode)) rotationControl(keyCode, alt, shift);
                switch (keyCode) {
                    case KeyCode.F: controller.getTreeSearchField().requestFocus(); //ctrl+F -> search
                }
            }
        });
    }


    public void rotationControl(KeyCode keyCode, boolean rotating) {rotationControl(keyCode, rotating, false);}

    /**
     * Rotates/Translates 3D objects depending on the keycode and boolean.
     * @param keyCode controls the direction
     * @param rotating false -> the camera is moved. true -> the objects are rotated
     * @param shift If shift is pressed, move the slice plane.
     */
    public void rotationControl(KeyCode keyCode, boolean rotating, boolean shift) {
        Group3DRotation groupRotator = shift ? slicePlaneGroupRotator : contentGroupRotator;

        if (keyCode == KeyCode.LEFT) {
            if (!rotating) groupRotator.applyTranslate(new Point3D(-zoomStep, 0, 0));
            else groupRotator.applyGlobalRotation(new Point3D(0, 1, 0), rotationStep);

        } else if (keyCode == KeyCode.RIGHT) {
            if (!rotating) groupRotator.applyTranslate(new Point3D(zoomStep, 0,0));
            else groupRotator.applyGlobalRotation(new Point3D(0,1, 0), -rotationStep);

        } else if (keyCode == KeyCode.UP) {
            if (!rotating) groupRotator.applyTranslate(new Point3D(0, -zoomStep, 0));
            else groupRotator.applyGlobalRotation(new Point3D(1, 0, 0), -rotationStep);

        } else if (keyCode == KeyCode.DOWN) {
            if (!rotating) groupRotator.applyTranslate(new Point3D(0, zoomStep, 0));
            else groupRotator.applyGlobalRotation(new Point3D(1,0,0), rotationStep);

        } else if (keyCode == KeyCode.PLUS) {
            if (!rotating) groupRotator.applyTranslate(new Point3D(0 ,0 , -zoomStep));
            else groupRotator.applyGlobalRotation(new Point3D(0,0,1), rotationStep);
        }
        else if (keyCode == KeyCode.MINUS) {
            if (!rotating) groupRotator.applyTranslate(new Point3D(0,0,zoomStep));
            else groupRotator.applyGlobalRotation(new Point3D(0,0,1), -rotationStep);
        }
    }


    /**
     * @return the treeview the user is currently looking at
     */
    private TreeView<ANode> getSelectedTreeView() {
        TreeView<ANode> result = null;
        for (TreeView<ANode> treeView : this.treeViews) if (treeView.getId().equals(controller.getTreeTabPane().getTabs().get(controller.getTreeTabPane().getSelectionModel().getSelectedIndex()).getText())) result = treeView;
        return result;
    }

    /**
     * @return the treeSearcher for the currently selected treeView.
     */
    private SearchTree getSelectedSearchTree() {
        return searchTrees.get(controller.getTreeTabPane().getSelectionModel().getSelectedIndex());
    }

    private SelectionMediator_Tree_3D getSelectionMediatorTree3D(String name) {
        for (SelectionMediator_Tree_3D selectionMediatorTree3D : selectionMediatorTree3DS) if (selectionMediatorTree3D.getId().equals(name)) return selectionMediatorTree3D;
        return null;
    }

    /**
     * First clear the Redo-list. Then execute a command and add it to the stack of commands.
     */
    private void executeCommand(Command command) {
        redoList.clear();
        undoList.add(command);
        command.execute();
        //System.out.println("Executing " + command.name());
    }

    /**
     * Undo the last command from the undo-list and add it to the redo-list.
     */
    private void undoCommand() {
        if (!undoList.isEmpty()) {
            undoList.getLast().undo();
            //System.out.println("Undoing " + undoList.getLast().name());
            redoList.add(undoList.removeLast());
        }
    }

    /**
     * Redo the last command from the undo-list and add it to the redo-list.
     */
    private void redoCommand() {
        if (!redoList.isEmpty()) {
            redoList.getLast().redo();
            //System.out.println("Redoing " + redoList.getLast().name());
            undoList.add(redoList.removeLast());
        }
    }

    /**
     * <li>Creates the slice plane according to the size of innergroup.</li>
     * <li>Sets the maximum allowed distance the slice plane and the contentgroup are allowed to move,
     * making sure that they always overlap.</li>
     */
    private void setupSlicePlane() {
        double planeSize = Math.max(Math.max(innerGroup.getBoundsInLocal().getMaxX() - innerGroup.getBoundsInLocal().getMinX(), innerGroup.getBoundsInLocal().getMaxY() - innerGroup.getBoundsInLocal().getMinY()), innerGroup.getBoundsInLocal().getMaxZ() - innerGroup.getBoundsInLocal().getMinZ()) * 2;
        Group slicePlane = Plane.makeSlicePlane((int) planeSize);
        slicePlaneGroupRotator.setIsTransformForbidden(new Function<Transform, Boolean>() {
            @Override
            public Boolean apply(Transform transform) {
                Transform conTrans = contentGroup.getTransforms().getFirst();
                Point3D diff = new Point3D(Math.abs(transform.getTx() - conTrans.getTx()), Math.abs(transform.getTy() - conTrans.getTy()), Math.abs(transform.getTz() - conTrans.getTz()));
                return diff.getX() >= planeSize / 4 || diff.getY() >= planeSize / 4|| diff.getZ() >= planeSize / 4;
            }
        });
        contentGroupRotator.setIsTransformForbidden(new Function<Transform, Boolean>() {
            @Override
            public Boolean apply(Transform transform) {
                Transform conTrans = slicePlaneGroup.getTransforms().getFirst();
                Point3D diff = new Point3D(Math.abs(transform.getTx() - conTrans.getTx()), Math.abs(transform.getTy() - conTrans.getTy()), Math.abs(transform.getTz() - conTrans.getTz()));
                return diff.getX() >= planeSize / 4 || diff.getY() >= planeSize / 4|| diff.getZ() >= planeSize / 4;
            }
        });
        slicePlaneGroup.getTransforms().setAll(contentGroup.getTransforms().getFirst());    //setting the transfrom so that the plane spawns in the middle of the contentgroup
        slicePlaneGroup.getChildren().addAll(slicePlane.getChildren());
    }


    /**
     * Centers the specified 3D group to its own local bounding box.
     * This method calculates the center point of the group's local bounds
     * and applies a translation to shift the group so that its center aligns with the origin.
     *
     * @param innerGroup The 3D group to be centered relative to its local coordinate system.
     */
    public static void centerGroupToItself(Group innerGroup) {
        Bounds bounds = innerGroup.getBoundsInLocal();
        double X = (bounds.getMinX() + bounds.getMaxX()) / 2;
        double Y = (bounds.getMinY() + bounds.getMaxY()) / 2;
        double Z = (bounds.getMinZ() + bounds.getMaxZ()) / 2;
        innerGroup.getTransforms().setAll(new Translate(-X, -Y, -Z));
    }

    /**
     * @return whether dark mode is selected or not
     */
    public BooleanProperty getDarkModeSelectedProperty() {
        return controller.getDarkModeMenuItem().selectedProperty();
    }

    /**
     * @return whether fullscreen is selected or not
     */
    public BooleanProperty getFullScreenCheckProperty() {return controller.getFullScreenCheck().selectedProperty();}

    private void makeModelVisible(Model model) {
        Tab tab = this.tabs.stream().filter(tab1 -> tab1.getText().equals(model.getName())).toList().getFirst();    //get the tab of the model
        controller.getTreeTabPane().getTabs().add(tab);     //make tab visible
        if (model.getFilesDirURL() != null) this.hasTheeDContentsContainer.addResourceLocation(model.getFilesDirURL()); // make the model drawable by adding the file location
        else if (model.getFilesDir() != null) this.hasTheeDContentsContainer.addFileDir(model.getFilesDir());
    }

    private void makeModelInvisible(Model model) {
        Tab tab = this.tabs.stream().filter(tab1 -> tab1.getText().equals(model.getName())).toList().getFirst();    //get the tab of the model
        controller.getTreeTabPane().getTabs().remove(tab);     //make tab visible
        if (model.getFilesDirURL() != null) this.hasTheeDContentsContainer.removeResourceLocation(model.getFilesDirURL()); // make the model drawable by adding the file location
        else if (model.getFilesDir() != null) this.hasTheeDContentsContainer.removeFileDir(model.getFilesDir());
    }

    private void loadModel(Model model) {
        loadModel(model, null);
    }
    /**
     * - generate TreeView
     * - create SelectionContainer
     * - add SelectionContainer to TreeSElectionGroup
     * - add TreeView to treeViews
     * - create a SearchTree
     * - create a Tab and return it
     * Does *not* make the model accessible yet by showing the tab
     */
    private void loadModel(Model model, ContextMenu contextMenu) {
        this.models.add(model);
        TreeView<ANode> treeView = new TreeView<>();
        treeView.setId(model.getName());
        treeViewSetup.setupTree(treeView, model.getRoot());
        TreeViewSelectionContainer treeViewSelectionContainer = new TreeViewSelectionContainer(new HasTreeView<ANode>(treeView), this.treeViewSelectionGroup);
        this.treeViewSelectionGroup.addSelectionContainer(treeViewSelectionContainer);
        this.treeViewSelectionContainers.add(treeViewSelectionContainer);
        this.selectionMediatorTree3D_Selection.addRoot(model.getRoot());
        this.selectionMediatorTree_3D_Content.addRoot(model.getRoot());
        this.selectionMediatorTree3DS.add(new SelectionMediator_Tree_3D(List.of(model.getRoot()), model.getName()));
        this.treeViews.add(treeView);
        searchTrees.add(new SearchTree(treeView));
        Tab tab = new Tab(model.getName(), treeView);
        this.tabs.add(tab);
        tab.setContextMenu(contextMenu);
        /*
        To add checklist:
        - create & setup treeview, set id. add to treeviews list
        - create tab with name, add to tabs list
        - create selectionContainer, add to list. add to selectiongroup
        - add root to mediators
        - create searchtree
         */
    }

    /**
     * Remove the model's Container, TreeView, Searchtree and Tab. Undraw all items from the model.
     * Relies on HasTreeView, TreeViewSelectionContainer and SearchTree .getId() == model.getName() and tab.getText() == model.getName().
     * CALLS makeModelInvisible() !
     * @param model The model to remove
     */
    private void forgetModel(Model model) {
        makeModelInvisible(model);
        TreeViewSelectionContainer modelTreeViewSelectionContainer = treeViewSelectionContainers.stream().filter(treeViewSelectionContainer -> treeViewSelectionContainer.getId().equals(model.getName())).toList().get(0);

        TreeView<ANode> modelTreeView = this.treeViews.stream().filter(treeView -> treeView.getId().equals(model.getName())).toList().getFirst();
        treeViewSelectionGroup.removeSelectionContainer(modelTreeViewSelectionContainer);

        threeDContentGroup.changeSelection(
                selectionMediatorTree_3D_Content.transformAselectionToBSelection(
                        modelTreeViewSelectionContainer.getSelectionFormatted(
                                FXCollections.observableList(
                                        TreeAnalysisUtils.accumulateForEveryNodeBelow(modelTreeView.getRoot(), treeItem -> treeItem)))), false, true);

        this.treeViewSelectionContainers.removeIf(treeViewSelectionContainer -> treeViewSelectionContainer.getId().equals(model.getName()));
        this.selectionMediatorTree3D_Selection.removeRoot(model.getRoot());
        this.selectionMediatorTree_3D_Content.removeRoot(model.getRoot());
        this.selectionMediatorTree3DS.remove(getSelectionMediatorTree3D(model.getName()));
        this.treeViews.remove(modelTreeView);
        this.searchTrees.removeIf(searchTree -> searchTree.getId().equals(model.getName()));
        this.tabs.removeIf(tab -> tab.getText().equals(model.getName()));
        controller.getTreeTabPane().getTabs().removeIf(tab -> tab.getText().equals(model.getName()));
        this.models.remove(model);
    }

    // This class sits here and not in Command because it depends entirely on the implementation of WindowPresenter.
    // (so do the other commands kind of but this one only calls addModel() / forgetModel() which work with so many
    // lists it would be a pain to pass them all in the constructor).

    /**
     * Add a model to this Presenter.
     */
    private class AddModelCommand implements Command {
        private final List<Model> models;
        private final List<ContextMenu> contextMenus;

        public AddModelCommand(List<Model> models, List<ContextMenu> contextMenus) {
            this.models = models;
            this.contextMenus = contextMenus;
        }
        public AddModelCommand(List<Model> models) {
            this.models = models;
            this.contextMenus = new ArrayList<>(models.size());
            for (int i = 0; i < models.size(); i++) contextMenus.add(null);
        }
        public AddModelCommand(Model model, ContextMenu contextMenu) {
            this(List.of(model), List.of(contextMenu));
        }
        @Override
        public void undo() {for (Model model : models) forgetModel(model);}
        @Override
        public void execute() {for (int m = 0; m < models.size(); m++) {
            Model model = models.get(m);
            loadModel(model, contextMenus.get(m));
            makeModelVisible(model);
        }}
        @Override
        public void redo() {execute();}
        @Override
        public String name() {return "AddModelCommand";}
    }

    /**
     * Remove the model and also all of its 3D stuff that is currently drawn.
     */
    private class RemoveModelCommand implements Command {
        private final List<Model> models;
        private final Set<String> meshViewIDsToRemove;  //meshviews that need to be undrawn when the model is removed
        private RememberFXGroupContents rememberFXGroupContents;
        private RememberFXMeshViewColors rememberFXMeshViewColors;
        private final List<ContextMenu> contextMenus;

        public RemoveModelCommand(List<Model> models) {
            this.models = models;
            this.contextMenus = new ArrayList<>(models.size());
            this.meshViewIDsToRemove = new HashSet<>();
            for (Model model : models) {
                TreeView<ANode> modelTreeView = treeViews.stream().filter(treeView -> treeView.getId().equals(model.getName())).toList().getFirst();
                TreeViewSelectionContainer modelTreeViewSelectionContainer = treeViewSelectionContainers.stream().filter(treeViewSelectionContainer -> treeViewSelectionContainer.getId().equals(model.getName())).toList().get(0);
                meshViewIDsToRemove.addAll(selectionMediatorTree_3D_Content.transformAselectionToBSelection(
                        modelTreeViewSelectionContainer.getSelectionFormatted(
                                FXCollections.observableList(
                                        TreeAnalysisUtils.accumulateForEveryNodeBelow(modelTreeView.getRoot(), treeItem -> treeItem)))));
                Tab tab_ = null;
                for (Tab tab : tabs) {
                    if (tab.getText().equals(model.getName())) {contextMenus.add(tab.getContextMenu()); tab_ = tab; break;}
                }
                if (tab_ == null) contextMenus.add(null);
            }
        }
        public RemoveModelCommand(Model model) {this(List.of(model));}

        @Override
        public void execute() {
            this.rememberFXGroupContents = new RememberFXGroupContents(innerGroup);
            this.rememberFXMeshViewColors = new RememberFXMeshViewColors(meshViewIDsToRemove, hasInnerGroupContents, threeDSelectionGroup);
            for (Model model : models) {forgetModel(model);}
        }
        @Override
        public void undo() {
            for (int m = 0; m < models.size(); m++) {
                Model model = models.get(m);
                loadModel(model, contextMenus.get(m));makeModelVisible(model);
            }
            this.rememberFXGroupContents.restoreSelection();
            this.rememberFXMeshViewColors.restoreSelection();
        }
        @Override
        public void redo() {execute();}
        @Override
        public String name() {return "RemoveModelCommand";}
    }



    private boolean searchForDupIDs(TreeItem<ANode> root) {
        LinkedList<TreeItem<ANode>> allAITreeItems = new LinkedList<>();
        TreeAnalysisUtils.accumulateForEveryNodeBelow(root, allAITreeItems, t -> t);
        HashMap<TreeItem<ANode>, Collection<TreeItem<ANode>>> duplicateItemsMap = TreeAnalysisUtils.lookForDuplicateIDsInTree(allAITreeItems, root, false);


        if (!duplicateItemsMap.isEmpty()) {

            StringBuilder stringBuilder = new StringBuilder();
            for (TreeItem<ANode> dupTreeItem : duplicateItemsMap.keySet()) {
                for (TreeItem<ANode> aNode : duplicateItemsMap.get(dupTreeItem))
                    stringBuilder.append("ID: " + dupTreeItem.getValue().conceptId() + " Name: " + dupTreeItem.getValue().name() + "  |  ID: " + aNode.getValue().conceptId() + " Name: " + aNode.getValue().name() + "\n");
            }

            String popupText = """
                The followings nodes share the same IDs with at least one other node.
                Assign new IDs to duplicated nodes and continue?
                
                Node  |  Other node with same ID
                
                """ + stringBuilder.toString();

            if (LittlePopUp.showScrollableTextPopup("Warning", popupText)) {
                for (TreeItem<ANode> itemWDuplicateANode : duplicateItemsMap.keySet()) replaceANodeByCopy(itemWDuplicateANode, root);
                return true;
            } else return false;
        } else {
            LittlePopUp.showScrollableTextPopup("Info", "No duplicate IDs in the tree.");
            return true;
        }
    }

}
