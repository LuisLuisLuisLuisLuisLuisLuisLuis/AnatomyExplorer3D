package Project.model;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;


public class ANode implements Comparable<ANode> {

    private String conceptId;
    private String name;
    private Set<String> fileIds;
    private Set<ANode> children;

    public String getConceptId() {return conceptId;}
    public String getName() {return name;}
    public Set<String> getFileIds() {return fileIds;}
    public Set<ANode> getChildren() {return children;}

    public String conceptId() {return conceptId;}
    public String name() {return name;}
    public Set<String> fileIds() {return fileIds;}
    public Set<ANode> children() {return children;}

    public void setName(String name) {this.name = name;}

    /**
     * Compares names of the ANodes. Thus, NOT consistent with Objects.equals().
     */
    public int compareTo(@NotNull ANode other) {
        return this.getName().compareTo(other.getName());
    }

    /**
     * New implementation of ANode! (DIFFERENT TO ANode FROM ASSIGNMENT01!)
     * @param conceptId
     * @param name
     * @param children
     * @param fileIds
     *
     * @author Luis Reimer, Niklas Gerbes
     */
    public ANode(String conceptId, String name, Set<ANode> children, Set<String> fileIds) {
        this.conceptId = conceptId;
        this.children = children;
        this.name = name;
        this.fileIds = fileIds;
    }

    public String toString() {
        return name;// + " (" + conceptId + ")";
    }
    protected void addChild(ANode child) {
        children.add(child);
    }

    public ANode copy() {return new ANode(new String(conceptId), new String(name), new HashSet<>(children), new HashSet<>(fileIds));}

    /**
     * Prints all paths in the subtree below a node (only paths that end in a leaf, not internal node).
     * Keywords may be added to only print those paths which include all the keywords.
     * Works as a wrapper for the private recursive collectPrint() function
     * @param root as the node whose subtree is to be printed
     * @param filters as a String Array of keywords.
     */
    public static void printTree(ANode root, String[] filters) {
        System.out.println(collectPrint(root, "", filters));
    }

    /**
     * Does the computation required by printTree(). Hands back the complete String, ready to be printed.
     * @param root
     * @param p
     * @param filters
     * @return
     */
    private static String collectPrint(ANode root, String p, String[] filters) {
        p += root.name;
        StringBuilder pBuilder = new StringBuilder();

        for (ANode child : root.children) {
            pBuilder.append(collectPrint(child, p+ "->", filters));
        }
        if (root.children.isEmpty()) { //avoid printing paths that end in internal nodes
            pBuilder.append(p).append("\n");
        }
        String res = pBuilder.toString();
        for (String filter : filters) { //avoid printing paths that dont contain the keywords
            if (!res.contains(filter)) {
                return "";
            }
        }
        return res;
    }

}