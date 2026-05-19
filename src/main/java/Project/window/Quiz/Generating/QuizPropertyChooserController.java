package Project.window.Quiz.Generating;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class QuizPropertyChooserController {

    @FXML
    private Label noTreeSelectedLabel;
    @FXML
    private ToolBar treeItemChoosingToolbar;
    @FXML
    private HBox treeItemDisplayOuterHBox;
    @FXML
    private ScrollPane treeItemDisplayScrollpane;
    @FXML
    private ChoiceBox<String> timeUnitChoicebox;
    @FXML
    private Slider howMuchDrawSlider;
    @FXML
    private Button generateButton;
    @FXML
    private HBox treeItemDisplayHBox;
    @FXML
    private Button addTreeItemButton;
    @FXML
    private TextField timeTextField;
    @FXML
    private CheckBox showCorrectAnswerCheckbox;
    @FXML
    private ToolBar difficultyToolbar;
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

    public CheckBox getShowCorrectAnswerCheckbox() {
        return showCorrectAnswerCheckbox;
    }

    public ToolBar getDifficultyToolbar() {
        return difficultyToolbar;
    }

    public ToolBar getNquestionsToolbar() {
        return nquestionsToolbar;
    }

    public TextField getNquestionsTextfield() {
        return nquestionsTextfield;
    }

    public Button getGenerateButton() {
        return generateButton;
    }

    public Slider getDifficultySlider() {
        return howMuchDrawSlider;
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

    public Label getNoTreeSelectedLabel() {
        return noTreeSelectedLabel;
    }
}
