package Project.window.TreeView.TreeRestructuring;

import Project.command.Command;
import Project.command.TreeCommands.TreeEditorMockCommand;
import Project.model.ANode;
import Project.window.PopUp.LegendItem;
import Project.window.PopUp.LittlePopUp;
import Project.window.SupportingUI.TextSearch.SearchTree;
import Project.window.TreeView.TreeAnalysis.TreeAnalysisUtils;
import Project.window.TreeView.TreeViewEditing.Command.UndoableANodeTreeViewEditor;
import Project.window.TreeView.TreeViewEditing.TreeViewSetup;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.util.*;

import static Project.window.TreeView.TreeAnalysis.TreeAnalysisUtils.replaceANodeByCopy;


public class TreeRestructuring {

    private final TreeItem<ANode> realTreeMasterRoot;
    private TreeItem<ANode> realTreeRoot; // root of the subtree being worked on. NOT the master root of the tree the subtree belongs to.
    private final TreeRestructuringController controller;
    private final TableView<TreeItemRelationShip> tableView;
    private final TreeView<ANode> restrucTreeView;
    private final TreeViewSetup treeViewSetup;
    private final UndoableANodeTreeViewEditor treeViewEditor;
    private final HashMap<TreeItem<ANode>, String> treeCellColorMap = new HashMap<>();  //maps some TreeItems of the restructured tree to css colors
    private LinkedList<TreeItem<ANode>> missingTreeItems = new LinkedList<>();        //holds TreeItems of the original Tree that are not part of the restructured
    private LinkedList<TreeItem<ANode>> nonUniqueIDTreeItems = new LinkedList<>();
    private final String missingNodeColor = "lightSalmon";
    private final String newTwinCellColor = "peachPuff";        //css colors found in treeCellColorMap
    private final String newEdgeCellColor = "lightBlue";
    private final String newNameCellColor = "paleGoldenRod";
    private final String noUniqueCellColor = "darkSalmon";

    private final LinkedList<Runnable> onAcceptedRunnables = new LinkedList<>();
    private void runOnAccepted() {for (Runnable runnable : onAcceptedRunnables) runnable.run();}
    public void addOnAcceptedRunnable(Runnable runnable) {onAcceptedRunnables.add(runnable);}
    public void removeOnAcceptedRunnable(Runnable runnable) {onAcceptedRunnables.remove(runnable);}

    public TreeItem<ANode> result = null;
    private void setResult(TreeItem<ANode> result) {this.result = result;}
    public TreeItem<ANode> getResultNow() {return result;}

    private final Project.window.PopUp.LittlePopUp.ShowPopup showPopup;

    private final ObservableList<Command> undoList = FXCollections.observableList(new LinkedList<>());
    private final ObservableList<Command> redoList = FXCollections.observableList(new LinkedList<>());
    /**
     * First clear the Redo-list. Then execute a command and add it to the stack of commands.
     */
    private void executeCommand(Command command) {
        redoList.clear();
        command.execute();
        undoList.add(command);
    }
    /**
     * Undo the last command from the undo-list and add it to the redo-list.
     */
    private void undoCommand() {
        if (!undoList.isEmpty()) {
            undoList.getLast().undo();
            redoList.add(undoList.removeLast());
        }
    }
    /**
     * Redo the last command from the undo-list and add it to the redo-list.
     */
    private void redoCommand() {
        if (!redoList.isEmpty()) {
            redoList.getLast().redo();
            undoList.add(redoList.removeLast());
        }
    }

    /**
     *
     * @param root Root of the tree that is to be restructured. Is the root of TreeView.
     * @throws IOException if loading goes wrong
     * @throws IllegalArgumentException If root has a parent.
     */
    public TreeRestructuring(TreeItem<ANode> root) throws IOException{
        this(null, root);
    }

    public TreeRestructuring(TreeItem<ANode> masterRoot, TreeItem<ANode> realTreeRoot) throws IOException, IllegalArgumentException {
        //create the window

        if (masterRoot == null && realTreeRoot.getParent() != null) throw new IllegalArgumentException("root has a parent but no master root is provided.");
        if (masterRoot != null) if (TreeAnalysisUtils.getTreeItemWANodeId(realTreeRoot.getValue().conceptId(), masterRoot) == null) throw new IllegalArgumentException("Master root and subtree are not from the same tree.");

        TreeRestructuringView treeRestructuringView = new TreeRestructuringView();
        this.controller = treeRestructuringView.getController();
        this.tableView = controller.getTableView();

        this.restrucTreeView = controller.getTreeView();
        this.treeViewEditor = new UndoableANodeTreeViewEditor();
        this.treeViewSetup = new TreeViewSetup(treeViewEditor);
        this.realTreeRoot = realTreeRoot;
        this.realTreeMasterRoot = masterRoot != null ? masterRoot : realTreeRoot;

        controller.getUndoButton().setOnAction(e -> undoCommand());
        controller.getRedoButton().setOnAction(e -> redoCommand());
        controller.getUndoButton().disableProperty().bind(Bindings.isEmpty(undoList));
        controller.getRedoButton().disableProperty().bind(Bindings.isEmpty(redoList));

        treeViewEditor.addOnExecute(() -> executeCommand(new TreeEditorMockCommand<>(treeViewEditor)));

        this.showPopup = new LittlePopUp.ShowPopup(treeRestructuringView.getRoot(), "Modify tree via AI", 1000, 500);

        showPopup.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.F) controller.getTreeSearchField().requestFocus();
        });

        controller.getParseButton().setOnAction(e -> {
            controller.getTableView().getColumns().clear();
            analyzeReply(controller.getEdgeTextArea().getText());
            treeViewSetup.setCellFactory(restrucTreeView, treeViewEditor, restrucTreeView.getRoot().getValue(), true);
            controller.getAcceptButton().setDisable(false);
        });

        controller.getAcceptButton().setOnAction(e -> acceptTree());

        SearchTree searchTree = new SearchTree(controller.getTreeView());
        controller.getTreeSearchField().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !controller.getTreeSearchField().getText().isEmpty()) {
                searchTree.find(controller.getTreeSearchField().getText(), false, controller.getRegexCheck().isSelected());
            }
        });

        // setup dynamic resizing of the search field to be as large as possible while leaving enough space for the buttons
        controller.getSearchToolBar().widthProperty().addListener((obs, oldVal, newVal) -> controller.getTreeSearchField().setPrefWidth(Math.max(100, newVal.doubleValue()-250)));
        controller.getTreeSearchNextButton().setOnAction(e -> searchTree.next());
        controller.getTreeSearchNextButton().disableProperty().bind(searchTree.getCanNextObservable().not());
        controller.getTreeSearchPrevButton().setOnAction(e -> searchTree.previous());
        controller.getTreeSearchPrevButton().disableProperty().bind(searchTree.getHasPreviousObservable());

        controller.getSearchForDupIDsButton().setOnAction(e -> searchForDupIDs(restrucTreeView.getRoot(), "There are duplicate IDs in the tree.", "No duplicate IDs in the tree."));    // wanna check if any IDs within aiTree are duplicate

        controller.getAddMissingToTreeButton().setOnAction(e -> {
            TreeItem<ANode> restructRoot = restrucTreeView.getRoot();
            if (restructRoot == null || missingTreeItems.isEmpty()) return;
            LinkedList<TreeItem<ANode>> twinsOfMissing = new LinkedList<>();
            LinkedList<TreeItem<ANode>> mock = new LinkedList<>();
            for (TreeItem<ANode> missingTreeItem : missingTreeItems) {
                TreeItem<ANode> twin = makeTwin(missingTreeItem, missingTreeItem.getValue().conceptId(), missingTreeItem.getValue().name(), twinsOfMissing, mock, false);
                twinsOfMissing.add(twin);
                restructRoot.getChildren().add(twin);
                restructRoot.getValue().children().add(twin.getValue());
            }
        });

        controller.getLegend().getChildren().addAll(
                new LegendItem(missingNodeColor, "Removed"),
                new LegendItem(newTwinCellColor, "New Node"),
                new LegendItem(newEdgeCellColor, "New Edge"),
                new LegendItem(newNameCellColor, "New Name"),
                new LegendItem(noUniqueCellColor, "No unique ID")
        );
        controller.getLegend().setSpacing(LegendItem.DEFAULT_SPACING);
        controller.getLegend().setAlignment(LegendItem.DEFAULT_ALIGNMENT_POS);
        controller.getLegend().setPadding(LegendItem.DEFAULT_PADDING);

    }

    private boolean searchForDupIDs(TreeItem<ANode> root, String errmsg, String goodmsg) {
        LinkedList<TreeItem<ANode>> allAITreeItems = new LinkedList<>();
        TreeAnalysisUtils.accumulateForEveryNodeBelow(restrucTreeView.getRoot(), allAITreeItems, t -> t);
        HashMap<TreeItem<ANode>, Collection<TreeItem<ANode>>> duplicateItemsMap = TreeAnalysisUtils.lookForDuplicateIDsInTree(allAITreeItems, root, false, realTreeRoot);


        if (!duplicateItemsMap.isEmpty()) {

            StringBuilder stringBuilder = new StringBuilder();
            for (TreeItem<ANode> dupTreeItem : duplicateItemsMap.keySet()) {
                for (TreeItem<ANode> aNode : duplicateItemsMap.get(dupTreeItem))
                    stringBuilder.append("ID: " + dupTreeItem.getValue().conceptId() + " Name: " + dupTreeItem.getValue().name() + "  |  ID: " + aNode.getValue().conceptId() + " Name: " + aNode.getValue().name() + "\n");
            }

//            String popupText = """
//                The followings nodes share the same IDs with at least one other node.
//                Assign new IDs to duplicated nodes and continue?
//
//                Node  |  Other node with same ID
//
//                """ + stringBuilder.toString();
            String popupText = errmsg + """
                
                
                Item to paste | already present item with same ID
                
                """ + stringBuilder.toString();
            LittlePopUp.showScrollableTextPopup("Error", popupText, false);    // i dont want to implement this as undoable..
            return false;
//            if (LittlePopUp.showScrollableTextPopup("Warning", popupText)) {
//                for (TreeItem<ANode> itemWDuplicateANode : duplicateItemsMap.keySet()) replaceANodeByCopy(itemWDuplicateANode, root);
//                return true;
//            } else return false;
        } else {
            LittlePopUp.showScrollableTextPopup("Info", goodmsg, false);
            return true;
        }
    }

    private void acceptTree() {
        if (!searchForDupIDs(restrucTreeView.getRoot(), "Cannot accept. There are duplicate IDs in the tree.", "No duplicate IDs in the tree.") || !searchForDupIDs(realTreeMasterRoot, "Cannot accept. Some IDs already exist in the real tree.", "No overlapping IDs with the real tree.")) return;   //want to check if any of the IDs of aiTree already occur anywhere in the full real tree
//        executeCommand(new AcceptCommand(realTreeRoot, restructRoot));
//        treeViewSetup.cut(aiTreeRoot);
//        treeViewSetup.paste(realTreeParent, realTreeMasterRoot);
//        aiTreeView.setRoot(null);
//        realTreeRoot = aiTreeRoot; if you do this you can keep on working here, otherwise not bcuz this does not know about the new real subtree anymore
        setResult(restrucTreeView.getRoot());
        restrucTreeView.setRoot(null);
        runOnAccepted();
        this.showPopup.getStage().close();  // but i choose to close this after accepting once. this means if its opened recursively, the windows working on this tree will be useless.
    }


    /**
     * Creates the tree from the edgeList and sets its root as root of the TreeView.
     */
    private void analyzeReply(String edgeList) {
        // the treeItems of the tree defined in edgeList
        LinkedList<TreeItem<ANode>> twins = new LinkedList<>();     //treeItems that are created from already existing ones (so they're 'twins').
                                                                    //they are NEITHER the same treeItem NOR do they have the same ANode instance.
        LinkedList<TreeItem<ANode>> newTwins = new LinkedList<>();  //treeItems that weren't created from existing TreeItems (not twins of anything actually)

        //for every line of the written edge list
        for (String line : edgeList.split("\n")) {
            String[] fields = line.split("\t");
            if (fields.length != 4) continue;
            String parentID = fields[0].trim();
            String childID = fields[2].trim();
            String parentName = fields[1].trim();
            String childName = fields[3].trim();

            TreeItem<ANode> parent = findTreeItemWithConceptID(parentID, realTreeMasterRoot); //try to find an existing treeItem with that conceptID
            TreeItem<ANode> child = findTreeItemWithConceptID(childID, realTreeMasterRoot);   //for parent and child (or return null)
            TreeItem<ANode> parentTwin = makeTwin(parent, parentID, parentName, twins, newTwins, false);   //create the twins (i.e. corresponding TreeItem
            TreeItem<ANode> childTwin = makeTwin(child, childID, childName, twins, newTwins, false);       //in this TreeView

            if (childTwin.getParent() != null){ // if the childTwin already has a parent, don't set it as child of another different parent (that would break the tree).
                                                // instead, make a copy of it (new TreeItem instance) and proceed.
                nonUniqueIDTreeItems.add(childTwin); // add old and new childTwin to the list of TreeItems with non-unique conceptID
                childTwin = makeTwin(child, childID, childName, twins, newTwins, true);
                nonUniqueIDTreeItems.add(childTwin);
            }

            parentTwin.getChildren().add(childTwin);    // create the relationship, for TreeItem and ANode.
            parentTwin.getValue().children().add(childTwin.getValue());


            // assign the twin to a color if something special occurred.
            if (parent != null && child != null) if (!parent.getChildren().contains(child)) treeCellColorMap.put(childTwin, newEdgeCellColor);
            if (parent != null) if(!parent.getValue().name().equals(parentName)) treeCellColorMap.put(parentTwin, newNameCellColor);
            if (child != null) if (!child.getValue().name().equals(childName)) treeCellColorMap.put(childTwin, newNameCellColor);
        }


        //now find the root of the new tree
        LinkedList<TreeItem<ANode>> roots = new LinkedList<>();
        for (TreeItem<ANode> treeItemTwin : twins) {
            if (treeItemTwin.getParent() == null) {
                System.out.println("root found: " + treeItemTwin.getValue().name());
                roots.add(treeItemTwin);
            }
        }

        if (roots.size() == 1) restrucTreeView.setRoot(roots.getFirst()); // if there's one root its straightforward
        else {                                                          // if there's many roots, create a root and make them children of it
//            aiTreeRoot = new TreeItem<ANode>(treeViewSetup.makeAnode("AIRoot", realTreeRoot.getValue())); this would be wrong because the new ai root does not correspond to that anode.
            TreeItem<ANode> restructRoot = new TreeItem<ANode>(TreeAnalysisUtils.makeAnode("New Root", new ANode("newRoot", "newRoot", new HashSet<>(), new HashSet<>())));
            for (TreeItem<ANode> possibleRoot : roots) {
                restructRoot.getChildren().add(possibleRoot);
                restructRoot.getValue().children().add(possibleRoot.getValue());
            }
            restrucTreeView.setRoot(restructRoot);
        }
        // all the new 'twins' who don't have corresponding treeItems in the real tree get a color
        for (TreeItem<ANode> newTwin : newTwins) treeCellColorMap.put(newTwin, newTwinCellColor);
        // all twins whose conceptID isn't unique
        for (TreeItem<ANode> twin : nonUniqueIDTreeItems) treeCellColorMap.put(twin, noUniqueCellColor);
        // fetch the TreeItems in the original tree that don't have a corresponding twin in this new tree
        this.missingTreeItems = findMissingNodesBelowByID(realTreeRoot, twins);

        setupTable();   //set up the colorful table
    }


    private TreeItem<ANode> makeTwin(TreeItem<ANode> maybeOgTreeItem, String conceptID, String name, LinkedList<TreeItem<ANode>> existingTwins, LinkedList<TreeItem<ANode>> newTwins, boolean forceNewTwin) {
        TreeItem<ANode> twin = null;
        boolean twinAlreadyExists = false;
        if (!forceNewTwin) {
            for (TreeItem<ANode> treeItemTwin : existingTwins) {
                if (treeItemTwin.getValue().conceptId().equals(conceptID)) {
                    twin = treeItemTwin;
                    twinAlreadyExists = true;
                    break;
                }
            }
        }

        if (!twinAlreadyExists) {
            if (maybeOgTreeItem != null) {
                twin = new TreeItem<>(maybeOgTreeItem.getValue().copy());
                twin.getValue().setName(name);
                twin.getValue().children().clear();
            }
            else {
                twin = new TreeItem<>(new ANode(conceptID, name, new HashSet<>(), new HashSet<>()));
                newTwins.add(twin);
            }
            existingTwins.add(twin);
        }

        return twin;
    }

    /**
     * Find the TreeItem with the ANode with conceptID and return it.
     * @return the TreeItem or null if no TreeItem has an ANode with that conceptID.
     */
    public static TreeItem<ANode> findTreeItemWithConceptID(String conceptID, TreeItem<ANode> root) {
        if (root.getValue().conceptId().equals(conceptID)) return root;
        else {
            for (TreeItem<ANode> child : root.getChildren()) {
                TreeItem<ANode> treeItem = findTreeItemWithConceptID(conceptID, child);
                if (treeItem != null) return treeItem;
            }
        }
        return null;
    }

    public record TreeItemRelationShip(TreeItem<ANode> parent, TreeItem<ANode> child){}

    private void setupTable() {
        TableColumn<TreeItemRelationShip, String> parentCol = new TableColumn<>();
        TableColumn<TreeItemRelationShip, String> childCol = new TableColumn<>();
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        parentCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().parent.getValue().name()));
        childCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().child.getValue().name()));
        parentCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || getTableRow().getItem() == null || getTableRow().getItem().parent == null) {
                    setText(null);
                    setStyle("");
                } else {
                    TreeItemRelationShip treeItemRelationShip = getTableRow().getItem();
                    setText(treeItemRelationShip.parent.getValue().name());
                    if (treeCellColorMap.containsKey(treeItemRelationShip.parent)) setStyle("-fx-background-color: " + treeCellColorMap.get(treeItemRelationShip.parent) + ";");
                    else if (missingTreeItems.contains(treeItemRelationShip.parent)) setStyle("-fx-background-color: " + missingNodeColor + ";"); else setStyle("");

                }
            }
        });
        childCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || getTableRow().getItem() == null || getTableRow().getItem().child == null) {
                    setText(null);
                    setStyle("");
                } else {
                    TreeItemRelationShip treeItemRelationShip = getTableRow().getItem();
                    setText(treeItemRelationShip.child.getValue().name());
                    if (treeCellColorMap.containsKey(treeItemRelationShip.child)) setStyle("-fx-background-color: " + treeCellColorMap.get(treeItemRelationShip.child) + ";");
                    else if (missingTreeItems.contains(treeItemRelationShip.child)) setStyle("-fx-background-color: " + missingNodeColor + ";"); else setStyle("");
                }
            }
        });
        ObservableList<TreeItemRelationShip> treeItemRelationShips = FXCollections.observableList(new LinkedList<>());
        createTreeItemRelationList(restrucTreeView.getRoot(), treeItemRelationShips);  //add all TreeItems of the restructure tree to the table
                                                                        //then add all missing TreeItems from the original table
        for (TreeItem<ANode> missingTreeItem : missingTreeItems) {
            if (missingTreeItem.getChildren().isEmpty()) treeItemRelationShips.add(new TreeItemRelationShip(new TreeItem<>(new ANode("", "", new HashSet<>(), new HashSet<>())), missingTreeItem));
            else treeItemRelationShips.add(new TreeItemRelationShip(missingTreeItem, new TreeItem<>(new ANode("", "", new HashSet<>(), new HashSet<>()))));
        }
        tableView.setItems(treeItemRelationShips);

        tableView.getColumns().addAll(parentCol, childCol);
    }

    /**
     * Accumulates all TreeItems that aren't in the list of available TreeItems in the list of missing TreeItems.
     * Does this by checking the conceptID of the TreeItems' ANodes.
     */
    private LinkedList<TreeItem<ANode>> findMissingNodesBelowByID(TreeItem<ANode> root, List<TreeItem<ANode>> available) {
        LinkedList<TreeItem<ANode>> missing = new LinkedList<>();
        HashSet<String> conceptIDs = new HashSet<>();

        for (TreeItem<ANode> treeItem : available) conceptIDs.add(treeItem.getValue().conceptId());
        findMissingNodesBelowByIDRec(root, conceptIDs, missing);
        return missing;
    }

    private void findMissingNodesBelowByIDRec(TreeItem<ANode> root, Set<String>conceptIDs, List<TreeItem<ANode>> missing) {
        if (!conceptIDs.contains(root.getValue().conceptId())) missing.add(root);
        for (TreeItem<ANode> child : root.getChildren()) findMissingNodesBelowByIDRec(child, conceptIDs, missing);
    }



    private void createTreeItemRelationList(TreeItem<ANode> root, ObservableList<TreeItemRelationShip> list) {
        for (TreeItem<ANode> child : root.getChildren()) list.add(new TreeItemRelationShip(root, child));
        for (TreeItem<ANode> child : root.getChildren()) createTreeItemRelationList(child, list);
    }


}
