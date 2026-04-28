package Project.window.Quiz;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleMultipleChoiceUIQuestion<T,S extends Comparable<S>> extends SimpleMultipleChoiceQuestion<Node,T,S> implements UIQuestion<List<T>,S>{

    private static final Logger logger = Logger.getLogger(SimpleMultipleChoiceUIQuestion.class.getName());

    private boolean forceOnlyOneAnswer;

    /**
     * the root of the UI which will be returned by ask()
     */
    protected BorderPane uiRoot;

    private final VBox feedbackBox = new VBox();

    private final HBox correctAnswerBox = new HBox();

    private boolean showCorrectAnswerOnWrongSubmit = false;

    private final SimpleBooleanProperty answeredProperty = new SimpleBooleanProperty(isAnswered);

    private final SimpleObjectProperty<List<T>> submittedAnswerProperty = new SimpleObjectProperty<>();

    private final SimpleObjectProperty<S> scoreProperty = new SimpleObjectProperty<>(minScore());

    @Override
    public SimpleBooleanProperty getAnsweredProperty() {return answeredProperty;}

    @Override
    public SimpleObjectProperty<S> getScoreProperty() {return scoreProperty;}

    @Override
    public SimpleObjectProperty<List<T>> submittedAnswerProperty() {return submittedAnswerProperty;}

    @Override
    public void showCorrectAnswerOnWrongSubmit(boolean flag) {this.showCorrectAnswerOnWrongSubmit = flag;}


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
        if (showCorrectAnswerOnWrongSubmit) feedbackBox.getChildren().add(correctAnswerBox);
        feedbackBox.setVisible(true);
    }

    protected void setupCorrectAnswerBox() {
        Label l = new Label("Correct answer: ");
        l.setStyle("-fx-font-weight: bold");
        correctAnswerBox.getChildren().add(l);
        for (int i = 0; i < correctAnswer.size(); i++) {
            T answer = correctAnswer.get(i);
            correctAnswerBox.getChildren().add(new Label(i < correctAnswer.size()-1 ? answer.toString() + ", " : answer.toString()));
        }
    }


    /**
     * A simple multiple choice question model with only one correct answer. Shows the user a multiple choice UI.
     *
     * @param difficulty      difficulty of the question
     * @param minScore        minimum score
     * @param maxScore        max score
     * @param correctAnswers   all correct answers
     * @param possibleAnswers choices for this question. it is only possible to answer with one of the choices
     */
    public SimpleMultipleChoiceUIQuestion(Difficulty difficulty, S minScore, S maxScore, List<T> correctAnswers, List<T> possibleAnswers, boolean exposesNumberOfCorrectChoices) {
        super(difficulty, minScore, maxScore, correctAnswers, possibleAnswers, exposesNumberOfCorrectChoices);
        this.forceOnlyOneAnswer = false;
        setupCorrectAnswerBox();
        this.submittedAnswerProperty.addListener((obs, old, neww) -> {
            logger.log(Level.CONFIG, "" + getScore().compareTo(maxScore()));
            if (getScore().compareTo(maxScore()) < 0) wrongFeedback();
            else correctFeedback();
        });
    }

    /**
     * A simple multiple choice question model with only one correct answer. Shows the user a multiple choice UI.
     *
     * @param difficulty      difficulty of the question
     * @param minScore        minimum score
     * @param maxScore        max score
     * @param correctAnswer   the single correct answer
     * @param possibleAnswers choices for this question. it is only possible to answer with one of the choices
     * @param forceOnlyOneAnswer do not allow the user to select multiple answers (only possible if there is only one correct answer)
     */
    public SimpleMultipleChoiceUIQuestion(Difficulty difficulty, S minScore, S maxScore, T correctAnswer, List<T> possibleAnswers, boolean forceOnlyOneAnswer) {
        this(difficulty, minScore, maxScore, List.of(correctAnswer), possibleAnswers, forceOnlyOneAnswer);
        this.forceOnlyOneAnswer = forceOnlyOneAnswer;
    }

    /**
     * Generates the multiple choice UI for this question.
     */
    private VBox makeRadioButtonUI() {
        ToggleGroup toggleGroup = new ToggleGroup();
        VBox ui = new VBox();
        ui.setSpacing(QuestionFormat.DEFAULT_CHECK_SPACING);
        for (T possibleAnswer : possibleAnswers) {
            RadioButton radioButton = new RadioButton(possibleAnswer.toString());
            radioButton.setToggleGroup(toggleGroup);
            radioButton.setUserData(possibleAnswer);
            ui.getChildren().add(radioButton);
        }

        Button submitButton = new Button("Submit");
        submitButton.setOnAction(e -> {
            for (Node node : ui.getChildren()) {
                if (node instanceof RadioButton radioButton) {
                    submit(List.of((T) radioButton.getUserData()));
                    return;
                }
            }
        });
        HBox botBox = new HBox(submitButton);
        ui.getChildren().add(botBox);
        return ui;
    }

    private VBox makeCheckBoxUI() {
        VBox uiVbox = new VBox();
        uiVbox.setSpacing(QuestionFormat.DEFAULT_CHECK_SPACING);
        for (T possibleAnswer : possibleAnswers) {
            CheckBox checkBox = new CheckBox(possibleAnswer.toString());
            checkBox.setUserData(possibleAnswer);
            uiVbox.getChildren().add(checkBox);
        }

        Button submitButton = new Button("Submit");
        submitButton.setOnAction(e -> {
            List<T> answer = new LinkedList<>();
            for (Node node : uiVbox.getChildren()) {
                if (node instanceof CheckBox checkBox) {
                    if (checkBox.isSelected()) answer.add((T) checkBox.getUserData());
                }
            }
            submit(answer);
        });
        HBox botBox = new HBox(submitButton);
        uiVbox.getChildren().add(botBox);

        return uiVbox;
    }

    private void generateUI() {
        VBox answerVBox = forceOnlyOneAnswer ? makeRadioButtonUI() : makeCheckBoxUI();
        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        HBox answerVBoxWrapper = new HBox(spacer1, answerVBox, spacer2);
        VBox ui = new VBox(answerVBoxWrapper, feedbackBox);
        this.uiRoot = QuestionFormat.stacked(getName(), getInstructions(), getDifficulty(), maxScore(), minScore(), ui);
    }

    /**
     * @return The multiple choice UI of this question. Allows the user to select and submit an answer from the list of possible answers.
     */
    @Override
    public Node ask() {
        generateUI();
        return uiRoot;
    }

    /**
     * Checks for equality of answer and correct answer, ignoring ordering.
     * @param answer answer to the question
     * @return maxScore if they are equal, else minScore.
     */
    @Override
    public S evaluate(List<T> answer) {
        logger.log(Level.CONFIG, "evaluating answer: " + answer);
        if (new HashSet<T>(answer).equals(new HashSet<T>(correctAnswer))) return maxScore();    //using sets to ignore list ordering
        else return minScore();
    }

    @Override
    public void submit(List<T> answer) {
        super.submit(answer);
        this.answeredProperty.set(super.isAnswered);
        this.submittedAnswerProperty.set(getSubmittedAnswer());
        this.scoreProperty.set(getScore());
    }

}

