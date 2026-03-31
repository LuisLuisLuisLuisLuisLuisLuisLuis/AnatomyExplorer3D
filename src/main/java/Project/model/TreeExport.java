package Project.model;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import Project.window.SupportingUI.FileExplorerInteraction;

import java.io.File;
import java.util.List;

public class TreeExport {

    /**
     * From the TreeView, generate the following files:
     * <ul>
     *     <li> relation_list.txt (originally inclusion_relation_list.txt) </li>
     *     <li> file_list.txt (originally element_parts.txt) </li>
     * </ul>
     * @param treeView
     * @return True if it worked, else false.
     */
    public static boolean writeToFiles(TreeView<ANode> treeView, String saveDirectory) {
        File relationsFile = FileExplorerInteraction.chooseSaveDestination("Save relations file", new FileChooser.ExtensionFilter("Text", "*.*"));
        File fileListFile = FileExplorerInteraction.chooseSaveDestination("Save files list file", new FileChooser.ExtensionFilter("Text", "*.*"));
        if (relationsFile == null || fileListFile == null) return false;
        if (FileExplorerInteraction.writeToFile(generateRelationList(treeView),relationsFile) && FileExplorerInteraction.writeToFile(generateFileList(treeView), fileListFile)) return true;
        else return false;
    }

    public static void saveTrees(List<TreeView<ANode>> treeViews, List<String> treeNames) {
        if (treeViews.size() != treeNames.size()) return;
        File directory = FileExplorerInteraction.findDir("Choose saving directory");
        if (directory == null) return;
        for (int i = 0; i < treeViews.size(); i++) {
            TreeView<ANode> treeView = treeViews.get(i);
            String name = treeNames.get(i);
            File relationsFile = new File(directory, name + "_inclusion_relation_list.txt");
            File fileListFile = new File(directory, name + "_element_parts.txt");
            if (relationsFile.exists() || fileListFile.exists()) {
                System.err.println("File already exists");
                return;
            }
            FileExplorerInteraction.writeToFile(generateRelationList(treeView), relationsFile);
            FileExplorerInteraction.writeToFile(generateFileList(treeView),fileListFile);
        }
    }


    public static String generateRelationList(TreeView<ANode> treeView) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("parent id\tparent name\tchild id\tchild name\n");
        //edge case where the only TreeItem of the treeview is the root
        if (treeView.getRoot().getChildren().isEmpty()) stringBuilder.append(treeView.getRoot().getValue().conceptId()+ "\t" + treeView.getRoot().getValue().name() + "\t\t");
        else collectRelationsAsString(treeView.getRoot(), stringBuilder);
        return stringBuilder.toString();
    }

    private static void collectRelationsAsString(TreeItem<ANode> root, StringBuilder stringBuilder) {
        for (TreeItem<ANode> child : root.getChildren()) {
            stringBuilder.append(root.getValue().conceptId()+"\t"+root.getValue().name()+"\t"+child.getValue().conceptId()+"\t"+child.getValue().getName() + "\n");
            collectRelationsAsString(child, stringBuilder);
        }
    }

    public static String generateFileList(TreeView<ANode> treeView) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("concept id\tname\telement file id\n");
        generateFileListRec(treeView.getRoot(), stringBuilder);
        return stringBuilder.toString();
    }

    private static void generateFileListRec(TreeItem<ANode> root, StringBuilder stringBuilder) {
        for (String fileID : root.getValue().fileIds()) stringBuilder.append(root.getValue().conceptId() + "\t" + root.getValue().name() + "\t" + fileID + "\n");
        for (TreeItem<ANode> child : root.getChildren()) generateFileListRec(child, stringBuilder);
    }

    public static String printSubtreeBelow(TreeItem<ANode> root, boolean inclID) {
        StringBuilder stringBuilder = new StringBuilder();
        if (inclID) {
//            stringBuilder.append("parent ID\tparent name\tchild ID\tchild name\n");
            printSubtreeBelowRecIncID(root, stringBuilder);
        } else {
//            stringBuilder.append("parent\tchild\n");
            printSubtreeBelowRec(root, stringBuilder);
        }
        return stringBuilder.toString();
    }

    private static void printSubtreeBelowRec(TreeItem<ANode> root, StringBuilder stringBuilder) {
        for (TreeItem<ANode> child : root.getChildren()) stringBuilder.append(root.getValue().name() + "\t" + child.getValue().name() + "\n");
        for (TreeItem<ANode> child : root.getChildren()) printSubtreeBelowRec(child, stringBuilder);
    }
    private static void printSubtreeBelowRecIncID(TreeItem<ANode> root, StringBuilder stringBuilder) {
        for (TreeItem<ANode> child : root.getChildren()) stringBuilder.append(root.getValue().conceptId() + "\t" + root.getValue().name() + "\t" + child.getValue().conceptId() + "\t" + child.getValue().name() + "\n");
        for (TreeItem<ANode> child : root.getChildren()) printSubtreeBelowRecIncID(child, stringBuilder);
    }


}
