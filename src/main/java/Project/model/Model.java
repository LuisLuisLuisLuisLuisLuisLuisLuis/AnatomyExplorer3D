package Project.model;

import com.almasb.fxgl.input.Input;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

/**
 * Loads the Tree from the files and stores the root.
 */
public class Model {
    private final ANode partOf_Root;
    private final ANode isA_Root;
    private final ANode dumpRoot;

    /**
     * All files must be tab separated and are assumed to have one header line.
     * There are two hierarchies: part-Of and is-a. They are both organized in three types of files with distinct structures.
     * Below is an explanation of which parameter takes which filepath and an outline of the structure of the corresponding file.
     * For the part-of hierarchy:
     *
     * @param partOf_element_parts_file of format: concept id | name | filename.     File name: xxx_element_parts.txt
     * @param partOf_inclusion_relation_list_file of format: concept id (parent) | name (parent) | concept id (child) | name (child). File name: xxx_inclusion_relation_list.txt
     * For the is-a hierarchy:
     * @param isA_element_parts_file of format: concept id | name | filename.     File name: xxx_element_parts.txt
     * @param isA_inclusion_relation_list_file of format: concept id (parent) | name (parent) | concept id (child) | name (child). File name: xxx_inclusion_relation_list.txt
     */

    public Model(InputStream partOf_element_parts_file, InputStream partOf_inclusion_relation_list_file, InputStream isA_element_parts_file, InputStream isA_inclusion_relation_list_file, InputStream dumprelations, InputStream dumpfileslist) throws IOException {
        dumpRoot = TreeLoader.load(dumpfileslist, dumprelations);
        partOf_Root = TreeLoader.load(partOf_element_parts_file, partOf_inclusion_relation_list_file);
        isA_Root = TreeLoader.load(isA_element_parts_file, isA_inclusion_relation_list_file);
    }


    public ANode getDumpRoot() {return dumpRoot;}
    public ANode getPartOfRoot() {
        return partOf_Root;
    }
    public ANode getIsA_Root() {return isA_Root;}
}
