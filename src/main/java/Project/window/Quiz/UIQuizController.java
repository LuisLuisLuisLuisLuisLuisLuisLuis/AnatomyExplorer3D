package Project.window.Quiz;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class UIQuizController {

    @FXML
    private BorderPane mainBorderpane;
    @FXML
    private VBox mainProgressVBox;
    @FXML
    private ProgressIndicator mainProgressInd;
    @FXML
    private ProgressBar mainProgressBar;
    @FXML
    private Label mainProgressLabel;
    @FXML
    private SplitPane questSplitPane;
    @FXML
    private Region topSpacer1;
    @FXML
    private Region topSpacer2;
    @FXML
    private ToolBar questInfoToolbar;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button endButton;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private Label correctLabel;
    @FXML
    private AnchorPane questUIAnchorPane;
    @FXML
    private Label questProgressLabel;

    public Button getPrevButton() {
        return prevButton;
    }

    public Button getNextButton() {
        return nextButton;
    }

    public Button getEndButton() {
        return endButton;
    }

    public Label getScoreLabel() {
        return scoreLabel;
    }

    public Label getTimeLabel() {
        return timeLabel;
    }

    public Label getCorrectLabel() {
        return correctLabel;
    }

    public AnchorPane getQuestUIAnchorPane() {
        return questUIAnchorPane;
    }

    public Label getQuestProgressLabel() {
        return questProgressLabel;
    }

    public ToolBar getTitleToolbar() {
        return questInfoToolbar;
    }

    public Region getTopSpacer2() {
        return topSpacer2;
    }

    public Region getTopSpacer1() {
        return topSpacer1;
    }

    public SplitPane getQuestSplitPane() {
        return questSplitPane;
    }

    public VBox getMainProgressVBox() {
        return mainProgressVBox;
    }

    public BorderPane getMainBorderpane() {
        return mainBorderpane;
    }

    public ProgressIndicator getMainProgressInd() {
        return mainProgressInd;
    }

    public ProgressBar getMainProgressBar() {
        return mainProgressBar;
    }

    public Label getMainProgressLabel() {
        return mainProgressLabel;
    }

    public ToolBar getQuestInfoToolbar() {
        return questInfoToolbar;
    }
}

