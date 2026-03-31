package Project.window.TreeView.TreeRestructuring;

import Project.model.ANode;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class TreeRestructuringController {

    @FXML
    private HBox legend;
    @FXML
    private ToolBar botToolBar;
    @FXML
    private Button redoButton;
    @FXML
    private Button undoButton;
    @FXML
    private Button addMissingToTreeButton;

    public Button getAddMissingToTreeButton() {return addMissingToTreeButton;}

    @FXML
    private Button searchForDupIDsButton;

    public Button getSearchForDupIDsButton() {return searchForDupIDsButton;}

    @FXML
    private ToolBar searchToolBar;

    @FXML
    private TextField treeSearchField;
    @FXML
    private Button treeSearchPrevButton;
    @FXML
    private Button treeSearchNextButton;
    @FXML
    private CheckBox regexCheck;
    @FXML
    private TextArea aiTextArea;
    @FXML
    private Button parseButton;
    @FXML
    private TableView<TreeRestructuring.TreeItemRelationShip> tableView;
    @FXML
    private TreeView<ANode> treeView;
    @FXML
    private Button acceptButton;

    public Button getAcceptButton() {
        return acceptButton;
    }

    public TextArea getEdgeTextArea() {
        return aiTextArea;
    }

    public Button getParseButton() {
        return parseButton;
    }

    public TableView<TreeRestructuring.TreeItemRelationShip> getTableView() {
        return tableView;
    }

    public TreeView<ANode> getTreeView() {
        return treeView;
    }

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

    public Button getRedoButton() {return redoButton;}

    public Button getUndoButton() {return undoButton;}

    public ToolBar getBotToolBar() {return botToolBar;}

    public HBox getLegend() {return legend;}
}
