package Project.model;

import java.io.File;
import java.io.InputStream;


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

    private String resourceFilesDir = null;
    public String getResourceFilesDir() {return resourceFilesDir;}
    public void setFilesDir(String resourceFilesDir) {this.resourceFilesDir = resourceFilesDir;}
    // the location of this model's Files can be specified as either URL (as is the case for the included anatomy trees)
    // or File (as is the case when the model is created at runtime by the user (to be implemented)).

    private File filesDir = null;
    public File getFilesDir() {return filesDir;}
    public void setFilesDir(File filesDir) {this.filesDir = filesDir;}


    /*
    NOTES regarding File and URL:
    ❌ Don’t do:
    file.toURL();  // deprecated, unsafe
    ✅ Do:
    URL url = file.toURI().toURL();
     */


    /**
     * For every tree to be constructed, pass 1 relations file and 1 fileID file. Their positions in their arrays must be the same.
     * @param relations Edge list of format: concept id (parent)\tname (parent)\tconcept id (child)\tname (child)
     * @param fileIDs List of format: concept id\tname\tfilename, where filename corresponds to the name of an .obj file excluding the .obj extension.
     * @param resourceFilesDir Directory within resources that contains the OBJ Files. Usable via getClass().getResource()
     */
    public Model(InputStream relations, InputStream fileIDs, String name, String resourceFilesDir) throws IllegalArgumentException {
        this.root = TreeLoader.load(fileIDs, relations);
        this.name = name;
        this.resourceFilesDir = resourceFilesDir;
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
    }

    public Model(InputStream relations, String name) {
        this.root = TreeLoader.load(relations);
        this.name = name;
    }

}
