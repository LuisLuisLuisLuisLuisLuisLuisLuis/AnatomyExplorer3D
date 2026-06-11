package Project.window.ThreeDPaneHandling;

import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;

import java.util.LinkedList;

public class Setup3DSubPane {

    /**
     * Sets up a 3D sub-pane for rendering 3D objects within the application.
     * Creates and configures a 3D scene with lighting, camera, and background color.
     * Adds the configured subscene to the application's 3D drawing pane.
     *
     * @param drawPane The 3D drawing pane.
     * @return A Group representing the root content group of the 3D scene,
     *         which can be further used for adding 3D objects to the scene.
     */
    public static LinkedList<Group> setup3DSubPane(Pane drawPane, PerspectiveCamera camera, Point3D initialCameraPosition) {

        Group contentGroup = new Group(); // gets the objects to draw
        Group root3d = new Group(contentGroup);

        SubScene subScene = new SubScene(root3d, 600, 600, true, SceneAntialiasing.BALANCED);
        subScene.widthProperty().bind(drawPane.widthProperty());
        subScene.heightProperty().bind(drawPane.heightProperty());
        //background lightgrey
        subScene.setFill(Color.LIGHTGRAY);

        // add camera
        camera.setFarClip(8000);
        camera.setNearClip(20);//0.1 made the two boxes of which the slicePlane is made up glitch. besides, 20 is much more practical visually and close enough as the meshes are very large here anyway.
        camera.setTranslateZ(initialCameraPosition.getZ());
        camera.setTranslateX(initialCameraPosition.getX());
        camera.setTranslateY(initialCameraPosition.getY());

        subScene.setCamera(camera);


        // Add PointLight
        PointLight pointLight = new PointLight(Color.DARKGREY);

        pointLight.setTranslateX(initialCameraPosition.getX());
        pointLight.setTranslateY(initialCameraPosition.getY());
        pointLight.setTranslateZ(initialCameraPosition.getZ());

        // Add AmbientLight
        AmbientLight ambientLight = new AmbientLight(Color.rgb(180, 180, 180));

        // Add lights to the root3d group
        root3d.getChildren().addAll(pointLight, ambientLight);

        drawPane.getChildren().add(subScene);
        LinkedList<Group> groups = new LinkedList<>();
        groups.add(root3d);
        groups.add(contentGroup);
        return groups;
    }

}
