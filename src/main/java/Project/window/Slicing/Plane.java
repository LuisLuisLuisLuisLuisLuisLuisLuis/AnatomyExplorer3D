package Project.window.Slicing;

import Project.window.ThreeDPaneHandling.Axes;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;

public class Plane {

    public static Group makeSlicePlane() {
        Box keepSide = makeBox(2000, 2000, 1, Axes.makePhongMaterialW(Color.GREEN));
        Box cutSide = makeBox(2000, 2000, 1, Axes.makePhongMaterialW(Color.RED));
        keepSide.setTranslateZ(keepSide.getTranslateZ() + 1);
        return new Group(keepSide, cutSide);
    }

    public static Box makeBox() {
        return makeBox(2000,2000,0.1, Axes.makePhongMaterialW(Color.GREY));
    }

    public static Box makeBox(double height, double width, double depth, PhongMaterial phongMaterial) {
        Box box = new Box(width, height, depth);
        box.setMaterial(phongMaterial);
        box.setTranslateX(0);
        box.setTranslateY(0);
        return (Box) Axes.rotateShape3D(box, new Point3D(0,0,1), 90);
//        return box;
    }


}
