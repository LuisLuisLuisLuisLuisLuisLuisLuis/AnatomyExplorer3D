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
import Project.command.Remember.RememberHasFXGroupContents;
import Project.command.Remember.RememberFXMeshViewColors;
import Project.command.TreeCommands.CollapseTreeViewCommand;
import Project.command.TreeCommands.ExpandTreeViewCommand;
import Project.command.TreeCommands.SelectAllTreeViewCommand;
import Project.command.TreeCommands.SelectNoneTreeViewCommand;
import Project.model.*;
import Project.window.SupportingUI.PopUp.About;
import Project.window.SupportingUI.PopUp.Help;
import Project.window.SupportingUI.PopUp.InfoChart;
import Project.window.SupportingUI.PopUp.LittlePopUp;
import Project.window.Quiz.Generating.RandomUIQuizGenerator;
import Project.window.Slicing.Plane;
import Project.window.SupportingUI.FileExplorerInteraction;
import Project.window.SupportingUI.TextSearch.SearchTree;
import Project.window.ThreeDPaneHandling.*;
import Project.SelectionModel.*;

import Project.window.ThreeDPaneHandling.Effects.Explode;
import Project.window.ThreeDPaneHandling.Effects.SelectionEffect;
import Project.window.ThreeDPaneHandling.Movement.CamMover;
import Project.window.ThreeDPaneHandling.Movement.Group3DRotation;
import Project.window.ThreeDPaneHandling.Movement.MouseScrolling3D;
import Project.window.ThreeDPaneHandling.Movement.SetupMouseRotate3D;
import Project.window.TreeView.TreeAnalysis.TreeAnalysisUtils;
import Project.command.TreeCommands.TreeEditorMockCommand;
import Project.window.TreeView.TreeViewEditing.Command.UndoableANodeTreeViewEditor;
import Project.window.TreeView.TreeViewEditing.TreeViewSetup;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static Project.model.FileUtil.*;


/**
 * Connects UI to classes.
 */
public class WindowPresenter {

    private static WindowPresenter windowPresenter = null;

    public static WindowPresenter getWindowPresenter() {
        return windowPresenter;
    }

    private static final Logger logger = Logger.getLogger(WindowPresenter.class.getName());


    private final MainWindowController controller; //holds all components of the UI (buttons etc)
    private final ObservableList<Command> undoList = FXCollections.observableList(new LinkedList<>()); //undo-redo
    private final ObservableList<Command> redoList = FXCollections.observableList(new LinkedList<>());

    private static final PerspectiveCamera camera = new PerspectiveCamera(true); //camera in 3D view
    private static final Point3D initialCameraPosition = ThreeDGroupHandler.DEFAULT_INITIAL_CAMERA_POSITION;

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
    private final Group buttonsIn3DGroup;

    private final CamMover camMover = new CamMover(camera); // controls camera movement. not actually used anymore since i switched to moving group instead of camera..
    private final Group3DRotation contentGroupRotator;      // implements rotation of the 3D view via rotation of a group
    private final Group3DRotation slicePlaneGroupRotator;   // rotates the slicing plane
    private final SetupMouseRotate3D setupMouseRotate3D;    // uses groupRotater to rotate a group using mouse drag
    private boolean isRotating = false; //is the user rotating the 3D view via mouse drag right now? if so, disable click-selecting meshViews

    // the currently chosen selection effect
    private SelectionEffect.Effect selectionEffect = SelectionEffect.Effect.VIA_SATURATION;


    private final ArrayList<TreeView<ANode>> treeViews;
    private final ArrayList<SearchTree> searchTrees;
    private final SelectionGroup<ANode> treeViewSelectionGroup;
    private final ArrayList<TreeViewSelectionContainer> treeViewSelectionContainers;
    /**
     * Maps model id to a boolean property. True if the model has unsaved changes.
     */
    private final HashMap<String, BooleanProperty> isModelUnsaved = new HashMap<>();
    private final boolean[] bp3dV3_unsavedReminder = new boolean[2];


    private final HasFXGroupContents hasInnerGroupContents;
    private final HasFXGroupContentsContainer hasTheeDContentsContainer;
    private final SelectionGroup<String> threeDContentGroup;
    private final HasFXGroupSelection hasInnerGroupSelectedItems;
    private final FXGroupSelectionContainer threeDSelectionContainer;
    private final SelectionGroup<String> threeDSelectionGroup;

    private final ArrayList<SelectionMediator_Tree_3D> selectionMediatorTree3DS;
    private final SelectionMediator_Tree_3D selectionMediatorTree3D_Selection;
    private final SelectionMediator_Tree_3D selectionMediatorTree_3D_Content;

    private final UndoableANodeTreeViewEditor treeViewEditor = new UndoableANodeTreeViewEditor();
    private final TreeViewSetup treeViewSetup = new TreeViewSetup(treeViewEditor);

    private final BooleanProperty treeEditingEnabled;

    private final ArrayList<Tab> tabs;

    private final ArrayList<Model> models;
    private Model mainModel = null;
    private Model partof_v3 = null;
    private Model isa_v3 = null;

    private long sizeOfLoadedModels = 0;    // this counts the size of OBJ files associated with models loaded by the user in MB. (excluding the anatomy models shipped with this software).
                                            // The user is not allowed to load more than maxSizeOfUserModels MB of OBJ files.
    private static final int maxSizeOfUserModels = 500 + 427;   // +427 is the size of the standard unmodified anatomy hierarchy

    private static final double freeRamFractionThreshold = 0.1; // when freeMemory / totalMemory < threshold manageRAM() will try to free up RAM

    private static final Set<String> supportedFileExtensions = Set.of(".obj");   //may be expanded in the future
    public Set<String> getSupportedFileExtensions() {return supportedFileExtensions;}


    private final AIService aiService;  //implements AI support

    /**
     *
     * @param controller Holds all UI FXML components used by this presenter.
     */
    public WindowPresenter(MainWindowController controller) {

        if (windowPresenter != null) throw new RuntimeException("WindowPresenter instance already exists");
        windowPresenter = this;

        this.controller = controller;

        //initialize the contentGroup via Setup3DSubPane
        LinkedList<Group> outerGroups = Setup3DSubPane.setup3DSubPane(controller.getThreeDPane(), camera, initialCameraPosition);
        root3d = outerGroups.getFirst();    // root3d holds contentGroup.
                                        // If you want to add more content that is independet from the meshes (i.e. independent from innerGroup),
                                        // add a new group to root3d and put it in there. like for example the slice pane.
                                        // It will not be affected by clearing the view or any rotation, mouse drag etc unless you add that group to
                                        // specific methods (like SetupMouseRotate3D).
        slicePlaneGroup = new Group();
        root3d.getChildren().add(this.slicePlaneGroup);
        contentGroup = outerGroups.get(1);
        this.innerGroup = new Group();
        this.buttonsIn3DGroup = new Group();
        root3d.getChildren().add(buttonsIn3DGroup);


        // Rotation
        this.contentGroupRotator = new Group3DRotation(contentGroup);
        this.slicePlaneGroupRotator = new Group3DRotation(slicePlaneGroup);
        setupMouseRotate3D = new SetupMouseRotate3D(controller.getThreeDPane(), new Group[]{contentGroup, slicePlaneGroup});

        //mouse scrolling functionality
        MouseScrolling3D mouseScrolling3D = new MouseScrolling3D(new Group3DRotation[] {contentGroupRotator, slicePlaneGroupRotator},controller.getThreeDPane());


        contentGroup.getChildren().add(innerGroup);
        contentGroup.getTransforms().setAll(initialTransform);


        //----------------create the models----------------

        this.models = new ArrayList<>();
        logger.log(Level.CONFIG, "null check:" + (getClass().getResource("/Project/anatomy/BP3D_4.0_obj_99/FJ1252.obj") == null));
        try {
            mainModel = new Model(
                    getClass().getResourceAsStream("/Project/anatomy/anatomy_edge_list.txt"),
                    getClass().getResourceAsStream("/Project/anatomy/anatomy_file_list_obj.txt"),
                    "anatomy",
                    "/Project/anatomy/BP3D_4.0_obj_99/");
        } catch (IllegalArgumentException illegalArgumentException) {LittlePopUp.showMsg("Error", "Failed to load main model. Perhaps the files were modified or moved.\nError: " + illegalArgumentException.getMessage(), "OK");}

        try {
            partof_v3 = new Model(
                    getClass().getResourceAsStream("/Project/anatomy/bp3d_v3_conventional_partof.txt"),
                    getClass().getResourceAsStream("/Project/anatomy/bp3d_v3_parts_v4_format_cleaned.txt"),
                    "BP3D 3.0 part-of",
                    "/Project/anatomy/BodyParts3D_3.0_obj_99/"
            );
        } catch (IllegalArgumentException e) {LittlePopUp.showMsg("Error", "Failed to load BP3D part-of. Perhaps the files were modified or moved.\nError: " + e.getMessage(), "OK");}

        try {
            isa_v3 = new Model(
                    getClass().getResourceAsStream("/Project/anatomy/bp3d_v3_composite_isa.txt"),
                    getClass().getResourceAsStream("/Project/anatomy/bp3d_v3_parts_v4_format_cleaned.txt"),
                    "BP3D 3.0 is-a",
                    "/Project/anatomy/BodyParts3D_3.0_obj_99/"
            );
        } catch (IllegalArgumentException e) {LittlePopUp.showMsg("Error", "Failed to load BP3D is-a. Perhaps the files were modified or moved.\nError: " + e.getMessage(), "OK");}

//        this.models.add(mainModel);
//        this.models.add(partof_v3);
//        this.models.add(isa_v3);

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
        controller.getTreeSelectAllButton().setOnAction(e -> new SelectAllTreeViewCommand(getSelectedTreeView()).execute());    // not undoable because its unintuitive and can produce unexpected behaviour
        controller.getTreeSelectNoneButton().setOnAction(e -> new SelectNoneTreeViewCommand(getSelectedTreeView()).execute());  // since click-based selection is of course not undoable.

        //menu: File
        controller.getMenuClose().setOnAction(e -> requestExit());

        controller.getMenuSaveTree().setOnAction(e -> {
            //purposefully save all and not skip unchanged ones.
            List<Boolean> result = TreeExport.saveTrees(treeViews.stream().toList(), treeViews.stream().map(TreeView::getId).toList());
            if (result == null) return;
            for (int i = 0; i < result.size(); i++) {
                boolean r = result.get(i);
                String id = treeViews.get(i).getId();
                if (isModelUnsaved.get(id).get()) if (r) isModelUnsaved.get(id).set(false);
            }
        });
        controller.getEnableTreeEditingCheckMenu().selectedProperty().addListener((observable, oldValue, newValue) -> {
            for (TreeView<ANode> treeView : treeViews) {
                Model model = models.stream().filter(m -> m.getName().equals(treeView.getId())).toList().getFirst();
                treeViewSetup.setCellFactory(treeView, treeViewEditor, model.getRoot(), newValue);
            }
        });
        this.treeEditingEnabled = controller.getEnableTreeEditingCheckMenu().selectedProperty();

        controller.getEnableBP3DV3checkMenu().setUserData(false); //a flag to prevent this listener from actioning
        controller.getEnableBP3DV3checkMenu().selectedProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if ((boolean) controller.getEnableBP3DV3checkMenu().getUserData()) return;
                if (newValue) {
                    if (partof_v3 == null || isa_v3 == null) {
                        LittlePopUp.showMsg("Error", "BP3D 3.0 was not loaded properly", "OK");
                        return;
                    }
                    if (maxSizeOfUserModels - sizeOfLoadedModels < 430) {
                        LittlePopUp.showMsg("Error", "Not enough RAM to load BP3D 3.0. Disable some other models first.", "Ok");
                        controller.getEnableBP3DV3checkMenu().setSelected(false);
                        return;
                    }
                    if (!LittlePopUp.showMsg("Disclaimer", """
                            The BodyParts3D database comes in different versions. 
                            The versions share most anatomy parts, but there are some anatomy parts that can only be found in one version.
                            
                            By default, this program uses version 4.0. By enabling version 3.0, items labeled with '(available in BP3D 3.0)' will become drawable. Further, you will be able to browse the full tree of version 3.0.
                            
                            Note that versions 4.0 and 3.0 do not share the same coordinates and sizes! This means that even identical anatomy parts from different versions will be offset from each other. Items from version 3.0 will have a slightly wrong location when drawn next to items from version 4.0. The severity of this offset is different from part to part.
                            You can see this, for example, by drawing the tibia of versions 4.0 and 3.0 next to each other.
                            Version 3.0 will always be drawn in default green to always make them distinguishable.
                            
                            If you wish to continue, press 'Ok'.
                            """, "Ok", "Cancel")) {
                        controller.getEnableBP3DV3checkMenu().setSelected(false);
                        return;
                    }
                    executeCommand(new EnableBP3DV3Command());
                }
                else {
                    if (tabs.stream().filter(tab -> tab.getText().equals("BP3D 3.0 part-of") || tab.getText().equals("BP3D 3.0 is-a")).toList().size() > 0) executeCommand(new DisableBP3DV3Command());
                }
            }
        });
        controller.getTreeTabPane().getTabs().addListener(new InvalidationListener() {  //if BP3D 3.0 is added/removed via undo/redo the menu must still show the correct check.
            @Override
            public void invalidated(Observable observable) {
                if (tabs.stream().filter(tab -> tab.getText().equals("BP3D 3.0 part-of") || tab.getText().equals("BP3D 3.0 is-a")).toList().size() > 0) {
                    controller.getEnableBP3DV3checkMenu().setUserData(true);
                    controller.getEnableBP3DV3checkMenu().setSelected(true);
                } else {
                    controller.getEnableBP3DV3checkMenu().setUserData(true);
                    controller.getEnableBP3DV3checkMenu().setSelected(false);
                }
                controller.getEnableBP3DV3checkMenu().setUserData(false);
            }
        });


        //removed axes for now because it doesn't look good with the anatomy parts
        //controller.getMenuAddAxes().setOnAction(e -> executeCommand(new AddAxesCommand(innerGroup)));
        //controller.getMenuRmAxes().setOnAction(e -> executeCommand(new RemoveAxesCommand(innerGroup)));

        //3D related buttons
        Button info3DButton = new Button("🛈");
        AnchorPane anchorPane = new AnchorPane(info3DButton);
        AnchorPane.setLeftAnchor(info3DButton, 6.0);
        AnchorPane.setTopAnchor(info3DButton, 5.0);
        info3DButton.setOnAction(e -> {
            if (LittlePopUp.showMsg(
                    "How to use the 3D view",
                    """
                            Mouse drag to rotate
                            Scroll to zoom
                            
                            Ctrl + Scroll to move up/down
                            Alt + Scroll to move left/right
                            
                            Ctrl + Arrow keys to move up/down/left/right
                            Alt + Arrow keys to rotate
                            
                            Shift + Ctrl/Alt + Scroll/Keys to move the plane of cross section
                            
                            
                            You can show/hide this button via Help > Show 3D Info Button
                            """,
                    "Hide Info Button",
                    "Cancel"
            )) controller.getShowInfo3DButton().setSelected(false);
        });
        controller.getThreeDPane().getChildren().add(anchorPane);
//        info3DButton.setTranslateX(-230);
//        info3DButton.setTranslateZ(-960);
//        info3DButton.setTranslateY(-230);



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

        //-------------initialize selection model for 3D contents (not 3D selection but what meshViews are drawn)
        this.hasInnerGroupContents = new HasFXGroupContents(innerGroup, "hasinnergroupMeshes");
        this.threeDContentGroup = new SelectionGroup<String>("3dContentGroup");
        this.hasTheeDContentsContainer = new HasFXGroupContentsContainer(hasInnerGroupContents); //, new String[]{isAFilesLocation.getValue().getAbsolutePath(), partOfFilesLocation.getValue().getAbsolutePath()}
        this.hasInnerGroupContents.setHandleLoadingTask((task) -> {this.runBlockingTask(task, true); return null;});
        this.hasInnerGroupContents.setHasFXGroupContentsContainer(hasTheeDContentsContainer);
        threeDContentGroup.addSelectionContainer(hasTheeDContentsContainer);
        //---------------------------


        //---------------initialize 3D selection model-----------
        threeDSelectionGroup = new SelectionGroup<>("3dSelectionGroup");
        hasInnerGroupSelectedItems = new HasFXGroupSelection(hasInnerGroupContents, "hasInnerGroupSelectedItems");
        threeDSelectionContainer = new FXGroupSelectionContainer(hasInnerGroupSelectedItems);//, threeDSelectionGroup);
        threeDSelectionGroup.addSelectionContainer(threeDSelectionContainer);
        //-----------------------------------------------



        //mediator between tree selection and 3D content
        this.selectionMediatorTree_3D_Content = new SelectionMediator_Tree_3D(treeViewSelectionGroup, threeDContentGroup);

        //mediator between tree selection and 3D selection
        this.selectionMediatorTree3D_Selection = new SelectionMediator_Tree_3D(treeViewSelectionGroup, threeDSelectionGroup);

        //------------load main model------------
        if (mainModel != null) {
            MenuItem mmSave = new MenuItem("Save");
            loadModel(mainModel, new ContextMenu(mmSave)); //not as command because it should not be undoable / removable
            mmSave.setOnAction(actionEvent -> tryToSaveTree(treeViews.stream().filter(t -> t.getId().equals(mainModel.getName())).toList().getFirst()));
            mmSave.disableProperty().bind(isModelUnsaved.get(mainModel.getName()).not());
        }
        //---------------------------------------

        hasInnerGroupContents.getSelection().addListener((InvalidationListener) e -> centerGroupToItself(innerGroup));  //should be called after the command below, otherwise it'll fire every loop

        //little bug-avoidance in case i add some objects at some point and forget to give them an ID. null ID will throw exceptions.
        //im not sure if this is ever called (it shouldnt be)
        hasInnerGroupContents.getSelection().addListener(new ListChangeListener<Node>() {
            @Override
            public void onChanged(Change<? extends Node> c) {
                while (c.next()) {
                    if (c.wasAdded()) {
                        for (Node node : c.getAddedSubList()) {
                            if (node.getId() == null) {
                                node.setId("null");
                                logger.log(Level.WARNING, "node with null ID drawn. id set to 'null'");
                            }
                        }
                    }
                }
            }
        });


        //listen to TreeViewSetup for cut, paste and delete events.
        //these necessitate that
        // - the mediator reloads his mapping of FileIDs to ANodes.
        // - the TreeViewSelectionContainers check what new TreeItems they might have
        treeViewEditor.addOnEvent(new Runnable() {
            @Override
            public void run() {
                selectionMediatorTree3D_Selection.reloadDicts();
                selectionMediatorTree_3D_Content.reloadDicts();
                for (SelectionMediator_Tree_3D selectionMediatorTree3D : selectionMediatorTree3DS) selectionMediatorTree3D.reloadDicts();
                for (TreeViewSelectionContainer treeViewSelectionContainer : treeViewSelectionContainers) treeViewSelectionContainer.setupQuickAccessMap();
                updateTreeBotLabel();
            }
        });

        treeViewEditor.addOnExecute(new Runnable() {
            @Override
            public void run() {
                executeCommand(new TreeEditorMockCommand(treeViewEditor, treeViewEditor.treeViewIDOfLastExecuted()));
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
//            selectionMediatorTree3D_Selection.pushBtoA(); unfortunately not correct if there are ANodes with same id but different files in multiple trees!
            TreeViewSelectionContainer treeViewSelectionContainer = null;
            for (TreeViewSelectionContainer treeViewSelectionContainer1 : this.treeViewSelectionContainers) if (treeViewSelectionContainer1.getId().equals(getSelectedTreeView().getId())) treeViewSelectionContainer = treeViewSelectionContainer1;
            if (treeViewSelectionContainer == null) {return;}
            SelectionMediator_Tree_3D selectionMediatorTree3D = getSelectionMediatorTree3D(treeViewSelectionContainer.getId());
            selectionMediatorTree3D = selectionMediatorTree3D != null ? selectionMediatorTree3D : selectionMediatorTree3D_Selection;
//            treeViewSelectionContainer.changeSelection(selectionMediatorTree3D.transformBSelectionToASelection(threeDSelectionGroup.getSelection()), true, false);
//            treeViewSelectionContainer.updateGroup(); //TODO:
            getSelectedTreeView().getSelectionModel().clearSelection();
            Set<ANode> anodesToSelect = selectionMediatorTree3D.transformBSelectionToASelection(threeDSelectionGroup.getSelection());
            for (ANode aNode : anodesToSelect) getSelectedTreeView().getSelectionModel().select(TreeAnalysisUtils.getTreeItemWANodeId(aNode.getConceptId(), getSelectedTreeView().getRoot()));
            if (!getSelectedTreeView().getSelectionModel().getSelectedIndices().isEmpty()) getSelectedTreeView().scrollTo(getSelectedTreeView().getSelectionModel().getSelectedIndices().getFirst());   //scroll to selection
        });

        //--------------------------------


        //---------------implement 3D mouse selection------------------
        innerGroup.setOnMouseReleased(event -> {
            if (isRotating) {
                isRotating = false; //dont de-/select when you were just mouse dragging for rotating
                return;
            }
            PickResult result = event.getPickResult();
            Node pickedNode = result.getIntersectedNode();
            if (pickedNode instanceof MeshView mesh) {
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
        hasInnerGroupSelectedItems.getSelection().addListener((ListChangeListener<MeshView>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (MeshView node : c.getAddedSubList()) {
                        if (node != null) {
                            meshViewSelectionEffect(node, true);

                        }
                    }
                }
                if (c.wasRemoved()) {
                    for (MeshView node : c.getRemoved()) {
                        if (node != null) {
                            meshViewSelectionEffect(node, false);
                        }
                    }
                }
            }
        });
        //---------------------------------------------------

        //----------menu File: 3D
//        controller.getMenuResetView().setOnAction(e -> executeCommand(new ResetViewDrawCommand(new Group[]{contentGroup, slicePlaneGroup}, camera, initialTransform, initialCameraPosition, setupMouseRotate3D)));
//        controller.getMenuClearView().setOnAction(e -> {
//            executeCommand(new ClearCommand(contentGroup, hasInnerGroupContents));
//            threeDSelectionGroup.changeSelection(new HashSet<>(), true, false);
//        });

        controller.getResetViewButton().setOnAction(e -> executeCommand(new ResetViewDrawCommand(new Group[]{contentGroup,slicePlaneGroup}, camera, initialTransform, initialCameraPosition, setupMouseRotate3D)));
        controller.getClearViewButton().setOnAction(e -> {
            executeCommand(new ClearCommand<>(contentGroup, hasInnerGroupContents));
            threeDSelectionGroup.changeSelection(new HashSet<>(), true, false);
        });


        //------------------optional sync tree + 3D selection
        HasGroup<ANode> hasTreeSelectionGroup = new HasGroup<>(treeViewSelectionGroup);
        HasTreeSelectionGroupContainer hasTreeSelectionGroupContainer = new HasTreeSelectionGroupContainer(hasTreeSelectionGroup, selectionMediatorTree3D_Selection);

        controller.getSelectionSynchCheck().setOnAction(e -> {
            if (controller.getSelectionSynchCheck().isSelected()) {
                threeDSelectionGroup.addSelectionContainer(hasTreeSelectionGroupContainer);
            } else threeDSelectionGroup.removeSelectionContainer(hasTreeSelectionGroupContainer);
        });
        //-----------------------------


        //------expand and collapse button for the tree view-----------------
        controller.getTreeExpandButton().setOnAction(e -> {
            treeViewSelectionGroup.setNoUpdating(true);
            new ExpandTreeViewCommand(getSelectedTreeView()).execute();
            treeViewSelectionGroup.setNoUpdating(false);
            treeViewSelectionGroup.changeSelection(new HashSet<>(), true, false);
        });
        controller.getTreeCollapseButton().setOnAction(e -> {
            treeViewSelectionGroup.setNoUpdating(true);
            new CollapseTreeViewCommand(getSelectedTreeView()).execute();
            treeViewSelectionGroup.setNoUpdating(false);
            treeViewSelectionGroup.changeSelection(new HashSet<>(), true, false);
        });
        //----------------------------------

        //--------3D buttons for selection--------------
//        controller.getSelectAll3DButton().setOnAction(e -> hasInnerGroupSelectedItems.selectAll());
        controller.getSelectAll3DButton().setOnAction(e -> hasInnerGroupSelectedItems.setSelection(hasInnerGroupContents.getSelection()));
        controller.getUnselectAll3DButton().setOnAction(e -> {
            //hasInnerGroupSelectedItems.clearSelection();
            threeDSelectionGroup.changeSelection(new HashSet<>(), true, false);
        });

        //-------------------------------------------------

        //-----------------buttons to draw / undraw------------------------------
        controller.getRemoveObjButton().setOnAction(e -> {
            if (hasInnerGroupSelectedItems.getSelection().isEmpty()) return;
            if (controller.getSelectionSynchCheck().isSelected()) {
                Set<MeshView> toRemove = new HashSet<>(hasInnerGroupSelectedItems.getSelection());
                threeDSelectionGroup.changeSelection(Set.of(),true, false); //this will be the result of the operation anyway so i can save time by doing this here instead of updating per item during the remove operation.
                executeCommand(new RemoveObjFrom3DCommand(toRemove, hasInnerGroupContents, hasInnerGroupSelectedItems));
            } else executeCommand(new RemoveObjFrom3DCommand(new HashSet<>(hasInnerGroupSelectedItems.getSelection()), hasInnerGroupContents, hasInnerGroupSelectedItems));
        });
        controller.getRemoveObjButton().disableProperty().bind(Bindings.isEmpty(hasInnerGroupContents.getSelection()));


        Timeline treeInfoTimeline = new Timeline(new KeyFrame(Duration.seconds(4))); //used to set the show duration of the treeInfolabel.
        treeInfoTimeline.setOnFinished(tef -> controller.getTreeInfoLabel().setText(""));

        controller.getDrawIn3DButton().setOnAction(e -> {
            TreeViewSelectionContainer treeViewSelectionContainer = null;
            for (TreeViewSelectionContainer treeViewSelectionContainer1 : this.treeViewSelectionContainers) if (treeViewSelectionContainer1.getId().equals(getSelectedTreeView().getId())) treeViewSelectionContainer = treeViewSelectionContainer1;
            if (treeViewSelectionContainer == null) {System.out.println("drawin3dButton: treeViewSelectionContainer is null!"); return;}
            // commented out because if you switch to another treetab and there is stuff selected (because tabs are synced) then the button would still draw the selection of the tree where the selection was last changed (not the one youre looking at).
//            HashSet<String> toDraw = new HashSet<String>(selectionMediatorTree_3D_Content.transformAselectionToBSelection(treeViewSelectionGroup.getSelection()));
//            for (String item : new HashSet<String>(selectionMediatorTree_3D_Content.transformAselectionToBSelection(treeViewSelectionGroup.getSelection()))) {  //need to create this again to not concurrently modify doDraw
            HashSet<String> toDraw = new HashSet<String>(selectionMediatorTree_3D_Content.transformAselectionToBSelection(treeViewSelectionContainer.getSelectionFormatted()));
            if (toDraw.isEmpty()) {
                controller.getTreeInfoLabel().setText(" No 3D Files in selection!");
                controller.getTreeInfoLabel().setStyle("-fx-text-background-color: crimson; ");
                treeInfoTimeline.playFromStart();
                return;
            }
            for (String item : new HashSet<String>(selectionMediatorTree_3D_Content.transformAselectionToBSelection(treeViewSelectionContainer.getSelectionFormatted()))) {  //need to create this again to not concurrently modify doDraw
                if (threeDContentGroup.getSelection().contains(item)) {
                    toDraw.remove(item);    //remove already present items
                }
            }
            if (!toDraw.isEmpty()) {
//                executeCommand(new DrawItemIn3DCommand(toDraw, threeDContentGroup, threeDSelectionGroup));
                Set<MeshView> meshViewsToDraw = new HashSet<>(toDraw.size());
                for (String id : toDraw) meshViewsToDraw.addAll(hasTheeDContentsContainer.transformGroupItemToSelectionItem(id));
                executeCommand(new DrawItemIn3DCommand(meshViewsToDraw, hasInnerGroupContents, hasInnerGroupSelectedItems));
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

            HashSet<String> toRemove = new HashSet<>(selectionMediatorTree_3D_Content.transformAselectionToBSelection(treeViewSelectionContainer.getSelectionFormatted()));
            for (String item : new HashSet<>(selectionMediatorTree_3D_Content.transformAselectionToBSelection(treeViewSelectionContainer.getSelectionFormatted()))) {  //need to create this again to not concurrently modify toRemove
                if (!threeDContentGroup.getSelection().contains(item)) {
                    toRemove.remove(item);    //remove items that aren't even drawn
                }
            }
            if (!toRemove.isEmpty()) {
                Set<MeshView> meshViews = new HashSet<>();
                for (String id : toRemove) meshViews.addAll(hasTheeDContentsContainer.transformGroupItemToSelectionItem(id));

//                executeCommand(new RemoveObjFrom3DCommand(meshViews, hasInnerGroupContents, hasInnerGroupSelectedItems));
//                if (controller.getSelectionSynchCheck().isSelected()) {
//                    treeViewSelectionGroup.setNoUpdating(true);
//                    threeDSelectionGroup.changeSelection(toRemove, false, false);
//                    treeViewSelectionGroup.setNoUpdating(false);
//                }

                if (controller.getSelectionSynchCheck().isSelected()) {
                    treeViewSelectionGroup.changeSelection(selectionMediatorTree3D_Selection.transformBSelectionToASelection(toRemove), false, true);   //prevents lengthy updating of treeviewgroup during removecommand by unselecting those items before already.
                    executeCommand(new RemoveObjFrom3DCommand(meshViews, hasInnerGroupContents, hasInnerGroupSelectedItems));
                } else executeCommand(new RemoveObjFrom3DCommand(meshViews, hasInnerGroupContents, hasInnerGroupSelectedItems));

            }
        });
        //-----------------------------------------------------------

        //--------------------some (not all) disable button bindings---------------------
        controller.getRemoveFrom3DButton().disableProperty().bind(Bindings.isEmpty(hasInnerGroupContents.getSelection()));
        controller.getSelectIn3DButton().disableProperty().bind(controller.getSelectionSynchCheck().selectedProperty());
        controller.getShowInTreeButton().disableProperty().bind(controller.getSelectionSynchCheck().selectedProperty().or(Bindings.isEmpty(hasInnerGroupSelectedItems.getSelection())));
        //------------------------------------------------------------


        //-----------------enable loading of custom models---------------------
        controller.getLoadHierarchyMenu().setOnAction(e -> {

            if (!LittlePopUp.showLoadHierInfo()) return;

            InputStream relationsStream = makeStream(FileExplorerInteraction.findTxt("Edge list"));
            if (relationsStream == null) return;
            long estOBJsSize = 0;
            File filesListFile = null;
            File filesDir = null;
            if (LittlePopUp.showMsg("", "Add OBJ files?", "Yes", "No")) {
                if (models.contains(partof_v3) || models.contains(isa_v3)) {
                    if (!LittlePopUp.showMsg("Info", "To load custom OBJ files, BP3D 3.0 must be disabled to free up RAM.\n", "Continue", "Cancel")) return;
                    else executeCommand(new DisableBP3DV3Command());
                }
                if (!LittlePopUp.showMsg("Info", "You may load up to " + (maxSizeOfUserModels - sizeOfLoadedModels) + " MB worth of OBJ files. Continue by providing the file list?", "Continue", "Cancel")) return;
                filesListFile = FileExplorerInteraction.findTxt("Provide the File List");
                if (filesListFile != null) {    //only ask for the directory if the files list is provided

                    do {    // loop for finding a directory containing OBJ files. can be cancelled to avoid choosing OBJ files by setting fileListDir=null and breaking out.

                        filesDir = FileExplorerInteraction.findDir("Folder containing .obj files");

                        if (filesDir == null || !filesDir.isDirectory() || filesDir.list().length == 0) {
                            if (!LittlePopUp.showMsg("Error", "No valid directory", "Retry", "Cancel")) {
                                filesListFile = null;
                                break;
                            } else continue;
                        }

                        estOBJsSize = estimateSizeOfOBJsInDir(filesDir, FileUtil.extractFileIDsFromFileList(makeStream(filesListFile)), supportedFileExtensions);
                        if (sizeOfLoadedModels + estOBJsSize > maxSizeOfUserModels) {
                            String wrnmsg1 = "The provided OBJ files (" + estOBJsSize + " MB) exceed the maximum capacity of " + maxSizeOfUserModels + " MB.";
                            String actmsg1 = "Choose a different directory?";
                            String wrnmsg2 = "You have already uploaded " + sizeOfLoadedModels + " MB of OBJ files. Uploading the provided OBJ files (" + estOBJsSize + " MB) will exceed the maximum capacity of " + maxSizeOfUserModels + " MB.";
                            String actmsg2 = actmsg1; //"Choose a different directory or Cancel and remove other models first.";
                            if (sizeOfLoadedModels == 0) {
                                if (LittlePopUp.showMsg("Error", wrnmsg1 + "\n" + actmsg1, "Continue", "Cancel")) continue;
                            } else if (LittlePopUp.showMsg("Error", wrnmsg2 + "\n" +  actmsg2, "Continue", "Cancel")) continue;
                            filesListFile = null;
                            break;
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


            try {
                Model model;
                try {
                    model = filesListFile == null ? new Model(relationsStream, name) : new Model(relationsStream,  makeStream(filesListFile), name, filesDir);
                } catch (IllegalArgumentException illegalArgumentException) {
                    LittlePopUp.showMsg("Error", "Failed to load " + name + ".\nError: " + illegalArgumentException.getMessage(), "OK");
                    return;
                }
                ContextMenu contextMenu = new ContextMenu();
                MenuItem remove = new MenuItem("Remove");
                MenuItem save = new MenuItem("Save");
                remove.setOnAction(actionEvent -> {
                    while (isModelUnsaved.get(model.getName()).get()) {
                        int r = LittlePopUp.showMsg("Info", "The tree has unsaved changes. Save before removing?", "Yes","No", "Cancel");
                        if (r==-1) return;
                        if (r==0) break;
                        if (r==1) {
                            if (tryToSaveTree(treeViews.stream().filter(t -> t.getId().equals(model.getName())).toList().getFirst())){
                                break;
                            }
                        }
                    }
                    executeCommand(new RemoveModelCommand(model));
                });
                contextMenu.getItems().add(remove);
                executeCommand(new AddModelCommand(model, contextMenu));
                // can only bind disable property once the boolean property exists
                save.setOnAction(actionEvent -> tryToSaveTree(treeViews.stream().filter(t -> t.getId().equals(model.getName())).toList().getFirst()));
                save.disableProperty().bind(isModelUnsaved.get(model.getName()).not());
                contextMenu.getItems().add(save);
            }
            catch (Exception x) {
                LittlePopUp.showMsg("Error", "Failed to parse model:\n" + x.getMessage(), "OK");
                return;
            }
        });
        //-----------------------------------------


        // ----------------set up text search in trees----

        controller.getTreeSearchField().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !controller.getTreeSearchField().getText().isEmpty()) {
                getSelectedSearchTree().find(controller.getTreeSearchField().getText(), false, controller.getRegexCheck().isSelected());
            }
        });



        //TODO: it seems this only works because of the selectionGroups. when i try in AITreeIntegration without selectiongroups it only selects one item.
        // consider fixing so that it works without selectiongroups too.
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


        hasInnerGroupSelectedItems.getSelection().addListener((ListChangeListener<? super MeshView>) i -> {
            update3DBotLabel(hasInnerGroupSelectedItems.getSelection().size() + " items selected.");
            updateFileIDBotTextField();
        });

        controller.getFileIDbotCheck().selectedProperty().addListener(e -> updateFileIDBotTextField());
        //------------------------------


        // ------------------------
        // bind things that depend on which tree is selected. i need to do it once in the beginning and then again every time the tree tab switches
        bindTreeDependantThings(treeBotLabelUpdater);
        controller.getTreeTabPane().getSelectionModel().selectedItemProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                bindTreeDependantThings(treeBotLabelUpdater);  // stuff in here needs to be rebound every time the tree tab switches because getSelectedTree..() doesn't dynamically react to tab-switching
                updateTreeBotLabel(); // update the bottom label
            }
        });
        // -------------------


        //make 3D mouse rotation undoable ------ //I leave it out tho because its imperfect.
        /*setupMouseRotate3D.setListener(() -> {
            executeCommand(new Mouse3DRotationCommand(setupMouseRotate3D.deltaX(), setupMouseRotate3D.deltaY(), contentGroup));
        });
        //----------------------------------------
         */


        //-----------------setup color picking----------------------    for now disabled because it messes with saturation selection effect
//        controller.getApplyColorButton().setOnAction(e -> {
//            if (!hasInnerGroupSelectedItems.getSelection().isEmpty()) executeCommand(new ColorMeshviewsCommand(controller.getColorPicker().getValue(), new HashSet<>(threeDSelectionGroup.getSelection()), hasInnerGroupContents, hasInnerGroupSelectedItems));
//        });
//        controller.getApplyColorButton().disableProperty().bind(Bindings.isEmpty(hasInnerGroupSelectedItems.getSelection()));
//
//        controller.getColorPicker().getCustomColors().add(0, HasFXGroupContentsContainer.DEFAULT_COLOR); // default color
        //---------------------------------------------

        //-------------choose selection mode-------------------------------------

        controller.getSaturationStyleCheckMenu().selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                if (selectionEffect != SelectionEffect.Effect.VIA_SATURATION) return;
                controller.getSaturationStyleCheckMenu().setSelected(true);
            }
            else {
                if (selectionEffect == SelectionEffect.Effect.VIA_SATURATION) return;
                changeSelectionEffect(SelectionEffect.Effect.VIA_SATURATION);
                controller.getFillStyleCheckMenu().setSelected(false);
            }
        });
        controller.getFillStyleCheckMenu().selectedProperty().addListener((obs, oldValue, newVaule) -> {
            if (!newVaule) {
                if (selectionEffect != SelectionEffect.Effect.VIA_DRAWMODE) return;
                controller.getFillStyleCheckMenu().setSelected(true);
            }
            else {
                if (selectionEffect == SelectionEffect.Effect.VIA_DRAWMODE) return;
                changeSelectionEffect(SelectionEffect.Effect.VIA_DRAWMODE);
                controller.getSaturationStyleCheckMenu().setSelected(false);

            }
        });

        //------------------------------------------------------------------------

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
                nodesToExplode.add(hasInnerGroupContents.getMeshViewWithID(nodeID));
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
                if (!cell.isEmpty() && event.isShortcutDown()) {
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
//        if (apiKey == null) controller.getShowAICheck().disableProperty().set(true);

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
        controller.getMenuGuide().setOnAction(e -> Help.showGuide());
        controller.getMenuAbout().setOnAction(e -> LittlePopUp.showPaddedPopup(About.getAbout(), "About", 400, 100));
        controller.getShowInfo3DButton().selectedProperty().addListener((obs, old, nw) -> info3DButton.setVisible(nw));



        //------------------cutting-------------------------
        controller.getCutButt().setUserData(false);
        ListChangeListener<Object> setupSlicePlaneListener = new ListChangeListener<>() {
            @Override
            public void onChanged(Change<?> c) {
                while (c.next()) {
                    if (c.wasRemoved()) continue;
                    setupSlicePlane();
                }
            }
        };

        controller.getCutButt().setOnAction(e -> {
            boolean isCutting = (boolean) controller.getCutButt().getUserData();
            if (isCutting) {
                controller.getCutButt().setUserData(false);
                controller.getCutButt().setText("Slice");
                controller.getCutSelectionButton().setDisable(true);
                slicePlaneGroup.getChildren().clear();
                contentGroupRotator.clearIsTransformForbidden();
                hasInnerGroupContents.getSelection().removeListener(setupSlicePlaneListener);
            } else {
                if (hasInnerGroupContents.getSelection().isEmpty()) return;
                controller.getCutSelectionButton().setDisable(false);
                controller.getCutButt().setText("Cancel");
                controller.getCutButt().setUserData(true);      //size of the plane will be the size of the drawn 3D objects * 2
                setupSlicePlane();
                hasInnerGroupContents.getSelection().addListener(setupSlicePlaneListener);
            }
        });
        //cutSelectionButton will store the last clicked MenuItem under UserData.
        //That's what fires when the button is clicked. 'All' is the default behaviour.
        controller.getCutSelectionButton().setUserData(controller.getCutSelectionButton().getItems().getFirst());
        controller.getCutSelectionButton().setOnAction(e -> {
            ((MenuItem) controller.getCutSelectionButton().getUserData()).fire();    //fire the menuItem stored in UserData
        });
        // Set the default action and text after the user cut something
        Function<MenuItem, Boolean> postCutEvent = menuItem -> {
            controller.getCutSelectionButton().setUserData(menuItem);
            controller.getCutSelectionButton().setText(menuItem.getText());
            controller.getCutSelectionButton().setDisable(true);
            controller.getCutButt().setUserData(false);
            controller.getCutButt().setText("Cut");
            slicePlaneGroup.getChildren().clear();
            hasInnerGroupContents.getSelection().removeListener(setupSlicePlaneListener);
            hasInnerGroupSelectedItems.clearSelection();
            return true;
        };

        //actions for the menuitems
        controller.getCutAllMenuItem().setOnAction(e -> {
            LinkedList<MeshView> meshViews = new LinkedList<>();
            for (MeshView node : hasInnerGroupContents.getSelection()) if (node != null) meshViews.add(node);
            SliceCommand sliceCommand = new SliceCommand(meshViews, innerGroup, (Box) slicePlaneGroup.getChildren().getFirst(), hasInnerGroupContents, hasInnerGroupSelectedItems);
            sliceCommand.setHandleSlicingTask(task -> {runBlockingTask(task, true); return null;});
            executeCommand(sliceCommand);
            postCutEvent.apply(controller.getCutAllMenuItem());
        });
        controller.getCutSelectedMenuItem().setOnAction(e -> {
            if (hasInnerGroupSelectedItems.getSelection().isEmpty()) return;
            LinkedList<MeshView> meshViews = new LinkedList<>();
            for (MeshView node : hasInnerGroupSelectedItems.getSelection()) if (node != null) meshViews.add(node);
            SliceCommand sliceCommand = new SliceCommand(meshViews, innerGroup, (Box) slicePlaneGroup.getChildren().getFirst(), hasInnerGroupContents, hasInnerGroupSelectedItems);
            sliceCommand.setHandleSlicingTask(task -> {runBlockingTask(task, true); return null;});
            executeCommand(sliceCommand);
            postCutEvent.apply(controller.getCutSelectedMenuItem());
        });


        //---------------------------end cutting-------------------------

        //-------------------------------quiz------------------------------
        controller.getQuizButton().setOnAction(e -> {
            RandomUIQuizGenerator r = new RandomUIQuizGenerator(models);
            r.addOnCreateRunnable(() -> {r.getIntegerUIQuiz().start();});
        });
        //-------------------------------------------------------------------

        hasInnerGroupContents.getSelection().addListener((ListChangeListener<? super Node>) i -> controller.getBotLabelDrawCount().setText(hasInnerGroupContents.getSelection().size() + " items drawn. "));



//-----------------end of constructor----------------
    }


    //updates the label in the bottom left
    private void updateTreeBotLabel() {
        controller.getBotLabel_Tree().setText(getSelectedTreeView().getSelectionModel().getSelectedItems().size() + " items selected.");
    }

    private void update3DBotLabel(String text) {
        controller.getBotLabel_3D().setText(text);
    }

    private void updateFileIDBotTextField() {
        String text = "";
        if (controller.getFileIDbotCheck().isSelected()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Node node : hasInnerGroupSelectedItems.getSelection()) stringBuilder.append(node.getId()).append(", ");
            text = stringBuilder.toString();
            text = (text.substring(0, text.length() > 2 ? text.length()-2 : text.length()));
        } else {
            text = selectionMediatorTree3D_Selection.transformBSelectionToASelection(threeDSelectionGroup.getSelection()) + "";
            text = (text.substring(1, text.length()-1));
        }
        controller.getFileIDTextfield().setText(text);}

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
     * Apply the 3D selection effect.
     * @param meshView to select.
     * @param selected: to select or to unselect (decides which effect is applied).
     */
    private void meshViewSelectionEffect(MeshView meshView, boolean selected) {
        SelectionEffect.applySelectionEffect(meshView, selected, selectionEffect);
    }

    /**
     * Applies the currently chosen selection effect to the given MeshView.
     * @param selected whether to select or unselect.
     * */
    public void applySelectionEffect(MeshView meshView, boolean selected) {
        meshViewSelectionEffect(meshView, selected);
    }

    /**
     * Change the selectionEffect of this to the newEffect. Then apply the new effect on all MeshViews of threeDContentGroup.
     * @param newEffect the new effect
     */
    private void changeSelectionEffect(SelectionEffect.Effect newEffect) {
        for (MeshView meshView : hasInnerGroupContents.getAllItems()) {
            boolean isSelected = threeDSelectionGroup.getSelection().contains(meshView.getId());
            SelectionEffect.transformSelectionMode(meshView, isSelected, this.selectionEffect, newEffect);
        }
        this.selectionEffect = newEffect;
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
            if (event.isShortcutDown() || alt) {
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
     * @param rotating false -> the objects are moved. true -> the objects are rotated
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


    private void updateOnUndo(Command command) {
        if (command instanceof TreeEditorMockCommand treeEditorMockCommand) {
            boolean hasBeenModifiedB4 = false;
            for (Command command1 : undoList.reversed()) {
                if (command1 instanceof AddModelCommand addModelCommand) if (addModelCommand.getModelIDs().contains(treeEditorMockCommand.getTreeViewID())) break;
                if (command1 instanceof TreeEditorMockCommand treeEditorMockCommand1) {
                    if (treeEditorMockCommand1.getTreeViewID().equals(treeEditorMockCommand.getTreeViewID())) {
                        hasBeenModifiedB4 = true;
                        break;
                    }
                }
            }
            isModelUnsaved.get(treeEditorMockCommand.getTreeViewID()).set(hasBeenModifiedB4);
        }
    }

    private void updateOnExecRedo(Command command) {
        if (command instanceof TreeEditorMockCommand treeEditorMockCommand) {
            if (isModelUnsaved.containsKey(treeEditorMockCommand.getTreeViewID())) isModelUnsaved.get(treeEditorMockCommand.getTreeViewID()).set(true);
        }
        for (String s : isModelUnsaved.keySet()) System.out.println(s + ": " + isModelUnsaved.get(s).get());
    }

    /**
     * First clear the Redo-list. Then execute a command and add it to the stack of commands.
     */
    private void executeCommand(Command command) {
        logger.log(Level.INFO, "Executing " + command.name());
        redoList.clear();
//        WindowPresenter.printMem();
        manageRAM();
        command.execute();
        undoList.add(command);
        updateOnExecRedo(command);
//        WindowPresenter.printMem();
    }

    /**
     * Undo the last command from the undo-list and add it to the redo-list.
     */
    private void undoCommand() {
        if (!undoList.isEmpty()) {
            logger.log(Level.INFO, "Undo " + undoList.getLast().name());
            undoList.getLast().undo();
            redoList.add(undoList.removeLast());
            updateOnUndo(redoList.getLast());
        }
    }

    /**
     * Redo the last command from the undo-list and add it to the redo-list.
     */
    private void redoCommand() {
        if (!redoList.isEmpty()) {
            logger.log(Level.INFO, "Redo " + redoList.getLast().name());
            redoList.getLast().redo();
            undoList.add(redoList.removeLast());
            updateOnExecRedo(undoList.getLast());
        }
    }

    /**
     * <li>Creates the slice plane according to the size of innergroup.</li>
     * <li>Sets the maximum allowed distance the slice plane and the contentgroup are allowed to move,
     * making sure that they always overlap.</li>
     */
    private void setupSlicePlane() {
        if (hasInnerGroupContents.getSelection().isEmpty()) {
            return;
        }
        slicePlaneGroup.getChildren().clear();
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

    /**
     * - generate TreeView
     * - create SelectionContainer
     * - add SelectionContainer to TreeSElectionGroup
     * - add TreeView to treeViews
     * - create a SearchTree
     * - create a Tab and return it
     * Does *not* make the model accessible yet by showing the tab
     */
    private void loadModel(@NotNull Model model, ContextMenu contextMenu) {
        this.models.add(model);
        TreeView<ANode> treeView = new TreeView<>();
        treeView.setId(model.getName());
        treeViewSetup.setupTree(treeView, model.getRoot(), this.treeEditingEnabled.get());
        TreeViewSelectionContainer treeViewSelectionContainer = new TreeViewSelectionContainer(new HasTreeView<ANode>(treeView));//, this.treeViewSelectionGroup);
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
        if (model.getResourceFilesDir() != null) {
            logger.log(Level.CONFIG, "Adding filesdirURL " + model.getResourceFilesDir() + " to model " + model.getName());
            this.hasInnerGroupContents.addResourceLocation(model.getResourceFilesDir(), FileUtil.collectFileIDsBelowToSet(model.getRoot())); // make the model drawable by adding the file location
        }
        else {
            if (model.getFilesDir() != null) {
                try {
                    this.hasInnerGroupContents.addFileDir(model.getFilesDir(), FileUtil.collectFileIDsBelowToSet(model.getRoot()));
                } catch (IllegalArgumentException e) {
                    LittlePopUp.showMsg("Error", "Failed to load 3D files of model " + model.getName() + "\n" + e.getMessage(), "OK");
                    logger.log(Level.SEVERE,"Failed to load 3D files of model " + model.getName(),e);
                }
            } else logger.log(Level.CONFIG, "filesDirURL and filesDir of model are null");
        }
        controller.getTreeTabPane().getTabs().add(tab);

        hasInnerGroupContents.getIsLoadingOBJs().addListener((_, _, nw) -> {
            if (nw == false) addMeshviewTooltips(model);
        });

        if (model.getFilesDir() != null) {  //it is only possible to determine directory size via File. but in the compiled shipped product, resources aren't accessible as File anymore,
            //thus i hardcode the size of the shipped models. this may lead to incorrect calculations since the user has the ability to modify them.
            //an improvement for this would be to count the number of fileIDs in the tree and multiply that with the average OBJ file size.
            //a further improvement to this could be to discard those fileIDs for which no inputstream can be created to avoid fileIDs that were added to the tree even when no corresponding file exists.
            sizeOfLoadedModels += FileUtil.estimateSizeOfOBJsInDir(model.getFilesDir(), FileUtil.collectFileIDsBelowToSet(model.getRoot()), supportedFileExtensions);
        } else {
            if (model.equals(mainModel)) {
                sizeOfLoadedModels += FileUtil.estimateSizeOfTreeFiles(treeView.getRoot(), 214) / 1024;
            } else if (model.equals(partof_v3) || model.equals(isa_v3)) {   // like this i prevent the size from being added twice.
                if (models.stream().filter(m -> m.equals(partof_v3) || m.equals(isa_v3)).toList().size() == 1) sizeOfLoadedModels += FileUtil.estimateSizeOfTreeFiles(treeView.getRoot(), 477) / 1024;
            }
        }
        logger.log(Level.CONFIG, "size of loaded models: " + sizeOfLoadedModels);

        this.isModelUnsaved.put(model.getName(), new SimpleBooleanProperty(false));
        Circle circle = new Circle(3,Color.GRAY);
        this.isModelUnsaved.get(model.getName()).addListener((observable, oldValue, newValue) -> {
            if (newValue) tab.setGraphic(circle);
            else tab.setGraphic(null);
        });

    }

    /**
     * To every MeshView in hasInnerGroupContents, adds a Tooltip with the
     * @param model
     */
    private void addMeshviewTooltips(Model model) {
        TreeAnalysisUtils.applyRec(
                model.getRoot(),
                node -> {
                    for (String fileID : node.fileIds()) {
                        MeshView meshView = hasInnerGroupContents.getMeshViewWithID(fileID);
                        if (meshView == null) continue;
                        Tooltip tooltip = new Tooltip(node.getName() + " (ID=" + node.conceptId() + ")" + " [File=" + fileID + "]");
                        tooltip.setShowDuration(new Duration(16000));
                        Tooltip.install(meshView, tooltip);
                    }
                    return null;
                });
    }

    /**
     * Remove the model's Container, TreeView, Searchtree and Tab. Undraw all items from the model.
     * Relies on HasTreeView, TreeViewSelectionContainer and SearchTree .getId() == model.getName() and tab.getText() == model.getName().
     * @param model The model to remove
     */
    private void forgetModel(Model model) {
        if (model.getResourceFilesDir() != null) this.hasInnerGroupContents.removeResourceLocation(model.getResourceFilesDir());
        else if (model.getFilesDir() != null) this.hasInnerGroupContents.removeFileDir(model.getFilesDir());

        TreeViewSelectionContainer modelTreeViewSelectionContainer = treeViewSelectionContainers.stream().filter(treeViewSelectionContainer -> treeViewSelectionContainer.getId().equals(model.getName())).toList().getFirst();

        TreeView<ANode> modelTreeView = this.treeViews.stream().filter(treeView -> treeView.getId().equals(model.getName())).toList().getFirst();
        treeViewSelectionGroup.removeSelectionContainer(modelTreeViewSelectionContainer);

        Set<String> meshViewIDsToRemove = selectionMediatorTree_3D_Content.transformAselectionToBSelection(
                modelTreeViewSelectionContainer.getSelectionFormatted(
                        FXCollections.observableList(
                                TreeAnalysisUtils.accumulateForEveryNodeBelow(modelTreeView.getRoot(), treeItem -> treeItem))));
        for (TreeView<ANode> treeView : treeViews) {
            if (model.getName().equals(treeView.getId())) continue;
            TreeAnalysisUtils.applyRec(treeView.getRoot(), new Function<TreeItem<ANode>, Object>() {
                @Override
                public Object apply(TreeItem<ANode> treeItem) {
                    meshViewIDsToRemove.removeAll(treeItem.getValue().fileIds());
                    return null;
                }
            });
        }
        meshViewIDsToRemove.removeAll(threeDContentGroup.getSelection());

        for (String id : meshViewIDsToRemove) hasInnerGroupContents.removeOBJ(id);

        this.treeViewSelectionContainers.removeIf(treeViewSelectionContainer -> treeViewSelectionContainer.getId().equals(model.getName()));
        this.selectionMediatorTree3D_Selection.removeRoot(model.getRoot());
        this.selectionMediatorTree_3D_Content.removeRoot(model.getRoot());
        this.selectionMediatorTree3DS.remove(getSelectionMediatorTree3D(model.getName()));
        this.treeViews.remove(modelTreeView);
        treeViewEditor.removeTreeView(modelTreeView);
        this.searchTrees.removeIf(searchTree -> searchTree.getId().equals(model.getName()));
        this.tabs.removeIf(tab -> tab.getText().equals(model.getName()));
        controller.getTreeTabPane().getTabs().removeIf(tab -> tab.getText().equals(model.getName()));
        this.models.remove(model);
        if (model.getFilesDir() != null) {  //it is only possible to determine directory size via File. but in the compiled shipped product, resources aren't accessible as File anymore,
            //thus i hardcode the size of the shipped models. this may lead to incorrect calculations since the user has the ability to modify them.
            //an improvement for this would be to count the number of fileIDs in the tree and multiply that with the average OBJ file size.
            //a further improvement to this could be to discard those fileIDs for which no inputstream can be created to avoid fileIDs that were added to the tree even when no corresponding file exists.
            sizeOfLoadedModels -= FileUtil.estimateSizeOfOBJsInDir(model.getFilesDir(), FileUtil.collectFileIDsBelowToSet(model.getRoot()), supportedFileExtensions);
        } else {
            if (model.equals(mainModel)) {
                sizeOfLoadedModels -= FileUtil.estimateSizeOfTreeFiles(modelTreeView.getRoot(), 214) / 1024;
            } else if (model.equals(partof_v3) || model.equals(isa_v3)) {   // like this i prevent the size from being added twice.
                if (models.stream().filter(m -> m.equals(partof_v3) || m.equals(isa_v3)).toList().size() == 1) sizeOfLoadedModels -= FileUtil.estimateSizeOfTreeFiles(modelTreeView.getRoot(), 477) / 1024;
            }
        }
        logger.log(Level.CONFIG, "size of loaded models: " + sizeOfLoadedModels);

        this.isModelUnsaved.remove(model.getName());
    }

    // This class sits here and not in Command because it depends entirely on the implementation of WindowPresenter.
    // (so do the other commands but this one only calls loadModel(), makeModelVisible(), forgetModel() which work with so many
    // lists it would be a pain to pass them all in the constructor).

    /**
     * Add a model to this Presenter.
     */
    private class AddModelCommand implements Command {
        private final List<Model> models;
        protected final List<ContextMenu> contextMenus;

        public List<String> getModelIDs() {return models.stream().map(Model::getName).toList();}

        public AddModelCommand(List<Model> models, List<ContextMenu> contextMenus) {
            this.models = models;
            this.contextMenus = contextMenus;
        }

        public AddModelCommand(List<Model> models) {
            this.models = models;
            this.contextMenus = new ArrayList<>(models.size());
            for (Model model : models) this.contextMenus.add(null);
        }

        public AddModelCommand(Model model, ContextMenu contextMenu) {
            this(List.of(model), List.of(contextMenu));
        }

        @Override
        public void undo() {for (Model model : models) forgetModel(model);}
        @Override
        public void execute() {
            for (int m = 0; m < models.size(); m++) {
                Model model = models.get(m);
                loadModel(model, contextMenus.get(m));
            }
        }
        @Override
        public void redo() {execute();}
        @Override
        public String name() {return "AddModelCommand";}
    }

    /**
     * Remove the model and also all of its 3D stuff that is currently drawn.
     */
    private class RemoveModelCommand implements Command {
        private final List<Model> modelsToRm;
        private Set<String> meshViewIDsToRemove;  //meshviews that need to be undrawn when the model is removed
        private RememberHasFXGroupContents rememberFXGroupContents;
        private RememberFXMeshViewColors rememberFXMeshViewColors;
        private List<ContextMenu> contextMenus;
        public List<String> getModelIDs() {return modelsToRm.stream().map(Model::getName).toList();}
        private boolean[] isUnsaved;  //remember if any model has unsaved changed.

        public RemoveModelCommand(List<Model> modelsToRm) {
            this.modelsToRm = modelsToRm;
            this.contextMenus = new ArrayList<>(modelsToRm.size());
            this.meshViewIDsToRemove = new HashSet<>();
            this.isUnsaved = new boolean[modelsToRm.size()];

            logger.log(Level.CONFIG, "meshviewIDsToRemove after: " + meshViewIDsToRemove);
        }

        public RemoveModelCommand(Model model) {
            this(List.of(model));
        }

        @Override
        public void execute() {
            this.contextMenus.clear();
            this.meshViewIDsToRemove.clear();

            int i = 0;
            for (Model model : this.modelsToRm) {
                TreeView<ANode> modelTreeView = treeViews.stream().filter(treeView -> treeView.getId().equals(model.getName())).toList().getFirst();
                TreeViewSelectionContainer modelTreeViewSelectionContainer = treeViewSelectionContainers.stream().filter(treeViewSelectionContainer -> treeViewSelectionContainer.getId().equals(model.getName())).toList().get(0);
                meshViewIDsToRemove.addAll(selectionMediatorTree_3D_Content.transformAselectionToBSelection(
                        modelTreeViewSelectionContainer.getSelectionFormatted(
                                FXCollections.observableList(
                                        TreeAnalysisUtils.accumulateForEveryNodeBelow(modelTreeView.getRoot(), treeItem -> treeItem)))));
                Tab tab_ = null;
                for (Tab tab : tabs) {
                    if (tab.getText().equals(model.getName())) {this.contextMenus.add(tab.getContextMenu()); tab_ = tab; break;}
                }
                if (tab_ == null) this.contextMenus.add(null);

                isUnsaved[i] = isModelUnsaved.get(model.getName()).get();
                i++;
            }
            logger.log(Level.CONFIG, "meshviewIDsToRemove before: " + meshViewIDsToRemove);
            Collection<String> modelsToRMNames = modelsToRm.stream().map(Model::getName).toList();
            for (TreeView<ANode> treeView : treeViews) {
                if (modelsToRMNames.contains(treeView.getId())) continue;
                TreeAnalysisUtils.applyRec(treeView.getRoot(), new Function<TreeItem<ANode>, Object>() {
                    @Override
                    public Object apply(TreeItem<ANode> treeItem) {
                        meshViewIDsToRemove.removeAll(treeItem.getValue().fileIds());
                        return null;
                    }
                });
            }

            this.rememberFXGroupContents = new RememberHasFXGroupContents(hasInnerGroupContents);
            this.rememberFXMeshViewColors = new RememberFXMeshViewColors(meshViewIDsToRemove, hasInnerGroupContents, hasInnerGroupSelectedItems);
            threeDContentGroup.changeSelection(meshViewIDsToRemove, false, true);
            for (Model model : modelsToRm) {forgetModel(model);}
        }
        @Override
        public void undo() {
            SimpleIntegerProperty count = new SimpleIntegerProperty(0);
            for (int m = 0; m < modelsToRm.size(); m++) {
                Model model = modelsToRm.get(m);
                loadModel(model, contextMenus.get(m));
                isModelUnsaved.get(model.getName()).set(this.isUnsaved[m]);
                count.set(count.getValue()+1);
            }

            // getIsLoadingOBJs() is necessary because loadModel() causes HasFXGroupContentsContainer to create MeshViews in a diff Thread
            // before making them drawable by HasFXGroupContents. But bcuz that happens on a diff thread, the FXThread moves on and
            // would like to immediately call restoreSelection(), even tho the MeshViews needed haven't been created yet.
            // -> need to wait.
            // note that when many models are being loaded in a loop, the listener will trigger as soon as the first model
            // has completed loading OBJs, which will lead to the same scenario. To avoid this, it is necessary to make sure
            // that the listener only actions if loading OBJs of the last model is completed and that's done by making sure all
            // loops have gone thru using the count variable.
            if (hasInnerGroupContents.getIsLoadingOBJs().get()) {
                hasInnerGroupContents.getIsLoadingOBJs().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        if (!newValue && count.get() == modelsToRm.size()) {
                            rememberFXGroupContents.restoreSelection();
                            rememberFXMeshViewColors.restoreSelection();
                        }
                    }
                });
            }

        }
        @Override
        public void redo() {execute();}
        @Override
        public String name() {return "RemoveModelCommand";}
    }

    private class EnableBP3DV3Command extends AddModelCommand {
//        muss sich iwo erinnern ob BP3D schon modifiziert wurde (änlich wie in RemoveModelCommand implementiert)
        public EnableBP3DV3Command() {
            super(List.of(partof_v3, isa_v3), List.of(new ContextMenu(), new ContextMenu()));
        }

        @Override
        public void undo() {
            bp3dV3_unsavedReminder[0] = isModelUnsaved.get(partof_v3.getName()).get();
            bp3dV3_unsavedReminder[1] = isModelUnsaved.get(isa_v3.getName()).get();
            super.undo();
            TreeView<ANode> mainModelTreeView = null;
            for (TreeView<ANode> treeView : treeViews) if (treeView.getId().equals(mainModel.getName())) mainModelTreeView = treeView;
            if (mainModelTreeView == null) return;
            EnableDisableBP3DV3Parts.removeV3FilesFromTree(mainModelTreeView.getRoot());
        }

        @Override
        public void execute() {
            super.execute();

            for (int i = 0; i < 2; i++) {
                Model model = (i == 0 ? partof_v3 : isa_v3);
                ContextMenu contextMenu = this.contextMenus.get(i);
                contextMenu.getItems().clear();

                MenuItem save = new MenuItem("Save");
                // can only bind disable property once the isunsaved boolean property exists
                save.setOnAction(actionEvent -> tryToSaveTree(treeViews.stream().filter(t -> t.getId().equals(model.getName())).toList().getFirst()));
                save.disableProperty().bind(isModelUnsaved.get(model.getName()).not());
                contextMenu.getItems().add(save);
            }
            isModelUnsaved.get(partof_v3.getName()).set(bp3dV3_unsavedReminder[0]);
            isModelUnsaved.get(isa_v3.getName()).set(bp3dV3_unsavedReminder[1]);

            TreeView<ANode> mainModelTreeView = null;
            for (TreeView<ANode> treeView : treeViews) if (treeView.getId().equals(mainModel.getName())) mainModelTreeView = treeView;
            if (mainModelTreeView == null) return;

            EnableDisableBP3DV3Parts.addV3FilesToTree(mainModelTreeView.getRoot());
        }

        @Override
        public void redo() {
            execute();
        }

        @Override
        public String name() {
            return "EnableBP3DV3Command";
        }
    }

    private class DisableBP3DV3Command extends RemoveModelCommand {

        public DisableBP3DV3Command() {
            super(tabs.stream().map(tab -> {
                if (tab.getText().equals("BP3D 3.0 part-of")) return partof_v3;
                else if (tab.getText().equals("BP3D 3.0 is-a")) return isa_v3;
                return null;
            }).filter(model -> !(model == null)).toList());
        }

        @Override
        public void undo() {
            super.undo();
            TreeView<ANode> mainModelTreeView = null;
            for (TreeView<ANode> treeView : treeViews) if (treeView.getId().equals(mainModel.getName())) mainModelTreeView = treeView;
            if (mainModelTreeView == null) return;
            EnableDisableBP3DV3Parts.addV3FilesToTree(mainModelTreeView.getRoot());

            selectionMediatorTree3D_Selection.reloadDicts();
            selectionMediatorTree_3D_Content.reloadDicts();
        }

        @Override
        public void execute() {
            TreeView<ANode> mainModelTreeView = null;
            for (TreeView<ANode> treeView : treeViews) if (treeView.getId().equals(mainModel.getName())) mainModelTreeView = treeView;
            if (mainModelTreeView == null) return;
            EnableDisableBP3DV3Parts.removeV3FilesFromTree(mainModelTreeView.getRoot());
            selectionMediatorTree3D_Selection.reloadDicts();
            selectionMediatorTree_3D_Content.reloadDicts();
            bp3dV3_unsavedReminder[0] = isModelUnsaved.get(partof_v3.getName()).get();
            bp3dV3_unsavedReminder[1] = isModelUnsaved.get(isa_v3.getName()).get();
            super.execute();
        }

        @Override
        public void redo() {
            execute();
        }

        @Override
        public String name() {
            return "DisableBP3DV3Command";
        }
    }


    /**
     * Shuts down the application in an orderly manner. (saves things that need saving)
     */
    public void requestExit() {

        // save the trees
        List<TreeView<ANode>> unsavedTreeViews = new LinkedList<>();
        for (TreeView<ANode> treeView : this.treeViews) if (isModelUnsaved.get(treeView.getId()).get()) unsavedTreeViews.add(treeView);
        if (!tryToSaveTrees(unsavedTreeViews)) return;

        System.exit(0);
    }

    /**
     * @param unsavedTreeViews TreeViews to save.
     * @return False if aborted by the user.
     */
    private boolean tryToSaveTrees(List<TreeView<ANode>> unsavedTreeViews) {
        if (!unsavedTreeViews.isEmpty()) {
            List<String> idsToSave = LittlePopUp.scrollableListPopup(unsavedTreeViews.stream().map(TreeView::getId).toList(), "Unsaved changes", "The following trees have unsaved changes. Select the ones you want to save:");
            if (idsToSave == null) return false;

            if (!idsToSave.isEmpty()) {
                unsavedTreeViews.removeIf(t -> !idsToSave.contains(t.getId()));
                if (TreeExport.saveTrees(unsavedTreeViews, idsToSave) == null) return false;  //abort if cancelled by the user
            }
        }
        return true;
    }

    /**
     * Tries to save the given tree.
     * Set isModelUnSaved to saved if the tree had unsaved changes and saving was successful.
     * @return True if the tree has been saved successfully, false if cancelled.
     */
    private boolean tryToSaveTree(TreeView<ANode> treeView) {
        boolean result = TreeExport.saveTree(treeView, treeView.getId());
        if (isModelUnsaved.get(treeView.getId()).getValue()) isModelUnsaved.get(treeView.getId()).set(!result);
        return result;
    }


    public static long freeMemory() {return Runtime.getRuntime().freeMemory()  / (1024*1024);}
    public static long totalMemory() {return Runtime.getRuntime().totalMemory()  / (1024*1024);}

    public static void printMem() {
        Runtime r = Runtime.getRuntime();
        System.out.println(Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB max memory");
        System.out.println(r.totalMemory()  / (1024*1024)+ " MB total memory");
        System.out.println(r.freeMemory()  / (1024*1024)+ " MB free memory");
        System.out.println("Used MB: " + (r.totalMemory() - r.freeMemory()) / (1024*1024));
    }


    /**
     * This tries to keep the heap from maxing out.
     * Should be improved to account for the expensiveness of expensive commands, which depends largely on the number
     * and size of new TriangleMeshes they create.
     */
    private void manageRAM() {
        if (undoList.size() > 60 || isRAMcritical()) {
            int us = undoList.size();
            Iterator<Command> iterator = undoList.listIterator();
            while (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
                if (!( (double) freeMemory() / totalMemory() < freeRamFractionThreshold)) break;
            }
            System.out.println("Removed " + (us - undoList.size()) + " commands");
        }
    }

    /**
     * @return if RAM usage has passed a threshold.
     */
    public static boolean isRAMcritical() {
        boolean result = (double) freeMemory() / totalMemory() < freeRamFractionThreshold;
        logger.log(Level.CONFIG, "free memory=" + freeMemory() + ". total memory=" + totalMemory() + " -> " + result);
        return result;
    }

    /**
     * Binds a progress showing UI to a Task and runs it on a new Thread. Blurrs the main window while the task is running.
     * @param task The task
     * @param knowsProgress Can the task report progress or is progress indetermined?
     */
    public <T> void runBlockingTask(Task<T> task, boolean knowsProgress) {
        logger.log(Level.CONFIG, "binding task. " + task.getMessage());
        Function<Boolean, Boolean> blockingEffect = new Function<Boolean, Boolean>() {
            @Override
            public Boolean apply(Boolean flag) {
                VBox blockingVBox = controller.getBlockingProgressVBox();

                blockingVBox.setManaged(flag);
                blockingVBox.setVisible(flag);
                controller.getMainBorderpain().setDisable(flag);

                controller.getMainBorderpain().setEffect(flag ? new GaussianBlur(10) : null);
                return null;
            }
        };

        controller.getBlockingProgressLabel().textProperty().bind(task.messageProperty());
        if (knowsProgress) {
            controller.getBlockingProgressBar().progressProperty().bind(task.progressProperty());
            controller.getBlockingProgressInd().progressProperty().bind(task.progressProperty());
        } else {
            controller.getBlockingProgressInd().setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            controller.getBlockingProgressBar().setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        }
        task.stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.CANCELLED || newState == Worker.State.SUCCEEDED || newState == Worker.State.FAILED) blockingEffect.apply(false);
            if (newState == Worker.State.RUNNING) blockingEffect.apply(true);
        });

        new Thread(task).start();
    }

}
