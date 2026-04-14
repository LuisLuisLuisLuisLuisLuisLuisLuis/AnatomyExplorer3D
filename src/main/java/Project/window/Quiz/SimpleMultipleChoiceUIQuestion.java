package Project.window.Quiz;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleMultipleChoiceUIQuestion<T,S> extends SimpleMultipleChoiceQuestion<Node,T,S> implements UIQuestion<List<T>,S>{

    private static final Logger logger = Logger.getLogger(SimpleMultipleChoiceUIQuestion.class.getName());

    private boolean forceOnlyOneAnswer;

    /**
     * the root of the UI
     */
    private VBox root;

    private final SimpleBooleanProperty answeredProperty = new SimpleBooleanProperty(isAnswered);

    @Override
    public SimpleBooleanProperty getAnsweredProperty() {return answeredProperty;}

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
    private void makeRadioButtonUI() {
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

        this.root = QuestionFormat.stacked(getName(), getInstructions(), getDifficulty(), maxScore(), minScore(), ui);
    }

    private void makeCheckBoxUI() {
        VBox ui = new VBox();
        ui.setSpacing(QuestionFormat.DEFAULT_CHECK_SPACING);
        for (T possibleAnswer : possibleAnswers) {
            CheckBox checkBox = new CheckBox(possibleAnswer.toString());
            checkBox.setUserData(possibleAnswer);
            ui.getChildren().add(checkBox);
        }

        Button submitButton = new Button("Submit");
        submitButton.setOnAction(e -> {
            List<T> answer = new LinkedList<>();
            for (Node node : ui.getChildren()) {
                if (node instanceof CheckBox checkBox) {
                    if (checkBox.isSelected()) answer.add((T) checkBox.getUserData());
                }
            }
            submit(answer);
        });
        HBox botBox = new HBox(submitButton);
        ui.getChildren().add(botBox);

        this.root = QuestionFormat.stacked(getName(), getInstructions(), getDifficulty(), maxScore(), minScore(), ui);
    }

    private void generateUI() {
        if (forceOnlyOneAnswer) makeRadioButtonUI();
        else makeCheckBoxUI();
    }

    /**
     * @return the multiple choice UI of this question. Allows the user to submit answer from the list of possible answers.
     */
    @Override
    public Node ask() {
        generateUI();
        return root;
    }

    @Override
    public S evaluate(List<T> answer) {
        logger.log(Level.CONFIG, "evaluating answer: " + answer);
        if (answer.equals(correctAnswer)) return maxScore();
        else return minScore();
    }

    @Override
    public void submit(List<T> answer) {
        super.submit(answer);
        this.answeredProperty.set(super.isAnswered);
    }

}
