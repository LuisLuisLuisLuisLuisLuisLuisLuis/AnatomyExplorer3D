package Project.window.ThreeDPaneHandling;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

import java.util.ArrayList;

/*
Enables rotation of a Group by mouse click-and-drag on a Pane.
 */

public class SetupMouseRotate3D {
    private double xPrev;
    private double yPrev;
    private double xSave;
    private double ySave;
    Point3D axis;
    double angle;
    private RememberRotation rememberRotation = new RememberRotation();
    private final Group[] groups;

    private Runnable listener;

    private ArrayList<ContinuousRotator> continuousRotators;

    public double deltaX() {
        return xPrev - xSave;
    }
    public double deltaY() {
        return yPrev - ySave;
    }

    /**
     Pivot around which all groups rotate. It is the pivot of the first of the groups so that all rotation occurs around the same pivot.
     */
    private SimpleObjectProperty<Point3D> pivot = new SimpleObjectProperty<>(new Point3D(0,0,0));

    public SetupMouseRotate3D(Pane pane, Group[] groups) {
        this.groups = groups;
        setup(pane, groups);
    }

    //be able to notify someone of mouse rotation events
    public void setListener(Runnable listener) {
        this.listener = listener;
    }

    //remember mouse coordinates on click and stop any continuous rotation
    private void setup(Pane pane, Group[] groups) {

        groups[0].getTransforms().addListener((InvalidationListener) l -> {
            pivot.set(new Point3D(groups[0].getTransforms().getFirst().getTx(), groups[0].getTransforms().getFirst().getTy(), groups[0].getTransforms().getFirst().getTz()));
        });

        this.continuousRotators = new ArrayList<>(groups.length);
        pane.setOnMousePressed(e -> {
            xPrev = e.getSceneX();
            yPrev = e.getSceneY();
            xSave = xPrev;
            ySave = yPrev;
            this.stopContinuousRotation();
            //if (continuousRotator != null) continuousRotator.stop();
        });


        pane.setOnMouseReleased(e -> {
            //run the listener (if initialized)
            if (listener != null && deltaX() != 0 && deltaY() != 0) listener.run();

            //implement continous rotation animation if keys are pressed
            if (e.isControlDown() && e.isShiftDown()) {
                //if (this.continuousRotator != null) this.continuousRotator.stop(); // restart if running
                this.stopContinuousRotation();
                //start a new continuous rotation using the axis the user is currently rotating with
                //as speed of rotation, use the distance the user has dragged the mouse
                this.continuousRotators.clear();
                for (Group group : groups) {
                    this.continuousRotators.add(new ContinuousRotator(group, axis, Math.sqrt(Math.pow(xSave - xPrev, 2) + Math.pow(ySave - yPrev, 2)), pivot));
                    this.continuousRotators.getLast().start();
                }
//                this.continuousRotator =
//                this.continuousRotator.start();
            }
        });

        //manual rotation via mouse drag
        pane.setOnMouseDragged(e -> {
            var dx = e.getSceneX() - xPrev;
            var dy = e.getSceneY() - yPrev;
            axis = new Point3D(dy, -dx, 0).normalize();
            angle = Math.sqrt(dx * dx + dy * dy) * 0.5; // based on the distance of the mouse movement

            //so that they all rotate around the same pivot, i make them rotate around the pivot of the first group.
            //otherwise they'd rotate round their own ones which arent equal
            for (Group group : groups) Group3DRotation.applyGlobalRotation(group, axis, angle, pivot.get());//Group3DRotation.applyGlobalRotation(group, axis, angle);

            xPrev = e.getSceneX();
            yPrev = e.getSceneY();
        });
    }
    public void stopContinuousRotation() {
        for (ContinuousRotator continuousRotator : continuousRotators) continuousRotator.stop();
        //if (continuousRotator != null) continuousRotator.stop();
    }

    public static class RememberRotation {
        private int i = 0;

        public int getI() {
            return i;
        }

        public void change() {
            if (i == 0 ) i = 1; else i = 0;
        }
    }
}
