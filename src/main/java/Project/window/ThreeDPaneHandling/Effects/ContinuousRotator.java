package Project.window.ThreeDPaneHandling.Effects;

import Project.window.ThreeDPaneHandling.Movement.Group3DRotation;
import javafx.animation.AnimationTimer;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point3D;
import javafx.scene.Group;


public class ContinuousRotator {

    private final AnimationTimer timer;
    private final double anglePerFrame;

    /**
     * //implements continuous rotation along a given axis at a given speed.
     * //does this by the same mechanic as mouse drag rotation, using applyGlobalRotation()
     * @param group A Group to rotate.
     * @param axis Axis
     * @param anglePerSecond Angle per second
     * @param pivot Pivot around which the rotation will occur. Must be observable so that the rotation is always correct even as the pivot changes.
     */
    public ContinuousRotator(Group group, Point3D axis, double anglePerSecond, SimpleObjectProperty<Point3D> pivot) {

        Point3D finalAxis = axis.normalize();
        this.anglePerFrame = anglePerSecond / 60.0; //smaller angle for 60 FPS -> 1x full angle per second

        this.timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                Group3DRotation.applyGlobalRotation(group, finalAxis, anglePerFrame, pivot.get());
            }
        };
    }
    public void start() {
        timer.start();
    }
    public void stop() {
        timer.stop();
    }

}