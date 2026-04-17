package Project.window.Quiz;

import Project.window.PopUp.LittlePopUp;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class UIQuiz<S extends Comparable<S>> implements Quiz<S>{

    private static final Logger logger = Logger.getLogger(UIQuiz.class.getName());

    /**
     * @return the finishedProperty. is true once the quiz has ended
     */
    public BooleanProperty finishedProperty() {return finishedProperty;}

    /**
     * @return the questions of this quiz
     */
    public List<UIQuestion<?, S>> getQuestions() {return new LinkedList<>(this.questions);}

    /**
     * @return the runningProperty. knows if this quiz is running.
     */
    public BooleanProperty runningPropertyProperty() {return runningProperty;}

    /**
     * @param name set the name of this quiz
     */
    public void setName(String name) {this.name = name;}

    /**
     * @param description give a description of this quiz
     */
    public void setDescription(String description) {this.description = description;}


    private final SimpleObjectProperty<S> scoreProperty;
    /**
     * will hold the UI of the questions. initialized with mock value to avoid any possible null pointer exceptions.
     */
    protected AnchorPane questUIAnchorPane = new AnchorPane();
    protected void setQuestUIAnchorPaneChild(Node child) {
        AnchorPane.setTopAnchor(child, 0.0);
        AnchorPane.setBottomAnchor(child, 0.0);
        AnchorPane.setLeftAnchor(child, 0.0);
        AnchorPane.setRightAnchor(child, 0.0);
        questUIAnchorPane.getChildren().clear();
        questUIAnchorPane.getChildren().add(child);
    }

    //don't give to constructor but only set (via jackson) when creating from save file
    private S pb = null;

    private String name = null;

    private String description = null;

    private final BooleanProperty runningProperty = new SimpleBooleanProperty(false);

    private final BooleanProperty finishedProperty = new SimpleBooleanProperty(false);

    private double progress = 0;

    private Timeline timeline;

    private final ArrayList<UIQuestion<?, S>> questions;

    protected final List<SimpleObjectProperty<S>> scores;

    private int currentQuestIndex = -1;

    private SimpleObjectProperty<UIQuestion<?, S>> currentQuestion = new SimpleObjectProperty<>();

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
     * @param timeUnit time unit of the given allowed time. Must be one of {HOURS, MINUTES, SECONDS}.
     */
    public UIQuiz(List<UIQuestion<?, S>> questions, boolean showScore, boolean showCorrect, int allowedTime, TimeUnit timeUnit) {
        if (timeUnit == TimeUnit.DAYS || timeUnit == TimeUnit.MICROSECONDS || timeUnit == TimeUnit.MILLISECONDS) throw new IllegalArgumentException("Timeunit must be one of {DAYS, HOURS, MINUTES, SECONDS}.");
        this.questions = new ArrayList<>(questions);
        this.showScore = showScore;
        this.showCorrect = showCorrect;
        this.allowedTime = allowedTime;
        this.timeUnit = timeUnit;
        this.scores = questions.stream().map(UIQuestion::getScoreProperty).toList();
        this.scoreProperty = new SimpleObjectProperty<>(computeScore(this.scores.stream().map(SimpleObjectProperty::get).toList()));    // compute initial score from default scores
    }

    public UIQuiz(List<UIQuestion<?, S>> questions, boolean showScore, boolean showCorrect) {
        this(questions, showScore, showCorrect, -1, TimeUnit.MINUTES);
    }

    protected Parent makeUI() throws IOException{
        UIQuizView uiQuizView = new UIQuizView();
        UIQuizController controller = uiQuizView.getController();
        this.questUIAnchorPane = controller.getQuestUIAnchorPane();

        //always keep the score up to date
        for (UIQuestion<?,S> question : questions) question.getScoreProperty().addListener((e, i, o) -> scoreProperty.set(computeScore(questions.stream().map(Question::getScore).toList())));

        controller.getScoreLabel().setText(showScore ? "Score: " + (getScore() != null ? getScore() : "") : "");

        if (showScore) scoreProperty.addListener(e -> controller.getScoreLabel().setText("Score: " + scoreProperty.get().toString()));

//        if (showCorrect) for (UIQuestion<?, S> question : questions) question.submittedAnswerProperty().addListener(e -> question.showCorrectAnswer());
        if (showCorrect) for (UIQuestion<?, S> question : questions) question.showCorrectAnswerOnWrongSubmit(true);

        // set up the timer if needed
        if (allowedTime > -1) {
            AtomicInteger cycleCount = new AtomicInteger();
            SimpleObjectProperty<Duration> durationProp = new SimpleObjectProperty<>();
            switch (timeUnit) {
                case HOURS -> cycleCount.set(allowedTime * 60 * 60);
                case MINUTES -> cycleCount.set(allowedTime * 60);
                case SECONDS -> cycleCount.set(allowedTime);
            }

            timeline = new Timeline(
                    new KeyFrame(Duration.seconds(1), e -> {
                        cycleCount.getAndDecrement();
                        durationProp.set(Duration.seconds(cycleCount.get()));

                        int hrs = cycleCount.get() / 3600;
                        int mins = (cycleCount.get() % 3600) / 60;
                        int secs = cycleCount.get() % 60;
                        controller.getTimeLabel().setText(String.format("%02d:%02d:%02d", hrs, mins, secs));
                    })
            );
            timeline.setCycleCount(cycleCount.get());

        } else controller.getTimeLabel().setText("        ");   // for correct spacing

        HBox.setHgrow(controller.getTopSpacer1(), Priority.ALWAYS);
        HBox.setHgrow(controller.getTopSpacer2(), Priority.ALWAYS);


        currentQuestion.addListener(observable -> {
            controller.getQuestProgressLabel().setText("Question " + (currentQuestIndex+1) + "/" + this.questions.size());
            controller.getPrevButton().setDisable(!canPrevious());
            controller.getNextButton().setDisable(!canNext());
        });

        controller.getPrevButton().setDisable(true);
        controller.getNextButton().setDisable(!canNext());

        controller.getNextButton().setOnAction(e -> next());
        controller.getPrevButton().setOnAction(e -> previous());
        controller.getEndButton().setOnAction(e -> stop());

        return uiQuizView.getRoot();
    }
    

    @Override
    public void start() {
        if (runningProperty.get()) return;
        finishedProperty.set(false);
        runningProperty.set(true);
        try {
            Scene scene = new Scene(makeUI());
            scene.getStylesheets().add(getClass().getResource("/Project/Styles/fonts.css").toExternalForm());
            Stage popupStage = new Stage();
            popupStage.setTitle(this.getName());
            popupStage.setScene(scene);
            popupStage.show();
            if (timeline != null) timeline.play();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to start quiz:\n" + e.getMessage() + Arrays.toString(e.getStackTrace()));
            LittlePopUp.showMsg("Error", "Failed to start quiz:\n" + e.getMessage() + Arrays.toString(e.getStackTrace()), "OK");
        }
    }

    @Override
    public void stop() {
        //TODO: allow stop if not all questions have been answered / looked at? -> warn message
        runningProperty.set(false);
        finishedProperty.set(true);
    }

    @Override
    public double getProgress() {
        return this.progress;
    }

    @Override
    public S getScore() {
        return scoreProperty.get();
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
        return currentQuestIndex < questions.size()-1;
    }

    @Override
    public boolean canPrevious() {
        return currentQuestIndex > 0;
    }

    @Override
    public UIQuestion<?, S> next() {
        if (canNext()){
            currentQuestIndex++;
            currentQuestion.set(questions.get(currentQuestIndex));
            setQuestUIAnchorPaneChild(currentQuestion.get().ask());
            return currentQuestion.get();
        } else return null;
    }

    @Override
    public UIQuestion<?, S> previous() {
        if (canPrevious()) {
            currentQuestIndex--;
            currentQuestion.set(questions.get(currentQuestIndex));
            setQuestUIAnchorPaneChild(currentQuestion.get().ask());
            return currentQuestion.get();
        } else return null;
    }

}