package Project.window.Slicing;

import Project.window.ThreeDPaneHandling.Axes;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.NonInvertibleTransformException;

public class Plane {

    public static Group makeSlicePlane() {
        Box keepSide = makeBox(2000, 2000, 1, Axes.makePhongMaterialW(Color.GREEN));
        Box cutSide = makeBox(2000, 2000, 1, Axes.makePhongMaterialW(Color.RED));
        keepSide.setTranslateZ(keepSide.getTranslateZ() + 1);
        return new Group(keepSide, cutSide);
    }


    public static Box makeBox(double height, double width, double depth, PhongMaterial phongMaterial) {
        Box box = new Box(width, height, depth);
        box.setMaterial(phongMaterial);
        box.setTranslateX(0);
        box.setTranslateY(0);
        return (Box) Axes.rotateShape3D(box, new Point3D(0,0,1), 90);
    }

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

    public static Box makeBox() {
        return makeBox(2000,2000,0.1, Axes.makePhongMaterialW(Color.GREY));
    }


}
