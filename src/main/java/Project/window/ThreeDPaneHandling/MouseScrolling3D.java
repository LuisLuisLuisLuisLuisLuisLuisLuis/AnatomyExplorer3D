package Project.window.ThreeDPaneHandling;

import javafx.beans.InvalidationListener;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Transform;

import java.util.HashMap;

/**
 * Sets up camera movement via mouse scrolling for a Pane and a PerspectiveCamera encapsulated by a CamMover.
 */
public class MouseScrolling3D {
    private CamMover camMover;
    private Group3DRotation groupRotator;
    private final Pane pane;
    private Runnable listener;
    private Point3D camPositionPrev;
    private Point3D camPositionPost;
    public Point3D getCamPositionPrev() {return camPositionPrev;}
    public Point3D getCamPositionPost() {return camPositionPost;}
    private int counter = 0;

    private final Group3DRotation[] groupRotators;

    /**
     * Provide a listener to be able to react to scrolling once it is finished.
     */
    public void setListener(Runnable listener) {this.listener = listener;}

    public MouseScrolling3D(Group3DRotation[] groupRotators, Pane pane) {
        this.groupRotators = groupRotators;
        this.pane = pane;

        pane.setOnScroll(event -> {
            double deltaY = event.getDeltaY();
            if (event.isControlDown()) for (Group3DRotation groupRotator : groupRotators) groupRotator.applyTranslate(new Point3D(0, deltaY, 0));
            else if (event.isAltDown()) for (Group3DRotation groupRotator : groupRotators) groupRotator.applyTranslate(new Point3D(deltaY, 0,0));
            else for (Group3DRotation groupRotator : groupRotators) groupRotator.applyTranslate(new Point3D(0, 0,-deltaY));
        });
    }

//    public MouseScrolling3D(Group3DRotation groupRotator, Pane pane) {
//        this.pane = pane;
//        this.groupRotator = groupRotator;
//
//        pane.setOnScroll(event -> {
//            double deltaY = event.getDeltaY();
//            if (event.isControlDown()) groupRotator.applyTranslate(new Point3D(0, deltaY, 0));
//            else if (event.isAltDown()) groupRotator.applyTranslate(new Point3D(deltaY, 0,0));
//            else groupRotator.applyTranslate(new Point3D(0, 0,-deltaY));
//        });
//    }
//
//    public MouseScrolling3D(CamMover camMover, Pane pane) {
//        this.camMover = camMover;
//        this.pane = pane;
//
//        pane.setOnScroll(event -> {
//            if (counter == 0) this.camPositionPrev = camMover.getCamLocation();
//
//            double deltaY = event.getDeltaY();
//            if (event.isControlDown()) camMover.moveY(- deltaY);
//            else {
//                if (event.isAltDown()) camMover.moveX(deltaY);
//                else camMover.moveZ(deltaY);
//            }
//            if (counter > 75) {
//                scrollEnd();
//                counter = 0;
//            } else {counter++;}
//        });
//    }

    /**
     * Unfortunately, the touchpads dont produce pane.setOnScrollFinished(). Thus, on those devices, scrolling can
     * not be tracked conveniently and I use this workaround to catch intervals of scrolling.
     * ScrollEnd is called after a certain amount of scrolling has happened. It records the end position of the camera
     * and runs the listener.
     */
    private void scrollEnd() {
        this.camPositionPost = camMover.getCamLocation();
        if (listener != null) listener.run();
    }
}
