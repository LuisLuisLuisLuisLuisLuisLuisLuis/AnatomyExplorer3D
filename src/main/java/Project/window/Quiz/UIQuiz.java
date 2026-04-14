package Project.window.Quiz;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

public class UIQuiz<S> implements Quiz<S>{

    private SimpleObjectProperty<S> scoreProperty = new SimpleObjectProperty<>(getScore());

    //don't give to constructor but only set via jackson when creating from save file
    private S pb = null;

    private String name = null;
    private String description = null;
    public void setName(String name) {this.name = name;}
    public void setDescription(String description) {this.description = description;}

    private final BooleanProperty runningProperty = new SimpleBooleanProperty(false);

    /**
     * @return the runningProperty. knows if this quiz is running.
     */
    public BooleanProperty runningPropertyProperty() {return runningProperty;}

    private final BooleanProperty finishedProperty = new SimpleBooleanProperty(false);

    /**
     * @return the finishedProperty. is true once the quiz has ended
     */
    public BooleanProperty finishedProperty() {return finishedProperty;}

    private final List<UIQuestion<?, S>> questions;
    @Override
    public List<UIQuestion<?, S>> getQuestions() {return new LinkedList<>(this.questions);}

    private final ListIterator<UIQuestion<?, S>> questionIterator;
    private UIQuestion<?, S> currentQuestion;
    
    private final boolean showScore;
    private final boolean showCorrect;
    private final int allowedTime;
    private final TimeUnit timeUnit;

    /**
     * 
     * @param questions the questions for this quiz
     * @param showScore whether to display the currents score (and highscore) during the quiz
     * @param showCorrect whether to inform the user if their answer was correct or not and if not to show them the correct solution
     * @param allowedTime time after which the quiz will automatically end
     * @param timeUnit time unit of the given allowed time
     */
    public UIQuiz(List<UIQuestion<?, S>> questions, boolean showScore, boolean showCorrect, int allowedTime, TimeUnit timeUnit) {
        this.questions = questions;
        this.showScore = showScore;
        this.showCorrect = showCorrect;
        this.allowedTime = allowedTime;
        this.timeUnit = timeUnit;
        this.questionIterator = this.questions.listIterator();
    }

    public UIQuiz(List<UIQuestion<?, S>> questions, boolean showScore, boolean showCorrect) {
        this(questions, showScore, showCorrect, -1, TimeUnit.MINUTES);
    }

    private Node makeUI() throws IOException{
        UIQuizView uiQuizView = new UIQuizView();
        UIQuizController controller = uiQuizView.getController();
        if (showScore) {
            ToolBar scoreToolbar = controller.getScoreToolBar();
            Label scoreLabel = new Label("Score: " + getScore().toString());
            Label pbLabel = new Label("PB: " + pb.toString());
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            scoreToolbar.getItems().addAll(scoreLabel, spacer, pbLabel);
            scoreToolbar.setVisible(true);
        } else {
            controller.getScoreToolBar().setVisible(false);
        }
        return new VBox();
    }
    

    @Override
    public void start() {
        runningProperty.set(true);
    }

    @Override
    public void stop() {
        finishedProperty.set(true);
    }

    @Override
    public double getProgress() {
        return 0;
    }

    @Override
    public S getScore() {
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public S getPB() {
        return this.pb;
    }
    
    private static class UIQuizView {
        private final UIQuizController controller;
        private final Parent root;

        public UIQuizView() throws IOException {
            URL fxmlUrl = getClass().getResource("/Project/FXML/UIQuiz.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            root = loader.load();
            controller = loader.getController();
        }

        public UIQuizController getController() {return controller; }
        public Parent getRoot() { return root; }
    }

    @Override
    public boolean canNext() {
        return questionIterator.hasNext();
    }

    @Override
    public boolean canPrevious() {
        return questionIterator.hasPrevious();
    }

    @Override
    public UIQuestion<?, S> next() {
        if (questionIterator.hasNext()){
            currentQuestion = questionIterator.next();
            return currentQuestion;
        } else return null;
    }

    @Override
    public UIQuestion<?, S> previous() {
        if (questionIterator.hasPrevious()) {
            currentQuestion = questionIterator.previous();
            return currentQuestion;
        } else return null;
    }
}
