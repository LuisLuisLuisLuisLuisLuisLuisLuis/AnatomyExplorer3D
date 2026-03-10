package Project.window.TreeView.TreeAnalysis;

import Project.model.ANode;
import Project.window.PopUp.LittlePopUp;
import Project.window.TreeView.TreeViewSetup;
import javafx.scene.control.TreeItem;

import java.util.*;
import java.util.function.Function;

public class TreeAnalysisUtils {

    /**
     * Modify the tree so that no two ANodes have the same conceptID.
     * @param root
     */
    public static void findAndChangeIdenticalIDsInTreeView(TreeItem<ANode> root) {
        LinkedList<TreeItem<ANode>> allTreeItems = new LinkedList<>();
        accumulateForEveryNodeBelow(root, allTreeItems, t -> t);
        for (TreeItem<ANode> treeItem : allTreeItems) findAndChangeIdenticalIDsOfNode(root, treeItem, root);
    }
    /**
     * Exchange all Anodes in the tree that have the same conceptID as ogTreeItem by a copy of themselves with a different conceptID.
     */
    public static void findAndChangeIdenticalIDsOfNode(TreeItem<ANode> root, TreeItem<ANode> ogTreeItem, TreeItem<ANode> masterRoot) {
        if (root != ogTreeItem && root.getValue().conceptId().equals(ogTreeItem.getValue().conceptId())) replaceANodeByCopy(root, masterRoot);
        for (TreeItem<ANode> child : root.getChildren()) findAndChangeIdenticalIDsOfNode(child, ogTreeItem, masterRoot);
    }


    /**
     * Replace ANode by a copy that has a different conceptID that does not occur in the tree of root and modify the ANode relationships accordingly.
     */
    public static void replaceANodeByCopy(TreeItem<ANode> patient, TreeItem<ANode> root) {
        ANode newANode = copyANodeWNewID(patient.getValue(), root.getValue());
        ANode parentANode = patient.getParent().getValue();
        if (parentANode != null ) parentANode.children().remove(patient.getValue());
        patient.setValue(newANode);
        if (parentANode != null) parentANode.children().add(patient.getValue());
    }

    /**
     * Check if any ANode in the tree of the given root has the given conceptID.
     */
    public static boolean doesIDExistBelow(ANode root, String conceptID) {
        if (root.conceptId().equals(conceptID)) return true;
        else {
            for (ANode child : root.children()) {
                if (doesIDExistBelow(child, conceptID)) return true;
            }
            return false;
        }
    }


    /**
     * Add result of parsing root with function to result. Then recurse on children.
     */
    public static <T, O> void accumulateForEveryNodeBelow(TreeItem<T> root, Collection<O> result, Function<TreeItem<T>, O> function) {
        result.add(function.apply(root));
        for (TreeItem<T> child : root.getChildren()) accumulateForEveryNodeBelow(child, result, function);
    }
    /**
     * Add result of parsing root with function to result. Then recurse on children.
     * @param avoid Avoid this treeItem.
     */
    public static <T, O> void accumulateForEveryNodeBelow(TreeItem<T> root, Collection<O> result, Function<TreeItem<T>, O> function, TreeItem<T> avoid) {
        if (root != avoid) {
            result.add(function.apply(root));
            for (TreeItem<T> child : root.getChildren()) accumulateForEveryNodeBelow(child, result, function, avoid);
        }

    }
    public static <T, O> LinkedList<O> accumulateForEveryNodeBelow(TreeItem<T> root, Function<TreeItem<T>, O> function) {
        LinkedList<O> result = new LinkedList<>();
        accumulateForEveryNodeBelow(root, result, function);
        return result;
    }


    /**
     * Applies fun to every treeItem recursively without returning anything.
     * @param root Root of Tree
     * @param fun Function
     * @param <T> Type of TreeItem
     * @param <O> Return type of function (return value is unused)
     */
    public static <T,O> void applyRec(TreeItem<T> root, Function<TreeItem<T>,O> fun) {
        fun.apply(root);
        for (TreeItem<T> child : root.getChildren()) applyRec(child, fun);
    }

    /**
     * Find and return the ANode with the specified name in the ANode tree with the given root or null if not present.
     */
    public static ANode getANodeWithName(String name, ANode root) {
        if (root.name().equals(name)) return root;
        else {
            if (root.children().isEmpty()) return null;
            else {
                ANode result = null;
                for (ANode child : root.children()) {
                    result = getANodeWithName(name,child);
                    if (result != null) break;
                }
                return result;
            }
        }
    }

    public static ANode copyANodeWNewID(ANode aNode, ANode modelRoot) {
        return new ANode(TreeViewSetup.generateUnusedID(modelRoot), aNode.name(), new HashSet<>(aNode.getChildren()), new HashSet<>(aNode.getFileIds()));
    }

    /**
     * @param anodeToLookFor The ANode to look for. Does not check by reference, but checks if conceptIDs are equal.
     * @param checkSameANodeInstance Report TreeItems that hold the anodeToLookFor instance.
     *                               This means that if set to true, if it is known that anodeToLookFor appears at least once in the tree, the result will be of size >= 1.
     *                               If set to false, then Nodes that have the same conceptID because they hold the anodeToLookFor instance will be ignored.
     * @param exceptBelow Avoid this subtree.
     * @return the TreeItems with ANodes that have the same conceptID as anodeToLookFor.
     */
    public static Collection<TreeItem<ANode>> lookForNodesWIdenticalConceptID(ANode anodeToLookFor, TreeItem<ANode> root, boolean checkSameANodeInstance, TreeItem<ANode> exceptBelow) {
        LinkedList<TreeItem<ANode>> duplicates = new LinkedList<>();
        if (exceptBelow != null) accumulateForEveryNodeBelow(root, duplicates, aNodeTreeItem -> aNodeTreeItem.getValue().conceptId().equals(anodeToLookFor.conceptId()) ? aNodeTreeItem : null, exceptBelow);
        else accumulateForEveryNodeBelow(root, duplicates, aNodeTreeItem -> aNodeTreeItem.getValue().conceptId().equals(anodeToLookFor.conceptId()) ? aNodeTreeItem : null);
        duplicates.removeIf(Objects::isNull);
        duplicates.removeIf((o) -> !checkSameANodeInstance && o.getValue() == anodeToLookFor);
        return duplicates;
    }
    public static Collection<TreeItem<ANode>> lookForNodesWIdenticalConceptID(ANode anodeToLookFor, TreeItem<ANode> root, boolean checkSameANodeInstance) {
        return  lookForNodesWIdenticalConceptID(anodeToLookFor, root, checkSameANodeInstance,null);
    }



    /**
     * For every given TreeItem, check if there are other nodes in the tree below root that have an ANode with the same conceptID.
     * @param root Root of tree
     * @param itemsToCheck Collection of TreeItems to check
     * @param exceptBelow Avoid this subtree.
     * @return Map of Node : Nodes with the same ID
     */
    public static HashMap<TreeItem<ANode>, Collection<TreeItem<ANode>>> lookForDuplicateIDsInTree(Collection<TreeItem<ANode>> itemsToCheck, TreeItem<ANode> root, boolean checkSameANodeInstance, TreeItem<ANode> exceptBelow) {

        HashMap<TreeItem<ANode>, Collection<TreeItem<ANode>>> duplicateItemsMap = new HashMap<>();
        for (TreeItem<ANode> treeItemToCheck : itemsToCheck) {
            Collection<TreeItem<ANode>> duplicates;
            if (exceptBelow != null) duplicates = lookForNodesWIdenticalConceptID(treeItemToCheck.getValue(),root, checkSameANodeInstance, exceptBelow);
            else duplicates = lookForNodesWIdenticalConceptID(treeItemToCheck.getValue(),root, checkSameANodeInstance);
            if (!duplicates.isEmpty()) {
                duplicateItemsMap.put(treeItemToCheck, duplicates);
            }
        }
        return duplicateItemsMap;
    }

    public static HashMap<TreeItem<ANode>, Collection<TreeItem<ANode>>> lookForDuplicateIDsInTree(Collection<TreeItem<ANode>> itemsToCheck, TreeItem<ANode> root, boolean checkSameANodeInstance) {
        return lookForDuplicateIDsInTree(itemsToCheck, root, checkSameANodeInstance, null);
    }

    public static boolean doesNodeWFilesContainName(TreeItem<ANode> root, String name) {
        if (!root.getValue().fileIds().isEmpty() && root.getValue().name().contains(name)) return true;
        for (TreeItem<ANode> child : root.getChildren()) {
            if (doesNodeWFilesContainName(child, name)) return true;
        }
        return false;
    }
}
