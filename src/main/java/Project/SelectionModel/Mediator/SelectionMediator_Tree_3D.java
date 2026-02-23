package Project.SelectionModel.Mediator;

import Project.SelectionModel.SelectionGroup;
import Project.model.ANode;

import java.util.*;

public class SelectionMediator_Tree_3D extends SelectionMediator<ANode, String> {

    @Override
    public Set<String> transformAselectionToBSelection(Set<ANode> selection) {
        Set<String> transformedSelection = new HashSet<>();
        for (ANode item : selection) {
            //if (item == null) System.out.println("SelectionGroupA.getSelection has a null item; in transformASelectionToBSelection in Mediator");
            Collection<String> fileIDs = item.getFileIds();
            transformedSelection.addAll(fileIDs);
            for (String fileID : fileIDs) System.out.println(item + ", " + fileID);
        }
        return transformedSelection;
    }

    @Override
    public Set<ANode> transformBSelectionToASelection(Set<String> selection) {
        Set<ANode> transformedSelection = new HashSet<>();
        for (String item : selection) {
            //transformedSelection.add(fileIDtoName.getOrDefault(item, new LinkedList<>()));
            for (ANode anode : fileIDtoANode.getOrDefault(item, new LinkedList<>())) transformedSelection.add(anode);
        }
        return transformedSelection;
    }

    /**
     * Map fileIDs to ANode.name and vice versa.
     */
    private TreeMap<String, Collection<ANode>> fileIDtoANode = new TreeMap<>();
    private TreeMap<ANode, Collection<String>> anodeToFileID = new TreeMap<>();

    private final ANode partofRoot;
    private final ANode isaRoot;

    public Collection<ANode> fileIDtoANode(String fileID) {
        return fileIDtoANode.get(fileID);
    }
    public Collection<String> nameToFileID(ANode aNode) {
        return anodeToFileID.get(aNode);
    }

    public SelectionMediator_Tree_3D (SelectionGroup<ANode> selectionGroupTree, SelectionGroup<String> selectionGroup3D, ANode isARoot, ANode partOfRoot) {
        super(selectionGroupTree, selectionGroup3D);
        this.partofRoot = partOfRoot;
        this.isaRoot = isARoot;
        reloadDicts();
    }

    /**
     * Allows post-creation extension of the Treemaps.
     */
    public void addNameToFileIDmapping(ANode anode, Set<String> fileIDs) {
        for (String fileID : fileIDs) {
            Collection<ANode> list = fileIDtoANode.getOrDefault(fileID, new LinkedList<>());
            list.add(anode);
            fileIDtoANode.put(fileID, list);
        }
        anodeToFileID.put(anode, fileIDs);
    }

    //may be necessary multiple times if the FileID associations change.
    //like when the tree is modified.
    public void reloadDicts() {
        this.fileIDtoANode.clear();
        initializeDicts(isaRoot);
        initializeDicts(partofRoot);
    }

    /**
     * Initialize the Maps with ANode roots from the trees.
     * @param node
     */
    private void initializeDicts(ANode node) {
        /*
        INFO:
         - what to select / (draw) when internal nodes are selected?
         - all their children basically? or nothing
         -> comes down to: do i want to select all children if i select an internal node?
            - because internal nodes dont have (only one) corresponding body part.
            -> this logic is handled by the tree . i only add leaf to fileID mapping here.

          UPDATE: for TreeEditor, I also map internal nodes to their FileIDs, as it turns out
          that there are few FileIDs that are only mapped to internal nodes
         */
        Collection<ANode> children = node.children();
//        if (children.isEmpty()) {
//            for (String fileID : node.fileIds()) {
//                Collection<String> list = fileIDtoName.getOrDefault(fileID, new LinkedList<>());
//                list.add(node.name());
//                fileIDtoName.put(fileID, list);
//            }
//            nameToFileID.put(node.name(), node.fileIds());
//            } else {
//            for (ANode child : children) {
//                initializeDicts(child);
//            }
//        }
        for (String fileID : node.getFileIds()) {
                Collection<ANode> list = fileIDtoANode.getOrDefault(fileID, new LinkedList<>());
                list.add(node);
                fileIDtoANode.put(fileID, list);
            }
//        anodeToFileID.put(node, node.fileIds());
        for (ANode child : children) {
                initializeDicts(child);
            }
    }
}
