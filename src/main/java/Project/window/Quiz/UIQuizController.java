package Project.window.Quiz;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.AnchorPane;

public class UIQuizController {

    @FXML
    private ToolBar titleToolbar;
    @FXML
    private ToolBar scoreToolBar;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button endButton;
    @FXML
    private Label pbLabel;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private Label correctLabel;
    @FXML
    private AnchorPane questUIAnchorPane;
    @FXML
    private Label questNameLabel;
    @FXML
    private Label questLabel;
    @FXML
    private TextArea descriptionArea;

    public Label getPbLabel() {
        return pbLabel;
    }

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

    public Label getQuestNameLabel() {
        return questNameLabel;
    }

    public Label getQuestLabel() {
        return questLabel;
    }

    public TextArea getDescriptionArea() {
        return descriptionArea;
    }

    public ToolBar getScoreToolBar() {return scoreToolBar;}

    public ToolBar getTitleToolbar() {return titleToolbar;}
}
