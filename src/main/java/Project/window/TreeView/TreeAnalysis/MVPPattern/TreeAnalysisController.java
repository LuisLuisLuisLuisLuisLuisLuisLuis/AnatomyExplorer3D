package Project.window.TreeView.TreeAnalysis.MVPPattern;

import Project.window.TreeView.TreeViewEditing.Command.UndoableANodeTreeViewEditor;
import Project.window.TreeView.TreeViewEditing.TreeViewSetup;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class TreeAnalysisController {

    @FXML
    private TextField nameTextfield;
    @FXML
    private HBox legend;
    @FXML
    private ToolBar botToolBar;
    @FXML
    private Button searchButton;

    @FXML
    private Label botlabel;

    @FXML
    private Button removeFileIDButton;

    @FXML
    private Button addFileIDButton;

    @FXML
    private TextField addFileIDTextField;

    @FXML
    private TableView<TreeViewSetup.FileIDRow> fileIDTableView;

    public Button getAddFileIDButton() {
        return addFileIDButton;
    }

    public TextField getAddFileIDTextField() {
        return addFileIDTextField;
    }

    public TableView<TreeViewSetup.FileIDRow> getFileIDTableView() {
        return fileIDTableView;
    }

    public Button getRemoveFileIDButton() {
        return removeFileIDButton;
    }

    public Button getSearchButton() {return searchButton;}

    public Label getBotlabel() {return botlabel;}

    public ToolBar getBotToolBar() {return botToolBar;}

    public HBox getLegend() {return legend;}

    public TextField getNameTextfield() {return nameTextfield;}
}
