package Project.window.ThreeDPaneHandling;

import Project.window.WindowPresenter;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

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

    private Runnable listener;

    private ContinuousRotator continuousRotator;

    public double deltaX() {
        return xPrev - xSave;
    }
    public double deltaY() {
        return yPrev - ySave;
    }

    public SetupMouseRotate3D(Pane pane, Group contentGroup, Group innerGroup) {
        setup(pane, contentGroup);
    }

    //be able to notify someone of mouse rotation events
    public void setListener(Runnable listener) {
        this.listener = listener;
    }

    //remember mouse coordinates on click and stop any continuous rotation
    private void setup(Pane pane, Group group1) {
        pane.setOnMousePressed(e -> {
            xPrev = e.getSceneX();
            yPrev = e.getSceneY();
            xSave = xPrev;
            ySave = yPrev;

            if (continuousRotator != null) continuousRotator.stop();
        });


        pane.setOnMouseReleased(e -> {
            //run the listener (if initialized)
            if (listener != null && deltaX() != 0 && deltaY() != 0) listener.run();

            //implement continous rotation animation if keys are pressed
            if (e.isControlDown() && e.isShiftDown()) {
                if (this.continuousRotator != null) this.continuousRotator.stop(); // restart if running
                //start a new continuous rotation using the axis the user is currently rotating with
                //as speed of rotation, use the distance the user has dragged the mouse
                this.continuousRotator = new ContinuousRotator(group1, axis, Math.sqrt(Math.pow(xSave - xPrev, 2) + Math.pow(ySave - yPrev, 2))); // 30° per second
                this.continuousRotator.start();
            }
        });

        //manual rotation via mouse drag
        pane.setOnMouseDragged(e -> {
            var dx = e.getSceneX() - xPrev;
            var dy = e.getSceneY() - yPrev;
            axis = new Point3D(dy, -dx, 0).normalize();
            angle = Math.sqrt(dx * dx + dy * dy) * 0.5; // based on the distance of the mouse movement

            applyGlobalRotation(group1, axis, angle);

            xPrev = e.getSceneX();
            yPrev = e.getSceneY();
        });
    }
    public void stopContinuousRotation() {
        if (continuousRotator != null) continuousRotator.stop();
    }

    public void applyGlobalRotation(Group group, Point3D axis, double angle) {
        var currentTransform = (group.getTransforms().size() == 0)? new Translate() : group.getTransforms().getFirst();
        var rotate = new Rotate(angle, axis);
        currentTransform = rotate.createConcatenation(currentTransform);
        group.getTransforms().setAll(currentTransform);
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
