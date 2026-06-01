package Project.window.TreeView.TreeAnalysis;

import Project.model.ANode;
import Project.window.TreeView.TreeViewEditing.Command.UndoableANodeTreeViewEditor;
import Project.window.TreeView.TreeViewEditing.TreeViewSetup;
import javafx.scene.control.TreeItem;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public class TreeAnalysisUtils {

    /**
     *
     * @param root The root of a tree
     * @param node A node
     * @return is the node part of the tree of root?
     */
    public static <T> boolean isPartOfTree(TreeItem<T> root, @NotNull TreeItem<T> node) {
        if (node.equals(root)) return true;
        if (node.getParent() == null) return false;
        return isPartOfTree(root, node.getParent());
    }

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

    public static <T,O> void applyRec(ANode root, Function<ANode, O> fun) {
        fun.apply(root);
        for (ANode child : root.getChildren()) applyRec(child, fun);
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

    /**
     * Find and return the TreeItem whose ANode has the specified id in the tree with the given root or null if not present.
     */
    public static TreeItem<ANode> getTreeItemWANodeId(String id, TreeItem<ANode> root) {
        if (root.getValue().conceptId().equals(id)) return root;
        else {
            if (root.getChildren().isEmpty()) return null;
            else {
                TreeItem<ANode> result = null;
                for (TreeItem<ANode> child : root.getChildren()) {
                    result = getTreeItemWANodeId(id, child);
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

    public static void accumulateAllTreeItemsBelow(TreeItem<ANode> root, Collection<TreeItem<ANode>> dump) {
        dump.add(root);
        for (TreeItem<ANode> child : root.getChildren()) accumulateAllTreeItemsBelow(child, dump);
    }

    public static Set<TreeItem<ANode>> accumulateAllTreeItemsBelow(TreeItem<ANode> root) {
        HashSet<TreeItem<ANode>> result = new HashSet<>();
        accumulateAllTreeItemsBelow(root, result);
        return result;
    }

    public static boolean doesNodeWFilesContainName(TreeItem<ANode> root, String name) {
        if (!root.getValue().fileIds().isEmpty() && root.getValue().name().contains(name)) return true;
        for (TreeItem<ANode> child : root.getChildren()) {
            if (doesNodeWFilesContainName(child, name)) return true;
        }
        return false;
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



    /**
     * If the subtree below treeItem contains items whose names contain 'left' and 'right', this will split the subtree
     * apart as far as possible and create two separate subtrees for 'left' and 'right' side.
     * @param treeItem A treeItem with a subtree that is to be split.
     * @param modelRoot ANode root of the tree that contains treeItem.
     */
    public static void splitSubtreeLeftRight(TreeItem<ANode> treeItem, ANode modelRoot) {
        if (!isLeftRightSymetricBelow(treeItem, "")) return;
        TreeItem<ANode> rightRoot = new TreeItem<>(makeAnode("right " + treeItem.getValue().getName(), modelRoot));
        TreeItem<ANode> leftRoot = new TreeItem<>(makeAnode("left " + treeItem.getValue().name(), modelRoot));
        treeItem.getParent().getChildren().add(rightRoot);
        treeItem.getParent().getChildren().add(leftRoot);
        treeItem.getParent().getValue().children().add(rightRoot.getValue());
        treeItem.getParent().getValue().children().add(leftRoot.getValue());
        collectSideLeavesBelowRec(treeItem, rightRoot, "right", modelRoot);
        collectSideLeavesBelowRec(treeItem, leftRoot, "left", modelRoot);
    }

    /**
     * Collects all treeItems below root that have the given side ('left' or 'right') and places them below
     * sidedRoot, respecting their hierarchy below root.
     * @param root TreeItem with left-right mixed subtree.
     * @param sidedRoot TreeItem below which a subtree with only the given side is to be constructed.
     * @param side left or right
     * @param modelRoot ANode root of the tree that contains root.
     */
    public static void collectSideLeavesBelowRec(TreeItem<ANode> root, TreeItem<ANode> sidedRoot, String side, ANode modelRoot) {

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

                TreeItem<ANode> sidedChild = createSidedTreeItemFromUnsided(child, side, modelRoot);
                sidedRoot.getChildren().add(sidedChild);
                sidedRoot.getValue().getChildren().add(sidedChild.getValue());
                collectSideLeavesBelowRec(child, sidedChild, side, modelRoot);
            } else {
                if (getExpectedSide.apply(child.getValue().name()).equals(side)) {
                    UndoableANodeTreeViewEditor.moveTreeItem(child, sidedRoot);
//                    cut(child);
//                    paste(sidedRoot, this.getTreeView().getRoot());
                    if (child.getValue().name().replace(side+" ", "").trim().equals(sidedRoot.getValue().getName().replace(side, "").trim()) && sidedRoot.getValue().fileIds().containsAll(child.getValue().fileIds())) {
//                                if (child.getValue().name().equals(sidedRoot.getValue().getName()) && sidedRoot.getValue().fileIds().containsAll(child.getValue().fileIds())) {
                        sidedRoot.getChildren().remove(child);
                        sidedRoot.getValue().children().remove(child.getValue());
//                        runOnANodeFileIDsModified();
                    } else System.out.println("false because " + child.getValue().name().replace(side+" ", "").trim() + " != " + sidedRoot.getValue().getName().replace(side, "").trim());
                    continue;
                } else continue;
            }

        }
    }

    /**
     * Creates a new TreeItem with the value of the given one but prepends the given side to the name.
     * @param treeItem old treeItem
     * @param side left or right (or any other string)
     * @param modelRoot ANode root of the tree that contains treeItem. Needed to be able to create a new unique ANode in the tree.
     * @return the new TreeItem with the new ANode.
     */
    public static TreeItem<ANode> createSidedTreeItemFromUnsided(TreeItem<ANode> treeItem, String side, ANode modelRoot) {
        return new TreeItem<>(makeAnode(side + " " + treeItem.getValue().name(), modelRoot));
    }

    /**
     * Checks whether the tree below root is left/right symetric. I.e. if for every item whose name contains a side ('left'/'right')
     * there exists an item with the same name for the opposing side in the correct position in the tree.
     * @param root TreeItem below which to check
     * @param expectedSide Set this if you expect this tree to only hold this side. Give empty string to ignore.
     * @return true/false.
     */
    public static boolean isLeftRightSymetricBelow(TreeItem<ANode> root, String expectedSide) {

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

    public static ANode makeAnode(String name, ANode modelRoot) {
        String id = TreeViewSetup.generateUnusedID(modelRoot);
        return new ANode(
                id, name, new HashSet<>(), new HashSet<>()
        );
    }

    /**
     * Moves 'left' / 'right' behind the n'th last word in every item in the subtree.
     * IS BUGGY.
     * @param root root of subtree
     * @param wordpos n
     */
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

}