package Project.window.Slicing;

import Project.window.ThreeDPaneHandling.Objects.Axes;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.NonInvertibleTransformException;


public class Plane {

    public enum BodyAxis {
        /** Corresponds to z-axis in BP3D, where bottom -> top is positive direction.*/
        AXIAL,
        /** Corresponds to x-axis in BP3D, where right -> left is positive direction.*/
        SAGGITAL,
        /** Corresponds to y-axis in BP3D, where front -> back is positive direction.*/
        CORONAL,
    }

    /**
     * Makes a plane with a red and green side by drawing two thin red and green boxes.
     * @param size Width and height of the plane
     * @return Both boxes in a group.
     */
    public static Group makeSlicePlane(int size) {
        Box keepSide = makeBox(size, size, 1, Axes.makePhongMaterialW(Color.GREEN));
        Box cutSide = makeBox(size, size, 1, Axes.makePhongMaterialW(Color.RED));
        keepSide.setTranslateZ(keepSide.getTranslateZ() - 1);
        return new Group(keepSide, cutSide);
    }

    /**
     * Makes a box with the given specifications
     * @return The Box.
     */
    public static Box makeBox(double height, double width, double depth, PhongMaterial phongMaterial) {
        Box box = new Box(width, height, depth);
        box.setMaterial(phongMaterial);
        box.setTranslateX(0);
        box.setTranslateY(0);
        return box;
//        return (Box) Axes.rotateShape3D(box, new Point3D(0,0,1), 90);
    }

    /**
     * @param node a node
     * @return the center of the node in local space
     */
    public static Point3D computeCenter(Node node) {
        Bounds bounds = node.getBoundsInLocal();
        double X = (bounds.getMinX() + bounds.getMaxX()) / 2;
        double Y = (bounds.getMinY() + bounds.getMaxY()) / 2;
        double Z = (bounds.getMinZ() + bounds.getMaxZ()) / 2;
        return new Point3D(X,Y,Z);
    }

    /**
     * Assume the Box represents a Plane whose normal was originally (0,0,1).
     * Obtain the direction of the normal in the coordinate system of meshGroup.
     * @return The plane as normal (x,y,z,d)
     */
    public static double[] extractPlaneFromBox(Box box, Group meshGroup) throws NonInvertibleTransformException {

        // 1. local normal (thin axis = Z)
        Point3D localNormal = new Point3D(0,0,-1);

        // 2. normal to world
        Point3D worldNormal = box.getLocalToSceneTransform()
                .deltaTransform(localNormal)
                .normalize();

        // 3. point on plane (box center)
        Point3D worldPoint = box.localToScene(Point3D.ZERO);

        // 4. convert to mesh local space
        Point3D meshPoint = meshGroup.sceneToLocal(worldPoint);

        Point3D meshNormal = meshGroup.getLocalToSceneTransform()
                .createInverse()
                .deltaTransform(worldNormal)
                .normalize();

        // 5. compute d
        double nx = meshNormal.getX();
        double ny = meshNormal.getY();
        double nz = meshNormal.getZ();

        double d = nx * meshPoint.getX()
                + ny * meshPoint.getY()
                + nz * meshPoint.getZ();

        return new double[]{ nx, ny, nz, d };
    }


}
