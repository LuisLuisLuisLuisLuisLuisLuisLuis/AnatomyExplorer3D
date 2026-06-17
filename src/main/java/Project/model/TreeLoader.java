package Project.model;


import Project.SelectionModel.FXGroupDraw.HasFXGroupContents;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class responsible for loading a tree structure from tab-separated data files.
 * <p>
 * The class builds a tree of {@link ANode} objects using concept and relation data
 * provided in the input files. It expects:
 * <ul>
 *   <li>A parts file mapping concept IDs to representation IDs</li>
 *   <li>An elements file mapping concept IDs to associated filenames</li>
 *   <li>A relations file describing parent-child relationships between concepts</li>
 * </ul>
 *
 * This class is intended to be used for constructing a single-rooted tree structure
 * which represents hierarchical relations between concepts.
 *
 * All files must contain a header line and be tab-separated.
 *
 * @author Luis Reimer, Niklas Gerbes
 */
public class TreeLoader {

    /**
     *
     * All files must be tab separated and are assumed to have one header line.
     * @param elementsFile of format: concept id | name | filename.     File name: xxx_element_parts.txt
     * @param relationsFile of format: concept id (parent) | name (parent) | concept id (child) | name (child). File name: xxx_inclusion_relation_list.txt
     * @return the root of the tree
     */
    public static ANode load(InputStream elementsFile, InputStream relationsFile) throws IllegalArgumentException{
        HashMap<String, HashSet<String>> fileList  = loadFileList (elementsFile);
        LinkedList<Relation> relations = Relation.loadFromFile(relationsFile);

        if (relations.isEmpty()) {
            return null;
        }

        return createTree(relations, fileList);
    }

    public static ANode load(InputStream relationsFile) {
        if (relationsFile == null) return null;
        return createTree(Relation.loadFromFile(relationsFile), new HashMap<>());
    }

    /**
     * From a List of Relations, generates ANode objects with the appropriate child-relations.
     * Hands back the master root or null if there is no root because there are no edges.
     * Assumes that the list of Relations represents one tree. otherwise there may be bugs (?)
     */
    private static ANode createTree(LinkedList<Relation> relations, HashMap<String, HashSet<String>> fileList) {

        HashMap<String, ANode> nodes = new HashMap<>(); //all nodes
        HashSet<String> notRootID = new HashSet<>(); //all nodes that are children
        //the set and map will be compared to find the node that is the root

        HashSet<ANode> children = new HashSet<>();

        for (Relation relation : relations) {
            String parentID = relation.parentID();
            String parentName = relation.parentName();
            String childID = relation.childID();
            String childName = relation.childName();

            if (childID.isBlank()) {
                if (!nodes.containsKey(parentID)) nodes.put(parentID, new ANode(parentID, parentName, new HashSet<>(), new HashSet<>()));
                continue;
            }

            //get child/parent
            ANode child = nodes.containsKey(childID) ? nodes.get(childID) : new ANode(childID, childName, new HashSet<>(), fileList.getOrDefault(childID, new HashSet<>()));
            if (children.contains(child)) {
                logger.log(Level.WARNING, "Skipping relation " + relation + " because it would create a cycle.");
                continue; // prevents cycles
            }
            else children.add(child);
            ANode parent = nodes.containsKey(parentID) ? nodes.get(parentID) : new ANode(parentID, parentName, new HashSet<>(), fileList.getOrDefault(parentID, new HashSet<>()));

            //add child to parents children
            parent.addChild(child);

            nodes.put(childID, child);
            nodes.put(parentID, parent);

            notRootID.add(child.conceptId());

        }

        //collect all possible roots
        HashSet<ANode> roots = new HashSet<>();
        for (String ID : nodes.keySet()) {
            if (!notRootID.contains(ID)) {
                roots.add(nodes.get(ID));
            }
        }
        //if there is only one option, return it.
        if (roots.size() == 1) return (ANode) roots.toArray()[0];
        //else, create a new artificial root holding all collected roots.
        return new ANode("newArtificialRoot", "new artificial root", roots, new HashSet<>());



    }

    /**
     * Generates a mapping of conceptId to representationId.
     * unused because i dont use representationID
     * @param inputStream of file path
     * @return the mapping
     */
    private static HashMap<String, String> mapConceptIDRepID(InputStream inputStream) {
        String fileContents = readInputStream(inputStream);
        String[] lines = fileContents.split("\n");
        HashMap<String, String> conceptToRepresentationMap = new HashMap<>();
        for (int i = 1; i < lines.length; i++) { // start from 1, skip header
            String line = lines[i];
            if (line.isBlank()) continue;
            String[] lineArr = line.split("\t");
            conceptToRepresentationMap.put(lineArr[0].trim(), lineArr[1].trim());
        }
        return conceptToRepresentationMap;
    }


    private static HashMap<String, HashSet<String>> loadFileList(InputStream inputStream) throws IllegalArgumentException{
        HashMap<String, HashSet<String>> IDtoFilelist = new HashMap<>();
        String fileContents = readInputStream(inputStream);
        String[] lines = fileContents.split("\n");
        for (int i = 1; i < lines.length; i++) { // start from 1, skip header
            String line = lines[i];
            if (line.isBlank()) continue;
            String[] lineArr = line.split("\t");
            if (lineArr.length != 3) throw new IllegalArgumentException("Required format for file list: ID\\tName\\tFileName\nFailed to parse line: " + line);
            String conceptID = lineArr[0].trim();
            HashSet<String> fileList = IDtoFilelist.containsKey(conceptID) ? IDtoFilelist.get(conceptID) : new HashSet<>();
            fileList.add(lineArr[2].trim());
            IDtoFilelist.put(conceptID, fileList);
        }
        for (String id : IDtoFilelist.keySet()) {
            HasFXGroupContents.appendFileExtension(IDtoFilelist.get(id)); // every fileID must have a file extension.
        }
        return IDtoFilelist;
    }

    /**
     * Given an inputStream of a file path: read the file and give back as full string.
     * Will contain \n after every line.
     */
    public static String readInputStream(InputStream inputStream) {StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {e.printStackTrace();}
        return stringBuilder.toString();
    }

    private static final Logger logger = Logger.getLogger(TreeLoader.class.getName());
}
