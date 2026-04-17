package Project.window.Quiz;

import Project.SelectionModel.FXGroupDraw.HasFXGroupContents;
import Project.SelectionModel.FXGroupSelection.HasFXGroupSelection;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;

import java.util.List;

public class SimpleMultipleChoice3DQuestion<T,S extends Comparable<S>> extends SimpleMultipleChoiceUIQuestion<T,S>{

    private final HasFXGroupContents hasFXGroupContents;
    private final HasFXGroupSelection hasFXGroupSelection;

    private final List<MeshView> toDraw;
    private final List<MeshView> toSelect;


    /**
     * A simple multiple choice question model with only one correct answer. Draws some stuff in 3D and asks via mulitple choice.
     * Only one correct answer.
     *
     * @param difficulty      difficulty of the question
     * @param minScore        minimum score
     * @param maxScore        max score
     * @param correctAnswer   correct answer
     * @param possibleAnswers choices for this question. it is only possible to answer with one of the choices
     * @param hasFXGroupContents A HasFXGRoupContents that can draw stuff
     * @param hasFXGroupSelection a HasFXGroupSelection that can select stuff drawn by HasFXGroupContents
     * @param toDraw Meshviews that will be drawn upon asking the question
     * @param toSelect Meshviews that will be selected upon asking the question
     */
    public SimpleMultipleChoice3DQuestion(Difficulty difficulty, S minScore, S maxScore, T correctAnswer, List<T> possibleAnswers, List<MeshView> toDraw, List<MeshView> toSelect, HasFXGroupContents hasFXGroupContents, HasFXGroupSelection hasFXGroupSelection) {
        super(difficulty, minScore, maxScore, correctAnswer, possibleAnswers, false);
        this.hasFXGroupContents = hasFXGroupContents;
        this.hasFXGroupSelection = hasFXGroupSelection;
        this.hasFXGroupContents.addMeshViews(toDraw);
        this.toDraw = toDraw;
        this.toSelect = toSelect;
    }

    @Override
    public Node ask() {
        hasFXGroupContents.setSelection(toDraw);
        hasFXGroupSelection.setSelection(toSelect);
        return super.ask();
    }
}
