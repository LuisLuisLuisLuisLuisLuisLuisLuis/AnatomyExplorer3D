package Project.window.Quiz.Question;

import Project.command.DrawCommands.ColorMeshviewsCommand;
import Project.window.Quiz.Difficulty;
import Project.window.Slicing.Plane;
import Project.window.ThreeDPaneHandling.Effects.SelectionEffect;
import Project.window.ThreeDPaneHandling.Objects.Arrow;
import Project.window.ThreeDPaneHandling.ThreeDGroupHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static Project.window.Quiz.Question.SimpleSliceMCQuestion.getOrthogonalPlanes;

public class SimpleMultipleChoice3DQuestion<T,S extends Comparable<S>> extends SimpleMultipleChoiceUIQuestion<T,S> implements Needs3DAccess {

    protected ThreeDGroupHandler threeDGroupHandler = null;

    protected final List<String> idsToDraw;
    protected final List<String> idsToSelect;

    protected List<String> nonExist;    // ids which the ThreeDGroupHandler doesn't know about (either draw or select).

    /** List of directories in resources. Will be accessed via getClass().getResource(...) */
    private List<String> resourceLocations = List.of();

    private List<File> fileDirs = List.of();

    /** Provide a list of directories in resources. Will be accessed via getClass().getResource(...) */
    public void setResourceLocations(List<String> resourceDirs) {
        this.resourceLocations = resourceDirs;
    }

    public void setFileDirs(List<File> dirs) {
        this.fileDirs = dirs;
    }

    /**
     * A simple multiple choice question model with only one correct answer. Draws some stuff in 3D and asks via multiple choice.
     * Only one correct answer.
     *
     * @param difficulty      difficulty of the question
     * @param minScore        minimum score
     * @param maxScore        max score
     * @param correctAnswer   correct answer
     * @param possibleAnswers choices this question. it is only possible to answer with one of the choices
     * @param idsToDraw MeshView IDs that will be drawn upon asking the question. In the format of filenames of OBJ files.
     * @param idsToSelect MeshViews that will be selected upon asking the question. In the format of filenames of OBJ files.
     */
    public SimpleMultipleChoice3DQuestion(Difficulty difficulty, S minScore, S maxScore, T correctAnswer, List<T> possibleAnswers, List<String> idsToDraw, List<String> idsToSelect) {
        super(difficulty, minScore, maxScore, correctAnswer, possibleAnswers, false);

        this.idsToDraw = idsToDraw;
        this.idsToSelect = idsToSelect;
    }

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
     */
    public SimpleMultipleChoice3DQuestion(Difficulty difficulty, S minScore, S maxScore, T correctAnswer, List<T> possibleAnswers, List<String> idsToDraw, List<String> idsToSelect, boolean forceOnlyOneAnsewr) {
        super(difficulty, minScore, maxScore, correctAnswer, possibleAnswers, forceOnlyOneAnsewr);

        this.idsToDraw = idsToDraw;
        this.idsToSelect = idsToSelect;
    }

    protected void checkIDs() {
        nonExist = new LinkedList<>();
        for (String id : idsToDraw) {
            MeshView m = threeDGroupHandler.getHasFXGroupContents().getMeshViewWithID(id);
            if (m == null) nonExist.add(id);
        }
        for (String id : idsToSelect) {
            MeshView m = threeDGroupHandler.getHasFXGroupContents().getMeshViewWithID(id);
            if (m == null) nonExist.add(id);
        }
    }

    /**
     * Checks if this question is able to draw and select everything it needs. Then draws and selects everything.
     */
    @Override
    public Node ask() {
        checkIDs();
        if (!nonExist.isEmpty()) {  // if some ids cannot be resolved, question cannot be asked. full points are granted
            Label l = new Label("Failed to draw or select the following MeshViews:\n" + nonExist + "\nQuestion cannot be asked.");
            l.setWrapText(true);
            if (this.formattedQuestionUI != null) this.formattedQuestionUI.getChildren().clear();
            else this.formattedQuestionUI = new BorderPane();
            this.formattedQuestionUI.setTop(l);
            submit(correctAnswer);
            // at this point, there is no submit button anymore -> user cannot work on this question.
            return formattedQuestionUI;
        }
//        this.threeDGroupHandler.softViewReset();
        this.threeDGroupHandler.hardViewReset();
        this.threeDGroupHandler.getHasFXGroupContents().setSelection(idsToDraw.stream().map(id -> threeDGroupHandler.getHasFXGroupContents().getMeshViewWithID(id)).toList());
        this.threeDGroupHandler.defaultMeshes();
        this.threeDGroupHandler.getHasFXGroupSelection().setSelection(idsToSelect.stream().map(id -> threeDGroupHandler.getHasFXGroupContents().getMeshViewWithID(id)).toList());
        this.threeDGroupHandler.resetView();
        this.threeDGroupHandler.setClickSelectable(false);
        Node result = super.ask();
        Button resetViewButton = new Button("Reset View");
        resetViewButton.setOnAction(e -> threeDGroupHandler.resetView());
        Button showArrowButton = new Button("Show Arrow");
        Button hideArrowButton = new Button("Remove arrow");
        hideArrowButton.setDisable(true);
        Button changeSelEffectButton = new Button("Change selection effect");
        showArrowButton.setOnAction(e -> {
            new ColorMeshviewsCommand(arrowColor,new HashSet<>(idsToSelect), threeDGroupHandler.getHasFXGroupContents(), threeDGroupHandler.getHasFXGroupSelection()).execute();
            changeArrow();
            showArrowButton.setText("Change Arrow");
            hideArrowButton.setDisable(false);
        });
        hideArrowButton.setOnAction(e -> {
            threeDGroupHandler.removeArrow(arrow);
            threeDGroupHandler.resetColors();
            showArrowButton.setText("Show Arrow");
            hideArrowButton.setDisable(true);
        });
        changeSelEffectButton.setOnAction(e -> threeDGroupHandler.changeSelectionEffect(threeDGroupHandler.getSelectionEffect() == SelectionEffect.Effect.VIA_DRAWMODE ? SelectionEffect.Effect.VIA_SATURATION : SelectionEffect.Effect.VIA_DRAWMODE));

        formattedQuestionUI.setBottom(new ToolBar(resetViewButton, showArrowButton, hideArrowButton, changeSelEffectButton));

        return result;
    }

    /** between 0 and 5 incl.*/
    protected int arrowConfigCount = 0;
    protected boolean arrowAxisPositive = true;
    protected int file = 0;
    protected int arrowTargetInd = 0;

    protected void changeArrow() {
        String arrowTargetID = idsToSelect.get(arrowTargetInd);
        threeDGroupHandler.removeArrow(arrow);
        boolean dontPierce = !(arrowConfigCount == 0);
        int arrowPlaneInd = arrowConfigCount%2;
        Plane.BodyAxis arrowPlane = getOrthogonalPlanes(Plane.BodyAxis.CORONAL)[arrowPlaneInd];

        arrow = threeDGroupHandler.pointArrowAt(threeDGroupHandler.getHasFXGroupContents().getMeshViewWithID(arrowTargetID), arrowPlane, arrowAxisPositive, dontPierce, arrowColor);

        if (arrowPlaneInd==0) arrowAxisPositive = !arrowAxisPositive;

        if (arrowConfigCount == 5) {
            arrowConfigCount = 0;
            if (idsToSelect.size() -1 > arrowTargetInd) arrowTargetInd++; else arrowTargetInd = 0;
        } else arrowConfigCount++;

    }

    protected Arrow arrow = null;
    protected Color arrowColor = Color.RED;

    @Override
    public void giveAccess(@NotNull ThreeDGroupHandler threeDGroupHandler) {
        this.threeDGroupHandler = threeDGroupHandler;
        HashSet<String> allFileIDs = new HashSet<>((int) ((idsToSelect.size() + idsToDraw.size()) /0.75));
        allFileIDs.addAll(idsToSelect);
        allFileIDs.addAll(idsToDraw);

        for (String dir : this.resourceLocations) {
            this.threeDGroupHandler.getHasFXGroupContents().addResourceLocation(dir, allFileIDs);
        }
        for (File dir : this.fileDirs) {
            this.threeDGroupHandler.getHasFXGroupContents().addFileDir(dir, allFileIDs); //TODO
        }
    }

}
