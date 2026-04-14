package Project.window.Quiz;

import Project.SelectionModel.FXGroupDraw.HasFXGroupContents;
import Project.SelectionModel.FXGroupSelection.HasFXGroupSelection;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;

import java.util.List;

public abstract class SimpleSelectIn3DQuestion<S> extends SimpleQuestion<Node, List<MeshView>, S> implements UIQuestion<List<MeshView>,S>{

    private final HasFXGroupContents hasFXGroupContents;
    private final HasFXGroupSelection hasFXGroupSelection;
    private final List<MeshView> toDraw;

    private Node root;
    protected void setRoot(Node node) {this.root = node;}

    /**
     * A simple 3D question model.
     * <P>
     * ask() clears what is drawn and selected in 3D, then draws toDraw and then calls instructUser().
     * <P>
     * Implement evaluate().
     *
     * @param difficulty    difficulty of the question
     * @param minScore      minimum score
     * @param maxScore      max score
     * @param correctAnswer a list of one or more MeshViews that represent the correct answer
     * @param toDraw MeshViews that will be drawn upon calling ask()
     */
    public SimpleSelectIn3DQuestion(Difficulty difficulty, S minScore, S maxScore, List<MeshView> correctAnswer, HasFXGroupContents hasFXGroupContents, HasFXGroupSelection hasFXGroupSelection, List<MeshView> toDraw) {
        super(difficulty, minScore, maxScore, correctAnswer);
        this.hasFXGroupContents = hasFXGroupContents;
        this.hasFXGroupSelection = hasFXGroupSelection;
        hasFXGroupContents.addMeshViews(correctAnswer);
        hasFXGroupContents.addMeshViews(toDraw);
        this.toDraw = toDraw;
    }

    @Override
    public Node ask() {
        hasFXGroupSelection.clearSelection();
        hasFXGroupContents.setSelection(toDraw);
        return root;
    }

}
