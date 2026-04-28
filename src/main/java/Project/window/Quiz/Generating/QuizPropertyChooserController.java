package Project.window.Quiz.Generating;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class QuizPropertyChooserController {

    @FXML
    private Label noTreeSelectedLabel;
    @FXML
    private Label topicsInfoLabel;
    @FXML
    private ToolBar treeItemChoosingToolbar;
    @FXML
    private HBox treeItemDisplayOuterHBox;
    @FXML
    private ScrollPane treeItemDisplayScrollpane;
    @FXML
    private ChoiceBox<String> timeUnitChoicebox;
    @FXML
    private Slider difficultySlider;
    @FXML
    private Button acceptButton;
    @FXML
    private HBox treeItemDisplayHBox;
    @FXML
    private Button addTreeItemButton;
    @FXML
    private TextField timeTextField;
    @FXML
    private MenuButton timeUnitMenuButton;
    @FXML
    private CheckBox showCorrectAnswerCheckbox;
    @FXML
    private ToolBar difficultyToolbar;
    @FXML
    private MenuButton difficutlyMenuButton;
    @FXML
    private ToolBar nquestionsToolbar;
    @FXML
    private TextField nquestionsTextfield;

    public HBox getTreeItemDisplayHBox() {
        return treeItemDisplayHBox;
    }

    public Button getAddTreeItemButton() {
        return addTreeItemButton;
    }

    public TextField getTimeTextField() {
        return timeTextField;
    }

    public MenuButton getTimeUnitMenuButton() {
        return timeUnitMenuButton;
    }

    public CheckBox getShowCorrectAnswerCheckbox() {
        return showCorrectAnswerCheckbox;
    }

    public ToolBar getDifficultyToolbar() {
        return difficultyToolbar;
    }

    public MenuButton getDifficutlyMenuButton() {
        return difficutlyMenuButton;
    }

    public ToolBar getNquestionsToolbar() {
        return nquestionsToolbar;
    }

    public TextField getNquestionsTextfield() {
        return nquestionsTextfield;
    }

    public Button getAcceptButton() {
        return acceptButton;
    }

    public Slider getDifficultySlider() {
        return difficultySlider;
    }

    public ChoiceBox<String> getTimeUnitChoiceBox() {
        return timeUnitChoicebox;
    }

    public ScrollPane getTreeItemDisplayScrollpane() {
        return treeItemDisplayScrollpane;
    }

    public HBox getTreeItemDisplayOuterHBox() {
        return treeItemDisplayOuterHBox;
    }

    public ToolBar getTreeItemChoosingToolbar() {
        return treeItemChoosingToolbar;
    }

    public Label getTopicsInfoLabel() {
        return topicsInfoLabel;
    }

    public Label getNoTreeSelectedLabel() {
        return noTreeSelectedLabel;
    }
}
