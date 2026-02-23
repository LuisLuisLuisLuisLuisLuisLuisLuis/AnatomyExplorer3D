package Project.window.ThreeDPaneHandling;

import javafx.animation.AnimationTimer;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.transform.Rotate;

//implements continuous rotation along a given axis at a given speed.
//does this by the same mechanic as mouse drag rotation, using applyGlobalRotation()
public class ContinuousRotator {

    private final AnimationTimer timer;
    private final double anglePerFrame;

    public ContinuousRotator(Group group, Point3D axis, double anglePerSecond) {

        Point3D finalAxis = axis.normalize();
        this.anglePerFrame = anglePerSecond / 60.0; //smaller angle for 60 FPS -> 1x full angle per second

        this.timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                Group3DRotation.applyGlobalRotation(group, finalAxis, anglePerFrame);
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