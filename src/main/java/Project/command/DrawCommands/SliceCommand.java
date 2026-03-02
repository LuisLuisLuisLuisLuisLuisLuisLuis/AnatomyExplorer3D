package Project.command.DrawCommands;

import Project.command.Command;
import Project.window.Slicing.MeshSlicer_Aware;
import Project.window.Slicing.Plane;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.NonInvertibleTransformException;

import java.util.List;

public class SliceCommand implements Command {

    private final List<MeshView> meshViews;
    private final Box slicePlane;
    private final Group meshViewGroup;

    public SliceCommand(List<MeshView> meshViews, Group meshViewGroup, Box box) {
        this.meshViews = meshViews;
        this.slicePlane = box;
        this.meshViewGroup = meshViewGroup;
    }

    @Override
    public void undo() {
        throw new RuntimeException("Undo of SliceCommand not yet implemented");
    }

    @Override
    public void execute() {
        for (MeshView meshView : meshViews) {
            if (!(meshView.getMesh() instanceof TriangleMesh)) continue;
            double[] nxyd;
            try {
                 nxyd = Plane.extractPlaneFromBox(this.slicePlane, meshViewGroup);
            } catch (NonInvertibleTransformException ne) {
                return;
            }
            meshView.setMesh(MeshSlicer_Aware.slicePositiveSide((TriangleMesh) meshView.getMesh(), nxyd[0], nxyd[1], nxyd[2], nxyd[3]));
            meshView.setCullFace(CullFace.NONE);    // VERY IMPORTANT! because now we can see inside the mesh, i.e. we can see both sides of the faces -> both sides need to be rendered -> FRONT/BACK wont suffice
        }
    }

    @Override
    public void redo() {
        execute();
    }

    @Override
    public String name() {
        return "SliceCommand";
    }
}
