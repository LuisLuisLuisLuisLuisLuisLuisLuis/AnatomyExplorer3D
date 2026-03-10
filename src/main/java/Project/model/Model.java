package Project.model;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

/**
 * Loads the Tree from the files and stores the root.
 */
public class Model {

    /**
     * Returns the root of this tree. If the file from which this model was constructed did not contain a single unifying root
     * then a new root was created on top to make this a rooted tree.
     */
    private final ANode root;
    public ANode getRoot() {return root;}

    private final String name;
    public String getName() {return name;}

    private URL filesDirURL = null;
    public URL getFilesDirURL() {return filesDirURL;}
    public void setFilesDir(URL filesDirURL) {this.filesDirURL = filesDirURL;}
    // the location of this model's Files can be specified as either URL (as is the case for the included anatomy trees)
    // or File (as is the case when the model is created at runtime by the user (to be implemented)).

    private File filesDir = null;
    public File getFilesDir() {return filesDir;}
    public void setFilesDir(File filesDir) {this.filesDir = filesDir;}

    /*
    NOTES regarding File and URL:

    Do not convert URL to File unless you check:
    if (url.getProtocol().equals("file"))
    If it’s jar, conversion will break.

    ❌ Don’t do:
    file.toURL();  // deprecated, unsafe
    ✅ Do:
    URL url = file.toURI().toURL();
     */

    //TODO: REMOVE LEGACY COMMENT / COMMENTED OUT CODE
    /**
     * All files must be tab separated and are assumed to have one header line.
     * There are two hierarchies: part-Of and is-a. They are both organized in three types of files with distinct structures.
     * Below is an explanation of which parameter takes which filepath and an outline of the structure of the corresponding file.
     * For the part-of hierarchy:
     * (note that tabs are replaced by | here for readability)
     *
     * @param partOf_element_parts_file of format: concept id | name | filename.     File name: xxx_element_parts.txt
     * @param partOf_inclusion_relation_list_file of format: concept id (parent) | name (parent) | concept id (child) | name (child). File name: xxx_inclusion_relation_list.txt
     * For the is-a hierarchy:
     * @param isA_element_parts_file of format: concept id | name | filename.     File name: xxx_element_parts.txt
     * @param isA_inclusion_relation_list_file of format: concept id (parent) | name (parent) | concept id (child) | name (child). File name: xxx_inclusion_relation_list.txt
     */

//    public Model(InputStream partOf_element_parts_file, InputStream partOf_inclusion_relation_list_file, InputStream isA_element_parts_file, InputStream isA_inclusion_relation_list_file, InputStream dumprelations, InputStream dumpfileslist) throws IOException {
//        dumpRoot = TreeLoader.load(dumpfileslist, dumprelations);
//        partOf_Root = TreeLoader.load(partOf_element_parts_file, partOf_inclusion_relation_list_file);
//        isA_Root = TreeLoader.load(isA_element_parts_file, isA_inclusion_relation_list_file);
//    }

    /**
     * For every tree to be constructed, pass 1 relations file and 1 fileID file. Their positions in their arrays must be the same.
     * @param relations Edge list of format: concept id (parent)\tname (parent)\tconcept id (child)\tname (child)
     * @param fileIDs List of format: concept id\tname\tfilename, where filename corresponds to the name of an .obj file excluding the .obj extension.
     * @param filesDirURL Directory that holds the Files.
     */
    public Model(InputStream relations, InputStream fileIDs, String name, URL filesDirURL) {
        this.root = TreeLoader.load(fileIDs, relations);
        this.name = name;
        this.filesDirURL = filesDirURL;
    }

    /**
     * For every tree to be constructed, pass 1 relations file and 1 fileID file. Their positions in their arrays must be the same.
     * @param relations Edge list of format: concept id (parent)\tname (parent)\tconcept id (child)\tname (child)
     * @param fileIDs List of format: concept id\tname\tfilename, where filename corresponds to the name of an .obj file excluding the .obj extension.
     * @param filesDir Directory that holds the Files.
     */
    public Model(InputStream relations, InputStream fileIDs, String name, File filesDir) throws IllegalArgumentException{
        if (!filesDir.isDirectory()) throw new IllegalArgumentException("The provided File is no directory.");
        this.root = TreeLoader.load(fileIDs, relations);
        this.name = name;
        this.filesDir = filesDir;
        try {this.filesDirURL = filesDir.toURI().toURL();}
        catch (MalformedURLException ignored) {}    //TODO: can this even happen if filesDir is confirmed to be a directory?
    }

    public Model(InputStream relations, String name) {
        this.root = TreeLoader.load(relations);
        this.name = name;
    }

}
