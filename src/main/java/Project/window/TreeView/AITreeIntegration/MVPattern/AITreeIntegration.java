package Project.window.TreeView.AITreeIntegration.MVPattern;

import Project.model.ANode;
import Project.window.PopUp.LittlePopUp;
import Project.window.SupportingUI.TextSearch.SearchTree;
import Project.window.TreeView.TreeAnalysis.TreeAnalysisUtils;
import Project.window.TreeView.TreeAnalysis.TreeViewEditing.TreeViewEditor;
import Project.window.TreeView.TreeViewSetup;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.IOException;
import java.util.*;

import static Project.window.TreeView.TreeAnalysis.TreeAnalysisUtils.replaceANodeByCopy;


public class AITreeIntegration {

    private final TreeItem<ANode> realTreeMasterRoot;
    private TreeItem<ANode> realTreeRoot; // root of the subtree being worked on. NOT the master root of the tree the subtree belongs to.
    private TreeItem<ANode> aiTreeRoot;         // this tree's master root, which is usually equivalent to realTreeRoot.
    private final AITreeIntegrationController controller;
    private final TableView<TreeItemRelationShip> tableView;
    private final TreeView<ANode> aiTreeView;
    private final TreeViewSetup treeViewSetup;
    private final TreeViewEditor<ANode> treeViewEditor;
    private final HashMap<TreeItem<ANode>, String> treeCellColorMap = new HashMap<>();  //maps some TreeItems of the AI generated tree to css colors
    private LinkedList<TreeItem<ANode>> missingTreeItems = new LinkedList<>();        //holds TreeItems of the original Tree that are not part of the AI tree
    private LinkedList<TreeItem<ANode>> nonUniqueIDTreeItems = new LinkedList<>();
    private final String missingNodeColor = "lightSalmon";
    private final String newTwinCellColor = "peachPuff";        //css colors found in treeCellColorMap
    private final String newEdgeCellColor = "lightBlue";
    private final String newNameCellColor = "paleGoldenRod";
    private final String noUniqueCellColor = "darkSalmon";

    /**
     * @param masterRoot Master root of the tree that contains realTreeRoot.
     * @param realTreeRoot Root of the subtree that is being worked on. Will be deleted if the new tree is accepted.
     * @param treeViewEditor TreeViewEditor instance used by the real tree.
     * @throws IOException
     */
    public AITreeIntegration(TreeItem<ANode> masterRoot, TreeItem<ANode> realTreeRoot, TreeViewEditor<ANode> treeViewEditor) throws IOException {
        //create the window
        AITreeIntegrationView aiTreeIntegrationView = new AITreeIntegrationView();
        this.controller = aiTreeIntegrationView.getController();
        this.tableView = controller.getTableView();

        this.aiTreeView = controller.getTreeView();
        this.treeViewEditor = treeViewEditor;
        this.treeViewSetup = new TreeViewSetup(treeViewEditor);
        this.realTreeRoot = realTreeRoot;
        this.realTreeMasterRoot = masterRoot;


        Scene scene = LittlePopUp.showPopup(aiTreeIntegrationView.getRoot(), "Modify tree via AI", 1000, 500);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.F) controller.getTreeSearchField().requestFocus();
        });

        controller.getParseButton().setOnAction(e -> {
            controller.getTableView().getColumns().clear();
            analyzeReply(controller.getAiTextArea().getText());
            treeViewSetup.setCellFactory(aiTreeView, treeViewEditor, aiTreeRoot.getValue());
            aiTreeView.setRoot(aiTreeRoot);
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

        controller.getSearchForDupIDsButton().setOnAction(e -> searchForDupIDs(aiTreeRoot));    // wanna check if any IDs within aiTree are duplicate

        controller.getAddMissingToTreeButton().setOnAction(e -> {
            if (aiTreeRoot == null || missingTreeItems.isEmpty()) return;
            LinkedList<TreeItem<ANode>> twinsOfMissing = new LinkedList<>();
            LinkedList<TreeItem<ANode>> mock = new LinkedList<>();
            for (TreeItem<ANode> missingTreeItem : missingTreeItems) {
                TreeItem<ANode> twin = makeTwin(missingTreeItem, missingTreeItem.getValue().conceptId(), missingTreeItem.getValue().name(), twinsOfMissing, mock, false);
                twinsOfMissing.add(twin);
                aiTreeRoot.getChildren().add(twin);
                aiTreeRoot.getValue().children().add(twin.getValue());
            }


        });
    }

    /**
     *
     * @param root
     * @return
     */
    private boolean searchForDupIDs(TreeItem<ANode> root) {
        LinkedList<TreeItem<ANode>> allAITreeItems = new LinkedList<>();
        TreeAnalysisUtils.accumulateForEveryNodeBelow(aiTreeRoot, allAITreeItems, t -> t);
        HashMap<TreeItem<ANode>, Collection<TreeItem<ANode>>> duplicateItemsMap = TreeAnalysisUtils.lookForDuplicateIDsInTree(allAITreeItems, root, false, realTreeRoot);


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

    private void acceptTree() {
        if (!searchForDupIDs(aiTreeRoot) || !searchForDupIDs(realTreeMasterRoot)) return;   //want to check if any of the IDs of aiTree already occur anywhere in the full real tree
        TreeItem<ANode> realTreeParent = realTreeRoot.getParent();
        treeViewSetup.delete(realTreeRoot);
        treeViewSetup.cut(aiTreeRoot);
        treeViewSetup.paste(realTreeParent, realTreeMasterRoot);
        aiTreeView.setRoot(null);
        realTreeRoot = aiTreeRoot;
    }


    private void analyzeReply(String aiReply) {

        LinkedList<TreeItem<ANode>> twins = new LinkedList<>();
        LinkedList<TreeItem<ANode>> newTwins = new LinkedList<>(); //twins that weren't created from existing TreeItems

        //for every line of the written edge list
        for (String line : aiReply.split("\n")) {
            String[] fields = line.split("\t");
            if (fields.length != 4) continue;
            String parentID = fields[0].trim();
            String childID = fields[2].trim();
            String parentName = fields[1].trim();
            String childName = fields[3].trim();

            TreeItem<ANode> parent = findTreeItemWithConceptID(parentID, realTreeRoot); //try to find an existing treeItem with that conceptID
            TreeItem<ANode> child = findTreeItemWithConceptID(childID, realTreeRoot);   //for partent and child (or return null)
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
        LinkedList<TreeItem<ANode>> aiTreeRoots = new LinkedList<>();
        for (TreeItem<ANode> treeItemTwin : twins) {
            if (treeItemTwin.getParent() == null) {
                System.out.println("root found: " + treeItemTwin.getValue().name());
                aiTreeRoots.add(treeItemTwin);
            }
        }
        if (aiTreeRoots.size() == 1) aiTreeRoot = aiTreeRoots.get(0);   //if there's one root its straightforward
        else {                                                          // if there's many roots, create a root and make them children of it
//            aiTreeRoot = new TreeItem<ANode>(treeViewSetup.makeAnode("AIRoot", realTreeRoot.getValue())); this would be wrong because the new ai root does not correspond to that anode.
            aiTreeRoot = new TreeItem<ANode>(treeViewSetup.makeAnode("AIRoot", new ANode("aiRoot", "aiRoot", new HashSet<>(), new HashSet<>())));
            for (TreeItem<ANode> possibleRoot : aiTreeRoots) {
                aiTreeRoot.getChildren().add(possibleRoot);
                aiTreeRoot.getValue().children().add(possibleRoot.getValue());
            }
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
        createTreeItemRelationList(aiTreeRoot, treeItemRelationShips);  //add all TreeItems of the AI tree to the table
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
