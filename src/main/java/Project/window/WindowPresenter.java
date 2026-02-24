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
import Project.window.SupportingUI.TextSearch.SearchTree;
import Project.window.ThreeDPaneHandling.*;
import Project.SelectionModel.*;

import Project.window.TreeView.TreeViewSetup;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.shape.MeshView;
import javafx.scene.text.Text;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.function.Function;


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

    //these hold the locations of the files of the respective trees
    private SimpleObjectProperty<String> partOfFilesLocation = new SimpleObjectProperty<>();
    private SimpleObjectProperty<String> isAFilesLocation = new SimpleObjectProperty<>();

    //these are classes that have a tree and can search it
    private SearchTree partOfTreeSearcher;
    private SearchTree isATreeSearcher;


    private final AIService aiService;  //implements AI support

    /**
     *
     * @param controller Holds all UI FXML components used by this presenter.
     * @param model Has ANode roots of the part-of and is-a tree
     */
    public WindowPresenter(MainWindowController controller, Model model, String partOf_folder, String isa_folder) {

        partOfFilesLocation.set(partOf_folder);
        isAFilesLocation.set(isa_folder);

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
//        MouseScrolling3D mouseScrolling3D = new MouseScrolling3D(camMover, controller.getThreeDPane());
        MouseScrolling3D mouseScrolling3D = new MouseScrolling3D(new Group3DRotation[] {contentGroupRotator, slicePlaneGroupRotator},controller.getThreeDPane());


        innerGroup.getChildren().addListener((InvalidationListener) e -> {
            centerGroupToItself(innerGroup);
        });

        contentGroup.getChildren().add(innerGroup);
//        contentGroup.getChildren().add(slicePlaneGroup);
        contentGroup.getTransforms().setAll(initialTransform);
        contentGroup.getTransforms().addListener(new ListChangeListener<Transform>() {
            @Override
            public void onChanged(Change<? extends Transform> c) {

            }
        });


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


        //bind undo redo buttons
        controller.getUndoButton().setOnAction(e -> undoCommand());
        controller.getUndoButton().disableProperty().bind(Bindings.isEmpty(undoList));
        controller.getRedoButton().setOnAction(e -> redoCommand());
        controller.getRedoButton().disableProperty().bind(Bindings.isEmpty(redoList));

        //select all, select none button
        controller.getTreeSelectAllButton().setOnAction(e -> executeCommand(new SelectAllTreeViewCommand(getSelectedTreeView())));
        controller.getTreeSelectNoneButton().setOnAction(e -> executeCommand(new SelectNoneTreeViewCommand(getSelectedTreeView())));

        //menu: File
        controller.getMenuClose().setOnAction(e -> Platform.exit());
//        controller.getMenuSaveTree().setOnAction(e -> TreeExport.writeToFiles(getSelectedTreeView(), new File(partOfFilesLocation.get()).getParent()));
        controller.getMenuSaveTree().setOnAction(e -> {
            TreeExport.saveTrees(List.of(controller.getPartOfTreeView(), controller.getIsATreeView(), controller.getDumpTreeView()), List.of("partof", "isa", "dump"));
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


        //-----initialize the trees
        TreeViewSetup treeViewSetup = new TreeViewSetup();
        treeViewSetup.setupTree(controller.getPartOfTreeView(), model.getPartOfRoot());
        treeViewSetup.setupTree(controller.getIsATreeView(), model.getIsA_Root());
        treeViewSetup.setupTree(controller.getDumpTreeView(), model.getDumpRoot());
        //-------------

        //--------initialize tree selection model----------
        SelectionGroup<ANode> treeViewSelectionGroup = new SelectionGroup<>();
        TreeViewSelectionContainer partOfTreeSelectionContainer = new TreeViewSelectionContainer(
                new HasTreeView<ANode>(controller.getPartOfTreeView()), treeViewSelectionGroup
        );
        TreeViewSelectionContainer isATreeSelectionContainer = new TreeViewSelectionContainer(
                new HasTreeView<ANode>(controller.getIsATreeView()), treeViewSelectionGroup
        );
        treeViewSelectionGroup.addSelectionContainer(partOfTreeSelectionContainer);
        treeViewSelectionGroup.addSelectionContainer(isATreeSelectionContainer);

        TreeViewSelectionContainer dumpTreeSelectionContainer = new TreeViewSelectionContainer(
                new HasTreeView<>(controller.getDumpTreeView()), treeViewSelectionGroup
        );
        treeViewSelectionGroup.addSelectionContainer(dumpTreeSelectionContainer);
        //-----------------------------


        //---------------initialize 3D selection model-----------
        SelectionGroup<String> threeDSelectionGroup = new SelectionGroup<>();
        HasFXGroupSelection hasInnerGroupSelectedItems = new HasFXGroupSelection(innerGroup);
        FXGroupSelectionContainer threeDSelectionContainer = new FXGroupSelectionContainer(
                hasInnerGroupSelectedItems, threeDSelectionGroup);
        threeDSelectionGroup.addSelectionContainer(threeDSelectionContainer);
        //-----------------------------------------------

        //create a mediator
        SelectionMediator_Tree_3D selectionMediatorTree3D_Selection = new SelectionMediator_Tree_3D(treeViewSelectionGroup, threeDSelectionGroup, model.getIsA_Root(), model.getPartOfRoot());
        //listen to TreeViewSetup for cut, paste and delete events.
        //these necessitate that the mediator reloads his mapping of FileIDs to ANodes.
        treeViewSetup.setOnEvent(new Runnable() {
            @Override
            public void run() {
                System.out.println("running the runnable");
                selectionMediatorTree3D_Selection.reloadDicts();
            }
        });

        //---------enable the mediator------------------
        controller.getSelectIn3DButton().setOnAction(e -> {
            selectionMediatorTree3D_Selection.pushAtoB();
        });
        controller.getShowInTreeButton().setOnAction(e -> {
            selectionMediatorTree3D_Selection.pushBtoA();
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

        //-------------initialize selection model for 3D contents (so not 3D selection but what meshViews are drawn)
        HasFXGroupContents hasInnerGroupContents = new HasFXGroupContents(innerGroup);
        SelectionGroup<String> threeDContentGroup = new SelectionGroup<String>();
        HasFXGroupContentsContainer hasTheeDContentsContainer = new HasFXGroupContentsContainer(hasInnerGroupContents, threeDContentGroup); //, new String[]{isAFilesLocation.getValue().getAbsolutePath(), partOfFilesLocation.getValue().getAbsolutePath()}
        threeDContentGroup.addSelectionContainer(hasTheeDContentsContainer);
        //TODO: what if file locations don't exist because parsing went wrong?
        hasTheeDContentsContainer.addResourceLocation(isAFilesLocation.getValue());
        hasTheeDContentsContainer.addResourceLocation(partOfFilesLocation.getValue());

        controller.getDrawEverythingButton().setOnAction(e -> {
            URL isaDir = (WindowPresenter.class.getResource(isa_folder));
            String[] isaFiles = new String[]{};
            URL partofDir = (WindowPresenter.class.getResource(partOf_folder));
            String[] partofFiles = new String[]{};;
            if (isaDir != null) {

                File isaFile = new File(isaDir.getFile().substring(1));
                isaFiles = (isaFile.list());
            }
            if (partofDir != null) {
                partofFiles = new File(partofDir.getFile().substring(1)).list();
            }
            System.out.println(isaFiles.length + " isaFiles and " + partofFiles.length + " partof files");

            HashSet<String> allFiles = new HashSet<>();
            for (String file : partofFiles) allFiles.add(file.substring(0, file.lastIndexOf(".")));
            for (String file : isaFiles) allFiles.add(file.substring(0, file.lastIndexOf(".")));
            threeDContentGroup.changeSelection(allFiles, true, false);
        });


        //---------------------------

        //mediator between tree selection and 3D content
        SelectionMediator_Tree_3D selectionMediatorTree_3D_Content = new SelectionMediator_Tree_3D(treeViewSelectionGroup, threeDContentGroup, model.getIsA_Root(), model.getPartOfRoot());

        //----------menu File: 3D
        controller.getMenuResetView().setOnAction(e -> executeCommand(new ResetViewDrawCommand(contentGroup, innerGroup, camera, initialTransform, initialCameraPosition, setupMouseRotate3D)));
        controller.getMenuClearView().setOnAction(e -> {
            executeCommand(new ClearCommand(contentGroup, innerGroup));
            threeDSelectionGroup.changeSelection(new HashSet<>(), true, false);


        });
        controller.getResetViewButton().setOnAction(e -> executeCommand(new ResetViewDrawCommand(contentGroup, innerGroup, camera, initialTransform, initialCameraPosition, setupMouseRotate3D)));
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
            HashSet<String> toDraw = new HashSet<String>(selectionMediatorTree_3D_Content.transformAselectionToBSelection(treeViewSelectionGroup.getSelection()));
            for (String item : new HashSet<String>(selectionMediatorTree_3D_Content.transformAselectionToBSelection(treeViewSelectionGroup.getSelection()))) {
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
        controller.getDrawIn3DButton().disableProperty().bind(partOfFilesLocation.isNull().or(isAFilesLocation.isNull()));

        controller.getRemoveFrom3DButton().setOnAction(e -> {
            executeCommand(new RemoveObjFrom3DCommand(selectionMediatorTree_3D_Content.transformAselectionToBSelection(treeViewSelectionGroup.getSelection()), threeDContentGroup, hasInnerGroupContents, threeDSelectionGroup));
        });
        controller.getRemoveFrom3DButton().disableProperty().bind(
                Bindings.isEmpty(hasInnerGroupContents.getSelection()).or(partOfFilesLocation.isNull().or(isAFilesLocation.isNull())
        ));
        //-----------------------------------------------------------


        //------------find the .obj files ----------------
        /*
        controller.getFindFileTreeMenuItem().setOnAction(e -> {
            partOfFilesLocation.set(FileExplorerInteraction.findDir("folder containing * Part-Of * anatomy files."));
            isAFilesLocation.set(FileExplorerInteraction.findDir("folder containing * is-A * anatomy files."));

            if (partOfFilesLocation.getValue() != null && isAFilesLocation.getValue() != null) {
                System.out.println(partOfFilesLocation.getValue().getAbsolutePath());
                System.out.println(isAFilesLocation.getValue().getAbsolutePath());
                hasTheeDContentsContainer.addFileLocation(isAFilesLocation.getValue().getAbsolutePath());
                hasTheeDContentsContainer.addFileLocation(partOfFilesLocation.getValue().getAbsolutePath());
            }
        });
        //-----------------------------------------
        */

        // ----------------set up text search in trees----
        partOfTreeSearcher = new SearchTree(controller.getPartOfTreeView());
        isATreeSearcher = new SearchTree(controller.getIsATreeView());
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
                treeViewSelectionGroup.changeSelection(partOfTreeSelectionContainer.getSelectionFormatted(getSelectedTreeView().getSelectionModel().getSelectedItems()), true, false);
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
        controller.getPartOfTreeView().getSelectionModel().getSelectedItems().addListener((InvalidationListener) i -> {
            updateTreeBotLabel();
        });
        controller.getIsATreeView().getSelectionModel().getSelectedItems().addListener((InvalidationListener) i -> {
            updateTreeBotLabel();
        });
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
        bindTreeDependantThings();
        controller.getTreeTabPane().getSelectionModel().selectedItemProperty().addListener((InvalidationListener) e -> {
            // stuff in here needs to be rebound every time the tree tab switches because getSelectedTree..() dont dynamically react to tab-switching
            bindTreeDependantThings();
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
        controller.getCutButt().setOnAction(e -> {
            boolean isCutting = (boolean) controller.getCutButt().getUserData();
            if (isCutting) {
                //TODO: cancel
                controller.getCutButt().setUserData(false);
                controller.getCutButt().setText("Cut");
                controller.getCutSelectionButton().setDisable(true);
                throw new RuntimeException("Cancel slicing not yet implemented");

            } else {
                controller.getCutSelectionButton().setDisable(false);
                controller.getCutButt().setText("Cancel");
                controller.getCutButt().setUserData(true);
                Group slicePlane = Plane.makeSlicePlane();  //setting the transfrom so that the plane spawns in the middle of the contentgroup
                slicePlaneGroup.getTransforms().setAll(contentGroup.getTransforms().getFirst());
                slicePlaneGroup.getChildren().add(slicePlane);
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
                return true;
            }
        };

        //actions for the menuitems
        controller.getCutAllMenuItem().setOnAction(e -> {
            LinkedList<MeshView> meshViews = new LinkedList<>();
            for (Node node : innerGroup.getChildren()) if (node instanceof MeshView) meshViews.add((MeshView) node);
            executeCommand(new SliceCommand(meshViews));
            postCutEvent.apply(controller.getCutAllMenuItem());
        });
        controller.getCutSelectedMenuItem().setOnAction(e -> {
            System.out.println("cut selected");
            LinkedList<MeshView> meshViews = new LinkedList<>();
            for (Node node : hasInnerGroupSelectedItems.getSelection()) if (node instanceof MeshView) meshViews.add((MeshView) node);
            executeCommand(new SliceCommand(meshViews));
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
     */
    private void bindTreeDependantThings() {
        controller.getTreeSearchNextButton().disableProperty().unbind();
        controller.getTreeSearchPrevButton().disableProperty().unbind();

        controller.getTreeSearchNextButton().setOnAction(e -> getSelectedSearchTree().next());
        controller.getTreeSearchNextButton().disableProperty().bind(getSelectedSearchTree().getCanNextObservable().not());
        controller.getTreeSearchPrevButton().setOnAction(e -> getSelectedSearchTree().previous());
        controller.getTreeSearchPrevButton().disableProperty().bind(getSelectedSearchTree().getHasPreviousObservable());
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
        return switch (controller.getTreeTabPane().getSelectionModel().getSelectedIndex()) {
            case 0 -> controller.getPartOfTreeView();
            case 1 -> controller.getIsATreeView();
            default -> controller.getDumpTreeView();
        };
    }

    /**
     * @return the treeSearcher for the currently selected treeView.
     */
    private SearchTree getSelectedSearchTree() {
        if (controller.getTreeTabPane().getSelectionModel().getSelectedIndex() == 0) return partOfTreeSearcher;
        else return isATreeSearcher;
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

}
