package Project.SelectionModel.Mediator;

import Project.SelectionModel.SelectionGroup;
import Project.model.ANode;

import java.util.*;

public class SelectionMediator_Tree_3D extends SelectionMediator<ANode, String> {

    /**
     * Collects all FileIDs from the set of ANodes.
     * @param selection
     * @return
     */
    @Override
    public Set<String> transformAselectionToBSelection(Set<ANode> selection) {
        Set<String> transformedSelection = new HashSet<>();
        for (ANode item : selection) {
            //if (item == null) System.out.println("SelectionGroupA.getSelection has a null item; in transformASelectionToBSelection in Mediator");
            Collection<String> fileIDs = item.getFileIds();
            transformedSelection.addAll(fileIDs);
        }
        System.out.println("mediator_tree_3d A->B " + transformedSelection);
        return transformedSelection;
    }

    @Override
    public Set<ANode> transformBSelectionToASelection(Set<String> selection) {
        Set<ANode> transformedSelection = new HashSet<>();
        for (String item : selection) {
            //transformedSelection.add(fileIDtoName.getOrDefault(item, new LinkedList<>()));
            for (ANode anode : fileIDtoANode.getOrDefault(item, new LinkedList<>())) transformedSelection.add(anode);
        }
        System.out.println("mediator_tree_3d B->A " + transformedSelection);
        return transformedSelection;
    }

    /**
     * Map fileIDs to ANode.name and vice versa.
     */
    private TreeMap<String, Collection<ANode>> fileIDtoANode = new TreeMap<>();

    private final Collection<ANode> roots;
    public void addRoot(ANode root) {
        if (this.roots.contains(root)) return; //don't add if already present
        this.roots.add(root);
        reloadDicts();
    }
    public void removeRoot(ANode root) {
        this.roots.remove(root);
        reloadDicts();
    }

    public Collection<ANode> fileIDtoANode(String fileID) {
        return fileIDtoANode.get(fileID);
    }

    public SelectionMediator_Tree_3D (SelectionGroup<ANode> selectionGroupTree, SelectionGroup<String> selectionGroup3D, Collection<ANode> roots) {
        super(selectionGroupTree, selectionGroup3D);
        this.roots = roots;
        reloadDicts();
    }
    public SelectionMediator_Tree_3D (SelectionGroup<ANode> selectionGroupTree, SelectionGroup<String> selectionGroup3D) {
        super(selectionGroupTree, selectionGroup3D);
        this.roots = new ArrayList<>();
    }
    public SelectionMediator_Tree_3D (Collection<ANode> roots, String id) {
        super(null, null);
        this.roots = roots;
        reloadDicts();
        this.id = id;
    }


    //may be necessary multiple times if the FileID associations change.
    //like when the tree is modified.
    public void reloadDicts() {
        this.fileIDtoANode.clear();
        for (ANode root : roots) initializeDicts(root);
    }

    /**
     * Add all mappings of fileIDs to the ANodes that hold them to fileIDtoANode.
     */
    private void initializeDicts(ANode root) {

        for (String fileID : root.getFileIds()) {
                Collection<ANode> list = fileIDtoANode.getOrDefault(fileID, new LinkedList<>());
                list.add(root);
                fileIDtoANode.put(fileID, list);
            }

        for (ANode child : root.children()) {
                initializeDicts(child);
            }
    }

    private String id = "";
    public String getId() {return id;}
}
