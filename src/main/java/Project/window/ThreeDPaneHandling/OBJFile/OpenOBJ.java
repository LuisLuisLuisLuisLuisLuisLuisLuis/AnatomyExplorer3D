package Project.window.ThreeDPaneHandling.OBJFile;

import Project.SelectionModel.FXGroupDraw.HasFXGroupContentsContainer;
import Project.window.WindowPresenter;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class OpenOBJ {


    /**
     * Creates a meshView from a TriangleMesh and an image file.
     * If the image file doesn't exist, the triangleMesh will be green.
     * @param mesh TriangleMesh
     * @param image File
     * @return meshView
     */
    public static MeshView createMeshWithIMG(TriangleMesh mesh, File image, Color color) {
        MeshView meshView = new MeshView(mesh);
        PhongMaterial material = new PhongMaterial();
        material.setSpecularColor(Color.DARKGREY);

        if (image.exists()) {
            //meshView.setCullFace(CullFace.FRONT); strangely caused the cube to deform when rotating.
            material.setDiffuseMap(new Image(image.toURI().toString()));
        } else {
            if (color == null) color = HasFXGroupContentsContainer.DEFAULT_COLOR;
            material.setDiffuseColor(color);
        }
        meshView.setMaterial(material);
        WindowPresenter.getWindowPresenter().applySelectionEffect(meshView, false);
        return meshView;
    }
    //default case of no color
    public static MeshView createMeshWithIMG(TriangleMesh mesh, File image) {
        return createMeshWithIMG(mesh, image, null);
    }

    /**
     * Opens a FileChooser with .obj extension and multiple selection enabled.
     * @return the list of chosen files
     */

    public static List<File> openOBJDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open OBJ files");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("OBJ file", "*.obj"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );

        return fileChooser.showOpenMultipleDialog(new Stage()); //choose OBJ files
    }

    /**
     *
     * @param objFiles Accepts a list of files (they have to be .obj files).
     * @return a list of meshViews created from the files. Tries to use a .png file with identical name to color each .obj file
     * Sets the name of the file as ID for each meshview.
     */
    public static List<MeshView> objFilesToMeshViews(List<File> objFiles) {
        List<MeshView> meshViews = new LinkedList<>();  //will hold MeshViews of the parsed OBJ files

        for (File objFile : objFiles) {
            MeshView meshView = objFileToMeshViews(objFile);
            if (meshView != null) {
                meshViews.add(meshView);
            }
        }
        //add meshviews from all OBJ files to the content
        return meshViews;
    }

    public static MeshView objInputStreamToMeshViews(InputStream inputStream, String nameWithoutExtension, Color color) {
        if (inputStream != null) {
            File imgFile = new File(nameWithoutExtension + ".png");
            try {
                //generate mesh from OBJ inputstream. For now I ignore normals to save RAM.
                MeshView meshView = createMeshWithIMG(ObjParser_V3.load(inputStream, false), imgFile, color);
                //set file name as ID
                meshView.setId(nameWithoutExtension);

                return meshView;
            } catch (Exception e) {
                System.err.println(Arrays.toString(e.getStackTrace()));
                System.err.println(e.getMessage());
                System.err.println("Failed to parse Inputstream of .obj resource " + nameWithoutExtension + ".obj");
            }
        } return null;
    }
    //default case of no color
    public static MeshView objInputStreamToMeshViews(InputStream inputStream, String nameWithoutExtension) {
        return objInputStreamToMeshViews(inputStream, nameWithoutExtension, null);
    }


    /**
     * Accept a File (.obj). Generate a MeshView using createMeshWithIMG(). Set the filename minus extension as the ID.
     * @param objFile: a File (.obj)
     * @return the MeshView.
     */
    public static MeshView objFileToMeshViews(File objFile) {
        if (objFile != null) {

            try {
                return objInputStreamToMeshViews(objFile.toURI().toURL().openStream(), objFile.getName().replace(".obj", ""), null);
            } catch (IOException m) {
                return null;
            }

//          Legacy
//            String objPath = objFile.getAbsolutePath();
//            File imgFile = new File(objPath.substring(0, objPath.length() - 3) + "png");
//            try {
//                //generate mesh from the OBJ file
//                MeshView meshView = createMeshWithIMG(ObjParser_V2.load(objPath), imgFile);
//                //set file name as ID
//                meshView.setId(removeFileExtension(objFile.getName()));
//
//                return meshView;
//            } catch (Exception e) {
//                System.err.println("Failed to parse OBJ file: " + objPath);
//            }
        } return null;
    }



    private static String removeFileExtension(String fileName) {
        return fileName.substring(0, fileName.length()-4);
    }

}

