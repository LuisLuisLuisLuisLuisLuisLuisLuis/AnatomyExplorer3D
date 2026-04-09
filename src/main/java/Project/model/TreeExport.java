package Project.model;

import Project.window.PopUp.LittlePopUp;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import Project.window.SupportingUI.FileExplorerInteraction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
        try {
            FileExplorerInteraction.writeToFile(generateRelationList(treeView), relationsFile);
            FileExplorerInteraction.writeToFile(generateFileList(treeView), fileListFile);
            return true;
        } catch (IOException ignored) {return false;}
    }

    /**
     * Tries to save the given treeviews under the given names.
     * <ul>
     *     <li>Asks for a directory</li>
     *     <li>Notifies the user of the files that will be created</li>
     *     <li>Asks if existing files should be overwritten</li>
     * </ul>
     * The user can abort at any of these steps. Then, the files be attempted to be created. The user will be notified after
     * the process if any errors occurred.
     * @param treeViews
     * @param treeNames
     * @return List of boolean matching indices of TreeViews where true = TreeView was saved and false = TreeView was not saved.
     * Returns null if the process was aborted.
     */
    public static List<Boolean> saveTrees(List<TreeView<ANode>> treeViews, List<String> treeNames) {
        List<Boolean> result = new ArrayList<>();
        for (TreeView<ANode> ignored : treeViews) result.add(Boolean.FALSE);
        if (treeViews.size() != treeNames.size()) return null;
        File directory;
        File[] relationsFiles = new File[treeViews.size()];
        File[] fileListFiles = new File[treeViews.size()];

        while (true) {
            directory = FileExplorerInteraction.findDir("Choose saving directory");
            if (directory == null) break;
            StringBuilder names = new StringBuilder();
            for (int i = 0; i < treeViews.size(); i++) {
                String name = treeNames.get(i);
                relationsFiles[i] = new File(directory, name + "_edge_list.txt");
                fileListFiles[i] = new File(directory, name + "_file_list.txt");
                names.append(relationsFiles[i].getName()).append("\n").append(fileListFiles[i].getName()).append("\n");
            }
            if (LittlePopUp.showScrollableTextPopup("Info", "The following files will be created:", names.toString(), true)) break;
        }
        if (directory == null) return null;

        StringBuilder errMsgBuilder = new StringBuilder();
        for (int i = 0; i < relationsFiles.length; i++) {
            String name = treeNames.get(i);
            if (relationsFiles[i].exists()) errMsgBuilder.append(name + "_edge_list.txt\n");
            if (fileListFiles[i].exists()) errMsgBuilder.append(name + "_file_list.txt\n");
        }
        String errMsg = errMsgBuilder.toString();

        if (!errMsg.isBlank()) {
            if (!LittlePopUp.showScrollableTextPopup("Warning", "The following files already exist. Overwrite?", errMsg, true, "Overwrite")) return saveTrees(treeViews, treeNames);
        }
        errMsgBuilder = new StringBuilder();
        for (int i = 0; i < treeViews.size(); i++) {
            try {
                FileExplorerInteraction.writeToFile(generateRelationList(treeViews.get(i)), relationsFiles[i]);
                try {
                    FileExplorerInteraction.writeToFile(generateFileList(treeViews.get(i)), fileListFiles[i]);
                    result.set(i, true);
                    continue;
                } catch (IOException ioException) {
                    errMsgBuilder.append(treeNames.get(i) + ": " + ioException.getMessage());
                }
            } catch (IOException ioException) {
                errMsgBuilder.append(treeNames.get(i) + ": " + ioException.getMessage());
            }
            result.set(i, false);
        }
        errMsg = errMsgBuilder.toString();
        if (!errMsg.isBlank()) LittlePopUp.showScrollableTextPopup("Error", "Encountered errors saving the following models:", errMsg, false);
        return result;
    }

    /**
     * Tries to save the given treeview under the given name.
     * <ul>
     *     <li>Asks for a directory</li>
     *     <li>Notifies the user of the files that will be created</li>
     *     <li>Asks if existing files should be overwritten</li>
     * </ul>
     * The user can abort at any of these steps. Then, the files be attempted to be created. The user will be notified after
     * the process if any errors occurred.
     * @param treeView to be saved
     * @param name name to preprend to the files
     * @return true if saving was successful, false if cancelled or an error occurred.
     */
    public static boolean saveTree(TreeView<ANode> treeView, String name) {
        File directory;
        File relationsFile;
        File fileListFile;
        while (true) {
            directory = FileExplorerInteraction.findDir("Choose saving directory");
            if (directory == null) return false;
            relationsFile = new File(directory, name + "_edge_list.txt");
            fileListFile = new File(directory, name + "_file_list.txt");
            if (!LittlePopUp.showScrollableTextPopup("Info", "The following files will be created:", relationsFile.getName() + "\n" + fileListFile.getName(), true)) {
                return false;
            }
            String errmsg = "";
            if (relationsFile.exists()) errmsg += name + "_edge_list.txt\n";
            if (fileListFile.exists()) errmsg += name + "_file_list.txt\n";
            if (!errmsg.isBlank()) {
                if (LittlePopUp.showScrollableTextPopup("Warning", "The following files already exist. Overwrite?", errmsg, true, "Overwrite")) break;
            } else break;
        }
        try {
            FileExplorerInteraction.writeToFile(generateRelationList(treeView), relationsFile);
            FileExplorerInteraction.writeToFile(generateFileList(treeView),fileListFile);
            return true;
        } catch (IOException ioException) {
            LittlePopUp.showMsg("Error", "Encountered an error while saving:\n" + ioException.getMessage(), "OK");
            return false;
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
