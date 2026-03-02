package Project.window.Slicing;

import java.util.List;
import java.util.ArrayList;
import Project.window.ThreeDPaneHandling.Axes;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.NonInvertibleTransformException;


public class Plane {

    /**
     * Makes a plane with a red and green side by drawing two thin red and green boxes.
     * @param size Width and height of the plane
     * @return Both boxes in a group.
     */
    public static Group makeSlicePlane(int size) {
        Box keepSide = makeBox(size, size, 1, Axes.makePhongMaterialW(Color.GREEN));
        Box cutSide = makeBox(size, size, 1, Axes.makePhongMaterialW(Color.RED));
        keepSide.setTranslateZ(keepSide.getTranslateZ() + 1);
        return new Group(keepSide, cutSide);
    }

    /**
     * Makes a box with the given specifications rotated by 90deg around Z.
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
     * Assume the Box represents a Plane whose Normal was originally (0,0,1).
     * Obtain the direction of the normal in the coordinate system of meshGroup.
     * @return The plane as normal (x,y,z,d)
     */
    public static double[] extractPlaneFromBox(Box box, Group meshGroup) throws NonInvertibleTransformException {

        // 1. local normal (thin axis = Z)
        Point3D localNormal = new Point3D(0,0,1);

        // 2. normal to world
        Point3D worldNormal =
                box.getLocalToSceneTransform()
                        .deltaTransform(localNormal)
                        .normalize();

        // 3. point on plane (box center)
        Point3D worldPoint =
                box.localToScene(Point3D.ZERO);

        // 4. convert to mesh local space
        Point3D meshPoint =
                meshGroup.sceneToLocal(worldPoint);

        Point3D meshNormal =
                meshGroup.getLocalToSceneTransform()
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

    /**
     * Extracts the planes from a Box and transforms them to the coordinate system of the given Group.
     * @param box Box of which the planes are to be extracted.
     * @param meshGroup Group into whose coordinate system the planes should be merged.
     * @return List of planes. Each plane is represented by its normal [x,y,z,d].
     */
    public static List<double[]> extractBoxPlanes(Box box, Group meshGroup) throws NonInvertibleTransformException {

        double hw = box.getWidth()  / 2.0;
        double hh = box.getHeight() / 2.0;
        double hd = box.getDepth()  / 2.0;

        Point3D[] localNormals = {
                new Point3D( 1,0,0),
                new Point3D(-1,0,0),
                new Point3D(0, 1,0),
                new Point3D(0,-1,0),
                new Point3D(0,0, 1),
                new Point3D(0,0,-1)
        };

        Point3D[] localPoints = {
                new Point3D( hw,0,0),
                new Point3D(-hw,0,0),
                new Point3D(0, hh,0),
                new Point3D(0,-hh,0),
                new Point3D(0,0, hd),
                new Point3D(0,0,-hd)
        };

        ArrayList<double[]> planes = new ArrayList<>();

        for (int i=0; i<6; i++) {

            // normal to world
            Point3D worldNormal =
                    box.getLocalToSceneTransform()
                            .deltaTransform(localNormals[i])
                            .normalize();

            // point to world
            Point3D worldPoint =
                    box.localToScene(localPoints[i]);

            // convert to mesh space
            Point3D meshPoint =
                    meshGroup.sceneToLocal(worldPoint);

            Point3D meshNormal =
                    meshGroup.getLocalToSceneTransform()
                            .createInverse()
                            .deltaTransform(worldNormal)
                            .normalize();

            double nx = meshNormal.getX();
            double ny = meshNormal.getY();
            double nz = meshNormal.getZ();

            double d = nx * meshPoint.getX()
                    + ny * meshPoint.getY()
                    + nz * meshPoint.getZ();

            planes.add(new double[]{nx,ny,nz,d});
        }

        return planes;
    }


}
