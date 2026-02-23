package Project.window.TreeView.AITreeIntegration.MVPattern;

import Project.model.ANode;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AITreeIntegrationController {

    @FXML
    private Button addMissingToTreeButton;

    public Button getAddMissingToTreeButton() {return addMissingToTreeButton;}

    @FXML
    private Button searchForDupIDsButton;

    public Button getSearchForDupIDsButton() {return searchForDupIDsButton;}

    @FXML
    private ToolBar searchToolBar;

    public ToolBar getSearchToolBar() {
        return searchToolBar;
    }

    public TextField getTreeSearchField() {
        return treeSearchField;
    }

    public Button getTreeSearchPrevButton() {
        return treeSearchPrevButton;
    }

    public Button getTreeSearchNextButton() {
        return treeSearchNextButton;
    }


    public CheckBox getRegexCheck() {
        return regexCheck;
    }

    @FXML
    private TextField treeSearchField;

    @FXML
    private Button treeSearchPrevButton;

    @FXML
    private Button treeSearchNextButton;


    @FXML
    private CheckBox regexCheck;

    public Button getAcceptButton() {
        return acceptButton;
    }

    public TextArea getAiTextArea() {
        return aiTextArea;
    }

    public Button getParseButton() {
        return parseButton;
    }

    public TableView<AITreeIntegration.TreeItemRelationShip> getTableView() {
        return tableView;
    }

    public TreeView<ANode> getTreeView() {
        return treeView;
    }

    @FXML
    private TextArea aiTextArea;

    @FXML
    private Button parseButton;

    @FXML
    private TableView<AITreeIntegration.TreeItemRelationShip> tableView;

    @FXML
    private TreeView<ANode> treeView;

    @FXML
    private Button acceptButton;

}
