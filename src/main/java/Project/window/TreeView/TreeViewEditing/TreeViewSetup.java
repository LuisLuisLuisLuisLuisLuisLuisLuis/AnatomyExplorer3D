package Project.window.TreeView.TreeViewEditing;

import Project.model.ANode;
import Project.model.TreeExport;
import Project.window.PopUp.LegendItem;
import Project.window.PopUp.LittlePopUp;
import Project.window.TreeView.TreeAnalysis.MVPPattern.FileIDView;
import Project.window.TreeView.TreeAnalysis.TreeAnalysisUtils;
import Project.window.TreeView.TreeViewEditing.Command.UndoableANodeTreeViewEditor;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

import java.io.*;
import java.util.*;

import static Project.model.FileUtil.collectFileIDsBelow;
import static Project.window.TreeView.TreeAnalysis.TreeAnalysisUtils.*;

/**
 * A class to create TreeItems corresponding to ANode objects and their hierarchy.
 */
public class TreeViewSetup {


//    private final ObjectProperty<Runnable> onEvent = new SimpleObjectProperty<>();
//
//    public final ObjectProperty<Runnable> onEventProperty() {
//        return onEvent;
//    }
//
//    /*
//     * Set a Runnable that is run when changes are made to the tree.
//     */
//    public final void setOnEvent(Runnable runnable) {
//        onEvent.set(runnable);
//    }
//
//    /*
//     * Runnable property. The Runnable runs whenever a change is made to the tree.
//     */
//    public final Runnable getOnEvent() {
//        return onEvent.get();
//    }
//
//    //this gets run when the tree (and the fileIDs) changes
//    private void runOnANodeFileIDsModified() {
//        if (getOnEvent() != null) onEvent.get().run();
//    }

    private final UndoableANodeTreeViewEditor treeViewEditor1;

    public TreeViewSetup (UndoableANodeTreeViewEditor treeViewEditor) {
        this.treeViewEditor1 = treeViewEditor;
    }


    /**
     * @param treeView will be populated with treeItems.
     * @param modelRoot ANode root. will be root of tree.
     */
    public void setupTree(TreeView<ANode> treeView, ANode modelRoot, boolean allowEditing) {
        setCellFactory(treeView, treeViewEditor1, modelRoot, allowEditing);
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
     * @param treeView The TreeView.
     * @param _treeViewEditor TreeViewEditor that will work on this tree.
     */
    public void setCellFactory(TreeView<ANode> treeView, UndoableANodeTreeViewEditor _treeViewEditor, ANode modelRoot, boolean allowEditing) {
        treeView.setEditable(true);
        _treeViewEditor.addTreeView(treeView);
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
                private final UndoableANodeTreeViewEditor treeViewEditor1 = _treeViewEditor;

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
//                        MenuItem restore = new MenuItem("Restore");
                        MenuItem extract = new MenuItem("Extract children");
                        MenuItem create = new MenuItem("Create");
                        MenuItem fileIDInfo = new MenuItem("FileID Info");
                        MenuItem print = new MenuItem("Print Subtree");
                        MenuItem printF = new MenuItem("Print FileIDs of Subtree");
                        MenuItem splitLeftRight = new MenuItem("Split Subtree in Left/Right");
                        MenuItem moveLRBehind1LastWord = new MenuItem("Move L/R behind n'th last word");
                        MenuItem restrucTree = new MenuItem("Restructure Subtree");

                        cut.setOnAction(e -> cut(this.getTreeItem()));
                        paste.setOnAction(e -> paste(this.getTreeItem(), this.getTreeView().getRoot()));
                        paste.disableProperty().bind(treeViewEditor1.canPaste().not());
                        delete.setOnAction(e -> delete(this.getTreeItem()));
//                        restore.setOnAction(e -> restore());
                        rename.setOnAction(e -> getTreeView().edit(this.getTreeItem()));
                        create.setOnAction(e -> this.create());
                        extract.setOnAction(e -> extract(this.getTreeItem()));
                        fileIDInfo.setOnAction(e -> {
                            try {
                                showFileIDInfo(this.getTreeItem());
//                                runOnANodeFileIDsModified();
                            }
                            catch (IOException ioe) {
                                System.out.println("Failed to load FileIDInfo");
                                ioe.printStackTrace();
                            }
                        });
                        print.setOnAction(e -> LittlePopUp.showScrollableTextPopup("Subtree of " + this.getTreeItem().getValue().name() + "(" + this.getTreeItem().getValue().conceptId() + ")", TreeExport.printSubtreeBelow(this.getTreeItem(), true), false));
                        printF.setOnAction(e -> LittlePopUp.showScrollableTextPopup("FileIDs of " + this.getTreeItem().getValue().name() + "(" + this.getTreeItem().getValue().conceptId() + ")", collectFileIDsBelow(this.getTreeItem().getValue()).keySet().toString().replace("[", "").replace("]", "").replace(", ", "\n"), false));

//                        splitLeftRight.setOnAction(e -> {UndoableANodeTreeViewEditor.splitSubtreeLeftRight(this.getTreeItem(), modelRoot);});

//                        moveLRBehind1LastWord.setOnAction(e -> {
//                            Integer n = LittlePopUp.askForGreater0Int();
//                            if (n == null) return;
//                            UndoableANodeTreeViewEditor.applyLRNameChangeRec(this.getTreeItem(),n);
//                            getTreeView().refresh();
//                        });

                        restrucTree.setOnAction(e -> {
                            treeViewEditor1.restructureTree(this.getTreeItem(), this.getTreeView().getId());
                        });
                        if (allowEditing) {
                            if (getTreeItem().getParent() != null) setContextMenu(new ContextMenu(cut, paste, delete, rename, create, extract, restrucTree, fileIDInfo, print, printF));
                            else setContextMenu(new ContextMenu(paste, rename, create, restrucTree, fileIDInfo, print, printF));
                        } else {
                            setContextMenu(new ContextMenu(fileIDInfo, print, printF));
                        }
                    }
                }


                private void create() {
                    String id = generateUnusedID(modelRoot);
                    ANode newnode = new ANode(
                            id, id, new HashSet<>(), new HashSet<>()
                    );
                    TreeItem<ANode> newTreeItem = this.treeViewEditor1.create(this.getTreeItem(), newnode, treeView.getId());
//                    runOnANodeFileIDsModified();
//                    this.getTreeItem().getValue().children().add(newnode);  //also create new anode relationship
                    getTreeView().edit(newTreeItem);
                }

                private void extract(TreeItem<ANode> parent) {
                    if (parent.getParent() == null) return;
                    treeViewEditor1.extractChildren(parent, treeView.getId());
                }

                private void cut(TreeItem<ANode> treeItem) {
                    this.treeViewEditor1.cut(treeItem, treeView.getId());
                }

                /**
                 * Paste under the given treeItem.
                 * @param treeItem
                 * @param root It will be checked if any of the conceptIDs of the ANodes being pasted already occur in the tree below
                 *             root. If so, the user will be informed via a PopUp window and asked if the pasted nodes in question
                 *             should receive new randomized conceptIDs or else the pasting will not be allowed.
                 *             Usually this means that this parameter is the master root of the tree that contains treeItem.
                 */
                private void paste(TreeItem<ANode> treeItem, TreeItem<ANode> root) {
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

//                        String popupText = """
//                            Pasting will result in the following nodes being duplicated.
//                            Assign new IDs to duplicated nodes and continue?
//
//                            Item to paste | already present item with same ID
//
//                            """ + stringBuilder.toString();
                        String popupText = """
                                Cannot paste in this tree. The following node IDs would be duplicated:
                                
                                Item to paste | already present item with same ID
                                
                                """ + stringBuilder.toString();
                        LittlePopUp.showScrollableTextPopup("Error", popupText, false);    // i dont want to implement this as undoable..
                        return;
//                        if (!LittlePopUp.showScrollableTextPopup("Warning", popupText)) return;
//                        for (TreeItem<ANode> itemWDuplicateANode : duplicateItemsMap.keySet()) replaceANodeByCopy(itemWDuplicateANode, root);
                    }

                    //now the regular pasting.
                    this.treeViewEditor1.paste(treeItem, treeView.getId()); //this node IS the targetParent

                    //getLast() weil treeItem hier das parent ist und wir uns für die fileIDs des gerade hier gepasteten childs interessieren, was das letzte in der liste children ist.
//        Collection<String> fileIDsToAdd = collectFileIDsBelow(treeItem.getChildren().getLast().getValue()).keySet();  it is no longer desired for parents to hold all fileids of their subtree
//        for (String fileID : fileIDsToAdd) System.out.println(fileID);
//        addFileIDsToParentsRec(treeItem, fileIDsToAdd);   //add the fileIDs to all parents

//        for (TreeItem<ANode> childTreeItem : treeItem.getChildren()) {
//            ANode childNode = childTreeItem.getValue(); //make sure the anode relationships are also created!
//            if (!treeItem.getValue().children().contains(childNode)) treeItem.getValue().children().add(childNode);
//        }
//                    runOnANodeFileIDsModified();

                }

                private void delete(TreeItem<ANode> treeItem) {
                    if (treeItem.getParent() == null) return;
//        removeFileIDsFromParentsRec(treeItem.getParent(), treeItem.getValue().getFileIds(), treeItem);
//        removeFileIDsFromParentsRec(treeItem.getParent(), collectFileIDsBelow(treeItem.getValue()).keySet());

//        treeItem.getParent().getValue().children().remove(treeItem.getValue()); //also delete anode relationship
                    this.treeViewEditor1.delete(treeItem, treeView.getId());
//                    runOnANodeFileIDsModified();
                }

                /**
                 * Shows popup window with FileID info
                 * @throws IOException
                 */
                public void showFileIDInfo(TreeItem<ANode> treeItem) throws IOException {

                    final String fIDofThis = "lightgreen";
                    final String fIDofLeaf = "lightyellow";
                    final String fIDofInt = "darkseagreen";

                    FileIDView fileIDView = new FileIDView();

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
                            setTextFill(Color.BLACK);   // otherwise selected -> white, which is hard to read
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

                    ANode thisanode = treeItem.getValue();    //so i dont have to type getvalue all the time
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
                            if (!occursInLeaf) colorCss = fIDofInt;
                            else colorCss = fIDofLeaf;
                        } else {
                            colorCss = fIDofThis;
                        }
                        rows.add(new FileIDRow(fileID, fileIDtoANode.get(fileID).toString(), colorCss));
                    }

                    fileIDView.getController().getFileIDTableView().setItems(rows);

                    fileIDView.getController().getFileIDTableView().getColumns().addAll(col1, col2);

                    fileIDView.getController().getBotlabel().setText(String.valueOf(fileIDtoANode.keySet().size()) + " FileIDs");

                    fileIDView.getController().getLegend().setSpacing(LegendItem.DEFAULT_SPACING);
                    fileIDView.getController().getLegend().getChildren().addAll(
                            new LegendItem(fIDofThis, "This Node"),
                            new LegendItem(fIDofInt, "Internal Node"),
                            new LegendItem(fIDofLeaf, "Leaf Node")
                    );
                    fileIDView.getController().getLegend().setAlignment(LegendItem.DEFAULT_ALIGNMENT_POS);
                    fileIDView.getController().getLegend().setPadding(LegendItem.DEFAULT_PADDING);

                    fileIDView.getController().getNameTextfield().setText(treeItem.getValue().name() + " (id=" + treeItem.getValue().conceptId() + ")");

                    LittlePopUp.showPopup(fileIDView.getRoot(), "FileID Info for subtree of " + fileIDView.getController().getNameTextfield().getText(), 1000, 500);

                    //--------window is now visible---------------------


                    fileIDView.getController().getAddFileIDButton().setOnAction(e -> {
                        String fileID = fileIDView.getController().getAddFileIDTextField().getCharacters().toString();
                        if (fileID.isBlank() || thisanode.fileIds().contains(fileID)) return;
                        treeViewEditor1.addFileID(treeItem, fileID, treeView.getId());
                        rows.removeIf(row -> row.col1.equals(fileID));
                        rows.add(new FileIDRow(fileID, thisanode.name(), "lightblue"));
                    });

                    fileIDView.getController().getRemoveFileIDButton().setOnAction(e -> {
                        String fileID = fileIDView.getController().getAddFileIDTextField().getCharacters().toString();
                        if (fileID.isBlank() || !thisanode.fileIds().contains(fileID)) return;
                        treeViewEditor1.removeFileID(treeItem, fileID, treeView.getId());

                        rows.removeIf(row -> row.col1.equals(fileID));
                        rows.add(new FileIDRow(fileID, thisanode.name(), "gray"));
                    });


                    fileIDView.getController().getSearchButton().setOnAction(e -> {
                        for (FileIDRow fileIDRow : rows) {
                            if (col1.getCellData(fileIDRow).toUpperCase().contains(fileIDView.getController().getAddFileIDTextField().getText().toUpperCase())) {
                                fileIDView.getController().getFileIDTableView().getSelectionModel().clearSelection();
                                fileIDView.getController().getFileIDTableView().getSelectionModel().select(fileIDRow);
                                fileIDView.getController().getFileIDTableView().scrollTo(fileIDRow);
                            }
                        }
                    });

                }

                //implements rename
                //gets temporaryANode from StringConverter from which the entered name is extracted
                @Override
                public void commitEdit(ANode temporaryANode) {
                    if (temporaryANode == null || temporaryANode.name().isBlank() || this.getItem().name().equals(temporaryANode.name())) {
                        cancelEdit();
                        return;
                    }
                    treeViewEditor1.rename(this.getTreeItem(), temporaryANode.getName(), treeView.getId());
//                    this.getItem().setName(temporaryANode.getName());
                    super.commitEdit(this.getItem());
//                    setText(this.getItem().toString());
//                    runOnANodeFileIDsModified();
                }
            };
        });

    }


//    public void restore() {
//        if (!treeViewEditor1.canRestore().get()) return;
//        TreeItem<ANode> restored = treeViewEditor1.restore();
////        addFileIDsToParentsRec(restored.getParent(), new LinkedList<>());   //add the fileIDs to all parents
//        restored.getParent().getValue().children().add(restored.getValue());
//        runOnANodeFileIDsModified();
//    }


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

    public record FileIDRow(String col1, String col2, String colorCss) {}
}