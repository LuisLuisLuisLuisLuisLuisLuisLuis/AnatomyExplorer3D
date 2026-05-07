package Project.model;

import Project.window.PopUp.LittlePopUp;

import java.io.*;
import java.util.*;
import java.util.function.Function;

public class FileUtil {

    /**
     * Turns a file into an inputstream.
     * @param file
     * @return
     */
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
