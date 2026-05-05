package Project.window.ThreeDPaneHandling.Objects;

import Project.window.Slicing.Plane;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;


public class Arrow extends Group {

    private final double length;

    public Arrow(double length, Color color) {
        this(length, length * 0.02, color);
    }

    public Arrow(double length, double radius, Color color) {

        this.length = length;

        PhongMaterial phongMaterial = new PhongMaterial(color);

        // shaft
        Cylinder shaft = new Cylinder(radius, length * 0.8);
        shaft.setMaterial(phongMaterial);

        // tip (simple cone using TriangleMesh or approximate with Cylinder scaled)
        MeshView tip = createCone(radius * 2, length * 0.2);

        tip.setMaterial(phongMaterial);

        // position tip at end of shaft
        tip.setTranslateY(-length * 0.5);

        shaft.setTranslateY(length * 0.1);

        getChildren().addAll(shaft, tip);
    }

    private MeshView createCone(double radius, double height) {
        TriangleMesh mesh = new TriangleMesh();

        int divisions = 32;

        mesh.getPoints().addAll(0, 0, 0); // tip

        for (int i = 0; i < divisions; i++) {
            double angle = 2 * Math.PI * i / divisions;
            float x = (float)(radius * Math.cos(angle));
            float z = (float)(radius * Math.sin(angle));
            mesh.getPoints().addAll(x, (float)height, z);
        }

        mesh.getTexCoords().addAll(0, 0);

        for (int i = 1; i <= divisions; i++) {
            int next = i % divisions + 1;
            mesh.getFaces().addAll(0, 0, i, 0, next, 0);
        }
        MeshView result = new MeshView(mesh);
        result.setCullFace(CullFace.NONE);
        return result;
    }


    /**
     * Points this arrow at a node. Node must be a child of group. Adds the arrow to the group.
     * @param node node that the arrow will point at.
     * @param group group that holds the node. the arrow will be added to this group.
     * @param bodyAxis plane in which the arrow will lie.
     * @param positive whether the arrow will point in positive direction of the axis or not. That means for
     *                 <ul>
     *                 <li> z-axis in BP3D: bottom -> top.</li>
     *                 <li> x-axis in BP3D: right -> left </li>
     *                 <li> y-axis in BP3D: front -> back </li>
     *                 </ul>
     */
    public void pointAt(Node node, Group group, Plane.BodyAxis bodyAxis, boolean positive, boolean dontPierce) {
        group.getChildren().remove(this);
        group.getChildren().add(this);

        Bounds nodeBounds = node.getBoundsInLocal();

        double cx = (nodeBounds.getMinX() + nodeBounds.getMaxX()) / 2;
        double cy = (nodeBounds.getMinY() + nodeBounds.getMaxY()) / 2;
        double cz = (nodeBounds.getMinZ() + nodeBounds.getMaxZ()) / 2;

        double offset = (length / 2) * (positive ? -1 : 1);

        //initially, the arrow points in negative direction along y-axis

        double angle = 0;
        Point3D axis = null;
        switch (bodyAxis) {
            case CORONAL -> {
                axis = new Point3D(1,0,0); angle = (positive ? 180 : 0);
                cy += offset;
                if (dontPierce) {
                    if (positive) cy -= nodeBounds.getCenterY()-nodeBounds.getMinY(); else cy += nodeBounds.getMaxY() - nodeBounds.getCenterY();
                }
            }
            case AXIAL -> {
                axis = new Point3D(1,0,0); angle = (positive ? 270 : 90);
                cz += offset;
                if (dontPierce) {
                    if (positive) cz -= nodeBounds.getCenterZ()-nodeBounds.getMinZ(); else cz += nodeBounds.getMaxZ() - nodeBounds.getCenterZ();
                }
            }
            case SAGGITAL -> {
                axis = new Point3D(0,0,1); angle = (positive ? 90 : 270);
                cx += offset;
                if (dontPierce) {
                    if (positive) cx -= nodeBounds.getCenterX() - nodeBounds.getMinX(); else cx += nodeBounds.getMaxX() - nodeBounds.getCenterX();
                }
            }
        }

        this.setTranslateX(cx);
        this.setTranslateY(cy);
        this.setTranslateZ(cz);

        Rotate rotate = new Rotate(angle, axis);
        this.getTransforms().add(rotate);
    }


//    public void pointAt(Node node, Group group, Point3D axis, double angle) { not functional. only works as long as arrow stays in y-axis
//        group.getChildren().remove(this);                                     because offsetting only y is only correct that case.
//        group.getChildren().add(this);                                        if the arrow does not lie perfectly in the y-axis anymore,
//                                                                              the offset needs to be computed for cy, cz and cx.
//        Bounds nodeBounds = node.getBoundsInLocal();
//
//        double cx = (nodeBounds.getMinX() + nodeBounds.getMaxX()) / 2;
//        double cy = (nodeBounds.getMinY() + nodeBounds.getMaxY()) / 2 - length / 2;
//        double cz = (nodeBounds.getMinZ() + nodeBounds.getMaxZ()) / 2;
//
//        this.setTranslateX(cx);
//        this.setTranslateY(cy);
//        this.setTranslateZ(cz);
//
//        Rotate rotate = new Rotate(angle, axis);
//        this.getTransforms().add(rotate);
//    }


}