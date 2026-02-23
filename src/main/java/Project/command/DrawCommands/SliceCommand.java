package Project.command.DrawCommands;

import Project.command.Command;
import Project.window.Slicing.MeshSlicer_Aware;
import javafx.scene.Node;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.List;

public class SliceCommand implements Command {

    private final List<MeshView> meshViews;

    public SliceCommand(List<MeshView> meshViews) {
        this.meshViews = meshViews;
    }

    @Override
    public void undo() {
        throw new RuntimeException("Undo of SliceCommand not yet implemented");
    }

    @Override
    public void execute() {
        for (MeshView meshView : meshViews) {
            if (!(meshView.getMesh() instanceof TriangleMesh)) continue;
            meshView.setMesh(MeshSlicer_Aware.slicePositiveSide((TriangleMesh) meshView.getMesh(), 0,0,1,1000));
            meshView.setCullFace(CullFace.NONE);    // VERY IMPORTANT! because now we can see inside the mesh, i.e. we can see the other side of the faces too -> both sides need to be rendered -> FRONT/BACK wont suffice
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
