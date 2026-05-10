package Project.window.Quiz.Question;

import Project.command.DrawCommands.ColorMeshviewsCommand;
import Project.window.PopUp.LegendItem;
import Project.window.Quiz.Difficulty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleSelectIn3DUIQuestion<S extends Comparable<S>> extends SimpleSelectIn3DQuestion<S> implements UIQuestion<List<String>,S> {

    private static final Logger logger = Logger.getLogger(SimpleSelectIn3DUIQuestion.class.getName());

    private final SimpleBooleanProperty answeredProperty = new SimpleBooleanProperty(false);

    private final boolean showHintOnWrongAnswer;

    private final SimpleObjectProperty<S> scoreProperty = new SimpleObjectProperty<>(minScore());

    private final SimpleObjectProperty<List<String>> submittedAnswerProperty = new SimpleObjectProperty<>(List.of());

    private final HashMap<String, Color> colorMapping = new HashMap<>();

    private BorderPane uiRoot = new BorderPane();

    private final HBox feedbackBox = new HBox();

    protected void correctFeedback() {
        Label l = new Label("Correct");
        l.getStyleClass().add("text-correct");
        feedbackBox.getChildren().clear();
        feedbackBox.getChildren().add(l);
        feedbackBox.setVisible(true);
    }
    protected void wrongFeedback() {
        Label l = new Label("Wrong");
        l.getStyleClass().add("text-wrong");
        feedbackBox.getChildren().clear();
        feedbackBox.getChildren().add(l);
        if (showHintOnWrongAnswer) {
            Color userCorrectColor = Color.web("#0b8728");
            Color userWrongColor = Color.web("crimson");
            Color missingCorrectColor = Color.web("#0acf38");
            Color otherColor = Color.LIGHTGREY;
            LegendItem yourCorrect = new LegendItem(userCorrectColor, "Your correct pick");
            LegendItem missingPick = new LegendItem(missingCorrectColor, "Missing correct pick");
            LegendItem yourWrong = new LegendItem(userWrongColor, "Your wrong pick");
            VBox colorLegend = new VBox(yourCorrect, missingPick, yourWrong);
            feedbackBox.getChildren().addAll(new Separator(Orientation.VERTICAL), colorLegend);
            // give correct answer via coloring. also save that coloring to the map so that it may be re-applied when the question is reloaded.
            for (String id : getSubmittedAnswer()) {
                if (correctAnswer.contains(id)) {
                    new ColorMeshviewsCommand(userCorrectColor, Collections.singleton(id), threeDGroupHandler.getHasFXGroupContents(), threeDGroupHandler.getHasFXGroupSelection()).execute();
                    this.colorMapping.put(id, userCorrectColor);
                }
                else {
                    new ColorMeshviewsCommand(userWrongColor, Collections.singleton(id), threeDGroupHandler.getHasFXGroupContents(), threeDGroupHandler.getHasFXGroupSelection()).execute();
                    this.colorMapping.put(id, userWrongColor);
                }
            }
            for (String id : correctAnswer) {
                if (getSubmittedAnswer().contains(id)) continue;
                new ColorMeshviewsCommand(missingCorrectColor, Collections.singleton(id), threeDGroupHandler.getHasFXGroupContents(), threeDGroupHandler.getHasFXGroupSelection()).execute();
                this.colorMapping.put(id, missingCorrectColor);
            }
            for (MeshView meshView : threeDGroupHandler.getHasFXGroupContents().getSelection()) {
                if (!getSubmittedAnswer().contains(meshView.getId()) && !correctAnswer.contains(meshView.getId())) {
                    new ColorMeshviewsCommand(otherColor, Set.of(meshView.getId()), threeDGroupHandler.getHasFXGroupContents(), threeDGroupHandler.getHasFXGroupSelection()).execute();
                    this.colorMapping.put(meshView.getId(), otherColor);
                }
            }
        }
        feedbackBox.setVisible(true);
        threeDGroupHandler.getHasFXGroupSelection().clearSelection();
    }


    /**
     * A simple 3D question model.
     * <p>
     * Implement evaluate().
     *
     * @param difficulty        difficulty of the question
     * @param minScore          minimum score
     * @param maxScore          max score
     * @param correctAnswer     a list of one or more IDs of MeshViews that represent the correct answer
     * @param idsToDraw Meshview IDs that will be drawn upon asking the question. In the format of filenames of OBJ files.
     */
    public SimpleSelectIn3DUIQuestion(Difficulty difficulty, S minScore, S maxScore, List<String> correctAnswer, List<String> idsToDraw, boolean showHintOnWrongAnswer) {
        super(difficulty, minScore, maxScore, correctAnswer, idsToDraw);
        this.showHintOnWrongAnswer = showHintOnWrongAnswer;
    }


    protected void generateUI() {
        this.uiRoot.getChildren().clear();
        Button submitButton = new Button("Submit");
        submitButton.setOnAction(e -> submit(this.threeDGroupHandler.getHasFXGroupSelection().getSelection().stream().map(MeshView::getId).toList()));
        VBox answerVBox = new VBox(submitButton);
        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        HBox answerVBoxWrapper = new HBox(spacer1, answerVBox, spacer2);
        VBox ui = new VBox(answerVBoxWrapper, feedbackBox);
        this.uiRoot = QuestionFormat.stacked(getName(), getInstructions(), getDifficulty(), maxScore(), minScore(), ui);
    }

    @Override
    public SimpleBooleanProperty getAnsweredProperty() {
        return this.answeredProperty;
    }

    @Override
    public SimpleObjectProperty<S> getScoreProperty() {
        return this.scoreProperty;
    }

    @Override
    public SimpleObjectProperty<List<String>> submittedAnswerProperty() {
        return this.submittedAnswerProperty;
    }

    @Override
    public void showCorrectAnswerOnWrongSubmit(boolean flag) {

    }

    @Override
    public Node ask() {
        List<MeshView> toDraw = new ArrayList<>(idsToDraw.size());
        List<String> nonExisting = new LinkedList<>();
        for (String id : idsToDraw) {
            MeshView meshView = this.threeDGroupHandler.getHasFXGroupContents().getMeshViewWithID(id);
            if (meshView == null) nonExisting.add(id);
            else toDraw.add(meshView);
        }
        if (!nonExisting.isEmpty()) {
            if (this.uiRoot != null) this.uiRoot.getChildren().clear();
            else this.uiRoot = new BorderPane();
            this.uiRoot.setTop(new Label("Failed to draw the following nodes:\n" + nonExisting + "\nQuestion cannot be asked."));
            submit(correctAnswer);
            // at this point, there is no submit button anymore -> user cannot work on this question.
        }
        this.threeDGroupHandler.hardViewReset();
        this.threeDGroupHandler.getHasFXGroupSelection().clearSelection();
        this.threeDGroupHandler.getHasFXGroupContents().setSelection(toDraw);
        this.threeDGroupHandler.defaultMeshes();
        this.threeDGroupHandler.setClickSelectable(true);
        for (String id : this.colorMapping.keySet()) {
            logger.log(Level.CONFIG, "trying to re-color " + id + " " + this.colorMapping.get(id));
            new ColorMeshviewsCommand(this.colorMapping.get(id), Set.of(id), threeDGroupHandler.getHasFXGroupContents(), threeDGroupHandler.getHasFXGroupSelection()).execute();
        }
        generateUI();
        Button resetViewButton = new Button("Reset View and Colors");
        resetViewButton.setOnAction(e -> {
            threeDGroupHandler.resetView();
            threeDGroupHandler.resetColors();
        });
        uiRoot.setBottom(new ToolBar(resetViewButton));
        return this.uiRoot;
    }

    @Override
    public void submit(List<String> answer) {
        super.submit(answer);
        this.submittedAnswerProperty.set(getSubmittedAnswer());
        this.answeredProperty.set(super.isAnswered);
        this.scoreProperty.set(getScore());
        if (getScore().equals(maxScore())) correctFeedback();
        else wrongFeedback();   // TODO: partially correct answer?
    }

    @Override
    public S evaluate(List<String> answer) {
        HashSet<String> answerSet = new HashSet<>(answer);
        HashSet<String> correctAnswerSet = new HashSet<>(this.correctAnswer);
        if (answerSet.size() == correctAnswerSet.size() && answerSet.containsAll(correctAnswerSet)) return maxScore();
        else return minScore();
    }
}
