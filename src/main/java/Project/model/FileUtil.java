package Project.model;

import Project.window.SupportingUI.PopUp.LittlePopUp;
import Project.window.TreeView.TreeAnalysis.TreeAnalysisUtils;
import javafx.scene.control.TreeItem;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class FileUtil {

    /**
     * Turns a file into an inputstream.
     * @param file
     * @return
     */
    //TODO: when is this used and are those usages safe?
    public static InputStream makeStream(File file) {
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

    /**
     * Can be used to compute an estimate of the sum of file sizes associated with a tree.
     * @param root root of a tree
     * @param aveSize average file size
     * @return Accumulates all FileIDs in the tree in a set. Then returns set.size() * aveSize.
     * Result is of SAME UNIT as that of avesize. So pay attention to what you provide / receive.
     */
    public static long estimateSizeOfTreeFiles(TreeItem<ANode> root, long aveSize) {
        Set<String> fileIDs = new HashSet<>();
        TreeAnalysisUtils.accumulateForEveryNodeBelow(root, t -> {fileIDs.addAll(t.getValue().fileIds()); return null;});
        return fileIDs.size() * aveSize;
    }

    /**
     * Estimates the sum of sizes of all OBJ files in the directory.
     * @param dir A directory.
     * @param whiteList WhiteList of file names without file extension (like '.obj')
     * @return Size in MB. 0 if dir does not specify a directory or an exception is thrown.
     */
    public static long estimateSizeOfOBJsInDir(File dir, Set<String> whiteList, Set<String> supportedFileExtensions) {
        System.out.println(Arrays.toString(whiteList.toArray()));
        try {System.out.println("directory size: " + Files.size(dir.toPath()));}
        catch (IOException i) {return 0;}

        if (dir.isDirectory() && dir.listFiles() != null) {
            File[] files = dir.listFiles();
            if (files == null) return 0;
            long sumFileSizes = 0;
            for (File file : files) {
                try {
                    if (!file.isFile()) continue;
                    if (!file.getName().contains(".")) continue; //so that the next statement can safely run
                    String extension = file.getName().substring(file.getName().lastIndexOf("."));
                    if (!supportedFileExtensions.contains(extension)) continue;
                    if (!whiteList.contains(file.getName().substring(0, file.getName().lastIndexOf(".")))) continue;
                    sumFileSizes += Files.size(file.toPath());
                }
                catch (IOException ignored) {System.out.println("skipping file");}
            }
            sumFileSizes /= (1024 * 1024);
            System.out.println("file sizes: " + sumFileSizes );
            return sumFileSizes;
        }
        return 0;
    }

    /**
     * Given an InputStream for a File list of format (id\tname\tfileID) return all the fileIDs.
     * @param fileListStream InputStream for a file list of the specified format.
     * @return The set of fileIDs. Returns an empty set if an exception is thrown.
     */
    public static Set<String> extractFileIDsFromFileList(InputStream fileListStream) {
        try {
            Set<String> files = new HashSet<>();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileListStream));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] fields = line.split("\t");
                if (fields.length != 3) continue;
                files.add(fields[2].trim());
            }
            return files;
        } catch (Exception x) {
            return new HashSet<>();
        }

    }

    public static void collectFileIDsBelowToSet(ANode root, Set<String> set) {
        set.addAll(root.fileIds());
        for (ANode child : root.getChildren()) collectFileIDsBelowToSet(child, set);
    }

    public static Set<String> collectFileIDsBelowToSet(ANode root) {
        Set<String> result = new HashSet<>();
        collectFileIDsBelowToSet(root, result);
        return result;
    }

    /**
     * Collects the file IDs in the tree of root.
     * To print as column of fileIDs, do collectFileIDsBelow(this.getTreeItem().getValue()).keySet().toString().replace("[", "").replace("]", "").replace(", ", "\n")
     * @param root
     * @return
     */
    public static HashMap<String, LinkedList<ANode>> collectFileIDsBelow(ANode root) {
        HashMap<String, LinkedList<ANode>> fileIDtoANode = new HashMap<>();
        collectFileIDsBelowRec(root, fileIDtoANode);
        return fileIDtoANode;
    }

    public static void collectFileIDsBelowRec(ANode root, Map<String, LinkedList<ANode>> fileIDtoANode) {
        for (String fileID : root.fileIds()) {
            LinkedList<ANode> nodes = fileIDtoANode.getOrDefault(fileID.trim(), new LinkedList<>());
            nodes.add(root);
            fileIDtoANode.put(fileID.trim(), nodes);
        }
        for (ANode child : root.children()) collectFileIDsBelowRec(child, fileIDtoANode);
    }
}
