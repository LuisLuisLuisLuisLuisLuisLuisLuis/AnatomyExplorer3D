package Project.window.Quiz;

import Project.window.PopUp.LittlePopUp;
import Project.window.Quiz.Question.Needs3DAccess;
import Project.window.Quiz.Question.Question;
import Project.window.Quiz.Question.SimpleMultipleChoice3DQuestion;
import Project.window.Quiz.Question.UIQuestion;
import Project.window.ThreeDPaneHandling.Movement.Group3DRotation;
import Project.window.ThreeDPaneHandling.ThreeDGroupHandler;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.xml.stream.EventFilter;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
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
        questUIAnchorPane.prefHeightProperty().bind(controller.getQuestSplitPane().heightProperty().multiply(0.999));
        questUIAnchorPane.getChildren().clear();
        questUIAnchorPane.getChildren().add(child);
    }
    /** The stage of this quiz.*/
    protected Stage popupStage = new Stage();

    protected UIQuizController controller;

    protected Pane threeDPane = new Pane();
    protected Label threeDSelectedLabel = new Label();
    protected VBox threeDUI = new VBox(threeDPane, threeDSelectedLabel);

    /**
     * @return if there are any SimpleMultipleChoice3DQuestion in this quiz
     */
    public boolean has3DQuestions() {
        for (UIQuestion<?, S> question : this.questions) if (question instanceof SimpleMultipleChoice3DQuestion) return true;
        return false;
    }

    //don't give to constructor but only set (via jackson) when creating from save file
    private S pb = null;

    private String name = null;

    private String description = null;

    private final BooleanProperty runningProperty = new SimpleBooleanProperty(false);

    private final BooleanProperty finishedProperty = new SimpleBooleanProperty(false);

    private double progress = 0;

    private final ArrayList<UIQuestion<?, S>> questions;

    protected final List<SimpleObjectProperty<S>> scores;

    private int currentQuestIndex = -1;

    private SimpleObjectProperty<UIQuestion<?, S>> currentQuestionProperty = new SimpleObjectProperty<>();

    private final boolean showScore;

    private final boolean showCorrect;

    private final int allowedTime;

    private final TimeUnit timeUnit;

    private Timeline timeline;

    private ThreeDGroupHandler threeDGroupHandler = null;
    /**
     * 
     * @param questions the questions for this quiz
     * @param showScore whether to display the currents score (and highscore) during the quiz
     * @param showCorrect whether to inform the user if their answer was correct or not and if not to show them the correct solution
     * @param allowedTime time after which the quiz will automatically end
     * @param timeUnit time unit of the given allowed time. Must be one of {HOURS, MINUTES, SECONDS}.
     */
    public UIQuiz(List<UIQuestion<?, S>> questions, boolean showScore, boolean showCorrect, int allowedTime, TimeUnit timeUnit) {
        if (timeUnit == TimeUnit.DAYS || timeUnit == TimeUnit.MICROSECONDS || timeUnit == TimeUnit.MILLISECONDS) throw new IllegalArgumentException("Timeunit must be one of {HOURS, MINUTES, SECONDS}.");
        questions.removeIf(Objects::isNull);
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
        this.runningTasks.addListener(new ListChangeListener<Task>() {
            @Override
            public void onChanged(Change<? extends Task> c) {
                logger.log(Level.CONFIG, "running tasks: " + runningTasks.size());
            }
        });
        UIQuizView uiQuizView = new UIQuizView();
        this.controller = uiQuizView.getController();
        this.questUIAnchorPane = controller.getQuestUIAnchorPane();

        if (has3DQuestions()) {
            this.threeDGroupHandler = new ThreeDGroupHandler(this.threeDPane, new PerspectiveCamera(true), ThreeDGroupHandler.DEFAULT_INITIAL_CAMERA_POSITION, "quiz3DHandler");
            this.threeDGroupHandler.getHasFXGroupContentsContainer().setHandleLoadingTask(task -> {runBlockingTask(task, true); return null;});
            this.threeDGroupHandler.setHandleLoadingTask((task -> {runBlockingTask(task, true);return null;}));

            VBox.setVgrow(threeDPane, Priority.ALWAYS);
            for (UIQuestion<?, S> question : this.questions) {
                if (question instanceof Needs3DAccess needs3DAccessQuestion) {
                    needs3DAccessQuestion.giveAccess(this.threeDGroupHandler);
                }
            }
            this.threeDGroupHandler.getHasFXGroupSelection().getSelection().addListener((InvalidationListener) e -> {   // note how many items are selected
                int size = this.threeDGroupHandler.getHasFXGroupSelection().getSelection().size();
                this.threeDSelectedLabel.setText(size + (size == 1 ? " item selected." : " items selected."));
            });

        }

        //always keep the score up to date
        for (UIQuestion<?,S> question : questions) question.getScoreProperty().addListener((e, i, o) -> scoreProperty.set(computeScore(questions.stream().map(Question::getScore).toList())));

        controller.getScoreLabel().setText(showScore ? "Score: " + (getScore() != null ? getScore() : "") : "");

        if (showScore) scoreProperty.addListener(e -> controller.getScoreLabel().setText("Score: " + scoreProperty.get().toString()));

//        if (showCorrect) for (UIQuestion<?, S> question : questions) question.submittedAnswerProperty().addListener(e -> question.showCorrectAnswer());
        for (UIQuestion<?, S> question : questions) question.showCorrectAnswerOnWrongSubmit(this.showCorrect);

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

            timeline.setOnFinished(e -> {
                controller.getMainBorderpane().setDisable(true);
                controller.getMainBorderpane().setEffect(new GaussianBlur(10));
                controller.getTimeupVbox().setVisible(true);
                controller.getTimeupPtsLabel().setText("Score: " + getScore());
                controller.getTimeupQuitButton().setOnAction(ev -> stop());
            });

        } else controller.getTimeLabel().setText("        ");   // for correct spacing

        HBox.setHgrow(controller.getTopSpacer1(), Priority.ALWAYS);
        HBox.setHgrow(controller.getTopSpacer2(), Priority.ALWAYS);


        currentQuestionProperty.addListener(observable -> {
            controller.getQuestProgressLabel().setText("Question " + (currentQuestIndex+1) + "/" + this.questions.size());
            controller.getPrevButton().setDisable(!canPrevious());
            controller.getNextButton().setDisable(!canNext());
        });

        controller.getPrevButton().setDisable(true);
        controller.getNextButton().setDisable(!canNext());

        controller.getNextButton().setOnAction(e -> next());
        controller.getPrevButton().setOnAction(e -> previous());
        controller.getEndButton().setOnAction(e -> {
            if (LittlePopUp.showMsg("Quit", "Do you really want to quit?", "Yes", "No")) stop();
        });

        return uiQuizView.getRoot();
    }

    protected ObservableList<Task> runningTasks = FXCollections.observableList(new LinkedList<>());

    /*
     * blurs the UI and also halts the timer of the quiz.
     */
    protected void blockingEffect(boolean flag) {
        logger.log(Level.CONFIG, "Applying blurr="+flag);
        VBox blockingVBox = controller.getMainProgressVBox();

        blockingVBox.setManaged(flag);
        blockingVBox.setVisible(flag);
        controller.getMainBorderpane().setDisable(flag);    // to disable buttons and all

        if (flag) {
            if (timeline != null) timeline.pause();
        } else {
            if (timeline != null) timeline.play();
        }

        controller.getMainBorderpane().setEffect(flag ? new GaussianBlur(10) : null);
    }

    /**
     * Binds a progress showing UI to a Task and runs it on a new Thread. Blurrs the main window while the task is running.
     * @param task The task
     * @param knowsProgress Can the task report progress or is progress indetermined?
     */
    public <T> void runBlockingTask(Task<T> task, boolean knowsProgress) {

        logger.log(Level.CONFIG, "binding task. " + task.getMessage());

        task.stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.CANCELLED || newState == Worker.State.SUCCEEDED || newState == Worker.State.FAILED) {
                runningTasks.remove(task);
                if (!runningTasks.isEmpty()) {
                    controller.getMainProgressBar().progressProperty().unbind();
                    controller.getMainProgressInd().progressProperty().unbind();
                    controller.getMainProgressLabel().labelForProperty().unbind();
                    Task nextTask = runningTasks.getFirst();
                    controller.getMainProgressLabel().textProperty().bind(nextTask.messageProperty());
                    controller.getMainProgressBar().progressProperty().bind(nextTask.progressProperty());
                    controller.getMainProgressInd().progressProperty().bind(nextTask.progressProperty());

                } else {
                    blockingEffect(false);
                }

            } else if (newState == Worker.State.RUNNING) {
                blockingEffect(true);
                if (runningTasks.isEmpty()) {
                    logger.log(Level.CONFIG, "binding first task " + task.getMessage());

                    controller.getMainProgressLabel().textProperty().bind(task.messageProperty());

                    if (knowsProgress) {
                        controller.getMainProgressBar().progressProperty().bind(task.progressProperty());
                        controller.getMainProgressInd().progressProperty().bind(task.progressProperty());
                    } else {
                        controller.getMainProgressInd().setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                        controller.getMainProgressBar().setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                    }
                }
                runningTasks.add(task);
            }
        });

        new Thread(task).start();
        blockingEffect(true);
    }

    /**
     * does things that need to be done every time the current question changes
     */
    protected void onQuestionChange() {
        UIQuestion<?, S> currentQuestion = currentQuestionProperty.get();
        // only show the 3D Pane if needed
        if (currentQuestion instanceof Needs3DAccess) {
            if (!controller.getQuestSplitPane().getItems().contains(this.threeDUI)) controller.getQuestSplitPane().getItems().add(this.threeDUI);
        }
        else controller.getQuestSplitPane().getItems().remove(this.threeDUI);
    }
    

    @Override
    public void start() {
        if (runningProperty.get()) return;
        finishedProperty.set(false);
        runningProperty.set(true);
        try {
            Scene scene = new Scene(makeUI(),900,700);
            scene.getStylesheets().add(getClass().getResource("/Project/Styles/fonts.css").toExternalForm());
            popupStage.setTitle(this.getName());
            popupStage.setScene(scene);
            setKeyControls(scene);
            popupStage.show();
            popupStage.setOnCloseRequest(e -> {
                e.consume();
                stop();
            });
            if (timeline != null) timeline.play();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to start quiz:\n" + e.getMessage() + Arrays.toString(e.getStackTrace()));
            LittlePopUp.showMsg("Error", "Failed to start quiz:\n" + e.getMessage() + Arrays.toString(e.getStackTrace()), "OK");
        }
    }

    @Override
    public void stop() {
        runningProperty.set(false);
        finishedProperty.set(true);
        popupStage.close();
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
            currentQuestionProperty.set(questions.get(currentQuestIndex));
            onQuestionChange();
            logger.log(Level.CONFIG, "Asking next question: " + currentQuestionProperty.get().getName() + "\n" + currentQuestionProperty.get().getInstructions());
            setQuestUIAnchorPaneChild(currentQuestionProperty.get().ask());
            controller.getNextButton().requestFocus();
            return currentQuestionProperty.get();
        } else return null;
    }

    @Override
    public UIQuestion<?, S> previous() {
        if (canPrevious()) {
            currentQuestIndex--;
            currentQuestionProperty.set(questions.get(currentQuestIndex));
            logger.log(Level.CONFIG, "Asking previous question: " + currentQuestionProperty.get().getName() + "\n" + currentQuestionProperty.get().getInstructions());
            setQuestUIAnchorPaneChild(currentQuestionProperty.get().ask());
            controller.getPrevButton().requestFocus();
            onQuestionChange();
            return currentQuestionProperty.get();
        } else return null;
    }

    /**
     * Enables controlling the UI via keyboard shortcuts by adding EventFilter to the given Scene.
     */
    protected void setKeyControls(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            boolean alt = event.isAltDown();
            boolean shift = event.isShiftDown();
            KeyCode keyCode = event.getCode();
            if (event.isControlDown() || alt) {
                if (List.of(KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN, KeyCode.PLUS, KeyCode.MINUS).contains(keyCode)) threeDGroupHandler.rotationControl(keyCode, alt, shift);
            }
        });
    }

}