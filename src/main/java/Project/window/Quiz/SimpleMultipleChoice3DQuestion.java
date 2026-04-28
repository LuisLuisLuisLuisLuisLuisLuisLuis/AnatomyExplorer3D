package Project.window.Quiz;

import Project.window.ThreeDPaneHandling.ThreeDGroupHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.MeshView;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class SimpleMultipleChoice3DQuestion<T,S extends Comparable<S>> extends SimpleMultipleChoiceUIQuestion<T,S> implements Needs3DAccess {

    private ThreeDGroupHandler threeDGroupHandler = null;

    private final List<String> idsToDraw;
    private final List<String> idsToSelect;

    private final List<URI> resourceLocations;

    /**
     * A simple multiple choice question model with only one correct answer. Draws some stuff in 3D and asks via multiple choice.
     * Only one correct answer.
     *
     * @param difficulty      difficulty of the question
     * @param minScore        minimum score
     * @param maxScore        max score
     * @param correctAnswer   correct answer
     * @param possibleAnswers choices this question. it is only possible to answer with one of the choices
     * @param idsToDraw Meshview IDs that will be drawn upon asking the question. In the format of filenames of OBJ files.
     * @param idsToSelect Meshviews that will be selected upon asking the question. In the format of filenames of OBJ files.
     * @param resourceLocations where to find the OBJ files specified by idsToDraw and idsToSelect
     */
    public SimpleMultipleChoice3DQuestion(Difficulty difficulty, S minScore, S maxScore, T correctAnswer, List<T> possibleAnswers, List<String> idsToDraw, List<String> idsToSelect, List<URI> resourceLocations) {
        super(difficulty, minScore, maxScore, correctAnswer, possibleAnswers, false);
        this.idsToDraw = idsToDraw;
        this.idsToSelect = idsToSelect;
        this.resourceLocations = resourceLocations;
    }

    @Override
    public Node ask() {
        if (this.threeDGroupHandler == null) {
            Label l = new Label("Access to 3D Pane not provided. Cannot ask question.");
            l.setWrapText(true);
            this.uiRoot.getChildren().clear();
            this.uiRoot.setTop(l);
            submit(correctAnswer);
            // at this point, there is no submit button anymore -> user cannot work on this question.
            return uiRoot;
        }
        List<MeshView> toDraw = new LinkedList<>();
        List<MeshView> toSelect = new LinkedList<>();
        List<String> nonExist = new LinkedList<>();
        for (String id : idsToDraw) {
            MeshView m = threeDGroupHandler.getHasFXGroupContents().getMeshViewWithID(id);
            if (m == null) nonExist.add(id);
            else toDraw.add(m);
        }
        for (String id : idsToSelect) {
            MeshView m = threeDGroupHandler.getHasFXGroupContents().getMeshViewWithID(id);
            if (m == null) nonExist.add(id);
            else toSelect.add(m);
        }
        if (!nonExist.isEmpty()) {  // if some ids cannot be resolved, question cannot be asked. full points are granted
            Label l = new Label("Failed to draw or select the following MeshViews:\n" + nonExist + "\nQuestion cannot be asked.");
            l.setWrapText(true);
            if (this.uiRoot != null) this.uiRoot.getChildren().clear();
            else this.uiRoot = new BorderPane();
            this.uiRoot.setTop(l);
            submit(correctAnswer);
            // at this point, there is no submit button anymore -> user cannot work on this question.
            return uiRoot;
        }
        this.threeDGroupHandler.resetModifiables();
        this.threeDGroupHandler.getHasFXGroupContents().setSelection(toDraw);
        this.threeDGroupHandler.getHasFXGroupSelection().setSelection(toSelect);
        this.threeDGroupHandler.resetView();
        this.threeDGroupHandler.setClickSelectable(false);
        return super.ask();
    }

    @Override
    public void giveAccess(ThreeDGroupHandler threeDGroupHandler) {
        this.threeDGroupHandler = threeDGroupHandler;
        HashSet<String> allFileIDs = new HashSet<>();
        for (String id : idsToDraw) if (this.threeDGroupHandler.getHasFXGroupContents().getMeshViewWithID(id) == null) allFileIDs.add(id);
        for (String id : idsToSelect) if (this.threeDGroupHandler.getHasFXGroupContents().getMeshViewWithID(id) == null) allFileIDs.add(id);
        for (URI uri : this.resourceLocations) {
            try {
                this.threeDGroupHandler.getHasFXGroupContentsContainer().addResourceLocation(uri.toURL(), allFileIDs);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Failed to load resource location " + uri.toString());  //TODO: must be caught by Quiz
            }
        }
    }

}
