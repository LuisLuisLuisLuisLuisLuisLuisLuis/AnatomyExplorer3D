package Project.window.TreeView;

import Project.model.ANode;
import Project.model.TreeExport;
import Project.window.PopUp.LittlePopUp;
import Project.window.TreeView.AITreeIntegration.MVPattern.AITreeIntegration;
import Project.window.TreeView.TreeAnalysis.MVPPattern.TreeAnalysisView;
import Project.window.TreeView.TreeAnalysis.TreeAnalysisUtils;
import Project.window.TreeView.TreeAnalysis.TreeViewEditing.TreeViewEditor;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.util.StringConverter;

import java.io.*;
import java.util.*;
import java.util.function.Function;

import static Project.model.FileUtil.collectFileIDsBelow;
import static Project.window.TreeView.TreeAnalysis.TreeAnalysisUtils.*;

/**
 * A class to create TreeItems corresponding to ANode objects and their hierarchy.
 */
public class TreeViewSetup {


    private final ObjectProperty<Runnable> onEvent =
            new SimpleObjectProperty<>();

    public final ObjectProperty<Runnable> onEventProperty() {
        return onEvent;
    }
    //an outsider may set the runnable so that they may listen for events
    public final void setOnEvent(Runnable runnable) {
        onEvent.set(runnable);
    }

    public final Runnable getOnEvent() {
        return onEvent.get();
    }
    //this gets run when cut, paste, rename or delete are run (which change the fileIDs of the ANodes)
    private void runOnANodeFileIDsModified() {
        if (getOnEvent() != null) getOnEvent().run();
    }


    private final TreeViewEditor<ANode> treeViewEditor1;

    public TreeViewSetup(TreeViewEditor<ANode> treeViewEditor) {
        this.treeViewEditor1 = treeViewEditor;
    }
    public TreeViewSetup() {
        this.treeViewEditor1 = new TreeViewEditor<>();
    }

    /**
     * @param treeView will be populated with treeItems.
     * @param modelRoot ANode root. will be root of tree.
     */
    public void setupTree(TreeView<ANode> treeView, ANode modelRoot) {
        setCellFactory(treeView, treeViewEditor1, modelRoot);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE); //enable multiple selection

        //populate the treeView
        TreeItem<ANode> root = createTreeItemsRec(modelRoot);
        treeView.setRoot(root);
    }

    /**
     * Helper Function to recursively create TreeItems to populate the TreeView.
     */
    private TreeItem<ANode> createTreeItemsRec(ANode treeRoot) {
        TreeItem<ANode> item = new TreeItem<>(treeRoot);
        if (!treeRoot.children().isEmpty()) {
            for (ANode child : treeRoot.children()) {
                item.getChildren().add(createTreeItemsRec(child));
            }
        }
        return item;
    }


    /**
     * Sets CellFactory for the TreeView, i.e. changes how the cells are made
     * so that they can be cut, pasted and renamed.
     * @param treeView
     * @param _treeViewEditor
     */
    public void setCellFactory(TreeView<ANode> treeView, TreeViewEditor<ANode> _treeViewEditor, ANode modelRoot) {
        treeView.setEditable(true);
        treeView.setCellFactory(tv -> {
            return new TextFieldTreeCell<ANode>(
                    new StringConverter<ANode>() {
                        @Override
                        public String toString(ANode node) {
                            return node == null ? "" : node.toString();
                        }
                        //create temporary ANode holding the string entered by the user
                        @Override
                        public ANode fromString(String newname) {
                            return new ANode("", newname, new HashSet<>(), new HashSet<>());
                        }
                    }
            ) {
                @Override
                public void updateItem(ANode item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setContextMenu(null);
                    } else {
                        //show menu on right click
                        setText(item.toString());
                        MenuItem cut = new MenuItem("Cut");
                        MenuItem paste = new MenuItem("Paste");
                        MenuItem rename = new MenuItem("Rename");
                        MenuItem delete = new MenuItem("Delete");
                        MenuItem restore = new MenuItem("Restore");
                        MenuItem extract = new MenuItem("Extract children");
                        MenuItem create = new MenuItem("Create");
                        MenuItem fileIDInfo = new MenuItem("FileID Info");
                        MenuItem print = new MenuItem("Print Subtree");
                        MenuItem printL = new MenuItem("Print only leaves with Files of this Subtree");
                        MenuItem printF = new MenuItem("Print FileIDs of Subtree");
                        MenuItem splitLeftRight = new MenuItem("Split Subtree in Left/Right");
                        MenuItem moveLRBehind1LastWord = new MenuItem("Move L/R behind n'th last word");
                        MenuItem askAI = new MenuItem("Ask AI");

                        cut.setOnAction(e -> cut(this.getTreeItem()));
                        paste.setOnAction(e -> paste(this.getTreeItem(), this.getTreeView().getRoot()));
                        delete.setOnAction(e -> delete(this.getTreeItem()));
                        restore.setOnAction(e -> restore());
                        rename.setOnAction(e -> getTreeView().edit(this.getTreeItem()));
                        create.setOnAction(e -> this.create());
                        extract.setOnAction(e -> extract(this.getTreeItem()));
                        fileIDInfo.setOnAction(e -> {
                            try {this.showFileIDInfo();}
                            catch (IOException ioe) {
                                System.out.println("thats not gone well: ");
                                ioe.printStackTrace();}
                        });
                        print.setOnAction(e -> System.out.println(TreeExport.printSubtreeBelow(this.getTreeItem(), true)));
                        printF.setOnAction(e -> System.out.println(collectFileIDsBelow(this.getTreeItem().getValue()).keySet()));
                        printL.setOnAction(e -> {
                            try {
                                BufferedReader bufferedReader = new BufferedReader(new FileReader(new File("C:\\Users\\dnexu\\Documents\\msc\\Coding\\Python\\API\\BP3DMerging\\bp3d_v3_parts.txt")));
                                String line;
                                while ((line = bufferedReader.readLine()) != null) {
                                    String name = line.split("\t")[1].strip();
                                    if (!TreeAnalysisUtils.doesNodeWFilesContainName(this.getTreeItem(), name)) System.out.println(name);
                                }
                            } catch (FileNotFoundException ex) {
                                throw new RuntimeException(ex);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        });

                        splitLeftRight.setOnAction(e -> {
                            boolean isLRsymetric = isLeftRightSymetricBelow(this.getTreeItem(), "");
                            if (isLRsymetric) splitSubtreeLeftRight(this.getTreeItem());
                        });
                        moveLRBehind1LastWord.setOnAction(e -> {
                            Integer n = LittlePopUp.askForGreater0Int();
                            if (n == null) return;
                            applyLRNameChangeRec(this.getTreeItem(),n);
                            getTreeView().refresh();
                        });

                        askAI.setOnAction(e -> {
                            try {
                                AITreeIntegration aiTreeIntegration = new AITreeIntegration(this.getTreeView().getRoot(), this.getTreeItem(), treeViewEditor1);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        });

                        if (getTreeItem().getParent() != null) setContextMenu(new ContextMenu(delete, restore, cut, paste, extract, create, rename, fileIDInfo, print, printF, printL, splitLeftRight, moveLRBehind1LastWord,askAI));
                        else setContextMenu(new ContextMenu(restore, paste, rename, create, fileIDInfo, print, printF, printL, splitLeftRight, moveLRBehind1LastWord, askAI));
                    }
                }


                private final TreeViewEditor<ANode> treeViewEditor1 = _treeViewEditor;

                private void splitSubtreeLeftRight(TreeItem<ANode> treeItem) {
                    TreeItem<ANode> rightRoot = new TreeItem<>(makeAnode("right " + treeItem.getValue().getName(), modelRoot));
                    TreeItem<ANode> leftRoot = new TreeItem<>(makeAnode("left " + treeItem.getValue().name(), modelRoot));
                    treeItem.getParent().getChildren().add(rightRoot);
                    treeItem.getParent().getChildren().add(leftRoot);
                    treeItem.getParent().getValue().children().add(rightRoot.getValue());
                    treeItem.getParent().getValue().children().add(leftRoot.getValue());
                    collectSideLeavesBelowRec(treeItem, rightRoot, "right");
                    collectSideLeavesBelowRec(treeItem, leftRoot, "left");
                }

                private void collectSideLeavesBelowRec(TreeItem<ANode> root, TreeItem<ANode> sidedRoot, String side) {

                    Function<String, Boolean> isSided = (name) -> name.toLowerCase().contains("left") || name.toLowerCase().contains("right");
                    Function<String, String> getExpectedSide = (name) -> {
                        int il = name.toLowerCase().indexOf("left");
                        int ir = name.toLowerCase().indexOf("right");
                        if (il >= 0) {
                            if (ir >= 0) {
                                if (ir > il)  return "right";
                                else return "left";
                            } else return "left";
                        } else {
                            if (ir >= 0) return "right";
                        } return "";
                    };

                    for (TreeItem<ANode> child : new LinkedList<>(root.getChildren())) {
                        if (!isSided.apply(child.getValue().name()) && !child.getValue().children().isEmpty()) {

                            TreeItem<ANode> sidedChild = createSidedTreeItemFromUnsided(child, side);
                            sidedRoot.getChildren().add(sidedChild);
                            sidedRoot.getValue().getChildren().add(sidedChild.getValue());
                            collectSideLeavesBelowRec(child, sidedChild, side);
                        } else {
                            if (getExpectedSide.apply(child.getValue().name()).equals(side)) {
                                cut(child);
                                paste(sidedRoot, this.getTreeView().getRoot());
                                if (child.getValue().name().replace(side+" ", "").trim().equals(sidedRoot.getValue().getName().replace(side, "").trim()) && sidedRoot.getValue().fileIds().containsAll(child.getValue().fileIds())) {
//                                if (child.getValue().name().equals(sidedRoot.getValue().getName()) && sidedRoot.getValue().fileIds().containsAll(child.getValue().fileIds())) {
                                    sidedRoot.getChildren().remove(child);
                                    sidedRoot.getValue().children().remove(child.getValue());
                                    runOnANodeFileIDsModified();
                                } else System.out.println("false because " + child.getValue().name().replace(side+" ", "").trim() + " != " + sidedRoot.getValue().getName().replace(side, "").trim());
                                continue;
                            } else continue;
                        }

                    }
                }

                private TreeItem<ANode> createSidedTreeItemFromUnsided(TreeItem<ANode> treeItem, String side) {
                    return new TreeItem<>(makeAnode(side + " " + treeItem.getValue().name(), modelRoot));
                }

                private boolean isLeftRightSymetricBelow(TreeItem<ANode> root, String expectedSide) {

                    Function<String, String> getExpectedSide = (name) -> {
                        int il = name.toLowerCase().indexOf("left");
                        int ir = name.toLowerCase().indexOf("right");
                        if (il >= 0) {
                            if (ir >= 0) {
                                if (ir > il)  return "right";
                                else return "left";
                            } else return "left";
                        } else {
                            if (ir >= 0) return "right";
                        } return "";
                    };
                    Function<String, String> switchLR = (name) -> switch (name.toLowerCase()) {
                        case "left" -> "right";
                        case "right" -> "left";
                        default -> name;
                    };
                    Function<String, Boolean> isSided = (name) -> name.toLowerCase().contains("left") || name.toLowerCase().contains("right");
                    Function<String, String> otherSideName = (name)->{
                        name = name.toLowerCase();
                        if (name.contains("left")) name = name.replace("left", "xxxx");
                        else if (name.contains("right")) name = name.replace("right", "left");
                        return name.replace("xxxx", "right");};

                    if (root.getChildren().isEmpty()) return true;
                    expectedSide = getExpectedSide.apply(root.getValue().name());
                    LinkedList<String> sidedItems = new LinkedList<>();

                    for (TreeItem<ANode> child : root.getChildren()) {
                        if (isSided.apply(child.getValue().name())) {
                            if (sidedItems.contains(otherSideName.apply(child.getValue().name()))) sidedItems.remove(otherSideName.apply(child.getValue().name()));
                            else sidedItems.add(child.getValue().name());
                        }
                    }
                    if (expectedSide.isEmpty() && !sidedItems.isEmpty()) return false;
                    else {
                        for (String sidedItem : sidedItems) {
                            if (sidedItem.contains(switchLR.apply(expectedSide))) return false;
                        }
                    }
                    for (TreeItem<ANode> child : root.getChildren()) {
                        if (child.getValue().name().toLowerCase().contains("left")) expectedSide = "left";
                        else if (child.getValue().name().toLowerCase().contains("right")) expectedSide = "right";
                        if (!isLeftRightSymetricBelow(child, expectedSide)) return false;
                    }
                    return true;
                }


                private void create() {
                    String id = generateUnusedID(modelRoot);
                    ANode newnode = new ANode(
                            id, id, new HashSet<>(), new HashSet<>()
                    );
                    TreeItem<ANode> newTreeItem = this.treeViewEditor1.create(this.getTreeItem(), newnode);
                    this.getTreeItem().getValue().children().add(newnode);  //also create new anode relationship
                    getTreeView().edit(newTreeItem);

                }

                /**
                 * Shows popup window with FileID info
                 * @throws IOException
                 */
                private void showFileIDInfo() throws IOException {
                    TreeAnalysisView treeAnalysisView = new TreeAnalysisView();

                    TableColumn<FileIDRow, String> col1 = new TableColumn<>("FileID");
                    TableColumn<FileIDRow, String> col2 = new TableColumn<>("Occurrences in Subtree");
                    col1.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().col1));
                    col1.setMinWidth(100);
                    col1.setPrefWidth(100);
                    col1.setMaxWidth(100);
                    col2.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().col2));
                    col2.setCellFactory(col -> new TableCell<>() {
                        @Override
                        protected void updateItem(String value, boolean empty) {
                            super.updateItem(value, empty);
                            if (empty || getTableRow().getItem() == null) {
                                setText(null);
                                setStyle("");
                            } else {
                                FileIDRow row = getTableRow().getItem();
                                setText(value);
                                setStyle("-fx-background-color: " + row.colorCss() + ";");
                            }
                        }
                    });

                    ANode thisanode = this.getTreeItem().getValue();    //so i dont have to type getvalue all the time
                    HashMap<String, LinkedList<ANode>> fileIDtoANode = collectFileIDsBelow(thisanode);
                    LinkedList<String> sortedFileIDs = new LinkedList<>(fileIDtoANode.keySet());
                    sortedFileIDs.sort((fileID1, fileID2) -> {
                        if ((fileIDtoANode.get(fileID1).getFirst() == thisanode )) {
                            if (fileIDtoANode.get(fileID2).getFirst() != thisanode)  return -1;
                            else return 0;
                        } else {
                            if (fileIDtoANode.get(fileID2).getFirst() == thisanode) return 1;
                            else return 0;
                        }
                    });

                    HashSet<String> specials = new HashSet<>(List.of(new String[]{"FJ3583", "FJ1770", "FJ1758", "FJ1817", "FJ1806", "FJ1760", "FJ1808"}));
                    ObservableList<FileIDRow> rows = FXCollections.observableArrayList();
                    for (String fileID : sortedFileIDs) {
                        String colorCss;
                        boolean occursInThisNode = fileIDtoANode.get(fileID).getFirst() == thisanode;
                        boolean occursInLeaf = false;
                        for (ANode anode : fileIDtoANode.get(fileID)) {
                            if (anode.children().isEmpty()) {
                                occursInLeaf = true;
                                break;
                            }
                        }
                        if (!occursInThisNode) {
                            if (!occursInLeaf) colorCss = "lightCoral";
                            else colorCss = "lightGreen";
                        } else {
                            if (!occursInLeaf) colorCss = "sandyBrown";
                            else colorCss = "springGreen";
                        }
                        if (specials.contains(fileID)) System.out.println(fileID + ": " + fileIDtoANode.get(fileID).toString());
                        rows.add(new FileIDRow(fileID, fileIDtoANode.get(fileID).toString(), colorCss));
                    }

                    treeAnalysisView.getController().getFileIDTableView().setItems(rows);

                    treeAnalysisView.getController().getFileIDTableView().getColumns().addAll(col1, col2);

                    treeAnalysisView.getController().getBotlabel().setText(String.valueOf(fileIDtoANode.keySet().size()) + " FileIDs");

                    LittlePopUp.showPopup(treeAnalysisView.getRoot(), "FileID Info for " + this.getTreeItem().getValue().name() + " (id=" + this.getTreeItem().getValue().conceptId() + ")", 1000, 500);

                    //--------window is now visible---------------------

                    treeAnalysisView.getController().getAddFileIDButton().setOnAction(e -> {
                        String fileID = treeAnalysisView.getController().getAddFileIDTextField().getCharacters().toString();
                        if (fileID.isBlank()) return;
                        runOnANodeFileIDsModified();
                    });

                    treeAnalysisView.getController().getRemoveFileIDButton().setOnAction(e -> {
                        this.getTreeItem().getValue().fileIds().remove(treeAnalysisView.getController().getAddFileIDTextField().getText());
//                        removeFileIDsFromParentsRec(this.getTreeItem().getParent(), List.of(treeAnalysisView.getController().getAddFileIDTextField().getText()), this.getTreeItem());
                        runOnANodeFileIDsModified();
                    });

                    treeAnalysisView.getController().getSearchButton().setOnAction(e -> {
                        for (FileIDRow fileIDRow : rows) {
                            if (col1.getCellData(fileIDRow).contains(treeAnalysisView.getController().getAddFileIDTextField().getText().toUpperCase())) {
                                treeAnalysisView.getController().getFileIDTableView().getSelectionModel().clearSelection();
                                treeAnalysisView.getController().getFileIDTableView().getSelectionModel().select(fileIDRow);
                                treeAnalysisView.getController().getFileIDTableView().scrollTo(fileIDRow);
                            }
                        }
                    });

                }

                //implements rename
                //gets temporaryANode from StringConverter from which the entered name is extracted
                @Override
                public void commitEdit(ANode temporaryANode) {
                    if (temporaryANode == null || temporaryANode.name().isBlank()) {
                        cancelEdit();
                        return;
                    }
                    this.getItem().setName(temporaryANode.getName());
                    super.commitEdit(this.getItem());
                    setText(this.getItem().toString());
                    runOnANodeFileIDsModified();
                }
            };
        });

    }

    public static void applyLRNameChangeRec(TreeItem<ANode> root, int wordpos) {
        Function<TreeItem<ANode>,Boolean> changeName = (treeItem) -> {
            String side = treeItem.getValue().name().contains("left") ? "left " : (treeItem.getValue().name().contains("right") ? "right " : "");
            if (side.isEmpty()) return false;
            String newname = treeItem.getValue().name().replace(side, "");
            if (wordpos == 1) newname = newname.substring(0, newname.lastIndexOf(" ") + 1) + side + newname.substring(newname.lastIndexOf(" ") + 1);
            else {
                String firstbit = new String(newname);
                for (int j = 0; j < wordpos; j++) {
                    int upperBound = firstbit.lastIndexOf(" ");
                    firstbit = firstbit.substring(0, upperBound > 0 ? upperBound : firstbit.length());
                }
                newname = firstbit + " " + side + newname.substring(firstbit.length()+1);
            }
            treeItem.getValue().setName(newname);
            return true;
        };
        TreeAnalysisUtils.applyRec(root, changeName);
    }

    public record FileIDRow(String col1, String col2, String colorCss) {}

    public void extract(TreeItem<ANode> parent) {
        if (parent.getParent() == null) return;
        for (TreeItem<ANode> child : parent.getChildren()) {
            Collection<String> fileIDsToRemove = collectFileIDsBelow(child.getValue()).keySet();
            parent.getValue().children().remove(child.getValue());      //set the anode relationships
            parent.getParent().getValue().getChildren().add(child.getValue());
            parent.getValue().fileIds().removeAll(fileIDsToRemove);     // remove children subtree's fileIDs from parent.
        }
        treeViewEditor1.extractChildren(parent);
        runOnANodeFileIDsModified();
    }

    public void cut(TreeItem<ANode> treeItem) {
        if (!treeViewEditor1.canCut().get()) return;
        Collection<String> fileIDsToRemove = collectFileIDsBelow(treeItem.getValue()).keySet(); //not doing this.getFileIDs because what if theres a fileID in subtree that this doesnt have

        if (treeItem.getParent() != null) {
//            removeFileIDsFromParentsRec(treeItem.getParent(), fileIDsToRemove);
            treeItem.getParent().getValue().children().remove(treeItem.getValue()); //remove the anode relationship also!
        }

        this.treeViewEditor1.cut(treeItem);
        runOnANodeFileIDsModified();
    }

    /**
     * Paste whatever the clipboard of this instance's TreeViewEditor holds under the given treeItem.
     * @param treeItem
     * @param root It will be checked if any of the conceptIDs of the ANodes being pasted already occur in the tree below
     *             root. If so, the user will be informed via a PopUp window and asked if the pasted nodes in question
     *             should receive new randomized conceptIDs or else the pasting will not be allowed.
     *             Usually this means that this parameter is the master root of the tree that contains treeItem.
     */
    public void paste(TreeItem<ANode> treeItem, TreeItem<ANode> root) {
        if (!treeViewEditor1.canPaste().get()) return;
        // Check to see if there are already ANodes in this tree that have the same conceptID as any of the
        // Anodes of the TreeItems being pasted now.
        // If so, inform the user via a popup and ask whether to proceed.
        // If so, replace every ANode whose ID already occurs in this tree with a new ANode with different ID that is otherwise identical.
        //      Change the ANode relationships accordingly before pasting.
        // Then follows the regular paste operation.
        LinkedList<TreeItem<ANode>> itemsToPaste = new LinkedList<>();
        TreeAnalysisUtils.accumulateForEveryNodeBelow(treeViewEditor1.clipBoardContent(), itemsToPaste, t -> t);
        HashMap<TreeItem<ANode>, Collection<TreeItem<ANode>>> duplicateItemsMap = TreeAnalysisUtils.lookForDuplicateIDsInTree(itemsToPaste, root, false);

        if (!duplicateItemsMap.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (TreeItem<ANode> dupTreeItem : duplicateItemsMap.keySet()) {
                for (TreeItem<ANode> aNode : duplicateItemsMap.get(dupTreeItem)) stringBuilder.append("ID: " + dupTreeItem.getValue().conceptId() + " Name: " + dupTreeItem.getValue().name() + " | ID: " + aNode.getValue().conceptId() + " Name: " + aNode.getValue().name() + "\n");
            }

            String popupText = """
            Pasting will result in the following nodes being duplicated.
            Assign new IDs to duplicated nodes and continue?
            
            Item to paste | already present item with same ID
            
            """ + stringBuilder.toString();
            if (!LittlePopUp.showScrollableTextPopup("Warning", popupText)) return;
            for (TreeItem<ANode> itemWDuplicateANode : duplicateItemsMap.keySet()) replaceANodeByCopy(itemWDuplicateANode, root);
        }

        //now the regular pasting.
        if (!this.treeViewEditor1.paste(treeItem)) return; //this node IS the targetParent

        //getLast() weil treeItem hier das parent ist und wir uns für die fileIDs des gerade hier gepasteten childs interessieren, was das letzte in der liste children ist.
//        Collection<String> fileIDsToAdd = collectFileIDsBelow(treeItem.getChildren().getLast().getValue()).keySet();  it is no longer desired for parents to hold all fileids of their subtree
//        for (String fileID : fileIDsToAdd) System.out.println(fileID);
//        addFileIDsToParentsRec(treeItem, fileIDsToAdd);   //add the fileIDs to all parents
        for (TreeItem<ANode> childTreeItem : treeItem.getChildren()) {
            ANode childNode = childTreeItem.getValue(); //make sure the anode relationships are also created!
            if (!treeItem.getValue().children().contains(childNode)) treeItem.getValue().children().add(childNode);
        }
        runOnANodeFileIDsModified();
    }

    public void delete(TreeItem<ANode> treeItem) {
        if (treeItem.getParent() == null) return;
//        removeFileIDsFromParentsRec(treeItem.getParent(), treeItem.getValue().getFileIds(), treeItem);
//        removeFileIDsFromParentsRec(treeItem.getParent(), collectFileIDsBelow(treeItem.getValue()).keySet());

        treeItem.getParent().getValue().children().remove(treeItem.getValue()); //also delete anode relationship
        this.treeViewEditor1.delete(treeItem);
        runOnANodeFileIDsModified();
    }

    public void restore() {
        if (!treeViewEditor1.canRestore().get()) return;
        TreeItem<ANode> restored = treeViewEditor1.restore();
//        addFileIDsToParentsRec(restored.getParent(), new LinkedList<>());   //add the fileIDs to all parents
        restored.getParent().getValue().children().add(restored.getValue());
        runOnANodeFileIDsModified();
    }

    public static void addFileIDsToParentsRec(TreeItem<ANode> firstParent, Collection<String> fileIDs) {
        for (String fileID : fileIDs) if (!firstParent.getValue().getFileIds().contains(fileID)) firstParent.getValue().getFileIds().add(fileID);
        if (firstParent.getParent() != null) addFileIDsToParentsRec(firstParent.getParent(), fileIDs);
    }

    /**
     * Removes the fileIDs from all parents recursively.
     * @param firstParent The first parent from which the fileIDs will be removed.
     * @param fileIDs The fileIDs.
     */
    public static void removeFileIDsFromParentsRec(TreeItem<ANode> firstParent, Collection<String> fileIDs) {
        firstParent.getValue().getFileIds().removeAll(fileIDs);
        if (firstParent.getParent() != null) removeFileIDsFromParentsRec(firstParent.getParent(), fileIDs);
    }


    /**
     * Wrapper for removeFileIDsFromParentsRec().
     * Removes the fileIDs from the parents recursively except when they occur in the subtree below the parent elsewhere than avoid.
     * -> Thus, ensures that every fileID that occurs in the parent's subtree (except under avoid) is not removed from the parent.
     * This makes this method suitable for use when it is necessary that parents always hold the fileIDs of their subtree.
     */
    public static void removeFileIDsFromParentsRec(TreeItem<ANode> firstParent, Collection<String> fileIDs, TreeItem<ANode> avoid) {
        HashMap<String, Boolean> fileIDMap = new HashMap<>();
        for (String fileID : fileIDs) fileIDMap.put(fileID, false);
        removeFileIDsFromParentsRec(firstParent, fileIDMap, avoid);
    }
    /**
     * Removes the fileIDs from the parents recursively except when they occur in the subtree below the parent elsewhere than avoid.
     *      * -> Thus, ensures that every fileID that occurs in the parent's subtree (except under avoid) is not removed from the parent.
     *      * This makes this method suitable for use when it is necessary that parents always hold the fileIDs of their subtree.
     * @param firstParent
     * @param fileListMap
     * @param avoid
     */
    public static void removeFileIDsFromParentsRec(TreeItem<ANode> firstParent, HashMap<String, Boolean> fileListMap, TreeItem<ANode> avoid) {
        for (String fileID: fileListMap.keySet()) {
            if (!fileListMap.get(fileID)) {
                boolean isInSubtreeExceptAvoid = isFileIDInSubtreeExceptAvoidAndItsParent(fileID, firstParent, avoid);
                if (isInSubtreeExceptAvoid) {
                    fileListMap.put(fileID, isInSubtreeExceptAvoid);
                    continue;
                } else {
                    firstParent.getValue().getFileIds().remove(fileID);;
                }
            } else continue;
        }
        if (firstParent.getParent() != null) removeFileIDsFromParentsRec(firstParent.getParent(), fileListMap, avoid.getParent());
    }

    /**
     * Is this file in the subtree of parentOfAvoid, excluding avoid and the parent itself?
     * @param fileID
     * @param parentOfAvoid
     * @param avoid
     * @return
     */
    public static boolean isFileIDInSubtreeExceptAvoidAndItsParent(String fileID, TreeItem<ANode> parentOfAvoid, TreeItem<ANode> avoid) {
        for (TreeItem<ANode> child : parentOfAvoid.getChildren()) {
            if (child == avoid) continue;
            if (isFileIDinSubtreeExceptNode(fileID, child)) return true;
        }
        return false;
    }

    /**
     * Is fileID in the subtree of treeItem?
     */
    public static boolean isFileIDinSubtreeExceptNode(String fileID, TreeItem<ANode> treeItem) {
        if (treeItem.getValue().fileIds().contains(fileID)) return true;
        for (TreeItem<ANode> child : treeItem.getChildren()) {
            if (isFileIDinSubtreeExceptNode(fileID, child)) return true;
        }

        return false;
    }

    public static ANode makeAnode(String name, ANode modelRoot) {
        String id = generateUnusedID(modelRoot);
        return new ANode(
                id, name, new HashSet<>(), new HashSet<>()
        );
    }

    /**
     * Generate a new conceptID that is not in use in the tree of modelRoot.
     */
    public static String generateUnusedID(ANode modelRoot) {
        String id;
        do {
            Random r = new Random();
            int randomID = r.nextInt(1, 10000000);
            id = "newnode" + randomID;
        } while (doesIDExistBelow(modelRoot, id));
        return id;
    }
}