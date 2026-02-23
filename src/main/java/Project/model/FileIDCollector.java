package Project.model;

import java.util.*;

public class FileIDCollector {


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
