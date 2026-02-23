package Project.window.ThreeDPaneHandling;

import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.transform.Affine;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//large parts of geometry computation provided by a well known LLM (GPT-4o)

public class Explode {
    private SequentialTransition sequentialTransition;
    private SimpleBooleanProperty isExploding = new SimpleBooleanProperty(false);

    public SimpleBooleanProperty isExploding() {
        return this.isExploding;
    }

    public void explode(List<Node> nodes, Group group, Affine initialTransform) {

        this.isExploding.set(true);

        //remember original positions for meshs to return to after exploding
        Map<Node, Point3D> originalPositions = new HashMap<>();

        //compute the center of the set of selected meshes as the point from which they move away
        Bounds combinedBounds = null;
        for (Node node : nodes) {
            if (node == null) continue;
            Bounds bounds = node.getBoundsInParent();
            if (combinedBounds == null) {
                combinedBounds = bounds;
            } else {
                double minX = Math.min(combinedBounds.getMinX(), bounds.getMinX());
                double minY = Math.min(combinedBounds.getMinY(), bounds.getMinY());
                double minZ = Math.min(combinedBounds.getMinZ(), bounds.getMinZ());
                double maxX = Math.max(combinedBounds.getMaxX(), bounds.getMaxX());
                double maxY = Math.max(combinedBounds.getMaxY(), bounds.getMaxY());
                double maxZ = Math.max(combinedBounds.getMaxZ(), bounds.getMaxZ());

                combinedBounds = new BoundingBox(minX, minY, minZ, maxX - minX, maxY - minY, maxZ - minZ);
            }
        }

        Point3D selectionCenter = new Point3D(
                (combinedBounds.getMinX() + combinedBounds.getMaxX()) / 2,
                (combinedBounds.getMinY() + combinedBounds.getMaxY()) / 2,
                (combinedBounds.getMinZ() + combinedBounds.getMaxZ()) / 2
        );

        //generate the animation for each node
        for (Node mesh : nodes) {

            //compute the center of this mesh
            Bounds bounds = mesh.localToParent(mesh.getBoundsInParent());
            Point3D meshLocalCenter = new Point3D(
                    (bounds.getMinX() + bounds.getMaxX()) / 2,
                    (bounds.getMinY() + bounds.getMaxY()) / 2,
                    (bounds.getMinZ() + bounds.getMaxZ()) / 2

            );

            //the direction of movement is given by the vector (center of selected meshes -> center of this mesh)
            Point3D direction = meshLocalCenter.subtract(selectionCenter).normalize();
            Point3D target = direction.multiply(100); //explosion distance

            //remember the original position of this mesh
            originalPositions.put(mesh, new Point3D(
                    mesh.getTranslateX(),
                    mesh.getTranslateY(),
                    mesh.getTranslateZ()
            ));

            TranslateTransition explode = new TranslateTransition(Duration.seconds(2), mesh);
            explode.setToX(mesh.getTranslateX() + target.getX());
            explode.setToY(mesh.getTranslateY() + target.getY());
            explode.setToZ(mesh.getTranslateZ() + target.getZ());

            Point3D original = originalPositions.get(mesh);
            TranslateTransition implode = new TranslateTransition(Duration.seconds(2), mesh);
            implode.setToX(original.getX());
            implode.setToY(original.getY());
            implode.setToZ(original.getZ());

            this.sequentialTransition = new SequentialTransition(
                    explode, new PauseTransition(Duration.seconds(1)), implode
            );

            this.sequentialTransition.setOnFinished((InvalidationListener) -> {
                this.isExploding.set(false);
            });


            sequentialTransition.play();

        }

    }
}
