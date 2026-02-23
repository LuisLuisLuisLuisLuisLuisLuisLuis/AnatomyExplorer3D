package Project.window.TreeView.TreeAnalysis;

import Project.model.ANode;

import java.util.HashSet;

/**
 * This class compares one tree to two others. Used it to compare my restructured tree to BP3D Version 3.0 to see
 * which items they have that i don't. Only checks if all the leaves of the two trees are contained in the one tree.
 */
public class CompareBP3DV {
    private final ANode myTreeroot;
    private final ANode v3partof;
    private final ANode v3isa;

    private HashSet<String> missings = new HashSet<>();

    public CompareBP3DV(ANode myTreeroot, ANode v3partof, ANode v3isa) {
        this.myTreeroot = myTreeroot;
        this.v3partof = v3partof;
        this.v3isa = v3isa;
        fullLeafTreeCheck(myTreeroot, v3partof, missings);
        fullLeafTreeCheck(myTreeroot, v3isa, missings);
        for (String item : missings) System.out.println(item);
    }


    private static void fullLeafTreeCheck(ANode targetRoot, ANode testRoot, HashSet<String> missings) {
        if (testRoot.children().isEmpty() && !leafContainsName(targetRoot, testRoot.name())) missings.add(testRoot.name());
        for (ANode child : testRoot.children()) fullLeafTreeCheck(targetRoot, child, missings);
    }

    /**
     *
     * @param root
     * @param name
     * @return
     */
    private static boolean leafContainsName(ANode root, String name) {
        if (root.name().contains(name)) return true;
        for (ANode child : root.children()) {
            if (leafContainsName(child, name)) return true;
        }
        return false;
    }
}
